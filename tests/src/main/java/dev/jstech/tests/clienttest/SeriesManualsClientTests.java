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
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideBook;
import dev.jstech.industrial.guide.IndustrialGuide;
import net.minecraft.client.Minecraft;

/**
 * The series' three manuals as a player reads them: the Technical Reference, its cover, its contents, a chapter's
 * opening on a left page and an entry; the Guide to Operations, its own cover and an entry of its one chapter; and
 * the Plant Drawings, the folder, its drawing list, and a drawing's two sheets with their title block.
 */
public final class SeriesManualsClientTests {

    private static final int SETTLE = 4;
    private static final int OPEN = 40;
    /** The sheets of the Plant Drawings looked at one by one: the list and every drawing's sheets. */
    private static final int MOST_SHEETS = 17;
    /** The most sheets a drawing of the set was written for: the Macerator's three, its recipes on the third. */
    private static final int MOST_SHEETS_A_DRAWING = 3;

    private SeriesManualsClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void technicalReference_opensEachChapterOnALeftPage(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(CoreGuide.TECHNICAL_REFERENCE, ""))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> manual().spread() == -1, "the binder opens at its cover")
                .thenScreenshot(2, "series_reference_cover")
                .then(0, () -> manual().turn(1))
                .thenScreenshot(SETTLE, "series_reference_contents")
                .thenAssert(0, () -> manual().goTo("jsc") && opensOnALeftPage("jsc"),
                        "J's Computers opens on a left page, its sections facing it")
                .thenScreenshot(SETTLE, "series_reference_chapter")
                .thenAssert(0, () -> manual().goTo("jstech") && opensOnALeftPage("jstech")
                        && manual().goTo("jsindustrial") && opensOnALeftPage("jsindustrial"),
                        "and so does every other chapter")
                .thenAssert(0, () -> manual().goTo("jsc:graphics_cards") && shows("jsc:graphics_cards"),
                        "an entry of the Computers' chapter is in the series' binder")
                .thenScreenshot(SETTLE, "series_reference_entry")
                .thenAssert(0, () -> manual().goTo("jsindustrial:compressor") && shows("jsindustrial:compressor"),
                        "and so is a drawing of the Industrial's, drawn as a binder's page")
                .thenScreenshot(SETTLE, "series_reference_compressor")
                .then(0, SeriesManualsClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void guideToOperations_holdsTheComputersChapterInItsOwnBinder(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(ComputersGuide.GUIDE_TO_OPERATIONS, ""))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenScreenshot(SETTLE, "series_operations_cover")
                .thenAssert(0, () -> manual().book().pageOf("jsindustrial").isEmpty()
                        && manual().book().pageOf("jsc:personal_computers").isPresent(),
                        "the guide holds the Computers' chapter and no other")
                .thenAssert(0, () -> manual().goTo("jsc:personal_computers") && shows("jsc:personal_computers"),
                        "it opens at an entry")
                .thenScreenshot(SETTLE, "series_operations_entry")
                .then(0, SeriesManualsClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 400)
    public static void plantDrawings_openAtTheDrawingListAndTurnSheetBySheet(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(IndustrialGuide.PLANT_DRAWINGS, ""))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenScreenshot(SETTLE, "series_drawings_folder")
                .then(0, () -> manual().turn(1))
                .thenAssert(SETTLE, () -> manual().book().pages().getFirst().folio().equals("JI-000")
                        && manual().showing(0), "the folder opens at its drawing list, JI-000")
                .thenScreenshot(2, "series_drawings_list")
                .thenAssert(0, () -> manual().goTo("jsindustrial:compressor")
                        && page("jsindustrial:compressor").folio().equals("JI-102"), "the Compressor is JI-102")
                .thenScreenshot(SETTLE, "series_drawings_compressor_1")
                .then(0, () -> manual().turn(1))
                .thenScreenshot(SETTLE, "series_drawings_compressor_2")
                .thenAssert(0, () -> {
                    final GuideBook book = manual().book();
                    final int first = book.pageOf("jsindustrial:compressor").orElse(-1);
                    return manual().showing(first + 1) && book.pages().get(first + 1).sheet() == 2
                            && book.pages().get(first).sheets() == 2;
                }, "its drawing is two sheets, the second following the first")
                .thenAssert(0, () -> manual().goTo("jsindustrial:reading_drawings")
                        && page("jsindustrial:reading_drawings").folio().equals("JI-001"),
                        "how to read the drawings is JI-001")
                .thenScreenshot(SETTLE, "series_drawings_reading")
                .then(0, () -> manual().search("comp"))
                .thenAssert(SETTLE, () -> !manual().found().isEmpty(), "the search finds the Compressor")
                .thenScreenshot(2, "series_drawings_search")
                .then(0, SeriesManualsClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 600)
    public static void plantDrawings_fitEachDrawingOnTheSheetsItWasWrittenFor(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(IndustrialGuide.PLANT_DRAWINGS, ""))
                .thenAwaitScreen(ManualScreen.class, OPEN);
        for (int sheet = 0; sheet < MOST_SHEETS; sheet++) {
            final int page = sheet;
            ctx.then(SETTLE, () -> manual().goToPage(Math.min(page, manual().book().pages().size() - 1)))
                    .thenScreenshot(2, "series_drawings_sheet_" + page);
        }
        ctx.thenAssert(0, () -> manual().book().pages().stream().filter(page -> !page.chapter().isEmpty())
                        .allMatch(page -> page.sheets() <= MOST_SHEETS_A_DRAWING && !page.pieces().isEmpty()),
                        "every drawing fits the sheets it was written for, and no sheet is left empty")
                .then(0, SeriesManualsClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    private static ManualScreen manual() {
        return (ManualScreen) Minecraft.getInstance().screen;
    }

    private static GuideBook.Page page(final String target) {
        final GuideBook book = manual().book();
        return book.pages().get(book.pageOf(target).orElseThrow());
    }

    /** Whether the spread shown holds the page where that target starts. */
    private static boolean shows(final String target) {
        return manual().showing(manual().book().pageOf(target).orElse(-2));
    }

    /** Whether the chapter opens on a left page, shown now, with the page of its sections facing it. */
    private static boolean opensOnALeftPage(final String chapter) {
        final int page = manual().book().pageOf(chapter).orElse(-1);
        return page >= 0 && page % 2 == 0 && manual().showing(page);
    }

    private static void close() {
        Minecraft.getInstance().setScreen(null);
    }
}
