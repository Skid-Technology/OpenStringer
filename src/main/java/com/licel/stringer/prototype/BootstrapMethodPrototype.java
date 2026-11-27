package com.licel.stringer.prototype;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

public class BootstrapMethodPrototype {
    private static Object bootstrap(Object lookup, Object name, Object methodType) {
        try {
            return new ConstantCallSite(
                ((Lookup) lookup)
                    .unreflect(ReflectionClassPrototype.findMethod(Integer.valueOf((String) name, 32)))
                    .asType((MethodType) methodType)
            );
        } catch (ClassNotFoundException | IllegalAccessException e) {
            throw new BootstrapMethodError(e);
        }
    }
}
