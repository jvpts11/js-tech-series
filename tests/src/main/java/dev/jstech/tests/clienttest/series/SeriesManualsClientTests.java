/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.computers.guide.ComputersGuide;
import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.client.guide.ManualScreen;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideBook;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.gui.layout.ManualScreenLayout;
import dev.jstech.industrial.guide.IndustrialGuide;
import dev.jstech.space.guide.MissionHandbook;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * The series' manuals as a player reads them: the Technical Reference, its cover, its contents, a chapter's opening on
 * a left page and an entry; the Guide to Operations, its own cover and an entry of its one chapter; the Plant
 * Drawings, the folder, its drawing list, a drawing's two sheets with their title block, and the switch that shows a
 * diagram as its blocks and a geological section in its rocks' colours; and the Mission Handbook, its typed cover with
 * the mission's emblem, its tabs one a section and every piece of a mission's document on the test mod's page.
 */
public final class SeriesManualsClientTests {

    private static final int SETTLE = 4;
    private static final int OPEN = 40;
    /** The sheets of the Plant Drawings looked at one by one: the list and every drawing's sheets. */
    private static final int MOST_SHEETS = 32;
    /**
     * How much bluer than red a pixel of the sheet is, and how bright every channel of its ink; and how many pixels
     * of a figure, past those of the sheet, are many, or few.
     */
    private static final int BLUEPRINT = 30;
    private static final int INK = 200;
    private static final int MANY = 300;
    private static final int FEW = 100;
    /** The most sheets a drawing of the set was written for: the Macerator's three, its recipes on the third. */
    private static final int MOST_SHEETS_A_DRAWING = 3;
    /** A part on the plate of the first computer's page, whose own page is the processors'. */
    private static final String PROCESSOR = "jsc:cpu_integra_centro_c5_4690k";
    /* The Mission Handbook's emblem, and where its cover prints it, 32 pixels square. */
    private static final ResourceLocation EMBLEM = ResourceLocation.parse("jsspace:textures/gui/guide/emblem.png");
    private static final int EMBLEM_X = 23;
    private static final int EMBLEM_Y = 13;
    private static final int EMBLEM_SIZE = 32;
    private static final int OPAQUE = 0xFF;
    /* The handbook's own page, the test mod's page with every piece, and the tab of its section, Launch Operations. */
    private static final String THIS_HANDBOOK = "jsspace:this_handbook";
    private static final String LAUNCH_DRILL = "jstests:launch_drill";
    private static final int LAUNCH_OPERATIONS_TAB = 1;

