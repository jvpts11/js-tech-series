/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.id;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An enum constant's position in its declaration and its Java name are not an identity: reorder or rename the
 * constants and every number or name already written somewhere comes back as another constant, or as none. These
 * read the main sources of every module in the series and hold the rule with no exceptions: no {@code ordinal()}
 * (called or referenced), no constant picked out of {@code values()} by a number, no {@code valueOf} on one of the
 * series' enums, and no enum's {@code name()} (called or referenced) handed to a {@code put} or {@code write}. A constant that has to travel declares an
 * {@link IStableId id} or an {@link IStableName name}; code that only compares constants uses {@code compareTo},
 * an {@code EnumMap} or a {@code switch}.
 *
 * <p>The sources are read as text with comments and literals blanked out. Which receiver of {@code name()} is an
 * enum is worked out from declarations in the sources (a variable's declared type, a record component, a method's
 * return type); a receiver the text cannot settle is let through, so the rules never flag code they cannot read.
 */
class EnumIdentityRulesTest {

    private static final Pattern ENUM = Pattern.compile("\\benum\\s+([A-Z]\\w*)");
    private static final Pattern TYPE_DECLARATION =
            Pattern.compile("\\b(?:class|record|enum|interface)\\s+([A-Z]\\w*)");
    private static final Pattern RECORD_HEADER = Pattern.compile("\\brecord\\s+([A-Z]\\w*)\\s*(?:<[^>]*>)?\\s*\\(");
    private static final Pattern NO_ARGUMENT_METHOD = Pattern.compile(
            "\\b([A-Z]\\w*)(?:<[^;{}()]*>)?(?:\\[\\s*])*\\s+(\\w+)\\s*\\(\\s*\\)\\s*(?:throws\\s[^;{]*)?[{;]");
    private static final Pattern ORDINAL = Pattern.compile("\\bordinal\\s*\\(\\s*\\)|::\\s*ordinal\\b");
    private static final Pattern INDEXED_VALUES = Pattern.compile("\\bvalues\\s*\\(\\s*\\)\\s*\\[");
    private static final Pattern KEPT_VALUES = Pattern.compile("\\bvalues\\s*\\(\\s*\\)\\s*;");
    private static final Pattern ENUM_VALUE_OF = Pattern.compile("\\bEnum\\s*\\.\\s*valueOf\\s*\\(");
    private static final Pattern QUALIFIED_VALUE_OF =
            Pattern.compile("\\b([A-Z]\\w*)\\s*(?:\\.\\s*valueOf\\s*\\(|::\\s*valueOf\\b)");
    private static final Pattern VALUE_OF = Pattern.compile("\\bvalueOf\\s*\\(");
    private static final Pattern NAME_REFERENCE = Pattern.compile("\\b([A-Z]\\w*)\\s*::\\s*name\\b");
    private static final Pattern NAME_CALL = Pattern.compile("\\.\\s*name\\s*\\(\\s*\\)");

    /** Code that breaks each rule once per form, with how many findings each rule owes it. */
    private static final String FORBIDDEN = """
            package sample;

            enum Mode { ON, OFF }

            record Setting(Mode mode, String label) {
            }

            final class Uses {
                int number(final Mode mode) {
                    return mode.ordinal();
                }

                java.util.Comparator<Mode> byNumber() {
                    return java.util.Comparator.comparingInt(Mode::ordinal);
                }

                Mode fromNumber(final int number) {
                    return Mode.values()[number];
                }

                Mode[] kept() {
                    final Mode[] all = Mode.values();
                    return all;
                }

                Mode parsed(final String text) {
                    return Mode.valueOf(text);
                }

                Mode generic(final String text) {
                    return Enum.valueOf(Mode.class, text);
                }

                void saved(final Tag tag, final Mode mode, final Setting setting) {
                    tag.putString("Mode", mode.name());
                    tag.putString("Other", setting.mode().name());
                    tag.putString("Constant", Mode.ON.name());
                }

                void sent(final Writer writer) {
                    writer.write(Mode::name);
                }
            }
            """;

