package com.licel.stringer.analysis;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ResourceUsageMethodVisitor extends MethodVisitor implements Opcodes {
    private final ResourceUsageClassVisitor classVisitor;

    public ResourceUsageMethodVisitor(ResourceUsageClassVisitor classVisitor) {
        super(Opcodes.ASM9);
        this.classVisitor = classVisitor;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if ((name.equals("getResource") || name.equals("getResourceAsStream"))
                && (owner.equals("java/lang/Class") || owner.equals("java/lang/ClassLoader") || owner.equals("javax/servlet/ServletContext"))) {
            this.classVisitor.usesResourceCalls = true;
        }
        if ((name.equals("openConnection") || name.equals("openStream")) && owner.equals("java/net/URL")) {
            this.classVisitor.usesResourceCalls = true;
        }
        if (name.equals("getResource") && (owner.equals("java/lang/Class") || owner.equals("java/lang/ClassLoader"))) {
            this.classVisitor.usesResourceCalls = true;
        }
    }
}
