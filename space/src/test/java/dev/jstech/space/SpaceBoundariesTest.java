/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space;

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
 * J's Space stands on the Core alone: it names no other mod of the series and never reaches into the test mod, so it
 * is played without the others and the others without it. Reading the sources also catches a fully qualified name or
 * a class name in a string.
 */
class SpaceBoundariesTest {

    private static final Path SOURCES = Path.of("src", "main", "java");
    /* The other mods of the series, which J's Space may only meet through the Core or behind a guard. */
    private static final List<String> OTHER_MODS = List.of("dev.jstech.computers", "dev.jstech.industrial");

    @Test
    void sources_neverNameTheTestMod() {
        final List<String> found = mentions("dev.jstech.tests");
        assertTrue(found.isEmpty(), () -> "J's Space names the test mod:\n" + String.join("\n", found));
    }

    @Test
    void sources_neverNameAnotherModOfTheSeries() {
        final List<String> found = new ArrayList<>();
        for (final String mod : OTHER_MODS) {
            found.addAll(mentions(mod));
        }
        assertTrue(found.isEmpty(), () -> "J's Space names another mod of the series:\n" + String.join("\n", found));
    }

    @Test
    void sources_areFoundFromTheTestDirectory() {
        assertFalse(javaFiles().isEmpty(), "the sources are read from " + SOURCES.toAbsolutePath());
    }

    /* Every line of the sources that contains {@code name}, as {@code file:line: text}. */
    private static List<String> mentions(final String name) {
        final List<String> found = new ArrayList<>();
        for (final Path file : javaFiles()) {
            final List<String> lines = readLines(file);
            for (int i = 0; i < lines.size(); i++) {
                final String line = lines.get(i);
                if (line.contains(name)) {
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
