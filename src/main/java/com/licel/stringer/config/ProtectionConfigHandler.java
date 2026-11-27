package com.licel.stringer.config;

import com.licel.stringer.Config;
import com.licel.stringer.FileConfig;
import com.licel.stringer.FilteredParams;
import com.licel.stringer.IntegrityControlParams;
import com.licel.stringer.ProtectionElementParams;
import com.licel.stringer.ProtectionElementParamsContainer;
import com.licel.stringer.StringEncryptionParams;
import com.licel.stringer.model.ClassInfo;
import java.io.File;
import java.text.ParseException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ProtectionConfigHandler extends LegacyConfigHandler {
    private StringBuilder elementText;
    private FilteredParams currentSection;
    private FilteredParams currentFeature;
    private ClassInfo pendingFilterClass;

    public ProtectionConfigHandler(FileConfig fileConfig, Config config) {
        super(fileConfig, config);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        this.elementText = new StringBuilder();
        if (qName.equalsIgnoreCase("stringer")) {
            this.config.isLegacy = true;
            this.config.getStringEncryption().setEnabled(true);
            this.config.getHideAccess().setEnabled(true);
            this.config.getIntegrityControl().setEnabled(true);
            this.config.getResourceEncryption().setEnabled(true);
        }

        if (qName.equals("protectionElements")) {
            this.currentSection = this.config.getProtectionElements();
            this.currentSection.setEnabled(true);
        } else if (qName.equals("protectionElement") && this.currentSection instanceof ProtectionElementParamsContainer) {
            this.currentSection = this.config.getProtectionElements().createElement();
            this.currentSection.setEnabled(true);
        } else if (qName.equalsIgnoreCase("stringEncryption") && this.currentSection instanceof ProtectionElementParams) {
            this.currentFeature = ((ProtectionElementParams) this.currentSection).getStringEncryption();
            this.currentFeature.setEnabled(true);
        } else if (qName.equalsIgnoreCase("hideAccess") && this.currentSection instanceof ProtectionElementParams) {
            this.currentFeature = ((ProtectionElementParams) this.currentSection).getHideAccess();
            this.currentFeature.setEnabled(true);
        } else if (qName.equalsIgnoreCase("resourceEncryption") && this.currentSection instanceof ProtectionElementParams) {
            this.currentFeature = ((ProtectionElementParams) this.currentSection).getResourceEncryption();
            this.currentFeature.setEnabled(true);
        } else if (qName.equalsIgnoreCase("integrityControl") && this.currentSection instanceof ProtectionElementParams) {
            this.currentFeature = ((ProtectionElementParams) this.currentSection).getIntegrityControl();
            this.currentFeature.setEnabled(true);
        }

        if (this.config.isLegacy) {
            super.startElement(uri, localName, qName, attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String text = this.elementText.toString();
        if (qName.equalsIgnoreCase("filter")) {
            if (this.currentSection != null && this.currentFeature == null && !text.trim().isEmpty()) {
                this.pendingFilterClass = new ClassInfo(text);
                this.currentSection.addFilter(this.pendingFilterClass);
            }
            if (this.currentFeature != null && !text.trim().isEmpty()) {
                this.pendingFilterClass = new ClassInfo(text);
                this.currentFeature.addFilter(this.pendingFilterClass);
            }
        }

        if (qName.equals("mode") && this.currentFeature != null) {
            try {
                this.currentFeature.setMode(text);
            } catch (ParseException ex) {
                throw new SAXException(ex);
            }
        }

        if (qName.equals("verbose")) {
            this.config.setVerbose(Boolean.parseBoolean(text));
        }
        if (qName.equals("optimize")) {
            this.config.setOptimize(Boolean.parseBoolean(text));
        }
        if (qName.equals("proguardMapFile")) {
            this.config.setProguardMapFile(new File(text));
        }
        if (qName.equals("signMode")) {
            this.config.setSignMode(text);
        }
        if (qName.equals("keystore") && this.fileConfig.isReleaseMode() && !text.isEmpty()) {
            this.config.setKeystore(text);
        }
        if (qName.equals("alias") && this.fileConfig.isReleaseMode() && !text.isEmpty()) {
            this.config.setAlias(text);
        }
        if (qName.equals("storepass") && this.fileConfig.isReleaseMode() && !text.isEmpty()) {
            this.config.setStorepass(text);
        }
        if (qName.equals("storetype") && this.fileConfig.isReleaseMode() && !text.isEmpty()) {
            this.config.setKeystoreType(text);
        }
        if (qName.equals("keypass") && this.fileConfig.isReleaseMode() && !text.isEmpty()) {
            this.config.setKeypass(text);
        }
        if (qName.equals("trialExpirationTimestamp") && !text.isEmpty()) {
            this.config.setTrialExpirationTimestamp(text);
        }
        if (qName.equals("trialExpirationTimestampPattern") && !text.isEmpty()) {
            this.config.setTrialExpirationTimestampPattern(text);
        }
        if (qName.equals("target") && !text.isEmpty()) {
            this.config.setTarget(text);
        }
        if (qName.equals("tmpDir")) {
            this.config.setTmpDir(new File(text));
        }
        if (qName.equals("path") && this.currentSection instanceof ProtectionElementParams) {
            ((ProtectionElementParams) this.currentSection).addPath(text);
        }
        if (qName.equals("target") && this.currentSection instanceof ProtectionElementParams) {
            ((ProtectionElementParams) this.currentSection).setTarget(text);
        }
        if (qName.equals("checkCaller") && this.currentFeature instanceof StringEncryptionParams) {
            ((StringEncryptionParams) this.currentFeature).setCheckCaller(Boolean.parseBoolean(text));
        }
        if (qName.equals("internStrings") && this.currentFeature instanceof StringEncryptionParams) {
            ((StringEncryptionParams) this.currentFeature).setEqualsIntrinsicEnabled(Boolean.parseBoolean(text));
        }
        if (qName.equals("equalsIntrinsicEnabled") && this.currentFeature instanceof StringEncryptionParams) {
            ((StringEncryptionParams) this.currentFeature).setEqualsIntrinsicEnabled(Boolean.parseBoolean(text));
        }
        if (qName.equals("checkBytecode") && this.currentFeature instanceof IntegrityControlParams) {
            ((IntegrityControlParams) this.currentFeature).checkBytecode().setEnabled(Boolean.parseBoolean(text));
        }
        if (qName.equals("checkJar") && this.currentFeature instanceof IntegrityControlParams) {
            ((IntegrityControlParams) this.currentFeature).checkJar().setEnabled(Boolean.parseBoolean(text));
        }
        if (qName.equals("osgiCompatibility") && this.currentFeature instanceof IntegrityControlParams) {
            ((IntegrityControlParams) this.currentFeature).osgiCompatibility().setEnabled(Boolean.parseBoolean(text));
        }
        if (qName.equals("protectionElement")) {
            this.currentSection = this.config.getProtectionElements();
        }
        if (qName.equals("protectionElements")) {
            this.currentSection = null;
        }

        this.elementText = new StringBuilder();
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        this.elementText.append(ch, start, length);
    }
}
