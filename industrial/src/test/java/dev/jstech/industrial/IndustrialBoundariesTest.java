/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial;

import dev.jstech.core.optional.OptionalDependencyCheck;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * J's Industrial stands on the Core alone: its machines expose the Core's capabilities and J's Computers commands
 * them, so it names J's Computers only in its integration with it, which starts behind a guard when J's Computers is
 * installed, and it never reaches into the test mod. Reading the sources also catches a fully qualified name or a
 * class name in a string.
 */
class IndustrialBoundariesTest {

    private static final Path SOURCES = Path.of("src", "main", "java");
    private static final String COMPUTERS = "dev.jstech.computers";
    /* The integration with J's Computers, the one place that may name it. */
    private static final Path INTEGRATION = Path.of("dev", "jstech", "industrial", "integration", "computers");

    @Test
    void sources_neverNameTheTestMod() {
        final List<String> found = mentions("dev.jstech.tests", false);
        assertTrue(found.isEmpty(), () -> "J's Industrial names the test mod:\n" + String.join("\n", found));
    }

    @Test
    void sources_nameComputersOnlyInTheirIntegration() {
        final List<String> found = mentions(COMPUTERS, true);
        assertTrue(found.isEmpty(), () -> "J's Industrial names J's Computers outside its integration:\n"
                + String.join("\n", found));
    }

    @Test
    void computers_isReachedOnlyThroughItsGuard() {
        assertEquals(List.of(), OptionalDependencyCheck.of(List.of(COMPUTERS + ".")).violations(List.of(SOURCES)));
    }

    @Test
    void sources_areFoundFromTheTestDirectory() {
        assertFalse(javaFiles().isEmpty(), "the sources are read from " + SOURCES.toAbsolutePath());
    }

    /**
     * Every line of the sources that contains {@code name}, as {@code file:line: text}; outside the integration with
     * J's Computers only, when {@code outsideIntegration}.
     */
    private static List<String> mentions(final String name, final boolean outsideIntegration) {
        final List<String> found = new ArrayList<>();
        for (final Path file : javaFiles()) {
            if (outsideIntegration && SOURCES.relativize(file).startsWith(INTEGRATION)) {
                continue;
            }
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
