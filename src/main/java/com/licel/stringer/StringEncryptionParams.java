package com.licel.stringer;

public class StringEncryptionParams extends FilteredParams {
    public static final String ANNOTATION = "Lcom/stringer/annotations/StringEncryption;";

    private boolean checkCaller = true;
    private boolean equalsIntrinsicEnabled = false;

    public StringEncryptionParams() {
        this.name = "stringEncryption";
    }

    public boolean isCheckCaller() {
        return this.checkCaller;
    }

    public void setCheckCaller(boolean checkCaller) {
        this.checkCaller = checkCaller;
    }

    public boolean isEqualsIntrinsicEnabled() {
        return this.equalsIntrinsicEnabled;
    }

    public void setEqualsIntrinsicEnabled(boolean equalsIntrinsicEnabled) {
        this.equalsIntrinsicEnabled = equalsIntrinsicEnabled;
    }

    public boolean isInternStrings() {
        return this.equalsIntrinsicEnabled;
    }

    public void setInternStrings(boolean equalsIntrinsicEnabled) {
        this.equalsIntrinsicEnabled = equalsIntrinsicEnabled;
    }
}
