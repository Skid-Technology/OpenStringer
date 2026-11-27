package com.licel.stringer.transform;

import com.licel.stringer.Config;
import com.licel.stringer.Worker;
import com.licel.stringer.WorkerListener;
import com.licel.stringer.crypto.CryptoContainer;
import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.model.ClassMemberInfo;
import com.licel.stringer.model.MemberInfo;
import com.licel.stringer.model.MethodInfo;
import com.licel.stringer.util.AsmUtils;
import java.util.HashMap;
import java.util.Properties;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ClassProtector extends ClassVisitor implements Opcodes {
    private static final String BOOTSTRAP_DESCRIPTOR = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";

    final ClassInfo classInfo;
    String className;
    String superClassName;
    int classVersion;
    boolean useInvokeDynamic;
    int flags;
    Config config;
    CryptoContainer cryptoContainer;
    WorkerListener listener;
    Handle bootstrapMethodHandle;
    String bootstrapMethodName;
    final ClassInfo decoderClassInfo;
    int staticInvokeCount = 0;
    int virtualInvokeCount = 0;
    int specialInvokeCount = 0;
    int fieldGetCount = 0;
    int fieldSetCount = 0;
    int indyCallCount = 0;
    final HashMap<String, MethodInfo> superBridgeMethods = new HashMap<>();
    final String decoderClassName;
    final Properties reflectionProperties;
    boolean needsAccessInvokeHelper = false;
    boolean isEnumClass = false;

    public ClassProtector(
            ClassVisitor classVisitor,
            ClassInfo classInfo,
            Config config,
            ClassInfo decoderClassInfo,
            Properties reflectionProperties,
            CryptoContainer cryptoContainer) {
        super(Opcodes.ASM9, classVisitor);
        this.classInfo = classInfo;
        this.config = config;
        this.listener = config.getListener();
        this.decoderClassName = decoderClassInfo.name;
        this.decoderClassInfo = decoderClassInfo;
        this.reflectionProperties = reflectionProperties;
        this.cryptoContainer = cryptoContainer;
        this.bootstrapMethodName = Worker.get().internalNameGenerator.generateMethodName(classInfo, BOOTSTRAP_DESCRIPTOR, null, 2);
        classInfo.declaredMethods.add(new ClassMemberInfo(0, this.bootstrapMethodName, BOOTSTRAP_DESCRIPTOR));
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.superClassName = superName;
        int effectiveVersion = this.config.getTargetInt() > 0 ? this.config.getTargetInt() : version;
        this.classVersion = effectiveVersion;
        this.isEnumClass = (access & Opcodes.ACC_ENUM) == Opcodes.ACC_ENUM;
        if (effectiveVersion >= Opcodes.V1_7 && effectiveVersion != Opcodes.V1_1) {
            this.useInvokeDynamic = true;
        }
        if (this.useInvokeDynamic) {
            this.bootstrapMethodHandle = new Handle(Opcodes.H_INVOKESTATIC, this.className, this.bootstrapMethodName, BOOTSTRAP_DESCRIPTOR, false);
        }
        this.cv.visit(effectiveVersion, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return "Lcom/stringer/annotations/HideAccess;".equals(descriptor) ? null : this.cv.visitAnnotation(descriptor, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = AsmUtils.visitMethod(this.cv, access, name, descriptor, signature, exceptions, ClassWriter.COMPUTE_MAXS);
        if ("java/lang/SecurityManager".equals(this.superClassName)) {
            return mv;
        }

        Boolean canTrimTypes = this.classInfo.hideAccessTrimFlags.get(name + descriptor);
        Boolean canTrimMethodTypes = this.classInfo.hideAccessMethodFlags.get(name + descriptor);
        if (canTrimMethodTypes == null) {
            canTrimMethodTypes = true;
        }

        if (this.useInvokeDynamic) {
            return new MethodIndyProtector(mv, this, name, descriptor, false);
        }
        if (canTrimTypes != null) {
            return new MethodProtector(mv, this, name, descriptor, canTrimTypes, canTrimMethodTypes);
        }
        return new StackFrameDiscardingVisitor(mv);
    }

    @Override
    public void visitEnd() {
        if (this.useInvokeDynamic && this.indyCallCount > 0) {
            this.emitBootstrapMethod(this.cv);
        } else {
            if (!this.superBridgeMethods.isEmpty()) {
                for (MethodInfo methodInfo : this.superBridgeMethods.values()) {
                    this.emitSuperBridgeMethod(this.cv, methodInfo);
                }
            }
            if (this.needsAccessInvokeHelper) {
                this.emitAccessInvokeHelper(this.cv);
            }
        }
        this.cv.visitEnd();
    }

    private void emitBootstrapMethod(ClassVisitor cv) {
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, this.bootstrapMethodName, BOOTSTRAP_DESCRIPTOR, null, null);
        mv.visitCode();
        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label catchHandler = new Label();
        mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, "java/lang/ClassNotFoundException");
        mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, "java/lang/IllegalAccessException");

        mv.visitLabel(tryStart);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/invoke/ConstantCallSite");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/invoke/MethodHandles$Lookup");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
        mv.visitIntInsn(Opcodes.BIPUSH, 32);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(Ljava/lang/String;I)Ljava/lang/Integer;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                this.decoderClassName,
                this.reflectionProperties.getProperty("reflection.findMethod"),
                "(I)Ljava/lang/reflect/Method;",
                false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "unreflect", "(Ljava/lang/reflect/Method;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/invoke/MethodType");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
        mv.visitLabel(tryEnd);
        mv.visitInsn(Opcodes.ARETURN);

        mv.visitLabel(catchHandler);
        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/ReflectiveOperationException"});
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/BootstrapMethodError");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/BootstrapMethodError", "<init>", "(Ljava/lang/Throwable;)V", false);
        mv.visitInsn(Opcodes.ATHROW);
        mv.visitMaxs(5, 4);
        mv.visitEnd();
    }

    private void emitSuperBridgeMethod(ClassVisitor cv, MethodInfo methodInfo) {
        MethodVisitor mv = AsmUtils.visitMethod(
                cv,
                Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC,
                "super$" + methodInfo.name,
                methodInfo.descriptor,
                null,
                null,
                ClassWriter.COMPUTE_MAXS);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        Type[] argTypes = Type.getArgumentTypes(methodInfo.descriptor);
        int localOffset = 1;

        for (int i = 0; i < argTypes.length; i++) {
            int slot = i + localOffset;
            switch (argTypes[i].getSort()) {
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    mv.visitVarInsn(Opcodes.ILOAD, slot);
                    break;
                case Type.FLOAT:
                    mv.visitVarInsn(Opcodes.FLOAD, slot);
                    break;
                case Type.LONG:
                    mv.visitVarInsn(Opcodes.LLOAD, slot);
                    localOffset++;
                    break;
                case Type.DOUBLE:
                    mv.visitVarInsn(Opcodes.DLOAD, slot);
                    localOffset++;
                    break;
                default:
                    mv.visitVarInsn(Opcodes.ALOAD, slot);
            }
        }

        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, methodInfo.owner, methodInfo.name, methodInfo.descriptor, false);
        Type returnType = Type.getReturnType(methodInfo.descriptor);
        switch (returnType.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                mv.visitInsn(Opcodes.IRETURN);
                break;
            case Type.FLOAT:
                mv.visitInsn(Opcodes.FRETURN);
                break;
            case Type.LONG:
                mv.visitInsn(Opcodes.LRETURN);
                break;
            case Type.DOUBLE:
                mv.visitInsn(Opcodes.DRETURN);
                break;
            case Type.ARRAY:
            case Type.OBJECT:
                mv.visitInsn(Opcodes.ARETURN);
                break;
            default:
                mv.visitInsn(Opcodes.RETURN);
        }

        mv.visitMaxs(localOffset, 1);
        mv.visitEnd();
    }

    private void emitAccessInvokeHelper(ClassVisitor cv) {
        MethodVisitor mv = AsmUtils.visitMethod(
                cv,
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                "access$invoke",
                "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
                null,
                null,
                ClassWriter.COMPUTE_MAXS);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    private static String memberKey(int opcode, String owner, String name, String descriptor) {
        return owner + "." + name + ":" + descriptor;
    }

    public int getOrAssignClassIndex(String internalName) {
        Integer index = this.decoderClassInfo.hiddenClassIndices.get(internalName);
        if (index == null) {
            index = this.decoderClassInfo.nextClassReferenceIndex++;
            this.decoderClassInfo.hiddenClassIndices.put(internalName, index);
        }
        return index;
    }

    public MemberInfo registerConstructor(int opcode, String owner, String name, String descriptor, CryptoContainer cryptoContainer) {
        String key = memberKey(opcode, owner, name, descriptor);
        MemberInfo info = this.decoderClassInfo.hiddenMembers.get(key);
        if (info != null) {
            return info;
        }

        info = new MemberInfo();
        info.setReferenceIndex(this.decoderClassInfo.nextMemberReferenceIndex++);
        info.setAccessKindFromOpcode(Opcodes.NEW);
        info.owner = owner;
        info.name = name;
        info.descriptor = descriptor;
        info.ownerClassIndex = this.getOrAssignClassIndex(owner);
        info.setReferenceMask(cryptoContainer.nextInt(65536));

        Type[] argTypes = Type.getArgumentTypes(descriptor);
        int hash = info.referenceMask * 31 + '(';
        for (int i = 0; i < argTypes.length; i++) {
            Type argType = argTypes[i];
            if (i != 0) {
                hash = 31 * hash + ',';
            }
            if (argType.getSort() == Type.ARRAY) {
                hash = 31 * hash + argType.getDescriptor().replace('/', '.').hashCode();
            } else {
                hash = 31 * hash + argType.getClassName().hashCode();
            }
        }
        hash = 31 * hash + ')';
        hash = hash * 31 + info.referenceMask;
        info.encodedId = hash;

        this.decoderClassInfo.hiddenMembers.put(key, info);
        return info;
    }

    public MemberInfo registerMethod(int opcode, String owner, String name, String descriptor, CryptoContainer cryptoContainer) {
        String key = memberKey(opcode, owner, name, descriptor);
        MemberInfo info = this.decoderClassInfo.hiddenMembers.get(key);
        if (info != null) {
            return info;
        }

        info = new MemberInfo();
        info.setReferenceIndex(this.decoderClassInfo.nextMemberReferenceIndex++);
        info.setAccessKindFromOpcode(opcode);
        info.owner = owner;
        info.name = name;
        info.descriptor = descriptor;
        info.ownerClassIndex = this.getOrAssignClassIndex(owner);
        info.setReferenceMask(cryptoContainer.nextInt(65536));

        Type[] argTypes = Type.getArgumentTypes(descriptor);
        int hash = info.referenceMask * 31 + name.hashCode();
        hash = 31 * hash + '(';
        for (int i = 0; i < argTypes.length; i++) {
            Type argType = argTypes[i];
            if (i != 0) {
                hash = 31 * hash + ',';
            }
            if (argType.getSort() == Type.ARRAY) {
                hash = 31 * hash + argType.getDescriptor().replace('/', '.').hashCode();
            } else {
                hash = 31 * hash + argType.getClassName().hashCode();
            }
        }
        hash = 31 * hash + ')';

        Type returnType = Type.getReturnType(descriptor);
        if (returnType.getSort() == Type.ARRAY) {
            hash = 31 * hash + returnType.getDescriptor().replace('/', '.').hashCode();
        } else {
            hash = 31 * hash + returnType.getClassName().hashCode();
        }
        hash = hash * 31 + info.referenceMask;
        info.encodedId = hash;

        this.decoderClassInfo.hiddenMembers.put(key, info);
        return info;
    }

    public MemberInfo registerField(int opcode, String owner, String name, String descriptor, CryptoContainer cryptoContainer) {
        String key = memberKey(opcode, owner, name, descriptor);
        MemberInfo info = this.decoderClassInfo.hiddenMembers.get(key);
        if (info != null) {
            return info;
        }

        info = new MemberInfo();
        info.setReferenceIndex(this.decoderClassInfo.nextMemberReferenceIndex++);
        info.setAccessKindFromOpcode(opcode);
        info.owner = owner;
        info.name = name;
        info.descriptor = descriptor;
        info.ownerClassIndex = this.getOrAssignClassIndex(owner);
        info.setReferenceMask(cryptoContainer.nextInt(65536));

        int hash = info.referenceMask * 31 + name.hashCode();
        hash = 31 * hash + ':';
        Type fieldType = Type.getType(descriptor);
        if (fieldType.getSort() == Type.ARRAY) {
            hash = 31 * hash + fieldType.getDescriptor().replace('/', '.').hashCode();
        } else {
            hash = 31 * hash + fieldType.getClassName().hashCode();
        }
        hash = hash * 31 + info.referenceMask;
        info.encodedId = hash;

        this.decoderClassInfo.hiddenMembers.put(key, info);
        return info;
    }
}
