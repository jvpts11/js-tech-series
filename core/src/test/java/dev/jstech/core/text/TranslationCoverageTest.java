/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Every language a mod ships is the whole of its English: no sentence missing, none left over from one that was
 * taken out. And every sentence declared in code reaches its mod's English file.
 *
 * <p>The English file is generated from the code; the other languages are written by hand, and this is what stops
 * one of them falling behind. A translation that lacks a sentence shows its player English there, which is exactly
 * what translating was for, so the build does not let it happen.
 */
class TranslationCoverageTest {

    private static final String ENGLISH = "en_us.json";

    private static final Set<String> MODULES = Set.of("core", "computers", "industrial");

    /** A sentence declared as a field: the key is the first string it is made with. */
    private static final Pattern DECLARED = Pattern.compile("TextKey\\.of\\(\\s*\"([^\"]+)\"");
    private static final Pattern DECLARES = Pattern.compile("static\\s+final\\s+TextKey\\s+\\w+\\s*=");

    @Test
    void everyTranslationHasEveryEnglishSentenceAndNoOther() {
        final List<String> wrong = new ArrayList<>();
        for (final Map.Entry<String, List<Path>> mod : languagesByMod().entrySet()) {
            final Path english = mod.getValue().stream()
                    .filter(file -> file.getFileName().toString().equals(ENGLISH)).findFirst().orElse(null);
            if (english == null) {
                continue;
            }
            final Set<String> keys = keysOf(english);
            for (final Path language : mod.getValue()) {
                if (language.equals(english)) {
                    continue;
                }
                final Set<String> theirs = keysOf(language);
                final Set<String> missing = new TreeSet<>(keys);
                missing.removeAll(theirs);
                final Set<String> leftOver = new TreeSet<>(theirs);
                leftOver.removeAll(keys);
                if (!missing.isEmpty()) {
                    wrong.add(language + " is missing " + missing);
                }
                if (!leftOver.isEmpty()) {
                    wrong.add(language + " has sentences English no longer has " + leftOver);
                }
            }
        }
        assertTrue(wrong.isEmpty(), () -> String.join("\n", wrong));
    }

    @Test
    void everyDeclaredSentenceReachesItsModsEnglish() throws IOException {
        final List<String> wrong = new ArrayList<>();
        final Map<String, List<Path>> languages = languagesByMod();
        for (final Path module : modules()) {
            final Set<String> english = new TreeSet<>();
            languages.forEach((mod, files) -> files.stream()
                    .filter(file -> file.startsWith(module) && file.getFileName().toString().equals(ENGLISH))
                    .forEach(file -> english.addAll(keysOf(file))));
            for (final Path source : sources(module)) {
                final Matcher declared = DECLARED.matcher(Files.readString(source, StandardCharsets.UTF_8));
                while (declared.find()) {
                    if (!english.contains(declared.group(1))) {
                        wrong.add(declared.group(1) + " (" + source.getFileName() + ")");
                    }
                }
            }
        }
        assertTrue(wrong.isEmpty(), () -> "declared but not in the English file; is its class a @TextHolder, and"
                + " has the data been generated? " + wrong);
    }

    @Test
    void everyClassThatDeclaresSentencesIsATextHolder() throws IOException {
        final List<String> wrong = new ArrayList<>();
        for (final Path module : modules()) {
            for (final Path source : sources(module)) {
                final String text = Files.readString(source, StandardCharsets.UTF_8);
                if (DECLARES.matcher(text).find() && !text.contains("@TextHolder")) {
                    wrong.add(source.getFileName().toString());
                }
            }
        }
        assertTrue(wrong.isEmpty(), () -> "these declare sentences the generator will not find: " + wrong);
    }

    /** Every language file of every mod, by the mod's id: the generated English and the translations. */
    private static Map<String, List<Path>> languagesByMod() {
        final Map<String, List<Path>> out = new TreeMap<>();
        for (final Path module : modules()) {
            for (final String tree : List.of("generated", "main")) {
                final Path assets = module.resolve("src").resolve(tree).resolve("resources").resolve("assets");
                if (!Files.isDirectory(assets)) {
                    continue;
                }
                try (Stream<Path> mods = Files.list(assets)) {
                    for (final Path mod : mods.toList()) {
                        final Path lang = mod.resolve("lang");
                        if (!Files.isDirectory(lang)) {
                            continue;
                        }
                        try (Stream<Path> files = Files.list(lang)) {
                            out.computeIfAbsent(mod.getFileName().toString(), id -> new ArrayList<>())
                                    .addAll(files.filter(file -> file.toString().endsWith(".json")).sorted()
                                            .toList());
                        }
                    }
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return out;
    }

    /**
     * The keys of a language file, which is one flat object of strings: every string at the object's own level that
     * a colon follows is a key.
     */
    private static Set<String> keysOf(final Path file) {
        final String json;
        try {
            json = Files.readString(file, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        final Set<String> keys = new TreeSet<>();
        int depth = 0;
        int i = 0;
        while (i < json.length()) {
            final char c = json.charAt(i);
            if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
            } else if (c == '"') {
                final StringBuilder text = new StringBuilder();
                i++;
                while (i < json.length() && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\' && i + 1 < json.length()) {
                        i++;
                    }
                    text.append(json.charAt(i));
                    i++;
                }
                int after = i + 1;
                while (after < json.length() && Character.isWhitespace(json.charAt(after))) {
                    after++;
                }
                if (depth == 1 && after < json.length() && json.charAt(after) == ':') {
                    keys.add(text.toString());
                }
            }
            i++;
        }
        return keys;
    }

    /** The mods a player plays with; the test mod is for development only and declares keys only to test them. */
    private static List<Path> modules() {
        final Path root = Path.of("").toAbsolutePath().getParent();
        return MODULES.stream().sorted().map(root::resolve).filter(module -> Files.isDirectory(module.resolve("src")))
                .toList();
    }

    private static List<Path> sources(final Path module) {
        final Path main = module.resolve("src").resolve("main").resolve("java");
        if (!Files.isDirectory(main)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(main)) {
            return walk.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
