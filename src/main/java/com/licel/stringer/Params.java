package com.licel.stringer;

public class Params {
    protected String name;
    protected boolean enabled = false;

    public String getName() {
        return this.name;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
