/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.check;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a source file into the promises it makes: its public types and what anything outside can reach of them, one
 * line each in the photo's words.
 *
 * <p>It reads source rather than classes because a unit test sees every mod's sources but only its own classes, and
 * the game's types an API names are on no unit test's class path at all. It knows as much Java as an API is written
 * in: types nested in types, records, enums, interfaces whose members are public without saying so, annotations,
 * generics, and the bodies and initializers it only has to step over.
 */
final class ApiSourceReader {

    private final String code;
    private final String pkg;
    private final List<ApiPromise> found = new ArrayList<>();
    private int at;

    private static final Set<String> MODIFIERS = Set.of("public", "protected", "private", "static", "final",
            "abstract", "default", "sealed", "non-sealed", "synchronized", "native", "strictfp", "transient",
            "volatile");
    /* The modifiers that change what a caller or an implementer may rely on, in the order they are written. */
    private static final List<String> KEPT = List.of("protected", "abstract", "default", "static", "final",
            "sealed", "non-sealed");
    private static final Pattern TYPE_HEAD =
            Pattern.compile("^(class|interface|enum|record|@interface)\\s+(\\w+)(.*)$", Pattern.DOTALL);
    private static final Pattern ANNOTATION = Pattern.compile("@[\\w.]+(\\s*\\([^)]*\\))?\\s*");
    private static final Pattern MODIFIER = Pattern.compile("^(non-sealed|[a-z]+)\\b");
    private static final Pattern PACKAGE = Pattern.compile("\\bpackage\\s+([\\w.]+)\\s*;");
    private static final String FILE = "file";

    ApiSourceReader(final String source) {
        this.code = withoutCommentsOrLiterals(source);
        final Matcher pkgMatch = PACKAGE.matcher(this.code);
        this.pkg = pkgMatch.find() ? pkgMatch.group(1) : "";
    }

    List<ApiPromise> read() {
        body(new Scope(this.pkg, FILE, false, false, false));
        return this.found;
    }

