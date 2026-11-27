package com.licel.stringer.transform;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

public class FieldEncryptionAnnotationVisitor extends FieldVisitor implements Opcodes {
    private final ClassFieldMover fieldMover;

    public FieldEncryptionAnnotationVisitor(ClassFieldMover fieldMover, FieldVisitor fieldVisitor) {
        super(Opcodes.ASM9, fieldVisitor);
        this.fieldMover = fieldMover;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if ("Lcom/licel/stringer/annotations/secured;".equals(descriptor)
                || "Lcom/licel/stringer/annotations/insecure;".equals(descriptor)
                || "Lcom/stringer/annotations/StringEncryption;".equals(descriptor)) {
            return null;
        }
        return this.fv.visitAnnotation(descriptor, visible);
    }
}
