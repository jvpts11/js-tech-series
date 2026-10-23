/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Core is the library every mod of the series is built on, so it never names one of them. The build already
 * refuses an import of a mod the Core does not depend on; reading the sources also catches a fully qualified name or
 * a class name in a string, which compile and then fail at run time.
 *
 * <p>Inside the Core, the platform (generic, for any technology mod) never reaches into the series' own model, so a
 * mod outside the series can build on the platform without taking the series with it. Only the places where the Core
 * puts its two layers together name both.
 */
class CoreBoundariesTest {

    private static final Path SOURCES = Path.of("src", "main", "java");
    private static final List<String> FORBIDDEN = List.of("dev.jstech.computers", "dev.jstech.industrial", "dev.jstech.tests");
    private static final Path SERIES = Path.of("dev", "jstech", "core", "jstechseries");
    /**
     * Where the Core puts its two layers together: the mod's entry points and its data generation's, and the list of
     * what it promises, which names packages of both.
     */
    private static final List<Path> JOINS = List.of(Path.of("dev", "jstech", "core", "JsCore.java"),
            Path.of("dev", "jstech", "core", "JsCoreClient.java"),
            Path.of("dev", "jstech", "core", "datagen", "JsCoreDataGenerators.java"),
            Path.of("dev", "jstech", "core", "api", "JsCoreApi.java"));
    /**
     * The adapters to other mods, which sit above both layers: each hands the scripts and the recipe viewers what the
     * whole Core has, the series' events and kinds of Operation with the platform's.
     */
    private static final Path INTEGRATIONS = Path.of("dev", "jstech", "core", "integration");

    @Test
    void sources_nameNoModBuiltOnTheCore() {
        final List<String> found = mentions(FORBIDDEN, file -> true);
        assertTrue(found.isEmpty(), () -> "the Core names a mod built on it:\n" + String.join("\n", found));
    }

    @Test
    void platform_neverReachesIntoTheSeriesModel() {
        final List<String> found = mentions(List.of("dev.jstech.core.jstechseries"), file -> {
            final Path relative = SOURCES.relativize(file);
            return !relative.startsWith(SERIES) && !relative.startsWith(INTEGRATIONS) && !JOINS.contains(relative);
        });
        assertTrue(found.isEmpty(), () -> "the platform reaches into the series' model:\n" + String.join("\n", found));
    }

    @Test
    void sources_areFoundFromTheTestDirectory() {
        assertFalse(javaFiles().isEmpty(), "the Core's sources are read from " + SOURCES.toAbsolutePath());
    }

    /** Every line of the sources {@code where} picks that contains one of {@code names}, as {@code file:line: text}. */
    private static List<String> mentions(final List<String> names, final Predicate<Path> where) {
        final List<String> found = new ArrayList<>();
        for (final Path file : javaFiles().stream().filter(where).toList()) {
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
