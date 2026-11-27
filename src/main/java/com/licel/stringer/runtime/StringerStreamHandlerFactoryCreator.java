package com.licel.stringer.runtime;

import com.licel.stringer.model.ClassInfo;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class StringerStreamHandlerFactoryCreator extends ClassVisitor implements Opcodes {
    public static final Properties METHOD_MAPPINGS = new Properties();
    public static final Properties FIELD_MAPPINGS = new Properties();
    private final ClassInfo classInfo;

    public StringerStreamHandlerFactoryCreator(ClassVisitor classVisitor, ClassInfo classInfo) {
        super(Opcodes.ASM9, classVisitor);
        this.classInfo = classInfo;
        this.generate();
    }

    public void generate() {
        this.cv.visit(
                this.classInfo.classFileVersion,
                ACC_SUPER,
                "com/licel/stringer/StringerStreamHandlerFactoryPrototype",
                null,
                "java/lang/Object",
                new String[]{"java/net/URLStreamHandlerFactory"});

        FieldVisitor fv = this.cv.visitField(
                ACC_PRIVATE | ACC_FINAL,
                "protocolHandlers",
                "Ljava/util/Map;",
                "Ljava/util/Map<Ljava/lang/String;Ljava/net/URLStreamHandler;>;",
                null);
        fv.visitEnd();

        MethodVisitor mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "<init>",
                "(Ljava/lang/String;Ljava/net/URLStreamHandler;)V",
                null,
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, "java/util/HashMap");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
        mv.visitFieldInsn(
                PUTFIELD,
                "com/licel/stringer/StringerStreamHandlerFactoryPrototype",
                "protocolHandlers",
                "Ljava/util/Map;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(
                GETFIELD,
                "com/licel/stringer/StringerStreamHandlerFactoryPrototype",
                "protocolHandlers",
                "Ljava/util/Map;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(
                INVOKEINTERFACE,
                "java/util/Map",
                "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                true);
        mv.visitInsn(POP);
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, "java/util/HashMap");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
        mv.visitFieldInsn(
                PUTFIELD,
                "com/licel/stringer/StringerStreamHandlerFactoryPrototype",
                "protocolHandlers",
                "Ljava/util/Map;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "addHandler",
                "(Ljava/lang/String;Ljava/net/URLStreamHandler;)V",
                null,
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(
                GETFIELD,
                "com/licel/stringer/StringerStreamHandlerFactoryPrototype",
                "protocolHandlers",
                "Ljava/util/Map;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(
                INVOKEINTERFACE,
                "java/util/Map",
                "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                true);
        mv.visitInsn(POP);
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "createURLStreamHandler",
                "(Ljava/lang/String;)Ljava/net/URLStreamHandler;",
                null,
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(
                GETFIELD,
                "com/licel/stringer/StringerStreamHandlerFactoryPrototype",
                "protocolHandlers",
                "Ljava/util/Map;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(
                INVOKEINTERFACE,
                "java/util/Map",
                "get",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                true);
        mv.visitTypeInsn(CHECKCAST, "java/net/URLStreamHandler");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        this.cv.visitEnd();
    }

    static {
        FIELD_MAPPINGS.put("protocolHandlers", "Ljava/util/Map;");
    }
}
