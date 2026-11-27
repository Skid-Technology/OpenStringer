package com.licel.stringer.runtime;

import com.licel.stringer.model.ClassInfo;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class StringerURLHandlerCreator extends ClassVisitor implements Opcodes {
    public static final Properties METHOD_MAPPINGS = new Properties();
    public static final Properties FIELD_MAPPINGS = new Properties();
    private final ClassInfo classInfo;
    public String protocol;

    public StringerURLHandlerCreator(ClassVisitor classVisitor, ClassInfo classInfo) {
        super(Opcodes.ASM9, classVisitor);
        this.classInfo = classInfo;
        String[] parts = classInfo.name.split("/");
        this.protocol = parts[1];
        this.generate();
    }

    public void generate() {
        this.cv.visit(
                this.classInfo.classFileVersion,
                ACC_PUBLIC | ACC_SUPER,
                "com/licel/stringer/StringerURLHandlerPrototype",
                null,
                "java/net/URLStreamHandler",
                null);

        MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/net/URLStreamHandler", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PROTECTED,
                "openConnection",
                "(Ljava/net/URL;)Ljava/net/URLConnection;",
                null,
                new String[]{"java/io/IOException"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/net/URL", "getProtocol", "()Ljava/lang/String;", false);
        mv.visitLdcInsn(this.protocol);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        Label label0 = new Label();
        mv.visitJumpInsn(IFEQ, label0);
        mv.visitTypeInsn(NEW, "java/net/URL");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/net/URL", "toString", "()Ljava/lang/String;", false);
        mv.visitIntInsn(BIPUSH, this.protocol.length() + 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/net/URL", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitTypeInsn(NEW, "com/licel/stringer/StringerURLConnectionPrototype");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(
                INVOKESPECIAL,
                "com/licel/stringer/StringerURLConnectionPrototype",
                "<init>",
                "(Ljava/net/URL;)V",
                false);
        mv.visitInsn(ARETURN);
        mv.visitLabel(label0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/net/URL", "openConnection", "()Ljava/net/URLConnection;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(4, 3);
        mv.visitEnd();

        this.cv.visitEnd();
    }
}
