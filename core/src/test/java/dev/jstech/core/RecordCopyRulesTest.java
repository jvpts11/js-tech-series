/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * A record that leaves the place it was built takes a copy of the list, set, map or array it was handed.
 *
 * <p>A record is supposed to be a value: what it says is what it is, and it does not change afterwards. A
 * record that keeps the very list it was handed is only as unchanging as whoever handed it over. The one that
 * built it usually still has it, and usually goes on adding to it, so the record quietly changes underneath
 * whoever is reading it. That is worse than it sounds where these go, because they cross threads: a payload
 * built from a machine's own live list is written to the network while the machine goes on working on it.
 *
 * <p>The other half is what a record hands out. A copy taken on the way in is one that cannot be reached from
 * outside, and the copy is unmodifiable, so somebody who takes it and adds to it fails where they wrote it
 * rather than somewhere else much later.
 *
 * <p>Which records this is about is the whole question, and the answer is the ones that go somewhere: a
 * record written to a packet, a record written to a save or a datapack, and a record a mod built on this one
 * can hold. Those outlive the method that built them and are read by somebody who never saw it. A record that
 * never leaves the method that made it cannot be aliased by anybody, and copying in one is not free: the
 * taskbar builds one every frame out of arrays it owns, and a copy there would be allocation at sixty frames
 * a second bought for nothing. Copying where it matters costs nothing at all, because copying a list that
 * cannot change hands back the same list.
 */
class RecordCopyRulesTest {

    /** A record declaration and everything up to the brace or the implements that closes its components. */
    private static final Pattern RECORD =
            Pattern.compile("record\\s+([A-Z][A-Za-z0-9_]*)\\s*\\(([^)]*)\\)", Pattern.DOTALL);

    /** A component whose value can be changed by whoever handed it over. */
    private static final Pattern SHARED =
            Pattern.compile("\\b(?:List|Set|Map|Collection|SortedMap|SortedSet|NavigableMap)\\s*<.*>\\s+"
                    + "([a-z][A-Za-z0-9_]*)\\s*$|\\[\\]\\s+([a-z][A-Za-z0-9_]*)\\s*$", Pattern.DOTALL);

    /** What counts as taking a copy, on either a collection or an array. */
    private static final Pattern COPIED =
            Pattern.compile("copyOf\\s*\\(|\\.toList\\s*\\(\\)|\\.clone\\s*\\(\\)|Collections\\.unmodifiable");

    /** A call that builds something out of what was handed over, which is not the thing handed over. */
    private static final Pattern REBUILT = Pattern.compile("^\\s*[A-Za-z_][A-Za-z0-9_.]*\\s*\\(");

    @Test
    void records_copyEveryCollectionTheyAreHanded() {
        final List<String> wrong = new ArrayList<>();
        for (final Map.Entry<String, String> source : mainSources().entrySet()) {
            wrong.addAll(wrongIn(source.getKey(), source.getValue()));
        }

        assertEquals(List.of(), wrong, () -> wrong.size() + " to fix:\n" + String.join("\n", wrong));
    }

    @Test
    void records_areReadFromEveryModuleOfTheSeries() {
        final List<String> modules = new ArrayList<>();
        for (final String file : mainSources().keySet()) {
            final String module = Path.of(file).getName(0).toString();
            if (!modules.contains(module)) {
                modules.add(module);
            }
        }

        assertTrue(modules.containsAll(List.of("core", "computers", "industrial")),
                () -> "the sources read come from " + modules);
    }

    private static List<String> wrongIn(final String file, final String text) {
        final List<String> wrong = new ArrayList<>();
        final Matcher records = RECORD.matcher(text);
        while (records.find()) {
            final String name = records.group(1);
            final String declaration = text.substring(records.end(), declarationEnd(text, records.end()));
            if (!travels(declaration)) {
                continue;
            }
            final String body = bodyAfter(text, records.end(), name);
            for (final String component : shared(records.group(2))) {
                if (!copies(body, component)) {
                    wrong.add(file + ": " + name + " keeps the " + component
                            + " it was handed instead of a copy of it");
                }
            }
        }
        return wrong;
    }

