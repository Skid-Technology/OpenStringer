package com.licel.stringer.transform;

import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.naming.PropertiesRemapper;
import java.util.Properties;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.ClassRemapper;

public class ClassNameRemappingTransformer extends ClassTransformer {
    private final ClassInfo classInfo;
    private final PropertiesRemapper remapper;

    public ClassNameRemappingTransformer(ClassInfo classInfo, Properties mappings) {
        this.classInfo = classInfo;
        this.remapper = new PropertiesRemapper(mappings);
    }

    @Override
    public ClassVisitor createVisitor(ClassVisitor classVisitor) {
        return new ClassRemapper(classVisitor, this.remapper);
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
