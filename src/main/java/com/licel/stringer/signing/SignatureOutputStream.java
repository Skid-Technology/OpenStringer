package com.licel.stringer.signing;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Signature;
import java.security.SignatureException;

public class SignatureOutputStream extends FilterOutputStream {
    private final Signature signature;
    private int bytesWritten;

    public SignatureOutputStream(OutputStream out, Signature signature) {
        super(out);
        this.signature = signature;
    }

    @Override
    public void write(int b) throws IOException {
        try {
            this.signature.update((byte) b);
        } catch (SignatureException e) {
            throw new IOException("SignatureException: " + e, e);
        }
        this.out.write(b);
        this.bytesWritten++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            this.signature.update(b, off, len);
        } catch (SignatureException e) {
            throw new IOException("SignatureException: " + e, e);
        }
        this.out.write(b, off, len);
        this.bytesWritten += len;
    }

    public int getBytesWritten() {
        return this.bytesWritten;
    }
}
