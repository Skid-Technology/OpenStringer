package com.licel.stringer.runtime;

import com.licel.stringer.model.ClassInfo;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class StringerFilterInputStreamCreator extends ClassVisitor implements Opcodes {
    public static final Properties METHOD_MAPPINGS = new Properties();
    public static final Properties FIELD_MAPPINGS = new Properties();
    private final ClassInfo classInfo;

    public StringerFilterInputStreamCreator(ClassVisitor classVisitor, ClassInfo classInfo) {
        super(Opcodes.ASM9, classVisitor);
        this.classInfo = classInfo;
        this.generate();
    }

    public void generate() {
        this.cv.visit(
                this.classInfo.classFileVersion,
                ACC_PUBLIC | ACC_SUPER,
                "com/licel/stringer/StringerFilterInputStreamPrototype",
                null,
                this.classInfo.superName,
                null);

        FieldVisitor fv = this.cv.visitField(0, "parent", "Lcom/licel/stringer/StringerInputStreamPrototype;", null, null);
        fv.visitEnd();
        fv = this.cv.visitField(ACC_PRIVATE, "singleByteBuf", "[B", null, null);
        fv.visitEnd();

        MethodVisitor mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "<init>",
                "(Ljava/io/InputStream;Lcom/licel/stringer/StringerInputStreamPrototype;)V",
                null,
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, "java/io/FilterInputStream", "<init>", "(Ljava/io/InputStream;)V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ICONST_1);
        mv.visitIntInsn(NEWARRAY, T_BYTE);
        mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/StringerFilterInputStreamPrototype", "singleByteBuf", "[B");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitFieldInsn(
                PUTFIELD,
                "com/licel/stringer/StringerFilterInputStreamPrototype",
                "parent",
                "Lcom/licel/stringer/StringerInputStreamPrototype;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC | ACC_SYNCHRONIZED,
                "read",
                "()I",
                null,
                new String[]{"java/io/IOException"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/StringerFilterInputStreamPrototype", "singleByteBuf", "[B");
        mv.visitInsn(ICONST_0);
        mv.visitInsn(ICONST_1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/licel/stringer/StringerFilterInputStreamPrototype", "read", "([BII)I", false);
        mv.visitInsn(ICONST_M1);
        Label label0 = new Label();
        mv.visitJumpInsn(IF_ICMPNE, label0);
        mv.visitInsn(ICONST_M1);
        Label label1 = new Label();
        mv.visitJumpInsn(GOTO, label1);
        mv.visitLabel(label0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/StringerFilterInputStreamPrototype", "singleByteBuf", "[B");
        mv.visitInsn(ICONST_0);
        mv.visitInsn(BALOAD);
        mv.visitIntInsn(SIPUSH, 255);
        mv.visitInsn(IAND);
        mv.visitLabel(label1);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(4, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "markSupported", "()Z", null, null);
        mv.visitCode();
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "read", "([B)I", null, new String[]{"java/io/IOException"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/licel/stringer/StringerFilterInputStreamPrototype", "read", "([BII)I", false);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(4, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "read", "([BII)I", null, new String[]{"java/io/IOException"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitVarInsn(ILOAD, 3);
        mv.visitMethodInsn(INVOKESPECIAL, "java/io/FilterInputStream", "read", "([BII)I", false);
        mv.visitVarInsn(ISTORE, 3);
        mv.visitVarInsn(ILOAD, 3);
        Label label2 = new Label();
        mv.visitJumpInsn(IFLE, label2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(
                GETFIELD,
                "com/licel/stringer/StringerFilterInputStreamPrototype",
                "parent",
                "Lcom/licel/stringer/StringerInputStreamPrototype;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitVarInsn(ILOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/licel/stringer/StringerInputStreamPrototype", "decrypt", "([BII)V", false);
        mv.visitLabel(label2);
        mv.visitVarInsn(ILOAD, 3);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(4, 4);
        mv.visitEnd();

        this.cv.visitEnd();
    }

    static {
        FIELD_MAPPINGS.put("parent", "Lcom/licel/stringer/StringerInputStreamPrototype;");
        FIELD_MAPPINGS.put("singleByteBuf", "[B");
    }
}
