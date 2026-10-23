/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * How a manual colours the code of a listing, by the language the listing names. A language registers how it is
 * coloured here, under its name as listings name it; a mod whose machines run languages of their own hands in what
 * finds one of them by a listing's name or extension ({@link #findWith}), so a listing named as one of their files
 * ({@code Hello.sgs}) is coloured as the editors of the game colour it.
 *
 * <pre>{@code
 * GuideHighlighters.register("iql", lines -> GuideCode.runs(lines, MyQueries.spans(lines)));
 * }</pre>
 */
public final class GuideHighlighters {

    private static final Map<String, IGuideHighlighter> REGISTERED = new ConcurrentHashMap<>();
    /* What finds how a language no highlighter is registered under is coloured, asked in the order handed in. */
    private static final List<Function<String, Optional<IGuideHighlighter>>> FINDERS = new CopyOnWriteArrayList<>();

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
     * Hands in what finds how a language is coloured by the name a listing gives it, in lower case, when no
     * highlighter is registered under that name: a mod's own languages, found by their extension.
     */
    public static void findWith(final Function<String, Optional<IGuideHighlighter>> finder) {
        FINDERS.add(Objects.requireNonNull(finder, "finder"));
    }

    /**
     * The runs of every line of a listing in that language: as a highlighter registered under its name colours them,
     * or as one a mod's finder finds for it does, or plain when none knows it.
     */
    public static List<List<GuideCode.Run>> colour(final String language, final List<String> lines) {
        final String name = language.toLowerCase(Locale.ROOT);
        final IGuideHighlighter registered = REGISTERED.get(name);
        if (registered != null) {
            return registered.colour(lines);
        }
        for (final Function<String, Optional<IGuideHighlighter>> finder : FINDERS) {
            final Optional<IGuideHighlighter> found = finder.apply(name);
            if (found.isPresent()) {
                return found.get().colour(lines);
            }
        }
        return GuideCode.plain(lines);
    }

    /** How the lines of a listing in one language are coloured. */
    @FunctionalInterface
    public interface IGuideHighlighter {

        /** The runs of every line, in order, as {@link GuideCode#runs} builds them. */
        List<List<GuideCode.Run>> colour(List<String> lines);
    }
}
