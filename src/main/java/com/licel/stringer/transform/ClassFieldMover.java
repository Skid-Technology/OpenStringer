package com.licel.stringer.transform;

import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.model.MovedFieldInfo;
import java.util.HashMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassFieldMover extends ClassVisitor implements Opcodes {
    private final HashMap<String, MovedFieldInfo> staticFields = new HashMap<>();
    private final HashMap<String, MovedFieldInfo> instanceFields = new HashMap<>();
    private boolean hasConstructor = false;
    private boolean hasStaticInitializer = false;
    private String className;
    private final boolean moveAllStringFields;
    private final ClassInfo classInfo;

    public ClassFieldMover(ClassVisitor classVisitor, boolean annotationsOnly, ClassInfo classInfo) {
        super(Opcodes.ASM9, classVisitor);
        this.moveAllStringFields = annotationsOnly ? classInfo.annotatedSecured : true;
        this.classInfo = classInfo;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.cv.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        Object fieldValue = value;
        if (value instanceof String && (this.classInfo.securedStringFields.containsKey(name) || this.moveAllStringFields)) {
            MovedFieldInfo movedField = new MovedFieldInfo(access, name, descriptor, signature, (String) value);
            if ((access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) {
                this.staticFields.put(name, movedField);
            } else {
                this.instanceFields.put(name, movedField);
            }
            fieldValue = null;
        }
        return new FieldEncryptionAnnotationVisitor(this, this.cv.visitField(access, name, descriptor, signature, fieldValue));
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = this.cv.visitMethod(access, name, descriptor, signature, exceptions);
        if ("<clinit>".equals(name)) {
            this.hasStaticInitializer = true;
            mv = new FieldInitializationMethodVisitor(this, mv, this.className, this.staticFields, true);
        }
        if ("<init>".equals(name)) {
            this.hasConstructor = true;
            mv = new FieldInitializationMethodVisitor(this, mv, this.className, this.instanceFields, false);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        if (!this.hasStaticInitializer && !this.staticFields.isEmpty()) {
            MethodVisitor mv = this.cv.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            for (MovedFieldInfo field : this.staticFields.values()) {
                if (field.initialValue != null) {
                    mv.visitLdcInsn(field.initialValue);
                    mv.visitFieldInsn(Opcodes.PUTSTATIC, this.className, field.name, "Ljava/lang/String;");
                }
            }
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(4, 0);
            mv.visitEnd();
        }

        if (!this.hasConstructor && !this.instanceFields.isEmpty()) {
            MethodVisitor mv = this.cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            for (MovedFieldInfo field : this.instanceFields.values()) {
                if (field.initialValue != null) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitLdcInsn(field.initialValue);
                    mv.visitFieldInsn(Opcodes.PUTFIELD, this.className, field.name, "Ljava/lang/String;");
                }
            }
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        this.cv.visitEnd();
    }
}
