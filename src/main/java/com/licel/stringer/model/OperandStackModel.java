package com.licel.stringer.model;

import java.util.ArrayList;

public class OperandStackModel extends ArrayList<StackValue> {
    public OperandStackModel() {
    }

    public void pop() {
        this.remove(this.size() - 1);
    }

    public void replaceTop(StackValue value) {
        this.set(this.size() - 1, value);
    }
}
