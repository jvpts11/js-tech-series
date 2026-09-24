/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Fixed words a player reads, counted file by file, so that there can only ever be fewer of them.
 *
 * <p>Every sentence a player reads is to be declared as a {@link TextKey} and translated. While the old ones are
 * being converted, this holds the line: the count of fixed prose in each file is kept, a file may never have more
 * than it had, and a file that is new starts at none. When a file has fewer, the new count is written down, so the
 * number only ever goes one way until it reaches nothing.
 *
 * <p>What counts is a string literal with a letter in it that is either handed to something that puts words in
 * front of a player (a screen drawing, a message, a terminal line, the answer a command gives), or reads as a
 * sentence, two words or more. What does not count is what is not for a player, or is data on purpose: a log line,
 * an exception's message, an annotation, a sentence's own English where it is declared (a {@code TextKey}, or the
 * name and description something is declared with, {@code .named(...)} and {@code .described(...)} chained on it),
 * and words wrapped in {@code Text.literal}, which is how data is marked so it can be found.
 */
class HardcodedTextTest {

    /** Where the counts as they are now are written when they are not the ones kept. */
    private static final Path WRITTEN = Path.of("build", "hardcoded-text.txt");

    /** The mods whose text a player reads; the test mod is for development only. */
    private static final Set<String> MODULES = Set.of("core", "computers", "industrial");

    /** Methods that put what they are handed in front of a player. */
    private static final Set<String> SINKS = Set.of("drawString", "drawCenteredString", "drawWordWrap",
            "text", "textS", "textRight", "textCenter", "textSRight", "textSCenter",
            "line", "error", "ok", "fail", "warn", "info", "dim", "accent", "header", "row", "entry", "say", "plain",
            "showBalloon", "raise", "displayClientMessage", "sendSystemMessage", "broadcastSystemMessage",
            "sendConsoleLine", "setTooltip", "withTooltip");

    /** Who a line for the developer is written through. */
    private static final Set<String> LOGGERS = Set.of("LOGGER", "LOG", "logger", "log");

    /**
     * An advancement tab declaring its advancements on itself: the title and description handed in are the English the
     * language file is generated from, under keys the advancement is shown by.
     */
    private static final Set<String> ADVANCEMENT_DECLARATIONS = Set.of("root", "task", "goal", "challenge", "secret",
            "goalWith", "challengeOfAll");

    /** Two runs of letters with something between them: words, not an identifier. */
    private static final Pattern PROSE = Pattern.compile("[A-Za-z]{2,}[^A-Za-z]+[A-Za-z]{2,}");
    private static final Pattern LETTER = Pattern.compile("[A-Za-z]");

    @Test
    void fixedProse_onlyEverGoesDown() throws IOException {
        final Map<String, List<Hit>> found = hitsByFile();
        final Map<String, Integer> kept = kept();
        final List<String> grown = new ArrayList<>();
        boolean changed = !kept.keySet().equals(counted(found).keySet());
        for (final Map.Entry<String, List<Hit>> file : found.entrySet()) {
            final int before = kept.getOrDefault(file.getKey(), 0);
            final int now = file.getValue().size();
            if (now > before) {
                grown.add(file.getKey() + ": " + before + " -> " + now);
                for (final Hit hit : file.getValue()) {
                    grown.add("    line " + hit.line() + ": \"" + hit.text() + "\"");
                }
            }
            changed |= now != before;
        }
        if (changed) {
            Files.createDirectories(WRITTEN.getParent());
            Files.writeString(WRITTEN, render(counted(found)), StandardCharsets.UTF_8);
        }
        if (!grown.isEmpty()) {
            fail("fixed words a player reads were added; declare each as a TextKey, or wrap data in Text.literal:\n"
                    + String.join("\n", grown));
        }
        if (changed) {
            fail("there is less fixed text than was counted, which is the way it should go: record it by copying "
                    + WRITTEN.toAbsolutePath() + " over src/test/resources/hardcoded-text.txt");
        }
    }

    @Test
    void everyModIsRead() {
        final Map<String, List<Hit>> found = hitsByFile();
        for (final String module : MODULES) {
            assertTrue(found.keySet().stream().anyMatch(path -> path.startsWith(module + "/")),
                    () -> "nothing was counted in " + module);
        }
    }