    private SeriesManualsClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void technicalReference_opensEachChapterOnALeftPage(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(CoreGuide.TECHNICAL_REFERENCE, ""))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, SeriesManualsClientTests::toCover)
                .thenAssert(SETTLE, () -> manual().spread() == -1, "the binder closes to its cover")
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
                .then(SETTLE, SeriesManualsClientTests::toCover)
                .thenScreenshot(SETTLE, "series_operations_cover")
                .thenAssert(0, () -> manual().book().pageOf("jsindustrial").isEmpty()
                        && manual().book().pageOf("jsc:personal_computers").isPresent(),
                        "the guide holds the Computers' chapter and no other")
                .thenAssert(0, () -> manual().goTo("jsc:personal_computers") && shows("jsc:personal_computers"),
                        "it opens at an entry")
                .thenScreenshot(SETTLE, "series_operations_entry")
                .thenAssert(0, () -> manual().goTo("jsc:graphics_cards") && shows("jsc:graphics_cards"),
                        "and at the graphics cards, a family of many items")
                .thenScreenshot(SETTLE, "series_operations_family")
                .thenAssert(0, () -> manual().goTo("jsc:case_front") && shows("jsc:case_front"),
                        "and at the front of the case, the page of the drives that go in its bays")
                .thenScreenshot(SETTLE, "series_operations_case_front")
                .then(0, () -> manual().turn(1))
                .thenScreenshot(SETTLE, "series_operations_case_front_2")
                .then(0, SeriesManualsClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 400)
    public static void guideToOperations_showsItsItemsOnAPlateAndItsFirmwareInPictures(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(ComputersGuide.GUIDE_TO_OPERATIONS, "jsc:welcome"))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> shows("jsc:welcome"), "the guide opens at its welcome")
                .thenScreenshot(2, "series_operations_welcome")
                .thenAssert(0, () -> manual().goTo("jsc:first_computer") && shows("jsc:first_computer"),
                        "and turns to building a first computer")
                .thenScreenshot(SETTLE, "series_operations_first_computer")
                .thenWaitUntil(() -> {
                    // The plate turns over every two seconds; the processor is clicked while it is on it.
                    final int[] point = manual().plateItemPoint(PROCESSOR);
                    if (point == null) {
                        return false;
                    }
                    ctx.click(point[0], point[1]);
                    return true;
                }, OPEN * 4, "the processor on the plate to be clicked")
                .thenAssert(SETTLE, () -> shows("jsc:processors"), "the processor leads to the processors' page")
                .thenAssert(0, () -> manual().goTo("jsc:firmware") && shows("jsc:firmware"),
                        "the firmware's entry is in the guide")
                .thenScreenshot(SETTLE, "series_operations_firmware")
                .then(0, () -> manual().turn(1))
                .thenScreenshot(SETTLE, "series_operations_firmware_2")
                .then(0, () -> manual().turn(1))
                .thenScreenshot(SETTLE, "series_operations_firmware_3")
                .then(0, SeriesManualsClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 400)
    public static void plantDrawings_openAtTheDrawingListAndTurnSheetBySheet(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(IndustrialGuide.PLANT_DRAWINGS, ""))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, SeriesManualsClientTests::toCover)
                .thenScreenshot(SETTLE, "series_drawings_folder")
                .then(0, () -> manual().turn(1))
                .thenAssert(SETTLE, () -> manual().book().pages().getFirst().folio().equals("JI-GN-000")
                        && manual().showing(0), "the folder opens at its drawing list, JI-GN-000")
                .thenScreenshot(2, "series_drawings_list")
                .thenAssert(0, () -> manual().goTo("jsindustrial:compressor")
                        && page("jsindustrial:compressor").folio().equals("JI-MC-001"),
                        "the Compressor is JI-MC-001, the first drawing of Machines")
                .thenScreenshot(SETTLE, "series_drawings_compressor_1")
                .then(0, () -> {
                    // Its second sheet faces its first when the drawing starts on a left page; else it is a turn on.
                    final int first = manual().book().pageOf("jsindustrial:compressor").orElse(-1);
                    if (!manual().showing(first + 1)) {
                        manual().turn(1);
                    }
                })
                .thenScreenshot(SETTLE, "series_drawings_compressor_2")
                .thenAssert(0, () -> {
                    final GuideBook book = manual().book();
                    final int first = book.pageOf("jsindustrial:compressor").orElse(-1);
                    // Its recipes grow with the metals the mod presses, so it is two sheets or more.
                    return manual().showing(first + 1) && book.pages().get(first + 1).sheet() == 2
                            && book.pages().get(first).sheets() >= 2;
                }, "its drawing is two sheets or more, the second following the first")
                .thenAssert(0, () -> manual().goTo("jsindustrial:reading_drawings")
                        && page("jsindustrial:reading_drawings").folio().equals("JI-GN-001"),
                        "how to read the drawings is JI-GN-001")
                .thenScreenshot(SETTLE, "series_drawings_reading")
                .then(0, () -> manual().search("comp"))
                .thenAssert(SETTLE, () -> !manual().found().isEmpty(), "the search finds the Compressor")
                .thenScreenshot(2, "series_drawings_search")
                .then(0, SeriesManualsClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 400)
    public static void plantDrawings_switchADiagramToItsBlocksAndASectionToItsColours(final ClientTestContext ctx) {
        final int[] counted = new int[2];
        ctx.then(0, () -> GuideClient.open(IndustrialGuide.PLANT_DRAWINGS, "jsindustrial:single_line_diagrams"))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> manual().switchPoint(1) != null,
                        "the example of a single-line diagram has its switch")
                .then(0, () -> counted[0] = offTheSheet(ctx))
                .thenScreenshot(0, "series_drawings_single_line_diagram")
                .then(0, () -> clickWay(ctx, 1))
                .thenAssert(SETTLE, () -> offTheSheet(ctx) > counted[0] + MANY,
                        "3D shows the plant's blocks, in their own colours, where the diagram was")
                .thenScreenshot(0, "series_drawings_single_line_3d")
                .then(0, () -> clickWay(ctx, 0))
                .thenAssert(SETTLE, () -> offTheSheet(ctx) < counted[0] + FEW, "and DIAGRAM brings the diagram back")
                .thenAssert(0, () -> manual().goTo("jsindustrial:geological_sections")
                        && manual().switchPoint(1) != null, "a geological section has its switch too")
                .then(SETTLE, () -> counted[1] = offTheSheet(ctx))
                .thenScreenshot(0, "series_drawings_section_lines")
                .then(0, () -> clickWay(ctx, 1))
                .thenScreenshot(SETTLE, "series_drawings_section_colours")
                .thenAssert(0, () -> offTheSheet(ctx) > counted[1] + MANY,
                        "COLOURS draws each rock in its own block's colours")
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
        ctx.then(0, () -> {
                    final List<String> wrong = manual().book().pages().stream()
                            .filter(page -> !page.chapter().isEmpty())
                            .filter(page -> page.sheets() > MOST_SHEETS_A_DRAWING || page.pieces().isEmpty())
                            .map(page -> page.title() + " (" + page.folio() + ") on " + page.sheets() + " sheets")
                            .toList();
                    ctx.assertTrue(wrong.isEmpty(), "every drawing fits the sheets it was written for, and no sheet"
                            + " is left empty: " + wrong);
                })
                .then(0, SeriesManualsClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    private static ManualScreen manual() {
        return (ManualScreen) Minecraft.getInstance().screen;
    }

    /* How many pixels of the figure under the first switch shown are not the sheet's own: neither blue nor white. */
    private static int offTheSheet(final ClientTestContext ctx) {
        final int[] at = manual().switchPoint(0);
        try (NativeImage figure = ctx.capture(at[2], at[3], at[4], at[5])) {
            int off = 0;
            for (int y = 0; y < figure.getHeight(); y++) {
                for (int x = 0; x < figure.getWidth(); x++) {
                    final int pixel = figure.getPixelRGBA(x, y);
                    final int red = pixel & 0xFF;
                    final int green = pixel >> 8 & 0xFF;
                    final int blue = pixel >> 16 & 0xFF;
                    final boolean sheet = blue > red + BLUEPRINT && blue >= green;
                    final boolean ink = red > INK && green > INK && blue > INK;
                    if (!sheet && !ink) {
                        off++;
                    }
                }
            }
            return off;
        }
    }

    /* Clicks the word of a way of the first switch shown. */
    private static void clickWay(final ClientTestContext ctx, final int way) {
        final int[] at = manual().switchPoint(way);
        ctx.click(at[0], at[1]);
    }

    /* A manual opens where it was last closed; this turns it back to its cover. */
    @ClientTest(timeoutTicks = 400)
    public static void missionHandbook_isTypedTabbedBySectionAndDressedAsItsCover(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(MissionHandbook.MANUAL, ""))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, SeriesManualsClientTests::toCover)
                .thenAssert(SETTLE, () -> manual().spread() == -1, "the binder closes to its cover")
                .then(2, () -> {
                    // The emblem is printed where an agency printed its own: every pixel it has, on the board.
                    final GuideStyle style = manual().style();
                    final ManualScreenLayout.Geometry geometry = ManualScreenLayout.of(style.pageWidth(),
                            style.pageHeight(), style.spread());
                    final int x = (manual().width - geometry.width()) / 2 + ManualScreenLayout.cover(geometry).x()
                            + EMBLEM_X;
                    final int y = (manual().height - geometry.height()) / 2 + EMBLEM_Y;
                    try (NativeImage drawn = ctx.captureGuiPixels(x, y, EMBLEM_SIZE, EMBLEM_SIZE);
                         NativeImage emblem = read(ctx, EMBLEM);
                         NativeImage expected = new NativeImage(EMBLEM_SIZE, EMBLEM_SIZE, false)) {
                        drawn.copyRect(expected, 0, 0, 0, 0, EMBLEM_SIZE, EMBLEM_SIZE, false, false);
                        for (int py = 0; py < EMBLEM_SIZE; py++) {
                            for (int px = 0; px < EMBLEM_SIZE; px++) {
                                final int pixel = emblem.getPixelRGBA(px, py);
                                if ((pixel >>> 24) == OPAQUE) {
                                    expected.setPixelRGBA(px, py, pixel);
                                }
                            }
                        }
                        ctx.assertSamePicture("handbook-emblem", drawn, expected);
                    }
                })
                .thenScreenshot(0, "space_handbook_cover")
                .then(0, () -> manual().turn(1))
                .thenScreenshot(SETTLE, "space_handbook_contents")
                .thenAssert(0, () -> manual().goTo(THIS_HANDBOOK) && shows(THIS_HANDBOOK)
                        && page(THIS_HANDBOOK).folio().startsWith("GN 1-"),
                        "the handbook's own page is in General, numbered by its section")
                .thenScreenshot(SETTLE, "space_handbook_entry")
                .then(0, () -> {
                    final int[] tab = manual().markPoint(LAUNCH_OPERATIONS_TAB);
                    ctx.assertTrue(tab != null, "Launch Operations has its tab, holding the test mod's page");
                    ctx.click(tab[0], tab[1]);
                })
                .thenAssert(SETTLE, () -> shows(LAUNCH_DRILL) && page(LAUNCH_DRILL).folio().startsWith("LO 2-"),
                        "its tab opens Launch Operations, section 2, its pages numbered LO 2-")
                .thenScreenshot(2, "space_handbook_launch_drill")
                .then(0, () -> manual().turn(1))
                .thenScreenshot(SETTLE, "space_handbook_launch_drill_2")
                .thenAssert(0, () -> manual().goTo(LAUNCH_DRILL) && manual().turn(1) && manual().turn(1),
                        "the cards on rings follow, on the next spread")
                .thenScreenshot(SETTLE, "space_handbook_launch_drill_3")
                .then(0, SeriesManualsClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    private static NativeImage read(final ClientTestContext ctx, final ResourceLocation texture) {
        try (InputStream in = ctx.mc().getResourceManager().getResourceOrThrow(texture).open()) {
            return NativeImage.read(in);
        } catch (final IOException failed) {
            throw new IllegalStateException("the picture " + texture + " could not be read", failed);
        }
    }

    private static void toCover() {
        manual().turn(-manual().spread() - 1);
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
