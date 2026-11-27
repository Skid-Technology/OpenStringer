package com.licel.stringer.prototype;

import java.util.zip.Inflater;

public class StringerInflaterPrototype extends Inflater {
    StringerInputStreamPrototype parent;

    public StringerInflaterPrototype(StringerInputStreamPrototype parent) {
        super(false);
        this.parent = parent;
    }

    @Override
    public void setInput(byte[] b, int off, int len) {
        this.parent.decrypt(b, off, len);
        super.setInput(b, off, len);
    }
}
