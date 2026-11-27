package com.licel.stringer.transform;

import com.licel.stringer.Worker;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ResourceReflectionMethodVisitor extends MethodVisitor implements Opcodes {
    private final ClassResourceReflection resourceReflection;
    private String lastLdcString;

    public ResourceReflectionMethodVisitor(ClassResourceReflection resourceReflection, MethodVisitor methodVisitor) {
        super(Opcodes.ASM9, methodVisitor);
        this.resourceReflection = resourceReflection;
        this.lastLdcString = null;
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (value instanceof String) {
            this.lastLdcString = (String) value;
        } else {
            this.lastLdcString = null;
        }
        this.mv.visitLdcInsn(value);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if ("java/lang/Class".equals(owner) && "getResourceAsStream".equals(name) && "(Ljava/lang/String;)Ljava/io/InputStream;".equals(descriptor)) {
            if (this.resourceReflection.classGetResourceAsStreamWrapper == null) {
                this.resourceReflection.classGetResourceAsStreamWrapper = Worker.get().nameGenerator.generateMethodName(
                        this.resourceReflection.classInfo, "(Ljava/lang/Class;Ljava/lang/String;)Ljava/io/InputStream;", null);
            }
            this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.resourceReflection.ownerClassName,
                    this.resourceReflection.classGetResourceAsStreamWrapper,
                    "(Ljava/lang/Class;Ljava/lang/String;)Ljava/io/InputStream;",
                    false);
        } else if ("java/lang/ClassLoader".equals(owner) && "getResourceAsStream".equals(name) && "(Ljava/lang/String;)Ljava/io/InputStream;".equals(descriptor)) {
            if (this.resourceReflection.classLoaderGetResourceAsStreamWrapper == null) {
                this.resourceReflection.classLoaderGetResourceAsStreamWrapper = Worker.get().nameGenerator.generateMethodName(
                        this.resourceReflection.classInfo, "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/io/InputStream;", null);
            }
            this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.resourceReflection.ownerClassName,
                    this.resourceReflection.classLoaderGetResourceAsStreamWrapper,
                    "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/io/InputStream;",
                    false);
        } else if ("javax/servlet/ServletContext".equals(owner) && "getResourceAsStream".equals(name) && "(Ljava/lang/String;)Ljava/io/InputStream;".equals(descriptor)) {
            if (this.resourceReflection.servletGetResourceAsStreamWrapper == null) {
                this.resourceReflection.servletGetResourceAsStreamWrapper = Worker.get().nameGenerator.generateMethodName(
                        this.resourceReflection.classInfo, "(Ljavax/servlet/ServletContext;Ljava/lang/String;)Ljava/io/InputStream;", null);
            }
            this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.resourceReflection.ownerClassName,
                    this.resourceReflection.servletGetResourceAsStreamWrapper,
                    "(Ljavax/servlet/ServletContext;Ljava/lang/String;)Ljava/io/InputStream;",
                    false);
        } else if ("java/lang/Class".equals(owner)
                && "getResource".equals(name)
                && "(Ljava/lang/String;)Ljava/net/URL;".equals(descriptor)
                && (this.lastLdcString == null || !this.lastLdcString.endsWith(".class"))) {
            if (this.resourceReflection.classGetResourceWrapper == null) {
                this.resourceReflection.classGetResourceWrapper = Worker.get().nameGenerator.generateMethodName(
                        this.resourceReflection.classInfo, "(Ljava/lang/Class;Ljava/lang/String;)Ljava/net/URL;", null);
            }
            this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.resourceReflection.ownerClassName,
                    this.resourceReflection.classGetResourceWrapper,
                    "(Ljava/lang/Class;Ljava/lang/String;)Ljava/net/URL;",
                    false);
        } else if ("java/lang/ClassLoader".equals(owner)
                && "getResource".equals(name)
                && "(Ljava/lang/String;)Ljava/net/URL;".equals(descriptor)
                && (this.lastLdcString == null || !this.lastLdcString.endsWith(".class"))) {
            if (this.resourceReflection.classLoaderGetResourceWrapper == null) {
                this.resourceReflection.classLoaderGetResourceWrapper = Worker.get().nameGenerator.generateMethodName(
                        this.resourceReflection.classInfo, "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/net/URL;", null);
            }
            this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.resourceReflection.ownerClassName,
                    this.resourceReflection.classLoaderGetResourceWrapper,
                    "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/net/URL;",
                    false);
        } else if ("javax/servlet/ServletContext".equals(owner)
                && "getResource".equals(name)
                && "(Ljava/lang/String;)Ljava/net/URL;".equals(descriptor)
                && (this.lastLdcString == null || !this.lastLdcString.endsWith(".class"))) {
            if (this.resourceReflection.servletGetResourceWrapper == null) {
                this.resourceReflection.servletGetResourceWrapper = Worker.get().nameGenerator.generateMethodName(
                        this.resourceReflection.classInfo, "(Ljavax/servlet/ServletContext;Ljava/lang/String;)Ljava/net/URL;", null);
            }
            this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.resourceReflection.ownerClassName,
                    this.resourceReflection.servletGetResourceWrapper,
                    "(Ljavax/servlet/ServletContext;Ljava/lang/String;)Ljava/net/URL;",
                    false);
        } else if ("java/net/URI".equals(owner)
                && "toURL".equals(name)
                && "()Ljava/net/URL;".equals(descriptor)
                && (this.lastLdcString == null || !this.lastLdcString.endsWith(".class"))) {
            if (this.resourceReflection.uriToUrlWrapper == null) {
                this.resourceReflection.uriToUrlWrapper = Worker.get().nameGenerator.generateMethodName(
                        this.resourceReflection.classInfo, "(Ljava/net/URI;)Ljava/net/URL;", null);
            }
            this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.resourceReflection.ownerClassName,
                    this.resourceReflection.uriToUrlWrapper,
                    "(Ljava/net/URI;)Ljava/net/URL;",
                    false);
        } else if ("java/net/URL".equals(owner)
                && "openStream".equals(name)
                && (this.lastLdcString == null || !this.lastLdcString.endsWith(".class"))) {
            if (this.resourceReflection.urlOpenStreamWrapper == null) {
                this.resourceReflection.urlOpenStreamWrapper = Worker.get().nameGenerator.generateMethodName(
                        this.resourceReflection.classInfo, "(Ljava/net/URL;)Ljava/io/InputStream;", null);
            }
            this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.resourceReflection.ownerClassName,
                    this.resourceReflection.urlOpenStreamWrapper,
                    "(Ljava/net/URL;)Ljava/io/InputStream;",
                    false);
        } else {
            this.mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }
}
