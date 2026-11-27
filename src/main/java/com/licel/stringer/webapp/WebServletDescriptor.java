package com.licel.stringer.webapp;

import java.util.ArrayList;

public class WebServletDescriptor {
    public final WebXmlCompiler compiler;
    public String name;
    public String className;
    public ArrayList<WebInitParameter> initParameters;
    public String loadOnStartup;
    public ArrayList<String> urlPatterns;
    public String asyncSupported;

    public WebServletDescriptor(String name, String className) {
        this(null, name, className);
    }

    public WebServletDescriptor(WebXmlCompiler compiler, String name, String className) {
        this.compiler = compiler;
        this.initParameters = new ArrayList<>();
        this.urlPatterns = new ArrayList<>();
        this.name = name;
        this.className = className;
    }
}
