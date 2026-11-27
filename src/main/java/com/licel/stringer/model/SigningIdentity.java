package com.licel.stringer.model;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class SigningIdentity {
    public PrivateKey privateKey;
    public X509Certificate certificate;

    public SigningIdentity(X509Certificate certificate, PrivateKey privateKey) {
        this.certificate = certificate;
        this.privateKey = privateKey;
    }
}
