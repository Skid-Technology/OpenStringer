package com.licel.stringer.webapp;

import java.util.ArrayList;

public class WebApplicationDescriptor {
    public final WebXmlCompiler compiler;
    public ArrayList<WebInitParameter> contextParameters;
    public ArrayList<WebListenerDescriptor> listeners;
    public ArrayList<WebFilterDescriptor> filters;
    public ArrayList<WebServletDescriptor> servlets;

    public WebApplicationDescriptor() {
        this(null);
    }

    public WebApplicationDescriptor(WebXmlCompiler compiler) {
        this.compiler = compiler;
        this.contextParameters = new ArrayList<>();
        this.listeners = new ArrayList<>();
        this.filters = new ArrayList<>();
        this.servlets = new ArrayList<>();
    }
}
