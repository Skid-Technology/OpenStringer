package com.licel.stringer.io;

import java.util.zip.ZipEntry;

public class ZipEntryRecord {
    public final ZipEntry entry;
    public final long localHeaderOffset;
    public final int flags;
    public final int padding;

    public ZipEntryRecord(ZipEntry entry, long localHeaderOffset, int flags, int padding) {
        this.entry = entry;
        this.localHeaderOffset = localHeaderOffset;
        this.flags = flags;
        this.padding = padding;
    }
}