    /** The parts of a list at its own level, split on the commas not inside angle brackets or parentheses. */
    static List<String> topLevelParts(final String list) {
        final List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < list.length(); i++) {
            final char c = list.charAt(i);
            if (c == '<' || c == '(') {
                depth++;
            } else if (c == '>' || c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(list.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(list.substring(start));
        return parts.stream().map(String::strip).filter(part -> !part.isEmpty()).toList();
    }

    /* The source with comments gone and every text and character literal emptied, so no brace in them counts. */
    private static String withoutCommentsOrLiterals(final String source) {
        final StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        while (i < source.length()) {
            final char c = source.charAt(i);
            final char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (c == '/' && next == '/') {
                while (i < source.length() && source.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && next == '*') {
                final int end = source.indexOf("*/", i + 2);
                i = end < 0 ? source.length() : end + 2;
                out.append(' ');
            } else if (source.startsWith("\"\"\"", i)) {
                final int end = source.indexOf("\"\"\"", i + 3);
                i = end < 0 ? source.length() : end + 3;
                out.append("\"\"");
            } else if (c == '"' || c == '\'') {
                i = closingQuote(source, i);
                out.append(c).append(c);
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /* Where the literal opened by the quote at open ends, past its closing quote. */
    private static int closingQuote(final String source, final int open) {
        final char quote = source.charAt(open);
        int i = open + 1;
        while (i < source.length()) {
            final char c = source.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == quote) {
                return i + 1;
            } else {
                i++;
            }
        }
        return i;
    }

    /* Reads the members of a type, or of the file, up to its closing brace, which it steps past. */
    private void body(final Scope owner) {
        if (owner.kind().equals("enum")) {
            constants(owner);
        }
        while (true) {
            skipSpace();
            if (this.at >= this.code.length()) {
                return;
            }
            if (this.code.charAt(this.at) == '}') {
                this.at++;
                return;
            }
            final Segment segment = segment();
            if (segment.end() == '}') {
                this.at++;
                return;
            }
            if (segment.end() == ';') {
                member(owner, segment.head(), false);
                continue;
            }
            // A block: a type of its own, or the body of a method, a constructor or an initializer.
            final Head head = Head.of(segment.head());
            final Matcher type = TYPE_HEAD.matcher(head.rest());
            if (type.matches()) {
                this.at++;
                body(type(owner, head, type));
            } else {
                member(owner, segment.head(), true);
                skipBlock();
            }
        }
    }

    /* The constants that open an enum's body, up to the semicolon after them or the enum's end. */
    private void constants(final Scope owner) {
        final StringBuilder all = new StringBuilder();
        int depth = 0;
        while (this.at < this.code.length()) {
            final char c = this.code.charAt(this.at);
            if (depth == 0 && (c == ';' || c == '}')) {
                if (c == ';') {
                    this.at++;
                }
                break;
            }
            if (depth == 0 && c == '{') {
                // A constant with a body of its own, which says nothing about what the constant is called.
                skipBlock();
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            all.append(c);
            this.at++;
        }
        for (final String constant : topLevelParts(all.toString())) {
            final Head head = Head.of(constant);
            final String name = head.rest().replaceAll("\\s*\\(.*$", "").strip();
            if (!name.isEmpty() && owner.visible()) {
                add(owner, head, name);
            }
        }
    }

    /*
     * Reads up to the end of one member's head: a semicolon, the brace opening its body, or the brace closing the
     * type. A brace inside parentheses (an annotation's array, a lambda handed to a call) is part of the head, and so
     * is one after an equals sign, which opens an initializer the reader steps over.
     */
    private Segment segment() {
        final StringBuilder head = new StringBuilder();
        int depth = 0;
        boolean initializer = false;
        while (this.at < this.code.length()) {
            final char c = this.code.charAt(this.at);
            if (depth == 0) {
                if (c == ';') {
                    this.at++;
                    return new Segment(head.toString(), ';');
                }
                if (c == '}') {
                    return new Segment(head.toString(), '}');
                }
                if (c == '{') {
                    if (!initializer) {
                        return new Segment(head.toString(), '{');
                    }
                    skipBlock();
                    head.append("{}");
                    continue;
                }
                if (c == '=') {
                    initializer = true;
                }
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            head.append(c);
            this.at++;
        }
        return new Segment(head.toString(), ';');
    }

    /* Steps over the block whose opening brace is under the reader, braces inside it and all. */
    private void skipBlock() {
        int depth = 0;
        while (this.at < this.code.length()) {
            final char c = this.code.charAt(this.at++);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return;
            }
        }
    }

    private void skipSpace() {
        while (this.at < this.code.length() && Character.isWhitespace(this.code.charAt(this.at))) {
            this.at++;
        }
    }

    /* A type declared at the reader, written down when it is part of the promise, and the scope of its body. */
    private Scope type(final Scope owner, final Head head, final Matcher type) {
        final String kind = type.group(1);
        final String name = owner.name().isEmpty() ? type.group(2) : owner.name() + "." + type.group(2);
        final boolean visible = !head.internal() && (owner.kind().equals(FILE) ? head.has("public")
                : owner.visible() && visible(owner, head));
        final boolean experimental = owner.experimental() || head.experimental();
        if (visible) {
            final String tail = normal(type.group(3));
            this.found.add(new ApiPromise(name, ApiPromise.TYPE + (head.deprecated() ? ApiPromise.DEPRECATED : "")
                    + kept(head, "") + kind + (tail.isEmpty() || tail.startsWith("(") || tail.startsWith("<")
                    ? tail : " " + tail), experimental));
        }
        return new Scope(name, kind, visible, experimental, !kind.equals("class") || head.has("final"));
    }

    /* A member at the reader: a method, a constructor or a field, written down when it is part of the promise. */
    private void member(final Scope owner, final String text, final boolean hasBody) {
        if (owner.kind().equals(FILE) || !owner.visible()) {
            return;
        }
        final Head head = Head.of(text);
        final String rest = head.rest();
        if (rest.isEmpty() || head.internal() || !visible(owner, head)) {
            return;
        }
        final boolean inInterface = owner.kind().equals("interface") || owner.kind().equals("@interface");
        final int open = topLevel(rest, '(');
        final int equals = topLevel(rest, '=');
        if (open >= 0 && (equals < 0 || open < equals)) {
            final String before = normal(rest.substring(0, open));
            final String parameters = parameterTypes(rest.substring(open + 1, closing(rest, open)));
            final String simple = owner.name().substring(owner.name().lastIndexOf('.') + 1);
            if (before.equals(simple)) {
                add(owner, head, "new(" + parameters + ")");
                return;
            }
            final boolean implicitAbstract = inInterface && !hasBody && !head.has("static")
                    && !head.has("default") && !head.has("private");
            add(owner, head, kept(head, implicitAbstract ? "abstract" : "") + before + "(" + parameters + ")");
        } else if (!hasBody) {
            final String declared = normal(equals < 0 ? rest : rest.substring(0, equals));
            final List<String> declarators = topLevelParts(declared);
            final String first = declarators.get(0);
            final int space = first.lastIndexOf(' ');
            if (space < 0) {
                return;
            }
            final String type = first.substring(0, space);
            final String mods = kept(head, inInterface && !head.has("static") ? "static final" : "");
            add(owner, head, mods + first);
            for (int i = 1; i < declarators.size(); i++) {
                add(owner, head, mods + type + " " + declarators.get(i).strip());
            }
        }
        // A body with no parentheses before it is a record's compact constructor or an initializer: no promise.
    }

    private void add(final Scope owner, final Head head, final String what) {
        this.found.add(new ApiPromise(owner.name(), (head.deprecated() ? ApiPromise.DEPRECATED : "") + what,
                owner.experimental() || head.experimental()));
    }

    /* Whether something declared in owner with that head can be reached from outside the mod. */
    private static boolean visible(final Scope owner, final Head head) {
        if (owner.kind().equals("interface") || owner.kind().equals("@interface")) {
            return !head.has("private");
        }
        return head.has("public") || head.has("protected") && !owner.finalType();
    }

    /* The modifiers a promise keeps, written before what it says, with one it has without writing it. */
    private static String kept(final Head head, final String implied) {
        final StringBuilder out = new StringBuilder();
        for (final String modifier : KEPT) {
            if (head.has(modifier) || (" " + implied + " ").contains(" " + modifier + " ")) {
                out.append(modifier).append(' ');
            }
        }
        return out.toString();
    }

    /* The types of a parameter list, without the names, which a caller never writes. */
    private static String parameterTypes(final String parameters) {
        final List<String> types = new ArrayList<>();
        for (final String parameter : topLevelParts(parameters)) {
            final String written = normal(parameter);
            if (written.isEmpty()) {
                continue;
            }
            final int space = written.lastIndexOf(' ');
            types.add(space < 0 ? written : written.substring(0, space));
        }
        return String.join(", ", types);
    }

    /* A piece of a signature as the photo writes it: no annotations, no final, spaces in one form. */
    private static String normal(final String written) {
        return ANNOTATION.matcher(written).replaceAll("")
                .replaceAll("\\bfinal\\s+", "")
                .replaceAll("\\s+", " ")
                .replaceAll("\\s*<\\s*", "<")
                .replaceAll("\\s+>", ">")
                .replaceAll("\\s*,\\s*", ", ")
                .replaceAll("\\s*\\(\\s*", "(")
                .replaceAll("\\s*\\)", ")")
                .replaceAll("\\s*\\[\\s*]", "[]")
                .strip();
    }

    /* Where that character first stands outside angle brackets and parentheses, or -1. */
    private static int topLevel(final String text, final char wanted) {
        int angles = 0;
        int parens = 0;
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c == wanted && angles == 0 && parens == 0) {
                return i;
            }
            if (c == '<') {
                angles++;
            } else if (c == '>') {
                angles--;
            } else if (c == '(') {
                parens++;
            } else if (c == ')') {
                parens--;
            }
        }
        return -1;
    }

    /* Where the parenthesis opened at open closes. */
    private static int closing(final String text, final int open) {
        int depth = 0;
        for (int i = open; i < text.length(); i++) {
            if (text.charAt(i) == '(') {
                depth++;
            } else if (text.charAt(i) == ')' && --depth == 0) {
                return i;
            }
        }
        return text.length();
    }

    /* What the reader has open: a type's body, or the file around the top-level types. */
    private record Scope(String name, String kind, boolean visible, boolean experimental, boolean finalType) {
    }

    /* A member's head read up to its end, with what ended it. */
    private record Segment(String head, char end) {
    }

    /*
     * A head taken apart: its modifiers, the marks the photo cares about (internal being public only for the mod's own
     * wiring, and so no promise), and what is declared.
     */
    private record Head(Set<String> modifiers, boolean deprecated, boolean experimental, boolean internal,
                        String rest) {

        static Head of(final String text) {
            String rest = text.strip();
            final Set<String> modifiers = new LinkedHashSet<>();
            boolean deprecated = false;
            boolean experimental = false;
            boolean internal = false;
            while (!rest.isEmpty()) {
                if (rest.startsWith("@") && !rest.startsWith("@interface")) {
                    int end = 1;
                    while (end < rest.length() && (Character.isJavaIdentifierPart(rest.charAt(end))
                            || rest.charAt(end) == '.')) {
                        end++;
                    }
                    final String name = rest.substring(1, end);
                    int after = end;
                    while (after < rest.length() && Character.isWhitespace(rest.charAt(after))) {
                        after++;
                    }
                    final int next = after < rest.length() && rest.charAt(after) == '('
                            ? closing(rest, after) + 1 : end;
                    deprecated |= name.equals("Deprecated");
                    experimental |= name.equals("ApiStatus.Experimental") || name.equals("Experimental");
                    internal |= name.equals("ApiStatus.Internal");
                    rest = rest.substring(Math.min(next, rest.length())).strip();
                    continue;
                }
                final Matcher word = MODIFIER.matcher(rest);
                if (word.find() && MODIFIERS.contains(word.group(1))) {
                    modifiers.add(word.group(1));
                    rest = rest.substring(word.end()).strip();
                    continue;
                }
                break;
            }
            return new Head(modifiers, deprecated, experimental, internal, rest.replaceAll("\\s+", " "));
        }

        boolean has(final String modifier) {
            return this.modifiers.contains(modifier);
        }
    }
}
