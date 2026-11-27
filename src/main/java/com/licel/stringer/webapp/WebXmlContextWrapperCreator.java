package com.licel.stringer.webapp;

import com.licel.stringer.model.ClassInfo;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class WebXmlContextWrapperCreator extends ClassVisitor implements Opcodes {
    public static final Properties METHOD_MAPPINGS = new Properties();
    public static final Properties FIELD_MAPPINGS = new Properties();
    public ClassInfo classInfo;
    public WebApplicationDescriptor webApplicationDescriptor;

    static {
        FIELD_MAPPINGS.put("ctx", "Ljavax/servlet/ServletContext;");
    }

    public WebXmlContextWrapperCreator(ClassVisitor classVisitor, ClassInfo classInfo) {
        super(Opcodes.ASM9, classVisitor);
        this.classInfo = classInfo;
        this.generate();
    }

    public void generate() {
        this.cv.visit(
                this.classInfo.classFileVersion,
                ACC_PUBLIC | ACC_SUPER,
                "com/licel/stringer/webapp/tomcat/ServletContextWrapper",
                null,
                this.classInfo.superName,
                new String[]{"java/lang/reflect/InvocationHandler"});

        FieldVisitor fv = this.cv.visitField(0, "ctx", "Ljavax/servlet/ServletContext;", null, null);
        fv.visitEnd();

        MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC, "<init>", "(Ljavax/servlet/ServletContext;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/webapp/tomcat/ServletContextWrapper", "ctx", "Ljavax/servlet/ServletContext;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = this.cv.visitMethod(
                ACC_PUBLIC,
                "invoke",
                "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                new String[]{"java/lang/Throwable"});
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "getName", "()Ljava/lang/String;", false);
        mv.visitLdcInsn("getResourceAsStream");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        Label delegateInvoke = new Label();
        mv.visitJumpInsn(IFEQ, delegateInvoke);
        mv.visitTypeInsn(NEW, "com/licel/stringer/StringerInputStreamPrototype");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ServletContextWrapper", "ctx", "Ljavax/servlet/ServletContext;");
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitTypeInsn(CHECKCAST, "java/io/InputStream");
        mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/StringerInputStreamPrototype", "<init>", "(Ljava/io/InputStream;)V", false);
        mv.visitInsn(ARETURN);
        mv.visitLabel(delegateInvoke);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "com/licel/stringer/webapp/tomcat/ServletContextWrapper", "ctx", "Ljavax/servlet/ServletContext;");
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(5, 4);
        mv.visitEnd();

        this.cv.visitEnd();
    }
}
