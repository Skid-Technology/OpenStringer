package com.licel.stringer.transform;

import com.licel.stringer.Config;
import com.licel.stringer.Worker;
import com.licel.stringer.crypto.AESFast;
import com.licel.stringer.crypto.CryptoContainer;
import com.licel.stringer.model.ClassInfo;
import java.util.HashMap;
import java.util.Properties;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassEncrypter extends ClassVisitor implements Opcodes {
    static final String INTERNAL_SECURED_ANNOTATION = "Lcom/licel/stringer/annotation/internal/secured;";
    static final String LEGACY_SECURED_ANNOTATION = "Lcom/licel/stringer/annotations/secured;";
    static final String LEGACY_INSECURE_ANNOTATION = "Lcom/licel/stringer/annotations/insecure;";

    static HashMap<String, Object> globalCache = new HashMap<>();
    static AESFast aes;
    static Worker worker;

    private final Properties[] decoderProperties;
    private String className;
    private final CryptoContainer[] cryptoContainers;
    private boolean encryptStrings;
    private final ClassInfo classInfo;
    private final Config config;
    public HashMap<String, Integer> methodContainerMap = new HashMap<>();

    public ClassEncrypter(
            Worker worker,
            ClassVisitor classVisitor,
            boolean annotationsOnly,
            ClassInfo classInfo,
            CryptoContainer[] cryptoContainers,
            Properties[] decoderProperties,
            Config config) {
        super(Opcodes.ASM9, classVisitor);
        this.cryptoContainers = cryptoContainers;
        this.decoderProperties = decoderProperties;
        ClassEncrypter.worker = worker;
        this.classInfo = classInfo;
        this.encryptStrings = classInfo.hasExplicitStringEncryptionAnnotation ? classInfo.annotatedSecured : true;
        this.config = config;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.cv.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (LEGACY_SECURED_ANNOTATION.equals(descriptor) || "Lcom/stringer/annotations/StringEncryption;".equals(descriptor)) {
            return null;
        }
        if (LEGACY_INSECURE_ANNOTATION.equals(descriptor)) {
            this.encryptStrings = false;
            return null;
        }
        return this.cv.visitAnnotation(descriptor, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = this.cv.visitMethod(access, name, descriptor, signature, exceptions);
        String methodKey = name + descriptor;
        int containerIndex = this.methodContainerMap.containsKey(methodKey)
                ? this.methodContainerMap.get(methodKey)
                : this.cryptoContainers[0].secureRandom.nextInt(this.decoderProperties.length);
        if (this.config.getStringEncryption().isCheckCaller()) {
            String callerId = (this.className + name).replace('/', '.');
            this.cryptoContainers[containerIndex].setCallerHash(callerId.hashCode());
        }

        return new StringEncryptionMethodVisitor(
                mv,
                worker,
                this.classInfo,
                this.encryptStrings,
                this.decoderProperties[containerIndex].getProperty("className"),
                this.decoderProperties[containerIndex].getProperty("decryptString"),
                this.decoderProperties[containerIndex].getProperty("extraDecryptStringSignature"),
                this.cryptoContainers[containerIndex]);
    }
}
