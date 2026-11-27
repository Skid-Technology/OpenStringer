package com.licel.stringer.naming;

import com.licel.stringer.model.ClassInfo;
import com.licel.stringer.model.ClassMemberInfo;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public abstract class NameGenerator {
    protected char[] characters = new char[]{
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'x', 'y', 'z'
    };
    protected SecureRandom random;

    public NameGenerator(SecureRandom random) {
        this.random = random;
    }

    public static boolean containsMember(List<ClassMemberInfo> members, String name, String descriptor) {
        for (int i = 0; i < members.size(); i++) {
            ClassMemberInfo member = members.get(i);
            if (member.name.equals(name)) {
                if (descriptor == null) {
                    return true;
                }
                if (descriptor.equals(member.descriptor)) {
                    return true;
                }
            }
        }
        return false;
    }

    public abstract String generateMethodName(ClassInfo classInfo, String descriptor, String prefix);

    public abstract String generateMethodName(ClassInfo classInfo, String descriptor, String prefix, int minLength);

    public abstract String generateFieldName(ClassInfo classInfo, String descriptor);

    public abstract void setMethodNameCandidates(ArrayList<ClassMemberInfo> methodNameCandidates);

    public abstract void setFieldNameCandidates(ArrayList<ClassMemberInfo> fieldNameCandidates);

    protected String selectAvailableName(
            String descriptor,
            int fallbackLength,
            ArrayList<ClassMemberInfo> existingMembers,
            ArrayList<ClassMemberInfo> candidateMembers) {
        String selected = null;
        if (!existingMembers.isEmpty()) {
            for (int i = 0; i < existingMembers.size(); i++) {
                selected = this.pickRandom(existingMembers).name;
                if (!containsMember(existingMembers, selected, descriptor)) {
                    break;
                }
                selected = null;
            }
        }

        if (selected == null && !candidateMembers.isEmpty()) {
            for (int i = 0; i < candidateMembers.size(); i++) {
                selected = this.pickRandom(candidateMembers).name;
                if (!containsMember(existingMembers, selected, descriptor)) {
                    break;
                }
                selected = null;
            }
        }

        if (selected == null) {
            selected = this.generateRandomIdentifier(fallbackLength);
            while (containsMember(existingMembers, selected, descriptor)) {
                selected = this.generateRandomIdentifier(10);
            }
        }

        return selected;
    }

    public String generateRandomIdentifier(int maxLength) {
        return this.generateRandomIdentifier(maxLength, false);
    }

    public String generateRandomIdentifier(int length, boolean exactLength) {
        StringBuilder builder = new StringBuilder();
        int targetLength = length;
        if (!exactLength) {
            targetLength = this.random.nextInt(length);
            while (targetLength == 0) {
                targetLength = this.random.nextInt(length);
            }
        }

        for (int i = 0; i < targetLength; i++) {
            builder.append(this.pickRandomChar(this.characters));
        }
        return builder.toString();
    }

    private char pickRandomChar(char[] pool) {
        if (pool.length == 1) {
            return pool[0];
        }
        int index = Math.round((pool.length - 1) * this.random.nextFloat());
        return pool[index];
    }

    public <T> T pickRandom(List<T> list) {
        if (list.size() == 1) {
            return list.get(0);
        }
        int index = Math.round((list.size() - 1) * this.random.nextFloat());
        return list.get(index);
    }

    public <T> T pickRandom(T[] array) {
        if (array.length == 1) {
            return array[0];
        }
        int index = Math.round((array.length - 1) * this.random.nextFloat());
        return array[index];
    }
}