    @Test
    void theScannerTellsDataFromWordsAPlayerReads() {
        final String source = """
                class A {
                    void f() {
                        LOGGER.warn("a log line for developers");
                        throw new IllegalStateException("an exception message here");
                        Objects.requireNonNull(name, "a name is never missing here");
                        ctx.out().line("Shown to the player");
                        ctx.out().line(Text.literal("/usr/ports"));
                        final String label = "Low on memory";
                        tag.getString("History");
                        drawString(font, "Power", 1, 2);
                        // "a comment with words"
                        KEY = TextKey.of("jsc.key", "the English itself");
                        BLOCK = CONTENT.block("x", X::new)
                                .named("Electric Furnace").register();
                        SPEC = ProgramSpec.of(id, "x").described("Keeps the network's files");
                        this.task("first", "root", ICON, "First Steps", "Start the machine", on(EVENT));
                    }
                    @Deprecated(since = "the annotation text")
                    void g() { }
                    @Override
                    void h() { ctx.out().error("after a bare annotation"); }
                }
                """;
        final List<String> read = scan(source).stream().map(Hit::text).toList();
        assertTrue(read.equals(List.of("Shown to the player", "Low on memory", "Power", "after a bare annotation")),
                () -> "read " + read);
    }

    /** One fixed piece of text: the line it is on and what it says. */
    private record Hit(int line, String text) {
    }

    /** A call the scanner is inside: what it is called, what it is called on, and what kind of call it is. */
    private record Frame(String name, String receiver, boolean constructor, boolean annotation) {

        boolean excludes() {
            return this.annotation
                    || (this.receiver != null && LOGGERS.contains(this.receiver))
                    || (this.constructor && this.name != null
                            && (this.name.endsWith("Exception") || this.name.endsWith("Error")))
                    // What a null check says when it fails is an exception's message, for whoever reads the log.
                    || ("Objects".equals(this.receiver) && "requireNonNull".equals(this.name))
                    || ("TextKey".equals(this.receiver) && "of".equals(this.name))
                    || (this.constructor && "TextKey".equals(this.name))
                    // A declaration's name or description, chained on it: the English the language file is made from.
                    || (("named".equals(this.name) || "described".equals(this.name)) && this.receiver == null)
                    || ("this".equals(this.receiver) && ADVANCEMENT_DECLARATIONS.contains(this.name))
                    || ("Text".equals(this.receiver) && "literal".equals(this.name))
                    // A command's example line or switch in its manual: what a player types, which is data.
                    || (this.constructor && ("Example".equals(this.name) || "Option".equals(this.name)));
        }

        boolean shows() {
            if (this.name == null || this.constructor) {
                return false;
            }
            if ("literal".equals(this.name)) {
                return "Component".equals(this.receiver);
            }
            // Map.entry builds a table, not a line on a terminal, whatever the two share in name.
            return SINKS.contains(this.name) && !("entry".equals(this.name) && "Map".equals(this.receiver));
        }
    }

