package com.licel.stringer.transform;

import com.licel.stringer.Worker;
import com.licel.stringer.crypto.AESFast;
import com.licel.stringer.crypto.CryptoContainer;
import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.util.StringerUtils;
import java.security.SecureRandom;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class StringEncryptionMethodVisitor extends MethodVisitor implements Opcodes {
    private static final int MAX_CHUNK_LENGTH = 16384;

    private final String decoderClassName;
    private final String decryptMethodName;
    private boolean encryptStrings;
    private final ClassInfo classInfo;
    private final String extraDecryptSignature;
    private final CryptoContainer cryptoContainer;
    private final Worker worker;

    public StringEncryptionMethodVisitor(
            MethodVisitor methodVisitor,
            Worker worker,
            ClassInfo classInfo,
            boolean encryptStrings,
            String decoderClassName,
            String decryptMethodName,
            String extraDecryptSignature,
            CryptoContainer cryptoContainer) {
        super(Opcodes.ASM9, methodVisitor);
        this.decoderClassName = decoderClassName;
        this.decryptMethodName = decryptMethodName;
        this.extraDecryptSignature = extraDecryptSignature;
        this.cryptoContainer = cryptoContainer;
        this.encryptStrings = encryptStrings;
        this.classInfo = classInfo;
        this.worker = worker;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if ("Lcom/licel/stringer/annotations/secured;".equals(descriptor)
                || "Lcom/licel/stringer/annotation/internal/secured;".equals(descriptor)
                || "Lcom/stringer/annotations/StringEncryption;".equals(descriptor)) {
            this.encryptStrings = true;
            return null;
        }
        if ("Lcom/licel/stringer/annotations/insecure;".equals(descriptor)) {
            this.encryptStrings = false;
            return null;
        }
        return this.mv.visitAnnotation(descriptor, visible);
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (this.classInfo.annotatedInsecure) {
            this.mv.visitLdcInsn(value);
            return;
        }

        if (value instanceof String
                && (this.encryptStrings || this.classInfo.securedStringFields.containsValue(value))
                && !"".equals(value)
                && !this.classInfo.excludedStrings.contains(value)) {
            String plaintext = (String) value;
            SecureRandom random = this.cryptoContainer.getSecureRandom();
            char charSalt = (char) random.nextInt(32767);
            int charIndex = random.nextInt(plaintext.length());
            short highSalt = (short) random.nextInt(32767);
            short lowSalt = (short) random.nextInt(32767);

            String encrypted = AESFast.encryptString(
                    plaintext,
                    this.cryptoContainer.getRoundKeys(),
                    this.cryptoContainer.getCallerIvWords(),
                    charIndex,
                    charSalt,
                    highSalt << 16 | lowSalt);
            encrypted = encrypted + (char) highSalt;

            int length = encrypted.length();
            if (length < MAX_CHUNK_LENGTH) {
                this.mv.visitLdcInsn(encrypted);
            } else {
                Worker.get().config.getListener().verbose("|->String of length " + length + " detected.");
                this.mv.visitLdcInsn(encrypted.substring(0, MAX_CHUNK_LENGTH));
                int fullChunks = length / MAX_CHUNK_LENGTH;
                for (int chunk = 1; chunk < fullChunks; chunk++) {
                    this.mv.visitLdcInsn(encrypted.substring(chunk * MAX_CHUNK_LENGTH, chunk * MAX_CHUNK_LENGTH + MAX_CHUNK_LENGTH));
                    this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
                }
                int remainder = length % MAX_CHUNK_LENGTH;
                if (remainder != 0) {
                    this.mv.visitLdcInsn(encrypted.substring(length - remainder));
                    this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
                }
            }

            this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false);
            this.mv.visitInsn(Opcodes.DUP);
            this.mv.visitInsn(Opcodes.DUP);
            StringerUtils.pushIntConstant(this.mv, charIndex);
            this.mv.visitInsn(Opcodes.DUP_X1);
            this.mv.visitInsn(Opcodes.CALOAD);
            StringerUtils.pushIntConstant(this.mv, charSalt);
            this.mv.visitInsn(Opcodes.IXOR);
            this.mv.visitInsn(Opcodes.I2C);
            this.mv.visitInsn(Opcodes.CASTORE);
            StringerUtils.pushIntConstant(this.mv, lowSalt);

            for (char paramType : this.extraDecryptSignature.toCharArray()) {
                this.pushDummyArgument(this.mv, paramType);
            }

            this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.decoderClassName,
                    this.decryptMethodName,
                    "(Ljava/lang/Object;S" + this.extraDecryptSignature + ")Ljava/lang/String;",
                    false);
            Worker.get().processedStringCount++;
        } else {
            this.mv.visitLdcInsn(value);
        }
    }

    private void pushDummyArgument(MethodVisitor mv, char typeCode) {
        switch (typeCode) {
            case 'B':
            case 'I':
            case 'S':
                StringerUtils.pushIntConstant(mv, Worker.get().secureRandom.nextInt(6));
                break;
            case 'Z':
                StringerUtils.pushIntConstant(mv, Worker.get().secureRandom.nextInt(2));
                break;
            default:
                throw new IllegalArgumentException();
        }
    }
}