    /**
     * Whether this record goes anywhere: to a packet, to a save or a datapack, or into a mod built on this one.
     *
     * <p>Read off the record itself rather than from a list kept here, so a new one of any of the three is
     * covered by being what it is.
     */
    private static boolean travels(final String declaration) {
        return declaration.contains("CustomPacketPayload")
                || declaration.contains("Codec<")
                || declaration.contains("StreamCodec<")
                || declaration.contains("MapCodec<");
    }

    /** Where the record's own declaration ends, which is its closing brace or the semicolon of an empty one. */
    private static int declarationEnd(final String text, final int from) {
        final int brace = text.indexOf('{', from);
        if (brace < 0) {
            return text.length();
        }
        int depth = 1;
        int i = brace + 1;
        while (i < text.length() && depth > 0) {
            final char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
            i++;
        }
        return i;
    }

    /** The names of the components somebody else could still change after handing them over. */
    private static List<String> shared(final String components) {
        final List<String> found = new ArrayList<>();
        for (final String component : splitComponents(components)) {
            final Matcher match = SHARED.matcher(component.strip());
            if (match.find()) {
                found.add(match.group(1) != null ? match.group(1) : match.group(2));
            }
        }
        return found;
    }

    /*
     * Split on the commas between components, which are the ones outside any angle brackets: a
     * Map<StorageKey, Long> has a comma of its own, and splitting on that would read as two components.
     */
    private static List<String> splitComponents(final String components) {
        final List<String> out = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < components.length(); i++) {
            final char c = components.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (c == ',' && depth == 0) {
                out.add(components.substring(start, i));
                start = i + 1;
            }
        }
        out.add(components.substring(start));
        return out;
    }

    /** Everything from the record's components to the end of its own compact constructor, if it has one. */
    private static String bodyAfter(final String text, final int from, final String name) {
        final Matcher compact =
                Pattern.compile("\\b" + name + "\\s*\\{").matcher(text);
        if (!compact.find(from)) {
            return "";
        }
        int depth = 1;
        int i = compact.end();
        while (i < text.length() && depth > 0) {
            final char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
            i++;
        }
        return text.substring(compact.end(), Math.min(i, text.length()));
    }

    /**
     * Whether that component is assigned a copy of itself somewhere in the compact constructor.
     *
     * <p>A call that takes the component and gives something back counts as well as the plain copy: a record
     * that rebuilds what it was handed into a shape of its own, padding a grid out to nine cells or trimming
     * every name in a list, is holding what it built rather than what it was given, which is the whole point.
     */
    private static boolean copies(final String body, final String component) {
        final Matcher assigned =
                Pattern.compile("\\b" + component + "\\s*=([^;]*);", Pattern.DOTALL).matcher(body);
        while (assigned.find()) {
            final String value = assigned.group(1);
            if (COPIED.matcher(value).find() || REBUILT.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }

    /** The main sources of every module of the series, which is where the rule holds; tests are their own. */
    private static Map<String, String> mainSources() {
        final Path root = Path.of("").toAbsolutePath().getParent();
        final Map<String, String> texts = new LinkedHashMap<>();
        try (Stream<Path> modules = Files.list(root)) {
            for (final Path module : modules.sorted().toList()) {
                if ("tests".equals(module.getFileName().toString())) {
                    continue;
                }
                final Path main = module.resolve("src").resolve("main").resolve("java");
                if (!Files.isDirectory(main)) {
                    continue;
                }
                try (Stream<Path> walk = Files.walk(main)) {
                    for (final Path file : walk.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                        texts.put(root.relativize(file).toString(), Files.readString(file, StandardCharsets.UTF_8));
                    }
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return texts;
    }
}