    private static Map<String, List<Hit>> hitsByFile() {
        final Map<String, List<Hit>> out = new TreeMap<>();
        final Path root = Path.of("").toAbsolutePath().getParent();
        for (final String module : MODULES) {
            final Path main = root.resolve(module).resolve("src").resolve("main").resolve("java");
            if (!Files.isDirectory(main)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(main)) {
                for (final Path file : walk.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                    final List<Hit> hits = scan(Files.readString(file, StandardCharsets.UTF_8));
                    if (!hits.isEmpty()) {
                        out.put(root.relativize(file).toString().replace('\\', '/'), hits);
                    }
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return out;
    }

    /**
     * The fixed text in one source, read with just enough of Java to know where each literal stands: comments are
     * skipped, and every open call is kept on a stack with its name and what it is called on.
     */
    private static List<Hit> scan(final String source) {
        final List<Hit> hits = new ArrayList<>();
        final Deque<Frame> calls = new ArrayDeque<>();
        final int length = source.length();
        int line = 1;
        String lastWord = null;
        String receiver = null;
        String dotted = null;
        boolean afterNew = false;
        boolean afterAt = false;
        // Whether the last word read is an annotation's name, which only a bracket straight after it opens.
        boolean annotationWord = false;
        boolean wordLast = false;
        int i = 0;
        while (i < length) {
            final char c = source.charAt(i);
            if (c == '\n') {
                line++;
                i++;
                continue;
            }
            if (c == '/' && i + 1 < length && source.charAt(i + 1) == '/') {
                while (i < length && source.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < length && source.charAt(i + 1) == '*') {
                final int end = source.indexOf("*/", i + 2);
                final int stop = end < 0 ? length : end + 2;
                line += count(source, i, stop);
                i = stop;
                continue;
            }
            if (c == '"') {
                final boolean block = source.startsWith("\"\"\"", i);
                final int start = i + (block ? 3 : 1);
                final int end = block ? source.indexOf("\"\"\"", start) : closing(source, start);
                final int stop = end < 0 ? length : end;
                final String text = source.substring(start, stop);
                if (counts(text, calls)) {
                    hits.add(new Hit(line, text.strip()));
                }
                line += count(source, i, stop);
                i = stop + (block ? 3 : 1);
                wordLast = false;
                continue;
            }
            if (c == '\'') {
                i = closingQuote(source, i + 1) + 1;
                wordLast = false;
                continue;
            }
            if (Character.isJavaIdentifierStart(c)) {
                final int start = i;
                while (i < length && Character.isJavaIdentifierPart(source.charAt(i))) {
                    i++;
                }
                final String word = source.substring(start, i);
                if ("new".equals(word)) {
                    afterNew = true;
                    wordLast = false;
                    continue;
                }
                receiver = dotted;
                dotted = null;
                lastWord = word;
                annotationWord = afterAt;
                afterAt = false;
                wordLast = true;
                continue;
            }
            if (c == '.') {
                dotted = wordLast ? lastWord : null;
                wordLast = false;
                i++;
                continue;
            }
            if (c == '@') {
                afterAt = true;
                i++;
                continue;
            }
            if (c == '(') {
                calls.push(wordLast ? new Frame(lastWord, receiver, afterNew, annotationWord)
                        : new Frame(null, null, false, false));
                afterNew = false;
                annotationWord = false;
                wordLast = false;
                i++;
                continue;
            }
            if (c == ')') {
                if (!calls.isEmpty()) {
                    calls.pop();
                }
            } else if (!Character.isWhitespace(c) && c != '<' && c != '>' && c != ',' && c != '?') {
                // Anything else ends a qualified name and whatever an annotation or a constructor was about to open.
                afterNew = afterNew && (c == '[' || c == ']');
                afterAt = false;
                annotationWord = false;
                dotted = null;
            }
            if (!Character.isWhitespace(c)) {
                wordLast = false;
            }
            i++;
        }
        return hits;
    }

    /** Whether a literal standing inside those calls is fixed text a player reads. */
    private static boolean counts(final String text, final Deque<Frame> calls) {
        if (!LETTER.matcher(text).find()) {
            return false;
        }
        boolean shown = false;
        for (final Frame frame : calls) {
            if (frame.excludes()) {
                return false;
            }
            shown |= frame.shows();
        }
        return shown || PROSE.matcher(text).find() && text.indexOf(' ') >= 0;
    }

    private static int closing(final String source, final int from) {
        int i = from;
        while (i < source.length()) {
            final char c = source.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '"' || c == '\n') {
                return i;
            }
            i++;
        }
        return source.length();
    }

    private static int closingQuote(final String source, final int from) {
        int i = from;
        while (i < source.length() && source.charAt(i) != '\'') {
            i += source.charAt(i) == '\\' ? 2 : 1;
        }
        return Math.min(i, source.length() - 1);
    }

    private static int count(final String source, final int from, final int to) {
        int lines = 0;
        for (int i = from; i < to && i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    private static Map<String, Integer> counted(final Map<String, List<Hit>> found) {
        final Map<String, Integer> out = new TreeMap<>();
        found.forEach((file, hits) -> out.put(file, hits.size()));
        return out;
    }

    private static String render(final Map<String, Integer> counts) {
        final StringBuilder out = new StringBuilder();
        counts.forEach((file, n) -> out.append(file).append(' ').append(n).append('\n'));
        return out.toString();
    }

    private static Map<String, Integer> kept() throws IOException {
        final Map<String, Integer> out = new TreeMap<>();
        try (var stream = HardcodedTextTest.class.getResourceAsStream("/hardcoded-text.txt")) {
            if (stream == null) {
                return out;
            }
            final Matcher row = Pattern.compile("^(\\S+) (\\d+)$", Pattern.MULTILINE)
                    .matcher(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            while (row.find()) {
                out.put(row.group(1), Integer.parseInt(row.group(2)));
            }
        }
        return out;
    }
}
