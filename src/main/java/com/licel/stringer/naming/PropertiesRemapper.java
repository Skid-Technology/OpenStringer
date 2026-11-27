package com.licel.stringer.naming;

import java.util.Properties;
import org.objectweb.asm.commons.Remapper;

public class PropertiesRemapper extends Remapper {
    private final Properties mappings;

    public PropertiesRemapper(Properties mappings) {
        this.mappings = mappings;
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        return this.mappings.getProperty(owner + "." + name + "." + descriptor, name);
    }

    @Override
    public String map(String internalName) {
        return this.mappings.getProperty(internalName, internalName);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        return this.mappings.getProperty(owner + "." + name + "." + descriptor, name);
    }

    @Override
    public Object mapValue(Object value) {
        if (value != null) {
            if (value instanceof String) {
                String text = (String) value;
                if (this.mappings.containsKey(text)) {
                    return this.mappings.getProperty(text);
                }
            }
            if (value instanceof Long) {
                String key = value.toString();
                if (this.mappings.containsKey(key)) {
                    return Long.parseLong(this.mappings.getProperty(key));
                }
            }
        }
        return super.mapValue(value);
    }
}
