package com.licel.stringer.transform;

import com.licel.stringer.model.MemberInfo;
import com.licel.stringer.model.MethodInfo;
import com.licel.stringer.util.StringerUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class MethodProtector extends MethodVisitor implements Opcodes {
    private static final int STATE_NONE = 0;
    private static final int STATE_SAW_NEW = 1;
    private static final int STATE_SAW_DUP = 2;

    private final ClassProtector classProtector;
    protected boolean isConstructor;
    protected boolean isStaticInitializer;
    protected boolean beforeSuperInit = true;
    protected boolean trimTypes;
    protected int allocationState = STATE_NONE;
    protected Type returnType;
    protected Type[] argumentTypes;
    protected String methodKey;
    protected boolean hideConstructors;

    public MethodProtector(
            MethodVisitor methodVisitor,
            ClassProtector classProtector,
            String methodName,
            String descriptor,
            boolean trimTypes,
            boolean hideConstructors) {
        super(Opcodes.ASM9, methodVisitor);
        this.classProtector = classProtector;
        this.isConstructor = "<init>".equals(methodName);
        this.isStaticInitializer = "<clinit>".equals(methodName);
        this.trimTypes = trimTypes;
        this.hideConstructors = hideConstructors;
        if (!hideConstructors) {
            this.trimTypes = false;
        }
        this.returnType = Type.getReturnType(descriptor);
        this.argumentTypes = Type.getArgumentTypes(descriptor);
        this.methodKey = methodName + descriptor;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        return "Lcom/stringer/annotations/HideAccess;".equals(descriptor) ? null : this.mv.visitAnnotation(descriptor, visible);
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        super.visitLocalVariable(name, descriptor, null, start, end, index);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (opcode == Opcodes.INVOKESPECIAL) {
            this.classProtector.staticInvokeCount++;
        } else {
            this.classProtector.virtualInvokeCount++;
        }

        if (opcode == Opcodes.INVOKESPECIAL) {
            if ("<init>".equals(name)) {
                if (this.hideConstructors) {
                    if (this.isConstructor && (this.beforeSuperInit || !this.trimTypes)) {
                        if (owner.equals(this.classProtector.superClassName)) {
                            this.mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            this.beforeSuperInit = false;
                        } else if (owner.equals(this.classProtector.className)) {
                            this.mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        } else {
                            this.emitConstructorInvocation(this.mv, descriptor, owner);
                        }
                    } else {
                        this.emitConstructorInvocation(this.mv, descriptor, owner);
                    }
                } else {
                    this.mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
            } else if (owner.equals(this.classProtector.className)) {
                this.emitReflectiveMethodInvocation(this.mv, opcode, owner, name, descriptor);
            } else {
                MethodInfo superMethod = new MethodInfo();
                superMethod.name = name;
                superMethod.owner = owner;
                superMethod.descriptor = descriptor;
                this.classProtector.superBridgeMethods.put(owner + "." + name + descriptor, superMethod);
                this.emitReflectiveMethodInvocation(this.mv, opcode, this.classProtector.className, "super$" + name, descriptor);
            }
        } else if (("java/lang/System".equals(owner) || "java/lang/Runtime".equals(owner))
                && ("load".equals(name) || "loadLibrary".equals(name))
                && "(Ljava/lang/String;)V".equals(descriptor)) {
            if (this.trimTypes && "java/lang/System".equals(owner)) {
                this.mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
            }
            if (this.trimTypes && "java/lang/Runtime".equals(owner)) {
                this.mv.visitInsn(Opcodes.SWAP);
                this.mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Runtime");
                this.mv.visitInsn(Opcodes.SWAP);
                this.mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
            }
            this.mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        } else if ("java/lang/Class".equals(owner) && "forName".equals(name) && "(Ljava/lang/String;)Ljava/lang/Class;".equals(descriptor)) {
            if (this.trimTypes) {
                this.mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
            }
            this.mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        } else if ("sun/misc/Unsafe".equals(owner) && "getUnsafe".equals(name)) {
            this.mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        } else if ("java/lang/Class".equals(owner) && "newInstance".equals(name)) {
            if (this.trimTypes) {
                this.mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Class");
            }
            this.mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        } else if ("java/lang/Object".equals(owner) && "getClass".equals(name)) {
            this.mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        } else if ("clone".equals(name)) {
            if (this.trimTypes) {
                this.mv.visitTypeInsn(Opcodes.CHECKCAST, owner);
            }
            this.mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        } else if ("java/lang/ClassLoader".equals(owner) && ("findClass".equals(name) || "findLibrary".equals(name))) {
            if (this.trimTypes) {
                this.mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
            }
            this.mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        } else if (opcode == Opcodes.INVOKEVIRTUAL
                && "java/lang/reflect/Method".equals(owner)
                && "invoke".equals(name)
                && "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;".equals(descriptor)) {
            this.classProtector.needsAccessInvokeHelper = true;
            this.emitReflectiveMethodInvocation(
                    this.mv,
                    Opcodes.INVOKESTATIC,
                    this.classProtector.className,
                    "access$invoke",
                    "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
        } else {
            this.emitReflectiveMethodInvocation(this.mv, opcode, owner, name, descriptor);
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        if (opcode != Opcodes.GETSTATIC && opcode != Opcodes.PUTSTATIC) {
            this.classProtector.fieldGetCount++;
        } else {
            this.classProtector.specialInvokeCount++;
        }

        if ((this.isConstructor || this.isStaticInitializer)
                && (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC)
                && owner.equals(this.classProtector.className)) {
            if (this.trimTypes && Type.getType(descriptor).getSort() == Type.OBJECT) {
                this.mv.visitTypeInsn(Opcodes.CHECKCAST, descriptor.substring(1, descriptor.length() - 1).replace(".", "/"));
            }
            this.mv.visitFieldInsn(opcode, owner, name, descriptor);
            return;
        }

        if (opcode == Opcodes.GETFIELD) {
            int fieldId = this.encodeFieldId(opcode, owner, name, descriptor);
            if (this.classProtector.config.isVerbose()) {
                this.classProtector.listener.verbose("        HideAccess:  field id 0x" + Integer.toHexString(fieldId) + " -> " + owner + "." + name + ":" + descriptor);
            }
            this.mv.visitLdcInsn(fieldId);
            this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.classProtector.decoderClassName,
                    this.classProtector.reflectionProperties.getProperty("reflection.getFieldValue"),
                    "(Ljava/lang/Object;I)Ljava/lang/Object;",
                    false);
            this.unboxOrCast(Type.getType(descriptor));
        } else if (opcode == Opcodes.GETSTATIC) {
            int fieldId = this.encodeFieldId(opcode, owner, name, descriptor);
            if (this.classProtector.config.isVerbose()) {
                this.classProtector.listener.verbose("        HideAccess:  field id 0x" + Integer.toHexString(fieldId) + " -> " + owner + "." + name + ":" + descriptor);
            }
            this.mv.visitLdcInsn(fieldId);
            this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.classProtector.decoderClassName,
                    this.classProtector.reflectionProperties.getProperty("reflection.getStaticFieldValue"),
                    "(I)Ljava/lang/Object;",
                    false);
            this.unboxOrCast(Type.getType(descriptor));
        } else if (opcode == Opcodes.PUTSTATIC) {
            Type fieldType = Type.getType(descriptor);
            this.boxIfPrimitive(fieldType);
            int fieldId = this.encodeFieldId(opcode, owner, name, descriptor);
            if (this.classProtector.config.isVerbose()) {
                this.classProtector.listener.verbose("        HideAccess:  field id 0x" + Integer.toHexString(fieldId) + " -> " + owner + "." + name + ":" + descriptor);
            }
            this.mv.visitLdcInsn(fieldId);
            this.mv.visitInsn(Opcodes.SWAP);
            this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.classProtector.decoderClassName,
                    this.classProtector.reflectionProperties.getProperty("reflection.setStaticFieldValue"),
                    "(ILjava/lang/Object;)V",
                    false);
        } else if (opcode == Opcodes.PUTFIELD) {
            Type fieldType = Type.getType(descriptor);
            this.boxIfPrimitive(fieldType);
            int fieldId = this.encodeFieldId(opcode, owner, name, descriptor);
            if (this.classProtector.config.isVerbose()) {
                this.classProtector.listener.verbose("        HideAccess:  field id 0x" + Integer.toHexString(fieldId) + " -> " + owner + "." + name + ":" + descriptor);
            }
            this.mv.visitLdcInsn(fieldId);
            this.mv.visitInsn(Opcodes.SWAP);
            this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.classProtector.decoderClassName,
                    this.classProtector.reflectionProperties.getProperty("reflection.setFieldValue"),
                    "(Ljava/lang/Object;ILjava/lang/Object;)V",
                    false);
        } else {
            this.mv.visitFieldInsn(opcode, owner, name, descriptor);
        }
    }

    private void boxIfPrimitive(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.FLOAT:
            case Type.LONG:
            case Type.DOUBLE:
                String boxedInternal = this.getBoxedClassName(type).replace(".", "/");
                this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, boxedInternal, "valueOf", "(" + type.getDescriptor() + ")L" + boxedInternal + ";", false);
                break;
            default:
                break;
        }
    }

    private int encodeFieldId(int opcode, String owner, String name, String descriptor) {
        MemberInfo info = this.classProtector.registerField(opcode, owner, name, descriptor, this.classProtector.cryptoContainer);
        return this.classProtector.cryptoContainer.encodeMemberId(info.getReferenceIndex(), info.referenceMask);
    }

    private void packArgumentsIntoArray(MethodVisitor mv, Type[] argTypes) {
        if (argTypes.length == 0) {
            mv.visitInsn(Opcodes.ACONST_NULL);
            return;
        }
        StringerUtils.pushIntConstant(mv, argTypes.length);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        for (int i = argTypes.length - 1; i >= 0; i--) {
            String boxedInternal = this.getBoxedClassName(argTypes[i]).replace(".", "/");
            switch (argTypes[i].getSort()) {
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                case Type.FLOAT:
                    mv.visitInsn(Opcodes.DUP_X1);
                    mv.visitInsn(Opcodes.SWAP);
                    StringerUtils.pushIntConstant(mv, i);
                    mv.visitInsn(Opcodes.SWAP);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, boxedInternal, "valueOf", "(" + argTypes[i].getDescriptor() + ")L" + boxedInternal + ";", false);
                    break;
                case Type.LONG:
                case Type.DOUBLE:
                    mv.visitInsn(Opcodes.DUP_X2);
                    mv.visitInsn(Opcodes.DUP_X2);
                    mv.visitInsn(Opcodes.POP);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, boxedInternal, "valueOf", "(" + argTypes[i].getDescriptor() + ")L" + boxedInternal + ";", false);
                    StringerUtils.pushIntConstant(mv, i);
                    mv.visitInsn(Opcodes.SWAP);
                    break;
                default:
                    mv.visitInsn(Opcodes.DUP_X1);
                    mv.visitInsn(Opcodes.SWAP);
                    StringerUtils.pushIntConstant(mv, i);
                    mv.visitInsn(Opcodes.SWAP);
            }
            mv.visitInsn(Opcodes.AASTORE);
        }
    }

    private void emitReflectiveMethodInvocation(MethodVisitor mv, int opcode, String owner, String name, String descriptor) {
        this.packArgumentsIntoArray(mv, Type.getArgumentTypes(descriptor));
        MemberInfo info = this.classProtector.registerMethod(opcode, owner, name, descriptor, this.classProtector.cryptoContainer);
        int encodedId = this.classProtector.cryptoContainer.encodeMemberId(info.getReferenceIndex(), info.referenceMask);
        if (this.classProtector.config.isVerbose()) {
            this.classProtector.listener.verbose("        HideAccess: method id 0x" + Integer.toHexString(encodedId) + " -> " + owner + "." + name + descriptor);
        }

        mv.visitLdcInsn(encodedId);
        mv.visitInsn(Opcodes.SWAP);
        if (opcode == Opcodes.INVOKESTATIC) {
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.classProtector.decoderClassName,
                    this.classProtector.reflectionProperties.getProperty("reflection.invokeStatic"),
                    "(I[Ljava/lang/Object;)Ljava/lang/Object;",
                    false);
        } else {
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.classProtector.decoderClassName,
                    this.classProtector.reflectionProperties.getProperty("reflection.invokeVirtual"),
                    "(Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;",
                    false);
        }

        this.unboxOrCast(Type.getReturnType(descriptor));
    }

    private void emitConstructorInvocation(MethodVisitor mv, String descriptor, String owner) {
        this.packArgumentsIntoArray(mv, Type.getArgumentTypes(descriptor));
        MemberInfo info = this.classProtector.registerConstructor(Opcodes.NEW, owner, "<init>", descriptor, this.classProtector.cryptoContainer);
        int encodedId = this.classProtector.cryptoContainer.encodeMemberId(info.getReferenceIndex(), info.referenceMask);
        if (this.classProtector.config.isVerbose()) {
            this.classProtector.listener.verbose("        HideAccess: constr id 0x" + Integer.toHexString(encodedId) + " -> " + owner + ".<init>" + descriptor);
        }

        mv.visitLdcInsn(encodedId);
        mv.visitInsn(Opcodes.SWAP);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                this.classProtector.decoderClassName,
                this.classProtector.reflectionProperties.getProperty("reflection.construct"),
                "(I[Ljava/lang/Object;)Ljava/lang/Object;",
                false);
        if (this.isConstructor && (this.beforeSuperInit || !this.trimTypes)) {
            mv.visitTypeInsn(Opcodes.CHECKCAST, owner);
        }
    }

    protected void unboxOrCast(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.FLOAT:
            case Type.LONG:
            case Type.DOUBLE:
                String boxedInternal = this.getBoxedClassName(type).replace(".", "/");
                this.mv.visitTypeInsn(Opcodes.CHECKCAST, boxedInternal);
                this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, boxedInternal, type.getClassName() + "Value", "()" + type.getDescriptor(), false);
                break;
            case Type.ARRAY:
                if (!this.isConstructor || !this.beforeSuperInit && this.trimTypes) {
                    if (type.getElementType().getSort() == Type.OBJECT) {
                        StringBuilder arrayDesc = new StringBuilder();
                        for (int dim = type.getDimensions(); dim > 0; dim--) {
                            arrayDesc.append('[');
                        }
                        arrayDesc.append("Ljava/lang/Object;");
                        this.mv.visitTypeInsn(Opcodes.CHECKCAST, arrayDesc.toString());
                    } else {
                        this.mv.visitTypeInsn(Opcodes.CHECKCAST, type.getDescriptor());
                    }
                } else {
                    this.mv.visitTypeInsn(Opcodes.CHECKCAST, type.getDescriptor());
                }
                break;
            case Type.OBJECT:
                if (!this.isConstructor || !this.beforeSuperInit && this.trimTypes) {
                    if (!this.trimTypes) {
                        this.mv.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
                    }
                } else {
                    this.mv.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
                }
                break;
            default:
                this.mv.visitInsn(Opcodes.POP);
        }
    }

    protected String getBoxedClassName(Type type) {
        return StringerUtils.getBoxedClassName(type);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (opcode == Opcodes.NEW) {
            if (!this.trimTypes && !this.hideConstructors) {
                this.mv.visitTypeInsn(opcode, type);
            } else {
                this.classProtector.fieldSetCount++;
                this.allocationState = STATE_SAW_NEW;
            }
        } else if (opcode == Opcodes.CHECKCAST) {
            if (!this.isConstructor || !this.beforeSuperInit && this.trimTypes) {
                if (type.startsWith("[")) {
                    Type arrayType = Type.getType(type);
                    if (arrayType.getElementType().getSort() == Type.OBJECT) {
                        StringBuilder erased = new StringBuilder();
                        for (int dim = arrayType.getDimensions(); dim > 0; dim--) {
                            erased.append('[');
                        }
                        erased.append("Ljava/lang/Object;");
                        this.mv.visitTypeInsn(Opcodes.CHECKCAST, erased.toString());
                    } else {
                        this.mv.visitTypeInsn(Opcodes.CHECKCAST, type);
                    }
                }
            } else {
                this.mv.visitTypeInsn(opcode, type);
            }
        } else {
            this.mv.visitTypeInsn(opcode, type);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if (this.allocationState == STATE_SAW_NEW) {
            if (opcode != Opcodes.DUP && opcode != Opcodes.DUP_X1) {
                throw new IllegalStateException("dup not found: " + Integer.toHexString(opcode));
            }
            this.allocationState = STATE_SAW_DUP;
        } else {
            this.allocationState = STATE_NONE;
            if (opcode == Opcodes.ATHROW && this.trimTypes) {
                this.mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Throwable");
            } else if (opcode == Opcodes.ARETURN && !"java/lang/Object".equals(this.returnType.getInternalName())) {
                this.mv.visitTypeInsn(Opcodes.CHECKCAST, this.returnType.getInternalName());
            }
            this.mv.visitInsn(opcode);
        }
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
    }
}
