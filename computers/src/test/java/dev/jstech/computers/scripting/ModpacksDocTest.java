/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.scripting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The page for modpack makers, {@code computers/docs/MODPACKS.md}, says only what is so: the tags it names are the
 * ones the data run writes, and all of them; the calls of {@code JsComputers} it shows are methods the binding has;
 * and every example it prints is in the test mod's own data or script, which the GameTests load and check, with the
 * namespace changed from {@code jstests} to {@code mypack}.
 */
class ModpacksDocTest {

    private static final Path ROOT = Path.of("").toAbsolutePath().getParent();
    private static final String PAGE = read(ROOT.resolve("computers/docs/MODPACKS.md"));
    private static final Path TAGS = ROOT.resolve("computers/src/generated/resources/data/jsc/tags/item");
    private static final Path BINDING = ROOT.resolve(
            "computers/src/main/java/dev/jstech/computers/scripting/JsComputersScripts.java");
    private static final Path TEST_DATA = ROOT.resolve("tests/src/main/resources/data");
    private static final Path TEST_SCRIPT = ROOT.resolve("tests/src/kubejs/server_scripts/jstests_recipes.js");
    private static final Pattern TAG = Pattern.compile("#jsc:([a-z0-9_/]+)");
    private static final Pattern CALL = Pattern.compile("JsComputers\\.(\\w+)\\(");
    private static final Pattern BLOCK = Pattern.compile("```(json|js)\\r?\\n(.*?)```", Pattern.DOTALL);

    @Test
    void tags_areExactlyTheOnesTheDataRunWrites() {
        final Set<String> written = new TreeSet<>();
        try (Stream<Path> walk = Files.walk(TAGS)) {
            walk.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                final String relative = TAGS.relativize(path).toString().replace('\\', '/');
                written.add(relative.substring(0, relative.length() - ".json".length()));
            });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        final Set<String> named = new TreeSet<>();
        final Matcher tag = TAG.matcher(PAGE);
        while (tag.find()) {
            named.add(tag.group(1));
        }
        assertTrue(written.size() >= 15, () -> "the data run writes J's Computers' item tags; read " + written);
        assertEquals(written, named, "the page names every item tag J's Computers has, and no other");
    }

    @Test
    void calls_areMethodsTheBindingHas() {
        final String binding = read(BINDING);
        final List<String> missing = new ArrayList<>();
        final Matcher call = CALL.matcher(PAGE);
        int calls = 0;
        while (call.find()) {
            calls++;
            final Pattern method = Pattern.compile("public static [\\w<>]+ " + call.group(1) + "\\(");
            if (!method.matcher(binding).find()) {
                missing.add(call.group(1));
            }
        }
        assertTrue(calls >= 6, "the page shows the binding's calls; found " + calls);
        assertTrue(missing.isEmpty(), () -> "the page shows calls the binding does not have: " + missing);
    }

    @Test
    void jsonExamples_areInTheTestModsData() {
        final List<JsonElement> data = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(TEST_DATA)) {
            walk.filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> data.add(JsonParser.parseString(read(path))));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        final List<String> examples = blocks("json");
        assertTrue(examples.size() >= 5, "the page prints the datapack examples; found " + examples.size());
        for (final String example : examples) {
            final JsonElement shown = JsonParser.parseString(asTested(example));
            assertTrue(data.stream().anyMatch(file -> holds(file, shown)),
                    () -> "a datapack example is not in the test mod's data:\n" + example);
        }
    }

    @Test
    void scriptExamples_areTheTestScript() {
        final String script = squeezed(read(TEST_SCRIPT));
        final List<String> examples = blocks("js");
        assertTrue(examples.size() >= 2, "the page prints the script examples; found " + examples.size());
        for (final String example : examples) {
            assertTrue(script.contains(squeezed(asTested(example))),
                    () -> "a script example is not in the test script:\n" + example);
        }
    }

    private static List<String> blocks(final String language) {
        final List<String> found = new ArrayList<>();
        final Matcher block = BLOCK.matcher(PAGE);
        while (block.find()) {
            if (block.group(1).equals(language)) {
                found.add(block.group(2));
            }
        }
        return found;
    }

    /* The page writes a modpack's namespace where the test mod has its own. */
    private static String asTested(final String example) {
        return example.replace("mypack", "jstests");
    }

    /*
     * Whether a file of data holds all the example shows: each key with what it holds, and each item of a list. A
     * file may hold more, as the test mod's list of loot modifiers names the Core's tests' modifiers beside the page's.
     */
    private static boolean holds(final JsonElement file, final JsonElement example) {
        if (example.isJsonObject()) {
            if (!file.isJsonObject()) {
                return false;
            }
            for (final Map.Entry<String, JsonElement> entry : example.getAsJsonObject().entrySet()) {
                final JsonElement there = file.getAsJsonObject().get(entry.getKey());
                if (there == null || !holds(there, entry.getValue())) {
                    return false;
                }
            }
            return true;
        }
        if (example.isJsonArray()) {
            if (!file.isJsonArray()) {
                return false;
            }
            for (final JsonElement item : example.getAsJsonArray()) {
                boolean found = false;
                for (final JsonElement candidate : file.getAsJsonArray()) {
                    found |= holds(candidate, item);
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }
        return example.equals(file);
    }

    /* Whitespace says nothing in JSON and only lays a script out, so the comparison leaves it out. */
    private static String squeezed(final String text) {
        return text.replaceAll("\\s+", "");
    }

    private static String read(final Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
