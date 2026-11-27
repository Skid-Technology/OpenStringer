package com.licel.stringer.transform;

import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassIntrinsicsInjector extends ClassVisitor implements Opcodes {
    private final Properties intrinsicsProperties;

    public ClassIntrinsicsInjector(ClassVisitor classVisitor, Properties intrinsicsProperties) {
        super(Opcodes.ASM9, classVisitor);
        this.intrinsicsProperties = intrinsicsProperties;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = this.cv.visitMethod(access, name, descriptor, signature, exceptions);
        return new EqualityIntrinsicsInjector(mv, this.intrinsicsProperties);
    }
}
