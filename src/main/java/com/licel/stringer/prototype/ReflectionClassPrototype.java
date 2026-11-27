package com.licel.stringer.prototype;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectionClassPrototype {
    private static final Object[] memberCache = new Object[123456];
    private static final Class<?>[] classCache = new Class<?>[98765];
    private static final int[] memberHash = new int[123456];
    private static final short[] classIds = new short[123456];
    private static final int keyX = Integer.parseInt("4444444");
    private static final int keyA = Integer.parseInt("1111111");

    private static Class<?> findClass(int memberIndex, int salt) throws ClassNotFoundException {
        int classIndex = classIds[memberIndex] & 0xFFFF;
        classIndex = (classIndex + salt) % 77777;
        Class<?> clazz = classCache[classIndex];
        if (clazz != null) {
            return clazz;
        }

        clazz = resolveClass(classIndex);
        classCache[classIndex] = clazz;
        return clazz;
    }

    private static Class<?> resolveClass(int classIndex) throws ClassNotFoundException {
        String className;
        switch (classIndex) {
            case 100:
                className = "aaaa";
                break;
            case 101:
                className = "bbbb";
                break;
            case 102:
                className = "cccc";
                break;
            default:
                throw new NoClassDefFoundError(Integer.toString(classIndex));
        }

        return Class.forName(className);
    }

    static Method findMethod(int encodedId) throws ClassNotFoundException {
        int decoded = ((encodedId - 55555555 ^ keyX) + 33333333 ^ 22222222) - keyA;
        int salt = decoded >>> 16;
        int memberIndex = decoded & 65535;
        Method cached = (Method) memberCache[memberIndex];
        if (cached != null) {
            return cached;
        }

        Class<?> targetClass = findClass(memberIndex, salt);
        Class<?> currentClass = targetClass;
        int expectedHash = memberHash[memberIndex];

        while (currentClass != null) {
            Method[] methods = currentClass.isInterface()
                ? currentClass.getMethods()
                : currentClass.getDeclaredMethods();

            for (Method method : methods) {
                int hash = salt * 31 + method.getName().hashCode();
                hash = 31 * hash + 40;
                Class<?>[] parameterTypes = method.getParameterTypes();

                for (int i = 0; i < parameterTypes.length; i++) {
                    if (i != 0) {
                        hash = 31 * hash + 44;
                    }
                    hash = 31 * hash + parameterTypes[i].getName().hashCode();
                }

                hash = 31 * hash + 41;
                hash = 31 * hash + method.getReturnType().getName().hashCode();
                hash = 31 * hash + salt;
                if (expectedHash == hash) {
                    method.setAccessible(true);
                    memberCache[memberIndex] = method;
                    return method;
                }
            }

            currentClass = currentClass.getSuperclass();
        }

        for (Class<?> cls = targetClass; cls != null; cls = cls.getSuperclass()) {
            for (Class<?> iface : cls.getInterfaces()) {
                for (Method method : iface.getMethods()) {
                    int hash = salt * 31 + method.getName().hashCode();
                    hash = 31 * hash + 40;
                    Class<?>[] parameterTypes = method.getParameterTypes();

                    for (int i = 0; i < parameterTypes.length; i++) {
                        if (i != 0) {
                            hash = 31 * hash + 44;
                        }
                        hash = 31 * hash + parameterTypes[i].getName().hashCode();
                    }

                    hash = 31 * hash + 41;
                    hash = 31 * hash + method.getReturnType().getName().hashCode();
                    hash = 31 * hash + salt;
                    if (expectedHash == hash) {
                        method.setAccessible(true);
                        memberCache[memberIndex] = method;
                        return method;
                    }
                }
            }
        }

        return null;
    }

    public static Object invokeStatic(int encodedId, Object[] args) throws Throwable {
        try {
            Method method = findMethod(encodedId);
            if (method == null) {
                throw new NoSuchMethodError(Integer.toString(encodedId));
            }
            return method.invoke(null, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    public static Object invokeVirtual(Object target, int encodedId, Object[] args) throws Throwable {
        try {
            Method method = findMethod(encodedId);
            if (method == null) {
                throw new NoSuchMethodError(Integer.toString(encodedId));
            }
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    public static Object construct(int encodedId, Object[] args) throws Throwable {
        try {
            int decoded = ((encodedId - 55555555 ^ keyX) + 33333333 ^ 22222222) - keyA;
            int salt = decoded >>> 16;
            int memberIndex = decoded & 65535;
            Class<?> targetClass = findClass(memberIndex, salt);
            Constructor<?> constructor = (Constructor<?>) memberCache[memberIndex];
            if (constructor == null) {
                int expectedHash = memberHash[memberIndex];
                for (Constructor<?> candidate : targetClass.getDeclaredConstructors()) {
                    int hash = salt * 31 + 40;
                    Class<?>[] parameterTypes = candidate.getParameterTypes();

                    for (int i = 0; i < parameterTypes.length; i++) {
                        if (i != 0) {
                            hash = 31 * hash + 44;
                        }
                        hash = 31 * hash + parameterTypes[i].getName().hashCode();
                    }

                    hash = 31 * hash + 41;
                    hash = 31 * hash + salt;
                    if (expectedHash == hash) {
                        candidate.setAccessible(true);
                        memberCache[memberIndex] = candidate;
                        constructor = candidate;
                        break;
                    }
                }
            }

            if (constructor == null) {
                throw new NoSuchMethodError(Integer.toString(memberIndex));
            }
            return constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private static Field findField(int encodedId) throws ReflectiveOperationException {
        int decoded = ((encodedId - 55555555 ^ keyX) + 33333333 ^ 22222222) - keyA;
        int salt = decoded >>> 16;
        int memberIndex = decoded & 65535;
        Class<?> targetClass = findClass(memberIndex, salt);
        Class<?> currentClass = targetClass;
        Field cached = (Field) memberCache[memberIndex];
        if (cached != null) {
            return cached;
        }

        int expectedHash = memberHash[memberIndex];

        while (currentClass != null) {
            for (Field field : currentClass.getDeclaredFields()) {
                int hash = 31 * salt + field.getName().hashCode();
                hash = 31 * hash + 58;
                hash = 31 * hash + field.getType().getName().hashCode();
                hash = 31 * hash + salt;
                if (expectedHash == hash) {
                    field.setAccessible(true);
                    if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
                        Field modifiersField = Field.class.getDeclaredField("modifiers");
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                    }
                    memberCache[memberIndex] = field;
                    return field;
                }
            }

            currentClass = currentClass.getSuperclass();
        }

        for (Class<?> cls = targetClass; cls != null; cls = cls.getSuperclass()) {
            for (Class<?> iface : cls.getInterfaces()) {
                for (Field field : iface.getFields()) {
                    int hash = 31 * salt + field.getName().hashCode();
                    hash = 31 * hash + 58;
                    hash = 31 * hash + field.getType().getName().hashCode();
                    hash = 31 * hash + salt;
                    if (expectedHash == hash) {
                        field.setAccessible(true);
                        if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
                            Field modifiersField = Field.class.getDeclaredField("modifiers");
                            modifiersField.setAccessible(true);
                            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                        }
                        memberCache[memberIndex] = field;
                        return field;
                    }
                }

                cls = cls.getSuperclass();
            }
        }

        return null;
    }

    public static Object getFieldValue(Object target, int encodedId) throws ReflectiveOperationException {
        Field field = findField(encodedId);
        if (field == null) {
            throw new NoSuchFieldError(Integer.toString(encodedId));
        }
        return field.get(target);
    }

    public static Object getStaticFieldValue(int encodedId) throws ReflectiveOperationException {
        return getFieldValue(null, encodedId);
    }

    public static void setFieldValue(Object target, int encodedId, Object value) throws ReflectiveOperationException {
        Field field = findField(encodedId);
        if (field == null) {
            throw new NoSuchFieldError(Integer.toString(encodedId));
        }
        field.set(target, value);
    }

    public static void setStaticFieldValue(int encodedId, Object value) throws ReflectiveOperationException {
        setFieldValue(null, encodedId, value);
    }

    static {
        memberHash[0] = 9999999;
        memberHash[10] = 9999999;
        memberHash[32767] = 9999999;
        memberHash[65535] = 9999999;
        classIds[0] = -1;
        classIds[1] = 32767;
        classIds[2] = -1;
    }
}
