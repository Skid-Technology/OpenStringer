package com.licel.stringer.transform;

import com.licel.stringer.analysis.ResourceUsageClassVisitor;
import com.licel.stringer.io.DirectoryResourcesOutput;
import com.licel.stringer.io.DirectorySourceIterator;
import com.licel.stringer.io.ResourceEntry;
import com.licel.stringer.io.ResourcesOutput;
import com.licel.stringer.io.SourceIterator;
import com.licel.stringer.io.ZipResourcesOutput;
import com.licel.stringer.io.ZipSourceIterator;
import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.model.ProtectionRegistry;
import com.licel.stringer.util.StringerUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class ResourceCallsWrapper {
    private final HashMap<String, Object> cache = new HashMap<>();

    public static boolean hasResourceCalls(SourceIterator sourceIterator) throws IOException {
        Iterator<ResourceEntry> entries = sourceIterator.entries();
        while (entries.hasNext()) {
            ResourceEntry entry = entries.next();
            if (entry.getName().endsWith(".class")) {
                try (BufferedInputStream in = new BufferedInputStream(sourceIterator.openStream(entry))) {
                    ClassReader reader = new ClassReader(in);
                    ResourceUsageClassVisitor visitor = new ResourceUsageClassVisitor(entry.getName());
                    reader.accept(visitor, 0);
                    if (visitor.usesResourceCalls) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void wrapArchive(File sourceFile, File destFile, ProtectionRegistry registry, Properties runtimeMappings) throws IOException {
        SourceIterator sourceIterator;
        if (sourceFile.isFile()) {
            sourceIterator = new ZipSourceIterator(sourceFile);
            if (destFile.exists()) {
                File parent = destFile.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                destFile.createNewFile();
            }
        } else {
            sourceIterator = new DirectorySourceIterator(sourceFile);
            if (destFile.exists()) {
                destFile.mkdirs();
            }
        }

        ResourcesOutput output = destFile.isFile()
                ? new ZipResourcesOutput(destFile, false)
                : new DirectoryResourcesOutput(sourceFile, destFile);
        output.open();

        Iterator<ResourceEntry> entries = sourceIterator.entries();
        while (entries.hasNext()) {
            ResourceEntry entry = entries.next();
            String normalized = entry.getNormalizedName();
            String className = normalized.contains(".class")
                    ? normalized.substring(0, normalized.indexOf(".class"))
                    : normalized;
            ClassInfo classInfo = registry.classes.get(className);
            if (entry.isDirectory()) {
                output.openEntry(new ResourceEntry(entry.getName(), true));
            } else if (classInfo != null && classInfo.usesResourceStreams) {
                output.openEntry(new ResourceEntry(entry.getName()));
                try (BufferedInputStream in = new BufferedInputStream(sourceIterator.openStream(entry))) {
                    ClassReader reader = new ClassReader(in);
                    ClassWriter writer = new ClassWriter(0);
                    ClassResourceReflection reflectionVisitor = new ClassResourceReflection(writer, classInfo, runtimeMappings);
                    reader.accept(reflectionVisitor, 0);
                    output.write(writer.toByteArray());
                }
            } else if (!(output instanceof DirectoryResourcesOutput) || !((DirectoryResourcesOutput) output).overwriteSource) {
                output.openEntry(new ResourceEntry(entry.getName()));
                StringerUtils.copyStream(sourceIterator.openStream(entry), output);
            }
        }

        sourceIterator.close();
        output.close();
    }
}
