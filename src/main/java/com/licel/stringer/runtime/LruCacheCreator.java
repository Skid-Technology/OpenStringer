package com.licel.stringer.runtime;

import com.licel.stringer.model.ClassInfo;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class LruCacheCreator extends ClassVisitor implements Opcodes {
    public static final Properties METHOD_MAPPINGS = new Properties();
    public static final Properties FIELD_MAPPINGS = new Properties();
    private final ClassInfo classInfo;

    public LruCacheCreator(ClassVisitor classVisitor, ClassInfo classInfo) {
        super(Opcodes.ASM9, classVisitor);
        this.classInfo = classInfo;
        this.generate();
    }

    public void generate() {
        this.cv.visit(
                this.classInfo.classFileVersion,
                ACC_PUBLIC | ACC_SUPER,
                "com/licel/stringer/LruCachePrototype",
                "<K:Ljava/lang/Object;V:Ljava/lang/Object;>Ljava/lang/Object;",
                "java/lang/Object",
                null);
        this.cv.visitInnerClass("java/util/Map$Entry", "java/util/Map", "Entry", ACC_PUBLIC | ACC_STATIC | ACC_INTERFACE | ACC_ABSTRACT);
        FieldVisitor fv = this.cv.visitField(ACC_PRIVATE | ACC_FINAL, "map", "Ljava/util/LinkedHashMap;", "Ljava/util/LinkedHashMap<TK;TV;>;", null);
        fv.visitEnd();
        fv = this.cv.visitField(ACC_PRIVATE, "size", "I", null, null);
        fv.visitEnd();
        fv = this.cv.visitField(ACC_PRIVATE, "maxSize", "I", null, null);
        fv.visitEnd();
        fv = this.cv.visitField(ACC_PRIVATE, "putCount", "I", null, null);
        fv.visitEnd();
        fv = this.cv.visitField(ACC_PRIVATE, "createCount", "I", null, null);
        fv.visitEnd();
        fv = this.cv.visitField(ACC_PRIVATE, "evictionCount", "I", null, null);
        fv.visitEnd();
        fv = this.cv.visitField(ACC_PRIVATE, "hitCount", "I", null, null);
        fv.visitEnd();
        fv = this.cv.visitField(ACC_PRIVATE, "missCount", "I", null, null);
        fv.visitEnd();

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC, "<init>", "(I)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitVarInsn(ILOAD, 1);
            Label label0 = new Label();
            mv.visitJumpInsn(IFGT, label0);
            mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
            mv.visitInsn(DUP);
            mv.visitLdcInsn("maxSize <= 0");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);
            mv.visitLabel(label0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/LruCachePrototype", "maxSize", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(NEW, "java/util/LinkedHashMap");
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_0);
            mv.visitLdcInsn(0.75F);
            mv.visitInsn(ICONST_1);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedHashMap", "<init>", "(IFZ)V", false);
            mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/LruCachePrototype", "map", "Ljava/util/LinkedHashMap;");
            mv.visitInsn(RETURN);
            mv.visitMaxs(6, 2);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNCHRONIZED, "get", "(Ljava/lang/Object;)Ljava/lang/Object;", "(TK;)TV;", null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 1);
            Label label0 = new Label();
            mv.visitJumpInsn(IFNONNULL, label0);
            mv.visitTypeInsn(NEW, "java/lang/NullPointerException");
            mv.visitInsn(DUP);
            mv.visitLdcInsn("key == null");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);
            mv.visitLabel(label0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "map", "Ljava/util/LinkedHashMap;");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedHashMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 2);
            Label label1 = new Label();
            mv.visitJumpInsn(IFNULL, label1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "hitCount", "I");
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/LruCachePrototype", "hitCount", "I");
            mv.visitVarInsn(ALOAD, 2);
            mv.visitInsn(ARETURN);
            mv.visitLabel(label1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "missCount", "I");
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/LruCachePrototype", "missCount", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/licel/stringer/LruCachePrototype", "create", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 2);
            Label label2 = new Label();
            mv.visitJumpInsn(IFNULL, label2);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "createCount", "I");
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/LruCachePrototype", "createCount", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "size", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/LruCachePrototype", "safeSizeOf", "(Ljava/lang/Object;Ljava/lang/Object;)I", false);
            mv.visitInsn(IADD);
            mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/LruCachePrototype", "size", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "map", "Ljava/util/LinkedHashMap;");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedHashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitInsn(POP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "maxSize", "I");
            mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/LruCachePrototype", "trimToSize", "(I)V", false);
            mv.visitLabel(label2);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(5, 3);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNCHRONIZED, "put", "(Ljava/lang/Object;Ljava/lang/Object;)V", "(TK;TV;)V", null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 1);
            Label label0 = new Label();
            mv.visitJumpInsn(IFNULL, label0);
            mv.visitVarInsn(ALOAD, 2);
            Label label1 = new Label();
            mv.visitJumpInsn(IFNONNULL, label1);
            mv.visitLabel(label0);
            mv.visitTypeInsn(NEW, "java/lang/NullPointerException");
            mv.visitInsn(DUP);
            mv.visitLdcInsn("key == null || value == null");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);
            mv.visitLabel(label1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "putCount", "I");
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/LruCachePrototype", "putCount", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "size", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/LruCachePrototype", "safeSizeOf", "(Ljava/lang/Object;Ljava/lang/Object;)I", false);
            mv.visitInsn(IADD);
            mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/LruCachePrototype", "size", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "map", "Ljava/util/LinkedHashMap;");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedHashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitVarInsn(ASTORE, 3);
            mv.visitVarInsn(ALOAD, 3);
            Label label2 = new Label();
            mv.visitJumpInsn(IFNULL, label2);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "size", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/LruCachePrototype", "safeSizeOf", "(Ljava/lang/Object;Ljava/lang/Object;)I", false);
            mv.visitInsn(ISUB);
            mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/LruCachePrototype", "size", "I");
            mv.visitLabel(label2);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "maxSize", "I");
            mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/LruCachePrototype", "trimToSize", "(I)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(5, 4);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PRIVATE, "trimToSize", "(I)V", null, null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "size", "I");
            mv.visitVarInsn(ILOAD, 1);
            Label label1 = new Label();
            mv.visitJumpInsn(IF_ICMPLE, label1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "map", "Ljava/util/LinkedHashMap;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedHashMap", "isEmpty", "()Z", false);
            mv.visitJumpInsn(IFNE, label1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "map", "Ljava/util/LinkedHashMap;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedHashMap", "entrySet", "()Ljava/util/Set;", false);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "iterator", "()Ljava/util/Iterator;", true);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "java/util/Map$Entry");
            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 2);
            Label label2 = new Label();
            mv.visitJumpInsn(IFNONNULL, label2);
            mv.visitJumpInsn(GOTO, label1);
            mv.visitLabel(label2);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;", true);
            mv.visitVarInsn(ASTORE, 3);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;", true);
            mv.visitVarInsn(ASTORE, 4);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "map", "Ljava/util/LinkedHashMap;");
            mv.visitVarInsn(ALOAD, 3);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedHashMap", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitInsn(POP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "size", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/LruCachePrototype", "safeSizeOf", "(Ljava/lang/Object;Ljava/lang/Object;)I", false);
            mv.visitInsn(ISUB);
            mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/LruCachePrototype", "size", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "evictionCount", "I");
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/LruCachePrototype", "evictionCount", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitVarInsn(ALOAD, 4);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/licel/stringer/LruCachePrototype", "entryEvicted", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
            mv.visitJumpInsn(GOTO, label0);
            mv.visitLabel(label1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "size", "I");
            Label label3 = new Label();
            mv.visitJumpInsn(IFLT, label3);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "map", "Ljava/util/LinkedHashMap;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedHashMap", "isEmpty", "()Z", false);
            Label label4 = new Label();
            mv.visitJumpInsn(IFEQ, label4);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "size", "I");
            mv.visitJumpInsn(IFEQ, label4);
            mv.visitLabel(label3);
            mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
            mv.visitInsn(DUP);
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn(".sizeOf() is reporting inconsistent results!");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);
            mv.visitLabel(label4);
            mv.visitInsn(RETURN);
            mv.visitMaxs(5, 5);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNCHRONIZED, "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", "(TK;)TV;", null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 1);
            Label label0 = new Label();
            mv.visitJumpInsn(IFNONNULL, label0);
            mv.visitTypeInsn(NEW, "java/lang/NullPointerException");
            mv.visitInsn(DUP);
            mv.visitLdcInsn("key == null");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);
            mv.visitLabel(label0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "map", "Ljava/util/LinkedHashMap;");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedHashMap", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 2);
            Label label1 = new Label();
            mv.visitJumpInsn(IFNULL, label1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "size", "I");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/LruCachePrototype", "safeSizeOf", "(Ljava/lang/Object;Ljava/lang/Object;)I", false);
            mv.visitInsn(ISUB);
            mv.visitFieldInsn(PUTFIELD, "com/licel/stringer/LruCachePrototype", "size", "I");
            mv.visitLabel(label1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(5, 3);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PROTECTED, "entryEvicted", "(Ljava/lang/Object;Ljava/lang/Object;)V", "(TK;TV;)V", null);
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 3);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PROTECTED, "create", "(Ljava/lang/Object;)Ljava/lang/Object;", "(TK;)TV;", null);
            mv.visitCode();
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 2);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PRIVATE, "safeSizeOf", "(Ljava/lang/Object;Ljava/lang/Object;)I", "(TK;TV;)I", null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/licel/stringer/LruCachePrototype", "sizeOf", "(Ljava/lang/Object;Ljava/lang/Object;)I", false);
            mv.visitVarInsn(ISTORE, 3);
            mv.visitVarInsn(ILOAD, 3);
            Label label0 = new Label();
            mv.visitJumpInsn(IFGE, label0);
            mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
            mv.visitInsn(DUP);
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn("Negative size: ");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn("=");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);
            mv.visitLabel(label0);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(4, 4);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PROTECTED, "sizeOf", "(Ljava/lang/Object;Ljava/lang/Object;)I", "(TK;TV;)I", null);
            mv.visitCode();
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(1, 3);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNCHRONIZED, "evictAll", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_M1);
            mv.visitMethodInsn(INVOKESPECIAL, "com/licel/stringer/LruCachePrototype", "trimToSize", "(I)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNCHRONIZED, "size", "()I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "size", "I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNCHRONIZED, "maxSize", "()I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "maxSize", "I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNCHRONIZED, "hitCount", "()I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "hitCount", "I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNCHRONIZED, "missCount", "()I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "missCount", "I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNCHRONIZED, "createCount", "()I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "createCount", "I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNCHRONIZED, "putCount", "()I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "putCount", "I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNCHRONIZED, "evictionCount", "()I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "evictionCount", "I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC | ACC_FINAL | ACC_SYNCHRONIZED, "snapshot", "()Ljava/util/Map;", "()Ljava/util/Map<TK;TV;>;", null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, "java/util/LinkedHashMap");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, "com/licel/stringer/LruCachePrototype", "map", "Ljava/util/LinkedHashMap;");
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedHashMap", "<init>", "(Ljava/util/Map;)V", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(3, 1);
            mv.visitEnd();
        }

        this.cv.visitEnd();
    }
}
