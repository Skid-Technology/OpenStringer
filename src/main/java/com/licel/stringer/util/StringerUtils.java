package com.licel.stringer.util;

import com.licel.stringer.Config;
import com.licel.stringer.ProductInfo;
import com.licel.stringer.Worker;
import com.licel.stringer.io.MatchingFileCollector;
import com.licel.stringer.io.ResourcesOutput;
import com.licel.stringer.model.SigningIdentity;
import com.licel.stringer.signing.KeystoreGenerator;
import com.licel.stringer.signing.SignedJarBuilder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class StringerUtils {
    private static final Map<String, String> PRIMITIVE_DESCRIPTORS = new HashMap<>();
    private static final byte[] COPY_BUFFER = new byte[1048576];
    private static final char[] BASE36_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z'
    };
    private static final char[] ALPHA_DIGITS = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D',
            'E', 'F', 'G', 'H', 'I', 'J'
    };

    static {
        PRIMITIVE_DESCRIPTORS.put("boolean", "Z");
        PRIMITIVE_DESCRIPTORS.put("byte", "B");
        PRIMITIVE_DESCRIPTORS.put("char", "C");
        PRIMITIVE_DESCRIPTORS.put("short", "S");
        PRIMITIVE_DESCRIPTORS.put("int", "I");
        PRIMITIVE_DESCRIPTORS.put("long", "J");
        PRIMITIVE_DESCRIPTORS.put("float", "F");
        PRIMITIVE_DESCRIPTORS.put("double", "D");
    }

    private StringerUtils() {
    }

    public static byte[] longToBytes(long value) {
        return new byte[]{
                (byte) (value >> 56 & 0xFFL),
                (byte) (value >> 48 & 0xFFL),
                (byte) (value >> 40 & 0xFFL),
                (byte) (value >> 32 & 0xFFL),
                (byte) (value >> 24 & 0xFFL),
                (byte) (value >> 16 & 0xFFL),
                (byte) (value >> 8 & 0xFFL),
                (byte) (value & 0xFFL)
        };
    }

    public static long bytesToLong(byte[] bytes) {
        return (bytes[0] & 0xFFL) << 56
                | (bytes[1] & 0xFFL) << 48
                | (bytes[2] & 0xFFL) << 40
                | (bytes[3] & 0xFFL) << 32
                | (bytes[4] & 0xFFL) << 24
                | (bytes[5] & 0xFFL) << 16
                | (bytes[6] & 0xFFL) << 8
                | (bytes[7] & 0xFFL);
    }

    public static Properties loadProguardMap(File mapFile) throws IOException {
        Properties mappings = new Properties();
        try (BufferedReader reader = new BufferedReader(new FileReader(mapFile))) {
            String line = reader.readLine();
            String currentClass = null;

            while (line != null) {
                line = line.trim();
                if (!line.startsWith("#")) {
                    if (line.endsWith(":")) {
                        String[] classMapping = parseClassMappingLine(line);
                        if (classMapping != null) {
                            mappings.setProperty(classMapping[0], classMapping[1]);
                            mappings.setProperty(classMapping[1], classMapping[0]);
                            currentClass = classMapping[0];
                        }
                    } else if (currentClass != null) {
                        String[] methodMapping = parseMethodMappingLine(currentClass, line, null);
                        if (methodMapping != null) {
                            mappings.setProperty(currentClass + ':' + methodMapping[0] + methodMapping[2], methodMapping[1]);
                        }
                    }
                }
                line = reader.readLine();
            }
        }
        return mappings;
    }

    private static String[] parseClassMappingLine(String line) {
        int arrowIndex = line.indexOf("->");
        if (arrowIndex < 0) {
            return null;
        }
        int colonIndex = line.indexOf(':', arrowIndex + 2);
        if (colonIndex < 0) {
            return null;
        }
        String originalName = line.substring(0, arrowIndex).trim().replace('.', '/');
        String obfuscatedName = line.substring(arrowIndex + 2, colonIndex).trim().replace('.', '/');
        return new String[]{obfuscatedName, originalName};
    }

    private static String[] parseMethodMappingLine(String className, String line, Properties mappings) {
        int colon1 = line.indexOf(':');
        int colon2 = colon1 < 0 ? -1 : line.indexOf(':', colon1 + 1);
        int spaceIndex = line.indexOf(' ', colon2 + 2);
        int openParen = line.indexOf('(', spaceIndex + 1);
        int closeParen = openParen < 0 ? -1 : line.indexOf(')', openParen + 1);
        int colon3 = closeParen < 0 ? -1 : line.indexOf(':', closeParen + 1);
        int colon4 = colon3 < 0 ? -1 : line.indexOf(':', colon3 + 1);
        int arrowSearchStart = (colon4 >= 0 ? colon4 : (colon3 >= 0 ? colon3 : (closeParen >= 0 ? closeParen : spaceIndex))) + 1;
        int arrowIndex = line.indexOf("->", arrowSearchStart);

        if (spaceIndex >= 0 && arrowIndex >= 0) {
            String originalMethod = line.substring(spaceIndex + 1, openParen >= 0 ? openParen : arrowIndex).trim();
            String obfuscatedMethod = line.substring(arrowIndex + 2).trim();
            int dotIndex = originalMethod.lastIndexOf('.');
            if (dotIndex >= 0) {
                originalMethod = originalMethod.substring(dotIndex + 1);
            }

            if (closeParen > 0) {
                if (colon2 >= 0) {
                    Integer.parseInt(line.substring(0, colon1).trim());
                    Integer.parseInt(line.substring(colon1 + 1, colon2).trim());
                }
                if (colon3 >= 0) {
                    Integer.parseInt(line.substring(colon3 + 1, colon4 > 0 ? colon4 : arrowIndex).trim());
                    if (colon4 >= 0) {
                        Integer.parseInt(line.substring(colon4 + 1, arrowIndex).trim());
                    }
                }
                String rawParams = line.substring(openParen + 1, closeParen).trim();
                return new String[]{obfuscatedMethod, originalMethod, toJvmArgumentDescriptor(rawParams, mappings)};
            }
        }
        return null;
    }

    public static String toJvmArgumentDescriptor(String parameterList, Properties proguardMap) {
        if (parameterList.trim().isEmpty()) {
            return "()";
        }
        StringBuilder descriptor = new StringBuilder();
        descriptor.append('(');
        for (String rawType : parameterList.split(",")) {
            String typeName = rawType.trim();
            String arrayElement = typeName.endsWith("[]") ? typeName.substring(0, typeName.length() - 2) : null;
            if (PRIMITIVE_DESCRIPTORS.containsKey(typeName)) {
                descriptor.append(PRIMITIVE_DESCRIPTORS.get(typeName));
            } else {
                if (arrayElement != null) {
                    descriptor.append('[');
                    if (PRIMITIVE_DESCRIPTORS.containsKey(arrayElement)) {
                        descriptor.append(PRIMITIVE_DESCRIPTORS.get(arrayElement));
                        continue;
                    }
                }
                descriptor.append('L');
                String className = arrayElement != null ? arrayElement : typeName;
                if (proguardMap != null) {
                    descriptor.append(proguardMap.getProperty(className, className).replace(".", "/"));
                } else {
                    descriptor.append(className.replace(".", "/"));
                }
                descriptor.append(';');
            }
        }
        descriptor.append(')');
        return descriptor.toString();
    }

    public static String getBoxedClassName(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                return Boolean.class.getName();
            case Type.CHAR:
                return Character.class.getName();
            case Type.BYTE:
                return Byte.class.getName();
            case Type.SHORT:
                return Short.class.getName();
            case Type.INT:
                return Integer.class.getName();
            case Type.FLOAT:
                return Float.class.getName();
            case Type.LONG:
                return Long.class.getName();
            case Type.DOUBLE:
                return Double.class.getName();
            default:
                return type.getClassName();
        }
    }

    public static int[] generateRandomPermutation(SecureRandom random, int length) {
        int[] indices = new int[length];
        for (int i = 0; i < length; i++) {
            indices[i] = i;
        }
        for (int i = length - 1; i > 0; i--) {
            int j = random.nextInt(i);
            int tmp = indices[j];
            indices[j] = indices[i];
            indices[i] = tmp;
        }
        return indices;
    }

    public static void copyStream(InputStream input, ResourcesOutput output) throws IOException {
        int read;
        while ((read = input.read(COPY_BUFFER)) > 0) {
            output.write(COPY_BUFFER, 0, read);
        }
    }

    public static ArrayList<Path> findMatchingFiles(File rootDir, String globPattern) throws IOException {
        Path root = Paths.get(rootDir.toURI());
        ArrayList<Path> matches = new ArrayList<>();
        String separator = File.separatorChar == '/' ? File.separator : "\\\\";
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern.replace("/", separator));
        Files.walkFileTree(root, new MatchingFileCollector(root, matcher, matches));
        return matches;
    }

    public static SigningIdentity loadSigningIdentity(Config config) throws IOException, GeneralSecurityException {
        if (config.isReleaseMode()) {
            return loadReleaseSigningIdentity(config);
        }
        return config.isDefaultMode() ? loadDefaultSigningIdentity(config) : null;
    }

    public static SigningIdentity loadReleaseSigningIdentity(Config config) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance(config.getKeystoreType() != null ? config.getKeystoreType() : KeyStore.getDefaultType());
        try (FileInputStream in = new FileInputStream(config.getKeystore())) {
            keyStore.load(in, config.getStorepass().toCharArray());
        }
        PasswordProtection keyProtection = new PasswordProtection(config.getKeypass().toCharArray());
        PrivateKeyEntry entry = (PrivateKeyEntry) keyStore.getEntry(config.getAlias(), keyProtection);
        return new SigningIdentity((X509Certificate) entry.getCertificate(), entry.getPrivateKey());
    }

    public static SigningIdentity loadDefaultSigningIdentity(Config config) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        File keystoreFile = new File(System.getProperty("user.home") + File.separator + "stringer.default.keystore");
        if (!keystoreFile.exists()) {
            if (config.isVerbose()) {
                config.getListener().verbose("Default keystore has not been found at path: " + keystoreFile);
            }
            KeystoreGenerator generator = new KeystoreGenerator();
            try {
                generator.generateKeystore(
                        Worker.get().secureRandom,
                        ProductInfo.getVendor(),
                        keystoreFile.getAbsolutePath());
            } catch (Throwable ex) {
                throw new RuntimeException("Error saving default keystore.", ex);
            }
            if (config.isVerbose()) {
                config.getListener().verbose("Default keystore has been created and saved at path: " + keystoreFile);
            }
        }

        try (FileInputStream in = new FileInputStream(keystoreFile)) {
            keyStore.load(in, "stringer".toCharArray());
        }
        PasswordProtection keyProtection = new PasswordProtection("stringer".toCharArray());
        PrivateKeyEntry entry = (PrivateKeyEntry) keyStore.getEntry("default", keyProtection);
        return new SigningIdentity((X509Certificate) entry.getCertificate(), entry.getPrivateKey());
    }

    public static void signJar(SigningIdentity identity, File inputJar, File outputJar, Manifest extraManifest)
            throws IOException, GeneralSecurityException {
        SignedJarBuilder builder = new SignedJarBuilder(new FileOutputStream(outputJar, false), identity.privateKey, identity.certificate);
        Manifest builderManifest = builder.getManifest();
        if (extraManifest != null && builderManifest != null) {
            builderManifest.getMainAttributes().putAll(extraManifest.getMainAttributes());
        }
        builder.writeZip(inputJar, null);
        builder.close();
    }

    public static void pushIntConstant(MethodVisitor mv, int value) {
        switch (value) {
            case 0:
                mv.visitInsn(Opcodes.ICONST_0);
                break;
            case 1:
                mv.visitInsn(Opcodes.ICONST_1);
                break;
            case 2:
                mv.visitInsn(Opcodes.ICONST_2);
                break;
            case 3:
                mv.visitInsn(Opcodes.ICONST_3);
                break;
            case 4:
                mv.visitInsn(Opcodes.ICONST_4);
                break;
            case 5:
                mv.visitInsn(Opcodes.ICONST_5);
                break;
            default:
                if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                    mv.visitIntInsn(Opcodes.BIPUSH, value);
                } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                    mv.visitIntInsn(Opcodes.SIPUSH, value);
                } else {
                    mv.visitLdcInsn(value);
                }
        }
    }

    public static String toUnsignedString(int value, int shift) {
        char[] buf = new char[32];
        int charPos = 32;
        int radix = 1 << shift;
        int mask = radix - 1;
        do {
            buf[--charPos] = BASE36_DIGITS[value & mask];
            value >>>= shift;
        } while (value != 0);
        return new String(buf, charPos, 32 - charPos);
    }

    public static String toAlphabeticUnsignedString(long value, int shift) {
        char[] buf = new char[64];
        int charPos = 64;
        int radix = 1 << shift;
        long mask = radix - 1;
        do {
            buf[--charPos] = ALPHA_DIGITS[(int) (value & mask)];
            value >>>= shift;
        } while (value != 0L);
        return new String(buf, charPos, 64 - charPos);
    }
}
