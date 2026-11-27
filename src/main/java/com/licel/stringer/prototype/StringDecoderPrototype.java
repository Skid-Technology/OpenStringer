package com.licel.stringer.prototype;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StringDecoderPrototype extends Thread {
    private static Object[] data;
    private int stage;
    private Object[] context;
    private static BigInteger[] syncBuffer = new BigInteger[1];

    StringDecoderPrototype(int stage, Object[] context) {
        this.stage = stage;
        this.context = context;
    }

    private static int getConstantPoolSize(Class<?> clazz) throws ReflectiveOperationException {
        Class<?> sharedSecretsClass;
        try {
            sharedSecretsClass = Class.forName("sun.misc.SharedSecrets");
        } catch (ClassNotFoundException e) {
            try {
                sharedSecretsClass = Class.forName("jdk.internal.misc.SharedSecrets");
            } catch (ClassNotFoundException ex) {
                sharedSecretsClass = Class.forName("jdk.internal.access.SharedSecrets");
            }
        }
        Method getJavaLangAccess = sharedSecretsClass.getMethod("getJavaLangAccess");
        Object javaLangAccess = getJavaLangAccess.invoke(null);
        Method getConstantPool = getJavaLangAccess.getReturnType().getMethod("getConstantPool", Class.class);
        Object constantPool = getConstantPool.invoke(javaLangAccess, clazz);
        Method getSize = getConstantPool.getReturnType().getMethod("getSize");
        return (Integer) getSize.invoke(constantPool);
    }

    private static int shift(int value, int bits) {
        return value >>> bits | value << -bits;
    }

    private static int subWord(byte[] sbox, int word) {
        int result = sbox[14] << 16;

        try {
            result = (sbox[word & 0xFF] & 0xFF)
                | (sbox[(word >> 8) & 0xFF] & 0xFF) << 8
                | (sbox[(word >> 16) & 0xFF] & 0xFF) << 16
                | sbox[(word >> 24) & 0xFF] << 24;
        } catch (Exception e) {
            try {
                result = sbox[word & 127] >> 8;
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    static String decryptStringWithConstantPool(String encrypted) throws ReflectiveOperationException {
        if (data == null) {
            initKeys();
        }

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String callContext = getConstantPoolSize(Class.forName(stackTrace[2].getClassName()))
            + stackTrace[2].getClassName()
            + stackTrace[2].getMethodName()
            + getConstantPoolSize(Class.forName(stackTrace[3].getClassName()));
        int callerHash = callContext.hashCode();
        int[] iv = (int[]) data[6];
        int s0 = callerHash ^ iv[0];
        int s1 = callerHash ^ iv[1];
        int s2 = callerHash ^ iv[2];
        int s3 = callerHash ^ iv[3];
        int[] roundKeys = (int[]) data[5];
        int[] te0 = (int[]) data[1];
        int[] te1 = (int[]) data[2];
        int[] te2 = (int[]) data[3];
        int[] te3 = (int[]) data[4];
        byte[] sbox = (byte[]) data[0];
        char[] chars = encrypted.toCharArray();

        try {
            int length = chars.length;

            for (int i = 0; i < length; i++) {
                if (i % 8 == 0) {
                    s0 ^= roundKeys[0];
                    s1 ^= roundKeys[1];
                    s2 ^= roundKeys[2];
                    s3 ^= roundKeys[3];
                    int keyIndex = 4;

                    while (keyIndex < 36) {
                        int t0 = te0[s0 & 0xFF] ^ te1[(s1 >> 8) & 0xFF] ^ te2[(s2 >> 16) & 0xFF] ^ te3[s3 >>> 24] ^ roundKeys[keyIndex];
                        int t1 = te0[s1 & 0xFF] ^ te1[(s2 >> 8) & 0xFF] ^ te2[(s3 >> 16) & 0xFF] ^ te3[s0 >>> 24] ^ roundKeys[keyIndex + 1];
                        int t2 = te0[s2 & 0xFF] ^ te1[(s3 >> 8) & 0xFF] ^ te2[(s0 >> 16) & 0xFF] ^ te3[s1 >>> 24] ^ roundKeys[keyIndex + 2];
                        int t3 = te0[s3 & 0xFF] ^ te1[(s0 >> 8) & 0xFF] ^ te2[(s1 >> 16) & 0xFF] ^ te3[s2 >>> 24] ^ roundKeys[keyIndex + 3];
                        keyIndex += 4;
                        s0 = te0[t0 & 0xFF] ^ te1[(t1 >> 8) & 0xFF] ^ te2[(t2 >> 16) & 0xFF] ^ te3[t3 >>> 24] ^ roundKeys[keyIndex];
                        s1 = te0[t1 & 0xFF] ^ te1[(t2 >> 8) & 0xFF] ^ te2[(t3 >> 16) & 0xFF] ^ te3[t0 >>> 24] ^ roundKeys[keyIndex + 1];
                        s2 = te0[t2 & 0xFF] ^ te1[(t3 >> 8) & 0xFF] ^ te2[(t0 >> 16) & 0xFF] ^ te3[t1 >>> 24] ^ roundKeys[keyIndex + 2];
                        s3 = te0[t3 & 0xFF] ^ te1[(t0 >> 8) & 0xFF] ^ te2[(t1 >> 16) & 0xFF] ^ te3[t2 >>> 24] ^ roundKeys[keyIndex + 3];
                        keyIndex += 4;
                    }

                    int t0 = te0[s0 & 0xFF] ^ te1[(s1 >> 8) & 0xFF] ^ te2[(s2 >> 16) & 0xFF] ^ te3[s3 >>> 24] ^ roundKeys[keyIndex];
                    int t1 = te0[s1 & 0xFF] ^ te1[(s2 >> 8) & 0xFF] ^ te2[(s3 >> 16) & 0xFF] ^ te3[s0 >>> 24] ^ roundKeys[keyIndex + 1];
                    int t2 = te0[s2 & 0xFF] ^ te1[(s3 >> 8) & 0xFF] ^ te2[(s0 >> 16) & 0xFF] ^ te3[s1 >>> 24] ^ roundKeys[keyIndex + 2];
                    int t3 = te0[s3 & 0xFF] ^ te1[(s0 >> 8) & 0xFF] ^ te2[(s1 >> 16) & 0xFF] ^ te3[s2 >>> 24] ^ roundKeys[keyIndex + 3];
                    keyIndex += 4;
                    s0 = (sbox[t0 & 0xFF] & 0xFF)
                        ^ (sbox[(t1 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t2 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t3 >>> 24] << 24
                        ^ roundKeys[keyIndex];
                    s1 = (sbox[t1 & 0xFF] & 0xFF)
                        ^ (sbox[(t2 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t3 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t0 >>> 24] << 24
                        ^ roundKeys[keyIndex + 1];
                    s2 = (sbox[t2 & 0xFF] & 0xFF)
                        ^ (sbox[(t3 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t0 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t1 >>> 24] << 24
                        ^ roundKeys[keyIndex + 2];
                    s3 = (sbox[t3 & 0xFF] & 0xFF)
                        ^ (sbox[(t0 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t1 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t2 >>> 24] << 24
                        ^ roundKeys[keyIndex + 3];
                }

                try {
                    switch (i % 8) {
                        case 0:
                            chars[i] = (char) ((s0 >> 16) ^ chars[i]);
                            break;
                        case 1:
                            chars[i] = (char) (s0 ^ chars[i]);
                            break;
                        case 2:
                            chars[i] = (char) ((s1 >> 16) ^ chars[i]);
                            break;
                        case 3:
                            chars[i] = (char) (s1 ^ chars[i]);
                            break;
                        case 4:
                            chars[i] = (char) ((s2 >> 16) ^ chars[i]);
                            break;
                        case 5:
                            chars[i] = (char) (s2 ^ chars[i]);
                            break;
                        case 6:
                            chars[i] = (char) ((s3 >> 16) ^ chars[i]);
                            break;
                        case 7:
                            chars[i] = (char) (s3 ^ chars[i]);
                            break;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        return new String(chars);
    }

    static String decryptString(String encrypted) {
        if (data == null) {
            initKeys();
        }

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String callContext = stackTrace[2].getClassName() + stackTrace[2].getMethodName();
        int callerHash = callContext.hashCode();
        int[] iv = (int[]) data[6];
        int s0 = callerHash ^ iv[0];
        int s1 = callerHash ^ iv[1];
        int s2 = callerHash ^ iv[2];
        int s3 = callerHash ^ iv[3];
        int[] roundKeys = (int[]) data[5];
        int[] te0 = (int[]) data[1];
        int[] te1 = (int[]) data[2];
        int[] te2 = (int[]) data[3];
        int[] te3 = (int[]) data[4];
        byte[] sbox = (byte[]) data[0];
        char[] chars = encrypted.toCharArray();

        try {
            int length = chars.length;

            for (int i = 0; i < length; i++) {
                if (i % 8 == 0) {
                    s0 ^= roundKeys[0];
                    s1 ^= roundKeys[1];
                    s2 ^= roundKeys[2];
                    s3 ^= roundKeys[3];
                    int keyIndex = 4;

                    while (keyIndex < 36) {
                        int t0 = te0[s0 & 0xFF] ^ te1[(s1 >> 8) & 0xFF] ^ te2[(s2 >> 16) & 0xFF] ^ te3[s3 >>> 24] ^ roundKeys[keyIndex];
                        int t1 = te0[s1 & 0xFF] ^ te1[(s2 >> 8) & 0xFF] ^ te2[(s3 >> 16) & 0xFF] ^ te3[s0 >>> 24] ^ roundKeys[keyIndex + 1];
                        int t2 = te0[s2 & 0xFF] ^ te1[(s3 >> 8) & 0xFF] ^ te2[(s0 >> 16) & 0xFF] ^ te3[s1 >>> 24] ^ roundKeys[keyIndex + 2];
                        int t3 = te0[s3 & 0xFF] ^ te1[(s0 >> 8) & 0xFF] ^ te2[(s1 >> 16) & 0xFF] ^ te3[s2 >>> 24] ^ roundKeys[keyIndex + 3];
                        keyIndex += 4;
                        s0 = te0[t0 & 0xFF] ^ te1[(t1 >> 8) & 0xFF] ^ te2[(t2 >> 16) & 0xFF] ^ te3[t3 >>> 24] ^ roundKeys[keyIndex];
                        s1 = te0[t1 & 0xFF] ^ te1[(t2 >> 8) & 0xFF] ^ te2[(t3 >> 16) & 0xFF] ^ te3[t0 >>> 24] ^ roundKeys[keyIndex + 1];
                        s2 = te0[t2 & 0xFF] ^ te1[(t3 >> 8) & 0xFF] ^ te2[(t0 >> 16) & 0xFF] ^ te3[t1 >>> 24] ^ roundKeys[keyIndex + 2];
                        s3 = te0[t3 & 0xFF] ^ te1[(t0 >> 8) & 0xFF] ^ te2[(t1 >> 16) & 0xFF] ^ te3[t2 >>> 24] ^ roundKeys[keyIndex + 3];
                        keyIndex += 4;
                    }

                    int t0 = te0[s0 & 0xFF] ^ te1[(s1 >> 8) & 0xFF] ^ te2[(s2 >> 16) & 0xFF] ^ te3[s3 >>> 24] ^ roundKeys[keyIndex];
                    int t1 = te0[s1 & 0xFF] ^ te1[(s2 >> 8) & 0xFF] ^ te2[(s3 >> 16) & 0xFF] ^ te3[s0 >>> 24] ^ roundKeys[keyIndex + 1];
                    int t2 = te0[s2 & 0xFF] ^ te1[(s3 >> 8) & 0xFF] ^ te2[(s0 >> 16) & 0xFF] ^ te3[s1 >>> 24] ^ roundKeys[keyIndex + 2];
                    int t3 = te0[s3 & 0xFF] ^ te1[(s0 >> 8) & 0xFF] ^ te2[(s1 >> 16) & 0xFF] ^ te3[s2 >>> 24] ^ roundKeys[keyIndex + 3];
                    keyIndex += 4;
                    s0 = (sbox[t0 & 0xFF] & 0xFF)
                        ^ (sbox[(t1 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t2 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t3 >>> 24] << 24
                        ^ roundKeys[keyIndex];
                    s1 = (sbox[t1 & 0xFF] & 0xFF)
                        ^ (sbox[(t2 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t3 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t0 >>> 24] << 24
                        ^ roundKeys[keyIndex + 1];
                    s2 = (sbox[t2 & 0xFF] & 0xFF)
                        ^ (sbox[(t3 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t0 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t1 >>> 24] << 24
                        ^ roundKeys[keyIndex + 2];
                    s3 = (sbox[t3 & 0xFF] & 0xFF)
                        ^ (sbox[(t0 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t1 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t2 >>> 24] << 24
                        ^ roundKeys[keyIndex + 3];
                }

                try {
                    switch (i % 8) {
                        case 0:
                            chars[i] = (char) ((s0 >> 16) ^ chars[i]);
                            break;
                        case 1:
                            chars[i] = (char) (s0 ^ chars[i]);
                            break;
                        case 2:
                            chars[i] = (char) ((s1 >> 16) ^ chars[i]);
                            break;
                        case 3:
                            chars[i] = (char) (s1 ^ chars[i]);
                            break;
                        case 4:
                            chars[i] = (char) ((s2 >> 16) ^ chars[i]);
                            break;
                        case 5:
                            chars[i] = (char) (s2 ^ chars[i]);
                            break;
                        case 6:
                            chars[i] = (char) ((s3 >> 16) ^ chars[i]);
                            break;
                        case 7:
                            chars[i] = (char) (s3 ^ chars[i]);
                            break;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        return new String(chars);
    }

    static String decryptStringFromParentCaller(String encrypted) {
        if (data == null) {
            initKeys();
        }

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String callContext = stackTrace[3].getClassName() + stackTrace[3].getMethodName();
        int callerHash = callContext.hashCode();
        int[] iv = (int[]) data[6];
        int s0 = callerHash ^ iv[0];
        int s1 = callerHash ^ iv[1];
        int s2 = callerHash ^ iv[2];
        int s3 = callerHash ^ iv[3];
        int[] roundKeys = (int[]) data[5];
        int[] te0 = (int[]) data[1];
        int[] te1 = (int[]) data[2];
        int[] te2 = (int[]) data[3];
        int[] te3 = (int[]) data[4];
        byte[] sbox = (byte[]) data[0];
        char[] chars = encrypted.toCharArray();

        try {
            int length = chars.length;

            for (int i = 0; i < length; i++) {
                if (i % 8 == 0) {
                    s0 ^= roundKeys[0];
                    s1 ^= roundKeys[1];
                    s2 ^= roundKeys[2];
                    s3 ^= roundKeys[3];
                    int keyIndex = 4;

                    while (keyIndex < 36) {
                        int t0 = te0[s0 & 0xFF] ^ te1[(s1 >> 8) & 0xFF] ^ te2[(s2 >> 16) & 0xFF] ^ te3[s3 >>> 24] ^ roundKeys[keyIndex];
                        int t1 = te0[s1 & 0xFF] ^ te1[(s2 >> 8) & 0xFF] ^ te2[(s3 >> 16) & 0xFF] ^ te3[s0 >>> 24] ^ roundKeys[keyIndex + 1];
                        int t2 = te0[s2 & 0xFF] ^ te1[(s3 >> 8) & 0xFF] ^ te2[(s0 >> 16) & 0xFF] ^ te3[s1 >>> 24] ^ roundKeys[keyIndex + 2];
                        int t3 = te0[s3 & 0xFF] ^ te1[(s0 >> 8) & 0xFF] ^ te2[(s1 >> 16) & 0xFF] ^ te3[s2 >>> 24] ^ roundKeys[keyIndex + 3];
                        keyIndex += 4;
                        s0 = te0[t0 & 0xFF] ^ te1[(t1 >> 8) & 0xFF] ^ te2[(t2 >> 16) & 0xFF] ^ te3[t3 >>> 24] ^ roundKeys[keyIndex];
                        s1 = te0[t1 & 0xFF] ^ te1[(t2 >> 8) & 0xFF] ^ te2[(t3 >> 16) & 0xFF] ^ te3[t0 >>> 24] ^ roundKeys[keyIndex + 1];
                        s2 = te0[t2 & 0xFF] ^ te1[(t3 >> 8) & 0xFF] ^ te2[(t0 >> 16) & 0xFF] ^ te3[t1 >>> 24] ^ roundKeys[keyIndex + 2];
                        s3 = te0[t3 & 0xFF] ^ te1[(t0 >> 8) & 0xFF] ^ te2[(t1 >> 16) & 0xFF] ^ te3[t2 >>> 24] ^ roundKeys[keyIndex + 3];
                        keyIndex += 4;
                    }

                    int t0 = te0[s0 & 0xFF] ^ te1[(s1 >> 8) & 0xFF] ^ te2[(s2 >> 16) & 0xFF] ^ te3[s3 >>> 24] ^ roundKeys[keyIndex];
                    int t1 = te0[s1 & 0xFF] ^ te1[(s2 >> 8) & 0xFF] ^ te2[(s3 >> 16) & 0xFF] ^ te3[s0 >>> 24] ^ roundKeys[keyIndex + 1];
                    int t2 = te0[s2 & 0xFF] ^ te1[(s3 >> 8) & 0xFF] ^ te2[(s0 >> 16) & 0xFF] ^ te3[s1 >>> 24] ^ roundKeys[keyIndex + 2];
                    int t3 = te0[s3 & 0xFF] ^ te1[(s0 >> 8) & 0xFF] ^ te2[(s1 >> 16) & 0xFF] ^ te3[s2 >>> 24] ^ roundKeys[keyIndex + 3];
                    keyIndex += 4;
                    s0 = (sbox[t0 & 0xFF] & 0xFF)
                        ^ (sbox[(t1 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t2 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t3 >>> 24] << 24
                        ^ roundKeys[keyIndex];
                    s1 = (sbox[t1 & 0xFF] & 0xFF)
                        ^ (sbox[(t2 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t3 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t0 >>> 24] << 24
                        ^ roundKeys[keyIndex + 1];
                    s2 = (sbox[t2 & 0xFF] & 0xFF)
                        ^ (sbox[(t3 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t0 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t1 >>> 24] << 24
                        ^ roundKeys[keyIndex + 2];
                    s3 = (sbox[t3 & 0xFF] & 0xFF)
                        ^ (sbox[(t0 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t1 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t2 >>> 24] << 24
                        ^ roundKeys[keyIndex + 3];
                }

                try {
                    switch (i % 8) {
                        case 0:
                            chars[i] = (char) ((s0 >> 16) ^ chars[i]);
                            break;
                        case 1:
                            chars[i] = (char) (s0 ^ chars[i]);
                            break;
                        case 2:
                            chars[i] = (char) ((s1 >> 16) ^ chars[i]);
                            break;
                        case 3:
                            chars[i] = (char) (s1 ^ chars[i]);
                            break;
                        case 4:
                            chars[i] = (char) ((s2 >> 16) ^ chars[i]);
                            break;
                        case 5:
                            chars[i] = (char) (s2 ^ chars[i]);
                            break;
                        case 6:
                            chars[i] = (char) ((s3 >> 16) ^ chars[i]);
                            break;
                        case 7:
                            chars[i] = (char) (s3 ^ chars[i]);
                            break;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        return new String(chars);
    }

    static String decryptStringWithoutCallContext(String encrypted) {
        if (data == null) {
            initKeys();
        }

        int[] iv = (int[]) data[6];
        int s0 = iv[0];
        int s1 = iv[1];
        int s2 = iv[2];
        int s3 = iv[3];
        int[] roundKeys = (int[]) data[5];
        int[] te0 = (int[]) data[1];
        int[] te1 = (int[]) data[2];
        int[] te2 = (int[]) data[3];
        int[] te3 = (int[]) data[4];
        byte[] sbox = (byte[]) data[0];
        char[] chars = encrypted.toCharArray();

        try {
            int length = chars.length;

            for (int i = 0; i < length; i++) {
                if (i % 8 == 0) {
                    s0 ^= roundKeys[0];
                    s1 ^= roundKeys[1];
                    s2 ^= roundKeys[2];
                    s3 ^= roundKeys[3];
                    int keyIndex = 4;

                    while (keyIndex < 36) {
                        int t0 = te0[s0 & 0xFF] ^ te1[(s1 >> 8) & 0xFF] ^ te2[(s2 >> 16) & 0xFF] ^ te3[s3 >>> 24] ^ roundKeys[keyIndex];
                        int t1 = te0[s1 & 0xFF] ^ te1[(s2 >> 8) & 0xFF] ^ te2[(s3 >> 16) & 0xFF] ^ te3[s0 >>> 24] ^ roundKeys[keyIndex + 1];
                        int t2 = te0[s2 & 0xFF] ^ te1[(s3 >> 8) & 0xFF] ^ te2[(s0 >> 16) & 0xFF] ^ te3[s1 >>> 24] ^ roundKeys[keyIndex + 2];
                        int t3 = te0[s3 & 0xFF] ^ te1[(s0 >> 8) & 0xFF] ^ te2[(s1 >> 16) & 0xFF] ^ te3[s2 >>> 24] ^ roundKeys[keyIndex + 3];
                        keyIndex += 4;
                        s0 = te0[t0 & 0xFF] ^ te1[(t1 >> 8) & 0xFF] ^ te2[(t2 >> 16) & 0xFF] ^ te3[t3 >>> 24] ^ roundKeys[keyIndex];
                        s1 = te0[t1 & 0xFF] ^ te1[(t2 >> 8) & 0xFF] ^ te2[(t3 >> 16) & 0xFF] ^ te3[t0 >>> 24] ^ roundKeys[keyIndex + 1];
                        s2 = te0[t2 & 0xFF] ^ te1[(t3 >> 8) & 0xFF] ^ te2[(t0 >> 16) & 0xFF] ^ te3[t1 >>> 24] ^ roundKeys[keyIndex + 2];
                        s3 = te0[t3 & 0xFF] ^ te1[(t0 >> 8) & 0xFF] ^ te2[(t1 >> 16) & 0xFF] ^ te3[t2 >>> 24] ^ roundKeys[keyIndex + 3];
                        keyIndex += 4;
                    }

                    int t0 = te0[s0 & 0xFF] ^ te1[(s1 >> 8) & 0xFF] ^ te2[(s2 >> 16) & 0xFF] ^ te3[s3 >>> 24] ^ roundKeys[keyIndex];
                    int t1 = te0[s1 & 0xFF] ^ te1[(s2 >> 8) & 0xFF] ^ te2[(s3 >> 16) & 0xFF] ^ te3[s0 >>> 24] ^ roundKeys[keyIndex + 1];
                    int t2 = te0[s2 & 0xFF] ^ te1[(s3 >> 8) & 0xFF] ^ te2[(s0 >> 16) & 0xFF] ^ te3[s1 >>> 24] ^ roundKeys[keyIndex + 2];
                    int t3 = te0[s3 & 0xFF] ^ te1[(s0 >> 8) & 0xFF] ^ te2[(s1 >> 16) & 0xFF] ^ te3[s2 >>> 24] ^ roundKeys[keyIndex + 3];
                    keyIndex += 4;
                    s0 = (sbox[t0 & 0xFF] & 0xFF)
                        ^ (sbox[(t1 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t2 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t3 >>> 24] << 24
                        ^ roundKeys[keyIndex];
                    s1 = (sbox[t1 & 0xFF] & 0xFF)
                        ^ (sbox[(t2 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t3 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t0 >>> 24] << 24
                        ^ roundKeys[keyIndex + 1];
                    s2 = (sbox[t2 & 0xFF] & 0xFF)
                        ^ (sbox[(t3 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t0 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t1 >>> 24] << 24
                        ^ roundKeys[keyIndex + 2];
                    s3 = (sbox[t3 & 0xFF] & 0xFF)
                        ^ (sbox[(t0 >> 8) & 0xFF] & 0xFF) << 8
                        ^ (sbox[(t1 >> 16) & 0xFF] & 0xFF) << 16
                        ^ sbox[t2 >>> 24] << 24
                        ^ roundKeys[keyIndex + 3];
                }

                try {
                    switch (i % 8) {
                        case 0:
                            chars[i] = (char) ((s0 >> 16) ^ chars[i]);
                            break;
                        case 1:
                            chars[i] = (char) (s0 ^ chars[i]);
                            break;
                        case 2:
                            chars[i] = (char) ((s1 >> 16) ^ chars[i]);
                            break;
                        case 3:
                            chars[i] = (char) (s1 ^ chars[i]);
                            break;
                        case 4:
                            chars[i] = (char) ((s2 >> 16) ^ chars[i]);
                            break;
                        case 5:
                            chars[i] = (char) (s2 ^ chars[i]);
                            break;
                        case 6:
                            chars[i] = (char) ((s3 >> 16) ^ chars[i]);
                            break;
                        case 7:
                            chars[i] = (char) (s3 ^ chars[i]);
                            break;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        return new String(chars);
    }

    private static long getExpirationTimestamp(long seed) {
        return 12345677L;
    }

    private static long getIntegritySalt() {
        return 0L;
    }

    @Override
    public void run() {
        try {
            executeInitStage(this.stage, this.context);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void initKeysParallel() throws InterruptedException {
        Object[] state = new Object[9];
        executeInitStage(0, state);
        calcStackDepth(state);
        StringDecoderPrototype tableWorker = new StringDecoderPrototype(1, state);
        tableWorker.start();
        tableWorker.join();
        StringDecoderPrototype keyWorker = new StringDecoderPrototype(3, state);
        keyWorker.start();
        StringDecoderPrototype ivWorker = new StringDecoderPrototype(4, state);
        ivWorker.start();
        keyWorker.join();
        ivWorker.join();
        StringDecoderPrototype modPowWorkerA = new StringDecoderPrototype(7, state);
        StringDecoderPrototype modPowWorkerB = new StringDecoderPrototype(8, state);
        modPowWorkerA.start();
        modPowWorkerB.start();
        modPowWorkerA.join();
        modPowWorkerB.join();
        data = state;
    }

    static void executeInitStage(int stage, Object stateObj) throws InterruptedException {
        Object[] state = (Object[]) stateObj;
        switch (stage) {
            case 0:
                state[0] = new byte[256];
                state[1] = new int[256];
                state[2] = new int[256];
                state[3] = new int[256];
                state[4] = new int[256];
                break;
            case 1: {
                int[] gfExp = new int[256];
                int val = 1;
                for (int i = 0; i < 256; i++) {
                    gfExp[i] = val;
                    val ^= (val << 1) ^ ((val >>> 7) * 283);
                }
                state[8] = gfExp;
                executeInitStage(2, state);
                break;
            }
            case 2: {
                byte[] sbox = (byte[]) state[0];
                sbox[0] = 99;
                int[] gfExp = (int[]) state[8];

                for (int i = 0; i < 255; i++) {
                    int x = gfExp[255 - i];
                    x |= x << 8;
                    x ^= (x >> 4) ^ (x >> 5) ^ (x >> 6) ^ (x >> 7);
                    sbox[gfExp[i]] = (byte) (x ^ 99);
                }

                int[] te0 = (int[]) state[1];
                int[] te1 = (int[]) state[2];
                int[] te2 = (int[]) state[3];
                int[] te3 = (int[]) state[4];

                for (int i = 0; i < 256; i++) {
                    int s = sbox[i] & 0xFF;
                    int s2 = (s << 1) ^ ((s >>> 7) * 283);
                    int t = ((s ^ s2) << 24) ^ (s << 16) ^ (s << 8) ^ s2;
                    t &= -1;
                    te0[i] = t;
                    te1[i] = (t << 8) | (t >>> -8);
                    te2[i] = (t << 16) | (t >>> -16);
                    te3[i] = (t << 24) | (t >>> -24);
                }
                break;
            }
            case 3: {
                char[] watermarkChars = "watermark".toCharArray();
                byte[] watermarkBytes = new byte[watermarkChars.length * 2];
                for (int i = 0; i < watermarkChars.length; i++) {
                    watermarkBytes[i * 2] = (byte) (watermarkChars[i] & 0xFF);
                    watermarkBytes[i * 2 + 1] = (byte) ((watermarkChars[i] & 0xFF00) >> 8);
                }

                char[] sboxChars = "sbox".toCharArray();
                byte[] sboxBytes = new byte[sboxChars.length * 2];
                for (int i = 0; i < sboxChars.length; i++) {
                    sboxBytes[i * 2] = (byte) (sboxChars[i] & 0xFF);
                    sboxBytes[i * 2 + 1] = (byte) ((sboxChars[i] & 0xFF00) >> 8);
                }

                byte[] keyBytes = new byte[16];
                try {
                    keyBytes[0] = watermarkBytes[sboxBytes[80] & 0xFF];
                    keyBytes[1] = watermarkBytes[sboxBytes[81] & 0xFF];
                    keyBytes[2] = watermarkBytes[sboxBytes[82] & 0xFF];
                    keyBytes[3] = watermarkBytes[sboxBytes[83] & 0xFF];
                    keyBytes[4] = watermarkBytes[sboxBytes[84] & 0xFF];
                    keyBytes[5] = watermarkBytes[sboxBytes[85] & 0xFF];
                    keyBytes[6] = watermarkBytes[sboxBytes[86] & 0xFF];
                    keyBytes[7] = watermarkBytes[sboxBytes[87] & 0xFF];
                } catch (Exception ignored) {
                } finally {
                    keyBytes[8] = watermarkBytes[sboxBytes[88] & 0xFF];
                    keyBytes[9] = watermarkBytes[sboxBytes[89] & 0xFF];
                    keyBytes[10] = watermarkBytes[sboxBytes[90] & 0xFF];
                    keyBytes[11] = watermarkBytes[sboxBytes[91] & 0xFF];
                    keyBytes[12] = watermarkBytes[sboxBytes[92] & 0xFF];
                    keyBytes[13] = watermarkBytes[sboxBytes[93] & 0xFF];
                    keyBytes[14] = watermarkBytes[sboxBytes[94] & 0xFF];
                    keyBytes[15] = watermarkBytes[sboxBytes[95] & 0xFF];
                }

                state[8] = keyBytes;
                executeInitStage(5, state);
                break;
            }
            case 4: {
                int[] iv = new int[]{12345678, 12345679, 12345680, 12345681};
                long integrityHash = calcIntegrityHash() ^ (((Long.MAX_VALUE - System.currentTimeMillis()) >> 63) & 1L);
                iv[2] ^= (int) integrityHash;
                state[6] = iv;
                break;
            }
            case 5: {
                byte[] keyBytes = (byte[]) state[8];
                int rounds = 4 + 6;
                int[] roundKeys = new int[(rounds + 1) * 4];
                int wordIndex = 0;
                int byteOffset = 0;

                try {
                    while (byteOffset < 16) {
                        roundKeys[(wordIndex >> 2) * 4 + (wordIndex & 3)] = (keyBytes[byteOffset] & 0xFF)
                            | (keyBytes[byteOffset + 1] & 0xFF) << 8
                            | (keyBytes[byteOffset + 2] & 0xFF) << 16
                            | keyBytes[byteOffset + 3] << 24;
                        byteOffset += 4;
                        wordIndex++;
                    }
                } catch (Exception ignored) {
                }

                state[5] = roundKeys;
                executeInitStage(6, state);
                break;
            }
            case 6: {
                int[] rcon = new int[30];
                int val = 1;
                for (int i = 0; i < 30; i++) {
                    rcon[i] = val;
                    val = (val << 1) ^ ((val >>> 7) * 283);
                }

                int[] roundKeys = (int[]) state[5];
                byte[] sbox = (byte[]) state[0];

                for (int i = 4; i < 44; i++) {
                    int temp = roundKeys[((i - 1) >> 2) * 4 + ((i - 1) & 3)];
                    if (i % 4 == 0) {
                        temp = subWord(sbox, shift(temp, 8)) ^ rcon[i / 4 - 1];
                    }
                    roundKeys[(i >> 2) * 4 + (i & 3)] = roundKeys[((i - 4) >> 2) * 4 + ((i - 4) & 3)] ^ temp;
                }
                break;
            }
            case 7: {
                BigInteger[] shared = syncBuffer;
                BigInteger exponent = new BigInteger("aTE", 16);
                BigInteger modulus = new BigInteger("aTM", 36);
                BigInteger addend = new BigInteger("aTA", 36);
                BigInteger xorMask = new BigInteger("aTX", 36);
                BigInteger acc = new BigInteger("aTR", 36);

                for (int outer = 0; outer < 4; outer++) {
                    for (int inner = 0; inner < 8; inner++) {
                        acc = acc.add(addend).xor(xorMask).modPow(exponent, modulus);
                    }

                    synchronized (shared) {
                        while (shared[0] == null) {
                            shared.wait(5000L);
                        }
                        addend = addend.xor(shared[0]);
                        shared[0] = null;
                    }
                }

                ((int[]) state[6])[0] ^= acc.intValue();
                break;
            }
            case 8: {
                BigInteger[] shared = syncBuffer;
                BigInteger exponent = new BigInteger("bTE", 16);
                BigInteger modulus = new BigInteger("bTM", 36);
                BigInteger addend = new BigInteger("bTA", 36);
                BigInteger xorMask = new BigInteger("bTX", 36);
                BigInteger acc = new BigInteger("bTR", 36);

                for (int outer = 0; outer < 4; outer++) {
                    for (int inner = 0; inner < 8; inner++) {
                        acc = acc.add(addend).xor(xorMask).modPow(exponent, modulus);
                    }

                    while (true) {
                        Thread.yield();
                        synchronized (shared) {
                            if (shared[0] == null) {
                                shared[0] = acc;
                                shared.notifyAll();
                                break;
                            }
                            shared.notifyAll();
                        }
                    }
                }

                ((int[]) state[6])[1] ^= acc.intValue();
                break;
            }
        }
    }

    static void initKeys() {
        int[] gfExp = new int[256];
        byte[] sbox = new byte[256];
        int[] te0 = new int[256];
        int[] te1 = new int[256];
        int[] te2 = new int[256];
        int[] te3 = new int[256];
        int[] rcon = new int[30];

        int val = 1;
        for (int i = 0; i < 256; i++) {
            gfExp[i] = val;
            val ^= (val << 1) ^ ((val >>> 7) * 283);
        }

        sbox[0] = 99;

        try {
            for (int i = 0; i < 255; i++) {
                int x = gfExp[255 - i];
                x |= x << 8;
                x ^= (x >> 4) ^ (x >> 5) ^ (x >> 6) ^ (x >> 7);
                sbox[gfExp[i]] = (byte) (x ^ 99);
            }

            for (int i = 0; i < 256; i++) {
                int s = sbox[i] & 0xFF;
                int s2 = (s << 1) ^ ((s >>> 7) * 283);
                int t = ((s ^ s2) << 24) ^ (s << 16) ^ (s << 8) ^ s2;
                t &= -1;
                te0[i] = t;
                te1[i] = (t << 8) | (t >>> -8);
                te2[i] = (t << 16) | (t >>> -16);
                te3[i] = (t << 24) | (t >>> -24);
            }

            val = 1;
            for (int i = 0; i < 30; i++) {
                rcon[i] = val;
                val = (val << 1) ^ ((val >>> 7) * 283);
            }
        } catch (Exception ignored) {
        }

        char[] watermarkChars = "watermark".toCharArray();
        byte[] watermarkBytes = new byte[watermarkChars.length * 2];
        for (int i = 0; i < watermarkChars.length; i++) {
            watermarkBytes[i * 2] = (byte) (watermarkChars[i] & 0xFF);
            watermarkBytes[i * 2 + 1] = (byte) ((watermarkChars[i] & 0xFF00) >> 8);
        }

        char[] sboxChars = "sbox".toCharArray();
        byte[] sboxBytes = new byte[sboxChars.length * 2];
        for (int i = 0; i < sboxChars.length; i++) {
            sboxBytes[i * 2] = (byte) (sboxChars[i] & 0xFF);
            sboxBytes[i * 2 + 1] = (byte) ((sboxChars[i] & 0xFF00) >> 8);
        }

        byte[] keyBytes = new byte[16];
        try {
            keyBytes[0] = watermarkBytes[sboxBytes[80] & 0xFF];
            keyBytes[1] = watermarkBytes[sboxBytes[81] & 0xFF];
            keyBytes[2] = watermarkBytes[sboxBytes[82] & 0xFF];
            keyBytes[3] = watermarkBytes[sboxBytes[83] & 0xFF];
            keyBytes[4] = watermarkBytes[sboxBytes[84] & 0xFF];
            keyBytes[5] = watermarkBytes[sboxBytes[85] & 0xFF];
            keyBytes[6] = watermarkBytes[sboxBytes[86] & 0xFF];
            keyBytes[7] = watermarkBytes[sboxBytes[87] & 0xFF];
        } catch (Exception ignored) {
        } finally {
            keyBytes[8] = watermarkBytes[sboxBytes[88] & 0xFF];
            keyBytes[9] = watermarkBytes[sboxBytes[89] & 0xFF];
            keyBytes[10] = watermarkBytes[sboxBytes[90] & 0xFF];
            keyBytes[11] = watermarkBytes[sboxBytes[91] & 0xFF];
            keyBytes[12] = watermarkBytes[sboxBytes[92] & 0xFF];
            keyBytes[13] = watermarkBytes[sboxBytes[93] & 0xFF];
            keyBytes[14] = watermarkBytes[sboxBytes[94] & 0xFF];
            keyBytes[15] = watermarkBytes[sboxBytes[95] & 0xFF];
        }

        int nk = 4;
        int rounds = nk + 6;
        int[] roundKeys = new int[(rounds + 1) * 4];

        try {
            int wordIndex = 0;
            int byteOffset = 0;

            try {
                while (byteOffset < 16) {
                    roundKeys[(wordIndex >> 2) * 4 + (wordIndex & 3)] = (keyBytes[byteOffset] & 0xFF)
                        | (keyBytes[byteOffset + 1] & 0xFF) << 8
                        | (keyBytes[byteOffset + 2] & 0xFF) << 16
                        | keyBytes[byteOffset + 3] << 24;
                    byteOffset += 4;
                    wordIndex++;
                }
            } catch (Exception ignored) {
            }

            int totalWords = (rounds + 1) << 2;
            for (int i = nk; i < totalWords; i++) {
                int temp = roundKeys[((i - 1) >> 2) * 4 + ((i - 1) & 3)];
                if (i % nk == 0) {
                    temp = subWord(sbox, shift(temp, 8)) ^ rcon[i / nk - 1];
                } else if (nk > 6 && i % nk == 4) {
                    temp = subWord(sbox, temp);
                }
                roundKeys[(i >> 2) * 4 + (i & 3)] = roundKeys[((i - nk) >> 2) * 4 + ((i - nk) & 3)] ^ temp;
            }
        } catch (Exception ignored) {
        }

        int[] iv = new int[]{12345678, 12345679, 12345680, 12345681};
        long integrityHash = calcIntegrityHash();
        iv[0] ^= (int) integrityHash;
        iv[1] ^= (int) (integrityHash >> 32);
        iv[2] ^= (int) integrityHash;
        iv[3] ^= (int) (integrityHash >> 32);
        data = new Object[]{sbox, te0, te1, te2, te3, roundKeys, iv};
        calcStackDepth(data);
    }

    static void calcStackDepth(Object[] state) {
    }

    static long calcIntegrityHash() {
        return 0L;
    }

    static byte[] longToBytes(long value) {
        return new byte[]{
            (byte) ((value >> 56) & 255L),
            (byte) ((value >> 48) & 255L),
            (byte) ((value >> 40) & 255L),
            (byte) ((value >> 32) & 255L),
            (byte) ((value >> 24) & 255L),
            (byte) ((value >> 16) & 255L),
            (byte) ((value >> 8) & 255L),
            (byte) (value & 255L)
        };
    }

    private static long calcJarIntegrityHash() throws ReflectiveOperationException {
        for (Method classMethod : Class.class.getDeclaredMethods()) {
            if (classMethod.hashCode() == 1559119063) {
                Object protectionDomain = classMethod.invoke(StringDecoderPrototype.class, (Object[]) null);

                for (Method pdMethod : protectionDomain.getClass().getDeclaredMethods()) {
                    if (pdMethod.hashCode() == -1201941151) {
                        Object codeSource = pdMethod.invoke(protectionDomain, (Object[]) null);

                        try {
                            for (Method csMethod : codeSource.getClass().getDeclaredMethods()) {
                                if (csMethod.hashCode() == -1736917247) {
                                    Object location = csMethod.invoke(codeSource, (Object[]) null);

                                    try {
                                        for (Method urlMethod : location.getClass().getDeclaredMethods()) {
                                            if (urlMethod.hashCode() == 1861435352) {
                                                ZipInputStream zipInputStream = null;

                                                try {
                                                    zipInputStream = new ZipInputStream(
                                                        (InputStream) urlMethod.invoke(location, (Object[]) null)
                                                    );
                                                    Method getNextEntryMethod = null;
                                                    Method closeEntryMethod = null;
                                                    Method getNameMethod = null;
                                                    Method getExtraMethod = null;

                                                    for (Method zisMethod : ZipInputStream.class.getDeclaredMethods()) {
                                                        switch (zisMethod.hashCode()) {
                                                            case -1736661825:
                                                                getNextEntryMethod = zisMethod;
                                                                break;
                                                            case 1566265196:
                                                                closeEntryMethod = zisMethod;
                                                                break;
                                                        }
                                                    }

                                                    Object entry = getNextEntryMethod.invoke(zipInputStream, (Object[]) null);
                                                    if (entry != null) {
                                                        for (Method zeMethod : ZipEntry.class.getDeclaredMethods()) {
                                                            switch (zeMethod.hashCode()) {
                                                                case -1186583068:
                                                                    getExtraMethod = zeMethod;
                                                                    break;
                                                                case 916583615:
                                                                    getNameMethod = zeMethod;
                                                                    break;
                                                            }
                                                        }
                                                    }

                                                    String charsetName = null;
                                                    for (String name : Charset.availableCharsets().keySet()) {
                                                        if (name.hashCode() == 81070450) {
                                                            charsetName = name;
                                                            break;
                                                        }
                                                    }

                                                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                                                    while (entry != null) {
                                                        String entryName = (String) getNameMethod.invoke(entry, (Object[]) null);
                                                        byte[] extra = (byte[]) getExtraMethod.invoke(entry, (Object[]) null);
                                                        if (extra != null
                                                            && extra[0] == 127
                                                            && extra[1] == -128
                                                            && entryName.lastIndexOf(46) > 0
                                                            && entryName.substring(entryName.lastIndexOf(46)).hashCode() == 1411683850) {
                                                            buffer.write(entryName.getBytes(charsetName));
                                                            buffer.write(extra);
                                                        }

                                                        closeEntryMethod.invoke(zipInputStream, (Object[]) null);
                                                        entry = getNextEntryMethod.invoke(zipInputStream, (Object[]) null);
                                                    }

                                                    buffer.close();
                                                    System.out.println(buffer.size());
                                                    return hash24(Long.MAX_VALUE, Long.MIN_VALUE, buffer.toByteArray());
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    if (zipInputStream != null) {
                                                        try {
                                                            zipInputStream.close();
                                                        } catch (Exception ignored) {
                                                        }
                                                    }
                                                    return 0L;
                                                }
                                            }
                                        }
                                    } catch (NullPointerException e) {
                                        e.printStackTrace();
                                    }

                                    return 0L;
                                }
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }

                        return 0L;
                    }
                }
                break;
            }
        }

        return 0L;
    }

    public static long hash24(long k0, long k1, byte[] input) {
        long v0 = 8317987319222330741L ^ k0;
        long v1 = 7237128888997146477L ^ k1;
        long v2 = 7816392313619706465L ^ k0;
        long v3 = 8387220255154660723L ^ k1;
        int fullBlocksEnd = input.length / 8 * 8;
        int pos = 0;

        while (pos < fullBlocksEnd) {
            long m = (input[pos++] & 255L)
                | (input[pos++] & 255L) << 8
                | (input[pos++] & 255L) << 16
                | (input[pos++] & 255L) << 24
                | (input[pos++] & 255L) << 32
                | (input[pos++] & 255L) << 40
                | (input[pos++] & 255L) << 48
                | (input[pos++] & 255L) << 56;
            v3 ^= m;
            v0 += v1;
            v2 += v3;
            v1 = v1 << 13 | v1 >>> 51;
            v3 = v3 << 16 | v3 >>> 48;
            v1 ^= v0;
            v3 ^= v2;
            v0 = v0 << 32 | v0 >>> 32;
            v2 += v1;
            v0 += v3;
            v1 = v1 << 17 | v1 >>> 47;
            v3 = v3 << 21 | v3 >>> 43;
            v1 ^= v2;
            v3 ^= v0;
            v2 = v2 << 32 | v2 >>> 32;
            v0 += v1;
            v2 += v3;
            v1 = v1 << 13 | v1 >>> 51;
            v3 = v3 << 16 | v3 >>> 48;
            v1 ^= v0;
            v3 ^= v2;
            v0 = v0 << 32 | v0 >>> 32;
            v2 += v1;
            v0 += v3;
            v1 = v1 << 17 | v1 >>> 47;
            v3 = v3 << 21 | v3 >>> 43;
            v1 ^= v2;
            v3 ^= v0;
            v2 = v2 << 32 | v2 >>> 32;
            v0 ^= m;
        }

        long lastBlock = 0L;
        for (int i = input.length - 1; i >= fullBlocksEnd; i--) {
            lastBlock <<= 8;
            lastBlock |= input[i] & 255L;
        }

        lastBlock |= (long) input.length << 56;
        v3 ^= lastBlock;
        v0 += v1;
        v2 += v3;
        v1 = v1 << 13 | v1 >>> 51;
        v3 = v3 << 16 | v3 >>> 48;
        v1 ^= v0;
        v3 ^= v2;
        v0 = v0 << 32 | v0 >>> 32;
        v2 += v1;
        v0 += v3;
        v1 = v1 << 17 | v1 >>> 47;
        v3 = v3 << 21 | v3 >>> 43;
        v1 ^= v2;
        v3 ^= v0;
        v2 = v2 << 32 | v2 >>> 32;
        v0 += v1;
        v2 += v3;
        v1 = v1 << 13 | v1 >>> 51;
        v3 = v3 << 16 | v3 >>> 48;
        v1 ^= v0;
        v3 ^= v2;
        v0 = v0 << 32 | v0 >>> 32;
        v2 += v1;
        v0 += v3;
        v1 = v1 << 17 | v1 >>> 47;
        v3 = v3 << 21 | v3 >>> 43;
        v1 ^= v2;
        v3 ^= v0;
        v2 = v2 << 32 | v2 >>> 32;
        v0 ^= lastBlock;
        v2 ^= 255L;
        v0 += v1;
        v2 += v3;
        v1 = v1 << 13 | v1 >>> 51;
        v3 = v3 << 16 | v3 >>> 48;
        v1 ^= v0;
        v3 ^= v2;
        v0 = v0 << 32 | v0 >>> 32;
        v2 += v1;
        v0 += v3;
        v1 = v1 << 17 | v1 >>> 47;
        v3 = v3 << 21 | v3 >>> 43;
        v1 ^= v2;
        v3 ^= v0;
        v2 = v2 << 32 | v2 >>> 32;
        v0 += v1;
        v2 += v3;
        v1 = v1 << 13 | v1 >>> 51;
        v3 = v3 << 16 | v3 >>> 48;
        v1 ^= v0;
        v3 ^= v2;
        v0 = v0 << 32 | v0 >>> 32;
        v2 += v1;
        v0 += v3;
        v1 = v1 << 17 | v1 >>> 47;
        v3 = v3 << 21 | v3 >>> 43;
        v1 ^= v2;
        v3 ^= v0;
        v2 = v2 << 32 | v2 >>> 32;
        v0 += v1;
        v2 += v3;
        v1 = v1 << 13 | v1 >>> 51;
        v3 = v3 << 16 | v3 >>> 48;
        v1 ^= v0;
        v3 ^= v2;
        v0 = v0 << 32 | v0 >>> 32;
        v2 += v1;
        v0 += v3;
        v1 = v1 << 17 | v1 >>> 47;
        v3 = v3 << 21 | v3 >>> 43;
        v1 ^= v2;
        v3 ^= v0;
        v2 = v2 << 32 | v2 >>> 32;
        v0 += v1;
        v2 += v3;
        v1 = v1 << 13 | v1 >>> 51;
        v3 = v3 << 16 | v3 >>> 48;
        v1 ^= v0;
        v3 ^= v2;
        v0 = v0 << 32 | v0 >>> 32;
        v2 += v1;
        v0 += v3;
        v1 = v1 << 17 | v1 >>> 47;
        v3 = v3 << 21 | v3 >>> 43;
        v1 ^= v2;
        v3 ^= v0;
        v2 = v2 << 32 | v2 >>> 32;
        long result = v0 ^ v1 ^ v2 ^ v3;
        System.out.println(result);
        System.out.println(k0);
        System.out.println(k1);
        System.out.println(input.length);
        return result;
    }

    static void println() {
        System.out.println(0);
    }
}
