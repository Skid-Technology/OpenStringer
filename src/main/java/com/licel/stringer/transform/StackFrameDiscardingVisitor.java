package com.licel.stringer.transform;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class StackFrameDiscardingVisitor extends MethodVisitor {
    public StackFrameDiscardingVisitor(MethodVisitor methodVisitor) {
        super(Opcodes.ASM9, methodVisitor);
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
    }
}
