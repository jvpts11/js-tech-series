/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * J's Industrial stands on the Core alone: its machines expose the Core's capabilities and J's Computers commands
 * them, so it never names J's Computers, and it never reaches into the test mod. The build refuses the imports;
 * reading the sources also catches a fully qualified name or a class name in a string.
 */
class IndustrialBoundariesTest {

    private static final Path SOURCES = Path.of("src", "main", "java");
    private static final List<String> FORBIDDEN = List.of("dev.jstech.computers", "dev.jstech.tests");

    @Test
    void sources_nameNeitherComputersNorTheTestMod() {
        final List<String> found = mentions(FORBIDDEN);
        assertTrue(found.isEmpty(), () -> "J's Industrial names a mod it may not depend on:\n" + String.join("\n", found));
    }

    @Test
    void sources_areFoundFromTheTestDirectory() {
        assertFalse(javaFiles().isEmpty(), "the sources are read from " + SOURCES.toAbsolutePath());
    }

    /** Every line of the sources that contains one of {@code names}, as {@code file:line: text}. */
    private static List<String> mentions(final List<String> names) {
        final List<String> found = new ArrayList<>();
        for (final Path file : javaFiles()) {
            final List<String> lines = readLines(file);
            for (int i = 0; i < lines.size(); i++) {
                final String line = lines.get(i);
                if (names.stream().anyMatch(line::contains)) {
                    found.add(SOURCES.relativize(file) + ":" + (i + 1) + ": " + line.strip());
                }
            }
        }
        return found;
    }

    private static List<Path> javaFiles() {
        if (!Files.isDirectory(SOURCES)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(SOURCES)) {
            return walk.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> readLines(final Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
