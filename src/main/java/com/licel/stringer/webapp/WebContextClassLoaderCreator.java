package com.licel.stringer.webapp;

import com.licel.stringer.model.ClassInfo;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class WebContextClassLoaderCreator extends ClassVisitor implements Opcodes {
    public static final Properties METHOD_MAPPINGS = new Properties();
    public static final Properties FIELD_MAPPINGS = new Properties();
    public ClassInfo classInfo;
    public Properties remappingProperties;
    public String urlProtocol;

    public WebContextClassLoaderCreator(ClassVisitor classVisitor, ClassInfo classInfo, Properties remappingProperties) {
        super(Opcodes.ASM9, classVisitor);
        this.classInfo = classInfo;
        this.remappingProperties = remappingProperties;
        String[] parts = remappingProperties.getProperty("com/licel/stringer/StringerInputStreamPrototype").split("/");
        this.urlProtocol = parts[1];
        this.generate();
    }

    public void generate() {
        this.cv.visit(V1_7, ACC_PUBLIC | ACC_SUPER, "com/licel/stringer/webapp/tomcat/CustomContextClassLoader", null, "java/lang/ClassLoader", null);

        MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/ClassLoader;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassLoader", "<init>", "(Ljava/lang/ClassLoader;)V", false);
        mv.visitTypeInsn(NEW, "com/licel/stringer/StringerStreamHandlerFactoryPrototype");
        mv.visitInsn(DUP);
        mv.visitLdcInsn(this.urlProtocol);
        mv.visitTypeInsn(NEW, "com/licel/stringer/StringerURLHandlerPrototype");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/StringerURLHandlerPrototype", "<init>", "()V", false);
        mv.visitMethodInsn(
                INVOKESPECIAL,
                "com/licel/stringer/StringerStreamHandlerFactoryPrototype",
                "<init>",
                "(Ljava/lang/String;Ljava/net/URLStreamHandler;)V",
                false);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitMethodInsn(
                INVOKESTATIC,
                "org/apache/catalina/webresources/TomcatURLStreamHandlerFactory",
                "getInstance",
                "()Lorg/apache/catalina/webresources/TomcatURLStreamHandlerFactory;",
                false);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "org/apache/catalina/webresources/TomcatURLStreamHandlerFactory",
                "addUserFactory",
                "(Ljava/net/URLStreamHandlerFactory;)V",
                false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(5, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getResource", "(Ljava/lang/String;)Ljava/net/URL;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassLoader", "getResource", "(Ljava/lang/String;)Ljava/net/URL;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/webapp/tomcat/CustomContextClassLoader", "wrapURL", "(Ljava/net/URL;)Ljava/net/URL;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "getResources",
                "(Ljava/lang/String;)Ljava/util/Enumeration;",
                "(Ljava/lang/String;)Ljava/util/Enumeration<Ljava/net/URL;>;",
                new String[]{"java/io/IOException"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassLoader", "getResources", "(Ljava/lang/String;)Ljava/util/Enumeration;", false);
        mv.visitMethodInsn(
                INVOKESPECIAL,
                "com/licel/stringer/webapp/tomcat/CustomContextClassLoader",
                "wrapURLs",
                "(Ljava/util/Enumeration;)Ljava/util/Enumeration;",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PROTECTED,
                "findResources",
                "(Ljava/lang/String;)Ljava/util/Enumeration;",
                "(Ljava/lang/String;)Ljava/util/Enumeration<Ljava/net/URL;>;",
                new String[]{"java/io/IOException"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassLoader", "findResources", "(Ljava/lang/String;)Ljava/util/Enumeration;", false);
        mv.visitMethodInsn(
                INVOKESPECIAL,
                "com/licel/stringer/webapp/tomcat/CustomContextClassLoader",
                "wrapURLs",
                "(Ljava/util/Enumeration;)Ljava/util/Enumeration;",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PROTECTED, "findResource", "(Ljava/lang/String;)Ljava/net/URL;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassLoader", "findResource", "(Ljava/lang/String;)Ljava/net/URL;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/webapp/tomcat/CustomContextClassLoader", "wrapURL", "(Ljava/net/URL;)Ljava/net/URL;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", null, null);
        mv.visitCode();
        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label catchHandler = new Label();
        mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, "java/io/IOException");
        mv.visitLabel(tryStart);
        mv.visitTypeInsn(NEW, "com/licel/stringer/StringerInputStreamPrototype");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassLoader", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/StringerInputStreamPrototype", "<init>", "(Ljava/io/InputStream;)V", false);
        mv.visitLabel(tryEnd);
        mv.visitInsn(ARETURN);
        mv.visitLabel(catchHandler);
        mv.visitFrame(F_SAME1, 0, null, 1, new Object[]{"java/io/IOException"});
        mv.visitInsn(POP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassLoader", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(4, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PRIVATE,
                "wrapURLs",
                "(Ljava/util/Enumeration;)Ljava/util/Enumeration;",
                "(Ljava/util/Enumeration<Ljava/net/URL;>;)Ljava/util/Enumeration<Ljava/net/URL;>;",
                null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, "java/util/ArrayList");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
        mv.visitVarInsn(ASTORE, 2);
        Label loopCondition = new Label();
        mv.visitJumpInsn(GOTO, loopCondition);
        Label loopBody = new Label();
        mv.visitLabel(loopBody);
        mv.visitFrame(F_APPEND, 1, new Object[]{"java/util/ArrayList"}, 0, null);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Enumeration", "nextElement", "()Ljava/lang/Object;", true);
        mv.visitTypeInsn(CHECKCAST, "java/net/URL");
        mv.visitVarInsn(ASTORE, 3);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/webapp/tomcat/CustomContextClassLoader", "wrapURL", "(Ljava/net/URL;)Ljava/net/URL;", false);
        mv.visitVarInsn(ASTORE, 4);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", false);
        mv.visitInsn(POP);
        mv.visitLabel(loopCondition);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Enumeration", "hasMoreElements", "()Z", true);
        mv.visitJumpInsn(IFNE, loopBody);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "enumeration", "(Ljava/util/Collection;)Ljava/util/Enumeration;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 5);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PRIVATE, "wrapURL", "(Ljava/net/URL;)Ljava/net/URL;", null, null);
        mv.visitCode();
        Label wrapTryStart = new Label();
        Label wrapTryEnd = new Label();
        Label wrapCatchHandler = new Label();
        mv.visitTryCatchBlock(wrapTryStart, wrapTryEnd, wrapCatchHandler, "java/io/IOException");
        mv.visitVarInsn(ALOAD, 1);
        Label nonNullUrl = new Label();
        mv.visitJumpInsn(IFNONNULL, nonNullUrl);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ARETURN);
        mv.visitLabel(nonNullUrl);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/net/URL", "toString", "()Ljava/lang/String;", false);
        mv.visitLdcInsn("/");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false);
        mv.visitJumpInsn(IFEQ, wrapTryStart);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ARETURN);
        mv.visitLabel(wrapTryStart);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitTypeInsn(NEW, "java/net/URL");
        mv.visitInsn(DUP);
        mv.visitInsn(ACONST_NULL);
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitLdcInsn(this.urlProtocol + ":");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/net/URL", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitTypeInsn(NEW, "com/licel/stringer/StringerURLHandlerPrototype");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/StringerURLHandlerPrototype", "<init>", "()V", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/net/URL", "<init>", "(Ljava/net/URL;Ljava/lang/String;Ljava/net/URLStreamHandler;)V", false);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitLabel(wrapTryEnd);
        mv.visitInsn(ARETURN);
        mv.visitLabel(wrapCatchHandler);
        mv.visitFrame(F_SAME1, 0, null, 1, new Object[]{"java/io/IOException"});
        mv.visitInsn(POP);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(6, 3);
        mv.visitEnd();

        this.cv.visitEnd();
    }
}
