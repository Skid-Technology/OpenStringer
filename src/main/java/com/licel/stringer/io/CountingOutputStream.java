package com.licel.stringer.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CountingOutputStream extends FilterOutputStream {
    private long bytesWritten = 0L;

    public CountingOutputStream(OutputStream out) {
        super(out);
    }

    public long getBytesWritten() {
        return this.bytesWritten;
    }

    @Override
    public void write(byte[] data) throws IOException {
        this.out.write(data);
        this.bytesWritten += data.length;
    }

    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        this.out.write(data, offset, length);
        this.bytesWritten += length;
    }

    @Override
    public void write(int b) throws IOException {
        this.out.write(b);
        this.bytesWritten++;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    public void writeUInt32(long value) throws IOException {
        this.write((int) ((value >>> 0) & 0xFFL));
        this.write((int) ((value >>> 8) & 0xFFL));
        this.write((int) ((value >>> 16) & 0xFFL));
        this.write((int) ((value >>> 24) & 0xFFL));
    }

    public void writeUInt16(int value) throws IOException {
        this.write((value >>> 0) & 0xFF);
        this.write((value >>> 8) & 0xFF);
    }
}
