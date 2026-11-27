package com.licel.stringer.transform;

import com.licel.stringer.model.ClassInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class FieldInitializationTransformer extends ClassTransformer {
    private final ClassInfo classInfo;
    private final boolean annotationsOnly;

    public FieldInitializationTransformer(boolean annotationsOnly, ClassInfo classInfo) {
        this.classInfo = classInfo;
        this.annotationsOnly = annotationsOnly;
    }

    @Override
    public ClassVisitor createVisitor(ClassVisitor classVisitor) {
        return new ClassFieldMover(classVisitor, this.annotationsOnly, this.classInfo);
    }

    @Override
    public int getClassWriterFlags() {
        return ClassWriter.COMPUTE_MAXS;
    }

    @Override
    public ClassInfo getClassInfo() {
        return this.classInfo;
    }
}
