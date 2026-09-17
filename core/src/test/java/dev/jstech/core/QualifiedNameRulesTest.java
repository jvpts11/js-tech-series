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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * A type is imported and written by its simple name; it is never spelled out in the middle of a line.
 *
 * <p>A qualified name in the body of a method makes the line unreadable and hides what the file depends on,
 * which is what the imports at the top are for. There is one reason to write one anyway: the simple name
 * means something else in that file. Where two types of the same name meet, both stay qualified, so that no
 * bare name in the file is quietly one of the two.
 *
 * <p>This reads the main sources of every module in the series, with comments and string literals blanked
 * out, and allows a qualified name only where the simple name is genuinely taken: by a type the file
 * declares, by a different type it imports, by a type beside it in its own package, by one java.lang brings
 * in, or by a type nested in another that the file also names. Anything else is a name that should have been
 * an import.
 */
class QualifiedNameRulesTest {

    /** A qualified name in code: packages in lower case, then the first part that starts with a capital. */
    private static final Pattern QUALIFIED =
            Pattern.compile("\\b((?:[a-z][A-Za-z0-9_]*\\.){2,})([A-Z][A-Za-z0-9_]*)\\b");

    private static final Pattern NOT_CODE = Pattern.compile("^\\s*(import|package)\\b|^\\s*\\*|^\\s*//|^\\s*/\\*");

    private static final Pattern IMPORTED = Pattern.compile("\\s*import\\s+([A-Za-z0-9_.]+);");

    /** What the documentation points a reader at: a link, or the type an exception tag names. */
    private static final Pattern LINKED =
            Pattern.compile("(?:\\{@link(?:plain)?|@throws|@see)\\s+([^}\\s]+)");

    private static final Pattern DECLARED =
            Pattern.compile("\\b(?:class|interface|enum|record)\\s+([A-Z][A-Za-z0-9_]*)");

    /** What java.lang brings in without being asked, so a file writing one of these out means another type. */
    private static final Set<String> JAVA_LANG = Set.of(
            "Object", "String", "Integer", "Long", "Double", "Float", "Short", "Byte", "Character", "Boolean",
            "Number", "Math", "System", "Thread", "Runnable", "Process", "ProcessBuilder", "Class", "Enum",
            "Record", "Iterable", "Comparable", "CharSequence", "StringBuilder", "Exception", "RuntimeException",
            "Error", "Throwable", "IllegalStateException", "IllegalArgumentException", "NullPointerException",
            "UnsupportedOperationException", "ClassCastException", "ArithmeticException", "InterruptedException",
            "Void", "AutoCloseable", "Cloneable", "StackTraceElement", "ThreadLocal");

    @Test
    void sources_writeNoTypeNameThatCouldHaveBeenAnImport() {
        final Map<String, String> sources = mainSources();
        final Map<String, Set<String>> beside = typesByPackage(sources);
        final List<String> wrong = new ArrayList<>();
        for (final Map.Entry<String, String> source : sources.entrySet()) {
            wrong.addAll(wrongIn(source.getKey(), source.getValue(), beside));
        }

        assertEquals(List.of(), wrong, () -> String.join("\n", wrong));
    }

    /*
     * The same rule inside the documentation. A link is an import in disguise: javadoc resolves it against
     * the file's imports, so writing the path out is the same omission as writing it out in code, and it
     * renders as the whole path where the simple name would have read. The one exception the rule itself
     * gives still applies, since a link to one of two types with the same name has to say which.
     */
    @Test
    void javadoc_linksNoTypeByAPathWhereAnImportWouldDo() {
        final Map<String, String> sources = mainSources();
        final Map<String, Set<String>> beside = typesByPackage(sources);
        final List<String> wrong = new ArrayList<>();
        for (final Map.Entry<String, String> source : sources.entrySet()) {
            wrong.addAll(wrongLinksIn(source.getKey(), source.getValue(), beside));
        }

        assertEquals(List.of(), wrong, () -> String.join("\n", wrong));
    }

