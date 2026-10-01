/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Text drawn raw, with the game's own {@code drawString} and its kin, can only get rarer.
 *
 * <p>Text in the series is drawn with a shadow worked out from the ground under it ({@code Draw.text}, which reads the
 * grounds painted on the frame). Lines drawn raw are older than that, and they are counted file by file in
 * {@code raw-text-calls.txt}: a file may draw fewer raw lines than its count and never more, and a file that is not on
 * the list starts at none. When a file draws fewer, its count is lowered to match, so it cannot creep back up. The
 * counts as they stand are written to {@code build/raw-text-calls.txt} on every run, to copy over the list.
 */
class RawTextCallsTest {

    /** The mods whose screens a player sees; the test mod is for development only. */
    private static final Set<String> MODULES = Set.of("core", "computers", "industrial");
    /** The one place a raw call is the point: the drawer every shadowed line goes through. */
    private static final String DRAWER = "core/src/main/java/dev/jstech/core/client/gui/component/Draw.java";
    /** The game's calls that write a line of text. */
    private static final List<String> RAW = List.of(".drawString(", ".drawCenteredString(", ".drawWordWrap(");
    private static final String LIST = "/raw-text-calls.txt";
    private static final Path WRITTEN = Path.of("build", "raw-text-calls.txt");

    @Test
    void rawTextCalls_neverGrowAndTheListFollowsThemDown() throws IOException {
        final Map<String, Integer> found = countsByFile();
        final Map<String, Integer> allowed = readList();
        writeCounts(found);
        final List<String> wrong = new ArrayList<>();
        found.forEach((file, count) -> {
            final int most = allowed.getOrDefault(file, 0);
            if (count > most) {
                wrong.add(file + " draws " + count + " raw line(s) where " + most + " are allowed; draw text with"
                        + " Draw.text, which shadows it by the ground under it");
            } else if (count < most) {
                wrong.add(file + " draws " + count + " raw line(s); lower its count in raw-text-calls.txt from "
                        + most + " to " + count + ", so it cannot creep back up");
            }
        });
        allowed.forEach((file, count) -> {
            if (!found.containsKey(file)) {
                wrong.add(file + " draws no raw line now; take it off raw-text-calls.txt");
            }
        });
        assertTrue(wrong.isEmpty(), () -> String.join("\n", wrong));
    }

    /*
     * A scanner pointed at the wrong place reads nothing and counts none, which would pass the ratchet for the wrong
     * reason, so every mod's sources must be there to be read.
     */
    @Test
    void everyModIsRead() {
        final Path root = Path.of("").toAbsolutePath().getParent();
        for (final String module : MODULES) {
            assertTrue(Files.isDirectory(sourcesOf(root, module)),
                    () -> "the sources of " + module + " were not found");
        }
    }

    @Test
    void theScannerCountsCallsOutsideCommentsAndStrings() {
        final String source = """
                class A {
                    void f(GuiGraphics g) {
                        g.drawString(font, "a", 0, 0, colour, false);
                        g.drawCenteredString(font, title, 0, 0, colour);
                        // g.drawString(font, "in a comment", 0, 0, colour, false);
                        /* g.drawWordWrap(font, text, 0, 0, 10, colour); */
                        final String s = "g.drawString(font, s, 0, 0, colour, false)";
                        Draw.text(g, font, "shadowed", 0, 0, colour);
                        g.drawWordWrap(font, text, 0, 0, 10, colour);
                    }
                }
                """;
        assertEquals(3, count(source));
    }

    private static Map<String, Integer> countsByFile() {
        final Map<String, Integer> out = new TreeMap<>();
        final Path root = Path.of("").toAbsolutePath().getParent();
        for (final String module : MODULES) {
            final Path main = sourcesOf(root, module);
            if (!Files.isDirectory(main)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(main)) {
                for (final Path file : walk.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                    final String name = root.relativize(file).toString().replace('\\', '/');
                    final int n = count(Files.readString(file, StandardCharsets.UTF_8));
                    if (n > 0 && !name.equals(DRAWER)) {
                        out.put(name, n);
                    }
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return out;
    }

    /** The list: one file a line, its count, a space and its path; blank lines and lines starting with # are notes. */
    private static Map<String, Integer> readList() throws IOException {
        final Map<String, Integer> out = new TreeMap<>();
        try (InputStream in = RawTextCallsTest.class.getResourceAsStream(LIST)) {
            assertNotNull(in, "raw-text-calls.txt is missing from the test resources");
            for (final String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                final String entry = line.strip();
                if (entry.isEmpty() || entry.startsWith("#")) {
                    continue;
                }
                final int space = entry.indexOf(' ');
                out.put(entry.substring(space + 1).strip(), Integer.parseInt(entry.substring(0, space)));
            }
        }
        return out;
    }

    private static void writeCounts(final Map<String, Integer> found) throws IOException {
        final StringBuilder text = new StringBuilder();
        found.forEach((file, count) -> text.append(count).append(' ').append(file).append('\n'));
        Files.createDirectories(WRITTEN.getParent());
        Files.writeString(WRITTEN, text.toString(), StandardCharsets.UTF_8);
    }

    private static Path sourcesOf(final Path root, final String module) {
        return root.resolve(module).resolve("src").resolve("main").resolve("java");
    }

    /** The raw text calls in one source, outside its comments, strings and characters. */
    private static int count(final String source) {
        final StringBuilder code = new StringBuilder(source.length());
        final int length = source.length();
        int i = 0;
        while (i < length) {
            final char c = source.charAt(i);
            if (c == '/' && i + 1 < length && source.charAt(i + 1) == '/') {
                while (i < length && source.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < length && source.charAt(i + 1) == '*') {
                final int end = source.indexOf("*/", i + 2);
                i = end < 0 ? length : end + 2;
            } else if (c == '"') {
                i = afterString(source, i);
                code.append(' ');
            } else if (c == '\'') {
                i = afterChar(source, i + 1);
                code.append(' ');
            } else {
                code.append(c);
                i++;
            }
        }
        final String plain = code.toString();
        int found = 0;
        for (final String call : RAW) {
            for (int at = plain.indexOf(call); at >= 0; at = plain.indexOf(call, at + call.length())) {
                found++;
            }
        }
        return found;
    }

    /** Where the string or text block that opens at {@code at} ends, just past its closing quote. */
    private static int afterString(final String source, final int at) {
        if (source.startsWith("\"\"\"", at)) {
            final int end = source.indexOf("\"\"\"", at + 3);
            return end < 0 ? source.length() : end + 3;
        }
        int i = at + 1;
        while (i < source.length() && source.charAt(i) != '"' && source.charAt(i) != '\n') {
            i += source.charAt(i) == '\\' ? 2 : 1;
        }
        return Math.min(i + 1, source.length());
    }

    private static int afterChar(final String source, final int from) {
        int i = from;
        while (i < source.length() && source.charAt(i) != '\'') {
            i += source.charAt(i) == '\\' ? 2 : 1;
        }
        return Math.min(i + 1, source.length());
    }
}
