package com.licel.stringer.signing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerIdentifier;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;

public class SignedJarBuilder {
    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final String DIGEST_ATTR = "SHA-256-Digest";
    private static final String DIGEST_MANIFEST_ATTR = "SHA-256-Digest-Manifest";
    private static final String MANIFEST_ENTRY = "META-INF/MANIFEST.MF";
    private static final String SIGNATURE_FILE_ENTRY = "META-INF/CERT.SF";
    private static final String CREATED_BY = "1.0 (Stringer Java Obfuscator)";
    public static final String CHARSET_UTF8 = "UTF-8";

    private JarOutputStream jarOutputStream;
    private final PrivateKey privateKey;
    private final X509Certificate certificate;
    private Manifest manifest;
    private MessageDigest messageDigest;
    private final byte[] buffer = new byte[4096];

    public SignedJarBuilder(OutputStream out, PrivateKey privateKey, X509Certificate certificate)
            throws IOException, NoSuchAlgorithmException {
        this.jarOutputStream = new JarOutputStream(new BufferedOutputStream(out));
        this.jarOutputStream.setLevel(Deflater.BEST_COMPRESSION);
        this.privateKey = privateKey;
        this.certificate = certificate;

        if (this.privateKey != null && this.certificate != null) {
            this.manifest = new Manifest();
            Attributes mainAttributes = this.manifest.getMainAttributes();
            mainAttributes.putValue("Manifest-Version", "1.0");
            this.messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        }
    }

    public void writeFile(File inputFile, String jarPath) throws IOException {
        try (FileInputStream input = new FileInputStream(inputFile)) {
            JarEntry entry = new JarEntry(jarPath);
            entry.setTime(inputFile.lastModified());
            writeEntry(input, entry);
        }
    }

