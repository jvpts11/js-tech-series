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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * What the series promises a mod built on it, and nothing more.
 *
 * <p>Everything reachable from an {@code api} package is the promise; everything else in the series is its
 * own business and changes without a word. The danger is a promise made by accident: an API method that
 * hands back or takes something from the inside drags that thing into the promise, and the next release
 * that changes it breaks a mod without anybody having decided to.
 *
 * <p>So the types an API signature is allowed to name are counted, and they are these: the API's own, the
 * few of ours that are the API even though they live where they belong, the language's, and the game's. A
 * new name in a signature fails here until somebody says which of those it is, which is the point: adding
 * to the promise is a decision, not a side effect.
 *
 * <p>The list below is the same one {@code docs/API.md} names, and the two are meant to be read together.
 */
class ApiSurfaceTest {

    /**
     * Ours that are part of the promise though they do not live in an api package.
     *
     * <p>Each is something an addon has to hold to do what the API is for: a registry it adds to, or a
     * description of a thing it is adding.
     */
    private static final Set<String> PROMISED = Set.of(
            // the registries an addon adds to
            "LanguageRegistry", "OperationTypeRegistry",
            // what it adds to them
            "IProgrammingLanguage", "OperationType", "IOperationArgs",
            // the computers: what a machine is, and what can be installed on one
            "ArchitectureSpec", "KernelDef", "OsDef", "ProgramSpec", "DesktopEnvironmentDef");

    /** What java.lang brings in, which a signature may name with no import at all. */
    private static final Set<String> JAVA_LANG = Set.of("String", "Object", "Integer", "Long", "Double", "Float",
            "Boolean", "Character", "Number", "CharSequence", "Iterable", "Comparable", "Class", "Enum", "Record",
            "Runnable", "Exception", "RuntimeException", "Throwable", "Void");

    /** Whose names an API signature may always say: the language's own and the game's. */
    private static final List<String> ELSEWHERE = List.of("java.", "javax.", "net.minecraft.", "net.neoforged.",
            "com.mojang.", "org.slf4j.", "org.jetbrains.");

    /** A member anything outside the mod can reach, with what it gives back and what it takes. */
    private static final Pattern PUBLIC_MEMBER = Pattern.compile("^\\s{4}public\\s+"
            + "(?:static\\s+|final\\s+|abstract\\s+)*([A-Za-z0-9_.<>\\[\\], ?]+?)\\s+(\\w+)\\s*\\(([^)]*)\\)");

    /** A public field or constant, which is reachable the same way a method is. */
    private static final Pattern PUBLIC_FIELD = Pattern.compile("^\\s{4}public\\s+"
            + "(?:static\\s+)?(?:final\\s+)?([A-Za-z0-9_.<>\\[\\], ?]+?)\\s+(\\w+)\\s*[=;]");

    @Test
    void api_namesNothingThatIsNotPartOfThePromise() {
        final List<Path> sources = apiSources();
        final Set<String> api = new LinkedHashSet<>();
        for (final Path file : sources) {
            api.add(file.getFileName().toString().replace(".java", ""));
        }
        final List<String> leaked = new ArrayList<>();
        for (final Path file : sources) {
            leaked.addAll(leakedIn(file, api));
        }

        assertEquals(List.of(), leaked, () -> String.join("\n", leaked));
    }

    @Test
    void api_isWhereItIsSaidToBe() {
        final List<Path> found = apiSources();

        assertTrue(found.stream().anyMatch(path -> path.endsWith("JsCoreApi.java")), () -> "found " + found);
        assertTrue(found.stream().anyMatch(path -> path.endsWith("CoreRegisterEvent.java")));
        assertTrue(found.stream().anyMatch(path -> path.endsWith("JsComputersApi.java")));
        assertTrue(found.stream().anyMatch(path -> path.endsWith("ComputersRegisterEvent.java")));
    }

    private static List<String> leakedIn(final Path file, final Set<String> api) {
        final String text = read(file);
        final List<String> leaked = new ArrayList<>();
        for (final String line : text.split("\n")) {
            final Matcher method = PUBLIC_MEMBER.matcher(line);
            final Matcher field = PUBLIC_FIELD.matcher(line);
            final Set<String> named = new LinkedHashSet<>();
            if (method.find()) {
                named.addAll(typesIn(method.group(1)));
                for (final String parameter : method.group(3).split(",")) {
                    named.addAll(typesIn(parameter.replace("final ", "").trim()));
                }
            } else if (field.find()) {
                named.addAll(typesIn(field.group(1)));
            } else {
                continue;
            }
            for (final String type : named) {
                if (!allowed(type, text, api)) {
                    leaked.add(file.getFileName() + ": " + type + " is named by the API but is not part of it");
                }
            }
        }
        return leaked;
    }

    /** The type names written in a piece of a signature, with the generics and the arrays taken apart. */
    private static Set<String> typesIn(final String written) {
        final Set<String> found = new LinkedHashSet<>();
        final Matcher names = Pattern.compile("\\b([A-Z][A-Za-z0-9_]*)\\b").matcher(written);
        while (names.find()) {
            found.add(names.group(1));
        }
        return found;
    }

    /**
     * Whether an API signature may name that type.
     *
     * <p>What decides it is where THAT type comes from, not what else the file happens to import. Getting
     * that wrong is how this test came to pass a deliberate leak the first time it was tried.
     */
    private static boolean allowed(final String type, final String text, final Set<String> api) {
        if (PROMISED.contains(type) || api.contains(type) || JAVA_LANG.contains(type)) {
            return true;
        }
        final String imported = importOf(type, text);
        if (imported != null) {
            for (final String outside : ELSEWHERE) {
                if (imported.startsWith(outside)) {
                    return true;
                }
            }
            // Imported from inside the series and not spoken for: a promise nobody decided to make.
            return false;
        }
        // Named without an import: something this very file declares, or something java.lang brings in.
        return text.contains("class " + type) || text.contains("interface " + type)
                || text.contains("record " + type) || text.contains("enum " + type);
    }

    /** The whole name the file imports that type as, or null when it imports no such type. */
    private static String importOf(final String type, final String text) {
        for (final String line : text.split("\n")) {
            final Matcher match = Pattern.compile("^\\s*import\\s+([A-Za-z0-9_.]+\\." + type + ");").matcher(line);
            if (match.find()) {
                return match.group(1);
            }
        }
        return null;
    }

    private static String read(final Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Every source in an api package of the series. */
    private static List<Path> apiSources() {
        final Path root = Path.of("").toAbsolutePath().getParent();
        final List<Path> found = new ArrayList<>();
        try (Stream<Path> modules = Files.list(root)) {
            for (final Path module : modules.sorted().toList()) {
                final Path main = module.resolve("src").resolve("main").resolve("java");
                if (!Files.isDirectory(main)) {
                    continue;
                }
                try (Stream<Path> walk = Files.walk(main)) {
                    found.addAll(walk.filter(path -> path.toString().endsWith(".java"))
                            .filter(path -> path.getParent().getFileName().toString().equals("api"))
                            .sorted().toList());
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return found;
    }
}
