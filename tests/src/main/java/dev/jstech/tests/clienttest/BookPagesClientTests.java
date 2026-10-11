/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.guide.ComputersGuide;
import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.client.guide.ManualScreen;
import dev.jstech.core.guide.GuideBook;
import dev.jstech.core.guide.GuidePiece;
import dev.jstech.core.guide.TextSize;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

/**
 * J's Computers' books photographed spread by spread where there is most to look at: the first pages, where each
 * chapter opens, every spread with code, a terminal session, a scene, a picture, a block's views, a plan, recipes or a
 * table, and one spread in every twelve besides, so how the pages lie can be read and checked by eye. That no page
 * cuts a word, runs past its foot or stands empty is proved for every page by the books' GameTests.
 */
public final class BookPagesClientTests {

    private static final int OPEN = 40;
    /** How many ticks a spread is shown before it is photographed. */
    private static final int SETTLE = 2;
    /** One spread in this many is photographed whatever it holds. */
    private static final int EVERY = 12;
    private static final int TIMEOUT = 6000;

    private BookPagesClientTests() {
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void guideToOperations_isPhotographed(final ClientTestContext ctx) {
        photographed(ctx, ComputersGuide.GUIDE_TO_OPERATIONS);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void framesResourceKit_isPhotographed(final ClientTestContext ctx) {
        photographed(ctx, ComputersGuide.FRAMES_RESOURCE_KIT);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void linuxDeskReference_isPhotographed(final ClientTestContext ctx) {
        photographed(ctx, ComputersGuide.LINUX_DESK_REFERENCE);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void linuxByHand_isPhotographed(final ClientTestContext ctx) {
        photographed(ctx, ComputersGuide.LINUX_BY_HAND);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void daemonHandbook_isPhotographed(final ClientTestContext ctx) {
        photographed(ctx, ComputersGuide.DAEMON_HANDBOOK);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void learningIql_isPhotographed(final ClientTestContext ctx) {
        photographed(ctx, ComputersGuide.LEARNING_IQL);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void bookOfSigma_isPhotographed(final ClientTestContext ctx) {
        photographed(ctx, ComputersGuide.BOOK_OF_SIGMA);
    }

    /* Opens the book at its cover, chooses the spreads worth a picture, and walks them, a picture each. */
    private static void photographed(final ClientTestContext ctx, final String book) {
        final String name = book.substring(book.indexOf(':') + 1);
        final Walk walk = new Walk();
        ctx.then(0, () -> GuideClient.open(book, ""))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, () -> {
                    walk.spreads.addAll(worthAPicture(manual().book()));
                    JsTests.LOGGER.info("{}: {} pages, {} spreads photographed", name,
                            manual().book().pages().size(), walk.spreads.size());
                })
                .thenAssert(0, () -> !walk.spreads.isEmpty(), name + " has spreads to photograph")
                .thenWaitUntil(() -> walk.step(ctx, name), TIMEOUT - OPEN * 2, "every chosen spread of " + name)
                .then(0, () -> Minecraft.getInstance().setScreen(null))
                .thenAwaitNoScreen(OPEN);
    }

    /** The spreads of a book worth a picture, in order. */
    private static List<Integer> worthAPicture(final GuideBook book) {
        final List<Integer> out = new ArrayList<>();
        final int spreads = (book.pages().size() + 1) / 2;
        for (int spread = 0; spread < spreads; spread++) {
            final List<GuidePiece> pieces = new ArrayList<>(book.pages().get(spread * 2).pieces());
            if (spread * 2 + 1 < book.pages().size()) {
                pieces.addAll(book.pages().get(spread * 2 + 1).pieces());
            }
            if (spread % EVERY == 0 || pieces.stream().anyMatch(BookPagesClientTests::worthLooking)) {
                out.add(spread);
            }
        }
        return out;
    }

    /** Whether a piece is one whose look is worth checking: a chapter's opening, code, a drawing or a table. */
    private static boolean worthLooking(final GuidePiece piece) {
        return switch (piece) {
            case GuidePiece.Listing listing -> true;
            case GuidePiece.Session session -> true;
            case GuidePiece.Scene scene -> true;
            case GuidePiece.Picture picture -> true;
            case GuidePiece.Views views -> true;
            case GuidePiece.Plan plan -> true;
            case GuidePiece.Recipes recipes -> true;
            // A chapter opens with its number in the largest letters, and a table under a caption of its number.
            case GuidePiece.Text text -> text.size() == TextSize.CHAPTER || text.text().startsWith("Table ")
                    && text.size() != TextSize.BODY;
            default -> false;
        };
    }

    private static ManualScreen manual() {
        return (ManualScreen) Minecraft.getInstance().screen;
    }

    /** The spreads still to photograph, and where the walk stands. */
    private static final class Walk {

        private final List<Integer> spreads = new ArrayList<>();
        private int at;
        private int shownFor;

        /*
         * Shows the next spread, and photographs it once the book is seen to stand there and has a moment; true when
         * every one is done. A book that opened late at the page it was left at is turned again before its picture.
         */
        boolean step(final ClientTestContext ctx, final String name) {
            if (this.at >= this.spreads.size()) {
                return true;
            }
            final int wanted = this.spreads.get(this.at);
            if (manual().spread() != wanted) {
                manual().goToPage(wanted * 2);
                this.shownFor = 0;
                return false;
            }
            if (this.shownFor < SETTLE) {
                this.shownFor++;
                return false;
            }
            ctx.screenshot(name + "_" + String.format(Locale.ROOT, "%03d", this.spreads.get(this.at)));
            this.at++;
            this.shownFor = 0;
            return this.at >= this.spreads.size();
        }
    }
}
