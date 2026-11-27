package com.licel.stringer;

import com.licel.stringer.config.MemberAccessFilter;
import com.licel.stringer.config.PathFilter;
import com.licel.stringer.util.StringerUtils;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

public class HideAccessParams extends FilteredParams {
    public static final String ANNOTATION = "Lcom/stringer/annotations/HideAccess;";

    public HideAccessParams() {
        this.name = "hideAccess";
    }

    private boolean isSameClassElementName(MemberAccessFilter filter, String owner, String memberName, String descriptor) {
        String dotOwner = owner.replace('/', '.');
        if (this.map != null && this.map.containsKey(dotOwner)) {
            this.map.getProperty(dotOwner).replace('.', '/');
        }

        String argDescriptor = descriptor.substring(0, descriptor.indexOf(')') + 1);
        String expectedArgs = StringerUtils.toJvmArgumentDescriptor(filter.parameterPattern, this.map);
        if (!expectedArgs.equals(argDescriptor)) {
            return false;
        }

        String resolvedName = memberName;
        if (this.map != null) {
            String key = dotOwner + ':' + memberName + StringerUtils.toJvmArgumentDescriptor(filter.parameterPattern, null);
            resolvedName = this.map.getProperty(key, memberName);
        }

        return resolvedName.equals(filter.memberName);
    }

    public boolean matchClassElement(String owner, String memberName, String descriptor) {
        if (!this.filtered) {
            return false;
        }
        if (this.filters.isEmpty()) {
            return true;
        }

        boolean defaultResult = false;
        for (int i = this.filters.size() - 1; i >= 0; i--) {
            MemberAccessFilter filter = (MemberAccessFilter) this.filters.get(i);
            if (filter.pathMatcher != null) {
                boolean matched = filter.pathMatcher.matches(Paths.get(this.resolveClassName(owner)))
                        && (filter.memberSignature == null || this.isSameClassElementName(filter, owner, memberName, descriptor));
                if (filter.exclude) {
                    defaultResult = true;
                    if (matched) {
                        return false;
                    }
                }
                if (matched) {
                    return true;
                }
            } else if (filter.pathExpression != null
                    && this.resolveClassName(owner).matches(this.wildcardToRegex(filter.pathExpression))) {
                return true;
            }
        }

        return defaultResult;
    }

    @Override
    protected PathFilter makeFilter(String filterPattern) {
        String rawPattern = filterPattern;
        String memberSignature = null;
        boolean exclude = false;
        int hashIndex = filterPattern.indexOf('#');
        if (hashIndex > 0) {
            filterPattern = filterPattern.substring(0, hashIndex);
            memberSignature = rawPattern.substring(hashIndex + 1);
        }

        PathMatcher pathMatcher = null;
        String syntaxPrefix = "";
        boolean isRegex = filterPattern.startsWith("regex:");
        boolean isGlob = filterPattern.startsWith("glob:");
        if (isRegex) {
            syntaxPrefix = "regex:";
        }
        if (isGlob) {
            syntaxPrefix = "glob:";
        }

        String expression = filterPattern;
        if (isRegex || isGlob) {
            int colonIndex = expression.indexOf(':');
            if (colonIndex + 1 >= expression.length()) {
                throw new RuntimeException("Invalid filter: " + expression);
            }
            expression = filterPattern.substring(colonIndex + 1);
            exclude = expression.charAt(0) == '!';
            if (exclude) {
                expression = expression.substring(1);
                filterPattern = syntaxPrefix.concat(expression);
            }
            pathMatcher = FileSystems.getDefault().getPathMatcher(filterPattern);
        }

        return new MemberAccessFilter(pathMatcher, memberSignature, syntaxPrefix, expression, exclude);
    }

    @Override
    public boolean match(String target, boolean resolveProguardName) {
        return this.matchClassElement(target, "", "()");
    }
}
