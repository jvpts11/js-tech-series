/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.guide;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.client.guide.GuidePalettes;
import dev.jstech.core.gui.ColorContrast;
import dev.jstech.core.palette.Palette;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookPalettesTest {

    /** What body text has to reach against its ground. */
    private static final double BODY = 4.5;
    /** What secondary text (page heads and feet, captions, passing remarks) has to reach. */
    private static final double SECONDARY = 3.0;

    private static final List<Palette<GuidePalettes.Binder>> BOOKS = List.of(BookPalettes.FRAMES_RESOURCE_KIT,
            BookPalettes.LINUX_DESK_REFERENCE, BookPalettes.LINUX_BY_HAND, BookPalettes.DAEMON_HANDBOOK,
            BookPalettes.LEARNING_IQL, BookPalettes.BOOK_OF_SIGMA);

    @Test
    void ink_readsOnThePaperOfEveryBook() {
        for (final Palette<GuidePalettes.Binder> book : BOOKS) {
            assertReads(book, "ink", book.declared().ink(), book.declared().paper(), BODY);
        }
    }

    @Test
    void headingsAndLinks_readOnThePaperOfEveryBook() {
        for (final Palette<GuidePalettes.Binder> book : BOOKS) {
            assertReads(book, "heading", book.declared().heading(), book.declared().paper(), BODY);
            assertReads(book, "link", book.declared().link(), book.declared().paper(), BODY);
        }
    }

    @Test
    void faint_readsOnThePaperOfEveryBook() {
        for (final Palette<GuidePalettes.Binder> book : BOOKS) {
            assertReads(book, "faint", book.declared().faint(), book.declared().paper(), SECONDARY);
        }
    }

    @Test
    void warning_readsOnItsBoxInEveryBook() {
        for (final Palette<GuidePalettes.Binder> book : BOOKS) {
            assertReads(book, "warning", book.declared().warning(), book.declared().highlight(), BODY);
            assertReads(book, "ink in a warning", book.declared().ink(), book.declared().highlight(), BODY);
        }
    }

    @Test
    void code_readsOnTheListingsPaperOfEveryBook() {
        for (final Palette<GuidePalettes.Binder> book : BOOKS) {
            final GuidePalettes.Binder colours = book.declared();
            for (final int ink : new int[] {colours.codeKeyword(), colours.codeName(), colours.codeText(),
                    colours.codeNumber()}) {
                assertReads(book, "code", ink, colours.codePaper(), BODY);
            }
            assertReads(book, "a comment", colours.codeComment(), colours.codePaper(), SECONDARY);
        }
    }

    private static void assertReads(final Palette<GuidePalettes.Binder> book, final String what, final int ink,
                                    final int ground, final double least) {
        final double ratio = ColorContrast.ratio(ink, ground);
        assertTrue(ratio >= least, () -> book.id() + ": " + what + " reads at " + ratio + ", under " + least);
    }
}
