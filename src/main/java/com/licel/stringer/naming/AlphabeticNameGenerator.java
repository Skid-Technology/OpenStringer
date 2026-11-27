package com.licel.stringer.naming;

import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.model.ClassMemberInfo;
import java.security.SecureRandom;
import java.util.ArrayList;

public class AlphabeticNameGenerator extends NameGenerator {
    private final String alphabet = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPLKJHGFDSAZXCVBNM";

    public AlphabeticNameGenerator(SecureRandom secureRandom) {
        super(secureRandom);
    }

    @Override
    public String generateMethodName(ClassInfo classInfo, String descriptor, String prefix) {
        return this.generateMethodName(classInfo, descriptor, prefix, 1);
    }

    @Override
    public String generateMethodName(ClassInfo classInfo, String descriptor, String prefix, int minLength) {
        ArrayList<ClassMemberInfo> usedMembers = new ArrayList<>();
        usedMembers.addAll(classInfo.declaredMethods);
        usedMembers.addAll(classInfo.referencedMethods);

        StringBuilder name = new StringBuilder();
        if (prefix != null) {
            name.append(prefix.replace("<", "$").replace(">", "$")).append("_");
        }

        while (name.length() < minLength) {
            name.append(this.alphabet.charAt(this.random.nextInt(this.alphabet.length())));
        }

        while (containsMember(usedMembers, name.toString(), descriptor)) {
            name.append(this.alphabet.charAt(this.random.nextInt(this.alphabet.length())));
        }

        return name.toString();
    }

    @Override
    public String generateFieldName(ClassInfo classInfo, String descriptor) {
        StringBuilder name = new StringBuilder();
        name.append(this.alphabet.charAt(this.random.nextInt(this.alphabet.length())));

        while (containsMember(classInfo.declaredFields, name.toString(), null)) {
            name.append(this.alphabet.charAt(this.random.nextInt(this.alphabet.length())));
        }

        return name.toString();
    }

    @Override
    public void setMethodNameCandidates(ArrayList<ClassMemberInfo> methodNameCandidates) {
    }

    @Override
    public void setFieldNameCandidates(ArrayList<ClassMemberInfo> fieldNameCandidates) {
    }
}
