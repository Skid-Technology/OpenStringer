package com.licel.stringer.config;

import java.nio.file.PathMatcher;

public class MemberAccessFilter extends PathFilter {
    public String memberSignature;
    public String memberName;
    public String parameterPattern;

    public MemberAccessFilter(PathMatcher pathMatcher, String memberSignature, String pattern, String pathExpression, boolean exclude) {
        super(pathMatcher, pattern, pathExpression, exclude);
        this.memberSignature = memberSignature;
        if (memberSignature != null) {
            int openParen = memberSignature.indexOf('(');
            int closeParen = memberSignature.indexOf(')');
            if (openParen < 1 || memberSignature.length() < 2 || closeParen != memberSignature.length() - 1) {
                throw new RuntimeException("The value of " + memberSignature + " has incorrect format");
            }
            this.memberName = memberSignature.substring(0, openParen);
            this.parameterPattern = memberSignature.substring(openParen + 1, closeParen);
        }
    }

    @Override
    public String toString() {
        return super.toString() + (this.memberSignature != null ? "#" + this.memberSignature : "");
    }
}
