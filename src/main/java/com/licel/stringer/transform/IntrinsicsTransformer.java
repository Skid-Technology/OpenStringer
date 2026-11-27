package com.licel.stringer.transform;

import com.licel.stringer.model.ClassInfo;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;

public class IntrinsicsTransformer extends ClassTransformer {
    private final ClassInfo classInfo;
    private final Properties intrinsicsProperties;

    public IntrinsicsTransformer(ClassInfo classInfo, Properties intrinsicsProperties) {
        this.classInfo = classInfo;
        this.intrinsicsProperties = intrinsicsProperties;
    }

    @Override
    public ClassVisitor createVisitor(ClassVisitor classVisitor) {
        return new ClassIntrinsicsInjector(classVisitor, this.intrinsicsProperties);
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
