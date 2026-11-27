package com.licel.stringer;

import java.util.ArrayList;
import org.objectweb.asm.Opcodes;

public class ProtectionElementParams extends FilteredParams {
    protected ArrayList<String> paths = null;
    protected StringEncryptionParams stringEncryption = new StringEncryptionParams();
    protected IntegrityControlParams integrityControl = new IntegrityControlParams();
    protected HideAccessParams hideAccess = new HideAccessParams();
    protected ResourceEncryptionParams resourceEncryption = new ResourceEncryptionParams();
    protected String target;
    protected int targetInt = 0;

    public ArrayList<String> getPaths() {
        return this.paths;
    }

    public void setPaths(ArrayList<String> paths) {
        this.paths = paths;
    }

    public void addPath(String path) {
        if (this.paths == null) {
            this.paths = new ArrayList<>();
        }
        this.paths.add(path);
    }

    public String getPathsString() {
        if (this.paths == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < this.paths.size(); i++) {
            builder.append(this.paths.get(i)).append(';');
        }
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) == ';') {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    public StringEncryptionParams getStringEncryption() {
        return this.stringEncryption;
    }

    public void setStringEncryption(StringEncryptionParams stringEncryption) {
        this.stringEncryption = stringEncryption;
    }

    public IntegrityControlParams getIntegrityControl() {
        return this.integrityControl;
    }

    public void setIntegrityControl(IntegrityControlParams integrityControl) {
        this.integrityControl = integrityControl;
    }

    public HideAccessParams getHideAccess() {
        return this.hideAccess;
    }

    public void setHideAccess(HideAccessParams hideAccess) {
        this.hideAccess = hideAccess;
    }

    public ResourceEncryptionParams getResourceEncryption() {
        return this.resourceEncryption;
    }

    public void setResourceEncryption(ResourceEncryptionParams resourceEncryption) {
        this.resourceEncryption = resourceEncryption;
    }

    public String getTarget() {
        return this.target;
    }

    public int getTargetInt() {
        return this.targetInt;
    }

    public void setTarget(String target) {
        this.target = target;
        if (target != null) {
            try {
                this.targetInt = (Integer) Opcodes.class.getField("V" + target.replace('.', '_')).get(null);
            } catch (Throwable ex) {
                throw new RuntimeException("Incorrect value of target bytecode version: " + target + ". Allowed values are 1.5, 1.6, 1.7, 1.8, 9.");
            }
        }
    }

    public void dump(WorkerListener listener) {
        listener.verbose("    protectionElement path: " + this.getPathsString());
        if (this.getTarget() != null) {
            listener.verbose("    force bytecode target: " + this.getTarget() + "(" + this.getTargetInt() + ")");
        }

        listener.verbose("    string encryption: ");
        listener.verbose("        enabled: " + this.stringEncryption.isEnabled());
        listener.verbose("        checkCaller: " + this.stringEncryption.isCheckCaller());
        listener.verbose("        equalsIntrinsicEnabled: " + this.stringEncryption.isEqualsIntrinsicEnabled());
        listener.verbose("        annotations: " + this.stringEncryption.isAnnotations());
        listener.verbose("        filtered: " + this.stringEncryption.isFiltered());
        if (this.stringEncryption.filters != null) {
            listener.verbose("          filter: " + this.stringEncryption.getFilterString(";"));
        }

        listener.verbose("    hide access: ");
        listener.verbose("        enabled: " + this.hideAccess.isEnabled());
        listener.verbose("        annotations: " + this.hideAccess.isAnnotations());
        listener.verbose("        filtered: " + this.hideAccess.isFiltered());
        if (this.hideAccess.filters != null) {
            listener.verbose("          filter: " + this.hideAccess.getFilterString(";"));
        }

        listener.verbose("    resource encryption: ");
        listener.verbose("        enabled: " + this.resourceEncryption.isEnabled());
        listener.verbose("        filtered: " + this.resourceEncryption.isFiltered());
        if (this.resourceEncryption.filters != null) {
            listener.verbose("          filter: " + this.resourceEncryption.getFilterString(";"));
        }

        listener.verbose("    integrity control: ");
        listener.verbose("        enabled: " + this.integrityControl.isEnabled());
        listener.verbose("        checkBytecode: " + this.integrityControl.checkBytecode().isEnabled());
        listener.verbose("        checkJar: " + this.integrityControl.checkJar().isEnabled());
        listener.verbose("        sealJar: " + this.integrityControl.sealJar().isEnabled());
        listener.verbose("        osgiCompatibility: " + this.integrityControl.osgiCompatibility().isEnabled());
        listener.verbose("        filtered: " + this.integrityControl.isFiltered());
        if (this.integrityControl.filters != null) {
            listener.verbose("          filter: " + this.integrityControl.getFilterString(";"));
        }
    }
}
