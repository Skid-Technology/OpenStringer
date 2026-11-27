package com.licel.stringer.transform;

import com.licel.stringer.model.MovedFieldInfo;
import java.util.HashMap;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class FieldInitializationMethodVisitor extends MethodVisitor implements Opcodes {
    private final ClassFieldMover fieldMover;
    private final HashMap<String, MovedFieldInfo> movedFields;
    private final String ownerClassName;
    private final boolean staticInitializer;

    public FieldInitializationMethodVisitor(
            ClassFieldMover fieldMover,
            MethodVisitor methodVisitor,
            String ownerClassName,
            HashMap<String, MovedFieldInfo> movedFields,
            boolean staticInitializer) {
        super(Opcodes.ASM9, methodVisitor);
        this.fieldMover = fieldMover;
        this.movedFields = movedFields;
        this.ownerClassName = ownerClassName;
        this.staticInitializer = staticInitializer;
    }

    @Override
    public void visitCode() {
        this.mv.visitCode();
        if (this.staticInitializer) {
            for (MovedFieldInfo field : this.movedFields.values()) {
                this.mv.visitLdcInsn(field.initialValue);
                this.mv.visitFieldInsn(Opcodes.PUTSTATIC, this.ownerClassName, field.name, "Ljava/lang/String;");
            }
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        if (!this.staticInitializer
                && (opcode & Opcodes.INVOKESPECIAL) == Opcodes.INVOKESPECIAL
                && "java/lang/Object".equals(owner)
                && "<init>".equals(name)
                && "()V".equals(descriptor)) {
            for (MovedFieldInfo field : this.movedFields.values()) {
                if (field.initialValue != null) {
                    this.mv.visitVarInsn(Opcodes.ALOAD, 0);
                    this.mv.visitLdcInsn(field.initialValue);
                    this.mv.visitFieldInsn(Opcodes.PUTFIELD, this.ownerClassName, field.name, "Ljava/lang/String;");
                }
            }
        }
    }
}
