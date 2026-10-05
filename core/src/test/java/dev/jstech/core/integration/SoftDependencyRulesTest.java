/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration;

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
 * The mods the series works with but does not need (Mekanism, JEI, EMI, FTB, ComputerCraft, Curios, Accessories,
 * Jade) must never be needed by
 * accident: a class of theirs is named only by a source under an {@code integration} package, and the class the rest
 * of a mod calls to start an integration names none of them, so the integration's classes load only after its guard
 * found the mod. The development-only test mod is not held to it.
 */
class SoftDependencyRulesTest {

    private static final List<String> OPTIONAL = List.of("mekanism.", "mezz.jei.", "dev.emi.", "dev.ftb.",
            "dev.architectury.", "dan200.computercraft.", "top.theillusivec4.curios.", "io.wispforest.",
            "snownee.jade.");
    private static final Pattern IMPORT = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+)\\s*;",
            Pattern.MULTILINE);
    private static final Pattern INTEGRATION_CLASS =
            Pattern.compile("\\b(dev\\.jstech\\.\\w+\\.integration\\.\\w+\\.\\w+)\\b");
    private static final String INTEGRATION = "/integration/";
    private static final Map<String, String> SOURCES = load();

    @Test
    void optionalMods_areNamedOnlyUnderAnIntegrationPackage() {
        final List<String> found = new ArrayList<>();
        for (final Map.Entry<String, String> source : SOURCES.entrySet()) {
            if (source.getKey().contains(INTEGRATION)) {
                continue;
            }
            for (final String imported : imports(source.getValue())) {
                if (isOptional(imported)) {
                    found.add(source.getKey() + " imports " + imported);
                }
            }
        }
        assertTrue(found.isEmpty(), () -> "optional mods named outside an integration package: " + found);
    }

    @Test
    void integrationEntries_nameNoOptionalMod() {
        final List<String> found = new ArrayList<>();
        for (final Map.Entry<String, String> source : SOURCES.entrySet()) {
            if (source.getKey().contains(INTEGRATION)) {
                continue;
            }
            for (final String imported : imports(source.getValue())) {
                final Matcher entry = INTEGRATION_CLASS.matcher(imported);
                if (!entry.matches()) {
                    continue;
                }
                final String entrySource = SOURCES.get(pathOf(imported));
                if (entrySource == null) {
                    continue;
                }
                for (final String named : imports(entrySource)) {
                    if (isOptional(named)) {
                        found.add(imported + " (called from " + source.getKey() + ") imports " + named);
                    }
                }
            }
        }
        assertTrue(found.isEmpty(), () -> "integration entries that load an optional mod's classes: " + found);
    }

    @Test
    void sources_includeTheMekanismBridgeAndItsGuard() {
        assertTrue(SOURCES.keySet().stream().anyMatch(path -> path.endsWith("MekanismChemicalBridge.java")),
                "the Mekanism bridge is read");
        assertTrue(SOURCES.keySet().stream().anyMatch(path -> path.endsWith("MekanismIntegration.java")),
                "and its guard");
    }

    private static List<String> imports(final String source) {
        final List<String> out = new ArrayList<>();
        final Matcher matcher = IMPORT.matcher(source);
        while (matcher.find()) {
            out.add(matcher.group(1));
        }
        return out;
    }

    private static boolean isOptional(final String imported) {
        for (final String prefix : OPTIONAL) {
            if (imported.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /* The path, as the sources are keyed, of the class {@code qualified} names: dev.jstech.<module>.... */
    private static String pathOf(final String qualified) {
        return qualified.split("\\.")[2] + "/src/main/java/" + qualified.replace('.', '/') + ".java";
    }

    /* Every main source of the mods, keyed by its path from the repository's root, with forward slashes. */
    private static Map<String, String> load() {
        final Path root = Path.of("").toAbsolutePath().getParent();
        final Map<String, String> texts = new LinkedHashMap<>();
        for (final String module : List.of("core", "computers", "industrial")) {
            final Path main = root.resolve(module).resolve("src").resolve("main").resolve("java");
            if (!Files.isDirectory(main)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(main)) {
                for (final Path file : walk.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                    texts.put(root.relativize(file).toString().replace('\\', '/'),
                            Files.readString(file, StandardCharsets.UTF_8));
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return texts;
    }
}
