package com.licel.stringer.runtime;

import com.licel.stringer.model.ClassInfo;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class IntrinsicsCreator extends ClassVisitor implements Opcodes {
    public static final Properties METHOD_MAPPINGS = new Properties();
    public static final Properties FIELD_MAPPINGS = new Properties();
    private final ClassInfo classInfo;

    public IntrinsicsCreator(ClassVisitor classVisitor, ClassInfo classInfo) {
        super(Opcodes.ASM9, classVisitor);
        this.classInfo = classInfo;
        this.generate();
    }

    public void generate() {
        this.cv.visit(
                this.classInfo.classFileVersion,
                ACC_PUBLIC | ACC_SUPER,
                "com/licel/stringer/IntrinsicsPrototype",
                null,
                "java/lang/Object",
                null);

        MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
                "equalsIntrinsics",
                "(Ljava/lang/Object;Ljava/lang/Object;)Z",
                null,
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        Label label0 = new Label();
        mv.visitJumpInsn(IF_ACMPEQ, label0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(INSTANCEOF, "java/lang/String");
        Label label1 = new Label();
        mv.visitJumpInsn(IFEQ, label1);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(INSTANCEOF, "java/lang/String");
        mv.visitJumpInsn(IFEQ, label1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z", false);
        mv.visitJumpInsn(IFEQ, label1);
        mv.visitLabel(label0);
        mv.visitInsn(ICONST_1);
        Label label2 = new Label();
        mv.visitJumpInsn(GOTO, label2);
        mv.visitLabel(label1);
        mv.visitInsn(ICONST_0);
        mv.visitLabel(label2);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        this.cv.visitEnd();
    }

    static {
        METHOD_MAPPINGS.put("equalsIntrinsics", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
    }
}
