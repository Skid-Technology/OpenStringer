package com.licel.stringer.model;

public class MovedFieldInfo {
    public int access;
    public String name;
    public String descriptor;
    public String initialValue;

    public MovedFieldInfo(int access, String name, String descriptor, String signature, String initialValue) {
        this.access = access;
        this.name = name;
        this.descriptor = descriptor;
        this.initialValue = initialValue;
    }
}
