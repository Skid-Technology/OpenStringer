package com.licel.stringer.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ClassInfo {
    public String name;
    public String superName;
    public HashMap<String, Object> stringFieldInitialValues;
    public HashMap<String, Object> securedStringFields;
    public HashSet<String> excludedStrings;
    public HashSet<String> stringLiterals;
    public List<ClassMemberInfo> declaredMethods;
    public List<ClassMemberInfo> declaredFields;
    public List<ClassMemberInfo> inheritedMethods;
    public List<ClassMemberInfo> inheritedFields;
    public boolean isInterface;
    public boolean hasNativeMethods = false;
    public boolean hasStaticInitializer = false;
    public List<String> referencedTypes;
    public boolean isEnum = false;
    public boolean hasLegacySecuredAnnotation = false;
    public boolean isSynthetic = false;
    public boolean annotatedSecured = false;
    public boolean hasStringEncryptionAnnotation = false;
    public boolean hasSecuredMethod = false;
    public boolean hasSecuredField = false;
    public boolean hasInsecureMethod = false;
    public boolean hasInsecureAnnotation = false;
    public boolean hasHideAccessTargets = false;
    public boolean unusedFlag = false;
    public boolean hasCorruptedFrames = false;
    public boolean usesResourceBundle = false;
    public boolean usesResourceStreams = false;
    public boolean usesGetResourceUrl = false;
    public int classFileVersion = 49;
    public List<ClassMemberInfo> referencedMethods;
    public boolean isGeneratedClass = false;
    public boolean hideAccessEnabled = false;
    public boolean stringEncryptionEnabled = false;
    public boolean moveStringFields = false;
    public boolean canHostDecoder = true;
    public boolean annotatedInsecure = false;
    public boolean inInjectList = false;
    public HashMap<String, Object> extraMetadata = new HashMap<>();
    public boolean isDecoderHost = false;
    public ClassInfo parentClassInfo;
    public int reservedIndex = 0;
    public HashMap<String, Object> extraAttributes;
    public HashMap<String, Boolean> hideAccessTrimFlags = new HashMap<>();
    public HashMap<String, Boolean> hideAccessMethodFlags = new HashMap<>();
    public int nextMemberReferenceIndex;
    public HashMap<String, MemberInfo> hiddenMembers;
    public int nextClassReferenceIndex = 0;
    public Map<String, Integer> hiddenClassIndices;
    public boolean hasExplicitStringEncryptionAnnotation = false;
    public boolean isModuleInfo = false;

    public ClassInfo(String name) {
        this.name = name;
        this.stringFieldInitialValues = new HashMap<>();
        this.securedStringFields = new HashMap<>();
        this.stringLiterals = new HashSet<>();
        this.excludedStrings = new HashSet<>();
        this.declaredMethods = new ArrayList<>();
        this.declaredFields = new ArrayList<>();
        this.inheritedMethods = new ArrayList<>();
        this.inheritedFields = new ArrayList<>();
        this.referencedTypes = new ArrayList<>();
        this.referencedMethods = new ArrayList<>();
        this.superName = "java/lang/Object";
        this.extraAttributes = new HashMap<>();
    }
}
