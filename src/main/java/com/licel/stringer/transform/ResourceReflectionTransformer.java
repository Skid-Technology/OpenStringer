package com.licel.stringer.transform;

import com.licel.stringer.model.ClassInfo;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;

public class ResourceReflectionTransformer extends ClassTransformer {
    private final ClassInfo classInfo;
    private final Properties runtimeMappings;

    public ResourceReflectionTransformer(ClassInfo classInfo, Properties runtimeMappings) {
        this.classInfo = classInfo;
        this.runtimeMappings = runtimeMappings;
    }

    @Override
    public ClassVisitor createVisitor(ClassVisitor classVisitor) {
        return new ClassResourceReflection(classVisitor, this.classInfo, this.runtimeMappings);
    }

    @Override
    public int getClassWriterFlags() {
        return 0;
    }

    @Override
    public ClassInfo getClassInfo() {
        return this.classInfo;
    }
}
