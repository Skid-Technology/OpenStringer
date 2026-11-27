package com.licel.stringer.runtime;

import com.licel.stringer.model.ClassInfo;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class StringerURLConnectionCreator extends ClassVisitor implements Opcodes {
    public static final Properties METHOD_MAPPINGS = new Properties();
    public static final Properties FIELD_MAPPINGS = new Properties();
    private final ClassInfo classInfo;

    public StringerURLConnectionCreator(ClassVisitor classVisitor, ClassInfo classInfo) {
        super(Opcodes.ASM9, classVisitor);
        this.classInfo = classInfo;
        this.generate();
    }

    public void generate() {
        this.cv.visit(
                this.classInfo.classFileVersion,
                ACC_PUBLIC | ACC_SUPER,
                "com/licel/stringer/StringerURLConnectionPrototype",
                null,
                "java/net/URLConnection",
                null);

        FieldVisitor fv = this.cv.visitField(0, "parentConnecton", "Ljava/net/URLConnection;", null, null);
        fv.visitEnd();

        MethodVisitor mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "<init>",
                "(Ljava/net/URL;)V",
                null,
                new String[]{"java/io/IOException"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, "java/net/URLConnection", "<init>", "(Ljava/net/URL;)V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/net/URL", "openConnection", "()Ljava/net/URLConnection;", false);
        mv.visitFieldInsn(
                PUTFIELD,
                "com/licel/stringer/StringerURLConnectionPrototype",
                "parentConnecton",
                "Ljava/net/URLConnection;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "connect", "()V", null, new String[]{"java/io/IOException"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(
                GETFIELD,
                "com/licel/stringer/StringerURLConnectionPrototype",
                "parentConnecton",
                "Ljava/net/URLConnection;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/net/URLConnection", "connect", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "getInputStream",
                "()Ljava/io/InputStream;",
                null,
                new String[]{"java/io/IOException"});
        mv.visitCode();
        mv.visitTypeInsn(NEW, "com/licel/stringer/StringerInputStreamPrototype");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(
                GETFIELD,
                "com/licel/stringer/StringerURLConnectionPrototype",
                "parentConnecton",
                "Ljava/net/URLConnection;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/net/URLConnection", "getInputStream", "()Ljava/io/InputStream;", false);
        mv.visitMethodInsn(
                INVOKESPECIAL,
                "com/licel/stringer/StringerInputStreamPrototype",
                "<init>",
                "(Ljava/io/InputStream;)V",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 1);
        mv.visitEnd();

        this.cv.visitEnd();
    }

    static {
        FIELD_MAPPINGS.put("parentConnecton", "Ljava/net/URLConnection;");
    }
}
