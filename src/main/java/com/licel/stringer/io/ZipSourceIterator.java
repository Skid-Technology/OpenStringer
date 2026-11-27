package com.licel.stringer.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipSourceIterator extends SourceIterator {
    private final File archiveFile;
    private final ZipFile zipFile;

    public ZipSourceIterator(File archiveFile) throws IOException {
        this.archiveFile = archiveFile;
        this.zipFile = new ZipFile(archiveFile);
    }

    @Override
    public Iterator<ResourceEntry> entries() {
        List<ResourceEntry> entries = new ArrayList<>();
        Enumeration<? extends ZipEntry> zipEntries = this.zipFile.entries();

        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = zipEntries.nextElement();
            boolean isDirectory = entry.isDirectory()
                    || entry.getName().endsWith("/")
                    || entry.getName().endsWith("\\");
            entries.add(new ResourceEntry(entry.getName(), isDirectory, entry.getSize()));
        }

        return entries.iterator();
    }

    @Override
    public InputStream openStream(ResourceEntry entry) throws IOException {
        return this.zipFile.getInputStream(this.zipFile.getEntry(entry.getName()));
    }

    @Override
    public void close() throws IOException {
        this.zipFile.close();
    }

    @Override
    public URL getSourceUrl() throws MalformedURLException {
        return this.archiveFile.toURI().toURL();
    }
}
