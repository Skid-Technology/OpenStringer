package com.licel.stringer.transform;

import com.licel.stringer.model.MemberInfo;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class MethodIndyProtector extends MethodVisitor {
    private static final String[] PRIMITIVE_ARRAY_TYPES = {"[Z", "[B", "[C", "[D", "[F", "[I", "[J", "[S"};

    private boolean hideEnabled = true;
    private final ClassProtector classProtector;

    public MethodIndyProtector(MethodVisitor methodVisitor, ClassProtector classProtector, String methodName, String descriptor, boolean unused) {
        super(Opcodes.ASM9, methodVisitor);
        this.classProtector = classProtector;
    }

    private boolean isPrimitiveArrayOwner(String owner) {
        for (String primitiveArray : PRIMITIVE_ARRAY_TYPES) {
            if (owner.equalsIgnoreCase(primitiveArray)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        boolean isStatic = opcode == Opcodes.INVOKESTATIC;
        boolean isSpecial = opcode == Opcodes.INVOKESPECIAL;
        boolean isInterfaceCall = opcode == Opcodes.INVOKEINTERFACE;

        if (!owner.equalsIgnoreCase(this.classProtector.className) && !isSpecial && !isInterfaceCall && this.hideEnabled) {
            if (!this.isPrimitiveArrayOwner(owner) && !owner.startsWith("[L")) {
                this.classProtector.indyCallCount++;
                String indyDesc = isStatic ? descriptor : descriptor.replace("(", "(Ljava/lang/Object;");
                Type returnType = Type.getReturnType(indyDesc);
                Type[] argTypes = Type.getArgumentTypes(indyDesc);
                for (int i = 0; i < argTypes.length; i++) {
                    argTypes[i] = this.eraseObjectType(argTypes[i]);
                }
                indyDesc = Type.getMethodDescriptor(this.eraseArrayReturnType(returnType), argTypes);

                MemberInfo memberInfo = this.classProtector.registerMethod(opcode, owner, name, descriptor, this.classProtector.cryptoContainer);
                int encodedId = this.classProtector.cryptoContainer.encodeMemberId(memberInfo.getReferenceIndex(), memberInfo.referenceMask);
                this.mv.visitInvokeDynamicInsn(Integer.toString(encodedId, 32), indyDesc, this.classProtector.bootstrapMethodHandle);
                if (returnType.getSort() == Type.ARRAY) {
                    this.mv.visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
                }
            } else {
                this.mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        } else {
            this.mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if ("Lcom/licel/stringer/annotations/secured;".equals(descriptor)
                || "Lcom/licel/stringer/annotation/internal/secured;".equals(descriptor)
                || "Lcom/stringer/annotations/HideAccess;".equals(descriptor)) {
            this.hideEnabled = true;
        } else if ("Lcom/licel/stringer/annotations/insecure;".equals(descriptor)) {
            this.hideEnabled = false;
        }
        return this.mv.visitAnnotation(descriptor, visible);
    }

    private Type eraseObjectType(Type type) {
        return type.getSort() == Type.OBJECT ? Type.getType(Object.class) : type;
    }

    private Type eraseArrayReturnType(Type type) {
        return type.getSort() == Type.ARRAY ? Type.getType(Object.class) : type;
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        super.visitLocalVariable(name, this.eraseObjectType(Type.getType(descriptor)).getDescriptor(), null, start, end, index);
    }
}
