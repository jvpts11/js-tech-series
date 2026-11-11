/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The words of a line of Markdown as a manual writes them: the lines of a paragraph joined into one, bold, emphasis and
 * code marks taken off (a manual's styles write all of a sentence alike), a character escaped with a backslash as
 * itself, and the few entities a text has written as their characters. A link, {@code [the eras](mymod:eras)}, a key,
 * {@code [[Ctrl]]}, and a value read from a mod, {@code {{mymod:memory_slots}}}, are a manual's own and stay as they
 * are written. Pure.
 */
public final class MarkdownText {

    private static final Pattern CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern STRONG = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern EMPHASIS = Pattern.compile("(?<![\\w*])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![\\w*])");
    private static final Pattern ESCAPED = Pattern.compile("\\\\([\\\\`*_{}\\[\\]()#+\\-.!|>~])");
    private static final Pattern SPACES = Pattern.compile("\\s+");
    /* Where an escaped character waits while the marks are taken off: a character of the private use area. */
    private static final String HELD = "";
    private static final Pattern HELD_MARK = Pattern.compile(HELD + "(\\d+)" + HELD);
    private static final List<String[]> ENTITIES = List.of(new String[] {"&lt;", "<"}, new String[] {"&gt;", ">"},
            new String[] {"&quot;", "\""}, new String[] {"&#39;", "'"}, new String[] {"&nbsp;", " "},
            new String[] {"&amp;", "&"});

    private MarkdownText() {
    }

    /** The lines of a paragraph as one line of words. */
    public static String of(final List<String> lines) {
        return of(String.join(" ", lines));
    }

    /** A line's words, its marks taken off. */
    public static String of(final String line) {
        // An escaped character is set aside before the marks are taken off, so no mark takes it: \*.\* stays *.*.
        final List<String> escaped = new ArrayList<>();
        String text = ESCAPED.matcher(line).replaceAll(match -> {
            escaped.add(match.group(1));
            return HELD + (escaped.size() - 1) + HELD;
        });
        text = CODE.matcher(text).replaceAll("$1");
        text = STRONG.matcher(text).replaceAll("$1");
        text = EMPHASIS.matcher(text).replaceAll("$1");
        for (final String[] entity : ENTITIES) {
            text = text.replace(entity[0], entity[1]);
        }
        text = SPACES.matcher(text).replaceAll(" ").strip();
        return HELD_MARK.matcher(text).replaceAll(match -> Matcher.quoteReplacement(
                escaped.get(Integer.parseInt(match.group(1)))));
    }
}
