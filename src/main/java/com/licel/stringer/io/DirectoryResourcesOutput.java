package com.licel.stringer.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DirectoryResourcesOutput extends ResourcesOutput {
    private final File outputDirectory;
    private File currentOutputFile;
    private FileOutputStream outputStream;
    public final boolean overwriteSource;

    public DirectoryResourcesOutput(File sourceDirectory, File outputDirectory) {
        this.outputDirectory = outputDirectory;
        this.overwriteSource = outputDirectory.equals(sourceDirectory);
    }

    public boolean isOverwriteSource() {
        return this.overwriteSource;
    }

    @Override
    public void openEntry(ResourceEntry entry) throws IOException {
        String outputPath = this.resolveEntryPath(entry.getName());
        this.closeCurrentStream();
        this.currentOutputFile = new File(outputPath);
        if (entry.isDirectory()) {
            this.currentOutputFile.mkdirs();
        } else {
            File parentDir = this.currentOutputFile.getParentFile();
            if (parentDir != null) {
                parentDir.mkdirs();
            }
        }
    }

    public void closeCurrentStream() throws IOException {
        if (this.outputStream != null) {
            this.outputStream.close();
        }

        this.outputStream = null;
    }

    @Override
    public void write(byte[] data) throws IOException {
        if (this.outputStream == null) {
            this.outputStream = new FileOutputStream(this.currentOutputFile);
        }

        this.outputStream.write(data);
    }

    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        if (this.outputStream == null) {
            this.outputStream = new FileOutputStream(this.currentOutputFile);
        }

        this.outputStream.write(data, offset, length);
    }

    @Override
    public void open() throws IOException {
        if (!this.outputDirectory.exists()) {
            this.outputDirectory.mkdirs();
        }
    }

    @Override
    public void close() throws IOException {
        this.closeCurrentStream();
    }

    private String resolveEntryPath(String entryName) {
        return this.outputDirectory.getAbsolutePath() + File.separator + entryName;
    }
}
