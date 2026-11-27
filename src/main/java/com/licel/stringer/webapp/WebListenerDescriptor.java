package com.licel.stringer.webapp;

public class WebListenerDescriptor {
    public final WebXmlCompiler compiler;
    public String className;

    public WebListenerDescriptor(String className) {
        this(null, className);
    }

    public WebListenerDescriptor(WebXmlCompiler compiler, String className) {
        this.compiler = compiler;
        this.className = className;
    }
}
