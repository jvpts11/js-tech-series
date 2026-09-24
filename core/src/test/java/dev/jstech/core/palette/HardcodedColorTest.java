/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.palette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * No colour is written into the code as a number.
 *
 * <p>A colour belongs to a palette: declared once with {@link Palettes#declare}, written to a file a resource pack can
 * replace, and read by whatever paints with it. What a palette declaration holds does not count, since that is where
 * a colour belongs. Any other ARGB literal ({@code 0xAARRGGBB}) fails the build, except in the few files listed as
 * data below, each with the number it holds and why those numbers are not a look a pack should change.
 */
class HardcodedColorTest {

    /** The mods whose screens a player sees; the test mod is for development only. */
    private static final Set<String> MODULES = Set.of("core", "computers", "industrial");

    /**
     * The files whose eight-digit numbers are data, not colours of the mod's look, and how many each holds. The count
     * is exact, so a colour added next to them is still caught.
     */
    private static final Map<String, Data> DATA = Map.of(
            "computers/src/main/java/dev/jstech/computers/os/fs/PixImage.java", new Data(16,
                    "the colours the in-game image format stores its pixels as: a file's contents, not the look"),
            "computers/src/main/java/dev/jstech/computers/program/install/voice/KernelVoices.java", new Data(2,
                    "the multipliers of a hash that picks a kernel's boot line"),
            "computers/src/main/java/dev/jstech/computers/program/install/voice/PortageVoices.java", new Data(1,
                    "the multiplier of a hash that picks a build line"));

    /** An ARGB colour as a number: eight hexadecimal digits. */
    private static final Pattern ARGB = Pattern.compile("0[xX][0-9A-Fa-f]{8}");

    @Test
    void colourLiterals_liveOnlyInPalettesAndData() {
        final Map<String, Integer> found = countsByFile();
        final List<String> wrong = new ArrayList<>();
        found.forEach((file, count) -> {
            final Data data = DATA.get(file);
            if (data == null) {
                wrong.add(file + " writes " + count + " colour(s) as numbers; declare them in a palette");
            } else if (count != data.count()) {
                wrong.add(file + " holds " + count + " numbers, where " + data.count() + " are data ("
                        + data.reason() + "); a new colour belongs in a palette, and fewer means the list is stale");
            }
        });
        DATA.forEach((file, data) -> {
            if (!found.containsKey(file)) {
                wrong.add(file + " is listed as data but holds none; take it off the list");
            }
        });
        assertTrue(wrong.isEmpty(), () -> String.join("\n", wrong));
    }

    /*
     * A scanner pointed at the wrong place reads nothing and counts none, which would pass the ratchet for the wrong
     * reason. What is checked is that every mod's sources are there to be read, not that any colours are left in
     * them: a mod whose colours have all moved to palettes counts none, which is where they are all meant to end.
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
    void theScannerCountsColoursOutsideADeclaration() {
        final String source = """
                class A {
                    static final Palette<Swatch> MINE = Palettes.declare(MOD, id("mine"),
                            new Swatch(0xFF000000, 0xFF3A6AE0));
                    void f() {
                        g.fill(0, 0, 4, 4, 0xFF1E1F23);
                        // 0xFFFFFFFF in a comment
                        final String s = "0xFFFFFFFF in a string";
                        final int mask = 0xFF;
                        final long big = 0x7FFFFFFFFFL;
                        draw(0x80FFFFFF);
                    }
                }
                """;
        assertEquals(2, count(source));
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
                    final int n = count(Files.readString(file, StandardCharsets.UTF_8));
                    if (n > 0) {
                        out.put(root.relativize(file).toString().replace('\\', '/'), n);
                    }
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return out;
    }

    private static Path sourcesOf(final Path root, final String module) {
        return root.resolve(module).resolve("src").resolve("main").resolve("java");
    }

    /**
     * The colour literals in one source outside a palette declaration, read with just enough of Java to know where
     * each one stands: comments, strings and characters are skipped, and every open call is kept on a stack with its
     * name and what it is called on.
     */
    private static int count(final String source) {
        int found = 0;
        final Deque<Boolean> declaring = new ArrayDeque<>();
        final int length = source.length();
        String lastWord = null;
        String receiver = null;
        String dotted = null;
        boolean wordLast = false;
        int i = 0;
        while (i < length) {
            final char c = source.charAt(i);
            if (c == '/' && i + 1 < length && source.charAt(i + 1) == '/') {
                while (i < length && source.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < length && source.charAt(i + 1) == '*') {
                final int end = source.indexOf("*/", i + 2);
                i = end < 0 ? length : end + 2;
                continue;
            }
            if (c == '"') {
                i = afterString(source, i);
                wordLast = false;
                continue;
            }
            if (c == '\'') {
                i = afterChar(source, i + 1);
                wordLast = false;
                continue;
            }
            if (Character.isDigit(c)) {
                final int start = i;
                while (i < length && Character.isJavaIdentifierPart(source.charAt(i))) {
                    i++;
                }
                if (ARGB.matcher(source.substring(start, i)).matches() && !declaring.contains(Boolean.TRUE)) {
                    found++;
                }
                wordLast = false;
                continue;
            }
            if (Character.isJavaIdentifierStart(c)) {
                final int start = i;
                while (i < length && Character.isJavaIdentifierPart(source.charAt(i))) {
                    i++;
                }
                receiver = dotted;
                dotted = null;
                lastWord = source.substring(start, i);
                wordLast = true;
                continue;
            }
            if (c == '.') {
                dotted = wordLast ? lastWord : null;
            } else if (c == '(') {
                declaring.push(wordLast && "declare".equals(lastWord) && "Palettes".equals(receiver));
            } else if (c == ')') {
                if (!declaring.isEmpty()) {
                    declaring.pop();
                }
            } else if (!Character.isWhitespace(c)) {
                dotted = null;
            }
            if (!Character.isWhitespace(c)) {
                wordLast = false;
            }
            i++;
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

    /** How many numbers a data file holds, and why they are data. */
    private record Data(int count, String reason) {
    }
}
