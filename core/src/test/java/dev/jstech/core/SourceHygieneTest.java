/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * What every source file of the series keeps to, read over all of them at once: the licence header naming the mod
 * the file belongs to, English only, no em dash and no emoji, every class that declares palettes marked so its
 * palettes are loaded, and no palette id or sentence key declared twice.
 *
 * <p>These are the slips the compiler does not see and the game only shows much later, if at all, and they are the
 * easy ones to make when many files change at once.
 */
class SourceHygieneTest {

    /** Each module and the name its files' licence header gives as their owner. */
    private static final Map<String, String> OWNERS = Map.of("core", "J's Core", "computers", "J's Computers",
            "industrial", "J's Industrial", "tests", "J's Tech Series");

    /** The mods a player plays with, whose declarations reach a language file or a palette file. */
    private static final List<String> SHIPPED = List.of("core", "computers", "industrial");

    /** Written as its number, so that this file does not hold the character it looks for. */
    private static final char EM_DASH = (char) 0x2014;

    /*
     * Portuguese is told apart by the letters English never uses: a tilde on a or o, and a cedilla before a, o or
     * the tilded vowels. A word with none of them slips through, but a sentence of Portuguese rarely has none. The
     * letters are written as regular-expression hex escapes so this file holds none of them itself.
     */
    private static final Pattern PORTUGUESE = Pattern.compile(
            "[\\x{E3}\\x{F5}\\x{C3}\\x{D5}]|\\x{E7}[a\\x{E3}o\\x{F5}]");

    private static final Pattern PALETTE_DECLARED = Pattern.compile(
            "Palettes\\.declare\\(\\s*(\\w+)\\.MODID\\s*,\\s*\"([^\"]+)\"");
    private static final Pattern KEY_DECLARED = Pattern.compile("TextKey\\.of\\(\\s*\"([^\"]+)\"");

    @Test
    void everySourceStartsWithTheHeaderOfItsMod() {
        final List<String> wrong = new ArrayList<>();
        forEachSource(OWNERS.keySet(), (module, file, text) -> {
            final String header = "/*\n * SPDX-License-Identifier: LGPL-3.0-only\n *\n * Copyright (C) 2026 jvpts11\n"
                    + " *\n * This file is part of " + OWNERS.get(module) + ".\n */\n";
            if (!text.startsWith(header)) {
                wrong.add(file);
            }
        });
        assertTrue(wrong.isEmpty(), () -> "the licence header is missing or names the wrong mod in:\n"
                + String.join("\n", wrong));
    }

    @Test
    void noSourceHasAnEmDashAnEmojiOrPortuguese() {
        final List<String> wrong = new ArrayList<>();
        forEachSource(OWNERS.keySet(), (module, file, text) -> {
            final String[] lines = text.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                final String line = lines[i];
                if (line.indexOf(EM_DASH) >= 0) {
                    wrong.add(file + ":" + (i + 1) + " has an em dash");
                }
                if (hasEmoji(line)) {
                    wrong.add(file + ":" + (i + 1) + " has an emoji");
                }
                if (PORTUGUESE.matcher(line).find()) {
                    wrong.add(file + ":" + (i + 1) + " reads as Portuguese: " + line.strip());
                }
            }
        });
        assertTrue(wrong.isEmpty(), () -> String.join("\n", wrong));
    }

    @Test
    void everyClassThatDeclaresPalettesIsAPaletteHolder() {
        final List<String> wrong = new ArrayList<>();
        forEachSource(SHIPPED, (module, file, text) -> {
            if (!file.contains("/src/main/")) {
                return;
            }
            if (text.contains("Palettes.declare(") && !text.contains("@PaletteHolder")) {
                wrong.add(file);
            }
        });
        assertTrue(wrong.isEmpty(), () -> "these declare palettes nothing will load, since they are not marked"
                + " @PaletteHolder:\n" + String.join("\n", wrong));
    }

    @Test
    void noPaletteIdOrSentenceKeyIsDeclaredTwice() {
        final Map<String, List<String>> palettes = new TreeMap<>();
        final Map<String, List<String>> keys = new TreeMap<>();
        forEachSource(SHIPPED, (module, file, text) -> {
            if (!file.contains("/src/main/")) {
                return;
            }
            final Matcher palette = PALETTE_DECLARED.matcher(text);
            while (palette.find()) {
                palettes.computeIfAbsent(palette.group(1) + ":" + palette.group(2), id -> new ArrayList<>())
                        .add(file);
            }
            final Matcher key = KEY_DECLARED.matcher(text);
            while (key.find()) {
                keys.computeIfAbsent(key.group(1), id -> new ArrayList<>()).add(file);
            }
        });
        final List<String> wrong = new ArrayList<>();
        palettes.forEach((id, files) -> {
            if (files.size() > 1) {
                wrong.add("palette " + id + " in " + files);
            }
        });
        keys.forEach((key, files) -> {
            if (files.size() > 1) {
                wrong.add("key " + key + " in " + files);
            }
        });
        assertTrue(wrong.isEmpty(), () -> "declared more than once:\n" + String.join("\n", wrong));
    }

    /** A picture character: the supplementary pictographs, or the selector that asks for one. */
    private static boolean hasEmoji(final String line) {
        return line.codePoints().anyMatch(c -> (c >= 0x1F000 && c <= 0x1FAFF) || c == 0xFE0F);
    }

    private static void forEachSource(final Iterable<String> modules, final ISourceVisitor visitor) {
        final Path root = Path.of("").toAbsolutePath().getParent();
        for (final String module : modules) {
            for (final String tree : List.of("main", "test")) {
                final Path java = root.resolve(module).resolve("src").resolve(tree).resolve("java");
                if (!Files.isDirectory(java)) {
                    continue;
                }
                try (Stream<Path> walk = Files.walk(java)) {
                    for (final Path file : walk.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                        final String text = Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
                        visitor.visit(module, root.relativize(file).toString().replace('\\', '/'), text);
                    }
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    /** What is done with each source: its module, its path from the repository's root, and its text. */
    @FunctionalInterface
    private interface ISourceVisitor {

        void visit(String module, String file, String text);
    }
}
