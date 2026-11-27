package com.licel.stringer.model;

import com.licel.stringer.Config;
import com.licel.stringer.Worker;
import com.licel.stringer.crypto.CryptoContainer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import org.objectweb.asm.Type;

public class PackageProtectionInfo {
    private final String packagePath;
    private final HashMap<String, ClassInfo> classes;
    private int protectedClassCount;
    private final HashMap<String, ResourceBundleInfo> resourceBundles;
    private long totalStringCount;
    private boolean noProtectedClasses = true;
    private boolean hasInjectCandidates = false;
    private boolean allInterfaces = true;
    private CryptoContainer[] cryptoContainers;

    public PackageProtectionInfo(String packagePath) {
        this.packagePath = packagePath;
        this.classes = new HashMap<>();
        this.protectedClassCount = 0;
        this.resourceBundles = new HashMap<>();
        this.totalStringCount = 0L;
    }

    public String getPackagePath() {
        return this.packagePath;
    }

    public HashMap<String, ClassInfo> getClasses() {
        return this.classes;
    }

    public HashMap<String, ResourceBundleInfo> getResourceBundles() {
        return this.resourceBundles;
    }

    public int getProtectedClassCount() {
        return this.protectedClassCount;
    }

    public CryptoContainer[] getCryptoContainers() {
        return this.cryptoContainers;
    }

    public void setCryptoContainers(CryptoContainer[] cryptoContainers) {
        this.cryptoContainers = cryptoContainers;
    }

    public void addClass(ClassInfo classInfo) {
        this.classes.put(classInfo.name, classInfo);
        this.totalStringCount += classInfo.stringLiterals.size() + classInfo.stringFieldInitialValues.values().size();
        if (classInfo.inInjectList) {
            this.hasInjectCandidates = true;
        }
        if (!classInfo.isInterface) {
            this.allInterfaces = false;
        }
    }

    public void addResourceBundle(ResourceBundleInfo bundleInfo) {
        this.resourceBundles.put(bundleInfo.name, bundleInfo);
    }

    public long getTotalStringCount() {
        return this.totalStringCount;
    }

    public int countNonGeneratedClasses() {
        int count = 0;
        for (ClassInfo info : this.classes.values()) {
            if (!info.isGeneratedClass) {
                count++;
            }
        }
        return count;
    }

    public ArrayList<String> getDecoderHostCandidates() {
        ArrayList<String> candidates = new ArrayList<>();
        for (String className : this.classes.keySet()) {
            ClassInfo info = this.classes.get(className);
            if (className.indexOf('$') < 0
                    && !info.hasNativeMethods
                    && !info.isInterface
                    && !info.isSynthetic
                    && !className.endsWith("/R")
                    && !className.endsWith("/BuildConfig")
                    && !className.endsWith("/Manifest")
                    && this.hasSafeStaticInit(info)
                    && !info.isDecoderHost
                    && !info.isGeneratedClass
                    && info.canHostDecoder
                    && !className.endsWith("package-info")) {
                if (this.hasInjectCandidates) {
                    if (info.inInjectList) {
                        candidates.add(className);
                    }
                } else {
                    candidates.add(className);
                }
            }
        }
        return candidates;
    }

    public boolean hasSafeStaticInit(ClassInfo classInfo) {
        if (!classInfo.hasStaticInitializer) {
            return true;
        }
        for (int i = 0; i < classInfo.referencedTypes.size(); i++) {
            String typeName = classInfo.referencedTypes.get(i);
            while (typeName.startsWith("[")) {
                typeName = typeName.substring(1);
            }
            if (typeName.startsWith("L")) {
                typeName = typeName.substring(1);
            }
            if (typeName.endsWith(";")) {
                typeName = typeName.substring(0, typeName.length() - 1);
            }
            ClassInfo referenced = this.classes.get(typeName);
            if (referenced != null
                    && !referenced.name.equals(classInfo.name)
                    && referenced.stringLiterals.size() + referenced.stringFieldInitialValues.values().size() > 0) {
                return false;
            }
        }
        return true;
    }

    public ArrayList<ClassMemberInfo> getMethodNameCandidates(String excludeClassName) {
        ArrayList<ClassMemberInfo> result = new ArrayList<>();
        for (String className : this.classes.keySet()) {
            if (!className.equalsIgnoreCase(excludeClassName)) {
                ClassInfo info = this.classes.get(className);
                for (ClassMemberInfo member : info.declaredMethods) {
                    if (member.name.indexOf('<') < 0) {
                        result.add(member);
                    }
                }
            }
        }
        return result;
    }

    public ArrayList<ClassMemberInfo> getFieldNameCandidates(String excludeClassName) {
        ArrayList<ClassMemberInfo> result = new ArrayList<>();
        for (String className : this.classes.keySet()) {
            if (!className.equalsIgnoreCase(excludeClassName)) {
                ClassInfo info = this.classes.get(className);
                for (ClassMemberInfo member : info.declaredFields) {
                    if (member.name.indexOf('$') < 0 && !member.name.equalsIgnoreCase("serialVersionUID")) {
                        result.add(member);
                    }
                }
            }
        }
        return result;
    }

