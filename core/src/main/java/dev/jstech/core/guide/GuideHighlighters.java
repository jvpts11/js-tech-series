/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.JsCore;
import dev.jstech.core.language.IProgrammingLanguage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How a manual colours the code of a listing, by the language the listing names. A language that the game can compile
 * colours itself: a listing named as one of its files ({@code Hello.sgs}), or naming one of its extensions, is
 * coloured as the editors of the game colour it. A language that is no programming language of the game, such as a
 * query language, registers how it is coloured here, under its own name.
 *
 * <pre>{@code
 * GuideHighlighters.register("iql", lines -> GuideCode.runs(lines, MyQueries.spans(lines)));
 * }</pre>
 */
public final class GuideHighlighters {

    private static final Map<String, IGuideHighlighter> REGISTERED = new ConcurrentHashMap<>();

    private GuideHighlighters() {
    }

    /** Registers how the listings of a language are coloured, under its name as listings name it. */
    public static void register(final String language, final IGuideHighlighter highlighter) {
        Objects.requireNonNull(highlighter, "highlighter");
        final String name = Objects.requireNonNull(language, "language").toLowerCase(Locale.ROOT);
        if (REGISTERED.putIfAbsent(name, highlighter) != null) {
            throw new IllegalStateException("the manuals' colours of " + language + " are registered twice");
        }
    }

    /**
     * The runs of every line of a listing in that language: as a highlighter registered under its name colours them,
     * or as the game's language of that extension does, or plain when neither knows it.
     */
    public static List<List<GuideCode.Run>> colour(final String language, final List<String> lines) {
        final String name = language.toLowerCase(Locale.ROOT);
        final IGuideHighlighter registered = REGISTERED.get(name);
        if (registered != null) {
            return registered.colour(lines);
        }
        final IProgrammingLanguage known = JsCore.languages().byExtension(name);
        if (known == null) {
            return GuideCode.plain(lines);
        }
        final List<IProgrammingLanguage.Token> tokens;
        try {
            tokens = known.tokenize(String.join("\n", lines));
        } catch (final RuntimeException e) {
            // A listing its language cannot read is still printed, in one colour.
            return GuideCode.plain(lines);
        }
        final List<GuideCode.Span> spans = new ArrayList<>();
        for (final IProgrammingLanguage.Token token : tokens) {
            spans.add(new GuideCode.Span(token.line(), token.column(), token.length(), switch (token.kind()) {
                case KEYWORD -> GuideCode.Ink.KEYWORD;
                case NAME -> GuideCode.Ink.NAME;
                case TEXT -> GuideCode.Ink.TEXT;
                case NUMBER -> GuideCode.Ink.NUMBER;
                case COMMENT -> GuideCode.Ink.COMMENT;
                case SYMBOL -> GuideCode.Ink.SYMBOL;
            }));
        }
        return GuideCode.runs(lines, spans);
    }

    /** How the lines of a listing in one language are coloured. */
    @FunctionalInterface
    public interface IGuideHighlighter {

        /** The runs of every line, in order, as {@link GuideCode#runs} builds them. */
        List<List<GuideCode.Run>> colour(List<String> lines);
    }
}
