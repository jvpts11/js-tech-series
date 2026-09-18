/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.edit.CodeRuns;
import dev.jstech.core.client.gui.logic.TextDocument;
import dev.jstech.core.language.IProgrammingLanguage;
import java.util.ArrayList;
import java.util.List;

/**
 * The colouring of the file a terminal editor has open, read again only when the text changes.
 *
 * <p>A listing is coloured as a listing, source by whichever language claims the file, and anything else is
 * left plain. Working that out means reading the whole text, which a frame cannot afford and does not need:
 * between two keystrokes the answer is the one already worked out.
 */
final class TtyInk {

    private List<List<CodeRuns.Run>> cached = List.of();
    private String colouredText;

    /** The rows' colouring, a list of runs to a row; empty when nothing claims the file. */
    List<List<CodeRuns.Run>> of(final String path, final TextDocument doc) {
        final String text = doc.text();
        if (text.equals(this.colouredText)) {
            return this.cached;
        }
        final List<String> lines = new ArrayList<>(doc.lineCount());
        for (int i = 0; i < doc.lineCount(); i++) {
            lines.add(doc.line(i));
        }
        final IProgrammingLanguage language = CodeWorkspace.languageOf(path);
        if (CodeWorkspace.isListing(path)) {
            this.cached = CodeWorkspace.colourListing(lines);
        } else if (language == null) {
            this.cached = List.of();
        } else {
            final List<CodeRuns.Span> spans = new ArrayList<>();
            for (final IProgrammingLanguage.Token token : language.tokenize(text)) {
                spans.add(new CodeRuns.Span(token.line(), token.column(), token.length(),
                        switch (token.kind()) {
                            case KEYWORD -> CodeRuns.Ink.KEYWORD;
                            case NAME -> CodeRuns.Ink.NAME;
                            case TEXT -> CodeRuns.Ink.TEXT;
                            case NUMBER -> CodeRuns.Ink.NUMBER;
                            case COMMENT -> CodeRuns.Ink.COMMENT;
                            case SYMBOL -> CodeRuns.Ink.SYMBOL;
                        }));
            }
            this.cached = CodeRuns.byLine(lines, spans);
        }
        this.colouredText = text;
        return this.cached;
    }
}
