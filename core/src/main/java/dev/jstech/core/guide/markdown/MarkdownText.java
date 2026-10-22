/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide.markdown;

import java.util.List;
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
        String text = CODE.matcher(line).replaceAll("$1");
        text = STRONG.matcher(text).replaceAll("$1");
        text = EMPHASIS.matcher(text).replaceAll("$1");
        text = ESCAPED.matcher(text).replaceAll("$1");
        for (final String[] entity : ENTITIES) {
            text = text.replace(entity[0], entity[1]);
        }
        return SPACES.matcher(text).replaceAll(" ").strip();
    }
}