    @Test
    void sources_areReadFromEveryModuleOfTheSeries() {
        final Set<String> modules = new HashSet<>();
        for (final String file : mainSources().keySet()) {
            modules.add(Path.of(file).getName(0).toString());
        }

        assertTrue(modules.containsAll(List.of("core", "computers", "industrial")),
                () -> "the sources read come from " + modules);
    }

    private static List<String> wrongIn(final String file, final String text,
                                        final Map<String, Set<String>> beside) {
        final String code = code(text);
        final List<String> wrong = new ArrayList<>();
        final Matcher found = QUALIFIED.matcher(code);
        while (found.find()) {
            if (!isTaken(found.group(2), found.group(1), text, code, beside)) {
                wrong.add(file + ": " + found.group(1) + found.group(2)
                        + " should be imported and written as " + found.group(2));
            }
        }
        return wrong;
    }

    private static List<String> wrongLinksIn(final String file, final String text,
                                             final Map<String, Set<String>> beside) {
        final String code = code(text);
        final List<String> wrong = new ArrayList<>();
        final Matcher found = LINKED.matcher(text);
        while (found.find()) {
            final Matcher name = QUALIFIED.matcher(found.group(1));
            if (name.lookingAt() && !isTaken(name.group(2), name.group(1), text, code, beside)) {
                wrong.add(file + ": the documentation links " + name.group(1) + name.group(2)
                        + ", which should be imported and linked as " + name.group(2));
            }
        }
        return wrong;
    }

    /** Whether the simple name means something else in this file, which is the one reason to write a path. */
    private static boolean isTaken(final String simple, final String where, final String text, final String code,
                                   final Map<String, Set<String>> beside) {
        final Map<String, String> imported = new LinkedHashMap<>();
        for (final String line : text.split("\n")) {
            final Matcher match = IMPORTED.matcher(line);
            if (match.lookingAt() && !line.contains("import static ")) {
                final String name = match.group(1);
                imported.put(name.substring(name.lastIndexOf('.') + 1), name);
            }
        }
        final Set<String> declared = new HashSet<>();
        final Matcher declares = DECLARED.matcher(code);
        while (declares.find()) {
            declared.add(declares.group(1));
        }
        final String own = packageOf(text);
        return declared.contains(simple)
                || imported.containsKey(simple) && !imported.get(simple).equals(where + simple)
                || beside.getOrDefault(own, Set.of()).contains(simple) && !where.equals(own + ".")
                || JAVA_LANG.contains(simple) && !"java.lang.".equals(where)
                || Pattern.compile("\\b[A-Z][A-Za-z0-9_]*\\." + simple + "\\b").matcher(code).find();
    }

    /** The text with everything that is not code blanked out, so nothing in a comment or a string counts. */
    private static String code(final String text) {
        final StringBuilder out = new StringBuilder();
        boolean inBlock = false;
        for (final String line : text.split("\n")) {
            if (inBlock) {
                inBlock = !line.contains("*/");
                out.append('\n');
                continue;
            }
            if (line.stripLeading().startsWith("/*") && !line.contains("*/")) {
                inBlock = true;
                out.append('\n');
                continue;
            }
            out.append(NOT_CODE.matcher(line).lookingAt() ? "" : masked(line)).append('\n');
        }
        return out.toString();
    }

    private static String masked(final String line) {
        final StringBuilder out = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (quote == 0 && c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                break;
            }
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                    continue;
                }
                if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String packageOf(final String text) {
        for (final String line : text.split("\n")) {
            if (line.startsWith("package ")) {
                return line.substring("package ".length(), line.indexOf(';'));
            }
        }
        return "";
    }

    private static Map<String, Set<String>> typesByPackage(final Map<String, String> sources) {
        final Map<String, Set<String>> found = new LinkedHashMap<>();
        for (final Map.Entry<String, String> source : sources.entrySet()) {
            final String name = Path.of(source.getKey()).getFileName().toString();
            found.computeIfAbsent(packageOf(source.getValue()), any -> new HashSet<>())
                    .add(name.substring(0, name.length() - ".java".length()));
        }
        return found;
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
