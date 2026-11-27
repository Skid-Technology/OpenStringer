package com.licel.stringer.config;

import java.nio.file.PathMatcher;

public class PathFilter {
    public PathMatcher pathMatcher;
    public boolean exclude;
    public String pattern;
    public String pathExpression;

    public PathFilter(PathMatcher pathMatcher, String pattern, String pathExpression, boolean exclude) {
        this.pathMatcher = pathMatcher;
        this.pattern = pattern;
        this.pathExpression = pathExpression;
        this.exclude = exclude;
    }

    @Override
    public String toString() {
        return this.pattern + (this.exclude ? "!" : "") + this.pathExpression;
    }
}
