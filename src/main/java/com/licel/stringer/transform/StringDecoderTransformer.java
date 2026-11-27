package com.licel.stringer.transform;

import com.licel.stringer.Config;
import com.licel.stringer.crypto.CryptoContainer;
import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.runtime.StringDecoderInjector;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class StringDecoderTransformer extends ClassTransformer {
    private final CryptoContainer cryptoContainer;
    private final String decoderClassName;
    private final Properties decoderProperties;
    private final long expirationTime;
    private final ClassInfo classInfo;
    private final Config config;
    private final byte[] jarCheckSalt;
    private final long integrityKey0;
    private final long integrityKey1;

    public StringDecoderTransformer(
            CryptoContainer cryptoContainer,
            String decoderClassName,
            Properties decoderProperties,
            Config config,
            long expirationTime,
            ClassInfo classInfo,
            byte[] jarCheckSalt,
            long integrityKey0,
            long integrityKey1) {
        this.cryptoContainer = cryptoContainer;
        this.decoderClassName = decoderClassName;
        this.decoderProperties = decoderProperties;
        this.expirationTime = expirationTime;
        this.classInfo = classInfo;
        this.config = config;
        this.jarCheckSalt = jarCheckSalt;
        this.integrityKey0 = integrityKey0;
        this.integrityKey1 = integrityKey1;
    }

    @Override
    public ClassVisitor createVisitor(ClassVisitor classVisitor) {
        return new StringDecoderInjector(
                classVisitor,
                this.decoderClassName,
                this.decoderProperties,
                this.cryptoContainer,
                this.config,
                this.expirationTime,
                this.jarCheckSalt,
                this.integrityKey0,
                this.integrityKey1);
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
