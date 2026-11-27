package com.licel.stringer.transform;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassOptimizer extends ClassVisitor implements Opcodes {
    public ClassOptimizer(ClassVisitor classVisitor) {
        super(Opcodes.ASM9, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = this.cv.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodDebugInfoRemover(mv);
    }

    @Override
    public void visitSource(String source, String debug) {
    }

    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        this.cv.visitAttribute(attribute);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
    }
}
