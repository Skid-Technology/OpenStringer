package com.licel.stringer.transform;

import com.licel.stringer.model.ClassInfo;
import org.objectweb.asm.ClassVisitor;

public class ClassOptimizationTransformer extends ClassTransformer {
    private final ClassInfo classInfo;

    public ClassOptimizationTransformer(ClassInfo classInfo) {
        this.classInfo = classInfo;
    }

    @Override
    public ClassVisitor createVisitor(ClassVisitor classVisitor) {
        return new ClassOptimizer(classVisitor);
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
