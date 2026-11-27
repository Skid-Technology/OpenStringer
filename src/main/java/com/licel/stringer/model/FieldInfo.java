package com.licel.stringer.model;

public class FieldInfo {
    public String name;
    public String descriptor;
    public String owner;

    public FieldInfo() {
    }

    public FieldInfo(String owner, String name, String descriptor) {
        this.name = name;
        this.descriptor = descriptor;
        this.owner = owner;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other != null && this.getClass() == other.getClass()) {
            FieldInfo that = (FieldInfo) other;
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
        return "FieldInfo{name='" + this.name + '\'' + ", desc='" + this.descriptor + '\'' + ", owner='" + this.owner + '\'' + '}';
    }
}
