package com.licel.stringer.io;

import java.io.IOException;

public abstract class ResourcesOutput {
    public abstract void open() throws IOException;

    public abstract void openEntry(ResourceEntry entry) throws IOException;

    public abstract void close() throws IOException;

    public abstract void write(byte[] data) throws IOException;

    public abstract void write(byte[] data, int offset, int length) throws IOException;
}
