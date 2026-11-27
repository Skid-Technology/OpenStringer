package com.licel.stringer.io;

public class ResourceEntry {
    private final String name;
    private final boolean directory;
    private final long size;
    private byte[] extra;

    public ResourceEntry(String name) {
        this(name, false, -1L);
    }

    public ResourceEntry(String name, boolean directory) {
        this(name, directory, -1L);
    }

    public ResourceEntry(String name, boolean directory, long size) {
        this.name = name;
        this.directory = directory;
        this.size = directory ? 0L : size;
    }

    public String getName() {
        return this.name;
    }

    public String getNormalizedName() {
        return this.name.replace('\\', '/') + (this.directory && !this.name.endsWith("/") ? "/" : "");
    }

    public boolean isDirectory() {
        return this.directory;
    }

    public long getSize() {
        return this.size;
    }

    public void setExtra(byte[] extra) {
        this.extra = extra;
    }

    public byte[] getExtra() {
        return this.extra;
    }
}
