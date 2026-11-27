package com.licel.stringer.transform;

import java.util.Properties;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class EqualityIntrinsicsInjector extends MethodVisitor implements Opcodes {
    private final Properties intrinsicsProperties;

    public EqualityIntrinsicsInjector(MethodVisitor methodVisitor, Properties intrinsicsProperties) {
        super(Opcodes.ASM9, methodVisitor);
        this.intrinsicsProperties = intrinsicsProperties;
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if (opcode == Opcodes.IF_ACMPEQ) {
            this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.intrinsicsProperties.getProperty("className"),
                    this.intrinsicsProperties.getProperty("equalsIntrinsics"),
                    "(Ljava/lang/Object;Ljava/lang/Object;)Z",
                    false);
            this.mv.visitJumpInsn(Opcodes.IFNE, label);
        } else if (opcode == Opcodes.IF_ACMPNE) {
            this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.intrinsicsProperties.getProperty("className"),
                    this.intrinsicsProperties.getProperty("equalsIntrinsics"),
                    "(Ljava/lang/Object;Ljava/lang/Object;)Z",
                    false);
            this.mv.visitJumpInsn(Opcodes.IFEQ, label);
        } else {
            this.mv.visitJumpInsn(opcode, label);
        }
    }
}
