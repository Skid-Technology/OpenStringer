package com.licel.stringer.transform;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodDebugInfoRemover extends MethodVisitor implements Opcodes {
    public MethodDebugInfoRemover(MethodVisitor methodVisitor) {
        super(Opcodes.ASM9, methodVisitor);
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
    }

    @Override
    public void visitLineNumber(int line, Label start) {
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        this.mv.visitAttribute(attribute);
    }
}
