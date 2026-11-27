package com.licel.stringer.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

public abstract class SourceIterator {
    public abstract Iterator<ResourceEntry> entries();

    public abstract InputStream openStream(ResourceEntry entry) throws IOException;

    public abstract void close() throws IOException;

    public abstract URL getSourceUrl() throws MalformedURLException;
}
