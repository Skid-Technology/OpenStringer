package com.licel.stringer;

public class ResourceEncryptionParams extends FilteredParams {
    private boolean generateStdWrappers = false;
    private boolean generateWebContaineWrappers = false;

    public ResourceEncryptionParams() {
        this.name = "resourceEncryption";
    }

    @Override
    public boolean match(String target) {
        return this.match(target, false);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            this.generateStdWrappers = true;
        }
    }

    public boolean isGenerateStdWrappers() {
        return this.generateStdWrappers;
    }

    public void setGenerateStdWrappers(boolean generateStdWrappers) {
        this.generateStdWrappers = generateStdWrappers;
    }

    public boolean isGenerateWebContaineWrappers() {
        return this.generateWebContaineWrappers;
    }

    public void setGenerateWebContaineWrappers(boolean generateWebContaineWrappers) {
        this.generateWebContaineWrappers = generateWebContaineWrappers;
    }
}
