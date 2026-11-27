package com.licel.stringer;

import java.io.File;
import java.lang.reflect.Method;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.PatternSet.NameEntry;

public class AntTask extends Task {
    private Config config = new Config();
    private final PatternSet patternSet;

    public AntTask() {
        this.config.setCheckCaller(true);
        this.config.setListener(new AntTaskWorkerListener(this, this));
        this.patternSet = new PatternSet();
    }

    @Override
    public void execute() {
        this.log("Mode: ANT");
        this.log(ProductInfo.getConsoleTitle());
        Project project = this.getProject();

        String[] includes = this.patternSet.getIncludePatterns(project);
        if (includes != null) {
            for (String include : includes) {
                this.config.addInclude(include);
            }
        }

        String[] excludes = this.patternSet.getExcludePatterns(project);
        if (excludes != null) {
            for (String exclude : excludes) {
                this.config.addExclude(exclude);
            }
        }

        try {
            Worker worker = new Worker(this.config);
            worker.stringerIt();
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw new BuildException(ex.getMessage());
        }
    }

    public void setSrcFile(File srcFile) {
        this.config.setSrcFile(srcFile);
    }

    public void setSrcDir(File srcDir) {
        this.config.setSrcDir(srcDir);
    }

    public void setDestFile(File destFile) {
        this.config.setDestFile(destFile);
    }

    public void setDestDir(File destDir) {
        this.config.setDestDir(destDir);
    }

    public void setVerbose(boolean verbose) {
        this.config.setVerbose(verbose);
    }

    public ParamsOptions createVerbose() {
        return new ParamsOptions(this.config, "setVerbose", boolean.class);
    }

    public void setOptimize(boolean optimize) {
        this.config.setOptimize(optimize);
    }

    public ParamsOptions createOptimize() {
        return new ParamsOptions(this.config, "setOptimize", boolean.class);
    }

    public void setIntegrityProtection(boolean integrityProtection) {
        this.config.setIntegrityProtection(integrityProtection);
    }

    public void setResourceEncryption(boolean resourceEncryption) {
        this.config.setResourceEncryption(resourceEncryption);
    }

    public void setOsgiCompatibility(boolean osgiCompatibility) {
        this.config.setOsgiCompatibility(osgiCompatibility);
    }

    public void setJ2EECompatibility(boolean j2eeCompatibility) {
        this.config.setOsgiCompatibility(true);
    }

    public void setNameGenMethod(String nameGenMethod) {
        this.config.setNameGenMethod(nameGenMethod);
    }

    public void setProguardMapFile(File proguardMapFile) {
        this.config.setProguardMapFile(proguardMapFile);
    }

    public ParamsOptions createProguardMapFile() {
        return new ParamsOptions(this.config, "setProguardMapFile", String.class);
    }

    public void setTarget(String target) {
        this.config.setTarget(target);
    }

    public ParamsOptions createTarget() {
        return new ParamsOptions(this.config, "setTarget", String.class);
    }

    public void setForceAndroid(boolean forceAndroid) {
        this.config.setForceAndroid(forceAndroid);
    }

    public void setTrialExpirationTimestamp(String trialExpirationTimestamp) {
        this.config.setTrialExpirationTimestamp(trialExpirationTimestamp);
    }

    public ParamsOptions createTrialExpirationTimestamp() {
        return new ParamsOptions(this.config, "setTrialExpirationTimestamp", String.class);
    }

    public void setTrialExpirationTimestampPattern(String trialExpirationTimestampPattern) {
        this.config.setTrialExpirationTimestampPattern(trialExpirationTimestampPattern);
    }

    public ParamsOptions createTrialExpirationTimestampPattern() {
        return new ParamsOptions(this.config, "setTrialExpirationTimestampPattern", String.class);
    }

    public void setSignMode(String signMode) {
        this.config.setSignMode(signMode);
    }

    public ParamsOptions createSignMode() {
        return new ParamsOptions(this.config, "setSignMode", String.class);
    }

    public void setKeystore(String keystore) {
        this.config.setKeystore(keystore);
    }

    public ParamsOptions createKeystore() {
        return new ParamsOptions(this.config, "setKeystore", String.class);
    }

    public void setAlias(String alias) {
        this.config.setAlias(alias);
    }

    public ParamsOptions createAlias() {
        return new ParamsOptions(this.config, "setAlias", String.class);
    }

    public void setStorepass(String storepass) {
        this.config.setStorepass(storepass);
    }

    public ParamsOptions createStorepass() {
        return new ParamsOptions(this.config, "setStorepass", String.class);
    }

    public void setKeypass(String keypass) {
        this.config.setKeypass(keypass);
    }

    public ParamsOptions createKeypass() {
        return new ParamsOptions(this.config, "setKeypass", String.class);
    }

    public void setStoretype(String storetype) {
        this.config.setKeystoreType(storetype);
    }

    public ParamsOptions createStoretype() {
        return new ParamsOptions(this.config, "setKeystoreType", String.class);
    }

