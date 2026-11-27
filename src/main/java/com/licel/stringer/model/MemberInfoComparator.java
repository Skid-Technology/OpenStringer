package com.licel.stringer.model;

import java.util.Comparator;

public class MemberInfoComparator implements Comparator<MemberInfo> {
    @Override
    public int compare(MemberInfo first, MemberInfo second) {
        return first.getReferenceIndex() - second.getReferenceIndex();
    }
}
