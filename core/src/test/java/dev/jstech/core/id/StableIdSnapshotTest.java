/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * The numbers that travel, written down, so that none of them can move without somebody saying so.
 *
 * <p>An enum that carries an {@link IStableId} has its constants written into saved worlds and sent over the
 * wire as numbers. A number given out once is part of what every world saved since then means, so it never
 * changes: change it and a world comes back holding something else than it was left with, everywhere it was
 * written, with nothing to say it happened.
 *
 * <p>What is kept is the whole of each constant as it is declared, rather than the number picked out of it.
 * That is both simpler and stronger: it needs no guess about which of a constant's parts is its number, and
 * it catches anything else about a constant changing as well.
 *
 * <p>The rules that stop a number being taken from a constant's POSITION are somewhere else, in
 * {@link EnumIdentityRulesTest}. This is the other half: those stop the numbers being derived, this stops
 * them being edited.
 */
class StableIdSnapshotTest {

    /** Where what they are now is written when they no longer match, so the change can be read and kept. */
    private static final Path WRITTEN = Path.of("build", "stable-ids.txt");

    private static final Pattern STABLE_ENUM =
            Pattern.compile("\\benum\\s+([A-Z]\\w*)\\b[^{]*\\bimplements\\b[^{]*\\bIStableId\\b[^{]*\\{");

    /** A constant of an enum: a name in capitals, then whatever it is declared with. */
    private static final Pattern CONSTANT = Pattern.compile("^\\s*([A-Z][A-Z0-9_]*)\\s*(\\(.*)?[,;]\\s*$");

    @Test
    void numbers_areTheOnesTheyHaveAlwaysBeen() throws IOException {
        final String found = String.join("\n", tables());
        final String kept = kept();

        if (!found.equals(kept)) {
            Files.createDirectories(WRITTEN.getParent());
            Files.writeString(WRITTEN, found + "\n", StandardCharsets.UTF_8);
        }

        assertEquals(kept, found, () -> "a number that travels has changed, or a constant that carries one has;"
                + " what they are now is in " + WRITTEN.toAbsolutePath());
    }

    @Test
    void everyOneOfThemIsRead() {
        final List<String> tables = tables();

        assertTrue(tables.stream().anyMatch(line -> line.startsWith("HardwareEra.HardwareEra ")),
                () -> "read " + tables.size() + " constants");
        assertTrue(tables.stream().anyMatch(line -> line.startsWith("IndustrialTier.IndustrialTier ")));
        // One declared inside the class it belongs to, so that the reading of those is covered as well.
        assertTrue(tables.stream().anyMatch(line -> line.startsWith("Halt.")));
    }

    /** One line per constant of every enum that carries a number, in a settled order. */
    private static List<String> tables() {
        final List<String> lines = new ArrayList<>();
        for (final Path file : sources()) {
            final String text = read(file);
            final String owner = file.getFileName().toString().replace(".java", "");
            final Matcher enums = STABLE_ENUM.matcher(text);
            /*
             * Every one in the file, not only the first: several of these are declared inside the class they
             * belong to, and one class can declare more than one. They are named by that class as well as by
             * themselves, since half of them are called Kind or State and there is more than one of each.
             */
            while (enums.find()) {
                final String name = owner + "." + enums.group(1);
                for (final String line : text.substring(enums.end()).split("\n")) {
                    final Matcher constant = CONSTANT.matcher(line);
                    if (constant.matches()) {
                        lines.add(name + " " + constant.group(1) + " "
                                + (constant.group(2) == null ? "()" : constant.group(2).trim()));
                    }
                    if (line.strip().endsWith(";")) {
                        break;
                    }
                }
            }
        }
        lines.sort(null);
        return lines;
    }

    private static String kept() {
        try (var stream = StableIdSnapshotTest.class.getResourceAsStream("/stable-ids.txt")) {
            return stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(final Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Every main source of the series, since the numbers are the series' and not one mod's. */
    private static List<Path> sources() {
        final Path root = Path.of("").toAbsolutePath().getParent();
        final List<Path> found = new ArrayList<>();
        try (Stream<Path> modules = Files.list(root)) {
            for (final Path module : modules.sorted().toList()) {
                final Path main = module.resolve("src").resolve("main").resolve("java");
                if (!Files.isDirectory(main)) {
                    continue;
                }
                try (Stream<Path> walk = Files.walk(main)) {
                    found.addAll(walk.filter(path -> path.toString().endsWith(".java")).sorted().toList());
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return found;
    }
}
