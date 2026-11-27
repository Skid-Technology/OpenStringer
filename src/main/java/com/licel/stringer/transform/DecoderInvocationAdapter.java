package com.licel.stringer.transform;

import com.licel.stringer.runtime.StringDecoderInjector;
import com.licel.stringer.util.StringerUtils;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class DecoderInvocationAdapter extends MethodVisitor {
    private final StringDecoderInjector decoderInjector;

    public DecoderInvocationAdapter(StringDecoderInjector decoderInjector, MethodVisitor mv) {
        super(Opcodes.ASM9, mv);
        this.decoderInjector = decoderInjector;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (this.decoderInjector.useIndy) {
            boolean isStatic = opcode == Opcodes.INVOKESTATIC;
            String indyDescriptor = isStatic ? descriptor : descriptor.replace("(", "(Ljava/lang/Object;");
            Type returnType = Type.getReturnType(indyDescriptor);
            Type[] argumentTypes = Type.getArgumentTypes(indyDescriptor);

            for (int i = 0; i < argumentTypes.length; i++) {
                argumentTypes[i] = this.eraseType(argumentTypes[i]);
            }

            indyDescriptor = Type.getMethodDescriptor(this.eraseReturnType(returnType), argumentTypes);
            String callSiteKey = owner + "." + name + "." + descriptor;
            String callSiteName = (String) this.decoderInjector.bootstrapCache.get(callSiteKey);
            if (callSiteName == null) {
                callSiteName = StringerUtils.toAlphabeticUnsignedString(
                        this.decoderInjector.cryptoContainer.getSecureRandom().nextLong(), 5);

                while (this.decoderInjector.bootstrapCache.containsKey(callSiteName)) {
                    callSiteName = StringerUtils.toAlphabeticUnsignedString(
                            this.decoderInjector.cryptoContainer.getSecureRandom().nextLong(), 5);
                }

                this.decoderInjector.bootstrapCache.put(callSiteName, callSiteKey);
            }

            switch (opcode) {
                case Opcodes.INVOKEVIRTUAL:
                case Opcodes.INVOKESTATIC:
                case Opcodes.INVOKEINTERFACE:
                    super.visitInvokeDynamicInsn(
                            callSiteName,
                            indyDescriptor,
                            this.decoderInjector.bootstrapHandle,
                            opcode,
                            owner.replaceAll("/", "."),
                            name,
                            descriptor);
                    if (returnType.getSort() == Type.ARRAY) {
                        super.visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
                    }
                    break;
                case Opcodes.INVOKESPECIAL:
                default:
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        } else {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }

    private Type eraseType(Type type) {
        switch (type.getSort()) {
            case Type.OBJECT:
                return Type.getType(Object.class);
            default:
                return type;
        }
    }

    private Type eraseReturnType(Type type) {
        switch (type.getSort()) {
            case Type.ARRAY:
                return Type.getType(Object.class);
            default:
                return type;
        }
    }
}
