package com.licel.stringer.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public final class MatchingFileCollector extends SimpleFileVisitor<Path> {
    private final Path rootPath;
    private final PathMatcher pathMatcher;
    private final List<Path> matchedFiles;

    public MatchingFileCollector(Path rootPath, PathMatcher pathMatcher) {
        this(rootPath, pathMatcher, new ArrayList<>());
    }

    public MatchingFileCollector(Path rootPath, PathMatcher pathMatcher, List<Path> matchedFiles) {
        this.rootPath = rootPath;
        this.pathMatcher = pathMatcher;
        this.matchedFiles = matchedFiles;
    }

    public List<Path> getMatchedFiles() {
        return this.matchedFiles;
    }

    private void matchPath(Path path) {
        Path relativePath = this.rootPath.relativize(path);
        if (relativePath != null && this.pathMatcher.matches(relativePath)) {
            this.matchedFiles.add(path);
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        this.matchPath(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        this.matchPath(dir);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
    }
}