    /** Code that looks close to the forbidden forms but is none of them. */
    private static final String ALLOWED = """
            package sample;

            enum Mode { ON, OFF }

            record Setting(Mode mode, String name) {
            }

            final class Uses {
                // mode.ordinal() and Mode.valueOf("ON") in a comment are not code.
                boolean later(final Mode a, final Mode b) {
                    return a.compareTo(b) > 0;
                }

                int count() {
                    int count = 0;
                    for (final Mode mode : Mode.values()) {
                        count++;
                    }
                    return count + Mode.values().length;
                }

                void saved(final Tag tag, final Setting setting, final Mode mode) {
                    tag.putString("Name", setting.name());
                    tag.putString("Text", String.valueOf(42));
                    tag.putString("Quoted", "Mode.values()[0] and mode.ordinal()");
                    log("mode " + mode.name());
                    log(String.join(" / ", java.util.List.of(Mode.ON, mode).stream().map(Mode::name).toList()));
                    tag.putString("Sorted", java.util.List.of(mode).stream().sorted().toList().toString());
                }

                void log(final String text) {
                }
            }
            """;

    @Test
    void mainSources_includeEveryModuleOfTheSeries() {
        final Set<String> modules = new HashSet<>();
        for (final Source file : MainSources.ALL.files()) {
            modules.add(Path.of(file.label()).getName(0).toString());
        }
        assertTrue(modules.containsAll(List.of("core", "computers", "industrial", "tests")),
                () -> "the sources read come from " + modules);
        assertTrue(MainSources.ALL.enums().contains("HardwareEra"), "the series' enums are found");
    }

    @Test
    void ordinal_isNeverCalled() {
        assertNone("calls to ordinal()", ordinalCalls(MainSources.ALL));
    }

    @Test
    void values_isNeverIndexedOrKeptAsAnArray() {
        assertNone("values() arrays indexed or kept", valuesArrays(MainSources.ALL));
    }

    @Test
    void valueOf_isNeverCalledOnAnEnum() {
        assertNone("valueOf calls on enums", valueOfCalls(MainSources.ALL));
    }

    @Test
    void enumNames_areNeverPutOrWritten() {
        assertNone("enum names put or written", enumNamesWritten(MainSources.ALL));
    }

    @Test
    void rules_findEveryFormTheyForbid() {
        final Sources sample = sourcesOf(Map.of("sample/Uses.java", FORBIDDEN));
        assertEquals(2, ordinalCalls(sample).size(), () -> "ordinal: " + ordinalCalls(sample));
        assertEquals(2, valuesArrays(sample).size(), () -> "values: " + valuesArrays(sample));
        assertEquals(2, valueOfCalls(sample).size(), () -> "valueOf: " + valueOfCalls(sample));
        assertEquals(4, enumNamesWritten(sample).size(), () -> "names: " + enumNamesWritten(sample));
    }

    @Test
    void rules_letThroughTheFormsTheyAllow() {
        final Sources sample = sourcesOf(Map.of("sample/Uses.java", ALLOWED));
        assertNone("calls to ordinal()", ordinalCalls(sample));
        assertNone("values() arrays indexed or kept", valuesArrays(sample));
        assertNone("valueOf calls on enums", valueOfCalls(sample));
        assertNone("enum names put or written", enumNamesWritten(sample));
    }

    // the rules

    private static List<String> ordinalCalls(final Sources sources) {
        final List<String> found = new ArrayList<>();
        for (final Source file : sources.files()) {
            final Matcher call = ORDINAL.matcher(file.code());
            while (call.find()) {
                found.add(where(file, call.start()));
            }
        }
        return found;
    }

    private static List<String> valuesArrays(final Sources sources) {
        final List<String> found = new ArrayList<>();
        for (final Source file : sources.files()) {
            final String code = file.code();
            final Matcher indexed = INDEXED_VALUES.matcher(code);
            while (indexed.find()) {
                found.add(where(file, indexed.start()));
            }
            final Matcher kept = KEPT_VALUES.matcher(code);
            while (kept.find()) {
                final int before = previousCodeChar(code, kept.start());
                // Unqualified, the call is an enum's own values(); qualified, it counts only on one of the series' enums.
                if (before < 0 || code.charAt(before) != '.'
                        || sources.enums().contains(identifierEndingAt(code, previousCodeChar(code, before)))) {
                    found.add(where(file, kept.start()));
                }
            }
        }
        return found;
    }

