/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.optional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The check that keeps an optional mod from becoming a required one by accident, run from a mod's own unit tests over
 * its sources:
 *
 * <pre>{@code
 * void optionalMods_areReachedOnlyThroughTheirGuards() {   // a JUnit test
 *     assertEquals(List.of(), OptionalDependencyCheck.of(List.of("mezz.jei.", "dev.emi."))
 *             .violations(List.of(Path.of("src", "main", "java"))));
 * }
 * }</pre>
 *
 * <p>Two rules, read from the imports: a class of an optional mod is named only by a source under an
 * {@code integration} package; and a class under one that the rest of the mod imports, the guard's way in, names none,
 * so the classes that do load only after the guard found the mod.
 */
@ApiStatus.Experimental
public final class OptionalDependencyCheck {

    private final List<String> optional;

    private static final Pattern IMPORT = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+)\\s*;",
            Pattern.MULTILINE);
    private static final String INTEGRATION = "integration";

    private OptionalDependencyCheck(final List<String> optional) {
        this.optional = List.copyOf(optional);
    }

    /** The check of the optional mods whose classes start with one of {@code packagePrefixes}, as "mezz.jei.". */
    public static OptionalDependencyCheck of(final List<String> packagePrefixes) {
        return new OptionalDependencyCheck(packagePrefixes);
    }

    /** What breaks the rules among the sources under {@code roots}, a line each; none when they keep them. */
    public List<String> violations(final Collection<Path> roots) {
        final Map<String, String> sources = new LinkedHashMap<>();
        final Map<String, String> byClass = new LinkedHashMap<>();
        for (final Path root : roots) {
            read(root, sources, byClass);
        }
        final List<String> found = new ArrayList<>();
        for (final Map.Entry<String, String> source : sources.entrySet()) {
            if (underIntegration(source.getKey())) {
                continue;
            }
            for (final String imported : imports(source.getValue())) {
                if (isOptional(imported)) {
                    found.add(source.getKey() + " names " + imported + " outside an integration package");
                    continue;
                }
                final String reached = sourceOf(imported, byClass);
                if (reached == null || !underIntegration(reached.replace('.', '/'))) {
                    continue;
                }
                for (final String named : imports(byClass.get(reached))) {
                    if (isOptional(named)) {
                        found.add(reached + ", reached from " + source.getKey() + ", names " + named
                                + " where its guard has not looked yet");
                    }
                }
            }
        }
        return found;
    }

    private boolean isOptional(final String imported) {
        for (final String prefix : optional) {
            if (imported.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /*
     * The class among the sources whose file holds {@code imported}: the class itself, or the one a static member or
     * a nested class is imported from, its last names taken off until a source's is left; null for none.
     */
    private static @Nullable String sourceOf(final String imported, final Map<String, String> byClass) {
        String name = imported;
        while (!byClass.containsKey(name)) {
            final int dot = name.lastIndexOf('.');
            if (dot < 0) {
                return null;
            }
            name = name.substring(0, dot);
        }
        return name;
    }

    private static boolean underIntegration(final String path) {
        return path.contains("/" + INTEGRATION + "/");
    }

    private static List<String> imports(final String source) {
        final List<String> out = new ArrayList<>();
        final Matcher matcher = IMPORT.matcher(source);
        while (matcher.find()) {
            out.add(matcher.group(1));
        }
        return out;
    }

    /* Every source under root, keyed by its path from the root with forward slashes, and by its class's name. */
    private static void read(final Path root, final Map<String, String> sources, final Map<String, String> byClass) {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (final Path file : walk.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                final String relative = root.relativize(file).toString().replace('\\', '/');
                final String text = Files.readString(file, StandardCharsets.UTF_8);
                sources.put("/" + relative, text);
                byClass.put(relative.substring(0, relative.length() - ".java".length()).replace('/', '.'), text);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
