package com.licel.stringer.transform;

import com.licel.stringer.model.ClassInfo;
import org.objectweb.asm.ClassVisitor;

public abstract class ClassTransformer {
    public static final int MIN_STACK_FRAME_VERSION = 49;

    public abstract ClassVisitor createVisitor(ClassVisitor classVisitor);

    public abstract int getClassWriterFlags();

    public abstract ClassInfo getClassInfo();
}
