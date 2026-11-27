package com.licel.stringer;

import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.util.StringerUtils;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class Config extends ProtectionElementParams {
    public static final String RELEASE_MODE = "release";
    public static final String DEFAULT_MODE = "default";
    public static final String NONE_MODE = "none";

    protected boolean verbose = false;
    protected boolean optimize = false;
    protected String nameGenMethod = "utf";
    protected long trialExpirationTimestampSeconds = 0L;
    protected String trialExpirationTimestamp = null;
    protected String trialExpirationTimestampPattern = null;
    protected List<String> injectList;
    protected File srcFile;
    protected File destFile;
    protected File proguardMapFile;
    protected Properties proguardMap;
    protected WorkerListener listener;
    protected String keystore;
    protected String keystoreType;
    protected String storepass;
    protected String alias;
    protected String keypass;
    protected String signMode = NONE_MODE;
    protected File tmpDir;
    protected ArrayList<String> includeList;
    protected ArrayList<String> excludeList;
    public boolean isLegacy = false;
    protected PersonalizationProfile personalizationProfile = new PersonalizationProfile();
    protected ProtectionElementParamsContainer protectionElements = new ProtectionElementParamsContainer();
    protected boolean forceGenerateInternalLibPkg = false;

    public void validate() {
        if (this.srcFile == null) {
            throw new RuntimeException("Missing srcFile");
        }
        if (!this.srcFile.exists()) {
            throw new RuntimeException("Could not find srcFile " + this.srcFile.getAbsolutePath());
        }
        if (this.destFile == null) {
            throw new RuntimeException("Missing destFile");
        }
        if (this.tmpDir != null && this.tmpDir.isFile()) {
            throw new RuntimeException("The value of tmpDir must point to a valid and writable directory");
        }

        if (this.trialExpirationTimestamp != null) {
            if (this.trialExpirationTimestampPattern == null) {
                try {
                    this.trialExpirationTimestampSeconds = Long.parseLong(this.trialExpirationTimestamp);
                    if (this.trialExpirationTimestampSeconds <= 0L) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    throw new RuntimeException("Incorrect format of trialExpirationTimestamp (positive integer > 0): " + this.trialExpirationTimestamp);
                }
            } else {
                try {
                    SimpleDateFormat format = new SimpleDateFormat(this.trialExpirationTimestampPattern);
                    this.trialExpirationTimestampSeconds = format.parse(this.trialExpirationTimestamp).getTime() / 1000L;
                } catch (IllegalArgumentException ex) {
                    throw new RuntimeException("Incorrect format of trialExpirationTimestampPattern: " + this.trialExpirationTimestampPattern);
                } catch (ParseException ex) {
                    throw new RuntimeException("Incorrect format of trialExpirationTimestamp (" + this.trialExpirationTimestampPattern + "): " + this.trialExpirationTimestamp);
                }
            }
        }

        if (!RELEASE_MODE.equalsIgnoreCase(this.signMode)
                && !DEFAULT_MODE.equalsIgnoreCase(this.signMode)
                && !NONE_MODE.equalsIgnoreCase(this.signMode)) {
            throw new RuntimeException("Incorrect signMode:" + this.signMode);
        }

        if (this.isReleaseMode()) {
            if (this.keystore == null) {
                throw new RuntimeException("In case of sign mode release it is necessary to set keystore path");
            }
            if (this.alias == null) {
                throw new RuntimeException("In case of sign mode release it is necessary to set key alias");
            }
            if (this.storepass == null) {
                throw new RuntimeException("In case of sign mode release it is necessary to set storepass");
            }
            if (this.keypass == null) {
                throw new RuntimeException("In case of sign mode release it is necessary to set keypass");
            }
        }

        if (this.includeList != null && this.excludeList != null) {
            this.stringEncryption.addFilter(new ClassInfo("glob:!**/**"));
            this.integrityControl.addFilter(new ClassInfo("glob:!**/**"));
            this.hideAccess.addFilter(new ClassInfo("glob:!**/**"));
            this.resourceEncryption.addFilter(new ClassInfo("glob:!**/**"));
        }

        if (this.includeList != null) {
            this.stringEncryption.setFiltered(true);
            this.stringEncryption.setEnabled(true);
            this.integrityControl.setFiltered(true);
            this.integrityControl.setEnabled(true);
            this.hideAccess.setFiltered(true);
            this.hideAccess.setEnabled(true);
            this.resourceEncryption.setFiltered(true);
            this.resourceEncryption.setEnabled(true);

            for (String include : this.includeList) {
                this.stringEncryption.addFilter(new ClassInfo(include));
                this.integrityControl.addFilter(new ClassInfo(include));
                this.hideAccess.addFilter(new ClassInfo(include));
                this.resourceEncryption.addFilter(new ClassInfo(include));
            }
        }

        if (this.excludeList != null) {
            this.stringEncryption.setFiltered(true);
            this.stringEncryption.setEnabled(true);
            this.integrityControl.setFiltered(true);
            this.integrityControl.setEnabled(true);
            this.hideAccess.setFiltered(true);
            this.hideAccess.setEnabled(true);
            this.resourceEncryption.setFiltered(true);
            this.resourceEncryption.setEnabled(true);

            for (String exclude : this.excludeList) {
                this.stringEncryption.addFilter(new ClassInfo("glob:!" + exclude));
                this.integrityControl.addFilter(new ClassInfo("glob:!" + exclude));
                this.hideAccess.addFilter(new ClassInfo("glob:!" + exclude));
                this.resourceEncryption.addFilter(new ClassInfo("glob:!" + exclude));
            }
        }

        if (this.proguardMapFile != null) {
            if (!this.proguardMapFile.exists()) {
                throw new RuntimeException("Could not find proguardMapFile: " + this.proguardMapFile.getAbsolutePath());
            }
            try {
                this.proguardMap = StringerUtils.loadProguardMap(this.proguardMapFile);
                this.stringEncryption.setMap(this.proguardMap);
                this.hideAccess.setMap(this.proguardMap);
                this.resourceEncryption.setMap(this.proguardMap);
                this.integrityControl.setMap(this.proguardMap);
            } catch (Exception ex) {
                throw new RuntimeException("Could not load proguardMapFile " + ex.getMessage());
            }
        } else {
            boolean hasNullPaths = false;
            for (ProtectionElementParams element : this.protectionElements.getElements()) {
                if (element.getPaths() == null) {
                    hasNullPaths = true;
                    break;
                }
            }
            if (hasNullPaths && this.protectionElements.getElements().size() > 1) {
                throw new RuntimeException("Protection elements w/ and w/o path tag cannot be used at the same time");
            }
        }
    }

    public ArrayList<Config> generateProtectionElementsConfigs() {
        ArrayList<Config> configs = new ArrayList<>();
        if (this.protectionElements.getElements().isEmpty()) {
            configs.add(this);
            return configs;
        }

        for (ProtectionElementParams element : this.protectionElements.getElements()) {
            Config elementConfig = new Config();
            elementConfig.setAlias(this.alias);
            elementConfig.setKeypass(this.keypass);
            elementConfig.setKeystore(this.keystore);
            elementConfig.setKeystoreType(this.keystoreType);
            elementConfig.setOptimize(this.optimize);
            elementConfig.setSignMode(this.signMode);
            elementConfig.setStorepass(this.storepass);
            elementConfig.setTrialExpirationTimestamp(this.trialExpirationTimestamp);
            elementConfig.setTrialExpirationTimestampPattern(this.trialExpirationTimestampPattern);
            elementConfig.setTrialExpirationTimestampSeconds(this.trialExpirationTimestampSeconds);
            elementConfig.setVerbose(this.verbose);
            elementConfig.setListener(this.listener);
            elementConfig.setProguardMapFile(this.proguardMapFile);
            elementConfig.setTarget(element.getTarget() != null ? element.getTarget() : this.target);
            elementConfig.setPaths(element.getPaths());
            elementConfig.setStringEncryption(element.getStringEncryption());
            elementConfig.setHideAccess(element.getHideAccess());
            elementConfig.setIntegrityControl(element.getIntegrityControl());
            elementConfig.setResourceEncryption(element.getResourceEncryption());
            if (this.proguardMap != null) {
                elementConfig.getStringEncryption().setMap(this.proguardMap);
                elementConfig.getHideAccess().setMap(this.proguardMap);
                elementConfig.getResourceEncryption().setMap(this.proguardMap);
                elementConfig.getIntegrityControl().setMap(this.proguardMap);
            }
            elementConfig.setSrcFile(this.srcFile);
            elementConfig.setDestFile(this.destFile);
            configs.add(elementConfig);
        }

        return configs;
    }

    public void addInclude(String pattern) {
        if (this.includeList == null) {
            this.includeList = new ArrayList<>();
        }
        this.includeList.add(pattern);
    }

    public void addExclude(String pattern) {
        if (this.excludeList == null) {
            this.excludeList = new ArrayList<>();
        }
        this.excludeList.add(pattern);
    }

    public void addInject(String pattern) {
        if (this.injectList == null) {
            this.injectList = new ArrayList<>();
        }
        this.injectList.add(pattern);
    }

    public List<String> getInjectList() {
        return this.injectList;
    }

    public void setCheckCaller(boolean checkCaller) {
        this.stringEncryption.setEnabled(true);
        this.stringEncryption.setCheckCaller(checkCaller);
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setIntegrityProtection(boolean integrityProtection) {
        this.integrityControl.setEnabled(true);
        this.integrityControl.checkBytecode().setEnabled(integrityProtection);
        this.integrityControl.checkJar().setEnabled(integrityProtection);
    }

    public void setResourceEncryption(boolean resourceEncryption) {
        this.resourceEncryption.setEnabled(resourceEncryption);
    }

    public boolean isOptimize() {
        return this.optimize;
    }

    public void setOptimize(boolean optimize) {
        this.optimize = optimize;
    }

    public boolean isJ2EECompatibility() {
        return this.isOsgiCompatibility();
    }

    public void setJ2EECompatibility(boolean j2eeCompatibility) {
        this.setOsgiCompatibility(j2eeCompatibility);
    }

    public boolean isOsgiCompatibility() {
        return this.integrityControl.osgiCompatibility().isEnabled();
    }

    public void setOsgiCompatibility(boolean osgiCompatibility) {
        this.integrityControl.osgiCompatibility().setEnabled(osgiCompatibility);
    }

    public String getNameGenMethod() {
        return this.nameGenMethod;
    }

    public void setNameGenMethod(String nameGenMethod) {
        this.nameGenMethod = nameGenMethod;
    }

    public File getDestFile() {
        return this.destFile;
    }

    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    public File getSrcFile() {
        return this.srcFile;
    }

    public void setSrcFile(File srcFile) {
        this.srcFile = srcFile;
    }

    public void setDestDir(File destDir) {
        this.destFile = destDir;
    }

    public void setSrcDir(File srcDir) {
        this.srcFile = srcDir;
    }

    public String getKeystore() {
        return this.keystore;
    }

    public void setKeystore(String keystore) {
        this.keystore = keystore;
    }

    public String getKeystoreType() {
        return this.keystoreType;
    }

    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }

    public String getStorepass() {
        return this.storepass;
    }

    public void setStorepass(String storepass) {
        this.storepass = storepass;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getKeypass() {
        return this.keypass;
    }

    public void setKeypass(String keypass) {
        this.keypass = keypass;
    }

    public String getSignMode() {
        return this.signMode;
    }

    public void setSignMode(String signMode) {
        this.signMode = signMode;
    }

    public boolean isReleaseMode() {
        return RELEASE_MODE.equalsIgnoreCase(this.signMode);
    }

    public boolean isDefaultMode() {
        return DEFAULT_MODE.equalsIgnoreCase(this.signMode);
    }

    public boolean isNoneMode() {
        return NONE_MODE.equalsIgnoreCase(this.signMode);
    }

    public File getProguardMapFile() {
        return this.proguardMapFile;
    }

    public void setProguardMapFile(File proguardMapFile) {
        this.proguardMapFile = proguardMapFile;
    }

    public void setProguardMapFile(String proguardMapFile) {
        this.proguardMapFile = new File(proguardMapFile);
    }

    public Properties getProguardMap() {
        return this.proguardMap;
    }

    public WorkerListener getListener() {
        return this.listener != null ? this.listener : this.getDefaultListener();
    }

    public void setListener(WorkerListener listener) {
        this.listener = listener;
    }

    public boolean isForceAndroid() {
        return false;
    }

    public void setForceAndroid(boolean forceAndroid) {
    }

    public String getTrialExpirationTimestamp() {
        return this.trialExpirationTimestamp;
    }

    public void setTrialExpirationTimestamp(String trialExpirationTimestamp) {
        this.trialExpirationTimestamp = trialExpirationTimestamp;
    }

    public long getTrialExpirationTimestampSeconds() {
        return this.trialExpirationTimestampSeconds;
    }

    public void setTrialExpirationTimestampSeconds(long trialExpirationTimestampSeconds) {
        this.trialExpirationTimestampSeconds = trialExpirationTimestampSeconds;
    }

    public String getTrialExpirationTimestampPattern() {
        return this.trialExpirationTimestampPattern;
    }

    public void setTrialExpirationTimestampPattern(String trialExpirationTimestampPattern) {
        this.trialExpirationTimestampPattern = trialExpirationTimestampPattern;
    }

    public void setCheckJar(boolean checkJar) {
        this.integrityControl.checkJar().setEnabled(checkJar);
    }

    public ProtectionElementParamsContainer getProtectionElements() {
        return this.protectionElements;
    }

    public PersonalizationProfile getPersonalizationProfile() {
        return this.personalizationProfile;
    }

    public void setPersonalizationProfile(PersonalizationProfile personalizationProfile) {
        this.personalizationProfile = personalizationProfile;
    }

    public File getTmpDir() {
        return this.tmpDir;
    }

    public void setTmpDir(File tmpDir) {
        this.tmpDir = tmpDir;
    }

    WorkerListener getDefaultListener() {
        return new ConsoleWorkerListener(this);
    }

    public boolean isForceGenerateInternalLibPkg() {
        return this.forceGenerateInternalLibPkg;
    }

    public void setForceGenerateInternalLibPkg(boolean forceGenerateInternalLibPkg) {
        this.forceGenerateInternalLibPkg = forceGenerateInternalLibPkg;
    }

    @Override
    public void dump(WorkerListener listener) {
        if (this.isLegacy) {
            listener.verbose("A legacy format configuration file is set. See actual protection settings in verbose mode.");
        }

        if (this.verbose) {
            listener.verbose("[Configuration]");
            listener.verbose("    source: " + this.srcFile.getAbsolutePath());
            listener.verbose("    destination: " + this.destFile.getAbsolutePath());
            listener.verbose("    sign mode: " + this.signMode);
            listener.verbose("    verbose: " + this.verbose);
            listener.verbose("    optimize: " + this.optimize);
            listener.verbose("    android library: " + this.isForceAndroid());
            if (this.getProguardMapFile() != null) {
                listener.verbose("    proguard map file: " + this.getProguardMapFile());
            }
            if (this.getTrialExpirationTimestampSeconds() > 0L) {
                Date expirationDate = new Date(this.getTrialExpirationTimestampSeconds() * 1000L);
                SimpleDateFormat format = new SimpleDateFormat();
                listener.verbose("    trialExpirationTimestamp: " + this.getTrialExpirationTimestampSeconds() + "(" + format.format(expirationDate) + ")");
            }
            if (this.getTmpDir() != null) {
                listener.verbose("    tmpDir: " + this.getTmpDir());
            }

            if (this.protectionElements.getElements().isEmpty()) {
                super.dump(listener);
            } else {
                for (ProtectionElementParams element : this.protectionElements.getElements()) {
                    element.dump(listener);
                }
            }
        }
    }
}
