/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import java.util.ArrayList;
import java.util.List;

/**
 * Lua's own pattern matching, the one {@code string.find}, {@code match}, {@code gmatch} and
 * {@code gsub} share.
 *
 * <p>Patterns are not regular expressions: a class is one character or {@code %a}-style set, the
 * quantifiers are {@code * + - ?} on a single class, captures are {@code ()} and can be position
 * captures, and {@code %b()} and {@code %f[set]} match balanced pairs and frontiers. The matcher is a
 * backtracking one written to the language's rules rather than a translation into Java's, so that
 * every program's pattern means here exactly what it meant there.
 */
public final class LuaPatterns {

    /** The most captures a pattern may have. */
    public static final int MAX_CAPTURES = 32;

    private static final int CAPTURE_UNFINISHED = -1;
    private static final int CAPTURE_POSITION = -2;
    private static final char ESCAPE = '%';
    private static final String SPECIALS = "^$*+?.([%-";
    private static final int MAX_DEPTH = 200;

    /** What one match found: where it starts and ends, and each capture as text or a position. */
    public record Match(int start, int end, List<Object> captures) {

        /** The captures, or the whole match when the pattern captured nothing. */
        public List<Object> results(final String subject) {
            if (!this.captures.isEmpty()) {
                return this.captures;
            }
            return List.of(subject.substring(this.start, this.end));
        }
    }

    /** Thrown for a pattern the language would refuse. */
    public static final class BadPattern extends RuntimeException {

        private static final long serialVersionUID = 1L;

        BadPattern(final String message) {
            super(message);
        }
    }

    private final String subject;
    private final String pattern;
    private int level;
    private final int[] captureStart = new int[MAX_CAPTURES];
    private final int[] captureLength = new int[MAX_CAPTURES];
    private int depth;

    private LuaPatterns(final String subject, final String pattern) {
        this.subject = subject;
        this.pattern = pattern;
    }

