package com.licel.stringer.config;

import java.io.File;

public class PatternMatchUtils {
    public static final String DEEP_TREE_MATCH = "**";

    public static boolean matchPath(String[] tokenizedPattern, String[] tokenizedPath, boolean caseSensitive) {
        int patIdxStart = 0;
        int patIdxEnd = tokenizedPattern.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = tokenizedPath.length - 1;

        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = tokenizedPattern[patIdxStart];
            if (patDir.equals(DEEP_TREE_MATCH)) {
                break;
            }
            if (!match(patDir, tokenizedPath[strIdxStart], caseSensitive)) {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }

        if (strIdxStart > strIdxEnd) {
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!tokenizedPattern[i].equals(DEEP_TREE_MATCH)) {
                    return false;
                }
            }
            return true;
        }
        if (patIdxStart > patIdxEnd) {
            return false;
        }

        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = tokenizedPattern[patIdxEnd];
            if (patDir.equals(DEEP_TREE_MATCH)) {
                break;
            }
            if (!match(patDir, tokenizedPath[strIdxEnd], caseSensitive)) {
                return false;
            }
            patIdxEnd--;
            strIdxEnd--;
        }

        if (strIdxStart > strIdxEnd) {
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!tokenizedPattern[i].equals(DEEP_TREE_MATCH)) {
                    return false;
                }
            }
            return true;
        }

        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (tokenizedPattern[i].equals(DEEP_TREE_MATCH)) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                patIdxStart++;
                continue;
            }

            int patLength = patIdxTmp - patIdxStart - 1;
            int strLength = strIdxEnd - strIdxStart + 1;
            int foundIdx = -1;

            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    String subPat = tokenizedPattern[patIdxStart + j + 1];
                    String subStr = tokenizedPath[strIdxStart + i + j];
                    if (!match(subPat, subStr, caseSensitive)) {
                        continue strLoop;
                    }
                }
                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }
            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (!tokenizedPattern[i].equals(DEEP_TREE_MATCH)) {
                return false;
            }
        }
        return true;
    }

    public static boolean match(String pattern, String value, boolean caseSensitive) {
        char[] patArr = pattern.toCharArray();
        char[] strArr = value.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;
        boolean containsStar = false;

        for (int i = 0; i < patArr.length; i++) {
            if (patArr[i] == '*') {
                containsStar = true;
                break;
            }
        }

        if (!containsStar) {
            if (patIdxEnd != strIdxEnd) {
                return false;
            }
            for (int i = 0; i <= patIdxEnd; i++) {
                char ch = patArr[i];
                if (ch != '?' && different(caseSensitive, ch, strArr[i])) {
                    return false;
                }
            }
            return true;
        }

        if (patIdxEnd == 0) {
            return true;
        }

        char ch;
        while ((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?' && different(caseSensitive, ch, strArr[strIdxStart])) {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }

        if (strIdxStart > strIdxEnd) {
            return allStars(patArr, patIdxStart, patIdxEnd);
        }

        while ((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?' && different(caseSensitive, ch, strArr[strIdxEnd])) {
                return false;
            }
            patIdxEnd--;
            strIdxEnd--;
        }

        if (strIdxStart > strIdxEnd) {
            return allStars(patArr, patIdxStart, patIdxEnd);
        }

        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (patArr[i] == '*') {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                patIdxStart++;
                continue;
            }

            int patLength = patIdxTmp - patIdxStart - 1;
            int strLength = strIdxEnd - strIdxStart + 1;
            int foundIdx = -1;

            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    ch = patArr[patIdxStart + j + 1];
                    if (ch != '?' && different(caseSensitive, ch, strArr[strIdxStart + i + j])) {
                        continue strLoop;
                    }
                }
                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }
            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        return allStars(patArr, patIdxStart, patIdxEnd);
    }

    private static boolean allStars(char[] chars, int start, int end) {
        for (int i = start; i <= end; i++) {
            if (chars[i] != '*') {
                return false;
            }
        }
        return true;
    }

    private static boolean different(boolean caseSensitive, char ch, char other) {
        return caseSensitive ? ch != other : Character.toUpperCase(ch) != Character.toUpperCase(other);
    }

    public static String[] tokenizePath(String path) {
        char sep = File.separatorChar;
        int start = 0;
        int len = path.length();
        int count = 0;

        for (int i = 0; i < len; i++) {
            if (path.charAt(i) == sep) {
                if (i != start) {
                    count++;
                }
                start = i + 1;
            }
        }
        if (len != start) {
            count++;
        }

        String[] tokens = new String[count];
        int index = 0;
        start = 0;
        for (int i = 0; i < len; i++) {
            if (path.charAt(i) == sep) {
                if (i != start) {
                    tokens[index++] = path.substring(start, i);
                }
                start = i + 1;
            }
        }
        if (len != start) {
            tokens[index] = path.substring(start);
        }
        return tokens;
    }
}
