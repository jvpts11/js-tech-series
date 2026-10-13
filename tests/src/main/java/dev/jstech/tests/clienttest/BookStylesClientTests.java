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
import dev.jstech.core.client.guide.ManualPlaces;
import dev.jstech.core.client.guide.ManualScreen;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.tests.TestGuide;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;

/**
 * J's Computers' books of the systems and the languages as a player sees them: each closed, at the cover its style
 * draws; each open, in a test book of its look holding the test chapters, on pages bound, papered and marked as the
 * style says; a mark at the edge leading to its chapter; and the ribbon lying across the pages the book was put down
 * at, the game closed in between, and hanging out under the book on any other.
 */
public final class BookStylesClientTests {

    private static final int SETTLE = 4;
    private static final int OPEN = 40;
    private static final List<String> BOOKS = List.of(ComputersGuide.FRAMES_RESOURCE_KIT,
            ComputersGuide.LINUX_DESK_REFERENCE, ComputersGuide.LINUX_BY_HAND, ComputersGuide.DAEMON_HANDBOOK,
            ComputersGuide.LEARNING_IQL, ComputersGuide.BOOK_OF_SIGMA);

    private BookStylesClientTests() {
    }

    @ClientTest(timeoutTicks = 900)
    public static void books_closeToTheCoversTheirStylesDraw(final ClientTestContext ctx) {
        for (final String book : BOOKS) {
            final String name = book.substring(book.indexOf(':') + 1);
            ctx.then(0, () -> GuideClient.open(book, ""))
                    .thenAwaitScreen(ManualScreen.class, OPEN)
                    .then(SETTLE, BookStylesClientTests::toCover)
                    .thenAssert(SETTLE, () -> manual().spread() == -1
                            && manual().style().cover().kind() == GuideStyle.CoverKind.BOOK,
                            name + " closes to a book's cover")
                    .thenScreenshot(2, "book_cover_" + name)
                    .then(0, () -> manual().turn(1))
                    .thenScreenshot(SETTLE, "book_front_" + name)
                    .then(0, BookStylesClientTests::close)
                    .thenAwaitNoScreen(OPEN);
        }
    }

    @ClientTest(timeoutTicks = 900)
    public static void books_openOnPagesMadeTheirOwnWay(final ClientTestContext ctx) {
        for (final String style : TestGuide.BOOK_STYLES) {
            ctx.then(0, () -> GuideClient.open(TestGuide.styledBook(style), TestGuide.PRESS))
                    .thenAwaitScreen(ManualScreen.class, OPEN)
                    .thenAssert(SETTLE, () -> shows(TestGuide.PRESS) && !manual().style().rings()
                            && manual().markPoint(0) != null && manual().markPoint(1) != null,
                            style + ": the test book opens at the press, bound as a book, a mark for each chapter")
                    .thenScreenshot(2, "book_spread_" + style)
                    .then(0, BookStylesClientTests::close)
                    .thenAwaitNoScreen(OPEN);
        }
    }

    /* A thumb index's block and a paper flag each open their chapter, as a binder's tab does. */
    @ClientTest(timeoutTicks = 400)
    public static void bookMarks_openTheirChapters(final ClientTestContext ctx) {
        for (final String style : List.of("daemon_handbook", "linux_by_hand")) {
            ctx.then(0, () -> GuideClient.open(TestGuide.styledBook(style), TestGuide.PRESS))
                    .thenAwaitScreen(ManualScreen.class, OPEN)
                    .then(SETTLE, () -> ctx.click(manual().markPoint(1)[0], manual().markPoint(1)[1]))
                    .thenAssert(SETTLE, () -> shows(TestGuide.SHARED.id()),
                            style + ": the second chapter's mark opens the shared chapter")
                    .thenScreenshot(2, "book_mark_" + style)
                    .then(0, () -> ctx.click(manual().markPoint(0)[0], manual().markPoint(0)[1]))
                    .thenAssert(SETTLE, () -> shows("jstests"), style + ": and the first's, the test chapter")
                    .then(0, BookStylesClientTests::close)
                    .thenAwaitNoScreen(OPEN);
        }
    }

    @ClientTest(timeoutTicks = 400)
    public static void ribbon_keepsThePagesTheBookWasPutDownAt(final ClientTestContext ctx) {
        final String daemon = TestGuide.styledBook("daemon_handbook");
        final AtomicInteger left = new AtomicInteger(-2);
        ctx.then(0, () -> GuideClient.open(daemon, TestGuide.LONG))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, () -> {
                    manual().turn(1);
                    left.set(manual().spread());
                })
                .then(0, BookStylesClientTests::close)
                .thenAwaitNoScreen(OPEN)
                // Forgetting what is held and reading the disk again is the game closed and opened anew.
                .then(0, ManualPlaces::readAgain)
                .then(0, () -> GuideClient.open(daemon, TestGuide.PRESS))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> left.get() > 0 && manual().ribbonSpread().orElse(-2) == left.get()
                        && manual().spread() != left.get(),
                        "opened elsewhere, the ribbon keeps the pages the book was put down at")
                .thenScreenshot(2, "book_ribbon_tail")
                .then(0, () -> manual().turn(left.get() - manual().spread()))
                .thenAssert(SETTLE, () -> manual().spread() == left.get(), "and turned to them, it lies across them")
                .thenScreenshot(2, "book_ribbon_kept")
                .then(0, BookStylesClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    private static ManualScreen manual() {
        return (ManualScreen) Minecraft.getInstance().screen;
    }

    /* A manual opens where it was last closed; this turns it back to its cover. */
    private static void toCover() {
        manual().turn(-manual().spread() - 1);
    }

    /** Whether the spread shown holds the page where that target starts. */
    private static boolean shows(final String target) {
        return manual().showing(manual().book().pageOf(target).orElse(-2));
    }

    private static void close() {
        Minecraft.getInstance().setScreen(null);
    }
}
