package com.licel.stringer.model;

public class ClassMemberInfo {
    public int access;
    public String name;
    public String descriptor;
    public String owner;

    public ClassMemberInfo(int access, String owner, String name, String descriptor) {
        this.access = access;
        this.name = name;
        this.descriptor = descriptor;
        this.owner = owner;
    }

    public ClassMemberInfo(int access, String name, String descriptor) {
        this(access, null, name, descriptor);
    }

    public String getName() {
        return this.name;
    }
}