    public void setConfigFile(File configFile) {
        try {
            FileConfig fileConfig = new FileConfig(configFile);
            if (this.config.getDestFile() != null) {
                fileConfig.setDestFile(this.config.getDestFile());
            }
            if (this.config.getSrcFile() != null) {
                fileConfig.setSrcFile(this.config.getSrcFile());
            }
            this.config = fileConfig;
        } catch (Exception ex) {
            throw new BuildException(ex);
        }
    }

    public NameEntry createInclude() {
        return this.patternSet.createInclude();
    }

    public NameEntry createExclude() {
        return this.patternSet.createExclude();
    }

    public void setCheckCaller(boolean checkCaller) {
        this.config.setCheckCaller(checkCaller);
    }

    public void setCheckJar(boolean checkJar) {
        this.config.setCheckJar(checkJar);
    }

    public ProtectionElementsOptions createProtectionElements() {
        return new ProtectionElementsOptions(this.config.getProtectionElements());
    }

    public class ParamsOptions {
        protected Params container;
        protected Method setMethod;
        protected Method valueOfMethod;

        public ParamsOptions(Params container, String methodName, Class<?> valueType) {
            this.container = container;
            try {
                this.setMethod = container.getClass().getMethod(methodName, valueType);
                Class<?> boxedType = valueType.equals(boolean.class) ? Boolean.class : valueType;
                Class<?> valueOfArg = boxedType.equals(String.class) ? Object.class : String.class;
                this.valueOfMethod = boxedType.getMethod("valueOf", valueOfArg);
            } catch (Exception ex) {
                throw new BuildException(ex);
            }
        }

        public void addText(String text) {
            try {
                Object parsedValue = this.valueOfMethod.invoke(null, text);
                this.setMethod.invoke(this.container, parsedValue);
            } catch (Exception ex) {
                throw new BuildException(ex);
            }
        }
    }

    public class FiltersOptions {
        protected Params container;

        public FiltersOptions(Params container) {
            this.container = container;
        }

        public ParamsOptions createFilter() {
            return new ParamsOptions(this.container, "addFilter", String.class);
        }
    }

    public class FilteredParamsOptions {
        protected Params container;

        public FilteredParamsOptions(Params container) {
            this.container = container;
            container.setEnabled(true);
        }

        public FiltersOptions createFilters() {
            return new FiltersOptions(this.container);
        }

        public ParamsOptions createMode() {
            return new ParamsOptions(this.container, "setMode", String.class);
        }
    }

    public class StringEncryptonParamsOptions extends FilteredParamsOptions {
        public StringEncryptonParamsOptions(Params params) {
            super(params);
        }

        public ParamsOptions createCheckCaller() {
            return new ParamsOptions(this.container, "setCheckCaller", boolean.class);
        }

        public ParamsOptions createEqualsIntrinsicEnabled() {
            return new ParamsOptions(this.container, "setEqualsIntrinsicEnabled", boolean.class);
        }
    }

    public class IntegrityControlParamsOptions extends FilteredParamsOptions {
        public IntegrityControlParamsOptions(Params params) {
            super(params);
        }

        public ParamsOptions createCheckBytecode() {
            return new ParamsOptions(((IntegrityControlParams) this.container).checkBytecode(), "setEnabled", boolean.class);
        }

        public ParamsOptions createCheckJar() {
            return new ParamsOptions(((IntegrityControlParams) this.container).checkJar(), "setEnabled", boolean.class);
        }

        public ParamsOptions createSealJar() {
            return new ParamsOptions(((IntegrityControlParams) this.container).sealJar(), "setEnabled", boolean.class);
        }

        public ParamsOptions createOsgiCompatibility() {
            return new ParamsOptions(((IntegrityControlParams) this.container).osgiCompatibility(), "setEnabled", boolean.class);
        }
    }

    public class PathsOptions {
        protected ProtectionElementParams container;

        public PathsOptions(ProtectionElementParams container) {
            this.container = container;
        }

        public ParamsOptions createPath() {
            return new ParamsOptions(this.container, "addPath", String.class);
        }
    }

    public class ProtectionElementOptions {
        protected ProtectionElementParams container;

        public ProtectionElementOptions(ProtectionElementParams container) {
            this.container = container;
        }

        public void setTarget(String target) {
            this.container.setTarget(target);
        }

        public ParamsOptions createTarget() {
            return new ParamsOptions(this.container, "setTarget", String.class);
        }

        public PathsOptions createPaths() {
            return new PathsOptions(this.container);
        }

        public StringEncryptonParamsOptions createStringEncryption() {
            return new StringEncryptonParamsOptions(this.container.getStringEncryption());
        }

        public FilteredParamsOptions createHideAccess() {
            return new FilteredParamsOptions(this.container.getHideAccess());
        }

        public FilteredParamsOptions createResourceEncryption() {
            return new FilteredParamsOptions(this.container.getResourceEncryption());
        }

        public IntegrityControlParamsOptions createIntegrityControl() {
            return new IntegrityControlParamsOptions(this.container.getIntegrityControl());
        }
    }

    public class ProtectionElementsOptions {
        protected ProtectionElementParamsContainer container;

        public ProtectionElementsOptions(ProtectionElementParamsContainer container) {
            this.container = container;
        }

        public ProtectionElementOptions createProtectionElement() {
            return new ProtectionElementOptions(this.container.createElement());
        }
    }
}
