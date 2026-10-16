/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Every reach of the Core into the game accounted for in {@code core/docs/REACHES.md}: each mixin of its configuration
 * and each line of its access transformer listed with why it is needed and the test that fails if the game moves what
 * it reaches, and each test named there one that exists. A reach nobody wrote down is one nobody will check when the
 * game moves.
 */
class ReachesTest {

    private static final String PAGE = "core/docs/REACHES.md";
    private static final String MIXINS = "core/src/main/resources/jscore.mixins.json";
    private static final String TRANSFORMER = "core/src/main/resources/META-INF/accesstransformer.cfg";
    private static final String TESTS = "tests/src/main/java";
    private static final Pattern MIXIN_NAME = Pattern.compile("\"(\\w+Mixin)\"");
    private static final Pattern TEST_NAME = Pattern.compile("`(\\w+)\\.(\\w+)`");
    private static final Pattern MEMBER = Pattern.compile("[\\w$]+");

    @Test
    void everyMixin_isListedWithItsReasonAndTest() {
        final List<String> rows = rows();
        final List<String> missing = new ArrayList<>();
        final Matcher found = MIXIN_NAME.matcher(read(MIXINS));
        while (found.find()) {
            final String name = found.group(1);
            if (rows.stream().noneMatch(row -> firstCell(row).contains("`" + name + "`"))) {
                missing.add(name);
            }
        }
        assertTrue(missing.isEmpty(), "mixins " + PAGE + " does not list: " + missing);
    }

    @Test
    void everyAccessTransformerLine_isListedWithItsReasonAndTest() {
        final List<String> rows = rows();
        final List<String> missing = new ArrayList<>();
        for (final String line : read(TRANSFORMER).split("\n")) {
            final String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            final String[] parts = trimmed.split("\\s+");
            final String owner = parts[1].substring(parts[1].lastIndexOf('.') + 1);
            final Matcher member = MEMBER.matcher(parts[2]);
            final String reach = owner + "#" + (member.find() ? member.group() : parts[2]);
            if (rows.stream().noneMatch(row -> firstCell(row).contains("`" + reach + "`"))) {
                missing.add(reach);
            }
        }
        assertTrue(missing.isEmpty(), "access transformer lines " + PAGE + " does not list: " + missing);
    }

    @Test
    void everyTestNamed_exists() {
        final List<String> wrong = new ArrayList<>();
        for (final String row : rows()) {
            final String[] cells = row.split("\\|");
            final Matcher test = TEST_NAME.matcher(cells[cells.length - 1]);
            if (!test.find()) {
                wrong.add(firstCell(row) + ": names no test");
                continue;
            }
            final Path source = sourceOf(test.group(1));
            if (source == null) {
                wrong.add(test.group(1) + ": no such test class");
            } else if (!read(source).contains(" void " + test.group(2) + "(")) {
                wrong.add(test.group(1) + "." + test.group(2) + ": no such test");
            }
        }
        if (!wrong.isEmpty()) {
            fail("tests " + PAGE + " names wrongly: " + wrong);
        }
    }

    /* The rows of the page's table, its heading and rule left out. */
    private static List<String> rows() {
        final List<String> rows = new ArrayList<>();
        for (final String line : read(PAGE).split("\n")) {
            if (line.startsWith("| `")) {
                rows.add(line);
            }
        }
        assertTrue(!rows.isEmpty(), PAGE + " lists no reach");
        return rows;
    }

    private static String firstCell(final String row) {
        return row.split("\\|")[1];
    }

    private static Path sourceOf(final String className) {
        try (Stream<Path> files = Files.walk(root().resolve(TESTS))) {
            return files.filter(path -> path.getFileName().toString().equals(className + ".java")).findFirst()
                    .orElse(null);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(final String relative) {
        return read(root().resolve(relative));
    }

    private static String read(final Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path root() {
        return Path.of("").toAbsolutePath().getParent();
    }
}
