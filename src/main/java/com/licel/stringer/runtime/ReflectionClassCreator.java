package com.licel.stringer.runtime;

import com.licel.stringer.crypto.CryptoContainer;
import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.model.MemberInfo;
import com.licel.stringer.model.MemberInfoComparator;
import com.licel.stringer.util.AsmUtils;
import com.licel.stringer.util.StringerUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ReflectionClassCreator extends ClassVisitor implements Opcodes {
    public static final Properties METHOD_MAPPINGS = new Properties();
    public static final Properties FIELD_MAPPINGS = new Properties();
    static final int RESOLVE_CLASS_SLOTS = 5;
    static final int MAX_CLASS_RESOLVER_CHUNK_SIZE = 5000;

    private final HashMap<String, String> methodNames = new HashMap<>();
    private final ClassInfo classInfo;
    private final CryptoContainer cryptoContainer;
    private final Properties mappingProperties;

    public ReflectionClassCreator(ClassVisitor cv, ClassInfo classInfo, Properties mappingProperties, CryptoContainer cryptoContainer) {
        super(Opcodes.ASM9, cv);
        this.classInfo = classInfo;
        this.cryptoContainer = cryptoContainer;
        this.methodNames.put("findClass", mappingProperties.getProperty("reflection.findClass"));
        this.methodNames.put("resolveClass", mappingProperties.getProperty("reflection.resolveClass"));

        for (int i = 1; i < 4; i++) {
            this.methodNames.put("resolveClass" + i, mappingProperties.getProperty("reflection.resolveClass" + i));
        }

        this.methodNames.put("findMethod", mappingProperties.getProperty("reflection.findMethod"));
        this.methodNames.put("findField", mappingProperties.getProperty("reflection.findField"));
        this.methodNames.put("invokeStatic", mappingProperties.getProperty("reflection.invokeStatic"));
        this.methodNames.put("invokeVirtual", mappingProperties.getProperty("reflection.invokeVirtual"));
        this.methodNames.put("construct", mappingProperties.getProperty("reflection.construct"));
        this.methodNames.put("getFieldValue", mappingProperties.getProperty("reflection.getFieldValue"));
        this.methodNames.put("setFieldValue", mappingProperties.getProperty("reflection.setFieldValue"));
        this.methodNames.put("getStaticFieldValue", mappingProperties.getProperty("reflection.getStaticFieldValue"));
        this.methodNames.put("setStaticFieldValue", mappingProperties.getProperty("reflection.setStaticFieldValue"));
        this.mappingProperties = mappingProperties;

        MemberInfo[] members = classInfo.hiddenMembers.values().toArray(new MemberInfo[classInfo.hiddenMembers.size()]);
        Arrays.sort(members, new MemberInfoComparator());
        Map<String, Integer> classIndices = classInfo.hiddenClassIndices;

        cv.visit(classInfo.classFileVersion, ACC_FINAL | ACC_SUPER, classInfo.name, null, classInfo.superName, null);

        FieldVisitor fv = cv.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, mappingProperties.getProperty("reflection.memberCache"), "[Ljava/lang/Object;", null, null);
        fv.visitEnd();
        fv = cv.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, mappingProperties.getProperty("reflection.classCache"), "[Ljava/lang/Class;", null, null);
        fv.visitEnd();
        fv = cv.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, mappingProperties.getProperty("reflection.memberHash"), "[I", null, null);
        fv.visitEnd();
        fv = cv.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, mappingProperties.getProperty("reflection.classIds"), "[S", null, null);
        fv.visitEnd();
        fv = cv.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, mappingProperties.getProperty("reflection.keyX"), "I", null, null);
        fv.visitEnd();
        fv = cv.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, mappingProperties.getProperty("reflection.keyA"), "I", null, null);
        fv.visitEnd();

        this.emitStaticInitializer(cv, members, classIndices);
        this.emitDefaultConstructor(cv);
        this.emitFindClassMethod(cv, classIndices.size());
        this.emitResolveClassMethods(cv, classIndices);
        this.emitFindMethodMethod(cv);
        this.emitInvokeStaticMethod(cv);
        this.emitInvokeVirtualMethod(cv);
        this.emitConstructMethod(cv);
        this.emitFindFieldMethod(cv);
        this.emitGetFieldValueMethod(cv);
        this.emitSetFieldValueMethod(cv);
        this.emitGetStaticFieldValueMethod(cv);
        this.emitSetStaticFieldValueMethod(cv);
        cv.visitEnd();
    }

    private void emitStaticInitializer(ClassVisitor cv, MemberInfo[] members, Map<String, Integer> classIndices) {
        MethodVisitor mv = AsmUtils.visitMethod(cv, ACC_STATIC, "<clinit>", "()V", null, null, ClassWriter.COMPUTE_MAXS);
        mv.visitCode();
        int[] obfuscationKeys = this.cryptoContainer.getIdObfuscationKeys();
        mv.visitLdcInsn(String.valueOf(obfuscationKeys[3]));
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
        mv.visitFieldInsn(PUTSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.keyX"), "I");
        mv.visitLdcInsn(String.valueOf(obfuscationKeys[0]));
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
        mv.visitFieldInsn(PUTSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.keyA"), "I");
        StringerUtils.pushIntConstant(mv, members.length);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitFieldInsn(PUTSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.memberCache"), "[Ljava/lang/Object;");
        StringerUtils.pushIntConstant(mv, classIndices.size());
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
        mv.visitFieldInsn(PUTSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.classCache"), "[Ljava/lang/Class;");
        StringerUtils.pushIntConstant(mv, members.length);
        mv.visitIntInsn(NEWARRAY, T_INT);
        mv.visitFieldInsn(PUTSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.memberHash"), "[I");
        char[] memberHashChars = new char[members.length * 2];
        for (MemberInfo member : members) {
            memberHashChars[member.getReferenceIndex() * 2] = (char)member.encodedId;
            memberHashChars[member.getReferenceIndex() * 2 + 1] = (char)((member.encodedId & -65536) >> 16);
        }
        mv.visitLdcInsn(new String(memberHashChars));
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false);
        mv.visitVarInsn(ASTORE, 100);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 1);
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(ILOAD, 1);
        StringerUtils.pushIntConstant(mv, members.length);
        Label l1 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l1);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.memberHash"), "[I");
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ALOAD, 100);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(ICONST_2);
        mv.visitInsn(IMUL);
        mv.visitInsn(CALOAD);
        mv.visitVarInsn(ALOAD, 100);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(ICONST_2);
        mv.visitInsn(IMUL);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IADD);
        mv.visitInsn(CALOAD);
        mv.visitIntInsn(BIPUSH, 16);
        mv.visitInsn(ISHL);
        mv.visitInsn(IOR);
        mv.visitInsn(IASTORE);
        mv.visitIincInsn(1, 1);
        mv.visitJumpInsn(GOTO, l0);
        mv.visitLabel(l1);
        StringerUtils.pushIntConstant(mv, members.length);
        mv.visitIntInsn(NEWARRAY, T_SHORT);
        mv.visitFieldInsn(PUTSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.classIds"), "[S");
        char[] classIdChars = new char[members.length];
        for (MemberInfo member : members) {
            classIdChars[member.getReferenceIndex()] = (char)((classIndices.size() - member.referenceMask % classIndices.size() + member.ownerClassIndex) % classIndices.size());
        }
        mv.visitLdcInsn(new String(classIdChars));
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false);
        mv.visitVarInsn(ASTORE, 100);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 1);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitVarInsn(ILOAD, 1);
        StringerUtils.pushIntConstant(mv, members.length);
        Label l3 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l3);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.classIds"), "[S");
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ALOAD, 100);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(CALOAD);
        mv.visitInsn(I2S);
        mv.visitInsn(SASTORE);
        mv.visitIincInsn(1, 1);
        mv.visitJumpInsn(GOTO, l2);
        mv.visitLabel(l3);
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 2);
        mv.visitEnd();
    }

    private void emitDefaultConstructor(ClassVisitor cv) {
        MethodVisitor mv = cv.visitMethod(0, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, this.classInfo.superName, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private void emitFindClassMethod(ClassVisitor cv, int classCount) {
        MethodVisitor mv = AsmUtils.visitMethod(
                cv,
                ACC_PRIVATE | ACC_STATIC,
                this.methodNames.get("findClass"),
                "(II)Ljava/lang/Class;",
                null,
                null,
                ClassWriter.COMPUTE_MAXS);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.classIds"), "[S");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitInsn(SALOAD);
        mv.visitLdcInsn(65535);
        mv.visitInsn(IAND);
        mv.visitVarInsn(ISTORE, 2);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(IADD);
        StringerUtils.pushIntConstant(mv, classCount);
        mv.visitInsn(IREM);
        mv.visitVarInsn(ISTORE, 2);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.classCache"), "[Ljava/lang/Class;");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitVarInsn(ALOAD, 3);
        Label l0 = new Label();
        mv.visitJumpInsn(IFNULL, l0);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l0);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitMethodInsn(INVOKESTATIC, this.classInfo.name, this.methodNames.get("resolveClass"), "(I)Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.classCache"), "[Ljava/lang/Class;");
        mv.visitVarInsn(ILOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitInsn(AASTORE);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 4);
        mv.visitEnd();
    }

    private void emitResolveClassMethods(ClassVisitor cv, Map<String, Integer> classIndices) {
        int classCount = classIndices.size();
        HashMap<Integer, String> indexToClassName = new HashMap<>();

        for (Entry<String, Integer> entry : classIndices.entrySet()) {
            indexToClassName.put(entry.getValue(), entry.getKey());
        }

        if (classCount < MAX_CLASS_RESOLVER_CHUNK_SIZE) {
            this.emitResolveClassChunk(cv, indexToClassName, this.methodNames.get("resolveClass"), null, 0, classCount);
        } else {
            int chunkIndex = 1;
            this.emitResolveClassChunk(
                    cv,
                    indexToClassName,
                    this.methodNames.get("resolveClass"),
                    MAX_CLASS_RESOLVER_CHUNK_SIZE < classCount ? this.methodNames.get("resolveClass1") : null,
                    0,
                    MAX_CLASS_RESOLVER_CHUNK_SIZE);

            for (int totalChunks = classCount / MAX_CLASS_RESOLVER_CHUNK_SIZE; chunkIndex < totalChunks; chunkIndex++) {
                this.emitResolveClassChunk(
                        cv,
                        indexToClassName,
                        this.methodNames.get("resolveClass" + chunkIndex),
                        (chunkIndex + 1) * MAX_CLASS_RESOLVER_CHUNK_SIZE < classCount
                                ? this.methodNames.get("resolveClass" + (chunkIndex + 1))
                                : null,
                        chunkIndex * MAX_CLASS_RESOLVER_CHUNK_SIZE,
                        MAX_CLASS_RESOLVER_CHUNK_SIZE);
            }

            int remainder = classCount % MAX_CLASS_RESOLVER_CHUNK_SIZE;
            if (remainder != 0) {
                this.emitResolveClassChunk(
                        cv,
                        indexToClassName,
                        this.methodNames.get("resolveClass" + chunkIndex),
                        null,
                        classCount - remainder,
                        remainder);
            }
        }
    }

    private void emitResolveClassChunk(
            ClassVisitor cv,
            Map<Integer, String> indexToClassName,
            String methodName,
            String nextMethodName,
            int startIndex,
            int count) {
        MethodVisitor mv = AsmUtils.visitMethod(
                cv,
                ACC_PRIVATE | ACC_STATIC,
                methodName,
                "(I)Ljava/lang/Class;",
                null,
                null,
                ClassWriter.COMPUTE_MAXS);
        mv.visitCode();
        mv.visitVarInsn(ILOAD, 0);
        int caseCount = count;
        Label[] caseLabels = new Label[caseCount];
        for (int i = 0; i < caseCount; i++) {
            caseLabels[i] = new Label();
        }
        Label l0 = new Label();
        Label l1 = new Label();
        mv.visitTableSwitchInsn(startIndex, startIndex + caseCount - 1, l0, caseLabels);
        for (int i = 0; i < caseCount; i++) {
            mv.visitLabel(caseLabels[i]);
            mv.visitLdcInsn(this.toBinaryClassName(indexToClassName.get(startIndex + i)));
            mv.visitVarInsn(ASTORE, 1);
            mv.visitJumpInsn(GOTO, l1);
        }
        mv.visitLabel(l0);
        if (nextMethodName == null) {
            mv.visitTypeInsn(NEW, "java/lang/NoClassDefFoundError");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ILOAD, 0);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NoClassDefFoundError", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);
        } else {
            mv.visitVarInsn(ILOAD, 0);
            mv.visitMethodInsn(INVOKESTATIC, this.classInfo.name, nextMethodName, "(I)Ljava/lang/Class;", false);
            mv.visitInsn(ARETURN);
        }
        mv.visitLabel(l1);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 2);
        mv.visitEnd();
    }

    private String toBinaryClassName(String internalName) {
        return internalName.replace('/', '.');
    }

    private void emitFindMethodMethod(ClassVisitor cv) {
        MethodVisitor mv = AsmUtils.visitMethod(
                cv,
                ACC_STATIC,
                this.methodNames.get("findMethod"),
                "(I)Ljava/lang/reflect/Method;",
                null,
                null,
                ClassWriter.COMPUTE_MAXS);
        mv.visitCode();
        mv.visitVarInsn(ILOAD, 0);
        int[] obfuscationKeys = this.cryptoContainer.getIdObfuscationKeys();
        int[] obfuscationSigns = this.cryptoContainer.getIdObfuscationSigns();
        StringerUtils.pushIntConstant(mv, obfuscationKeys[4]);
        mv.visitInsn(obfuscationSigns[2] == 0 ? ISUB : IADD);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.keyX"), "I");
        mv.visitInsn(IXOR);
        StringerUtils.pushIntConstant(mv, obfuscationKeys[2]);
        mv.visitInsn(obfuscationSigns[1] == 0 ? ISUB : IADD);
        StringerUtils.pushIntConstant(mv, obfuscationKeys[1]);
        mv.visitInsn(IXOR);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.keyA"), "I");
        mv.visitInsn(obfuscationSigns[0] == 0 ? ISUB : IADD);
        mv.visitVarInsn(ISTORE, 0);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitIntInsn(BIPUSH, 16);
        mv.visitInsn(IUSHR);
        mv.visitVarInsn(ISTORE, 1);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitLdcInsn(65535);
        mv.visitInsn(IAND);
        mv.visitVarInsn(ISTORE, 0);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.memberCache"), "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitInsn(AALOAD);
        mv.visitTypeInsn(CHECKCAST, "java/lang/reflect/Method");
        mv.visitVarInsn(ASTORE, 2);
        mv.visitVarInsn(ALOAD, 2);
        Label l0 = new Label();
        mv.visitJumpInsn(IFNULL, l0);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l0);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC, this.classInfo.name, this.methodNames.get("findClass"), "(II)Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitVarInsn(ASTORE, 4);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.memberHash"), "[I");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitInsn(IALOAD);
        mv.visitVarInsn(ISTORE, 5);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitVarInsn(ALOAD, 3);
        Label l2 = new Label();
        mv.visitJumpInsn(IFNULL, l2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "isInterface", "()Z", false);
        Label l3 = new Label();
        mv.visitJumpInsn(IFEQ, l3);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethods", "()[Ljava/lang/reflect/Method;", false);
        Label l4 = new Label();
        mv.visitJumpInsn(GOTO, l4);
        mv.visitLabel(l3);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethods", "()[Ljava/lang/reflect/Method;", false);
        mv.visitLabel(l4);
        mv.visitVarInsn(ASTORE, 6);
        mv.visitVarInsn(ALOAD, 6);
        mv.visitVarInsn(ASTORE, 7);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitVarInsn(ISTORE, 8);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 9);
        Label l5 = new Label();
        mv.visitLabel(l5);
        mv.visitVarInsn(ILOAD, 9);
        mv.visitVarInsn(ILOAD, 8);
        Label l6 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l6);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitVarInsn(ILOAD, 9);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, 10);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 11);
        mv.visitInsn(IMUL);
        mv.visitIntInsn(BIPUSH, 40);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "getParameterTypes", "()[Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 12);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 13);
        Label l7 = new Label();
        mv.visitLabel(l7);
        mv.visitVarInsn(ILOAD, 13);
        mv.visitVarInsn(ALOAD, 12);
        mv.visitInsn(ARRAYLENGTH);
        Label l8 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l8);
        mv.visitVarInsn(ALOAD, 12);
        mv.visitVarInsn(ILOAD, 13);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, 14);
        mv.visitVarInsn(ILOAD, 13);
        Label l9 = new Label();
        mv.visitJumpInsn(IFEQ, l9);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 11);
        mv.visitInsn(IMUL);
        mv.visitIntInsn(BIPUSH, 44);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitLabel(l9);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 11);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ALOAD, 14);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitIincInsn(13, 1);
        mv.visitJumpInsn(GOTO, l7);
        mv.visitLabel(l8);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 11);
        mv.visitInsn(IMUL);
        mv.visitIntInsn(BIPUSH, 41);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 11);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "getReturnType", "()Ljava/lang/Class;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 11);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitVarInsn(ILOAD, 5);
        mv.visitVarInsn(ILOAD, 11);
        Label l10 = new Label();
        mv.visitJumpInsn(IF_ICMPNE, l10);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitInsn(ICONST_1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "setAccessible", "(Z)V", false);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.memberCache"), "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitInsn(AASTORE);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l10);
        mv.visitIincInsn(9, 1);
        mv.visitJumpInsn(GOTO, l5);
        mv.visitLabel(l6);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getSuperclass", "()Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitJumpInsn(GOTO, l1);
        mv.visitLabel(l2);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitVarInsn(ASTORE, 3);
        Label l11 = new Label();
        mv.visitLabel(l11);
        mv.visitVarInsn(ALOAD, 3);
        Label l12 = new Label();
        mv.visitJumpInsn(IFNULL, l12);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getInterfaces", "()[Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 6);
        mv.visitVarInsn(ALOAD, 6);
        mv.visitVarInsn(ASTORE, 7);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitVarInsn(ISTORE, 8);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 9);
        Label l13 = new Label();
        mv.visitLabel(l13);
        mv.visitVarInsn(ILOAD, 9);
        mv.visitVarInsn(ILOAD, 8);
        Label l14 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l14);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitVarInsn(ILOAD, 9);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, 10);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethods", "()[Ljava/lang/reflect/Method;", false);
        mv.visitVarInsn(ASTORE, 11);
        mv.visitVarInsn(ALOAD, 11);
        mv.visitVarInsn(ASTORE, 12);
        mv.visitVarInsn(ALOAD, 12);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitVarInsn(ISTORE, 13);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 14);
        Label l15 = new Label();
        mv.visitLabel(l15);
        mv.visitVarInsn(ILOAD, 14);
        mv.visitVarInsn(ILOAD, 13);
        Label l16 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l16);
        mv.visitVarInsn(ALOAD, 12);
        mv.visitVarInsn(ILOAD, 14);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, 15);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ALOAD, 15);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 16);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 16);
        mv.visitInsn(IMUL);
        mv.visitIntInsn(BIPUSH, 40);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 16);
        mv.visitVarInsn(ALOAD, 15);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "getParameterTypes", "()[Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 17);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 18);
        Label l17 = new Label();
        mv.visitLabel(l17);
        mv.visitVarInsn(ILOAD, 18);
        mv.visitVarInsn(ALOAD, 17);
        mv.visitInsn(ARRAYLENGTH);
        Label l18 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l18);
        mv.visitVarInsn(ALOAD, 17);
        mv.visitVarInsn(ILOAD, 18);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, 19);
        mv.visitVarInsn(ILOAD, 18);
        Label l19 = new Label();
        mv.visitJumpInsn(IFEQ, l19);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 16);
        mv.visitInsn(IMUL);
        mv.visitIntInsn(BIPUSH, 44);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 16);
        mv.visitLabel(l19);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 16);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ALOAD, 19);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 16);
        mv.visitIincInsn(18, 1);
        mv.visitJumpInsn(GOTO, l17);
        mv.visitLabel(l18);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 16);
        mv.visitInsn(IMUL);
        mv.visitIntInsn(BIPUSH, 41);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 16);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 16);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ALOAD, 15);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "getReturnType", "()Ljava/lang/Class;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 16);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 16);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 16);
        mv.visitVarInsn(ILOAD, 5);
        mv.visitVarInsn(ILOAD, 16);
        Label l20 = new Label();
        mv.visitJumpInsn(IF_ICMPNE, l20);
        mv.visitVarInsn(ALOAD, 15);
        mv.visitInsn(ICONST_1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "setAccessible", "(Z)V", false);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.memberCache"), "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitVarInsn(ALOAD, 15);
        mv.visitInsn(AASTORE);
        mv.visitVarInsn(ALOAD, 15);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l20);
        mv.visitIincInsn(14, 1);
        mv.visitJumpInsn(GOTO, l15);
        mv.visitLabel(l16);
        mv.visitIincInsn(9, 1);
        mv.visitJumpInsn(GOTO, l13);
        mv.visitLabel(l14);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getSuperclass", "()Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitJumpInsn(GOTO, l11);
        mv.visitLabel(l12);
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 20);
        mv.visitEnd();
    }

    private void emitInvokeStaticMethod(ClassVisitor cv) {
        MethodVisitor mv = AsmUtils.visitMethod(
                cv,
                ACC_STATIC,
                this.methodNames.get("invokeStatic"),
                "(I[Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null,
                ClassWriter.COMPUTE_MAXS);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, "java/lang/reflect/InvocationTargetException");
        mv.visitLabel(l0);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, this.classInfo.name, this.methodNames.get("findMethod"), "(I)Ljava/lang/reflect/Method;", false);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitVarInsn(ALOAD, 2);
        Label l3 = new Label();
        mv.visitJumpInsn(IFNONNULL, l3);
        mv.visitTypeInsn(NEW, "java/lang/NoSuchMethodError");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NoSuchMethodError", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
        mv.visitLabel(l3);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitLabel(l1);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l2);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/InvocationTargetException", "getTargetException", "()Ljava/lang/Throwable;", false);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    private void emitInvokeVirtualMethod(ClassVisitor cv) {
        MethodVisitor mv = AsmUtils.visitMethod(
                cv,
                ACC_STATIC,
                this.methodNames.get("invokeVirtual"),
                "(Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null,
                ClassWriter.COMPUTE_MAXS);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, "java/lang/reflect/InvocationTargetException");
        mv.visitLabel(l0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC, this.classInfo.name, this.methodNames.get("findMethod"), "(I)Ljava/lang/reflect/Method;", false);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitVarInsn(ALOAD, 3);
        Label l3 = new Label();
        mv.visitJumpInsn(IFNONNULL, l3);
        mv.visitTypeInsn(NEW, "java/lang/NoSuchMethodError");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NoSuchMethodError", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
        mv.visitLabel(l3);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitLabel(l1);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l2);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/InvocationTargetException", "getTargetException", "()Ljava/lang/Throwable;", false);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(3, 4);
        mv.visitEnd();
    }

    private void emitConstructMethod(ClassVisitor cv) {
        MethodVisitor mv = AsmUtils.visitMethod(
                cv,
                ACC_STATIC,
                this.methodNames.get("construct"),
                "(I[Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null,
                ClassWriter.COMPUTE_MAXS);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, "java/lang/reflect/InvocationTargetException");
        mv.visitLabel(l0);
        mv.visitVarInsn(ILOAD, 0);
        int[] obfuscationKeys = this.cryptoContainer.getIdObfuscationKeys();
        int[] obfuscationSigns = this.cryptoContainer.getIdObfuscationSigns();
        StringerUtils.pushIntConstant(mv, obfuscationKeys[4]);
        mv.visitInsn(obfuscationSigns[2] == 0 ? ISUB : IADD);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.keyX"), "I");
        mv.visitInsn(IXOR);
        StringerUtils.pushIntConstant(mv, obfuscationKeys[2]);
        mv.visitInsn(obfuscationSigns[1] == 0 ? ISUB : IADD);
        StringerUtils.pushIntConstant(mv, obfuscationKeys[1]);
        mv.visitInsn(IXOR);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.keyA"), "I");
        mv.visitInsn(obfuscationSigns[0] == 0 ? ISUB : IADD);
        mv.visitVarInsn(ISTORE, 0);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitIntInsn(BIPUSH, 16);
        mv.visitInsn(IUSHR);
        mv.visitVarInsn(ISTORE, 2);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitLdcInsn(65535);
        mv.visitInsn(IAND);
        mv.visitVarInsn(ISTORE, 0);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitMethodInsn(INVOKESTATIC, this.classInfo.name, this.methodNames.get("findClass"), "(II)Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.memberCache"), "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitInsn(AALOAD);
        mv.visitTypeInsn(CHECKCAST, "java/lang/reflect/Constructor");
        mv.visitVarInsn(ASTORE, 4);
        mv.visitVarInsn(ALOAD, 4);
        Label l3 = new Label();
        mv.visitJumpInsn(IFNONNULL, l3);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.memberHash"), "[I");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitInsn(IALOAD);
        mv.visitVarInsn(ISTORE, 5);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredConstructors", "()[Ljava/lang/reflect/Constructor;", false);
        mv.visitVarInsn(ASTORE, 6);
        mv.visitVarInsn(ALOAD, 6);
        mv.visitVarInsn(ASTORE, 7);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitVarInsn(ISTORE, 8);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 9);
        Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitVarInsn(ILOAD, 9);
        mv.visitVarInsn(ILOAD, 8);
        mv.visitJumpInsn(IF_ICMPGE, l3);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitVarInsn(ILOAD, 9);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, 10);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitInsn(IMUL);
        mv.visitIntInsn(BIPUSH, 40);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Constructor", "getParameterTypes", "()[Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 12);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 13);
        Label l5 = new Label();
        mv.visitLabel(l5);
        mv.visitVarInsn(ILOAD, 13);
        mv.visitVarInsn(ALOAD, 12);
        mv.visitInsn(ARRAYLENGTH);
        Label l6 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l6);
        mv.visitVarInsn(ALOAD, 12);
        mv.visitVarInsn(ILOAD, 13);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, 14);
        mv.visitVarInsn(ILOAD, 13);
        Label l7 = new Label();
        mv.visitJumpInsn(IFEQ, l7);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 11);
        mv.visitInsn(IMUL);
        mv.visitIntInsn(BIPUSH, 44);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitLabel(l7);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 11);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ALOAD, 14);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitIincInsn(13, 1);
        mv.visitJumpInsn(GOTO, l5);
        mv.visitLabel(l6);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 11);
        mv.visitInsn(IMUL);
        mv.visitIntInsn(BIPUSH, 41);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 11);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitVarInsn(ILOAD, 5);
        mv.visitVarInsn(ILOAD, 11);
        Label l8 = new Label();
        mv.visitJumpInsn(IF_ICMPNE, l8);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitInsn(ICONST_1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Constructor", "setAccessible", "(Z)V", false);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.memberCache"), "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitInsn(AASTORE);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitVarInsn(ASTORE, 4);
        mv.visitJumpInsn(GOTO, l3);
        mv.visitLabel(l8);
        mv.visitIincInsn(9, 1);
        mv.visitJumpInsn(GOTO, l4);
        mv.visitLabel(l3);
        mv.visitVarInsn(ALOAD, 4);
        Label l9 = new Label();
        mv.visitJumpInsn(IFNONNULL, l9);
        mv.visitTypeInsn(NEW, "java/lang/NoSuchMethodError");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NoSuchMethodError", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
        mv.visitLabel(l9);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Constructor", "newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitLabel(l1);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l2);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/InvocationTargetException", "getTargetException", "()Ljava/lang/Throwable;", false);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(3, 15);
        mv.visitEnd();
    }

    private void emitFindFieldMethod(ClassVisitor cv) {
        MethodVisitor mv = AsmUtils.visitMethod(
                cv,
                ACC_PRIVATE | ACC_STATIC,
                this.methodNames.get("findField"),
                "(I)Ljava/lang/reflect/Field;",
                null,
                new String[]{"java/lang/Throwable"},
                ClassWriter.COMPUTE_MAXS);
        mv.visitCode();
        mv.visitVarInsn(ILOAD, 0);
        int[] obfuscationKeys = this.cryptoContainer.getIdObfuscationKeys();
        int[] obfuscationSigns = this.cryptoContainer.getIdObfuscationSigns();
        StringerUtils.pushIntConstant(mv, obfuscationKeys[4]);
        mv.visitInsn(obfuscationSigns[2] == 0 ? ISUB : IADD);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.keyX"), "I");
        mv.visitInsn(IXOR);
        StringerUtils.pushIntConstant(mv, obfuscationKeys[2]);
        mv.visitInsn(obfuscationSigns[1] == 0 ? ISUB : IADD);
        StringerUtils.pushIntConstant(mv, obfuscationKeys[1]);
        mv.visitInsn(IXOR);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.keyA"), "I");
        mv.visitInsn(obfuscationSigns[0] == 0 ? ISUB : IADD);
        mv.visitVarInsn(ISTORE, 0);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitIntInsn(BIPUSH, 16);
        mv.visitInsn(IUSHR);
        mv.visitVarInsn(ISTORE, 1);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitLdcInsn(65535);
        mv.visitInsn(IAND);
        mv.visitVarInsn(ISTORE, 0);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC, this.classInfo.name, this.methodNames.get("findClass"), "(II)Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.memberCache"), "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitInsn(AALOAD);
        mv.visitTypeInsn(CHECKCAST, "java/lang/reflect/Field");
        mv.visitVarInsn(ASTORE, 4);
        mv.visitVarInsn(ALOAD, 4);
        Label l0 = new Label();
        mv.visitJumpInsn(IFNULL, l0);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l0);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.memberHash"), "[I");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitInsn(IALOAD);
        mv.visitVarInsn(ISTORE, 5);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitVarInsn(ALOAD, 2);
        Label l2 = new Label();
        mv.visitJumpInsn(IFNULL, l2);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredFields", "()[Ljava/lang/reflect/Field;", false);
        mv.visitVarInsn(ASTORE, 6);
        mv.visitVarInsn(ALOAD, 6);
        mv.visitVarInsn(ASTORE, 7);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitVarInsn(ISTORE, 8);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 9);
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitVarInsn(ILOAD, 9);
        mv.visitVarInsn(ILOAD, 8);
        Label l4 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l4);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitVarInsn(ILOAD, 9);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, 10);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 11);
        mv.visitInsn(IMUL);
        mv.visitIntInsn(BIPUSH, 58);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 11);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "getType", "()Ljava/lang/Class;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 11);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 11);
        mv.visitVarInsn(ILOAD, 5);
        mv.visitVarInsn(ILOAD, 11);
        Label l5 = new Label();
        mv.visitJumpInsn(IF_ICMPNE, l5);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitInsn(ICONST_1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.memberCache"), "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitInsn(AASTORE);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l5);
        mv.visitIincInsn(9, 1);
        mv.visitJumpInsn(GOTO, l3);
        mv.visitLabel(l4);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getSuperclass", "()Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitJumpInsn(GOTO, l1);
        mv.visitLabel(l2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitVarInsn(ASTORE, 2);
        Label l6 = new Label();
        mv.visitLabel(l6);
        mv.visitVarInsn(ALOAD, 2);
        Label l7 = new Label();
        mv.visitJumpInsn(IFNULL, l7);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getInterfaces", "()[Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 6);
        mv.visitVarInsn(ALOAD, 6);
        mv.visitVarInsn(ASTORE, 7);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitVarInsn(ISTORE, 8);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 9);
        Label l8 = new Label();
        mv.visitLabel(l8);
        mv.visitVarInsn(ILOAD, 9);
        mv.visitVarInsn(ILOAD, 8);
        Label l9 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l9);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitVarInsn(ILOAD, 9);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, 10);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getFields", "()[Ljava/lang/reflect/Field;", false);
        mv.visitVarInsn(ASTORE, 11);
        mv.visitVarInsn(ALOAD, 11);
        mv.visitVarInsn(ASTORE, 12);
        mv.visitVarInsn(ALOAD, 12);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitVarInsn(ISTORE, 13);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 14);
        Label l10 = new Label();
        mv.visitLabel(l10);
        mv.visitVarInsn(ILOAD, 14);
        mv.visitVarInsn(ILOAD, 13);
        Label l11 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l11);
        mv.visitVarInsn(ALOAD, 12);
        mv.visitVarInsn(ILOAD, 14);
        mv.visitInsn(AALOAD);
        mv.visitVarInsn(ASTORE, 15);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ALOAD, 15);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 16);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 16);
        mv.visitInsn(IMUL);
        mv.visitIntInsn(BIPUSH, 58);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 16);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 16);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ALOAD, 15);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "getType", "()Ljava/lang/Class;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 16);
        mv.visitIntInsn(BIPUSH, 31);
        mv.visitVarInsn(ILOAD, 16);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 16);
        mv.visitVarInsn(ILOAD, 5);
        mv.visitVarInsn(ILOAD, 16);
        Label l12 = new Label();
        mv.visitJumpInsn(IF_ICMPNE, l12);
        mv.visitVarInsn(ALOAD, 15);
        mv.visitInsn(ICONST_1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);
        mv.visitFieldInsn(GETSTATIC, this.classInfo.name, this.mappingProperties.getProperty("reflection.memberCache"), "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitVarInsn(ALOAD, 15);
        mv.visitInsn(AASTORE);
        mv.visitVarInsn(ALOAD, 15);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l12);
        mv.visitIincInsn(14, 1);
        mv.visitJumpInsn(GOTO, l10);
        mv.visitLabel(l11);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getSuperclass", "()Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitIincInsn(9, 1);
        mv.visitJumpInsn(GOTO, l8);
        mv.visitLabel(l9);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getSuperclass", "()Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitJumpInsn(GOTO, l6);
        mv.visitLabel(l7);
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(4, 18);
        mv.visitEnd();
    }

    private void emitGetFieldValueMethod(ClassVisitor cv) {
        MethodVisitor mv = AsmUtils.visitMethod(
                cv,
                ACC_STATIC,
                this.methodNames.get("getFieldValue"),
                "(Ljava/lang/Object;I)Ljava/lang/Object;",
                null,
                null,
                ClassWriter.COMPUTE_MAXS);
        mv.visitCode();
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC, this.classInfo.name, this.methodNames.get("findField"), "(I)Ljava/lang/reflect/Field;", false);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitVarInsn(ALOAD, 2);
        Label l0 = new Label();
        mv.visitJumpInsn(IFNONNULL, l0);
        mv.visitTypeInsn(NEW, "java/lang/NoSuchFieldError");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NoSuchFieldError", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    private void emitSetFieldValueMethod(ClassVisitor cv) {
        MethodVisitor mv = AsmUtils.visitMethod(
                cv,
                ACC_STATIC,
                this.methodNames.get("setFieldValue"),
                "(Ljava/lang/Object;ILjava/lang/Object;)V",
                null,
                new String[]{"java/lang/Throwable"},
                ClassWriter.COMPUTE_MAXS);
        mv.visitCode();
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC, this.classInfo.name, this.methodNames.get("findField"), "(I)Ljava/lang/reflect/Field;", false);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitVarInsn(ALOAD, 3);
        Label l0 = new Label();
        mv.visitJumpInsn(IFNONNULL, l0);
        mv.visitTypeInsn(NEW, "java/lang/NoSuchFieldError");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NoSuchFieldError", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "set", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 4);
        mv.visitEnd();
    }

    private void emitGetStaticFieldValueMethod(ClassVisitor cv) {
        MethodVisitor mv = AsmUtils.visitMethod(
                cv,
                ACC_STATIC,
                this.methodNames.get("getStaticFieldValue"),
                "(I)Ljava/lang/Object;",
                null,
                null,
                ClassWriter.COMPUTE_MAXS);
        mv.visitCode();
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, this.classInfo.name, this.methodNames.get("getFieldValue"), "(Ljava/lang/Object;I)Ljava/lang/Object;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    private void emitSetStaticFieldValueMethod(ClassVisitor cv) {
        MethodVisitor mv = AsmUtils.visitMethod(
                cv,
                ACC_STATIC,
                this.methodNames.get("setStaticFieldValue"),
                "(ILjava/lang/Object;)V",
                null,
                null,
                ClassWriter.COMPUTE_MAXS);
        mv.visitCode();
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC, this.classInfo.name, this.methodNames.get("setFieldValue"), "(Ljava/lang/Object;ILjava/lang/Object;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 2);
        mv.visitEnd();
    }

    static {
        METHOD_MAPPINGS.put("findClass", "(II)Ljava/lang/Class;");
        METHOD_MAPPINGS.put("resolveClass", "(I)Ljava/lang/Class;");

        for (int i = 1; i < 4; i++) {
            METHOD_MAPPINGS.put("resolveClass" + i, "(I)Ljava/lang/Class;");
        }

        METHOD_MAPPINGS.put("findMethod", "(I)Ljava/lang/reflect/Method;");
        METHOD_MAPPINGS.put("findField", "(I)Ljava/lang/reflect/Field;");
        METHOD_MAPPINGS.put("invokeStatic", "(I[Ljava/lang/Object;)Ljava/lang/Object;");
        METHOD_MAPPINGS.put("invokeVirtual", "(Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;");
        METHOD_MAPPINGS.put("construct", "(I[Ljava/lang/Object;)Ljava/lang/Object;");
        METHOD_MAPPINGS.put("getFieldValue", "(Ljava/lang/Object;I)Ljava/lang/Object;");
        METHOD_MAPPINGS.put("setFieldValue", "(Ljava/lang/Object;ILjava/lang/Object;)V");
        METHOD_MAPPINGS.put("getStaticFieldValue", "(I)Ljava/lang/Object;");
        METHOD_MAPPINGS.put("setStaticFieldValue", "(ILjava/lang/Object;)V");
        FIELD_MAPPINGS.put("memberCache", "[Ljava/lang/Object;");
        FIELD_MAPPINGS.put("classCache", "[Ljava/lang/Class;");
        FIELD_MAPPINGS.put("memberHash", "[I");
        FIELD_MAPPINGS.put("classIds", "[S");
        FIELD_MAPPINGS.put("keyX", "I");
        FIELD_MAPPINGS.put("keyA", "I");
    }
}
