package com.licel.stringer;

import java.io.File;

public class ConsoleTask {
    public static void main(String[] args) {
        Config config = new Config();
        String configFilePath = null;
        boolean repairMode = false;
        int positionalIndex = 0;

        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-verbose")) {
                    config.setVerbose(true);
                } else if (args[i].equalsIgnoreCase("-checkCaller")) {
                    config.setCheckCaller(true);
                } else if (args[i].equalsIgnoreCase("-integrityProtection")) {
                    config.setIntegrityProtection(true);
                } else if (args[i].equalsIgnoreCase("-optimize")) {
                    config.setOptimize(true);
                } else if (args[i].equalsIgnoreCase("-resourceEncryption")) {
                    config.setResourceEncryption(true);
                } else if (args[i].equalsIgnoreCase("-osgiCompatibility")) {
                    config.setOsgiCompatibility(true);
                } else if (args[i].equalsIgnoreCase("-include")) {
                    if (i + 1 < args.length) {
                        config.addInclude(args[++i]);
                    }
                } else if (args[i].equalsIgnoreCase("-exclude")) {
                    if (i + 1 < args.length) {
                        config.addExclude(args[++i]);
                    }
                } else if (args[i].equalsIgnoreCase("-inject")) {
                    if (i + 1 < args.length) {
                        config.addInject(args[++i]);
                    }
                } else if (args[i].equalsIgnoreCase("-configFile")) {
                    if (i + 1 < args.length) {
                        configFilePath = args[++i];
                    }
                } else if (args[i].equalsIgnoreCase("-proguardMapFile")) {
                    if (i + 1 < args.length) {
                        config.setProguardMapFile(new File(args[++i]));
                    }
                } else if (args[i].equalsIgnoreCase("-nameGenMethod")) {
                    if (i + 1 < args.length) {
                        String method = args[++i];
                        if ("utf".equalsIgnoreCase(method) || "latin".equalsIgnoreCase(method)) {
                            config.setNameGenMethod(method);
                        }
                    }
                } else if (args[i].equalsIgnoreCase("-target")) {
                    if (i + 1 < args.length) {
                        config.setTarget(args[++i]);
                    }
                } else if (args[i].equalsIgnoreCase("-outputType")) {
                    if (i + 1 < args.length) {
                        i++;
                    }
                } else if (args[i].equalsIgnoreCase("-trialExpirationTimestamp")) {
                    if (i + 1 < args.length) {
                        config.setTrialExpirationTimestamp(args[++i]);
                    }
                } else if (args[i].equalsIgnoreCase("-trialExpirationTimestampPattern")) {
                    if (i + 1 < args.length) {
                        config.setTrialExpirationTimestampPattern(args[++i]);
                    }
                } else if (args[i].equalsIgnoreCase("-forceAndroid")) {
                    config.setForceAndroid(true);
                } else if (args[i].equalsIgnoreCase("-noCheckJar")) {
                    config.setCheckJar(false);
                } else if (args[i].equalsIgnoreCase("-equalsIntrinsicEnabled")) {
                    config.getStringEncryption().setEqualsIntrinsicEnabled(true);
                } else if (args[i].equalsIgnoreCase("-repair")) {
                    repairMode = true;
                } else if (args[i].equalsIgnoreCase("-info")) {
                    printProductInfo();
                } else {
                    positionalIndex = i;
                    break;
                }
            }
        } catch (Exception ex) {
            System.err.println("Error parsing config parameters : " + ex.getMessage());
            System.exit(-1);
        }

        if (configFilePath != null) {
            try {
                FileConfig fileConfig = new FileConfig(new File(configFilePath));
                if (config.getDestFile() != null) {
                    fileConfig.setDestFile(config.getDestFile());
                }
                if (config.getSrcFile() != null) {
                    fileConfig.setSrcFile(config.getSrcFile());
                }
                if (config.getProguardMapFile() != null) {
                    fileConfig.setProguardMapFile(config.getProguardMapFile());
                }
                config = fileConfig;
            } catch (Exception ex) {
                System.err.println("Error parsing config file : " + ex.getMessage());
                System.exit(-1);
            }
        }

        if (config.getSrcFile() == null && positionalIndex + 1 > args.length) {
            System.err.println("Missing src parameter");
            System.err.println("Usage : java -jar stringer.jar [options] src dest");
            System.exit(-1);
        } else if (positionalIndex < args.length) {
            File src = new File(args[positionalIndex]);
            if (src.isDirectory()) {
                config.setSrcDir(src);
            } else {
                config.setSrcFile(src);
            }
        }

        if (config.getDestFile() == null && positionalIndex + 2 > args.length) {
            System.err.println("Missing dest parameter");
            System.err.println("Usage : java -jar stringer.jar [options] src dest");
            System.exit(-1);
        } else if (positionalIndex + 1 < args.length) {
            config.setDestFile(new File(args[positionalIndex + 1]));
        }

        try {
            config.getListener().info("Mode: CLI");
            if (!repairMode) {
                Worker worker = new Worker(config);
                worker.stringerIt();
            } else {
                Worker.repairArchive(config.getSrcFile(), config.getDestFile());
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            System.err.println("Failed to process due to: " + ex.getMessage());
            System.exit(-1);
        }
    }

    private static void printProductInfo() {
        System.out.println(ProductInfo.getInfoText());
        System.exit(0);
    }
}
