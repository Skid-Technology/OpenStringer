package com.licel.stringer.transform;

import com.licel.stringer.Config;
import com.licel.stringer.Worker;
import com.licel.stringer.crypto.CryptoContainer;
import com.licel.stringer.model.ClassInfo;
import java.util.HashMap;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class ClassEncryptionTransformer extends ClassTransformer {
    private final boolean annotationsOnly;
    private final CryptoContainer[] cryptoContainers;
    private final Properties[] decoderProperties;
    private final Worker worker;
    private final ClassInfo classInfo;
    private final Config config;
    public HashMap<String, Integer> methodContainerMap = new HashMap<>();

    public ClassEncryptionTransformer(
            Worker worker,
            boolean annotationsOnly,
            ClassInfo classInfo,
            CryptoContainer[] cryptoContainers,
            Properties[] decoderProperties,
            Config config) {
        this.cryptoContainers = cryptoContainers;
        this.decoderProperties = decoderProperties;
        this.worker = worker;
        this.annotationsOnly = annotationsOnly;
        this.classInfo = classInfo;
        this.config = config;
    }

    @Override
    public ClassVisitor createVisitor(ClassVisitor classVisitor) {
        ClassEncrypter encrypter = new ClassEncrypter(
                this.worker,
                classVisitor,
                this.annotationsOnly,
                this.classInfo,
                this.cryptoContainers,
                this.decoderProperties,
                this.config);
        if (!this.methodContainerMap.isEmpty()) {
            encrypter.methodContainerMap = this.methodContainerMap;
        }
        return encrypter;
    }

    @Override
    public int getClassWriterFlags() {
        return ClassWriter.COMPUTE_MAXS;
    }

    @Override
    public ClassInfo getClassInfo() {
        return this.classInfo;
    }
}