    private static List<String> valueOfCalls(final Sources sources) {
        final List<String> found = new ArrayList<>();
        for (final Source file : sources.files()) {
            final String code = file.code();
            final Matcher generic = ENUM_VALUE_OF.matcher(code);
            while (generic.find()) {
                found.add(where(file, generic.start()));
            }
            final Matcher qualified = QUALIFIED_VALUE_OF.matcher(code);
            while (qualified.find()) {
                if (sources.enums().contains(qualified.group(1))) {
                    found.add(where(file, qualified.start()));
                }
            }
            final Matcher bare = VALUE_OF.matcher(code);
            while (bare.find()) {
                final int before = previousCodeChar(code, bare.start());
                if (before < 0 || code.charAt(before) != '.' && code.charAt(before) != ':') {
                    found.add(where(file, bare.start()));
                }
            }
        }
        return found;
    }

    private static List<String> enumNamesWritten(final Sources sources) {
        final List<String> found = new ArrayList<>();
        for (final Source file : sources.files()) {
            final String code = file.code();
            final Matcher reference = NAME_REFERENCE.matcher(code);
            while (reference.find()) {
                if (writes(enclosingCall(code, reference.start())) && sources.enums().contains(reference.group(1))) {
                    found.add(where(file, reference.start()) + " (" + reference.group(1) + ")");
                }
            }
            final Matcher call = NAME_CALL.matcher(code);
            while (call.find()) {
                if (!writes(enclosingCall(code, call.start()))) {
                    continue;
                }
                final String type = typeEndingAt(code, previousCodeChar(code, call.start()), sources);
                if (type != null && sources.enums().contains(type)) {
                    found.add(where(file, call.start()) + " (" + type + ")");
                }
            }
        }
        return found;
    }

    // reading code as text

    /** Whether a call named {@code method} puts or writes what it is given: a tag, a buffer, a stream. */
    private static boolean writes(final String method) {
        return method != null && (method.startsWith("put") || method.startsWith("write"));
    }

    /** The type of the expression that ends at {@code end}, or null when the declarations in the text do not settle it. */
    private static String typeEndingAt(final String code, final int end, final Sources sources) {
        if (end < 0) {
            return null;
        }
        if (code.charAt(end) == ')') {
            final int open = matchingOpen(code, end);
            final int nameEnd = open < 0 ? -1 : previousCodeChar(code, open);
            final String member = nameEnd < 0 ? "" : identifierEndingAt(code, nameEnd);
            if (member.isEmpty()) {
                return null;
            }
            final int before = previousCodeChar(code, nameEnd - member.length() + 1);
            final String owner = before >= 0 && code.charAt(before) == '.'
                    ? typeEndingAt(code, previousCodeChar(code, before), sources) : null;
            return sources.returnType(owner, member);
        }
        final String identifier = identifierEndingAt(code, end);
        if (identifier.isEmpty()) {
            return null;
        }
        final int start = end - identifier.length() + 1;
        final int before = previousCodeChar(code, start);
        if (before >= 0 && code.charAt(before) == '.') {
            final String qualifier = identifierEndingAt(code, previousCodeChar(code, before));
            if (sources.enums().contains(qualifier)) {
                return qualifier; // a constant, as in Mode.ON
            }
            if (!qualifier.equals("this")) {
                return null;
            }
        }
        return declaredType(code, identifier, start);
    }

    /** The declared type of {@code identifier} nearest before {@code use}, or the first one after it. */
    private static String declaredType(final String code, final String identifier, final int use) {
        final Matcher declaration = Pattern.compile("\\b([A-Z]\\w*)(?:<[^;{}()]*>)?(?:\\[\\s*])*\\s+"
                + Pattern.quote(identifier) + "\\s*[=;,):]").matcher(code);
        String nearest = null;
        while (declaration.find()) {
            if (declaration.start() < use || nearest == null) {
                nearest = declaration.group(1);
            }
            if (declaration.start() >= use) {
                break;
            }
        }
        return nearest;
    }

