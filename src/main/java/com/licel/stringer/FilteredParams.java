package com.licel.stringer;

import com.licel.stringer.config.PathFilter;
import com.licel.stringer.model.ClassInfo;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class FilteredParams extends Params {
    public static final String ANNOTATIONS_MODE = "annotations";
    public static final String FILTERED_MODE = "filter";
    public static final String OFF_MODE = "off";

    protected boolean annotations = false;
    protected boolean filtered = true;
    protected List<PathFilter> filters = new ArrayList<>();
    protected Properties map;

    public void setMode(String mode) throws ParseException {
        if (mode != null) {
            if (ANNOTATIONS_MODE.equals(mode)) {
                this.annotations = true;
                this.filtered = false;
                this.enabled = true;
            } else if (FILTERED_MODE.equals(mode)) {
                this.annotations = false;
                this.filtered = true;
                this.enabled = true;
            } else if (OFF_MODE.equals(mode)) {
                this.annotations = false;
                this.filtered = false;
                this.enabled = false;
            } else {
                throw new ParseException("Illegal mode value: " + mode, 0);
            }
        }
    }

    public String getMode() {
        if (this.annotations) {
            return ANNOTATIONS_MODE;
        }
        return this.filtered ? FILTERED_MODE : OFF_MODE;
    }

    public void setMap(Properties map) {
        this.map = map;
    }

    public boolean isAnnotations() {
        return this.annotations;
    }

    public void setAnnotations(boolean annotations) {
        this.annotations = annotations;
        this.filtered = !annotations;
    }

    public boolean isFiltered() {
        return this.filtered;
    }

    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
        this.annotations = !filtered;
    }

    public boolean isEmpty() {
        return this.filters.isEmpty();
    }

    public List<PathFilter> getFilters() {
        return this.filters;
    }

    public void addFilter(ClassInfo classInfo) {
        this.addFilter(classInfo.name);
    }

    public void addFilter(String filterPattern) {
        this.filters.add(this.makeFilter(filterPattern));
    }

    protected PathFilter makeFilter(String filterPattern) {
        PathMatcher pathMatcher = null;
        String syntaxPrefix = "";
        boolean exclude = false;
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

        return new PathFilter(pathMatcher, syntaxPrefix, expression, exclude);
    }

    public String getFilterString(String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < this.filters.size(); i++) {
            builder.append(this.filters.get(i).toString()).append(delimiter);
        }
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) == delimiter.charAt(0)) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    public void setFilterString(String filterList, String delimiterRegex) {
        this.filters = new ArrayList<>();
        String[] entries = filterList.split(delimiterRegex);
        for (String entry : entries) {
            if (!entry.trim().isEmpty()) {
                this.addFilter(new ClassInfo(entry));
            }
        }
    }

    public boolean match(ClassInfo classInfo) {
        return this.match(classInfo.name);
    }

    public boolean match(String target) {
        return this.match(target, true);
    }

    public boolean match(String target, boolean resolveProguardName) {
        if (!this.filtered) {
            return false;
        }
        if (this.filters.isEmpty()) {
            return true;
        }

        boolean defaultResult = false;
        for (int i = this.filters.size() - 1; i >= 0; i--) {
            PathFilter filter = this.filters.get(i);
            if (filter.pathMatcher != null) {
                boolean matched = filter.pathMatcher.matches(
                        resolveProguardName ? Paths.get(this.resolveClassName(target)) : Paths.get(target));
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
                    && (resolveProguardName ? this.resolveClassName(target) : target).matches(this.wildcardToRegex(filter.pathExpression))) {
                return true;
            }
        }

        return defaultResult;
    }

    protected String resolveClassName(String className) {
        String resolved = className;
        if (this.map != null && this.map.containsKey(resolved)) {
            resolved = this.map.getProperty(resolved).replace('.', '/');
        }
        return resolved;
    }

    protected String wildcardToRegex(String wildcard) {
        StringBuilder regex = new StringBuilder(wildcard.length());
        regex.append('^');
        for (int i = 0; i < wildcard.length(); i++) {
            char ch = wildcard.charAt(i);
            switch (ch) {
                case '$':
                case '(':
                case ')':
                case '.':
                case '[':
                case '\\':
                case ']':
                case '^':
                case '{':
                case '|':
                case '}':
                    regex.append("\\").append(ch);
                    break;
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                default:
                    regex.append(ch);
            }
        }
        regex.append('$');
        return regex.toString();
    }
}
