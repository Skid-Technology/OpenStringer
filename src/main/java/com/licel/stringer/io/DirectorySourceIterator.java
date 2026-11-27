package com.licel.stringer.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DirectorySourceIterator extends SourceIterator {
    private final File rootDirectory;
    private final int rootPathLength;

    public DirectorySourceIterator(File rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.rootPathLength = this.rootDirectory.getAbsolutePath().length() + 1;
    }

    @Override
    public Iterator<ResourceEntry> entries() {
        List<ResourceEntry> entries = new ArrayList<>();
        this.collectEntries(this.rootDirectory, entries);
        return entries.iterator();
    }

    @Override
    public InputStream openStream(ResourceEntry entry) throws IOException {
        return new FileInputStream(this.rootDirectory + File.separator + entry.getName());
    }

    private void collectEntries(File directory, List<ResourceEntry> entries) {
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }

        for (File child : children) {
            String relativeName = child.getAbsolutePath().substring(this.rootPathLength);
            entries.add(new ResourceEntry(relativeName, child.isDirectory(), child.length()));
            if (child.isDirectory()) {
                this.collectEntries(child, entries);
            }
        }
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public URL getSourceUrl() throws MalformedURLException {
        return this.rootDirectory.toURI().toURL();
    }
}
