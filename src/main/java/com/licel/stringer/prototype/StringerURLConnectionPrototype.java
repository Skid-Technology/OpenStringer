package com.licel.stringer.prototype;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class StringerURLConnectionPrototype extends URLConnection {
    URLConnection parentConnection;

    public StringerURLConnectionPrototype(URL url) throws IOException {
        super(url);
        this.parentConnection = url.openConnection();
    }

    @Override
    public void connect() throws IOException {
        this.parentConnection.connect();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new StringerInputStreamPrototype(this.parentConnection.getInputStream());
    }
}
