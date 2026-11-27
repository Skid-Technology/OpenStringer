package com.licel.stringer.naming;

import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.model.ClassMemberInfo;
import java.security.SecureRandom;
import java.util.ArrayList;

public class ExistingNameGenerator extends NameGenerator {
    private ArrayList<ClassMemberInfo> methodNameCandidates = new ArrayList<>();
    private ArrayList<ClassMemberInfo> fieldNameCandidates = new ArrayList<>();

    public ExistingNameGenerator(SecureRandom secureRandom) {
        super(secureRandom);
    }

    @Override
    public String generateMethodName(ClassInfo classInfo, String descriptor, String prefix) {
        ArrayList<ClassMemberInfo> existing = new ArrayList<>();
        existing.addAll(classInfo.declaredMethods);
        existing.addAll(classInfo.inheritedMethods);
        ArrayList<ClassMemberInfo> filteredExisting = this.filterMembers(existing, "!<");
        ArrayList<ClassMemberInfo> filteredCandidates = this.filterMembers(this.methodNameCandidates, "!<");
        return this.selectAvailableName(descriptor, 5, filteredExisting, filteredCandidates);
    }

    ArrayList<ClassMemberInfo> filterMembers(ArrayList<ClassMemberInfo> members, String filterExpr) {
        String[] tokens = filterExpr.split(" ");
        ArrayList<ClassMemberInfo> result = new ArrayList<>();

        for (int i = 0; i < members.size(); i++) {
            ClassMemberInfo member = members.get(i);
            boolean matched = false;

            for (int j = 0; j < tokens.length; j++) {
                boolean negated = tokens[j].startsWith("!");
                String pattern = negated ? tokens[j].substring(1) : tokens[j];
                if (!(negated & member.getName().indexOf(pattern) < 0)) {
                    break;
                }
                matched = true;
            }

            if (matched) {
                result.add(member);
            }
        }

        return result;
    }

    @Override
    public String generateFieldName(ClassInfo classInfo, String descriptor) {
        ArrayList<ClassMemberInfo> existing = new ArrayList<>();
        existing.addAll(classInfo.declaredFields);
        existing.addAll(classInfo.inheritedFields);
        ArrayList<ClassMemberInfo> filteredExisting = this.filterMembers(existing, "!serialVersionUID");
        ArrayList<ClassMemberInfo> filteredCandidates = this.filterMembers(this.fieldNameCandidates, "!serialVersionUID");
        return this.selectAvailableName(descriptor, 5, filteredExisting, filteredCandidates);
    }

    @Override
    public void setMethodNameCandidates(ArrayList<ClassMemberInfo> methodNameCandidates) {
        this.methodNameCandidates = methodNameCandidates;
    }

    @Override
    public void setFieldNameCandidates(ArrayList<ClassMemberInfo> fieldNameCandidates) {
        this.fieldNameCandidates = fieldNameCandidates;
    }

    @Override
    public String generateMethodName(ClassInfo classInfo, String descriptor, String prefix, int minLength) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
