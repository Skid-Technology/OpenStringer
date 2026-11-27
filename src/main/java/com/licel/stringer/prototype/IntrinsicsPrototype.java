package com.licel.stringer.prototype;

public class IntrinsicsPrototype {
    public static final boolean equalsIntrinsics(Object left, Object right) {
        return left == right || left instanceof String && right instanceof String && left.equals(right);
    }
}
