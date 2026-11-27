package com.licel.stringer.transform;

import com.licel.stringer.runtime.StringDecoderInjector;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class DecoderPrototypeRemapper extends MethodVisitor {
    private final StringDecoderInjector decoderInjector;
    private final String prototypeClassName;
    private final int[] ivWords;

    public DecoderPrototypeRemapper(StringDecoderInjector decoderInjector, MethodVisitor mv) {
        super(Opcodes.ASM9, mv);
        this.decoderInjector = decoderInjector;
        this.prototypeClassName = "com/licel/stringer/StringDecoderPrototype";
        this.ivWords = this.decoderInjector.cryptoContainer.getIntegrityIvWords();
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        if (this.decoderInjector.isEmitFrames()) {
            super.visitFrame(type, numLocal, local, numStack, stack);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (owner.equals(this.prototypeClassName)) {
            owner = this.decoderInjector.targetClassName;
            name = this.decoderInjector.methodMappings.getProperty(name);
        }

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        if (owner.equals(this.prototypeClassName)) {
            super.visitFieldInsn(
                    opcode,
                    this.decoderInjector.targetClassName,
                    this.decoderInjector.fieldMappings.getProperty(name),
                    descriptor);
        } else {
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (type.equals(this.prototypeClassName)) {
            super.visitTypeInsn(opcode, this.decoderInjector.targetClassName);
        } else {
            super.visitTypeInsn(opcode, type);
        }
    }

    @Override
    public void visitLdcInsn(Object value) {
        Object constant = value;
        if (constant instanceof Integer) {
            switch ((Integer) constant) {
                case 12345678:
                    constant = this.ivWords[0];
                    break;
                case 12345679:
                    constant = this.ivWords[1];
                    break;
                case 12345680:
                    constant = this.ivWords[2];
                    break;
                case 12345681:
                    constant = this.ivWords[3];
                    break;
                default:
                    break;
            }
        }

        if (constant instanceof Long && (Long) constant == Long.MAX_VALUE) {
            constant = this.decoderInjector.trialExpirationMillis > 0L
                    ? this.decoderInjector.trialExpirationMillis
                    : Long.MAX_VALUE;
        }

        super.visitLdcInsn(constant);
    }
}
