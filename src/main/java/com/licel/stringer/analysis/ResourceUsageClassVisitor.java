package com.licel.stringer.analysis;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ResourceUsageClassVisitor extends ClassVisitor implements Opcodes {
    public boolean usesResourceCalls = false;

    public ResourceUsageClassVisitor(String className) {
        super(Opcodes.ASM9);
    }

    public boolean usesResourceCalls() {
        return this.usesResourceCalls;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new ResourceUsageMethodVisitor(this);
    }
}
