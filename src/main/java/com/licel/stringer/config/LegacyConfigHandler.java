package com.licel.stringer.config;

import com.licel.stringer.Config;
import com.licel.stringer.FileConfig;
import java.io.File;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class LegacyConfigHandler extends DefaultHandler {
    protected final FileConfig fileConfig;
    protected final Config config;

    public LegacyConfigHandler(FileConfig fileConfig, Config config) {
        this.fileConfig = fileConfig;
        this.config = config;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (qName.equalsIgnoreCase("stringer")) {
            if (attributes.getValue("srcFile") != null) {
                this.config.setSrcFile(new File(attributes.getValue("srcFile")));
            }
            if (attributes.getValue("srcDir") != null) {
                this.config.setSrcDir(new File(attributes.getValue("srcDir")));
            }
            if (attributes.getValue("destFile") != null) {
                this.config.setDestFile(new File(attributes.getValue("destFile")));
            }
            if (attributes.getValue("destDir") != null) {
                this.config.setDestDir(new File(attributes.getValue("destDir")));
            }
            if (attributes.getValue("verbose") != null) {
                this.config.setVerbose(Boolean.parseBoolean(attributes.getValue("verbose")));
            }
            if (attributes.getValue("integrityProtection") != null) {
                this.config.setIntegrityProtection(Boolean.parseBoolean(attributes.getValue("integrityProtection")));
            }
            if (attributes.getValue("resourceEncryption") != null) {
                this.config.setResourceEncryption(Boolean.parseBoolean(attributes.getValue("resourceEncryption")));
            }
            if (attributes.getValue("optimize") != null) {
                this.config.setOptimize(Boolean.parseBoolean(attributes.getValue("optimize")));
            }
            if (attributes.getValue("osgiCompatibility") != null) {
                this.config.setOsgiCompatibility(Boolean.parseBoolean(attributes.getValue("osgiCompatibility")));
            }
            if (attributes.getValue("nameGenMethod") != null) {
                this.config.setNameGenMethod(attributes.getValue("nameGenMethod"));
            }
            if (attributes.getValue("checkCaller") != null) {
                this.config.setCheckCaller(Boolean.parseBoolean(attributes.getValue("checkCaller")));
            }
            if (attributes.getValue("proguardMapFile") != null) {
                this.config.setProguardMapFile(new File(attributes.getValue("proguardMapFile")));
            }
            if (attributes.getValue("target") != null) {
                this.config.setTarget(attributes.getValue("target"));
            }
            if (attributes.getValue("trialExpirationTimestamp") != null) {
                this.config.setTrialExpirationTimestamp(attributes.getValue("trialExpirationTimestamp"));
            }
            if (attributes.getValue("trialExpirationTimestampPattern") != null) {
                this.config.setTrialExpirationTimestampPattern(attributes.getValue("trialExpirationTimestampPattern"));
            }
            if (attributes.getValue("checkJar") != null) {
                this.config.setCheckJar(Boolean.parseBoolean(attributes.getValue("checkJar")));
            }
        } else if (qName.equalsIgnoreCase("include")) {
            this.config.addInclude(attributes.getValue("name"));
        } else if (qName.equalsIgnoreCase("exclude")) {
            this.config.addExclude(attributes.getValue("name"));
        } else if (qName.equalsIgnoreCase("inject")) {
            this.config.addInject(attributes.getValue("name"));
        }
    }
}
