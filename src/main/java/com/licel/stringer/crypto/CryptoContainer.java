package com.licel.stringer.crypto;

import com.licel.stringer.Watermark;
import com.licel.stringer.util.StringerUtils;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class CryptoContainer {
    private long primaryKeyHigh;
    private long currentKeyHigh;
    private long keyLow;
    private int[] ivWords = new int[4];
    private int[][] roundKeys;
    private int callerHash = 0;
    private int resourceMagicHeader;
    private byte[] keyTable;
    private byte[] keyIndexTable;
    private List<Integer> keyIndices;
    private int[] idObfuscationKeys = new int[5];
    private int[] idObfuscationSigns = new int[3];
    private long integrityHash = 0L;
    public SecureRandom secureRandom;

    public CryptoContainer(SecureRandom secureRandom, Watermark watermark, boolean generateRandomKey) {
        this.secureRandom = secureRandom;
        if (!generateRandomKey) {
            this.initWatermarkKey(watermark);
            for (int i = 0; i < this.ivWords.length; i++) {
                this.ivWords[i] = secureRandom.nextInt();
            }
            this.roundKeys = AESFast.expandKey(this.getKeyBytes());
            for (int i = 0; i < this.idObfuscationKeys.length; i++) {
                this.idObfuscationKeys[i] = secureRandom.nextInt();
            }
            for (int i = 0; i < this.idObfuscationSigns.length; i++) {
                this.idObfuscationSigns[i] = secureRandom.nextInt() & 1;
            }
        } else {
            this.primaryKeyHigh = (long) (secureRandom.nextInt(Integer.MAX_VALUE) << 32) + secureRandom.nextInt(Integer.MAX_VALUE);
            this.currentKeyHigh = this.primaryKeyHigh;
            this.keyLow = (long) (secureRandom.nextInt(Integer.MAX_VALUE) << 32) + secureRandom.nextInt(Integer.MAX_VALUE);
            for (int i = 0; i < this.ivWords.length; i++) {
                this.ivWords[i] = secureRandom.nextInt(Integer.MAX_VALUE);
            }
            this.resourceMagicHeader = secureRandom.nextInt(Integer.MAX_VALUE);
            this.roundKeys = AESFast.expandKey(this.getKeyBytes());
        }

        for (int i = 0; i < this.idObfuscationKeys.length; i++) {
            this.idObfuscationKeys[i] = secureRandom.nextInt();
        }
        for (int i = 0; i < this.idObfuscationSigns.length; i++) {
            this.idObfuscationSigns[i] = secureRandom.nextInt() & 1;
        }
    }

    private void initWatermarkKey(Watermark watermark) {
        this.keyTable = new byte[256];
        this.secureRandom.nextBytes(this.keyTable);
        byte[] watermarkBytes = watermark.generateKeyBlock(this.secureRandom);
        if (watermarkBytes.length != 96) {
            throw new IllegalArgumentException("");
        }

        System.arraycopy(watermarkBytes, 0, this.keyTable, 0, watermarkBytes.length);
        this.keyIndexTable = new byte[256];
        this.secureRandom.nextBytes(this.keyIndexTable);

        HashSet<Byte> selectedOffsets = new HashSet<>();
        while (selectedOffsets.size() < 16) {
            int offset = this.secureRandom.nextInt() & 0xFF;
            if (offset >= 8) {
                if (offset < 96) {
                    if (selectedOffsets.contains((byte) offset)) {
                        continue;
                    }
                    Byte pairedOffset = (byte) (offset < 56 ? offset + 44 : offset - 44);
                    if (selectedOffsets.contains(pairedOffset)) {
                        continue;
                    }
                }
                selectedOffsets.add((byte) offset);
            }
        }

        Byte[] offsets = selectedOffsets.toArray(new Byte[16]);
        HashSet<Integer> lookupSlots = new HashSet<>();
        while (lookupSlots.size() < 16) {
            lookupSlots.add(this.secureRandom.nextInt() & 0xFF);
        }

        this.keyIndices = Arrays.asList(lookupSlots.toArray(new Integer[16]));
        Collections.shuffle(this.keyIndices, this.secureRandom);

        for (int i = 0; i < 16; i++) {
            this.keyIndexTable[this.keyIndices.get(i)] = offsets[i];
        }

        byte[] highBytes = new byte[8];
        byte[] lowBytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            highBytes[i] = this.keyTable[this.keyIndexTable[this.keyIndices.get(i)] & 0xFF];
            lowBytes[i] = this.keyTable[this.keyIndexTable[this.keyIndices.get(i + 8)] & 0xFF];
        }

        this.primaryKeyHigh = StringerUtils.bytesToLong(highBytes);
        this.currentKeyHigh = this.primaryKeyHigh;
        this.keyLow = StringerUtils.bytesToLong(lowBytes);
        Arrays.fill(highBytes, (byte) 0);
        Arrays.fill(lowBytes, (byte) 0);
    }

    public long getCurrentKeyHigh() {
        return this.currentKeyHigh;
    }

    public long getPrimaryKeyHigh() {
        return this.primaryKeyHigh;
    }

    public long getKeyLow() {
        return this.keyLow;
    }

    public final byte[] getKeyBytes() {
        return this.combineKeyBytes(this.currentKeyHigh, this.keyLow);
    }

    public int[] getIvWords() {
        return this.ivWords;
    }

    public int[] getIntegrityIvWords() {
        return this.integrityHash != 0L
                ? new int[]{
                        this.ivWords[0] ^ (int) this.integrityHash,
                        this.ivWords[1] ^ (int) (this.integrityHash >> 32),
                        this.ivWords[2] ^ (int) this.integrityHash,
                        this.ivWords[3] ^ (int) (this.integrityHash >> 32)
                }
                : this.ivWords;
    }

    public final void setIntegrityHash(long integrityHash) {
        this.integrityHash = integrityHash;
    }

    public int[] getCallerIvWords() {
        if (this.callerHash == 0) {
            return this.ivWords;
        }
        int[] result = new int[4];
        for (int i = 0; i < 4; i++) {
            result[i] = this.callerHash ^ this.ivWords[i];
        }
        return result;
    }

    private byte[] combineKeyBytes(long high, long low) {
        byte[] result = new byte[16];
        System.arraycopy(StringerUtils.longToBytes(high), 0, result, 0, 8);
        System.arraycopy(StringerUtils.longToBytes(low), 0, result, 8, 8);
        return result;
    }

    public int[][] getRoundKeys() {
        return this.roundKeys;
    }

    public int getResourceMagicHeader() {
        return this.resourceMagicHeader;
    }

    public int[] createResourceIvWords() {
        int[] words = new int[4];
        words[0] = this.resourceMagicHeader;
        for (int i = 1; i < words.length; i++) {
            words[i] = this.secureRandom.nextInt();
        }
        return words;
    }

    public void setCallerHash(int callerHash) {
        this.callerHash = callerHash;
    }

    public int encodeMemberId(int memberIndex, int mask) {
        int value = mask << 16 | memberIndex;
        value += this.idObfuscationSigns[0] == 0 ? this.idObfuscationKeys[0] : -this.idObfuscationKeys[0];
        value ^= this.idObfuscationKeys[1];
        value += this.idObfuscationSigns[1] == 0 ? this.idObfuscationKeys[2] : -this.idObfuscationKeys[2];
        value ^= this.idObfuscationKeys[3];
        return value + (this.idObfuscationSigns[2] == 0 ? this.idObfuscationKeys[4] : -this.idObfuscationKeys[4]);
    }

    public int[] getIdObfuscationKeys() {
        return this.idObfuscationKeys;
    }

    public byte[] getKeyTable() {
        return this.keyTable;
    }

    public byte[] getKeyIndexTable() {
        return this.keyIndexTable;
    }

    public List<Integer> getKeyIndices() {
        return this.keyIndices;
    }

    public int[] getIdObfuscationSigns() {
        return this.idObfuscationSigns;
    }

    public int nextInt(int bound) {
        return this.secureRandom.nextInt(bound);
    }

    public SecureRandom getSecureRandom() {
        return this.secureRandom;
    }
}