    public void computeDigests(ZipInputStream zipInput, ZipEntryFilter filter) throws IOException {
        try {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                String name = entry.getName();
                if (!entry.isDirectory()
                        && !name.equals(MANIFEST_ENTRY)
                        && (filter == null || filter.checkEntry(name))) {
                    digestEntry(zipInput, entry);
                    zipInput.closeEntry();
                }
            }
        } finally {
            zipInput.close();
        }
    }

    public void writeZip(File inputFile, ZipEntryFilter filter) throws IOException, GeneralSecurityException {
        computeDigests(new ZipInputStream(new FileInputStream(inputFile)), filter);

        if (this.manifest != null) {
            this.jarOutputStream.putNextEntry(new JarEntry(MANIFEST_ENTRY));
            this.manifest.write(this.jarOutputStream);
            this.jarOutputStream.closeEntry();

            String keyAlgorithm = this.privateKey.getAlgorithm();
            String signatureAlgorithm = resolveSignatureAlgorithm(keyAlgorithm);
            Signature signature = Signature.getInstance(signatureAlgorithm);
            signature.initSign(this.privateKey);

            this.jarOutputStream.putNextEntry(new JarEntry(SIGNATURE_FILE_ENTRY));
            SignatureOutputStream signatureOut = new SignatureOutputStream(this.jarOutputStream, signature);
            writeSignatureFile(signatureOut);
            this.jarOutputStream.closeEntry();

            this.jarOutputStream.putNextEntry(new JarEntry("META-INF/CERT." + keyAlgorithm));
            writeSignatureBlock(signature, this.certificate, this.privateKey);
            this.jarOutputStream.closeEntry();
        }

        try (ZipInputStream zipInput = new ZipInputStream(new FileInputStream(inputFile))) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                String name = entry.getName();
                if (!entry.isDirectory()
                        && !name.equals(MANIFEST_ENTRY)
                        && (filter == null || filter.checkEntry(name))) {
                    JarEntry outputEntry = entry.getMethod() == ZipEntry.STORED
                            ? new JarEntry(entry)
                            : new JarEntry(name);
                    writeEntry(zipInput, outputEntry);
                    zipInput.closeEntry();
                }
            }
        }
    }

    public void close() throws IOException {
        if (this.jarOutputStream != null) {
            this.jarOutputStream.close();
            this.jarOutputStream = null;
        }
    }

    public void cleanUp() {
        if (this.jarOutputStream != null) {
            try {
                this.jarOutputStream.close();
            } catch (IOException ignored) {
            }
            this.jarOutputStream = null;
        }
    }

    public Manifest getManifest() {
        return this.manifest;
    }

    private void digestEntry(InputStream input, ZipEntry entry) throws IOException {
        int bytesRead;
        while ((bytesRead = input.read(this.buffer)) != -1) {
            if (this.messageDigest != null) {
                this.messageDigest.update(this.buffer, 0, bytesRead);
            }
        }

        if (this.manifest != null) {
            Attributes attributes = this.manifest.getAttributes(entry.getName());
            if (attributes == null) {
                attributes = new Attributes();
                this.manifest.getEntries().put(entry.getName(), attributes);
            }
            String digest = Base64.getEncoder().encodeToString(this.messageDigest.digest());
            attributes.putValue(DIGEST_ATTR, digest);
        }
    }

    private void writeEntry(InputStream input, JarEntry entry) throws IOException {
        this.jarOutputStream.putNextEntry(entry);
        int bytesRead;
        while ((bytesRead = input.read(this.buffer)) != -1) {
            this.jarOutputStream.write(this.buffer, 0, bytesRead);
        }
        this.jarOutputStream.closeEntry();
    }

    private void writeSignatureFile(SignatureOutputStream out) throws IOException, GeneralSecurityException {
        Manifest signatureManifest = new Manifest();
        Attributes mainAttributes = signatureManifest.getMainAttributes();
        mainAttributes.putValue("Signature-Version", "1.0");
        mainAttributes.putValue("Created-By", CREATED_BY);

        Base64.Encoder base64Encoder = Base64.getEncoder();
        MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        PrintStream printStream = new PrintStream(
                new DigestOutputStream(OutputStream.nullOutputStream(), digest),
                true,
                CHARSET_UTF8
        );

        this.manifest.write(printStream);
        printStream.flush();
        mainAttributes.putValue(DIGEST_MANIFEST_ATTR, base64Encoder.encodeToString(digest.digest()));

        Map<String, Attributes> entries = this.manifest.getEntries();
        for (Map.Entry<String, Attributes> entry : entries.entrySet()) {
            printStream.print("Name: " + entry.getKey() + "\r\n");
            for (Map.Entry<Object, Object> attribute : entry.getValue().entrySet()) {
                printStream.print(attribute.getKey() + ": " + attribute.getValue() + "\r\n");
            }
            printStream.print("\r\n");
            printStream.flush();

            Attributes sectionAttributes = new Attributes();
            sectionAttributes.putValue(DIGEST_ATTR, base64Encoder.encodeToString(digest.digest()));
            signatureManifest.getEntries().put(entry.getKey(), sectionAttributes);
        }

        signatureManifest.write(out);

        // Work around a legacy Manifest parser boundary condition on multiples of 1024 bytes.
        if (out.getBytesWritten() % 1024 == 0) {
            out.write('\r');
            out.write('\n');
        }
    }

    private void writeSignatureBlock(Signature signature, X509Certificate certificate, PrivateKey privateKey)
            throws IOException, GeneralSecurityException {
        AlgorithmIdentifier digestAlgorithm = new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256);
        AlgorithmIdentifier encryptionAlgorithm = resolveEncryptionAlgorithm(privateKey.getAlgorithm());

        X500Name issuer = X500Name.getInstance(
                ASN1Primitive.fromByteArray(certificate.getIssuerX500Principal().getEncoded()));
        IssuerAndSerialNumber issuerAndSerial = new IssuerAndSerialNumber(issuer, certificate.getSerialNumber());
        SignerIdentifier signerIdentifier = new SignerIdentifier(issuerAndSerial);

        SignerInfo signerInfo = new SignerInfo(
                signerIdentifier,
                digestAlgorithm,
                (ASN1Set) null,
                encryptionAlgorithm,
                new DEROctetString(signature.sign()),
                (ASN1Set) null
        );

        ASN1EncodableVector digestAlgorithms = new ASN1EncodableVector();
        digestAlgorithms.add(digestAlgorithm);

        ASN1EncodableVector certificates = new ASN1EncodableVector();
        certificates.add(ASN1Primitive.fromByteArray(certificate.getEncoded()));

        ASN1EncodableVector signerInfos = new ASN1EncodableVector();
        signerInfos.add(signerInfo);

        ContentInfo encapContentInfo = new ContentInfo(PKCSObjectIdentifiers.data, null);
        SignedData signedData = new SignedData(
                new DERSet(digestAlgorithms),
                encapContentInfo,
                new DERSet(certificates),
                null,
                new DERSet(signerInfos)
        );
        ContentInfo contentInfo = new ContentInfo(PKCSObjectIdentifiers.signedData, signedData);
        this.jarOutputStream.write(contentInfo.getEncoded(ASN1Encoding.DER));
    }

    private static String resolveSignatureAlgorithm(String keyAlgorithm) {
        if ("EC".equalsIgnoreCase(keyAlgorithm)) {
            return "SHA256withECDSA";
        }
        return "SHA256with" + keyAlgorithm;
    }

    private static AlgorithmIdentifier resolveEncryptionAlgorithm(String keyAlgorithm)
            throws NoSuchAlgorithmException {
        if ("RSA".equalsIgnoreCase(keyAlgorithm)) {
            return new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE);
        }
        if ("DSA".equalsIgnoreCase(keyAlgorithm)) {
            return new AlgorithmIdentifier(X9ObjectIdentifiers.id_dsa);
        }
        if ("EC".equalsIgnoreCase(keyAlgorithm) || "ECDSA".equalsIgnoreCase(keyAlgorithm)) {
            return new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey);
        }
        throw new NoSuchAlgorithmException("Unsupported key algorithm: " + keyAlgorithm);
    }
}
