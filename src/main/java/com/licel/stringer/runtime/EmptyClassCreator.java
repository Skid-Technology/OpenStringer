package com.licel.stringer.runtime;

import com.licel.stringer.model.ClassInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class EmptyClassCreator extends ClassVisitor implements Opcodes {
    public EmptyClassCreator(ClassVisitor classVisitor, ClassInfo classInfo) {
        super(Opcodes.ASM9, classVisitor);
        classVisitor.visit(
                classInfo.classFileVersion,
                ACC_PUBLIC | ACC_SUPER,
                classInfo.name,
                null,
                classInfo.superName,
                null);
        classVisitor.visitEnd();
    }
}