    public int computeWeight() {
        int depth = this.packagePath.split("\\/").length - 1;
        return depth * 3 + this.classes.size();
    }

    public void evaluateProtection(Config config, ProtectionRegistry registry) {
        for (ClassInfo classInfo : this.classes.values()) {
            classInfo.hideAccessEnabled = this.shouldApplyHideAccess(config, registry, classInfo);
            classInfo.stringEncryptionEnabled = this.shouldApplyStringEncryption(config, registry, classInfo);
            classInfo.moveStringFields = this.shouldMoveStringFields(registry, classInfo);
            if (classInfo.isModuleInfo) {
                classInfo.canHostDecoder = false;
                this.noProtectedClasses = true;
            } else {
                this.resolveHierarchy(classInfo, registry);
                if (classInfo.isInterface || classInfo.isEnum) {
                    classInfo.canHostDecoder = false;
                }
                if (classInfo.annotatedInsecure) {
                    classInfo.canHostDecoder = false;
                }
                if (classInfo.hideAccessEnabled || classInfo.stringEncryptionEnabled) {
                    this.protectedClassCount++;
                    this.noProtectedClasses = false;
                }
            }
        }
    }

    public void resolveHierarchy(ClassInfo classInfo, ProtectionRegistry registry) {
        HashMap<String, ClassInfo> jdkCache = Worker.get().jdkClassCache;
        ClassInfo current = classInfo;

        while (!"java/lang/Object".equals(current.superName)) {
            if (current.superName == null) {
                classInfo.canHostDecoder = false;
                return;
            }
            if (registry.classes.containsKey(current.superName)) {
                current = registry.classes.get(current.superName);
            } else if (jdkCache.containsKey(current.superName)) {
                current = jdkCache.get(current.superName);
            } else if (!current.name.startsWith("java/") && !current.name.startsWith("javax/")) {
                current = null;
            } else {
                if (current.name.startsWith("java/lang/annotation/Annotation")) {
                    classInfo.canHostDecoder = false;
                    return;
                }
                current = this.loadJdkClassInfo(current.superName);
                if (current != null) {
                    jdkCache.put(current.name, current);
                }
            }

            if (current == null) {
                classInfo.canHostDecoder = false;
                return;
            }
        }

        classInfo.inheritedFields.addAll(current.declaredFields);
        classInfo.inheritedMethods.addAll(current.declaredMethods);
    }

    ClassInfo loadJdkClassInfo(String internalName) {
        ClassInfo info = new ClassInfo(internalName);
        try {
            Class<?> clazz = Class.forName(internalName.replace("/", "."));
            info.superName = Type.getType(clazz.getSuperclass()).getInternalName();

            for (Method method : clazz.getDeclaredMethods()) {
                Type methodType = Type.getType(method);
                info.declaredMethods.add(new ClassMemberInfo(0, internalName, method.getName(), methodType.getDescriptor()));
            }
            for (Field field : clazz.getDeclaredFields()) {
                Type fieldType = Type.getType(field.getType());
                info.declaredFields.add(new ClassMemberInfo(0, internalName, field.getName(), fieldType.getDescriptor()));
            }
            return info;
        } catch (Exception ex) {
            return null;
        }
    }

    public boolean isSkipped() {
        return this.noProtectedClasses || this.allInterfaces;
    }

    private boolean shouldMoveStringFields(ProtectionRegistry registry, ClassInfo classInfo) {
        if (classInfo.annotatedInsecure) {
            return false;
        }
        return classInfo.hasSecuredField || !classInfo.stringFieldInitialValues.isEmpty();
    }

    private boolean shouldApplyHideAccess(Config config, ProtectionRegistry registry, ClassInfo classInfo) {
        if (!config.getHideAccess().isEnabled()) {
            return false;
        }
        boolean matched = false;
        if (config.getHideAccess().isAnnotations()) {
            if (registry.hasHideAccessTargets) {
                matched = classInfo.hasHideAccessTargets;
            }
        } else {
            matched = classInfo.hasHideAccessTargets;
        }
        return matched && !classInfo.referencedMethods.isEmpty() && !classInfo.isInterface;
    }

    private boolean shouldApplyStringEncryption(Config config, ProtectionRegistry registry, ClassInfo classInfo) {
        if (!config.getStringEncryption().isEnabled()) {
            return false;
        }
        if (config.getStringEncryption().isAnnotations()) {
            return classInfo.hasStringEncryptionAnnotation;
        }
        return config.getStringEncryption().match(classInfo)
                && classInfo.stringLiterals.size() + classInfo.stringFieldInitialValues.values().size() > 0;
    }
}
