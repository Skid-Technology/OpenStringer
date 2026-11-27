package com.licel.stringer.webapp;

import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.util.StringerUtils;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class WebContextLoaderListenerCreator extends ClassVisitor implements Opcodes {
    public static final Properties METHOD_MAPPINGS = new Properties();
    public static final Properties FIELD_MAPPINGS = new Properties();
    public ClassInfo classInfo;
    public WebApplicationDescriptor webApplicationDescriptor;

    static {
        FIELD_MAPPINGS.put("listeners", "Ljava/util/ArrayList;");
        FIELD_MAPPINGS.put("servletContext", "Ljavax/servlet/ServletContext;");
        METHOD_MAPPINGS.put("configureServletContext", "(Ljavax/servlet/ServletContext;)V");
    }

    public WebContextLoaderListenerCreator(
            ClassVisitor classVisitor,
            ClassInfo classInfo,
            WebApplicationDescriptor webApplicationDescriptor) {
        super(Opcodes.ASM9, classVisitor);
        this.webApplicationDescriptor = webApplicationDescriptor;
        this.classInfo = classInfo;
        this.generate();
    }

    public void generate() {
        this.cv.visit(
                this.classInfo.classFileVersion,
                ACC_PUBLIC | ACC_SUPER,
                "com/licel/stringer/webapp/tomcat/ContextLoaderListener",
                null,
                this.classInfo.superName,
                new String[]{"javax/servlet/ServletContextListener"});
        this.cv.visitInnerClass(
                "javax/servlet/ServletRegistration$Dynamic",
                "javax/servlet/ServletRegistration",
                "Dynamic",
                ACC_PUBLIC | ACC_STATIC | ACC_INTERFACE | ACC_ABSTRACT);

        FieldVisitor fv = this.cv.visitField(0, "listeners", "Ljava/util/ArrayList;", null, null);
        fv.visitEnd();
        fv = this.cv.visitField(0, "servletContext", "Ljavax/servlet/ServletContext;", null, null);
        fv.visitEnd();

        MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, "java/util/ArrayList");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
        mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "listeners", "Ljava/util/ArrayList;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 1);
        mv.visitEnd();
        this.cv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "contextInitialized",
                "(Ljavax/servlet/ServletContextEvent;)V",
                null,
                new String[]{"java/lang/Exception"});
        mv.visitCode();
        Label tryStart = new Label();
        Label finallyHandler = new Label();
        mv.visitTryCatchBlock(tryStart, finallyHandler, finallyHandler, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javax/servlet/ServletContextEvent", "getServletContext", "()Ljavax/servlet/ServletContext;", false);
        mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "servletContext", "Ljavax/servlet/ServletContext;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "servletContext", "Ljavax/servlet/ServletContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
        mv.visitLdcInsn("context");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ICONST_1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "servletContext", "Ljavax/servlet/ServletContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
        mv.visitLdcInsn("context");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
        mv.visitVarInsn(ASTORE, 4);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitInsn(ICONST_1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitVarInsn(ASTORE, 5);
        mv.visitTypeInsn(NEW, "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitTypeInsn(CHECKCAST, "org/apache/catalina/core/StandardContext");
        mv.visitVarInsn(ALOAD, 3);
        mv.visitTypeInsn(CHECKCAST, "org/apache/catalina/core/ApplicationContext");
        mv.visitMethodInsn(
                INVOKESPECIAL,
                "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper",
                "<init>",
                "(Lorg/apache/catalina/core/StandardContext;Lorg/apache/catalina/core/ApplicationContext;)V",
                false);
        mv.visitVarInsn(ASTORE, 6);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "servletContext", "Ljavax/servlet/ServletContext;");
        mv.visitVarInsn(ALOAD, 6);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "set", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "servletContext", "Ljavax/servlet/ServletContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
        mv.visitLdcInsn("objectCache");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
        mv.visitVarInsn(ASTORE, 7);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitInsn(ICONST_1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "servletContext", "Ljavax/servlet/ServletContext;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitTypeInsn(CHECKCAST, "java/util/Map");
        mv.visitVarInsn(ASTORE, 8);
        mv.visitVarInsn(ALOAD, 8);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "clear", "()V", true);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "servletContext", "Ljavax/servlet/ServletContext;");
        mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "configureServletContext", "(Ljavax/servlet/ServletContext;)V", false);
        mv.visitTypeInsn(NEW, "javax/servlet/ServletContextEvent");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "servletContext", "Ljavax/servlet/ServletContext;");
        mv.visitMethodInsn(INVOKESPECIAL, "javax/servlet/ServletContextEvent", "<init>", "(Ljavax/servlet/ServletContext;)V", false);
        mv.visitVarInsn(ASTORE, 9);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getContextClassLoader", "()Ljava/lang/ClassLoader;", false);
        mv.visitVarInsn(ASTORE, 10);
        mv.visitLabel(tryStart);
        mv.visitTypeInsn(NEW, "com/licel/stringer/webapp/tomcat/CustomContextClassLoader");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getContextClassLoader", "()Ljava/lang/ClassLoader;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/webapp/tomcat/CustomContextClassLoader", "<init>", "(Ljava/lang/ClassLoader;)V", false);
        mv.visitVarInsn(ASTORE, 11);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
        mv.visitVarInsn(ALOAD, 11);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "setContextClassLoader", "(Ljava/lang/ClassLoader;)V", false);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 12);
        Label loopCondition = new Label();
        mv.visitJumpInsn(GOTO, loopCondition);
        Label loopBody = new Label();
        mv.visitLabel(loopBody);
        mv.visitFrame(
                F_FULL,
                13,
                new Object[]{
                        "com/licel/stringer/webapp/tomcat/ContextLoaderListener",
                        "javax/servlet/ServletContextEvent",
                        "java/lang/reflect/Field",
                        "java/lang/Object",
                        "java/lang/reflect/Field",
                        "java/lang/Object",
                        "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper",
                        "java/lang/reflect/Field",
                        "java/util/Map",
                        "javax/servlet/ServletContextEvent",
                        "java/lang/ClassLoader",
                        "com/licel/stringer/webapp/tomcat/CustomContextClassLoader",
                        Opcodes.INTEGER
                },
                0,
                new Object[0]);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "listeners", "Ljava/util/ArrayList;");
        mv.visitVarInsn(ILOAD, 12);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "get", "(I)Ljava/lang/Object;", false);
        mv.visitTypeInsn(CHECKCAST, "javax/servlet/ServletContextListener");
        mv.visitVarInsn(ALOAD, 9);
        mv.visitMethodInsn(INVOKEINTERFACE, "javax/servlet/ServletContextListener", "contextInitialized", "(Ljavax/servlet/ServletContextEvent;)V", true);
        mv.visitIincInsn(12, 1);
        mv.visitLabel(loopCondition);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ILOAD, 12);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "listeners", "Ljava/util/ArrayList;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "size", "()I", false);
        mv.visitJumpInsn(IF_ICMPLT, loopBody);
        Label afterTry = new Label();
        mv.visitJumpInsn(GOTO, afterTry);
        mv.visitLabel(finallyHandler);
        mv.visitFrame(
                F_FULL,
                11,
                new Object[]{
                        "com/licel/stringer/webapp/tomcat/ContextLoaderListener",
                        "javax/servlet/ServletContextEvent",
                        "java/lang/reflect/Field",
                        "java/lang/Object",
                        "java/lang/reflect/Field",
                        "java/lang/Object",
                        "com/licel/stringer/webapp/tomcat/ApplicationContextWrapper",
                        "java/lang/reflect/Field",
                        "java/util/Map",
                        "javax/servlet/ServletContextEvent",
                        "java/lang/ClassLoader"
                },
                1,
                new Object[]{"java/lang/Throwable"});
        mv.visitVarInsn(ASTORE, 13);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "setContextClassLoader", "(Ljava/lang/ClassLoader;)V", false);
        mv.visitVarInsn(ALOAD, 13);
        mv.visitInsn(ATHROW);
        mv.visitLabel(afterTry);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitInsn(RETURN);
        mv.visitMaxs(4, 14);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PRIVATE,
                "configureServletContext",
                "(Ljavax/servlet/ServletContext;)V",
                null,
                new String[]{"java/lang/Exception"});
        mv.visitCode();
        mv.visitLdcInsn(Type.getType("Ljavax/servlet/DispatcherType;"));
        mv.visitMethodInsn(INVOKESTATIC, "java/util/EnumSet", "allOf", "(Ljava/lang/Class;)Ljava/util/EnumSet;", false);
        mv.visitVarInsn(ASTORE, 2);

        for (WebInitParameter contextParam : this.webApplicationDescriptor.contextParameters) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(contextParam.name);
            mv.visitLdcInsn(contextParam.value);
            mv.visitMethodInsn(INVOKEINTERFACE, "javax/servlet/ServletContext", "setInitParameter", "(Ljava/lang/String;Ljava/lang/String;)Z", true);
            mv.visitInsn(POP);
        }

        for (WebFilterDescriptor filter : this.webApplicationDescriptor.filters) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(filter.name);
            mv.visitLdcInsn(filter.className);
            mv.visitMethodInsn(
                    INVOKEINTERFACE,
                    "javax/servlet/ServletContext",
                    "addFilter",
                    "(Ljava/lang/String;Ljava/lang/String;)Ljavax/servlet/FilterRegistration$Dynamic;",
                    true);
            mv.visitVarInsn(ASTORE, 3);
            mv.visitVarInsn(ALOAD, 3);
            if (filter.asyncSupported != null) {
                mv.visitVarInsn(ALOAD, 3);
                mv.visitInsn(Boolean.parseBoolean(filter.asyncSupported) ? ICONST_1 : ICONST_0);
                mv.visitMethodInsn(INVOKEINTERFACE, "javax/servlet/FilterRegistration$Dynamic", "setAsyncSupported", "(Z)V", true);
            }

            for (WebInitParameter initParam : filter.initParameters) {
                mv.visitVarInsn(ALOAD, 3);
                mv.visitLdcInsn(initParam.name);
                mv.visitLdcInsn(initParam.value);
                mv.visitMethodInsn(INVOKEINTERFACE, "javax/servlet/FilterRegistration$Dynamic", "setInitParameter", "(Ljava/lang/String;Ljava/lang/String;)Z", true);
                mv.visitInsn(POP);
            }

            if (!filter.urlPatterns.isEmpty()) {
                mv.visitVarInsn(ALOAD, 3);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitInsn(ICONST_0);
                StringerUtils.pushIntConstant(mv, filter.urlPatterns.size());
                mv.visitTypeInsn(ANEWARRAY, "java/lang/String");

                for (int i = 0; i < filter.urlPatterns.size(); i++) {
                    mv.visitInsn(DUP);
                    StringerUtils.pushIntConstant(mv, i);
                    mv.visitLdcInsn(filter.urlPatterns.get(i));
                    mv.visitInsn(AASTORE);
                }

                mv.visitMethodInsn(
                        INVOKEINTERFACE,
                        "javax/servlet/FilterRegistration$Dynamic",
                        "addMappingForUrlPatterns",
                        "(Ljava/util/EnumSet;Z[Ljava/lang/String;)V",
                        true);
            }
        }

        for (WebServletDescriptor servlet : this.webApplicationDescriptor.servlets) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(servlet.name);
            mv.visitLdcInsn(servlet.className);
            mv.visitMethodInsn(
                    INVOKEINTERFACE,
                    "javax/servlet/ServletContext",
                    "addServlet",
                    "(Ljava/lang/String;Ljava/lang/String;)Ljavax/servlet/ServletRegistration$Dynamic;",
                    true);
            mv.visitVarInsn(ASTORE, 4);
            if (servlet.asyncSupported != null) {
                mv.visitVarInsn(ALOAD, 4);
                mv.visitInsn(Boolean.parseBoolean(servlet.asyncSupported) ? ICONST_1 : ICONST_0);
                mv.visitMethodInsn(INVOKEINTERFACE, "javax/servlet/ServletRegistration$Dynamic", "setAsyncSupported", "(Z)V", true);
            }

            if (servlet.loadOnStartup != null) {
                mv.visitVarInsn(ALOAD, 4);
                StringerUtils.pushIntConstant(mv, Integer.parseInt(servlet.loadOnStartup));
                mv.visitMethodInsn(INVOKEINTERFACE, "javax/servlet/ServletRegistration$Dynamic", "setLoadOnStartup", "(I)V", true);
            }

            for (WebInitParameter initParam : servlet.initParameters) {
                mv.visitVarInsn(ALOAD, 4);
                mv.visitLdcInsn(initParam.name);
                mv.visitLdcInsn(initParam.value);
                mv.visitMethodInsn(INVOKEINTERFACE, "javax/servlet/ServletRegistration$Dynamic", "setInitParameter", "(Ljava/lang/String;Ljava/lang/String;)Z", true);
                mv.visitInsn(POP);
            }

            if (!servlet.urlPatterns.isEmpty()) {
                mv.visitVarInsn(ALOAD, 4);
                StringerUtils.pushIntConstant(mv, servlet.urlPatterns.size());
                mv.visitTypeInsn(ANEWARRAY, "java/lang/String");

                for (int i = 0; i < servlet.urlPatterns.size(); i++) {
                    mv.visitInsn(DUP);
                    StringerUtils.pushIntConstant(mv, i);
                    mv.visitLdcInsn(servlet.urlPatterns.get(i));
                    mv.visitInsn(AASTORE);
                }

                mv.visitMethodInsn(INVOKEINTERFACE, "javax/servlet/ServletRegistration$Dynamic", "addMapping", "([Ljava/lang/String;)Ljava/util/Set;", true);
            }
        }

        for (WebListenerDescriptor listener : this.webApplicationDescriptor.listeners) {
            mv.visitLdcInsn(listener.className);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "newInstance", "()Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, "javax/servlet/ServletContextListener");
            mv.visitVarInsn(ASTORE, 5);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "listeners", "Ljava/util/ArrayList;");
            mv.visitVarInsn(ALOAD, 5);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", false);
            mv.visitInsn(POP);
        }

        mv.visitInsn(RETURN);
        mv.visitMaxs(7, 6);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PUBLIC, "contextDestroyed", "(Ljavax/servlet/ServletContextEvent;)V", null, null);
        mv.visitCode();
        Label newEventLabel = new Label();
        mv.visitLabel(newEventLabel);
        mv.visitTypeInsn(NEW, "javax/servlet/ServletContextEvent");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "servletContext", "Ljavax/servlet/ServletContext;");
        Label fallbackContextLabel = new Label();
        mv.visitJumpInsn(IFNULL, fallbackContextLabel);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "servletContext", "Ljavax/servlet/ServletContext;");
        Label initEventLabel = new Label();
        mv.visitJumpInsn(GOTO, initEventLabel);
        mv.visitLabel(fallbackContextLabel);
        mv.visitFrame(
                F_FULL,
                2,
                new Object[]{"com/licel/stringer/webapp/tomcat/ContextLoaderListener", "javax/servlet/ServletContextEvent"},
                2,
                new Object[]{newEventLabel, newEventLabel});
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javax/servlet/ServletContextEvent", "getServletContext", "()Ljavax/servlet/ServletContext;", false);
        mv.visitLabel(initEventLabel);
        mv.visitFrame(
                F_FULL,
                2,
                new Object[]{"com/licel/stringer/webapp/tomcat/ContextLoaderListener", "javax/servlet/ServletContextEvent"},
                3,
                new Object[]{newEventLabel, newEventLabel, "javax/servlet/ServletContext"});
        mv.visitMethodInsn(INVOKESPECIAL, "javax/servlet/ServletContextEvent", "<init>", "(Ljavax/servlet/ServletContext;)V", false);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 3);
        Label destroyLoopCondition = new Label();
        mv.visitJumpInsn(GOTO, destroyLoopCondition);
        Label destroyLoopBody = new Label();
        mv.visitLabel(destroyLoopBody);
        mv.visitFrame(F_APPEND, 2, new Object[]{"javax/servlet/ServletContextEvent", Opcodes.INTEGER}, 0, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "listeners", "Ljava/util/ArrayList;");
        mv.visitVarInsn(ILOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "get", "(I)Ljava/lang/Object;", false);
        mv.visitTypeInsn(CHECKCAST, "javax/servlet/ServletContextListener");
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEINTERFACE, "javax/servlet/ServletContextListener", "contextDestroyed", "(Ljavax/servlet/ServletContextEvent;)V", true);
        mv.visitIincInsn(3, 1);
        mv.visitLabel(destroyLoopCondition);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ILOAD, 3);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ContextLoaderListener", "listeners", "Ljava/util/ArrayList;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "size", "()I", false);
        mv.visitJumpInsn(IF_ICMPLT, destroyLoopBody);
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 4);
        mv.visitEnd();
    }
}
