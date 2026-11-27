package com.licel.stringer.util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class AsmUtils {
    private AsmUtils() {
    }

    public static MethodVisitor visitMethod(
            ClassVisitor target,
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions,
            int computeFlags) {
        if (computeFlags == 0) {
            return target.visitMethod(access, name, descriptor, signature, exceptions);
        }
        int version = Opcodes.V1_8;
        ClassWriter tempWriter = new ClassWriter(computeFlags) {
            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                try {
                    return super.getCommonSuperClass(type1, type2);
                } catch (RuntimeException ignored) {
                    return "java/lang/Object";
                }
            }
        };
        tempWriter.visit(version, Opcodes.ACC_PUBLIC, "com/licel/stringer/$MethodComputeStub", null, "java/lang/Object", null);
        MethodVisitor tempMethod = tempWriter.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodVisitor(Opcodes.ASM9, tempMethod) {
            @Override
            public void visitEnd() {
                super.visitEnd();
                tempWriter.visitEnd();
                ClassReader reader = new ClassReader(tempWriter.toByteArray());
                reader.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int mAccess, String mName, String mDesc, String mSig, String[] mExc) {
                        return target.visitMethod(mAccess, mName, mDesc, mSig, mExc);
                    }
                }, 0);
            }
        };
    }
}
