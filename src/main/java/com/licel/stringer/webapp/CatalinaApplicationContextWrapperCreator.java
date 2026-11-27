package com.licel.stringer.webapp;

import com.licel.stringer.model.ClassInfo;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CatalinaApplicationContextWrapperCreator extends ClassVisitor implements Opcodes {
    public static final Properties METHOD_MAPPINGS = new Properties();
    public static final Properties FIELD_MAPPINGS = new Properties();
    public ClassInfo classInfo;

    static {
        FIELD_MAPPINGS.put("parent", "Lorg/apache/catalina/core/ApplicationContext;");
    }

    public CatalinaApplicationContextWrapperCreator(ClassVisitor classVisitor, ClassInfo classInfo) {
        super(Opcodes.ASM9, classVisitor);
        this.classInfo = classInfo;
        this.generate();
    }

    public void generate() {
        int classVersion = this.classInfo.classFileVersion;
        this.cv.visit(
                classVersion,
                ACC_PUBLIC | ACC_SUPER,
                "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper",
                null,
                "org/apache/catalina/core/ApplicationContext",
                null);

        FieldVisitor fv = this.cv.visitField(0, "parent", "Lorg/apache/catalina/core/ApplicationContext;", null, null);
        fv.visitEnd();

        MethodVisitor mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "<init>",
                "(Lorg/apache/catalina/core/StandardContext;Lorg/apache/catalina/core/ApplicationContext;)V",
                null,
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, "org/apache/catalina/core/ApplicationContext", "<init>", "(Lorg/apache/catalina/core/StandardContext;)V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getAttribute", "(Ljava/lang/String;)Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getAttribute", "(Ljava/lang/String;)Ljava/lang/Object;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getAttributeNames", "()Ljava/util/Enumeration;", "()Ljava/util/Enumeration<Ljava/lang/String;>;", null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getAttributeNames", "()Ljava/util/Enumeration;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getContext", "(Ljava/lang/String;)Ljavax/servlet/ServletContext;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getContext", "(Ljava/lang/String;)Ljavax/servlet/ServletContext;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getContextPath", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getContextPath", "()Ljava/lang/String;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getInitParameter", "(Ljava/lang/String;)Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getInitParameter", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getInitParameterNames", "()Ljava/util/Enumeration;", "()Ljava/util/Enumeration<Ljava/lang/String;>;", null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getInitParameterNames", "()Ljava/util/Enumeration;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getMajorVersion", "()I", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getMajorVersion", "()I", false);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getMinorVersion", "()I", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getMinorVersion", "()I", false);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getMimeType", "(Ljava/lang/String;)Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getMimeType", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getNamedDispatcher", "(Ljava/lang/String;)Ljavax/servlet/RequestDispatcher;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "org/apache/catalina/core/ApplicationContext",
                "getNamedDispatcher",
                "(Ljava/lang/String;)Ljavax/servlet/RequestDispatcher;",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getRealPath", "(Ljava/lang/String;)Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getRealPath", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getRequestDispatcher", "(Ljava/lang/String;)Ljavax/servlet/RequestDispatcher;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "org/apache/catalina/core/ApplicationContext",
                "getRequestDispatcher",
                "(Ljava/lang/String;)Ljavax/servlet/RequestDispatcher;",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "getResource",
                "(Ljava/lang/String;)Ljava/net/URL;",
                null,
                new String[]{"java/net/MalformedURLException"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getResource", "(Ljava/lang/String;)Ljava/net/URL;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
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
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/StringerInputStreamPrototype", "<init>", "(Ljava/io/InputStream;)V", false);
        mv.visitLabel(tryEnd);
        mv.visitInsn(ARETURN);
        mv.visitLabel(catchHandler);
        mv.visitFrame(F_SAME1, 0, null, 1, new Object[]{"java/io/IOException"});
        mv.visitInsn(POP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(4, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getResourcePaths", "(Ljava/lang/String;)Ljava/util/Set;", "(Ljava/lang/String;)Ljava/util/Set<Ljava/lang/String;>;", null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getResourcePaths", "(Ljava/lang/String;)Ljava/util/Set;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getServerInfo", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getServerInfo", "()Ljava/lang/String;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getServlet", "(Ljava/lang/String;)Ljavax/servlet/Servlet;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getServlet", "(Ljava/lang/String;)Ljavax/servlet/Servlet;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getServletContextName", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getServletContextName", "()Ljava/lang/String;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getServletNames", "()Ljava/util/Enumeration;", "()Ljava/util/Enumeration<Ljava/lang/String;>;", null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getServletNames", "()Ljava/util/Enumeration;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getServlets", "()Ljava/util/Enumeration;", "()Ljava/util/Enumeration<Ljavax/servlet/Servlet;>;", null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getServlets", "()Ljava/util/Enumeration;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "log", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "log", "(Ljava/lang/String;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "log", "(Ljava/lang/Exception;Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "log", "(Ljava/lang/Exception;Ljava/lang/String;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "log", "(Ljava/lang/String;Ljava/lang/Throwable;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "log", "(Ljava/lang/String;Ljava/lang/Throwable;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "removeAttribute", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "removeAttribute", "(Ljava/lang/String;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "setAttribute", "(Ljava/lang/String;Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "setAttribute", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "addFilter", "(Ljava/lang/String;Ljava/lang/String;)Ljavax/servlet/FilterRegistration$Dynamic;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "org/apache/catalina/core/ApplicationContext",
                "addFilter",
                "(Ljava/lang/String;Ljava/lang/String;)Ljavax/servlet/FilterRegistration$Dynamic;",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "addFilter", "(Ljava/lang/String;Ljavax/servlet/Filter;)Ljavax/servlet/FilterRegistration$Dynamic;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "org/apache/catalina/core/ApplicationContext",
                "addFilter",
                "(Ljava/lang/String;Ljavax/servlet/Filter;)Ljavax/servlet/FilterRegistration$Dynamic;",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "addFilter",
                "(Ljava/lang/String;Ljava/lang/Class;)Ljavax/servlet/FilterRegistration$Dynamic;",
                "(Ljava/lang/String;Ljava/lang/Class<+Ljavax/servlet/Filter;>;)Ljavax/servlet/FilterRegistration$Dynamic;",
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "org/apache/catalina/core/ApplicationContext",
                "addFilter",
                "(Ljava/lang/String;Ljava/lang/Class;)Ljavax/servlet/FilterRegistration$Dynamic;",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "createFilter",
                "(Ljava/lang/Class;)Ljavax/servlet/Filter;",
                "<T::Ljavax/servlet/Filter;>(Ljava/lang/Class<TT;>;)TT;",
                new String[]{"javax/servlet/ServletException"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "createFilter", "(Ljava/lang/Class;)Ljavax/servlet/Filter;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getFilterRegistration", "(Ljava/lang/String;)Ljavax/servlet/FilterRegistration;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "org/apache/catalina/core/ApplicationContext",
                "getFilterRegistration",
                "(Ljava/lang/String;)Ljavax/servlet/FilterRegistration;",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "addServlet", "(Ljava/lang/String;Ljava/lang/String;)Ljavax/servlet/ServletRegistration$Dynamic;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "org/apache/catalina/core/ApplicationContext",
                "addServlet",
                "(Ljava/lang/String;Ljava/lang/String;)Ljavax/servlet/ServletRegistration$Dynamic;",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "addServlet", "(Ljava/lang/String;Ljavax/servlet/Servlet;)Ljavax/servlet/ServletRegistration$Dynamic;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "org/apache/catalina/core/ApplicationContext",
                "addServlet",
                "(Ljava/lang/String;Ljavax/servlet/Servlet;)Ljavax/servlet/ServletRegistration$Dynamic;",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "addServlet",
                "(Ljava/lang/String;Ljava/lang/Class;)Ljavax/servlet/ServletRegistration$Dynamic;",
                "(Ljava/lang/String;Ljava/lang/Class<+Ljavax/servlet/Servlet;>;)Ljavax/servlet/ServletRegistration$Dynamic;",
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "org/apache/catalina/core/ApplicationContext",
                "addServlet",
                "(Ljava/lang/String;Ljava/lang/Class;)Ljavax/servlet/ServletRegistration$Dynamic;",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "addJspFile", "(Ljava/lang/String;Ljava/lang/String;)Ljavax/servlet/ServletRegistration$Dynamic;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "org/apache/catalina/core/ApplicationContext",
                "addJspFile",
                "(Ljava/lang/String;Ljava/lang/String;)Ljavax/servlet/ServletRegistration$Dynamic;",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "createServlet",
                "(Ljava/lang/Class;)Ljavax/servlet/Servlet;",
                "<T::Ljavax/servlet/Servlet;>(Ljava/lang/Class<TT;>;)TT;",
                new String[]{"javax/servlet/ServletException"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "createServlet", "(Ljava/lang/Class;)Ljavax/servlet/Servlet;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getServletRegistration", "(Ljava/lang/String;)Ljavax/servlet/ServletRegistration;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "org/apache/catalina/core/ApplicationContext",
                "getServletRegistration",
                "(Ljava/lang/String;)Ljavax/servlet/ServletRegistration;",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "getDefaultSessionTrackingModes",
                "()Ljava/util/Set;",
                "()Ljava/util/Set<Ljavax/servlet/SessionTrackingMode;>;",
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getDefaultSessionTrackingModes", "()Ljava/util/Set;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "getEffectiveSessionTrackingModes",
                "()Ljava/util/Set;",
                "()Ljava/util/Set<Ljavax/servlet/SessionTrackingMode;>;",
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getEffectiveSessionTrackingModes", "()Ljava/util/Set;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getSessionCookieConfig", "()Ljavax/servlet/SessionCookieConfig;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getSessionCookieConfig", "()Ljavax/servlet/SessionCookieConfig;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "setSessionTrackingModes",
                "(Ljava/util/Set;)V",
                "(Ljava/util/Set<Ljavax/servlet/SessionTrackingMode;>;)V",
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "setSessionTrackingModes", "(Ljava/util/Set;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "setInitParameter", "(Ljava/lang/String;Ljava/lang/String;)Z", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "setInitParameter", "(Ljava/lang/String;Ljava/lang/String;)Z", false);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "addListener", "(Ljava/lang/Class;)V", "(Ljava/lang/Class<+Ljava/util/EventListener;>;)V", null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "addListener", "(Ljava/lang/Class;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "addListener", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "addListener", "(Ljava/lang/String;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "addListener", "(Ljava/util/EventListener;)V", "<T::Ljava/util/EventListener;>(TT;)V", null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "addListener", "(Ljava/util/EventListener;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "createListener",
                "(Ljava/lang/Class;)Ljava/util/EventListener;",
                "<T::Ljava/util/EventListener;>(Ljava/lang/Class<TT;>;)TT;",
                new String[]{"javax/servlet/ServletException"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "createListener", "(Ljava/lang/Class;)Ljava/util/EventListener;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC | ACC_VARARGS, "declareRoles", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "declareRoles", "([Ljava/lang/String;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getClassLoader", "()Ljava/lang/ClassLoader;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getEffectiveMajorVersion", "()I", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getEffectiveMajorVersion", "()I", false);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getEffectiveMinorVersion", "()I", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getEffectiveMinorVersion", "()I", false);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "getFilterRegistrations",
                "()Ljava/util/Map;",
                "()Ljava/util/Map<Ljava/lang/String;+Ljavax/servlet/FilterRegistration;>;",
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getFilterRegistrations", "()Ljava/util/Map;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getJspConfigDescriptor", "()Ljavax/servlet/descriptor/JspConfigDescriptor;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(
                INVOKEVIRTUAL,
                "org/apache/catalina/core/ApplicationContext",
                "getJspConfigDescriptor",
                "()Ljavax/servlet/descriptor/JspConfigDescriptor;",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "getServletRegistrations",
                "()Ljava/util/Map;",
                "()Ljava/util/Map<Ljava/lang/String;+Ljavax/servlet/ServletRegistration;>;",
                null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getServletRegistrations", "()Ljava/util/Map;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getVirtualServerName", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getVirtualServerName", "()Ljava/lang/String;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getSessionTimeout", "()I", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getSessionTimeout", "()I", false);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "setSessionTimeout", "(I)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "setSessionTimeout", "(I)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getRequestCharacterEncoding", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getRequestCharacterEncoding", "()Ljava/lang/String;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "setRequestCharacterEncoding", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "setRequestCharacterEncoding", "(Ljava/lang/String;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "getResponseCharacterEncoding", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "getResponseCharacterEncoding", "()Ljava/lang/String;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "setResponseCharacterEncoding", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper", "parent", "Lorg/apache/catalina/core/ApplicationContext;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/catalina/core/ApplicationContext", "setResponseCharacterEncoding", "(Ljava/lang/String;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        this.cv.visitEnd();
    }
}
