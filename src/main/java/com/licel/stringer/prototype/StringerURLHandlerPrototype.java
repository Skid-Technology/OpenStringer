package com.licel.stringer.prototype;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class StringerURLHandlerPrototype extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        if (url.getProtocol().equals("stringer")) {
            URL wrappedUrl = new URL(url.toString().substring(9));
            return new StringerURLConnectionPrototype(wrappedUrl);
        } else {
            return url.openConnection();
        }
    }
}
