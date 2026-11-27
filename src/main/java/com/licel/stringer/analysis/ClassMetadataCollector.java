package com.licel.stringer.analysis;

import com.licel.stringer.Config;
import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.model.ClassMemberInfo;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

public class ClassMetadataCollector extends ClassVisitor implements Opcodes {
    public ClassInfo classInfo;
    private boolean hideAccessMatched = false;
    private boolean hideAccessExcluded = false;
    private final Config config;

    public ClassMetadataCollector(String className, Config config) {
        super(Opcodes.ASM9);
        this.config = config;
        this.classInfo = new ClassInfo(className);
    }

    public ClassInfo getClassInfo() {
        return this.classInfo;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        this.classInfo.annotatedSecured = "Lcom/licel/stringer/annotations/secured;".equals(descriptor);
        this.classInfo.hasStringEncryptionAnnotation = this.classInfo.annotatedSecured;
        this.classInfo.annotatedInsecure = "Lcom/licel/stringer/annotations/insecure;".equals(descriptor);
        if ("Lcom/stringer/annotations/StringEncryption;".equals(descriptor)) {
            this.classInfo.annotatedSecured = true;
            this.classInfo.hasExplicitStringEncryptionAnnotation = true;
            this.classInfo.hasStringEncryptionAnnotation = true;
        }
        if ("Lcom/stringer/annotations/HideAccess;".equals(descriptor)) {
            this.hideAccessMatched = true;
        }
        return null;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.classInfo.classFileVersion = version;
        this.classInfo.name = name;
        this.classInfo.isInterface = (access & ACC_INTERFACE) == ACC_INTERFACE;
        this.classInfo.isEnum = (access & ACC_ENUM) == ACC_ENUM;
        this.classInfo.isSynthetic = (access & ACC_SYNTHETIC) == ACC_SYNTHETIC;
        this.classInfo.superName = superName;
        if (this.config.getHideAccess().isEnabled() && this.config.getHideAccess().isFiltered()) {
            this.hideAccessMatched = this.config.getHideAccess().match(this.classInfo);
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        this.classInfo.declaredFields.add(new ClassMemberInfo(access, name, descriptor));
        if (descriptor.equals("Ljava/lang/String;")) {
            this.classInfo.stringFieldInitialValues.put(name, value);
            return new FieldEncryptionAnnotationCollector(this.classInfo, name, value);
        }
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if ((access & ACC_NATIVE) == ACC_NATIVE) {
            this.classInfo.hasNativeMethods = true;
        }
        if (name.equals("<clinit>")) {
            this.classInfo.hasStaticInitializer = true;
        }
        this.classInfo.declaredMethods.add(new ClassMemberInfo(access, name, descriptor));
        return new MethodMetadataCollector(this.classInfo, name, descriptor, this.hideAccessMatched, this.hideAccessExcluded, this.config);
    }

    @Override
    public ModuleVisitor visitModule(String name, int access, String version) {
        this.classInfo.isModuleInfo = true;
        return null;
    }
}
