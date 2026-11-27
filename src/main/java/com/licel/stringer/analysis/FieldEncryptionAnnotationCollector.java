package com.licel.stringer.analysis;

import com.licel.stringer.model.ClassInfo;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

public class FieldEncryptionAnnotationCollector extends FieldVisitor implements Opcodes {
    private final ClassInfo classInfo;
    private final String initialValue;
    private final String fieldName;

    public FieldEncryptionAnnotationCollector(ClassInfo classInfo, String fieldName, Object value) {
        super(Opcodes.ASM9);
        this.classInfo = classInfo;
        this.fieldName = fieldName;
        this.initialValue = value != null ? (value instanceof String ? (String) value : null) : null;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (("Lcom/stringer/annotations/StringEncryption;".equals(descriptor) || this.classInfo.annotatedSecured)
                && !"Lcom/licel/stringer/annotations/insecure;".equals(descriptor)) {
            this.classInfo.hasStringEncryptionAnnotation = true;
            this.classInfo.hasSecuredField = true;
            this.classInfo.hasExplicitStringEncryptionAnnotation = true;
            this.classInfo.securedStringFields.put(this.fieldName, this.initialValue);
        }
        return null;
    }
}
