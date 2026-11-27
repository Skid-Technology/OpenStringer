package com.licel.stringer.prototype;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StringerFilterInputStreamPrototype extends FilterInputStream {
    StringerInputStreamPrototype parent;
    private byte[] singleByteBuf = new byte[1];

    public StringerFilterInputStreamPrototype(InputStream in, StringerInputStreamPrototype parent) {
        super(in);
        this.parent = parent;
    }

    @Override
    public synchronized int read() throws IOException {
        return this.read(this.singleByteBuf, 0, 1) == -1 ? -1 : this.singleByteBuf[0] & 0xFF;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = super.read(b, off, len);
        if (bytesRead > 0) {
            this.parent.decrypt(b, off, bytesRead);
        }

        return bytesRead;
    }
}
