package com.licel.stringer.webapp;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class WebXmlEntityResolver implements EntityResolver {
    public final WebXmlCompiler compiler;

    public WebXmlEntityResolver() {
        this(null);
    }

    public WebXmlEntityResolver(WebXmlCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) {
        return null;
    }
}
