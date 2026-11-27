package com.licel.stringer.runtime;

import com.licel.stringer.crypto.AESFast;
import com.licel.stringer.crypto.CryptoContainer;
import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.util.StringerUtils;
import java.util.ResourceBundle;
import java.util.Set;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ResourceBundleCompiler extends ClassVisitor implements Opcodes {
    private final ClassInfo classInfo;
    private final ResourceBundle resourceBundle;
    private final CryptoContainer cryptoContainer;
    private final String decoderClassName;
    private final String decoderMethodName;

    public ResourceBundleCompiler(
            ClassVisitor classVisitor,
            ClassInfo classInfo,
            ResourceBundle resourceBundle,
            CryptoContainer cryptoContainer,
            String decoderClassName,
            String decoderMethodName) {
        super(Opcodes.ASM9, classVisitor);
        this.classInfo = classInfo;
        this.resourceBundle = resourceBundle;
        this.cryptoContainer = cryptoContainer;
        this.decoderClassName = decoderClassName;
        this.decoderMethodName = decoderMethodName;
        this.generate();
    }

    public void generate() {
        Set<String> keys = this.resourceBundle.keySet();
        this.cv.visit(
                this.classInfo.classFileVersion,
                ACC_PUBLIC | ACC_SUPER,
                this.classInfo.name,
                null,
                "java/util/ListResourceBundle",
                null);

        MethodVisitor mv = this.cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/ListResourceBundle", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = this.cv.visitMethod(ACC_PROTECTED, "getContents", "()[[Ljava/lang/Object;", null, null);
        mv.visitCode();
        StringerUtils.pushIntConstant(mv, keys.size());
        mv.visitTypeInsn(ANEWARRAY, "[Ljava/lang/Object;");
        int index = 0;

        for (String key : keys) {
            mv.visitInsn(DUP);
            StringerUtils.pushIntConstant(mv, index++);
            mv.visitInsn(ICONST_2);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_0);
            String encryptedKey = AESFast.encryptString(
                    key, this.cryptoContainer.getRoundKeys(), this.cryptoContainer.getIvWords());
            mv.visitLdcInsn(encryptedKey);
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    this.decoderClassName,
                    this.decoderMethodName,
                    "(Ljava/lang/Object;)Ljava/lang/String;",
                    false);
            mv.visitInsn(AASTORE);
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_1);
            String encryptedValue = AESFast.encryptString(
                    this.resourceBundle.getString(key),
                    this.cryptoContainer.getRoundKeys(),
                    this.cryptoContainer.getIvWords());
            mv.visitLdcInsn(encryptedValue);
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    this.decoderClassName,
                    this.decoderMethodName,
                    "(Ljava/lang/Object;)Ljava/lang/String;",
                    false);
            mv.visitInsn(AASTORE);
            mv.visitInsn(AASTORE);
        }

        mv.visitInsn(ARETURN);
        mv.visitMaxs(7, 1);
        mv.visitEnd();

        this.cv.visitEnd();
    }
}
