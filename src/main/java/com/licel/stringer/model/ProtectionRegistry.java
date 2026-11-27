package com.licel.stringer.model;

import com.licel.stringer.Worker;
import java.util.HashMap;

public class ProtectionRegistry {
    public HashMap<String, PackageProtectionInfo> packages;
    public HashMap<String, ClassInfo> classes;
    public boolean javaFxApplication = false;
    public int classesWithMethodRefs = 0;
    public int classesWithoutMethodRefs = 0;
    public boolean usesResourceBundle = false;
    public boolean usesResourceStreams = false;
    public boolean usesGetResourceUrl = false;
    public boolean hasStringEncryptionAnnotations = false;
    public boolean hasLegacyAnnotations = false;
    public boolean hasHideAccessTargets = false;
    public int otherResourceCount = 0;
    public int propertyBundleCount = 0;
    public boolean containsSignature = false;

    public ProtectionRegistry() {
        this.packages = new HashMap<>();
        this.classes = new HashMap<>();
    }

    public void registerClass(ClassInfo classInfo) {
        if (!classInfo.isInterface) {
            if (!classInfo.referencedMethods.isEmpty()) {
                this.classesWithMethodRefs++;
            } else {
                this.classesWithoutMethodRefs++;
            }
        }
        if (classInfo.usesResourceBundle) {
            this.usesResourceBundle = true;
        }
        if (classInfo.usesResourceStreams) {
            this.usesResourceStreams = true;
        }
        if (classInfo.usesGetResourceUrl) {
            this.usesGetResourceUrl = true;
        }
        if (classInfo.hasHideAccessTargets) {
            this.hasHideAccessTargets = true;
        }
        if (!classInfo.isInterface) {
            if (!classInfo.referencedMethods.isEmpty()) {
                this.classesWithMethodRefs++;
            } else {
                this.classesWithoutMethodRefs++;
            }
        }

        this.classes.put(classInfo.name, classInfo);
        String packagePath = classInfo.name.substring(0, classInfo.name.lastIndexOf('/') + 1);
        this.getOrCreatePackage(packagePath).addClass(classInfo);
    }

    public void registerResourceBundle(ResourceBundleInfo bundleInfo) {
        if (bundleInfo.resourceBundle != null) {
            this.propertyBundleCount++;
        } else {
            this.otherResourceCount++;
        }
        String packagePath = bundleInfo.name.substring(0, bundleInfo.name.lastIndexOf('/') + 1);
        this.getOrCreatePackage(packagePath).addResourceBundle(bundleInfo);
    }

    public PackageProtectionInfo getOrCreatePackage(String packagePath) {
        if (this.packages.containsKey(packagePath)) {
            return this.packages.get(packagePath);
        }
        PackageProtectionInfo info = new PackageProtectionInfo(packagePath);
        this.packages.put(packagePath, info);
        return info;
    }

    public PackageProtectionInfo selectPreferredRuntimePackage() {
        PackageProtectionInfo selected = null;
        for (PackageProtectionInfo candidate : this.packages.values()) {
            int depth = candidate.getPackagePath().split("/").length;
            if (depth >= 1
                    && !candidate.getClasses().isEmpty()
                    && !candidate.getClasses().containsKey(candidate.getPackagePath() + "Handler")) {
                if (selected == null) {
                    selected = candidate;
                } else {
                    int nonGenCount = candidate.countNonGeneratedClasses();
                    if (nonGenCount > 0 && candidate.computeWeight() > selected.computeWeight() + Worker.get().secureRandom.nextInt(nonGenCount)) {
                        selected = candidate;
                    }
                }
            }
        }
        return selected;
    }
}
