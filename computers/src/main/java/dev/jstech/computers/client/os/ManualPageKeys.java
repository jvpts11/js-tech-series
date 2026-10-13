/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.help.HelpLine;
import dev.jstech.computers.gui.help.HelpPages;
import dev.jstech.computers.gui.help.HelpTexts;
import dev.jstech.computers.gui.help.HelpViews;
import dev.jstech.computers.os.edit.TtyLook;
import dev.jstech.core.client.guide.GuideRecipes;
import dev.jstech.core.guide.ManualReader;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * man reading an entry of a manual on UNIX and FreeBSD: the entry laid out as a manual page of section 7, its headings
 * in capitals at the margin and its text set in under them, read through the pager as every page is.
 *
 * <p>The page is the entry the binder prints, in the player's language, laid out again for the width of the glass
 * when the glass changes size; the keys are the pager's.
 */
public final class ManualPageKeys implements TtyEditor.IKeys {

    private final LessKeys pager = new LessKeys();
    private final GuideRecipes recipes = new GuideRecipes();
    private int laidFor = -1;

    @Override
    public TtyLook look(final TtyEditor editor) {
        return this.pager.look(editor);
    }

    @Override
    public void opened(final TtyEditor editor, final boolean existed) {
        this.lay(editor, editor.columns());
    }

    @Override
    public void resized(final TtyEditor editor, final int columns, final int rows) {
        if (columns != this.laidFor) {
            this.lay(editor, columns);
        }
    }

    @Override
    public String status(final TtyEditor editor) {
        final int line = editor.document().cursorLine() + 1;
        final int of = Math.max(1, editor.document().lineCount());
        return GameText.resolve(HelpTexts.MAN_STATUS.with(HelpPages.manName(this.entry(editor)), line, of,
                line * 100 / of));
    }

    @Override
    public boolean key(final TtyEditor editor, final int key, final int modifiers) {
        return this.pager.key(editor, key, modifiers);
    }

    @Override
    public boolean typed(final TtyEditor editor, final char c) {
        return this.pager.typed(editor, c);
    }

    /** Lays the entry out as a page for that many columns, and puts it in the pager from its top. */
    private void lay(final TtyEditor editor, final int columns) {
        this.laidFor = columns;
        final String entry = this.entry(editor);
        final List<String> lines = new ArrayList<>();
        final Optional<ManualReader.Article> found = this.article(entry);
        if (found.isEmpty()) {
            lines.add(GameText.resolve(HelpTexts.MAN_NO_ENTRY.with(HelpPages.manName(entry))));
        } else {
            final ManualReader reader = HelpBooks.all().stream()
                    .filter(manual -> manual.article(entry).isPresent()).findFirst().orElseThrow();
            for (final HelpLine line : HelpPages.article(reader, found.get(), HelpPages.Voice.MAN, columns,
                    GameText.LOADED, (type, output) -> HelpSource.recipeLines(this.recipes, type, output))) {
                lines.add(line.text());
            }
        }
        editor.document().setText(String.join("\n", lines));
        editor.document().setCursor(0, 0);
    }

    private String entry(final TtyEditor editor) {
        return HelpViews.namesManual(editor.path()) ? HelpViews.manualEntry(editor.path()) : editor.path();
    }

    /** The entry in the first manual holding it, the series' own first. */
    private Optional<ManualReader.Article> article(final String entry) {
        for (final ManualReader reader : HelpBooks.all()) {
            final Optional<ManualReader.Article> found = reader.article(entry);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }
}
