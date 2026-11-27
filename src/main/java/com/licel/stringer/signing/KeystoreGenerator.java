package com.licel.stringer.signing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class KeystoreGenerator {
    private static final int KEY_SIZE = 2048;
    private static final int SERIAL_NUMBER_BITS = 128;

    private final String keystoreType = "JKS";
    private final char[] storePassword = "stringer".toCharArray();
    private final String keyAlias = "default";
    private final char[] keyPassword = "stringer".toCharArray();
    private final int validityYears = 25;
    private final String keyAlgorithm = "RSA";
    private final String signatureAlgorithm = "SHA256withRSA";

    public void generateKeystore(SecureRandom random, String commonName, String outputPath) {
        try {
            SecureRandom secureRandom = random != null ? random : new SecureRandom();
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(this.keyAlgorithm);
            keyPairGenerator.initialize(KEY_SIZE, secureRandom);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            Calendar calendar = Calendar.getInstance();
            Date notBefore = calendar.getTime();
            calendar.add(Calendar.YEAR, this.validityYears);
            Date notAfter = calendar.getTime();

            BigInteger serialNumber = new BigInteger(SERIAL_NUMBER_BITS, secureRandom);
            if (serialNumber.signum() == 0) {
                serialNumber = BigInteger.ONE;
            }

            X500Name subjectAndIssuer = new X500Name("CN=" + commonName);
            JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    subjectAndIssuer,
                    serialNumber,
                    notBefore,
                    notAfter,
                    subjectAndIssuer,
                    keyPair.getPublic()
            );
            ContentSigner contentSigner = new JcaContentSignerBuilder(this.signatureAlgorithm)
                    .setSecureRandom(secureRandom)
                    .build(keyPair.getPrivate());
            X509Certificate certificate = new JcaX509CertificateConverter()
                    .getCertificate(certBuilder.build(contentSigner));

            KeyStore keyStore = KeyStore.getInstance(this.keystoreType);
            keyStore.load(null, this.storePassword);
            keyStore.setKeyEntry(
                    this.keyAlias,
                    keyPair.getPrivate(),
                    this.keyPassword,
                    new X509Certificate[] { certificate }
            );

            try (OutputStream out = new FileOutputStream(new File(outputPath))) {
                keyStore.store(out, this.storePassword);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
