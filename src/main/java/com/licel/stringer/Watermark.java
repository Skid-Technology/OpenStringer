package com.licel.stringer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.Adler32;
import org.bouncycastle.util.encoders.Base64;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.commons.FileUtilsV2_2;

public class Watermark {
    private byte[] identitySha1;
    private byte[] identityMd5;
    private int versionNumber;
    private String version;
    private String buildId;
    private byte[] combinedSha1;
    private byte[] combinedMd5;
    private Config config;

    public Watermark() {
    }

    public Watermark(Config config, File sourceFile) throws IOException, NoSuchAlgorithmException {
        this.config = config;
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[1024];

        byte[] identitySeed = ProductInfo.getVendor().getBytes(StandardCharsets.UTF_8);
        sha1.update(identitySeed);
        md5.update(identitySeed);

        this.identitySha1 = sha1.digest();
        this.identityMd5 = md5.digest();

        File tempZip = null;
        if (sourceFile.isDirectory() && sourceFile.list().length == 0) {
            SecureRandom random = Worker.get().secureRandom;
            random.nextBytes(buffer);
            sha1.update(buffer, 0, buffer.length);
            md5.update(buffer, 0, buffer.length);
        } else {
            File targetToHash;
            if (sourceFile.isDirectory()) {
                tempZip = File.createTempFile(sourceFile.getName(), "zip");
                ZipUtil.pack(sourceFile, tempZip, 0);
                targetToHash = tempZip;
            } else {
                targetToHash = sourceFile;
            }

            try (RandomAccessFile raf = new RandomAccessFile(targetToHash, "r")) {
                int read;
                while ((read = raf.read(buffer)) > 0) {
                    sha1.update(buffer, 0, read);
                    md5.update(buffer, 0, read);
                }
            } finally {
                if (tempZip != null) {
                    FileUtilsV2_2.deleteQuietly(tempZip);
                }
            }
        }

        this.combinedSha1 = sha1.digest();
        this.combinedMd5 = md5.digest();
        this.version = ProductInfo.getVersion();
        this.versionNumber = Integer.parseInt(this.version.replace(".", ""));
        this.buildId = ProductInfo.getBuildId();
    }

    public byte[] generateHeader(SecureRandom random) {
        byte[] header = new byte[]{0, 0, 68, 69, 88, 80, 0, 0};
        random.nextInt();

        int salt;
        do {
            salt = random.nextInt() >>> 16;
        } while (Integer.bitCount(salt) % 2 != 1);

        byte[] saltBytes = shortToBytes((short) salt, true);
        System.arraycopy(saltBytes, 0, header, 0, saltBytes.length);
        byte[] versionBytes = shortToBytes((short) this.versionNumber, true);
        System.arraycopy(versionBytes, 0, header, header.length - versionBytes.length, versionBytes.length);
        this.xorWithKey(header, 2, saltBytes);
        return header;
    }

    public Manifest createManifest(SecureRandom random) {
        Manifest manifest = new Manifest();
        if (!this.config.getPersonalizationProfile().getProtectedByTemplate().trim().isEmpty()) {
            manifest.getMainAttributes().putValue("Protected-By", this.config.getPersonalizationProfile().evaluateProtectedBy());
        }
        if (!this.config.getPersonalizationProfile().getNoticeTemplate().trim().isEmpty()) {
            manifest.getMainAttributes().putValue("Protected-Notice", this.config.getPersonalizationProfile().evaluateNotice());
        }

        random.nextInt();
        int salt;
        do {
            salt = random.nextInt();
        } while (Integer.bitCount(salt) % 2 != 1);

        byte[] saltBytes = intToBytes(salt, true);
        byte[] signatureData = new byte[saltBytes.length + this.identityMd5.length + this.combinedMd5.length];
        System.arraycopy(saltBytes, 0, signatureData, 0, saltBytes.length);
        System.arraycopy(this.identityMd5, 0, signatureData, saltBytes.length, this.identityMd5.length);
        System.arraycopy(this.combinedMd5, 0, signatureData, saltBytes.length + this.identityMd5.length, this.combinedMd5.length);
        this.xorWithKey(signatureData, 4, saltBytes);

        Attributes mainAttributes = manifest.getMainAttributes();
        mainAttributes.putValue("JAR-Signature", new String(Base64.encode(signatureData), StandardCharsets.US_ASCII));
        return manifest;
    }

