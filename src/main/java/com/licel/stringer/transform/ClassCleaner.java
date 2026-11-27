package com.licel.stringer.transform;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class ClassCleaner extends ClassVisitor implements Opcodes {
    public ClassCleaner(ClassVisitor classVisitor) {
        super(Opcodes.ASM9, classVisitor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if ("Lcom/licel/stringer/annotations/secured;".equals(descriptor)
                || "Lcom/licel/stringer/annotations/insecure;".equals(descriptor)
                || "Lcom/stringer/annotations/StringEncryption;".equals(descriptor)
                || "Lcom/stringer/annotations/HideAccess;".equals(descriptor)) {
            return null;
        }
        return this.cv.visitAnnotation(descriptor, visible);
    }
}
