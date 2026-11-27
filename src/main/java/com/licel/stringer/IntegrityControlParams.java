package com.licel.stringer;

public class IntegrityControlParams extends FilteredParams {
    private final CheckByteCodeParams checkBytecode = new CheckByteCodeParams();
    private final CheckJarParams checkJar = new CheckJarParams();
    private final SealJarParams sealJar = new SealJarParams();
    private final OsgiCompatibility osgiCompatibility = new OsgiCompatibility();

    public IntegrityControlParams() {
        this.name = "integrityControl";
    }

    public CheckByteCodeParams checkBytecode() {
        return this.checkBytecode;
    }

    public CheckJarParams checkJar() {
        return this.checkJar;
    }

    public SealJarParams sealJar() {
        return this.sealJar;
    }

    public OsgiCompatibility osgiCompatibility() {
        return this.osgiCompatibility;
    }

    public class CheckByteCodeParams extends Params {
        CheckByteCodeParams() {
            this.enabled = false;
            this.name = "checkBytecode";
        }
    }

    public class CheckJarParams extends Params {
        CheckJarParams() {
            this.enabled = true;
            this.name = "checkJar";
        }
    }

    public class SealJarParams extends Params {
        SealJarParams() {
            this.enabled = false;
            this.name = "sealJar";
        }
    }

    public class OsgiCompatibility extends Params {
        OsgiCompatibility() {
            this.enabled = false;
            this.name = "osgiCompatibility";
        }
    }
}
