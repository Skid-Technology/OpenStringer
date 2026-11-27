package com.licel.stringer.runtime;

import com.licel.stringer.model.ClassInfo;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class StringerInflaterCreator extends ClassVisitor implements Opcodes {
    public static final Properties METHOD_MAPPINGS = new Properties();
    public static final Properties FIELD_MAPPINGS = new Properties();
    private final ClassInfo classInfo;

    public StringerInflaterCreator(ClassVisitor classVisitor, ClassInfo classInfo) {
        super(Opcodes.ASM9, classVisitor);
        this.classInfo = classInfo;
        this.generate();
    }

    public void generate() {
        this.cv.visit(
                this.classInfo.classFileVersion,
                ACC_PUBLIC | ACC_SUPER,
                "com/licel/stringer/StringerInflaterPrototype",
                null,
                this.classInfo.superName,
                null);

        FieldVisitor fv = this.cv.visitField(0, "parent", "Lcom/licel/stringer/StringerInputStreamPrototype;", null, null);
        fv.visitEnd();

        MethodVisitor mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "<init>",
                "(Lcom/licel/stringer/StringerInputStreamPrototype;)V",
                null,
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ICONST_0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/zip/Inflater", "<init>", "(Z)V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(
                PUTFIELD,
                "com/licel/stringer/StringerInflaterPrototype",
                "parent",
                "Lcom/licel/stringer/StringerInputStreamPrototype;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "setInput", "([BII)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(
                GETFIELD,
                "com/licel/stringer/StringerInflaterPrototype",
                "parent",
                "Lcom/licel/stringer/StringerInputStreamPrototype;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitVarInsn(ILOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "com/licel/stringer/StringerInputStreamPrototype", "decrypt", "([BII)V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitVarInsn(ILOAD, 3);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/zip/Inflater", "setInput", "([BII)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(4, 4);
        mv.visitEnd();

        this.cv.visitEnd();
    }

    static {
        FIELD_MAPPINGS.put("parent", "Lcom/licel/stringer/StringerInputStreamPrototype;");
    }
}
