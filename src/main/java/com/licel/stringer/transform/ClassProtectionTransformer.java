package com.licel.stringer.transform;

import com.licel.stringer.Config;
import com.licel.stringer.crypto.CryptoContainer;
import com.licel.stringer.model.ClassInfo;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;

public class ClassProtectionTransformer extends ClassTransformer {
    private final ClassInfo classInfo;
    private final Config config;
    private final ClassInfo decoderClassInfo;
    private final Properties reflectionProperties;
    private final CryptoContainer cryptoContainer;

    public ClassProtectionTransformer(
            Config config,
            ClassInfo classInfo,
            ClassInfo decoderClassInfo,
            Properties reflectionProperties,
            CryptoContainer cryptoContainer) {
        this.classInfo = classInfo;
        this.decoderClassInfo = decoderClassInfo;
        this.reflectionProperties = reflectionProperties;
        this.cryptoContainer = cryptoContainer;
        this.config = config;
    }

    @Override
    public ClassVisitor createVisitor(ClassVisitor classVisitor) {
        return new ClassProtector(classVisitor, this.classInfo, this.config, this.decoderClassInfo, this.reflectionProperties, this.cryptoContainer);
    }

    @Override
    public int getClassWriterFlags() {
        return 0;
    }

    @Override
    public ClassInfo getClassInfo() {
        return this.classInfo;
    }
}
