package com.licel.stringer.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class DelegatingClassVisitor extends ClassVisitor {
    public DelegatingClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM9, classVisitor);
    }
}
