package com.licel.stringer.model;

public class MethodInfo {
    public String owner;
    public String name;
    public String descriptor;
    public String proxyOwner;
    public String proxyName;
    public String proxyDescriptor;
    public boolean isStatic;
    public boolean isInterface;
    public boolean isConstructor;
    public String targetName;
    public int opcode;

    public MethodInfo() {
    }

    public MethodInfo(String owner, String name, String descriptor) {
        this.name = name;
        this.descriptor = descriptor;
        this.owner = owner;
    }

    public MethodInfo(
            boolean isConstructor,
            boolean isStatic,
            String proxyOwner,
            String proxyName,
            String proxyDescriptor,
            String name,
            String descriptor,
            boolean isInterface) {
        this.isStatic = isStatic;
        this.proxyOwner = proxyOwner;
        this.proxyName = proxyName;
        this.name = name;
        this.descriptor = descriptor;
        this.proxyDescriptor = proxyDescriptor;
        this.isInterface = isInterface;
        this.isConstructor = isConstructor;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other != null && this.getClass() == other.getClass()) {
            MethodInfo that = (MethodInfo) other;
            if (!this.descriptor.equals(that.descriptor)) {
                return false;
            }
            return this.name.equals(that.name) && this.owner.equals(that.owner);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = this.name.hashCode();
        result = 31 * result + this.descriptor.hashCode();
        return 31 * result + this.owner.hashCode();
    }

    @Override
    public String toString() {
        return "MethodInfo{owner='" + this.owner + '\'' + ", name='" + this.name + '\'' + ", desc='" + this.descriptor + '\'' + '}';
    }
}
