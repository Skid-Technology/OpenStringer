package com.licel.stringer.analysis;

import com.licel.stringer.Config;
import com.licel.stringer.WorkerListener;
import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.model.ClassMemberInfo;
import com.licel.stringer.model.FieldInfo;
import com.licel.stringer.model.MethodInfo;
import com.licel.stringer.model.OperandStackModel;
import com.licel.stringer.model.StackValue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class MethodMetadataCollector extends MethodVisitor implements Opcodes {
    protected static final int STATE_IDLE = 0;
    private static final int STATE_NEW_SEEN = 1;
    private static final int STATE_NEW_DUP_SEEN = 2;

    private final ClassInfo classInfo;
    private final String methodName;
    private final String descriptor;
    private String lastStringLiteral = null;
    private boolean lastInsnWasStringLdc = false;
    private boolean hideAccessEnabled;
    private final boolean hideAccessExcluded;
    private final WorkerListener listener;
    private boolean canTrimTypes = true;
    private final String formattedMethodSignature;
    private final String formattedConstructorSignature;
    private final boolean isConstructor;
    private boolean beforeSuperInit = true;
    private boolean hasPreInitMethodCalls = false;
    private boolean hasPreInitFieldReads = false;
    private boolean allocatesSelfOrSuperPreInit = false;
    private final Set<Label> preInitJumpTargets = new HashSet<>();
    private final Set<MethodInfo> preInitMethodCalls = new HashSet<>();
    private final ArrayList<MethodInfo> superInitCalls = new ArrayList<>();
    private final Set<MethodInfo> constructorMethodCalls = new HashSet<>();
    private final Set<FieldInfo> preInitFieldReads = new HashSet<>();
    private final Config config;
    private boolean hasNonStandardNewPattern = false;
    protected int newPatternState = STATE_IDLE;
    private final OperandStackModel operandStackModel = new OperandStackModel();
    private int fieldRefIndex = 0;
    private int methodReturnIndex = 0;
    private boolean stackTrackingStopped = false;

    public MethodMetadataCollector(
            ClassInfo classInfo,
            String methodName,
            String descriptor,
            boolean hideAccessMatched,
            boolean hideAccessExcluded,
            Config config) {
        super(Opcodes.ASM9);
        this.classInfo = classInfo;
        this.methodName = methodName;
        this.descriptor = descriptor;
        this.hideAccessExcluded = hideAccessExcluded;
        this.hideAccessEnabled = hideAccessExcluded ? false : hideAccessMatched;
        this.formattedMethodSignature = formatMethodSignature(classInfo.name, methodName, descriptor);
        this.formattedConstructorSignature = formatConstructorSignature(classInfo.name, methodName, descriptor);
        this.listener = config.getListener();
        this.config = config;
        this.isConstructor = "<init>".equals(methodName);

        for (int i = 0; i < 1000; i++) {
            this.operandStackModel.add(null);
        }
    }

    @Override
    public void visitEnd() {
        if (!"<clinit>".equals(this.methodName) || !this.classInfo.isEnum) {
            if (!this.hideAccessEnabled) {
                if (!this.config.getHideAccess().matchClassElement(this.classInfo.name, this.methodName, this.descriptor)) {
                    return;
                }
            }

            if (this.isConstructor) {
                if (!this.allocatesSelfOrSuperPreInit && this.preInitJumpTargets.isEmpty()) {
                    if (this.hasPreInitMethodCalls || this.hasPreInitFieldReads) {
                        if (this.superInitCalls.isEmpty()) {
                            this.canTrimTypes = false;
                            this.listener.verbose("        Can't trim types in constructor (#2) " + this.formattedConstructorSignature);

                            for (MethodInfo call : this.constructorMethodCalls) {
                                this.listener.verbose("            -***-> " + call);
                            }

                            this.listener.verbose("        Can't trim types in constructor (#2) " + this.formattedConstructorSignature);
                        } else if (!this.hasOnlyPrimitiveOrObjectArguments(this.superInitCalls.get(0).descriptor)) {
                            int nonObjectReturnCount = 0;
                            if (this.hasPreInitMethodCalls) {
                                for (MethodInfo call : this.preInitMethodCalls) {
                                    if (!this.isPrimitiveOrObjectReturnType(call.descriptor)) {
                                        nonObjectReturnCount++;
                                    }
                                }
                            }

                            int nonObjectFieldCount = 0;
                            if (this.hasPreInitFieldReads) {
                                for (FieldInfo field : this.preInitFieldReads) {
                                    if (!this.isPrimitiveOrObjectType(field.descriptor)) {
                                        nonObjectFieldCount++;
                                    }
                                }
                            }

                            if (nonObjectReturnCount != 0 || nonObjectFieldCount != 0) {
                                this.listener.verbose("        Can't trim types in constructor (#3) " + this.formattedConstructorSignature);
                            }
                        }
                    }
                } else {
                    this.canTrimTypes = false;
                    this.listener.verbose("        Can't trim types in constructor (#1) " + this.formattedConstructorSignature);
                }
            }

            this.classInfo.hasHideAccessTargets = true;
            String methodKey = this.methodName + this.descriptor;
            this.classInfo.hideAccessTrimFlags.put(methodKey, this.canTrimTypes);
            if (this.hasNonStandardNewPattern) {
                this.listener.verbose("        Can't trim types in method (#4) " + methodKey);
                this.classInfo.hideAccessMethodFlags.put(methodKey, !this.hasNonStandardNewPattern);
            }
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        this.lastInsnWasStringLdc = false;
        if ("Lcom/licel/stringer/annotations/secured;".equals(descriptor)) {
            this.classInfo.hasStringEncryptionAnnotation = true;
            this.classInfo.hasSecuredMethod = true;
        }
        if ("Lcom/licel/stringer/annotations/insecure;".equals(descriptor)) {
            this.classInfo.hasInsecureMethod = true;
            this.classInfo.hasInsecureAnnotation = true;
        }
        if ("Lcom/stringer/annotations/StringEncryption;".equals(descriptor)) {
            this.classInfo.hasStringEncryptionAnnotation = true;
            this.classInfo.hasSecuredMethod = true;
            this.classInfo.hasExplicitStringEncryptionAnnotation = true;
        }
        if (!this.hideAccessExcluded && "Lcom/stringer/annotations/HideAccess;".equals(descriptor)) {
            this.classInfo.hasHideAccessTargets = true;
            this.hideAccessEnabled = true;
        }
        return null;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        this.lastInsnWasStringLdc = false;
        this.classInfo.referencedTypes.add(owner);
        this.classInfo.referencedTypes.add(descriptor);
        if ((opcode == PUTFIELD || opcode == PUTSTATIC) && descriptor.equals("Ljava/lang/String;") && this.lastStringLiteral != null) {
            if (this.classInfo.securedStringFields.containsKey(name)) {
                this.classInfo.securedStringFields.put(name, this.lastStringLiteral);
            }
            this.lastStringLiteral = null;
        }

        if (this.hideAccessEnabled && this.isConstructor && this.beforeSuperInit && (opcode == GETFIELD || opcode == GETSTATIC)) {
            this.hasPreInitFieldReads = true;
            this.preInitFieldReads.add(new FieldInfo(owner, name, descriptor));
        }

        if (this.hideAccessEnabled && this.isConstructor && this.beforeSuperInit && !this.stackTrackingStopped) {
            if (opcode != GETFIELD && opcode != GETSTATIC) {
                this.operandStackModel.pop();
            } else if (this.isPrimitiveOrObjectType(descriptor)) {
                this.operandStackModel.add(new StackValue(StackValue.KIND_OTHER));
            } else {
                this.operandStackModel.add(new StackValue(StackValue.KIND_FIELD_REF, this.fieldRefIndex++));
            }
        }
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
        this.lastInsnWasStringLdc = false;
        this.classInfo.referencedTypes.add(descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        this.lastInsnWasStringLdc = false;
        this.classInfo.referencedTypes.add(owner);
        this.classInfo.referencedMethods.add(new ClassMemberInfo(opcode, name, descriptor));
        if (name.equals("getBundle") && owner.equals("java/util/ResourceBundle")) {
            this.classInfo.usesResourceBundle = true;
        }
        if ((name.equals("getResource") || name.equals("getResourceAsStream"))
                && (owner.equals("java/lang/Class") || owner.equals("java/lang/ClassLoader"))) {
            this.classInfo.usesResourceStreams = true;
        }
        if ((name.equals("openConnection") || name.equals("openStream")) && owner.equals("java/net/URL")) {
            this.classInfo.usesResourceStreams = true;
        }
        if (name.equals("getResource") && (owner.equals("java/lang/Class") || owner.equals("java/lang/ClassLoader"))) {
            this.classInfo.usesGetResourceUrl = true;
        }

        if (this.hideAccessEnabled && this.isConstructor && this.beforeSuperInit && !this.stackTrackingStopped) {
            Type[] argTypes = Type.getArgumentTypes(descriptor);
            for (Type argType : argTypes) {
                if (argType.getSort() == Type.LONG || argType.getSort() == Type.DOUBLE) {
                    this.operandStackModel.pop();
                }
                this.operandStackModel.pop();
            }

            Type returnType = Type.getReturnType(descriptor);
            switch (returnType.getSort()) {
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                case Type.FLOAT:
                case Type.LONG:
                case Type.DOUBLE:
                    this.operandStackModel.add(new StackValue(StackValue.KIND_OTHER));
                    break;
                case Type.ARRAY:
                    Type elementType = returnType.getElementType();
                    if (elementType.getSort() == Type.OBJECT && !"java/lang/Object".equals(elementType.getInternalName())) {
                        this.operandStackModel.add(new StackValue(StackValue.KIND_METHOD_RETURN, this.methodReturnIndex));
                    } else {
                        this.operandStackModel.add(new StackValue(StackValue.KIND_OTHER));
                    }
                    break;
                case Type.OBJECT:
                    if ("java/lang/Object".equals(returnType.getInternalName())) {
                        this.operandStackModel.add(new StackValue(StackValue.KIND_OTHER));
                    } else {
                        this.operandStackModel.add(new StackValue(StackValue.KIND_METHOD_RETURN, this.methodReturnIndex));
                    }
                    break;
                default:
                    break;
            }

            this.methodReturnIndex++;
        }

        if (("java/lang/System".equals(owner) || "java/lang/Runtime".equals(owner))
                && ("load".equals(name) || "loadLibrary".equals(name))
                && "(Ljava/lang/String;)V".equals(descriptor)) {
            this.listener.verbose("        Can't hide call " + owner.replace('/', '.') + "." + name + " from " + this.formattedMethodSignature);
        }

        if (this.hideAccessEnabled && this.isConstructor) {
            this.constructorMethodCalls.add(new MethodInfo(owner, name, descriptor));
            if (this.beforeSuperInit
                    && (opcode != INVOKESPECIAL || !"<init>".equals(name) || (!this.classInfo.name.equals(owner) && !this.classInfo.superName.equals(owner)))) {
                this.hasPreInitMethodCalls = true;
                this.preInitMethodCalls.add(new MethodInfo(owner, name, descriptor));
            }
        }

        if (this.hideAccessEnabled && this.canTrimTypes && opcode == INVOKESPECIAL) {
            if (this.isConstructor) {
                if (!"<init>".equals(name) || this.classInfo.name.equals(owner) || this.classInfo.superName.equals(owner)) {
                    if ("<init>".equals(name) || (!this.classInfo.name.equals(owner) && !this.classInfo.superName.equals(owner))) {
                        this.beforeSuperInit = false;
                        this.superInitCalls.add(new MethodInfo(owner, name, descriptor));
                    }
                }
            }
        }
    }

    private boolean isPrimitiveOrObjectReturnType(String methodDescriptor) {
        Type returnType = Type.getReturnType(methodDescriptor);
        if (returnType.getSort() == Type.OBJECT && !"java/lang/Object".equals(returnType.getInternalName())) {
            return false;
        }
        if (returnType.getSort() == Type.ARRAY) {
            Type elementType = returnType.getElementType();
            if (elementType.getSort() == Type.OBJECT && !"java/lang/Object".equals(elementType.getInternalName())) {
                return false;
            }
        }
        return true;
    }

    private boolean isPrimitiveOrObjectType(String typeDescriptor) {
        Type type = Type.getType(typeDescriptor);
        if (type.getSort() == Type.OBJECT && !"java/lang/Object".equals(type.getInternalName())) {
            return false;
        }
        if (type.getSort() == Type.ARRAY) {
            Type elementType = type.getElementType();
            if (elementType.getSort() == Type.OBJECT && !"java/lang/Object".equals(elementType.getInternalName())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasOnlyPrimitiveOrObjectArguments(String methodDescriptor) {
        Type[] argTypes = Type.getArgumentTypes(methodDescriptor);
        for (Type argType : argTypes) {
            if (argType.getSort() == Type.OBJECT && !"java/lang/Object".equals(argType.getInternalName())) {
                return false;
            }
            if (argType.getSort() == Type.ARRAY) {
                Type elementType = argType.getElementType();
                if (elementType.getSort() == Type.OBJECT && !"java/lang/Object".equals(elementType.getInternalName())) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String formatConstructorSignature(String owner, String methodName, String descriptor) {
        StringBuilder builder = new StringBuilder();
        builder.append(owner.replace('/', '.'));
        builder.append('(');
        Type[] argTypes = Type.getArgumentTypes(descriptor);
        for (int i = 0; i < argTypes.length; i++) {
            if (i != 0) {
                builder.append(", ");
            }
            builder.append(argTypes[i].getClassName());
        }
        builder.append(')');
        return builder.toString();
    }

    public static String formatMethodSignature(String owner, String methodName, String descriptor) {
        Type returnType = Type.getReturnType(descriptor);
        StringBuilder builder = new StringBuilder();
        builder.append(returnType.getClassName());
        builder.append(' ');
        builder.append(owner.replace('/', '.'));
        builder.append('.');
        builder.append(methodName);
        builder.append('(');
        Type[] argTypes = Type.getArgumentTypes(descriptor);
        for (int i = 0; i < argTypes.length; i++) {
            if (i != 0) {
                builder.append(", ");
            }
            builder.append(argTypes[i].getClassName());
        }
        builder.append(')');
        return builder.toString();
    }

    @Override
    public void visitInsn(int opcode) {
        if (this.newPatternState == STATE_NEW_SEEN) {
            if (opcode != DUP && opcode != DUP_X1) {
                this.hasNonStandardNewPattern = true;
                this.newPatternState = STATE_IDLE;
            } else {
                this.newPatternState = STATE_NEW_DUP_SEEN;
            }
        } else if (this.newPatternState == STATE_NEW_DUP_SEEN && opcode == SWAP) {
            this.hasNonStandardNewPattern = true;
            this.newPatternState = STATE_IDLE;
        } else {
            this.newPatternState = STATE_IDLE;
        }

        if (opcode == MONITORENTER && this.lastInsnWasStringLdc && this.lastStringLiteral != null) {
            this.classInfo.excludedStrings.add(this.lastStringLiteral);
        }
        this.lastInsnWasStringLdc = false;
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        this.lastInsnWasStringLdc = false;
        switch (opcode) {
            case BIPUSH:
            case SIPUSH:
                this.operandStackModel.add(new StackValue(StackValue.KIND_OTHER));
                break;
            case NEWARRAY:
                this.operandStackModel.replaceTop(new StackValue(StackValue.KIND_OTHER));
                break;
            default:
                throw new RuntimeException("Unexpected opcode: " + opcode);
        }
    }

    @Override
    public void visitVarInsn(int opcode, int varIndex) {
        this.lastInsnWasStringLdc = false;
        super.visitVarInsn(opcode, varIndex);
    }

    @Override
    public void visitIincInsn(int varIndex, int increment) {
        this.lastInsnWasStringLdc = false;
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        this.lastInsnWasStringLdc = false;
        this.operandStackModel.pop();
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        this.lastInsnWasStringLdc = false;
        this.operandStackModel.pop();
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        this.lastInsnWasStringLdc = false;
        if (this.isConstructor && this.beforeSuperInit) {
            this.stackTrackingStopped = true;
            this.preInitJumpTargets.add(label);
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        this.lastInsnWasStringLdc = false;
        this.classInfo.referencedTypes.add(type);
        if (opcode == NEW) {
            this.newPatternState = STATE_NEW_SEEN;
        }

        if (this.isConstructor && this.beforeSuperInit && !this.stackTrackingStopped) {
            switch (opcode) {
                case NEW:
                    if (!type.equals(this.classInfo.name) && !type.equals(this.classInfo.superName)) {
                        this.operandStackModel.add(new StackValue(StackValue.KIND_OTHER));
                    } else {
                        this.stackTrackingStopped = true;
                        this.allocatesSelfOrSuperPreInit = true;
                    }
                    break;
                case ANEWARRAY:
                case INSTANCEOF:
                    this.operandStackModel.add(new StackValue(StackValue.KIND_OTHER));
                    break;
                case CHECKCAST:
                    this.operandStackModel.pop();
                    this.operandStackModel.add(new StackValue(StackValue.KIND_OTHER));
                    break;
                default:
                    throw new RuntimeException("Unexpected opcode: " + opcode);
            }
        }
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        this.lastInsnWasStringLdc = false;
        this.operandStackModel.add(new StackValue(StackValue.KIND_OTHER));
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (value instanceof String) {
            this.classInfo.stringLiterals.add((String) value);
            this.lastStringLiteral = (String) value;
            this.lastInsnWasStringLdc = true;
        }
        if (value instanceof Long || value instanceof Double) {
            this.operandStackModel.add(new StackValue(StackValue.KIND_OTHER));
        }
        this.operandStackModel.add(new StackValue(StackValue.KIND_OTHER));
    }
}
