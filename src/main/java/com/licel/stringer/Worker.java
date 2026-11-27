package com.licel.stringer;

import com.licel.stringer.analysis.ClassMetadataCollector;
import com.licel.stringer.config.PatternMatchUtils;
import com.licel.stringer.crypto.AESFast;
import com.licel.stringer.crypto.CryptoContainer;
import com.licel.stringer.crypto.SipHash;
import com.licel.stringer.io.DirectoryResourcesOutput;
import com.licel.stringer.io.DirectorySourceIterator;
import com.licel.stringer.io.JarIndex;
import com.licel.stringer.io.ResourceEntry;
import com.licel.stringer.io.ResourcesOutput;
import com.licel.stringer.io.SourceIterator;
import com.licel.stringer.io.ZipResourcesOutput;
import com.licel.stringer.io.ZipSourceIterator;
import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.model.ClassMemberInfo;
import com.licel.stringer.model.Job;
import com.licel.stringer.model.PackageProtectionInfo;
import com.licel.stringer.model.ProtectionRegistry;
import com.licel.stringer.model.ResourceBundleInfo;
import com.licel.stringer.model.SigningIdentity;
import com.licel.stringer.naming.AlphabeticNameGenerator;
import com.licel.stringer.naming.NameGenerator;
import com.licel.stringer.runtime.IntrinsicsCreator;
import com.licel.stringer.runtime.LruCacheCreator;
import com.licel.stringer.runtime.ReflectionClassCreator;
import com.licel.stringer.runtime.StringDecoderInjector;
import com.licel.stringer.runtime.StringerFilterInputStreamCreator;
import com.licel.stringer.runtime.StringerInflaterCreator;
import com.licel.stringer.runtime.StringerInputStreamCreator;
import com.licel.stringer.runtime.StringerStreamHandlerFactoryCreator;
import com.licel.stringer.runtime.StringerURLConnectionCreator;
import com.licel.stringer.runtime.StringerURLHandlerCreator;
import com.licel.stringer.transform.ClassCleanupTransformer;
import com.licel.stringer.transform.ClassEncryptionTransformer;
import com.licel.stringer.transform.ClassNameRemappingTransformer;
import com.licel.stringer.transform.ClassOptimizationTransformer;
import com.licel.stringer.transform.ClassProtectionTransformer;
import com.licel.stringer.transform.ClassTransformer;
import com.licel.stringer.transform.FieldInitializationTransformer;
import com.licel.stringer.transform.IntrinsicsTransformer;
import com.licel.stringer.transform.ResourceReflectionTransformer;
import com.licel.stringer.transform.RuntimeClassTransformer;
import com.licel.stringer.transform.StringDecoderTransformer;
import com.licel.stringer.util.StringerUtils;
import com.licel.stringer.webapp.CatalinaApplicationContextWrapperCreator;
import com.licel.stringer.webapp.WebContextClassLoaderCreator;
import com.licel.stringer.webapp.WebContextLoaderListenerCreator;
import com.licel.stringer.webapp.WebXmlCompiler;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class Worker {
    private final Config config;
    private static final ThreadLocal<WorkerContext> CONTEXT = new WorkerContextThreadLocal();

    public static WorkerContext get() {
        return CONTEXT.get();
    }

    public Worker(Config config) {
        get().secureRandom = new SecureRandom();
        WorkerListener listener = config.getListener();
        this.config = config;
        this.config.validate();

        this.config.dump(listener);
        get().internalNameGenerator = new AlphabeticNameGenerator(get().secureRandom);
    }

    public final void stringerIt() {
        this.config.getListener();
        ArrayList<Config> elementConfigs = this.config.generateProtectionElementsConfigs();
        boolean enterpriseOnly = elementConfigs.size() != 1 || elementConfigs.get(0).getPaths() != null;
        if (!enterpriseOnly) {
            get().processedStringCount = 0;
            get().config = elementConfigs.get(0);
            get().cryptoContainer = new CryptoContainer(get().secureRandom, null, true);
            get().nameGenerator = new AlphabeticNameGenerator(get().secureRandom);
            try {
                this.executeProtection();
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            WorkerException ex = new WorkerException(
                    this,
                    "Unfortunately this functionality is supported in the enterprise version only, please visit https://jfxstore.com/stringer/request-quote to request your new-user/upgrade quote.");
            ex.setStackTrace(new StackTraceElement[0]);
            throw ex;
        }
    }

    @SuppressWarnings("unchecked")
    private void executeProtection() throws Exception {
        Config activeConfig = get().config;
        boolean verbose = activeConfig.isVerbose();
        SecureRandom random = get().secureRandom;
        WorkerListener listener = activeConfig.getListener();
        long startTime = System.currentTimeMillis();
        listener.info("[Protecting " + activeConfig.getSrcFile().getName() + "]");

        int classesProcessed = 0;
        int resourcesProcessed = 0;
        HashMap<String, Object> seenEntries = new HashMap<>();
        get().sourceIterator = null;

        if (activeConfig.getSrcFile().isFile()) {
            get().sourceIterator = new ZipSourceIterator(activeConfig.getSrcFile());
            if (!activeConfig.getDestFile().exists()) {
                File parent = activeConfig.getDestFile().getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                activeConfig.getDestFile().createNewFile();
            }
        } else {
            get().sourceIterator = new DirectorySourceIterator(activeConfig.getSrcFile());
            if (!activeConfig.getDestFile().exists()) {
                activeConfig.getDestFile().mkdirs();
            }
        }

        Watermark watermark = new Watermark(activeConfig, activeConfig.getSrcFile());
        SourceIterator sourceIterator = get().sourceIterator;
        HashMap<String, Object> plan = this.buildProtectionPlan(sourceIterator, watermark);
        if (plan == null) {
            listener.info("[Finishing " + activeConfig.getSrcFile().getName() + "]");
            return;
        }

        boolean zipOutput = false;
        ResourcesOutput output;
        if (activeConfig.getDestFile().isFile()) {
            output = new ZipResourcesOutput(activeConfig.getDestFile(), activeConfig.isOptimize());
            zipOutput = true;
        } else {
            output = new DirectoryResourcesOutput(activeConfig.getSrcFile(), activeConfig.getDestFile());
        }

        output.open();
        Iterator<ResourceEntry> entries = sourceIterator.entries();
        long inputBytes = 0L;
        long outputBytes = 0L;
        JarIndex jarIndex = null;
        boolean manifestPatched = false;

        if (zipOutput) {
            try (ZipFile zipFile = new ZipFile(activeConfig.getSrcFile())) {
                if (zipFile.getEntry(JarIndex.INDEX_NAME) != null) {
                    jarIndex = new JarIndex(new String[]{activeConfig.getDestFile().getName()});
                }
            }
        }

        Manifest watermarkManifest = watermark.createManifest(random);
        if (activeConfig.getIntegrityControl().sealJar().isEnabled()) {
            watermarkManifest.getMainAttributes().putValue("Sealed", "true");
        }

        while (entries.hasNext()) {
            ResourceEntry entry = entries.next();
            inputBytes += entry.getSize();
            String entryName = entry.getName();
            String normalizedName = entry.getNormalizedName();

            if (seenEntries.containsKey(normalizedName)) {
                if (verbose) {
                    listener.verbose("Skipping duplicate file: " + entryName);
                }
                continue;
            }
            seenEntries.put(normalizedName, null);

            if (jarIndex != null
                    && !entryName.equals("META-INF")
                    && !entryName.equals(JarIndex.INDEX_NAME)
                    && !entryName.equals("META-INF/MANIFEST.MF")
                    && entryName.startsWith("META-INF/")
                    && !entry.isDirectory()) {
                jarIndex.addResource(entryName, activeConfig.getDestFile().getName());
            }

            if (!activeConfig.isNoneMode()
                    && entryName.startsWith("META-INF/")
                    && (entryName.endsWith(".SF") || entryName.endsWith(".RSA") || entryName.endsWith(".DSA"))) {
                if (verbose) {
                    listener.verbose("Removing signature file: " + entryName);
                }
                continue;
            }

            if ("META-INF/MANIFEST.MF".equals(entryName)) {
                if (verbose) {
                    listener.verbose("Patching META-INF/MANIFEST.MF");
                }
                Manifest existingManifest = new Manifest();
                try (BufferedInputStream in = new BufferedInputStream(sourceIterator.openStream(entry))) {
                    existingManifest.read(in);
                }
                existingManifest.getMainAttributes().putAll(watermarkManifest.getMainAttributes());
                output.openEntry(new ResourceEntry(entry.getName()));
                ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
                existingManifest.write(manifestBytes);
                output.write(manifestBytes.toByteArray());
                manifestBytes.close();
                manifestPatched = true;
                watermarkManifest.getMainAttributes().putAll(existingManifest.getMainAttributes());
            } else if (entry.isDirectory()) {
                output.openEntry(new ResourceEntry(entry.getName(), true));
            } else if (plan.containsKey(normalizedName)) {
                output.openEntry(new ResourceEntry(entry.getName()));
                Job job = (Job) plan.get(normalizedName);
                switch (job.type) {
                    case Job.TRANSFORM_CLASS:
                        classesProcessed++;
                        if (verbose) {
                            listener.verbose("Processing class: " + entryName);
                        }
                        byte[] transformed;
                        try (BufferedInputStream in = new BufferedInputStream(sourceIterator.openStream(entry))) {
                            transformed = this.applyClassTransformations(in, job);
                        }
                        outputBytes += transformed.length;
                        output.write(transformed);
                        break;
                    case Job.ENCRYPT_RESOURCE:
                        resourcesProcessed++;
                        if (verbose) {
                            listener.verbose("Processing resource: " + entryName);
                        }
                        int encryptedLength = AESFast.encryptResource(
                                sourceIterator.openStream(entry),
                                output,
                                get().cryptoContainer.getRoundKeys(),
                                get().cryptoContainer.createResourceIvWords(),
                                activeConfig);
                        outputBytes += encryptedLength;
                        break;
                    default:
                        break;
                }
            } else if (jarIndex != null && JarIndex.INDEX_NAME.equals(entryName)) {
                listener.verbose("Skipping: " + entryName);
            } else if (!(output instanceof DirectoryResourcesOutput) || !((DirectoryResourcesOutput) output).overwriteSource) {
                outputBytes += entry.getSize();
                output.openEntry(new ResourceEntry(entry.getName()));
                StringerUtils.copyStream(sourceIterator.openStream(entry), output);
            } else if (verbose) {
                listener.verbose("Skipping (input and output are the same directory): " + entryName);
            }
        }

        for (String key : plan.keySet()) {
            if (key.endsWith("#newClass")) {
                RuntimeClassTransformer creator = (RuntimeClassTransformer) plan.get(key);
                ClassInfo createdInfo = creator.getClassInfo();
                ResourceEntry newEntry = new ResourceEntry(createdInfo.name + ".class");
                ClassWriter writer = new ClassWriter(0);
                ClassVisitor visitor = creator.createVisitor(writer);
                if (visitor == null) {
                    throw new RuntimeException("Internal error");
                }

                if (!(visitor instanceof ReflectionClassCreator) || !creator.getClassInfo().hiddenMembers.isEmpty()) {
                    byte[] classBytes = writer.toByteArray();
                    if (plan.containsKey(newEntry.getNormalizedName() + "n")) {
                        try (ByteArrayInputStream in = new ByteArrayInputStream(classBytes)) {
                            classBytes = this.applyClassTransformations(in, (Job) plan.get(newEntry.getNormalizedName() + "n"));
                        }
                    }
                    if (classBytes.length == 0) {
                        throw new RuntimeException("Internal error");
                    }
                    if (verbose) {
                        listener.verbose("Creating class: " + newEntry.getNormalizedName());
                    }
                    if (jarIndex != null) {
                        jarIndex.addResource(newEntry.getName(), activeConfig.getDestFile().getName());
                    }
                    output.openEntry(newEntry);
                    output.write(classBytes);
                    outputBytes += classBytes.length;
                }
            }
        }

        if (jarIndex != null) {
            if (verbose) {
                listener.verbose("Adding: META-INF/INDEX.LIST");
            }
            output.openEntry(new ResourceEntry(JarIndex.INDEX_NAME));
            ByteArrayOutputStream indexOut = new ByteArrayOutputStream();
            jarIndex.write(indexOut);
            output.write(indexOut.toByteArray());
            indexOut.close();
        }

        ProtectionRegistry jarInfo = (ProtectionRegistry) get().attributes.get("jarInfo");
        if (activeConfig.isNoneMode() && !manifestPatched && jarInfo != null && !jarInfo.classes.isEmpty()) {
            if (verbose) {
                listener.verbose("Adding: META-INF/MANIFEST.MF");
            }
            output.openEntry(new ResourceEntry("META-INF/MANIFEST.MF"));
            ByteArrayOutputStream manifestOut = new ByteArrayOutputStream();
            if (watermarkManifest.getMainAttributes().get(Name.MANIFEST_VERSION) == null) {
                watermarkManifest.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
            }
            watermarkManifest.write(manifestOut);
            output.write(manifestOut.toByteArray());
            manifestOut.close();
        }

        sourceIterator.close();
        output.close();

        if (!activeConfig.isNoneMode()) {
            listener.info("[Signing " + activeConfig.getSrcFile().getName() + "]");
            SigningIdentity identity = StringerUtils.loadSigningIdentity(activeConfig);
            File signedTemp = File.createTempFile("signed", "jar");
            StringerUtils.signJar(identity, activeConfig.getDestFile(), signedTemp, watermarkManifest);
            FileUtils.deleteQuietly(activeConfig.getDestFile());
            FileUtils.moveFile(signedTemp, activeConfig.getDestFile());
        }

        Job permutationJob = (Job) plan.get("jarPermutation");
        if (permutationJob != null) {
            reorderArchiveEntries((ArrayList<ResourceEntry>) permutationJob.payload, activeConfig.isOptimize(), activeConfig.getDestFile(), activeConfig.getDestFile());
        }

        listener.info("[Finishing " + activeConfig.getSrcFile().getName() + "]");
        if (verbose) {
            listener.verbose("Total time: " + (System.currentTimeMillis() - startTime) + "ms");
        }
        listener.info("Total strings processed: " + get().processedStringCount);
        listener.info("Total classes processed: " + classesProcessed);
        listener.info("Total resources processed: " + resourcesProcessed);
        listener.info("Total bytes processed: " + inputBytes + ", ratio " + ((double) outputBytes / inputBytes));
    }

    public static void repairArchive(File sourceFile, File destFile) throws IOException {
        ZipFile sourceZip;
        try {
            sourceZip = new ZipFile(sourceFile);
        } catch (Exception ex) {
            throw new RuntimeException("Unsupported format of src file for the repair mode");
        }

        if (destFile.isDirectory()) {
            sourceZip.close();
            throw new RuntimeException("In the repair mode a jar file must be set as dest");
        }

        ArrayList<ResourceEntry> orderedEntries = new ArrayList<>();
        Enumeration<? extends ZipEntry> entries = sourceZip.entries();
        boolean sawExtra = false;

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getExtra() != null) {
                ResourceEntry resourceEntry = new ResourceEntry(entry.getName());
                resourceEntry.setExtra(entry.getExtra());
                orderedEntries.add(resourceEntry);
                sawExtra = true;
            } else if (sawExtra) {
                break;
            }
        }

        if (orderedEntries.isEmpty()) {
            sourceZip.close();
            throw new RuntimeException("In the src file there is no information to perform the repair");
        }

        try {
            sourceZip.close();
        } catch (Exception ignored) {
        }

        reorderArchiveEntries(orderedEntries, false, sourceFile, destFile);
    }

    private static void reorderArchiveEntries(ArrayList<ResourceEntry> priorityEntries, boolean optimize, File sourceFile, File destFile) throws IOException {
        ZipFile sourceZip = new ZipFile(sourceFile);
        ZipFile destZip = new ZipFile(destFile);
        HashMap<String, ZipEntry> remainingEntries = new HashMap<>();

        Enumeration<? extends ZipEntry> entries = destZip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            remainingEntries.put(entry.getName(), entry);
        }

        ZipResourcesOutput output = new ZipResourcesOutput(destFile, optimize);
        output.open();

        for (ResourceEntry priorityEntry : priorityEntries) {
            ZipEntry match = remainingEntries.get(priorityEntry.getName());
            if (match == null) {
                sourceZip.close();
                destZip.close();
                output.close();
                throw new RuntimeException("Internal error");
            }
            output.openEntry(priorityEntry);
            StringerUtils.copyStream(sourceZip.getInputStream(match), output);
            remainingEntries.remove(priorityEntry.getName());
        }

        for (ZipEntry leftover : remainingEntries.values()) {
            output.openEntry(new ResourceEntry(leftover.getName()));
            StringerUtils.copyStream(destZip.getInputStream(leftover), output);
        }

        sourceZip.close();
        destZip.close();
        output.close();
    }

    @SuppressWarnings("unchecked")
    private byte[] applyClassTransformations(InputStream inputStream, Job job) throws IOException {
        ClassReader reader = new ClassReader(inputStream);
        ArrayList<ClassTransformer> transformers = (ArrayList<ClassTransformer>) job.payload;
        ClassWriter writer = null;

        for (ClassTransformer transformer : transformers) {
            writer = new ClassWriter(transformer.getClassWriterFlags());
            ClassVisitor visitor = transformer.createVisitor(writer);
            reader.accept(visitor, 0);
            reader = new ClassReader(writer.toByteArray());
        }

        return writer.toByteArray();
    }

    private ProtectionRegistry analyzeSource(SourceIterator sourceIterator) throws Exception {
        WorkerListener listener = get().config.getListener();
        Config activeConfig = get().config;
        boolean verbose = activeConfig.isVerbose();
        ProtectionRegistry registry = new ProtectionRegistry();

        Properties proguardMap = new Properties();
        if (activeConfig.getProguardMapFile() != null) {
            try {
                proguardMap = StringerUtils.loadProguardMap(activeConfig.getProguardMapFile());
            } catch (Exception ex) {
                throw new Exception("Unable to parse the proguardMapFile: " + activeConfig.getProguardMapFile());
            }
        }

        ArrayList<String[]> injectPatterns = null;
        if (activeConfig.getInjectList() != null) {
            injectPatterns = new ArrayList<>();
            for (String pattern : activeConfig.getInjectList()) {
                injectPatterns.add(PatternMatchUtils.tokenizePath(pattern));
            }
        }

        Iterator<ResourceEntry> entries = sourceIterator.entries();
        while (entries.hasNext()) {
            ResourceEntry entry = entries.next();
            String entryName = entry.getName();
            if (entryName.endsWith(".class")) {
                if (entry.getNormalizedName().startsWith("com/javafx/main")) {
                    if (verbose) {
                        listener.verbose("    skipping JavaFX loader: " + entryName);
                    }
                    registry.javaFxApplication = true;
                    continue;
                }

                try (BufferedInputStream in = new BufferedInputStream(sourceIterator.openStream(entry))) {
                    ClassReader reader = new ClassReader(in);
                    String internalName = entryName.substring(0, entryName.indexOf(".class"));
                    if (verbose) {
                        listener.verbose("    class " + entryName + (proguardMap.containsKey(internalName) ? " (" + proguardMap.getProperty(internalName) + ")" : ""));
                    }

                    try {
                        ClassMetadataCollector collector = new ClassMetadataCollector(entryName, activeConfig);
                        try {
                            reader.accept(collector, 0);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            listener.verbose("        found corrupted frames, trying to skip them");
                            reader.accept(collector, ClassReader.SKIP_FRAMES);
                            collector.classInfo.hasCorruptedFrames = true;
                        }

                        if (injectPatterns != null) {
                            String matchName = collector.classInfo.name;
                            if (proguardMap.containsKey(matchName)) {
                                matchName = proguardMap.getProperty(matchName);
                            }
                            String[] tokenizedName = PatternMatchUtils.tokenizePath(matchName);
                            for (String[] pattern : injectPatterns) {
                                if (PatternMatchUtils.matchPath(pattern, tokenizedName, true)) {
                                    collector.classInfo.inInjectList = true;
                                }
                            }
                        }

                        registry.registerClass(collector.classInfo);
                    } catch (Throwable ex) {
                        throw new RuntimeException("Analysis of " + entryName + " class failed: " + ex.getMessage());
                    }
                }
            } else if ("META-INF/MANIFEST.MF".equalsIgnoreCase(entry.getNormalizedName())) {
                Manifest manifest = new Manifest();
                manifest.read(sourceIterator.openStream(entry));
                if (manifest.getMainAttributes().containsKey(new Name("JavaFX-Version"))) {
                    registry.javaFxApplication = true;
                }
            } else if (entry.getNormalizedName().startsWith("META-INF/") && entry.getNormalizedName().endsWith(".SF")) {
                registry.containsSignature = true;
            } else if (activeConfig.getResourceEncryption().isEnabled() && !entry.isDirectory()) {
                boolean isMetaInf = entry.getNormalizedName().startsWith("META-INF");
                if (verbose) {
                    listener.verbose("    resource " + entryName + (isMetaInf ? " (skipping)" : ""));
                }
                if (!isMetaInf) {
                    PropertyResourceBundle bundle = null;
                    if (entryName.endsWith(".properties")) {
                        bundle = new PropertyResourceBundle(sourceIterator.openStream(entry));
                    }
                    registry.registerResourceBundle(new ResourceBundleInfo(entryName, bundle));
                }
            }
        }

        return registry;
    }

    private String generateClassName(PackageProtectionInfo pkgInfo, String outerClassName, boolean allowInner) {
        ArrayList<String> candidates = pkgInfo.getDecoderHostCandidates();
        boolean useInnerNaming = candidates.size() >= 2 & allowInner;
        String baseName = outerClassName;
        int attempts = 0;

        if (baseName == null && useInnerNaming) {
            baseName = get().nameGenerator.pickRandom(candidates);
            while (baseName.contains("$")) {
                if (++attempts > 10) {
                    useInnerNaming = false;
                    break;
                }
                baseName = get().nameGenerator.pickRandom(candidates);
            }
        }

        attempts = 0;
        int nameLength = 3;
        if (baseName == null) {
            baseName = pkgInfo.getPackagePath() + get().internalNameGenerator.generateRandomIdentifier(3);
            while (pkgInfo.getClasses().containsKey(baseName)) {
                if (++attempts > 10) {
                    nameLength++;
                }
                baseName = pkgInfo.getPackagePath() + get().internalNameGenerator.generateRandomIdentifier(nameLength);
            }
            return baseName;
        }

        int counter = 0;
        String suffix = useInnerNaming
                ? get().nameGenerator.pickRandom(candidates).substring(pkgInfo.getPackagePath().length())
                : Integer.toString(counter++);
        String prefix = baseName + '$';
        String generated = prefix + suffix;

        for (int tries = 0; pkgInfo.getClasses().containsKey(generated); tries++) {
            if (tries > 10) {
                useInnerNaming = false;
            }
            generated = prefix + (useInnerNaming
                    ? get().nameGenerator.pickRandom(candidates).substring(pkgInfo.getPackagePath().length())
                    : Integer.toString(counter++));
        }

        return generated;
    }

    private Properties allocateRuntimeClass(
            ProtectionRegistry registry,
            PackageProtectionInfo pkgInfo,
            HashMap<String, Object> plan,
            boolean forceNewClass,
            int creatorKind,
            String superName,
            Properties methodTemplates,
            Properties fieldTemplates,
            int classFileVersion) {
        return this.allocateRuntimeClass(
                registry, pkgInfo, plan, forceNewClass, creatorKind, null, superName, null, methodTemplates, fieldTemplates, classFileVersion);
    }

    private Properties allocateRuntimeClass(
            ProtectionRegistry registry,
            PackageProtectionInfo pkgInfo,
            HashMap<String, Object> plan,
            boolean forceNewClass,
            int creatorKind,
            String superName,
            String explicitClassName,
            Properties methodTemplates,
            Properties fieldTemplates,
            int classFileVersion) {
        return this.allocateRuntimeClass(
                registry, pkgInfo, plan, forceNewClass, creatorKind, null, superName, explicitClassName, methodTemplates, fieldTemplates, classFileVersion);
    }

    private Properties allocateRuntimeClass(
            ProtectionRegistry registry,
            PackageProtectionInfo pkgInfo,
            HashMap<String, Object> plan,
            boolean forceNewClass,
            int creatorKind,
            Object creatorPayload,
            String superName,
            String explicitClassName,
            Properties methodTemplates,
            Properties fieldTemplates,
            int classFileVersion) {
        WorkerListener listener = get().config.getListener();
        Config activeConfig = get().config;
        boolean verbose = activeConfig.isVerbose();

        ArrayList<String> hostCandidates = pkgInfo.getDecoderHostCandidates();
        boolean createNew = hostCandidates.isEmpty() || forceNewClass;
        String targetClassName = explicitClassName != null
                ? explicitClassName
                : (!createNew ? get().nameGenerator.pickRandom(hostCandidates) : this.generateClassName(pkgInfo, null, true));

        if (verbose) {
            listener.verbose("    " + (createNew ? "new" : "existing") + " class for injects: " + targetClassName);
        }

        Properties generatedMappings = new Properties();
        ClassInfo classInfo;
        if (createNew) {
            classInfo = new ClassInfo(targetClassName);
            classInfo.isGeneratedClass = true;
            registry.registerClass(classInfo);
            plan.put(
                    pkgInfo.getPackagePath() + "." + targetClassName + ".#newClass",
                    new RuntimeClassTransformer(null, creatorKind, classFileVersion, targetClassName, superName, creatorPayload));
        } else {
            classInfo = pkgInfo.getClasses().get(targetClassName);
            classInfo.isDecoderHost = true;
        }

        Enumeration<?> methodKeys = methodTemplates.keys();
        while (methodKeys.hasMoreElements()) {
            String key = (String) methodKeys.nextElement();
            String descriptor = methodTemplates.getProperty(key);
            String generatedName = get().internalNameGenerator.generateMethodName(classInfo, descriptor, null);
            classInfo.declaredMethods.add(new ClassMemberInfo(0, generatedName, descriptor));
            generatedMappings.setProperty(key, generatedName);
            if (verbose) {
                listener.verbose("        adding method: " + generatedName + descriptor);
            }
        }

        Enumeration<?> fieldKeys = fieldTemplates.keys();
        while (fieldKeys.hasMoreElements()) {
            String key = (String) fieldKeys.nextElement();
            String descriptor = fieldTemplates.getProperty(key);
            String generatedName = get().internalNameGenerator.generateFieldName(classInfo, descriptor);
            classInfo.declaredFields.add(new ClassMemberInfo(0, generatedName, descriptor));
            generatedMappings.setProperty(key, generatedName);
            if (verbose) {
                listener.verbose("        adding field: " + generatedName);
            }
        }

        generatedMappings.setProperty("className", targetClassName);
        pkgInfo.evaluateProtection(activeConfig, registry);
        return generatedMappings;
    }

    private Job createRemapJob(
            Properties generatedMappings,
            String prototypeInternalName,
            Properties methodTemplates,
            Properties fieldTemplates,
            Properties globalRemapTable) {
        for (Object keyObj : generatedMappings.keySet()) {
            String key = (String) keyObj;
            if (fieldTemplates.containsKey(key)) {
                globalRemapTable.setProperty(prototypeInternalName + "." + key + "." + fieldTemplates.getProperty(key), generatedMappings.getProperty(key));
            } else if (methodTemplates.containsKey(key)) {
                globalRemapTable.setProperty(prototypeInternalName + "." + key + "." + methodTemplates.getProperty(key), generatedMappings.getProperty(key));
            }
        }

        String mappedClassName = generatedMappings.getProperty("className");
        globalRemapTable.setProperty(prototypeInternalName, mappedClassName);
        ArrayList<ClassTransformer> transformers = new ArrayList<>();
        transformers.add(new ClassNameRemappingTransformer(new ClassInfo(mappedClassName), globalRemapTable));
        return new Job(Job.TRANSFORM_CLASS, transformers);
    }

    @SuppressWarnings("unchecked")
    private HashMap<String, Object> buildProtectionPlan(SourceIterator sourceIterator, Watermark watermark) throws Exception {
        WorkerListener listener = get().config.getListener();
        Config activeConfig = get().config;
        boolean verbose = activeConfig.isVerbose();
        listener.info("[Analyzing " + activeConfig.getSrcFile().getName() + "]");

        HashMap<String, Object> plan = new HashMap<>();
        ProtectionRegistry registry = this.analyzeSource(sourceIterator);
        NameGenerator primaryNames = get().nameGenerator;
        NameGenerator internalNames = get().internalNameGenerator;

        if (verbose) {
            listener.verbose("Is it a JavaFx application: " + registry.javaFxApplication);
        }
        if (verbose && activeConfig.getHideAccess().isEnabled()) {
            listener.verbose("Classes to inject the security control: " + registry.classesWithMethodRefs + "/" + registry.classesWithoutMethodRefs);
        }
        if (verbose && activeConfig.getResourceEncryption().isEnabled() && activeConfig.getResourceEncryption().isGenerateStdWrappers()) {
            listener.verbose("Use ResourceBundle.getBundle(..): " + registry.usesResourceBundle);
            listener.verbose("Use Class/ClassLoader.getResource(..)/getResourceAsStream(..): " + registry.usesResourceStreams);
        }

        listener.info("Contains signature: " + registry.containsSignature);
        if (registry.containsSignature && activeConfig.isNoneMode()) {
            return null;
        }

        byte[] jarCheckSalt = null;
        boolean checkJarEnabled = activeConfig.getIntegrityControl().isEnabled()
                && activeConfig.getIntegrityControl().checkJar().isEnabled()
                && sourceIterator instanceof ZipSourceIterator;
        if (checkJarEnabled) {
            jarCheckSalt = new byte[2];
            get().secureRandom.nextBytes(jarCheckSalt);
        }

        Properties runtimeRemapTable = new Properties();
        PackageProtectionInfo runtimePackage = new PackageProtectionInfo("!");
        Properties lruCacheMappings = null;
        Properties intrinsicsMappings = null;

        if (!registry.classes.isEmpty() || activeConfig.isForceGenerateInternalLibPkg()) {
            String generatedPkg = internalNames.generateRandomIdentifier(3, true) + "/" + internalNames.generateRandomIdentifier(3, true) + "/";
            while (registry.packages.containsKey(generatedPkg)
                    || generatedPkg.endsWith("file/")
                    || generatedPkg.endsWith("ftp/")
                    || generatedPkg.endsWith("gopher/")
                    || generatedPkg.endsWith("http/")
                    || generatedPkg.endsWith("https/")
                    || generatedPkg.endsWith("jar/")
                    || generatedPkg.endsWith("mailto/")
                    || generatedPkg.endsWith("netdoc/")) {
                generatedPkg = internalNames.generateRandomIdentifier(3, true) + "/" + internalNames.generateRandomIdentifier(3, true) + "/";
            }

            runtimePackage = new PackageProtectionInfo(generatedPkg);
            String protocolName = runtimePackage.getPackagePath().split("/")[1];
            registry.packages.put(generatedPkg, runtimePackage);
            if (verbose) {
                listener.verbose("Stringer protocol name: " + protocolName + ", pkg: " + runtimePackage.getPackagePath());
            }

            if (activeConfig.getStringEncryption().isEqualsIntrinsicEnabled()) {
                intrinsicsMappings = this.allocateRuntimeClass(
                        registry, runtimePackage, plan, true, RuntimeClassTransformer.KIND_INTRINSICS,
                        "java/lang/Object", IntrinsicsCreator.METHOD_MAPPINGS, IntrinsicsCreator.FIELD_MAPPINGS, 49);
            }

            lruCacheMappings = this.allocateRuntimeClass(
                    registry, runtimePackage, plan, true, RuntimeClassTransformer.KIND_LRU_CACHE,
                    "java/lang/Object", LruCacheCreator.METHOD_MAPPINGS, LruCacheCreator.FIELD_MAPPINGS, 49);
            plan.put(
                    lruCacheMappings.getProperty("className") + ".classn",
                    this.createRemapJob(
                            lruCacheMappings,
                            "com/licel/stringer/LruCachePrototype",
                            LruCacheCreator.METHOD_MAPPINGS,
                            LruCacheCreator.FIELD_MAPPINGS,
                            runtimeRemapTable));
        }

        Collection<PackageProtectionInfo> allPackages = registry.packages.values();
        for (PackageProtectionInfo pkgInfo : allPackages) {
            pkgInfo.evaluateProtection(activeConfig, registry);
            if (pkgInfo.isSkipped() && !pkgInfo.getPackagePath().equals(runtimePackage.getPackagePath())) {
                if (verbose) {
                    listener.verbose("Skipping: " + pkgInfo.getPackagePath());
                }
                continue;
            }

            if (verbose) {
                listener.verbose("Processing package: " + pkgInfo.getPackagePath());
            }

            primaryNames.setMethodNameCandidates(pkgInfo.getMethodNameCandidates(""));
            primaryNames.setFieldNameCandidates(pkgInfo.getFieldNameCandidates(""));
            internalNames.setMethodNameCandidates(pkgInfo.getMethodNameCandidates(""));
            internalNames.setFieldNameCandidates(pkgInfo.getFieldNameCandidates(""));

            int decoderCount = pkgInfo.getProtectedClassCount() / 10 + 1;
            if (decoderCount > 5) {
                decoderCount = 5;
            }
            if (decoderCount == 1 && activeConfig.getHideAccess().isEnabled()) {
                decoderCount = 3;
            }

            CryptoContainer[] containers = new CryptoContainer[decoderCount];
            Properties[] decoderProps = new Properties[decoderCount];
            HashMap<String, Integer> decoderClassIndexMap = new HashMap<>();

            for (int i = 0; i < decoderCount; i++) {
                containers[i] = new CryptoContainer(get().secureRandom, watermark, false);
                String[] extraTypes = {"I", "B", "S", "Z"};
                Properties methodTemplates = new Properties();
                methodTemplates.putAll(StringDecoderInjector.METHOD_MAPPINGS);
                String extraSignature = extraTypes[get().secureRandom.nextInt(extraTypes.length)]
                        + extraTypes[get().secureRandom.nextInt(extraTypes.length)];
                methodTemplates.put("decryptString", "(Ljava/lang/Object;I" + extraSignature + ")Ljava/lang/String;");

                decoderProps[i] = this.allocateRuntimeClass(
                        registry, pkgInfo, plan, true, RuntimeClassTransformer.KIND_EMPTY_CLASS,
                        "java/lang/Object", methodTemplates, StringDecoderInjector.FIELD_MAPPINGS, 51);
                decoderClassIndexMap.put(decoderProps[i].getProperty("className"), i);
                decoderProps[i].put("extraDecryptStringSignature", extraSignature);
                decoderProps[i].put("lruCacheMapClassName", lruCacheMappings.getProperty("className"));
            }

            pkgInfo.setCryptoContainers(containers);

            if (pkgInfo.getPackagePath().equals(runtimePackage.getPackagePath())
                    && (activeConfig.getResourceEncryption().isGenerateStdWrappers() && (registry.usesResourceBundle || registry.usesResourceStreams)
                    || activeConfig.getResourceEncryption().isGenerateWebContaineWrappers()
                    || activeConfig.getStringEncryption().isEqualsIntrinsicEnabled())) {
                if (activeConfig.getResourceEncryption().isGenerateStdWrappers() && (registry.usesResourceBundle || registry.usesResourceStreams)
                        || activeConfig.getResourceEncryption().isGenerateWebContaineWrappers()) {
                    if (verbose) {
                        listener.verbose("Generating wrappers for encrypted resources");
                    }

                    runtimeRemapTable.setProperty("MAGIC", Integer.toString(get().cryptoContainer.getResourceMagicHeader()));
                    runtimeRemapTable.setProperty("AES1", Long.toString(get().cryptoContainer.getCurrentKeyHigh()));
                    runtimeRemapTable.setProperty("AES2", Long.toString(get().cryptoContainer.getKeyLow()));
                    runtimeRemapTable.setProperty("IV0", Integer.toString(get().cryptoContainer.getIvWords()[0]));
                    runtimeRemapTable.setProperty("IV1", Integer.toString(get().cryptoContainer.getIvWords()[1]));
                    runtimeRemapTable.setProperty("IV2", Integer.toString(get().cryptoContainer.getIvWords()[2]));
                    runtimeRemapTable.setProperty("IV3", Integer.toString(get().cryptoContainer.getIvWords()[3]));

                    Properties inputStreamProps = this.allocateRuntimeClass(
                            registry, runtimePackage, plan, true, RuntimeClassTransformer.KIND_INPUT_STREAM,
                            "java/io/FilterInputStream", StringerInputStreamCreator.METHOD_MAPPINGS, StringerInputStreamCreator.FIELD_MAPPINGS, 49);
                    Job inputStreamJob = this.createRemapJob(
                            inputStreamProps, "com/licel/stringer/StringerInputStreamPrototype",
                            StringerInputStreamCreator.METHOD_MAPPINGS, StringerInputStreamCreator.FIELD_MAPPINGS, runtimeRemapTable);
                    ((ArrayList<ClassTransformer>) inputStreamJob.payload).add(
                            new ClassEncryptionTransformer(this, false, new ClassInfo(inputStreamProps.getProperty("className")), containers, decoderProps, activeConfig));
                    plan.put(inputStreamProps.getProperty("className") + ".classn", inputStreamJob);

                    Properties inflaterProps = this.allocateRuntimeClass(
                            registry, runtimePackage, plan, true, RuntimeClassTransformer.KIND_INFLATER,
                            "java/util/zip/Inflater", StringerInflaterCreator.METHOD_MAPPINGS, StringerInflaterCreator.FIELD_MAPPINGS, 49);
                    plan.put(
                            inflaterProps.getProperty("className") + ".classn",
                            this.createRemapJob(
                                    inflaterProps, "com/licel/stringer/StringerInflaterPrototype",
                                    StringerInflaterCreator.METHOD_MAPPINGS, StringerInflaterCreator.FIELD_MAPPINGS, runtimeRemapTable));

                    Properties filterStreamProps = this.allocateRuntimeClass(
                            registry, runtimePackage, plan, true, RuntimeClassTransformer.KIND_FILTER_INPUT_STREAM,
                            "java/io/FilterInputStream", StringerFilterInputStreamCreator.METHOD_MAPPINGS, StringerFilterInputStreamCreator.FIELD_MAPPINGS, 49);
                    plan.put(
                            filterStreamProps.getProperty("className") + ".classn",
                            this.createRemapJob(
                                    filterStreamProps, "com/licel/stringer/StringerFilterInputStreamPrototype",
                                    StringerFilterInputStreamCreator.METHOD_MAPPINGS, StringerFilterInputStreamCreator.FIELD_MAPPINGS, runtimeRemapTable));

                    Properties factoryProps = this.allocateRuntimeClass(
                            registry, runtimePackage, plan, true, RuntimeClassTransformer.KIND_STREAM_HANDLER_FACTORY,
                            "java/lang/Object", StringerStreamHandlerFactoryCreator.METHOD_MAPPINGS, StringerStreamHandlerFactoryCreator.FIELD_MAPPINGS, 49);
                    plan.put(
                            factoryProps.getProperty("className") + ".classn",
                            this.createRemapJob(
                                    factoryProps, "com/licel/stringer/StringerStreamHandlerFactoryPrototype",
                                    StringerStreamHandlerFactoryCreator.METHOD_MAPPINGS, StringerStreamHandlerFactoryCreator.FIELD_MAPPINGS, runtimeRemapTable));

                    Properties urlHandlerProps = this.allocateRuntimeClass(
                            registry, runtimePackage, plan, true, RuntimeClassTransformer.KIND_URL_HANDLER,
                            "java/net/URLStreamHandler", runtimePackage.getPackagePath() + "Handler",
                            StringerURLHandlerCreator.METHOD_MAPPINGS, StringerURLHandlerCreator.FIELD_MAPPINGS, 49);
                    Job urlHandlerJob = this.createRemapJob(
                            urlHandlerProps, "com/licel/stringer/StringerURLHandlerPrototype",
                            StringerURLHandlerCreator.METHOD_MAPPINGS, StringerURLHandlerCreator.FIELD_MAPPINGS, runtimeRemapTable);
                    ((ArrayList<ClassTransformer>) urlHandlerJob.payload).add(
                            new ClassEncryptionTransformer(this, false, new ClassInfo(urlHandlerProps.getProperty("className")), containers, decoderProps, activeConfig));
                    plan.put(urlHandlerProps.getProperty("className") + ".classn", urlHandlerJob);

                    Properties urlConnProps = this.allocateRuntimeClass(
                            registry, runtimePackage, plan, true, RuntimeClassTransformer.KIND_URL_CONNECTION,
                            "java/net/URLConnection", StringerURLConnectionCreator.METHOD_MAPPINGS, StringerURLConnectionCreator.FIELD_MAPPINGS, 49);
                    plan.put(
                            urlConnProps.getProperty("className") + ".classn",
                            this.createRemapJob(
                                    urlConnProps, "com/licel/stringer/StringerURLConnectionPrototype",
                                    StringerURLConnectionCreator.METHOD_MAPPINGS, StringerURLConnectionCreator.FIELD_MAPPINGS, runtimeRemapTable));
                }

                if (activeConfig.getResourceEncryption().isGenerateWebContaineWrappers()) {
                    WebXmlCompiler webXmlCompiler = (WebXmlCompiler) get().attributes.get("webXmlInfo");
                    if (webXmlCompiler != null) {
                        Properties catalinaProps = this.allocateRuntimeClass(
                                registry, runtimePackage, plan, true, RuntimeClassTransformer.KIND_CATALINA_CONTEXT_WRAPPER,
                                "org/apache/catalina/core/ApplicationContext", null,
                                CatalinaApplicationContextWrapperCreator.METHOD_MAPPINGS, CatalinaApplicationContextWrapperCreator.FIELD_MAPPINGS, 50);
                        Job catalinaJob = this.createRemapJob(
                                catalinaProps, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper",
                                CatalinaApplicationContextWrapperCreator.METHOD_MAPPINGS, CatalinaApplicationContextWrapperCreator.FIELD_MAPPINGS, runtimeRemapTable);
                        ((ArrayList<ClassTransformer>) catalinaJob.payload).add(
                                new ClassEncryptionTransformer(this, false, new ClassInfo(catalinaProps.getProperty("className")), containers, decoderProps, activeConfig));
                        plan.put(catalinaProps.getProperty("className") + ".classn", catalinaJob);

                        Properties listenerProps = this.allocateRuntimeClass(
                                registry, runtimePackage, plan, true, RuntimeClassTransformer.KIND_WEB_CONTEXT_LOADER_LISTENER,
                                webXmlCompiler.getWebApplication(), "java/lang/Object", null,
                                WebContextLoaderListenerCreator.METHOD_MAPPINGS, WebContextLoaderListenerCreator.FIELD_MAPPINGS, 50);
                        Job listenerJob = this.createRemapJob(
                                listenerProps, "com/licel/stringer/webapp/tomcat/ContextLoaderListener",
                                WebContextLoaderListenerCreator.METHOD_MAPPINGS, WebContextLoaderListenerCreator.FIELD_MAPPINGS, runtimeRemapTable);
                        ((ArrayList<ClassTransformer>) listenerJob.payload).add(
                                new ClassEncryptionTransformer(this, false, new ClassInfo(listenerProps.getProperty("className")), containers, decoderProps, activeConfig));
                        plan.put(listenerProps.getProperty("className") + ".classn", listenerJob);
                        webXmlCompiler.setContextLoaderListenerClass(listenerProps.getProperty("className").replace('/', '.'));

                        Properties classLoaderProps = this.allocateRuntimeClass(
                                registry, runtimePackage, plan, true, RuntimeClassTransformer.KIND_WEB_CONTEXT_CLASS_LOADER,
                                runtimeRemapTable, "java/lang/Object", null,
                                WebContextClassLoaderCreator.METHOD_MAPPINGS, WebContextClassLoaderCreator.FIELD_MAPPINGS, 50);
                        Job classLoaderJob = this.createRemapJob(
                                classLoaderProps, "com/licel/stringer/webapp/tomcat/CustomContextClassLoader",
                                WebContextClassLoaderCreator.METHOD_MAPPINGS, WebContextClassLoaderCreator.FIELD_MAPPINGS, runtimeRemapTable);
                        ((ArrayList<ClassTransformer>) classLoaderJob.payload).add(
                                new ClassEncryptionTransformer(this, false, new ClassInfo(classLoaderProps.getProperty("className")), containers, decoderProps, activeConfig));
                        plan.put(classLoaderProps.getProperty("className") + ".classn", classLoaderJob);
                    }
                }

                if (activeConfig.getStringEncryption().isEqualsIntrinsicEnabled()) {
                    plan.put(
                            intrinsicsMappings.getProperty("className") + ".classn",
                            this.createRemapJob(
                                    intrinsicsMappings, "com/licel/stringer/IntrinsicsPrototype",
                                    IntrinsicsCreator.METHOD_MAPPINGS, IntrinsicsCreator.FIELD_MAPPINGS, runtimeRemapTable));
                }
            }

            Properties reflectionMappings = new Properties();
            String reflectionClassName = this.generateClassName(pkgInfo, null, false);
            ClassInfo reflectionClassInfo = new ClassInfo(reflectionClassName);
            reflectionClassInfo.classFileVersion = 49;
            reflectionClassInfo.superName = "java/lang/Object";
            reflectionClassInfo.isGeneratedClass = true;
            reflectionClassInfo.hiddenMembers = new HashMap<>(65536);
            reflectionClassInfo.hiddenClassIndices = new HashMap<>();
            pkgInfo.addClass(reflectionClassInfo);
            reflectionMappings.setProperty("reflection.className", reflectionClassName);
            if (verbose) {
                listener.verbose("    New reflection impl class: " + reflectionClassName);
            }

            Enumeration<?> reflMethodKeys = ReflectionClassCreator.METHOD_MAPPINGS.keys();
            while (reflMethodKeys.hasMoreElements()) {
                String key = (String) reflMethodKeys.nextElement();
                String descriptor = ReflectionClassCreator.METHOD_MAPPINGS.getProperty(key);
                String generatedName = primaryNames.generateMethodName(reflectionClassInfo, descriptor, null);
                reflectionClassInfo.declaredMethods.add(new ClassMemberInfo(0, generatedName, descriptor));
                reflectionMappings.setProperty("reflection." + key, generatedName);
                if (verbose) {
                    listener.verbose("        adding method: " + generatedName + descriptor);
                }
            }

            Enumeration<?> reflFieldKeys = ReflectionClassCreator.FIELD_MAPPINGS.keys();
            while (reflFieldKeys.hasMoreElements()) {
                String key = (String) reflFieldKeys.nextElement();
                String descriptor = ReflectionClassCreator.FIELD_MAPPINGS.getProperty(key);
                String generatedName = primaryNames.generateFieldName(reflectionClassInfo, descriptor);
                reflectionClassInfo.declaredFields.add(new ClassMemberInfo(0, generatedName, descriptor));
                reflectionMappings.setProperty("reflection." + key, generatedName);
                if (verbose) {
                    listener.verbose("        add field " + generatedName + descriptor);
                }
            }

            int userClassCount = 0;
            int hideAccessClassCount = 0;
            for (ClassInfo classInfo : pkgInfo.getClasses().values()) {
                if (!classInfo.isGeneratedClass) {
                    userClassCount++;
                }
                if (classInfo.isModuleInfo) {
                    continue;
                }

                ArrayList<ClassTransformer> pipeline = new ArrayList<>();
                if (activeConfig.isOptimize()) {
                    pipeline.add(new ClassOptimizationTransformer(classInfo));
                }
                if (classInfo.moveStringFields) {
                    pipeline.add(new FieldInitializationTransformer(registry.hasStringEncryptionAnnotations, classInfo));
                }
                if (activeConfig.getResourceEncryption().isEnabled()
                        && classInfo.usesResourceStreams
                        && activeConfig.getResourceEncryption().isGenerateStdWrappers()) {
                    if (verbose) {
                        listener.verbose("Class is used for resource encryption's methods: " + classInfo.name);
                    }
                    pipeline.add(new ResourceReflectionTransformer(classInfo, runtimeRemapTable));
                }
                if (classInfo.hideAccessEnabled) {
                    if (verbose) {
                        listener.verbose("Class to protect (" + classInfo.classFileVersion + "): " + classInfo.name);
                    }
                    pipeline.add(new ClassProtectionTransformer(activeConfig, classInfo, reflectionClassInfo, reflectionMappings, containers[0]));
                    if (classInfo.hasHideAccessTargets) {
                        hideAccessClassCount++;
                    }
                }
                if (decoderClassIndexMap.containsKey(classInfo.name)) {
                    int decoderIndex = decoderClassIndexMap.get(classInfo.name);
                    long expiration = activeConfig.getTrialExpirationTimestampSeconds();
                    pipeline.add(new StringDecoderTransformer(
                            containers[decoderIndex],
                            classInfo.name,
                            decoderProps[decoderIndex],
                            activeConfig,
                            expiration,
                            classInfo,
                            jarCheckSalt,
                            get().integrityKey0,
                            get().integrityKey1));
                }
                if (activeConfig.getStringEncryption().isEqualsIntrinsicEnabled() && !classInfo.isGeneratedClass) {
                    pipeline.add(new IntrinsicsTransformer(classInfo, intrinsicsMappings));
                }
                if (classInfo.stringEncryptionEnabled) {
                    if (verbose) {
                        listener.verbose("Class to encrypt strings in: " + classInfo.name);
                    }
                    pipeline.add(new ClassEncryptionTransformer(this, registry.hasStringEncryptionAnnotations, classInfo, containers, decoderProps, activeConfig));
                }
                if (classInfo.annotatedInsecure) {
                    if (verbose) {
                        listener.verbose("Class " + classInfo.name + " has @insecure annotation/s.");
                    }
                    pipeline.add(new ClassCleanupTransformer(classInfo));
                }

                if (!pipeline.isEmpty()) {
                    String jobKey = classInfo.name + ".class" + (classInfo.isGeneratedClass ? "n" : "");
                    Job existingJob = (Job) plan.get(jobKey);
                    if (existingJob == null) {
                        existingJob = new Job(Job.TRANSFORM_CLASS, pipeline);
                    } else if (existingJob.type == Job.TRANSFORM_CLASS) {
                        ((ArrayList<ClassTransformer>) existingJob.payload).addAll(pipeline);
                    }
                    plan.put(jobKey, existingJob);
                }
            }

            if (hideAccessClassCount > 0) {
                Object[] reflectionPayload = new Object[]{reflectionMappings, reflectionClassInfo};
                plan.put(
                        pkgInfo.getPackagePath() + "." + reflectionClassName + ".#newClass",
                        new RuntimeClassTransformer(containers[0], RuntimeClassTransformer.KIND_REFLECTION_CLASS, 49, reflectionClassName, "java/lang/Object", reflectionPayload));
                ArrayList<ClassTransformer> reflectionPipeline = new ArrayList<>();
                reflectionPipeline.add(new ClassEncryptionTransformer(this, false, reflectionClassInfo, containers, decoderProps, activeConfig));
                plan.put(reflectionClassName + ".classn", new Job(Job.TRANSFORM_CLASS, reflectionPipeline));
            }

            if (userClassCount == 0 && !pkgInfo.getPackagePath().equals(runtimePackage.getPackagePath())) {
                for (Properties decoderProp : decoderProps) {
                    plan.remove(pkgInfo.getPackagePath() + "." + decoderProp.getProperty("className") + ".#newClass");
                }
            }
        }

        for (PackageProtectionInfo pkgInfo : allPackages) {
            if (activeConfig.getResourceEncryption().isEnabled()) {
                if (verbose) {
                    listener.verbose("Processing resources in package: " + pkgInfo.getPackagePath());
                }
                for (ResourceBundleInfo bundleInfo : pkgInfo.getResourceBundles().values()) {
                    if (!activeConfig.getResourceEncryption().match(bundleInfo.name)) {
                        if (verbose) {
                            listener.verbose("skip: " + bundleInfo.name);
                        }
                        continue;
                    }
                    if (verbose) {
                        listener.verbose("Resource to encrypt: " + bundleInfo.name);
                    }
                    plan.put(bundleInfo.name, new Job(Job.ENCRYPT_RESOURCE, bundleInfo));

                    if (registry.usesResourceBundle && bundleInfo.name.endsWith(".properties")) {
                        String bundleClassName = bundleInfo.name.substring(0, bundleInfo.name.indexOf(".properties"));
                        ClassInfo bundleClassInfo = new ClassInfo(bundleClassName);
                        bundleClassInfo.isGeneratedClass = true;
                        pkgInfo.addClass(bundleClassInfo);
                        Object[] bundlePayload = new Object[]{
                                bundleInfo.resourceBundle,
                                runtimeRemapTable.getProperty("com/licel/stringer/StringerInputStreamPrototype"),
                                runtimeRemapTable.getProperty(
                                        "com/licel/stringer/StringerInputStreamPrototype.decryptStringWithoutCheckCallCtx.(Ljava/lang/Object;)Ljava/lang/String;")
                        };
                        plan.put(
                                pkgInfo.getPackagePath() + "." + bundleClassName + ".#newClass",
                                new RuntimeClassTransformer(
                                        get().cryptoContainer,
                                        RuntimeClassTransformer.KIND_RESOURCE_BUNDLE,
                                        49,
                                        bundleClassName,
                                        "java/util/ListResourceBundle",
                                        bundlePayload));
                    }
                }
            }
        }

        if (checkJarEnabled) {
            int[] permutation = StringerUtils.generateRandomPermutation(get().secureRandom, registry.classes.size());
            ArrayList<ResourceEntry> permutedEntries = new ArrayList<>();
            ByteArrayOutputStream hashInput = new ByteArrayOutputStream();
            Object[] classNames = registry.classes.keySet().toArray();

            for (int index : permutation) {
                byte[] extra = new byte[12];
                String entryName = classNames[index] + ".class";
                hashInput.write(entryName.getBytes(StandardCharsets.UTF_8));
                get().secureRandom.nextBytes(extra);
                extra[0] = jarCheckSalt[0];
                extra[1] = jarCheckSalt[1];
                extra[2] = 8;
                extra[3] = 0;
                hashInput.write(extra);
                ResourceEntry entry = new ResourceEntry(entryName);
                entry.setExtra(extra);
                permutedEntries.add(entry);
            }

            long integrityHash = Math.abs(SipHash.hash(get().integrityKey0, get().integrityKey1, hashInput.toByteArray()));
            for (PackageProtectionInfo pkgInfo : allPackages) {
                if (pkgInfo.getCryptoContainers() != null) {
                    for (CryptoContainer container : pkgInfo.getCryptoContainers()) {
                        container.setIntegrityHash(integrityHash);
                    }
                }
            }
            plan.put("jarPermutation", new Job(Job.TRANSFORM_WEB_XML, permutedEntries));
        }

        get().attributes.put("jarInfo", registry);
        return plan;
    }
}
