package com.licel.stringer.transform;

import com.licel.stringer.Worker;
import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.util.AsmUtils;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassResourceReflection extends ClassVisitor implements Opcodes {
    private final Properties runtimeMappings;
    String ownerClassName;
    String classGetResourceAsStreamWrapper;
    String classLoaderGetResourceAsStreamWrapper;
    String classGetResourceWrapper;
    String classLoaderGetResourceWrapper;
    String uriToUrlWrapper;
    String urlOpenStreamWrapper;
    String servletGetResourceAsStreamWrapper;
    String servletGetResourceWrapper;
    String registerHandlerMethodName;
    final ClassInfo classInfo;
    private boolean requiresStackFrames;
    private final String protocolPrefix;
    private final String handlerPackageName;

    public ClassResourceReflection(ClassVisitor classVisitor, ClassInfo classInfo, Properties runtimeMappings) {
        super(Opcodes.ASM9, classVisitor);
        this.runtimeMappings = runtimeMappings;
        this.classInfo = classInfo;
        String[] parts = runtimeMappings.getProperty("com/licel/stringer/StringerInputStreamPrototype").split("/");
        this.protocolPrefix = parts[1] + ":";
        this.handlerPackageName = parts[0];
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.ownerClassName = name;
        this.requiresStackFrames = version >= Opcodes.V1_6 && version != Opcodes.V1_1;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = this.cv.visitMethod(access, name, descriptor, signature, exceptions);
        return new ResourceReflectionMethodVisitor(this, mv);
    }

    @Override
    public void visitEnd() {
        if (this.uriToUrlWrapper != null
                || this.classLoaderGetResourceWrapper != null
                || this.classGetResourceWrapper != null
                || this.servletGetResourceWrapper != null) {
            this.registerHandlerMethodName = Worker.get().internalNameGenerator.generateMethodName(this.classInfo, "()V", null);
        }

        int computeFlags = (this.requiresStackFrames ? ClassWriter.COMPUTE_FRAMES : 0) | ClassWriter.COMPUTE_MAXS;
        String inputStreamClass = this.runtimeMappings.getProperty("com/licel/stringer/StringerInputStreamPrototype");
        String urlHandlerClass = this.runtimeMappings.getProperty("com/licel/stringer/StringerURLHandlerPrototype");

        if (this.classGetResourceAsStreamWrapper != null) {
            MethodVisitor mv = AsmUtils.visitMethod(
                    this.cv, Opcodes.ACC_STATIC, this.classGetResourceAsStreamWrapper,
                    "(Ljava/lang/Class;Ljava/lang/String;)Ljava/io/InputStream;", null, null, computeFlags);
            mv.visitCode();
            mv.visitTypeInsn(Opcodes.NEW, inputStreamClass);
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, inputStreamClass, "<init>", "(Ljava/io/InputStream;)V", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(4, 2);
            mv.visitEnd();
        }

        if (this.classLoaderGetResourceAsStreamWrapper != null) {
            MethodVisitor mv = AsmUtils.visitMethod(
                    this.cv, Opcodes.ACC_STATIC, this.classLoaderGetResourceAsStreamWrapper,
                    "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/io/InputStream;", null, null, computeFlags);
            mv.visitCode();
            mv.visitTypeInsn(Opcodes.NEW, inputStreamClass);
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/ClassLoader", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, inputStreamClass, "<init>", "(Ljava/io/InputStream;)V", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(4, 2);
            mv.visitEnd();
        }

        if (this.servletGetResourceAsStreamWrapper != null) {
            MethodVisitor mv = AsmUtils.visitMethod(
                    this.cv, Opcodes.ACC_STATIC, this.servletGetResourceAsStreamWrapper,
                    "(Ljavax/servlet/ServletContext;Ljava/lang/String;)Ljava/io/InputStream;", null, null, computeFlags);
            mv.visitCode();
            mv.visitTypeInsn(Opcodes.NEW, inputStreamClass);
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "javax/servlet/ServletContext", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, inputStreamClass, "<init>", "(Ljava/io/InputStream;)V", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(4, 2);
            mv.visitEnd();
        }

        if (this.classGetResourceWrapper != null) {
            this.emitResourceUrlWrapper(
                    this.classGetResourceWrapper,
                    "(Ljava/lang/Class;Ljava/lang/String;)Ljava/net/URL;",
                    "java/lang/Class",
                    urlHandlerClass,
                    computeFlags);
        }

        if (this.classLoaderGetResourceWrapper != null) {
            this.emitResourceUrlWrapper(
                    this.classLoaderGetResourceWrapper,
                    "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/net/URL;",
                    "java/lang/ClassLoader",
                    urlHandlerClass,
                    computeFlags);
        }

        if (this.servletGetResourceWrapper != null) {
            this.emitResourceUrlWrapper(
                    this.servletGetResourceWrapper,
                    "(Ljavax/servlet/ServletContext;Ljava/lang/String;)Ljava/net/URL;",
                    "javax/servlet/ServletContext",
                    urlHandlerClass,
                    computeFlags);
        }

        if (this.uriToUrlWrapper != null) {
            MethodVisitor mv = AsmUtils.visitMethod(
                    this.cv, Opcodes.ACC_STATIC, this.uriToUrlWrapper,
                    "(Ljava/net/URI;)Ljava/net/URL;", null, null, computeFlags);
            mv.visitCode();
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, this.ownerClassName, this.registerHandlerMethodName, "()V", false);
            mv.visitTypeInsn(Opcodes.NEW, "java/net/URL");
            mv.visitInsn(Opcodes.DUP);
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn(this.protocolPrefix);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/net/URI", "toURL", "()Ljava/net/URL;", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/net/URL", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitTypeInsn(Opcodes.NEW, urlHandlerClass);
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, urlHandlerClass, "<init>", "()V", false);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/net/URL", "<init>", "(Ljava/net/URL;Ljava/lang/String;Ljava/net/URLStreamHandler;)V", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(6, 1);
            mv.visitEnd();
        }

        if (this.urlOpenStreamWrapper != null) {
            MethodVisitor mv = AsmUtils.visitMethod(
                    this.cv, Opcodes.ACC_STATIC, this.urlOpenStreamWrapper,
                    "(Ljava/net/URL;)Ljava/io/InputStream;", null, new String[]{"java/io/IOException"}, computeFlags);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/net/URL", "getProtocol", "()Ljava/lang/String;", false);
            mv.visitLdcInsn("jar");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
            Label notJarLabel = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, notJarLabel);
            mv.visitTypeInsn(Opcodes.NEW, inputStreamClass);
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/net/URL", "openStream", "()Ljava/io/InputStream;", false);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, inputStreamClass, "<init>", "(Ljava/io/InputStream;)V", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(notJarLabel);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/net/URL", "openStream", "()Ljava/io/InputStream;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(3, 1);
            mv.visitEnd();
        }

        if (this.registerHandlerMethodName != null) {
            MethodVisitor mv = AsmUtils.visitMethod(
                    this.cv, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, this.registerHandlerMethodName,
                    "()V", null, null, computeFlags);
            mv.visitCode();
            mv.visitLdcInsn("java.protocol.handler.pkgs");
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
            Label alreadySetLabel = new Label();
            mv.visitJumpInsn(Opcodes.IFNONNULL, alreadySetLabel);
            mv.visitLdcInsn("java.protocol.handler.pkgs");
            mv.visitLdcInsn(this.handlerPackageName);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "setProperty", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false);
            mv.visitInsn(Opcodes.POP);
            Label doneLabel = new Label();
            mv.visitJumpInsn(Opcodes.GOTO, doneLabel);
            mv.visitLabel(alreadySetLabel);
            if (this.requiresStackFrames) {
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
            mv.visitLdcInsn("java.protocol.handler.pkgs");
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
            mv.visitLdcInsn(this.handlerPackageName);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
            mv.visitJumpInsn(Opcodes.IFNE, doneLabel);
            mv.visitLdcInsn("java.protocol.handler.pkgs");
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn("java.protocol.handler.pkgs");
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn("|");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn(this.handlerPackageName);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "setProperty", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false);
            mv.visitInsn(Opcodes.POP);
            mv.visitLabel(doneLabel);
            if (this.requiresStackFrames) {
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            }
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(3, 0);
            mv.visitEnd();
        }

        this.cv.visitEnd();
    }

    private void emitResourceUrlWrapper(
            String wrapperName,
            String wrapperDescriptor,
            String receiverType,
            String urlHandlerClass,
            int computeFlags) {
        MethodVisitor mv = AsmUtils.visitMethod(
                this.cv, Opcodes.ACC_STATIC, wrapperName, wrapperDescriptor, null, null, computeFlags);
        mv.visitCode();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, this.ownerClassName, this.registerHandlerMethodName, "()V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, receiverType, "getResource", "(Ljava/lang/String;)Ljava/net/URL;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        Label nonNullLabel = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, nonNullLabel);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(nonNullLabel);
        mv.visitTypeInsn(Opcodes.NEW, "java/net/URL");
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitLdcInsn(this.protocolPrefix);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/net/URL", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitTypeInsn(Opcodes.NEW, urlHandlerClass);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, urlHandlerClass, "<init>", "()V", false);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/net/URL", "<init>", "(Ljava/net/URL;Ljava/lang/String;Ljava/net/URLStreamHandler;)V", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(6, 3);
        mv.visitEnd();
    }
}
