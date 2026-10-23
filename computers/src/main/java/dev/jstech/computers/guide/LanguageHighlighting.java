/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.api.JsComputersApi;
import dev.jstech.computers.api.language.IProgrammingLanguage;
import dev.jstech.core.guide.GuideCode;
import dev.jstech.core.guide.GuideHighlighters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The manuals colour a listing in one of the machines' languages as the editors of the game colour it: a listing
 * named as one of its files ({@code Hello.sgs}), or naming one of its extensions, is cut into its language's tokens.
 */
final class LanguageHighlighting {

    private LanguageHighlighting() {
    }

    /** Hands the manuals what finds the machines' languages, once, from the mod's guide. */
    static void declare() {
        GuideHighlighters.findWith(name -> Optional.ofNullable(JsComputersApi.languages().byExtension(name))
                .map(language -> lines -> colour(language, lines)));
    }

    private static List<List<GuideCode.Run>> colour(final IProgrammingLanguage language, final List<String> lines) {
        final List<IProgrammingLanguage.Token> tokens;
        try {
            tokens = language.tokenize(String.join("\n", lines));
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
}