    /** The name of the call whose argument list holds {@code at}, or null when {@code at} is not inside one. */
    private static String enclosingCall(final String code, final int at) {
        int depth = 0;
        for (int i = at - 1; i >= 0; i--) {
            final char c = code.charAt(i);
            if (c == ')') {
                depth++;
            } else if (c == '(') {
                if (depth == 0) {
                    final int nameEnd = previousCodeChar(code, i);
                    return nameEnd < 0 ? null : identifierEndingAt(code, nameEnd);
                }
                depth--;
            } else if (depth == 0 && (c == ';' || c == '{' || c == '}')) {
                return null;
            }
        }
        return null;
    }

    private static int matchingOpen(final String code, final int close) {
        int depth = 0;
        for (int i = close; i >= 0; i--) {
            if (code.charAt(i) == ')') {
                depth++;
            } else if (code.charAt(i) == '(' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int matchingClose(final String code, final int open) {
        int depth = 0;
        for (int i = open; i < code.length(); i++) {
            if (code.charAt(i) == '(') {
                depth++;
            } else if (code.charAt(i) == ')' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int previousCodeChar(final String code, final int at) {
        int i = at - 1;
        while (i >= 0 && Character.isWhitespace(code.charAt(i))) {
            i--;
        }
        return i;
    }

    private static String identifierEndingAt(final String code, final int end) {
        int start = end;
        while (start >= 0 && Character.isJavaIdentifierPart(code.charAt(start))) {
            start--;
        }
        return end < 0 ? "" : code.substring(start + 1, end + 1);
    }

    private static String where(final Source file, final int at) {
        int line = 1;
        for (int i = 0; i < at; i++) {
            if (file.code().charAt(i) == '\n') {
                line++;
            }
        }
        return file.label() + ":" + line;
    }

    private static void assertNone(final String what, final List<String> found) {
        assertTrue(found.isEmpty(), () -> found.size() + " " + what + ":\n" + String.join("\n", found));
    }

    // gathering the sources

    private static Sources sourcesOf(final Map<String, String> texts) {
        final List<Source> files = new ArrayList<>(texts.size());
        final Set<String> enums = new HashSet<>();
        final Map<String, List<Member>> members = new HashMap<>();
        for (final Map.Entry<String, String> text : texts.entrySet()) {
            final String code = codeOnly(text.getValue());
            files.add(new Source(text.getKey(), code));
            final Matcher declaredEnum = ENUM.matcher(code);
            while (declaredEnum.find()) {
                enums.add(declaredEnum.group(1));
            }
            collectMembers(code, members);
        }
        return new Sources(files, enums, members);
    }

    /** Every no-argument method and every record component, with the type it belongs to and the type it gives. */
    private static void collectMembers(final String code, final Map<String, List<Member>> members) {
        final List<int[]> owners = new ArrayList<>();
        final List<String> ownerNames = new ArrayList<>();
        final Matcher type = TYPE_DECLARATION.matcher(code);
        while (type.find()) {
            owners.add(new int[]{type.start()});
            ownerNames.add(type.group(1));
        }
        final Matcher method = NO_ARGUMENT_METHOD.matcher(code);
        while (method.find()) {
            String owner = null;
            for (int i = 0; i < owners.size() && owners.get(i)[0] < method.start(); i++) {
                owner = ownerNames.get(i);
            }
            members.computeIfAbsent(method.group(2), name -> new ArrayList<>())
                    .add(new Member(owner, method.group(1)));
        }
        final Matcher record = RECORD_HEADER.matcher(code);
        while (record.find()) {
            final int open = record.end() - 1;
            final int close = matchingClose(code, open);
            if (close < 0) {
                continue;
            }
            for (final String component : topLevelParts(code.substring(open + 1, close))) {
                String plain = component.replaceAll("@\\w+", " ");
                for (String previous = ""; !plain.equals(previous); ) {
                    previous = plain;
                    plain = plain.replaceAll("<[^<>]*>", " ");
                }
                final String[] words = plain.trim().split("\\s+");
                if (words.length >= 2) {
                    final String declared = words[words.length - 2].replace("[]", "").replace("...", "");
                    members.computeIfAbsent(words[words.length - 1], name -> new ArrayList<>())
                            .add(new Member(record.group(1), declared.substring(declared.lastIndexOf('.') + 1)));
                }
            }
        }
    }

    private static List<String> topLevelParts(final String list) {
        final List<String> parts = new ArrayList<>();
        int depth = 0;
        int from = 0;
        for (int i = 0; i < list.length(); i++) {
            final char c = list.charAt(i);
            if (c == '<' || c == '(') {
                depth++;
            } else if (c == '>' || c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(list.substring(from, i));
                from = i + 1;
            }
        }
        parts.add(list.substring(from));
        return parts;
    }

    /** The source with every comment, string, text block and character literal blanked, newlines kept. */
    private static String codeOnly(final String source) {
        final StringBuilder out = new StringBuilder(source.length());
        final int n = source.length();
        int i = 0;
        while (i < n) {
            final char c = source.charAt(i);
            final char next = i + 1 < n ? source.charAt(i + 1) : '\0';
            if (c == '/' && next == '/') {
                while (i < n && source.charAt(i) != '\n') {
                    out.append(' ');
                    i++;
                }
            } else if (c == '/' && next == '*') {
                out.append("  ");
                i += 2;
                while (i < n && !source.startsWith("*/", i)) {
                    out.append(blank(source.charAt(i)));
                    i++;
                }
                if (i < n) {
                    out.append("  ");
                    i += 2;
                }
            } else if (source.startsWith("\"\"\"", i)) {
                out.append("   ");
                i += 3;
                while (i < n && !source.startsWith("\"\"\"", i)) {
                    final int width = source.charAt(i) == '\\' && i + 1 < n ? 2 : 1;
                    for (int k = 0; k < width; k++) {
                        out.append(blank(source.charAt(i + k)));
                    }
                    i += width;
                }
                if (i < n) {
                    out.append("   ");
                    i += 3;
                }
            } else if (c == '"' || c == '\'') {
                out.append(' ');
                i++;
                while (i < n && source.charAt(i) != c && source.charAt(i) != '\n') {
                    final int width = source.charAt(i) == '\\' && i + 1 < n ? 2 : 1;
                    out.append(width == 2 ? "  " : " ");
                    i += width;
                }
                if (i < n && source.charAt(i) == c) {
                    out.append(' ');
                    i++;
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static char blank(final char c) {
        return c == '\n' ? '\n' : ' ';
    }

    /** One source file with its comments and literals blanked, named by its path under the series' root. */
    private record Source(String label, String code) {
    }

    /** A method or record component: the type it is declared in (null when unknown) and the type it gives. */
    private record Member(String owner, String type) {
    }

    /** The files the rules read, the names of the series' enums, and what each named member gives. */
    private record Sources(List<Source> files, Set<String> enums, Map<String, List<Member>> members) {

        /** The type a member named {@code name} gives, on {@code owner} when known, or null when that is not one type. */
        String returnType(final String owner, final String name) {
            final List<Member> candidates = members.get(name);
            if (candidates == null) {
                return null;
            }
            String found = null;
            for (final Member candidate : candidates) {
                if (owner != null && !owner.equals(candidate.owner())) {
                    continue;
                }
                if (found != null && !found.equals(candidate.type())) {
                    return null;
                }
                found = candidate.type();
            }
            return found;
        }
    }

    /** The main sources of every module beside the Core, read once for all the rules. */
    private static final class MainSources {

        static final Sources ALL = load();

        private static Sources load() {
            final Path root = Path.of("").toAbsolutePath().getParent();
            final Map<String, String> texts = new LinkedHashMap<>();
            try (Stream<Path> modules = Files.list(root)) {
                for (final Path module : modules.sorted().toList()) {
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
            return sourcesOf(texts);
        }
    }
}
