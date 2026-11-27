package com.licel.stringer.crypto;

public class SipHash {
    public static long hash(long k0, long k1, byte[] data) {
        long v0 = 0x736F6D6570736575L ^ k0;
        long v1 = 0x646F72616E646F6DL ^ k1;
        long v2 = 0x6C7967656E657261L ^ k0;
        long v3 = 0x7465646279746573L ^ k1;
        int fullBlocksLength = data.length / 8 * 8;
        int offset = 0;

        while (offset < fullBlocksLength) {
            long word = (data[offset++] & 0xFFL)
                    | (data[offset++] & 0xFFL) << 8
                    | (data[offset++] & 0xFFL) << 16
                    | (data[offset++] & 0xFFL) << 24
                    | (data[offset++] & 0xFFL) << 32
                    | (data[offset++] & 0xFFL) << 40
                    | (data[offset++] & 0xFFL) << 48
                    | (data[offset++] & 0xFFL) << 56;
            v3 ^= word;
            for (int i = 0; i < 2; i++) {
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
            }
            v0 ^= word;
        }

        long lastBlock = 0L;
        for (int i = data.length - 1; i >= fullBlocksLength; i--) {
            lastBlock <<= 8;
            lastBlock |= data[i] & 0xFFL;
        }
        lastBlock |= (long) data.length << 56;

        v3 ^= lastBlock;
        for (int i = 0; i < 2; i++) {
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
        }
        v0 ^= lastBlock;
        v2 ^= 0xFFL;

        for (int i = 0; i < 4; i++) {
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
        }

        return v0 ^ v1 ^ v2 ^ v3;
    }
}