    public byte[] generateExtraField(SecureRandom random) {
        byte[] header = this.generateHeader(random);
        byte[] payload = new byte[this.identitySha1.length + this.combinedSha1.length + 4];
        byte[] mask = new byte[4];
        random.nextBytes(mask);
        byte[] result = new byte[header.length + mask.length + payload.length];

        System.arraycopy(this.identitySha1, 0, payload, 0, this.identitySha1.length);
        System.arraycopy(this.combinedSha1, 0, payload, this.identitySha1.length, this.combinedSha1.length);
        Adler32 checksum = new Adler32();
        checksum.update(payload, 0, this.identitySha1.length + this.combinedSha1.length);
        byte[] checksumBytes = intToBytes((int) checksum.getValue(), true);
        System.arraycopy(checksumBytes, 0, payload, this.identitySha1.length + this.combinedSha1.length, checksumBytes.length);

        this.xorWithKey(payload, 0, mask);
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(mask, 0, result, header.length, mask.length);
        System.arraycopy(payload, 0, result, header.length + mask.length, payload.length);
        return result;
    }

    public byte[] generateKeyBlock(SecureRandom random) {
        byte[] header = this.generateHeader(random);
        byte[] payload = new byte[this.identitySha1.length + this.combinedSha1.length + 4];
        byte[] mask = new byte[payload.length];
        random.nextBytes(mask);
        byte[] result = new byte[header.length + payload.length + mask.length];

        System.arraycopy(this.identitySha1, 0, payload, 0, this.identitySha1.length);
        System.arraycopy(this.combinedSha1, 0, payload, this.identitySha1.length, this.combinedSha1.length);
        Adler32 checksum = new Adler32();
        checksum.update(payload, 0, this.identitySha1.length + this.combinedSha1.length);
        byte[] checksumBytes = intToBytes((int) checksum.getValue(), true);
        System.arraycopy(checksumBytes, 0, payload, this.identitySha1.length + this.combinedSha1.length, checksumBytes.length);

        this.xorWithKey(payload, 0, mask);
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(payload, 0, result, header.length, payload.length);
        System.arraycopy(mask, 0, result, header.length + payload.length, mask.length);
        return result;
    }

    private void xorWithKey(byte[] data, int startOffset, byte[] key) {
        for (int i = 0; i < data.length - startOffset; i++) {
            data[i + startOffset] = (byte) (data[i + startOffset] ^ (key[i % key.length] & 0xFF));
        }
    }

    public static byte[] intToBytes(int value, boolean littleEndian) {
        byte[] bytes = new byte[4];
        if (littleEndian) {
            bytes[0] = (byte) (value & 0xFF);
            bytes[1] = (byte) (value >> 8 & 0xFF);
            bytes[2] = (byte) (value >> 16 & 0xFF);
            bytes[3] = (byte) (value >> 24 & 0xFF);
        } else {
            bytes[3] = (byte) (value & 0xFF);
            bytes[2] = (byte) (value >> 8 & 0xFF);
            bytes[1] = (byte) (value >> 16 & 0xFF);
            bytes[0] = (byte) (value >> 24 & 0xFF);
        }
        return bytes;
    }

    public static byte[] shortToBytes(short value, boolean littleEndian) {
        byte[] bytes = new byte[2];
        if (littleEndian) {
            bytes[0] = (byte) (value & 0xFF);
            bytes[1] = (byte) (value >> 8 & 0xFF);
        } else {
            bytes[1] = (byte) (value & 0xFF);
            bytes[0] = (byte) (value >> 8 & 0xFF);
        }
        return bytes;
    }
}
