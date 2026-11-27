package com.licel.stringer.model;

public class StackValue {
    public static final int KIND_OTHER = 0;
    public static final int KIND_FIELD_REF = 1;
    public static final int KIND_METHOD_RETURN = 2;

    public int kind;
    public int index;

    public StackValue() {
    }

    public StackValue(int kind) {
        this.kind = kind;
    }

    public StackValue(int kind, int index) {
        this.kind = kind;
        this.index = index;
    }
}
