package com.licel.stringer.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipAlignmentVerifier {
    private static final int UTF8_ENCODING_FLAG = 2048;

    private final File archiveFile;
    private final int alignment;
    private ZipFile zipFile;
    private RandomAccessFile input;
    private boolean failed = false;

    public ZipAlignmentVerifier(File archiveFile) {
        this(archiveFile, ZipAlignmentConstants.DEFAULT_ALIGNMENT);
    }

    public ZipAlignmentVerifier(File archiveFile, int alignment) {
        this.archiveFile = archiveFile;
        this.alignment = alignment;
    }

    public boolean isFailed() {
        return this.failed;
    }

    public void verify() throws IOException {
        try {
            this.openFiles();
            this.verifyEntries();
        } catch (Exception e) {
            this.failed = true;
        } finally {
            try {
                this.closeFiles();
            } catch (Exception e) {
                this.failed = true;
            }
        }
    }

    private void openFiles() throws IOException {
        this.zipFile = new ZipFile(this.archiveFile);
        this.input = new RandomAccessFile(this.archiveFile, "r");
    }

    private void verifyEntries() throws IOException {
        int entryCount = this.zipFile.size();
        if (entryCount == 0) {
            return;
        }

        Enumeration<? extends ZipEntry> entries = this.zipFile.entries();
        long offset = 0L;

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            this.input.seek(offset + ZipAlignmentConstants.EXTRA_FIELD_LENGTH_OFFSET);
            byte[] extraLengthBytes = new byte[ZipAlignmentConstants.EXTRA_FIELD_LENGTH_SIZE];
            if (this.input.read(extraLengthBytes) != extraLengthBytes.length) {
                this.failed = true;
                throw new IOException("Reading extra field length failed");
            }

            int extraLength = (extraLengthBytes[0] & 0xFF) | ((extraLengthBytes[1] & 0xFF) << 8);
            int headerLength = ZipAlignmentConstants.LOCAL_FILE_HEADER_SIZE
                    + extraLength
                    + entry.getName().getBytes(ZipAlignmentConstants.CHARSET_UTF8).length;

            if (entry.getMethod() == ZipEntry.STORED && (offset + headerLength) % this.alignment != 0L) {
                this.failed = true;
            }

            int flags = entry.getMethod() == ZipEntry.STORED ? 0 : ZipAlignmentConstants.DATA_DESCRIPTOR_FLAG;
            flags |= UTF8_ENCODING_FLAG;

            long dataLength;
            if ((flags & ZipAlignmentConstants.DATA_DESCRIPTOR_FLAG) != 0) {
                dataLength = (entry.isDirectory() ? 0L : entry.getCompressedSize())
                        + ZipAlignmentConstants.DATA_DESCRIPTOR_SIZE;
            } else {
                dataLength = entry.isDirectory() ? 0L : entry.getCompressedSize();
            }

            offset += headerLength + dataLength;
        }
    }

    private void closeFiles() throws IOException {
        try {
            if (this.zipFile != null) {
                this.zipFile.close();
            }
        } finally {
            if (this.input != null) {
                this.input.close();
            }
        }
    }
}
