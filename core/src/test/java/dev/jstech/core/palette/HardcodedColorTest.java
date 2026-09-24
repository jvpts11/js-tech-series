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
import static org.junit.jupiter.api.Assertions.fail;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Colours written into the code as numbers, counted file by file, so that there can only ever be fewer of them.
 *
 * <p>A colour belongs to a palette: declared once with {@link Palettes#declare}, written to a file a resource pack can
 * replace, and read by whatever paints with it. While the old ones move there, this holds the line: the count of
 * ARGB literals ({@code 0xAARRGGBB}) in each file is kept, a file may never have more than it had, and a file that is
 * new starts at none. What a palette declaration holds does not count, since that is where a colour belongs. When a
 * file has fewer, the new count is written down, so the number only ever goes one way.
 */
class HardcodedColorTest {

    /** Where the counts as they are now are written when they are not the ones kept. */
    private static final Path WRITTEN = Path.of("build", "hardcoded-colors.txt");

    /** The mods whose screens a player sees; the test mod is for development only. */
    private static final Set<String> MODULES = Set.of("core", "computers", "industrial");

    /** An ARGB colour as a number: eight hexadecimal digits. */
    private static final Pattern ARGB = Pattern.compile("0[xX][0-9A-Fa-f]{8}");

    @Test
    void colourLiterals_onlyEverGoDown() throws IOException {
        final Map<String, Integer> found = countsByFile();
        final Map<String, Integer> kept = kept();
        final List<String> grown = new ArrayList<>();
        boolean changed = !kept.keySet().equals(found.keySet());
        for (final Map.Entry<String, Integer> file : found.entrySet()) {
            final int before = kept.getOrDefault(file.getKey(), 0);
            if (file.getValue() > before) {
                grown.add(file.getKey() + ": " + before + " -> " + file.getValue());
            }
            changed |= file.getValue() != before;
        }
        if (changed) {
            Files.createDirectories(WRITTEN.getParent());
            Files.writeString(WRITTEN, render(found), StandardCharsets.UTF_8);
        }
        if (!grown.isEmpty()) {
            fail("colours were written into the code as numbers; declare them in a palette instead:\n"
                    + String.join("\n", grown));
        }
        if (changed) {
            fail("there are fewer colours in the code than were counted, which is the way it should go: record it"
                    + " by copying " + WRITTEN.toAbsolutePath() + " over src/test/resources/hardcoded-colors.txt");
        }
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

    private static String render(final Map<String, Integer> counts) {
        final StringBuilder out = new StringBuilder();
        counts.forEach((file, n) -> out.append(file).append(' ').append(n).append('\n'));
        return out.toString();
    }

    private static Map<String, Integer> kept() throws IOException {
        final Map<String, Integer> out = new TreeMap<>();
        try (var stream = HardcodedColorTest.class.getResourceAsStream("/hardcoded-colors.txt")) {
            if (stream == null) {
                return out;
            }
            final Matcher row = Pattern.compile("^(\\S+) (\\d+)$", Pattern.MULTILINE)
                    .matcher(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            while (row.find()) {
                out.put(row.group(1), Integer.parseInt(row.group(2)));
            }
        }
        return out;
    }
}