    /** Whether the pattern has nothing special in it, so a plain search will do. */
    public static boolean isPlain(final String pattern) {
        for (int i = 0; i < pattern.length(); i++) {
            if (SPECIALS.indexOf(pattern.charAt(i)) >= 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * The first match at or after {@code from} (zero-based), or null.
     *
     * <p>An anchored pattern is tried once, where the search starts; any other is tried at every
     * position until it matches or the subject runs out.
     */
    public static Match find(final String subject, final String pattern, final int from) {
        final boolean anchored = !pattern.isEmpty() && pattern.charAt(0) == '^';
        final int patternStart = anchored ? 1 : 0;
        int at = Math.max(0, from);
        do {
            final LuaPatterns state = new LuaPatterns(subject, pattern);
            final int end = state.match(at, patternStart);
            if (end >= 0) {
                return new Match(at, end, state.captures(at, end));
            }
            at++;
        } while (at <= subject.length() && !anchored);
        return null;
    }

    private List<Object> captures(final int start, final int end) {
        final List<Object> found = new ArrayList<>();
        for (int i = 0; i < this.level; i++) {
            found.add(this.capture(i, start, end));
        }
        return found;
    }

    private Object capture(final int index, final int start, final int end) {
        final int length = this.captureLength[index];
        if (length == CAPTURE_POSITION) {
            return (long) (this.captureStart[index] + 1);
        }
        if (length == CAPTURE_UNFINISHED) {
            throw new BadPattern("unfinished capture");
        }
        return this.subject.substring(this.captureStart[index], this.captureStart[index] + length);
    }

    /** The end of a match of the pattern from {@code p} against the subject from {@code s}, or -1. */
    private int match(final int s, final int p) {
        if (++this.depth > MAX_DEPTH) {
            throw new BadPattern("pattern too complex");
        }
        try {
            return this.matchInner(s, p);
        } finally {
            this.depth--;
        }
    }

    private int matchInner(int s, int p) {
        while (true) {
            if (p >= this.pattern.length()) {
                return s;
            }
            final char c = this.pattern.charAt(p);
            switch (c) {
                case '(' -> {
                    if (p + 1 < this.pattern.length() && this.pattern.charAt(p + 1) == ')') {
                        return this.startCapture(s, p + 2, CAPTURE_POSITION);
                    }
                    return this.startCapture(s, p + 1, CAPTURE_UNFINISHED);
                }
                case ')' -> {
                    return this.endCapture(s, p + 1);
                }
                case '$' -> {
                    if (p + 1 == this.pattern.length()) {
                        return s == this.subject.length() ? s : -1;
                    }
                }
                case ESCAPE -> {
                    if (p + 1 < this.pattern.length()) {
                        final char next = this.pattern.charAt(p + 1);
                        if (next == 'b') {
                            s = this.matchBalance(s, p + 2);
                            if (s < 0) {
                                return -1;
                            }
                            p += 4;
                            continue;
                        }
                        if (next == 'f') {
                            p += 2;
                            if (p >= this.pattern.length() || this.pattern.charAt(p) != '[') {
                                throw new BadPattern("missing '[' after '%f' in pattern");
                            }
                            final int classEnd = this.classEnd(p);
                            final char previous = s == 0 ? '\0' : this.subject.charAt(s - 1);
                            final char current = s < this.subject.length() ? this.subject.charAt(s) : '\0';
                            if (!this.matchClass(previous, p, classEnd - 1)
                                    && this.matchClass(current, p, classEnd - 1)) {
                                p = classEnd;
                                continue;
                            }
                            return -1;
                        }
                        if (Character.isDigit(next)) {
                            s = this.matchCapture(s, next);
                            if (s < 0) {
                                return -1;
                            }
                            p += 2;
                            continue;
                        }
                    }
                }
                default -> { }
            }
            final int classEnd = this.classEnd(p);
            final char quantifier = classEnd < this.pattern.length() ? this.pattern.charAt(classEnd) : '\0';
            if (quantifier == '?') {
                if (this.single(s, p, classEnd)) {
                    final int result = this.match(s + 1, classEnd + 1);
                    if (result >= 0) {
                        return result;
                    }
                }
                p = classEnd + 1;
                continue;
            }
            if (quantifier == '+') {
                return this.single(s, p, classEnd) ? this.maxExpand(s + 1, p, classEnd) : -1;
            }
            if (quantifier == '*') {
                return this.maxExpand(s, p, classEnd);
            }
            if (quantifier == '-') {
                return this.minExpand(s, p, classEnd);
            }
            if (!this.single(s, p, classEnd)) {
                return -1;
            }
            s++;
            p = classEnd;
        }
    }

    private int classEnd(final int start) {
        int p = start;
        if (p >= this.pattern.length()) {
            throw new BadPattern("malformed pattern (ends with '%')");
        }
        final char c = this.pattern.charAt(p++);
        if (c == ESCAPE) {
            if (p >= this.pattern.length()) {
                throw new BadPattern("malformed pattern (ends with '%')");
            }
            return p + 1;
        }
        if (c == '[') {
            if (p < this.pattern.length() && this.pattern.charAt(p) == '^') {
                p++;
            }
            do {
                if (p >= this.pattern.length()) {
                    throw new BadPattern("malformed pattern (missing ']')");
                }
                final char inner = this.pattern.charAt(p++);
                if (inner == ESCAPE) {
                    if (p >= this.pattern.length()) {
                        throw new BadPattern("malformed pattern (ends with '%')");
                    }
                    p++;
                }
            } while (p >= this.pattern.length() || this.pattern.charAt(p) != ']');
            return p + 1;
        }
        return p;
    }

    private boolean single(final int s, final int p, final int classEnd) {
        if (s >= this.subject.length()) {
            return false;
        }
        final char c = this.subject.charAt(s);
        final char first = this.pattern.charAt(p);
        return switch (first) {
            case '.' -> true;
            case ESCAPE -> matchOne(c, this.pattern.charAt(p + 1));
            case '[' -> this.matchClass(c, p, classEnd - 1);
            default -> first == c;
        };
    }

    /** Whether the character belongs to the set {@code [...]} between those two positions of the pattern. */
    private boolean matchClass(final char c, final int start, final int end) {
        int p = start + 1;
        boolean wanted = true;
        if (this.pattern.charAt(p) == '^') {
            wanted = false;
            p++;
        }
        while (p < end) {
            final char at = this.pattern.charAt(p);
            if (at == ESCAPE) {
                p++;
                if (matchOne(c, this.pattern.charAt(p))) {
                    return wanted;
                }
                p++;
            } else if (p + 2 < end && this.pattern.charAt(p + 1) == '-') {
                if (at <= c && c <= this.pattern.charAt(p + 2)) {
                    return wanted;
                }
                p += 3;
            } else {
                if (at == c) {
                    return wanted;
                }
                p++;
            }
        }
        return !wanted;
    }

    /** Whether the character is in the class the letter names; an upper-case letter names the rest. */
    static boolean matchOne(final char c, final char letter) {
        final boolean result = switch (Character.toLowerCase(letter)) {
            case 'a' -> Character.isLetter(c);
            case 'c' -> Character.isISOControl(c);
            case 'd' -> c >= '0' && c <= '9';
            case 'g' -> c > 32 && c < 127;
            case 'l' -> c >= 'a' && c <= 'z';
            case 'p' -> c > 32 && c < 127 && !Character.isLetterOrDigit(c);
            case 's' -> c == ' ' || (c >= '\t' && c <= '\r');
            case 'u' -> c >= 'A' && c <= 'Z';
            case 'w' -> Character.isLetterOrDigit(c);
            case 'x' -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            default -> {
                yield letter == c;
            }
        };
        if (Character.isLetter(letter) && Character.isUpperCase(letter)) {
            return !result;
        }
        return result;
    }

    private int maxExpand(final int s, final int p, final int classEnd) {
        int count = 0;
        while (this.single(s + count, p, classEnd)) {
            count++;
        }
        while (count >= 0) {
            final int result = this.match(s + count, classEnd + 1);
            if (result >= 0) {
                return result;
            }
            count--;
        }
        return -1;
    }

    private int minExpand(int s, final int p, final int classEnd) {
        while (true) {
            final int result = this.match(s, classEnd + 1);
            if (result >= 0) {
                return result;
            }
            if (this.single(s, p, classEnd)) {
                s++;
            } else {
                return -1;
            }
        }
    }

    private int startCapture(final int s, final int p, final int what) {
        if (this.level >= MAX_CAPTURES) {
            throw new BadPattern("too many captures");
        }
        this.captureStart[this.level] = s;
        this.captureLength[this.level] = what;
        this.level++;
        final int result = this.match(s, p);
        if (result < 0) {
            this.level--;
        }
        return result;
    }

    private int endCapture(final int s, final int p) {
        final int open = this.captureToClose();
        this.captureLength[open] = s - this.captureStart[open];
        final int result = this.match(s, p);
        if (result < 0) {
            this.captureLength[open] = CAPTURE_UNFINISHED;
        }
        return result;
    }

    private int captureToClose() {
        for (int i = this.level - 1; i >= 0; i--) {
            if (this.captureLength[i] == CAPTURE_UNFINISHED) {
                return i;
            }
        }
        throw new BadPattern("invalid pattern capture");
    }

    private int matchBalance(final int s, final int p) {
        if (p + 1 >= this.pattern.length()) {
            throw new BadPattern("malformed pattern (missing arguments to '%b')");
        }
        if (s >= this.subject.length() || this.subject.charAt(s) != this.pattern.charAt(p)) {
            return -1;
        }
        final char open = this.pattern.charAt(p);
        final char close = this.pattern.charAt(p + 1);
        int count = 1;
        for (int at = s + 1; at < this.subject.length(); at++) {
            final char c = this.subject.charAt(at);
            if (c == close) {
                if (--count == 0) {
                    return at + 1;
                }
            } else if (c == open) {
                count++;
            }
        }
        return -1;
    }

    private int matchCapture(final int s, final char digit) {
        final int index = digit - '1';
        if (index < 0 || index >= this.level || this.captureLength[index] == CAPTURE_UNFINISHED) {
            throw new BadPattern("invalid capture index %" + (index + 1));
        }
        final int length = this.captureLength[index];
        if (this.subject.length() - s >= length
                && this.subject.regionMatches(this.captureStart[index], this.subject, s, length)) {
            return s + length;
        }
        return -1;
    }
}
