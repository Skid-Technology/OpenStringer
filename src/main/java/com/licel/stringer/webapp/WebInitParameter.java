package com.licel.stringer.webapp;

public class WebInitParameter {
    public final WebXmlCompiler compiler;
    public String name;
    public String value;

    public WebInitParameter(String name, String value) {
        this(null, name, value);
    }

    public WebInitParameter(WebXmlCompiler compiler, String name, String value) {
        this.compiler = compiler;
        this.name = name;
        this.value = value;
    }
}
