package com.licel.stringer.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipResourcesOutput extends ResourcesOutput {
    private static final int DEFAULT_BUFFER_SIZE = 16 * 1024 * 1024;

    private final File outputFile;
    private final boolean optimize;
    private ByteArrayOutputStream buffer;
    private ZipOutputStream zipOutputStream;

    public ZipResourcesOutput(File outputFile, boolean optimize) {
        this.outputFile = outputFile;
        this.optimize = optimize;
    }

    @Override
    public void openEntry(ResourceEntry entry) throws IOException {
        ZipEntry zipEntry = new ZipEntry(entry.getName());
        zipEntry.setExtra(entry.getExtra());
        this.zipOutputStream.putNextEntry(zipEntry);
    }

    public void closeArchiveStream() throws IOException {
        this.zipOutputStream.close();
    }

    @Override
    public void write(byte[] data) throws IOException {
        this.zipOutputStream.write(data);
    }

    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        this.zipOutputStream.write(data, offset, length);
    }

    @Override
    public void open() throws IOException {
        if (this.outputFile.exists()) {
            this.buffer = new ByteArrayOutputStream((int) this.outputFile.length());
        } else {
            this.buffer = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
        }

        this.zipOutputStream = new ZipOutputStream(this.buffer);
        if (this.optimize) {
            this.zipOutputStream.setLevel(Deflater.BEST_COMPRESSION);
        }
    }

    @Override
    public void close() throws IOException {
        this.zipOutputStream.close();
        if (!this.outputFile.exists()) {
            this.outputFile.createNewFile();
        }

        try (FileOutputStream fileOut = new FileOutputStream(this.outputFile)) {
            this.buffer.writeTo(fileOut);
        }
    }
}
