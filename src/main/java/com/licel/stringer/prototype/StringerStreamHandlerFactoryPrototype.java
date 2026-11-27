package com.licel.stringer.prototype;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

public class StringerStreamHandlerFactoryPrototype implements URLStreamHandlerFactory {
    private final Map<String, URLStreamHandler> protocolHandlers = new HashMap<>();

    public StringerStreamHandlerFactoryPrototype(String protocol, URLStreamHandler urlHandler) {
        this.protocolHandlers.put(protocol, urlHandler);
    }

    public StringerStreamHandlerFactoryPrototype() {
    }

    public void addHandler(String protocol, URLStreamHandler urlHandler) {
        this.protocolHandlers.put(protocol, urlHandler);
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        return this.protocolHandlers.get(protocol);
    }
}
