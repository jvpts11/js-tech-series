/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import com.mojang.datafixers.util.Either;
import dev.jstech.computers.guide.ComputersGuide;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.client.audio.AudioMixer;
import dev.jstech.core.client.gui.PointerShape;
import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.client.guide.ManualMarks;
import dev.jstech.core.client.guide.ManualPlaces;
import dev.jstech.core.client.guide.ManualScreen;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.client.multiblock.MultiblockShows;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideBook;
import dev.jstech.core.guide.GuideLayout;
import dev.jstech.core.guide.GuidePiece;
import dev.jstech.core.guide.GuideSearch;
import dev.jstech.core.guide.GuideSounds;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.guide.TextSize;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.text.GameText;
import dev.jstech.industrial.guide.IndustrialGuide;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.TestGuide;
import dev.jstech.tests.TestPress;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.clienttest.TestGuideClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * The Core's manuals as a player reads them: the test manual opened at its cover and turned to its contents, opened
 * at an entry, a link and a tab followed, the whole manual searched as the player types, Back and Forward with the
 * buttons, the mouse and Backspace, the hand over what takes a click, a long entry run on to the pages after its
 * first, a table run on and a table stacked, a scene turned and brought nearer, the manual opened again where it was
 * closed after the game was, an item's page opened by the manual key held over it while a bar in the manual's look
 * fills, and the manual item opening its manual.
 */
public final class ManualClientTests {

    private static final int SETTLE = 4;
    private static final int OPEN = 40;
    /** What the reader writes in a page's margin. */
    private static final String NOTE = "Feed it from the top";
    /* Far enough into a hold for its bar to show, and early enough to see it before the page opens. */
    private static final float HALF_HELD = 0.35F;

    private ManualClientTests() {
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_closesToItsCoverAndTurnsToItsContents(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, ""))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, () -> manual().turn(-manual().spread() - 1))
                .thenAssert(SETTLE, () -> manual().spread() == -1, "the manual turns back to its cover")
                .thenScreenshot(2, "manual_cover")
                .then(0, () -> manual().turn(1))
                .thenAssert(SETTLE, () -> manual().spread() == 0
                        && manual().book().pages().getFirst().folio().equals("i"), "and turns to its contents")
                .thenScreenshot(2, "manual_contents")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_opensAtTheEntryAskedFor(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.PRESS))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> shows(TestGuide.PRESS), "the manual opens at the press's page")
                .thenScreenshot(2, "manual_entry")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_followsALinkAndAChaptersTab(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.PRESS))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> manual().goTo(TestGuide.SWATCH) && shows(TestGuide.SWATCH),
                        "the press's link leads to the swatch's page")
                .then(0, () -> TestGuideClient.SWATCHES_DRAWN.set(0))
                .thenWaitUntil(() -> TestGuideClient.SWATCHES_DRAWN.get() > 0, OPEN,
                        "the special block is drawn by the test mod's renderer")
                .thenScreenshot(2, "manual_swatch")
                .thenAssert(0, () -> manual().goTo("jstests") && shows("jstests"),
                        "the chapter's tab leads to the chapter's opening page")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_searchFindsAsItsReaderTypes(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, ""))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, () -> manual().search("press"))
                .thenAssert(SETTLE, () -> {
                    final List<GuideSearch.Hit> found = manual().found();
                    return manual().searching() && !found.isEmpty() && found.getFirst().line().isEmpty()
                            && found.stream().allMatch(hit -> (hit.text() + " " + hit.line())
                            .toLowerCase(Locale.ROOT).contains("press"));
                }, "the search finds the press by its name first, and only what holds the word")
                .thenScreenshot(2, "manual_search")
                .then(0, () -> manual().search("mkdir"))
                .thenAssert(SETTLE, () -> manual().found().stream().anyMatch(hit -> hit.target().equals(
                        TestGuide.TABLES) && hit.line().equals("mkdir")),
                        "a command in a table is found, with the line it stands on")
                .thenScreenshot(2, "manual_search_words")
                .then(0, () -> manual().search("nothing by this name"))
                .thenAssert(SETTLE, () -> manual().found().isEmpty(), "and nothing for a name no entry has")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_listsCodeColouredAndCopiesItWhole(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.CODE))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> codePiece("using").colour().equals(GuideStyle.CODE_KEYWORD)
                        && codePiece("QUERY").colour().equals(GuideStyle.CODE_KEYWORD),
                        "Sigma# is coloured as the game's editors colour it, and so is the network's query language")
                .thenAssert(0, () -> manual().book().pages().get(manual().book().pageOf(TestGuide.CODE).orElseThrow())
                        .pieces().stream().anyMatch(piece -> piece instanceof GuidePiece.Key key
                                && key.name().equals("Page Down")), "a key in a sentence is drawn as a key")
                .thenAssert(0, () -> {
                    final GuidePiece.Listing hello = listingPiece(TestGuide.HELLO);
                    final int rows = (hello.height() - GuideLayout.LISTING_HEAD - 2 * GuideLayout.CODE_PAD)
                            / GuideLayout.CODE_PITCH;
                    return rows > hello.lines().size();
                }, "the line too long for the page goes on under itself")
                .thenScreenshot(2, "manual_code")
                .then(0, () -> ctx.click(manual().listingPoint(TestGuide.HELLO, true)[0],
                        manual().listingPoint(TestGuide.HELLO, true)[1]))
                .thenAssert(SETTLE, () -> Minecraft.getInstance().keyboardHandler.getClipboard().equals(String.join(
                        "\n", listingPiece(TestGuide.HELLO).lines())), "Copy puts the whole listing on the clipboard")
                .then(0, () -> ctx.click(manual().listingPoint(TestGuide.HELLO, false)[0],
                        manual().listingPoint(TestGuide.HELLO, false)[1]))
                .thenAssert(SETTLE, () -> manual().listingOpen(), "a click on a listing opens it over the book")
                .thenScreenshot(2, "manual_code_open")
                .then(0, () -> {
                    for (int i = 0; i < 4; i++) {
                        manual().mouseScrolled(0.0, 0.0, -1.0, 0.0);
                    }
                })
                .thenScreenshot(2, "manual_code_open_across")
                .thenAssert(0, () -> manual().keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0) && !manual().listingOpen()
                        && Minecraft.getInstance().screen instanceof ManualScreen,
                        "and Escape puts it away, the book still open")
                .thenAssert(0, () -> {
                    final GuideBook book = manual().book();
                    return book.pages().subList(book.pageOf(TestGuide.CODE).orElseThrow(), book.indexAt()).stream()
                            .flatMap(page -> page.pieces().stream()).anyMatch(piece -> piece
                                    instanceof GuidePiece.Session session && session.title().equals("MC-DOS 6.22")
                                    && session.rows().getFirst().getFirst().ink() == GuidePiece.SessionInk.PROMPT);
                }, "a session at MC-DOS is printed, its first line opening with the prompt")
                .then(0, () -> manual().turn(1))
                .thenScreenshot(2, "manual_code_sessions")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_printsAGlossaryOfEveryWordItExplains(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(ComputersGuide.GUIDE_TO_OPERATIONS, GuideLayout.GLOSSARY))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> shows(GuideLayout.GLOSSARY) && manual().book().pages()
                        .get(manual().book().pageOf(GuideLayout.GLOSSARY).orElseThrow()).folio().equals("GL-1"),
                        "the Guide to Operations opens at its glossary, page GL-1")
                .thenScreenshot(2, "manual_glossary")
                .then(0, () -> manual().turn(1))
                .thenScreenshot(2, "manual_glossary_on")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_turnsASceneDraggedAndBringsItNearerWithTheWheel(final ClientTestContext ctx) {
        final String scene = TestGuide.SCENE + "#1";
        final GuideBlock.SceneView book = new GuideBlock.SceneView(-30, 25, 1);
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.SCENE))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, () -> toScene(scene))
                .thenAssert(SETTLE, () -> manual().scenePoint(scene, false) != null
                        && manual().sceneView(scene).orElseThrow().equals(book),
                        "the scene is drawn as the book shows it")
                .thenScreenshot(2, "manual_scene")
                .then(0, () -> {
                    final int[] at = manual().scenePoint(scene, false);
                    manual().mouseClicked(at[0], at[1], 0);
                    manual().mouseDragged(at[0] + 40, at[1] + 10, 0, 40, 10);
                    manual().mouseReleased(at[0] + 40, at[1] + 10, 0);
                })
                .thenAssert(SETTLE, () -> manual().sceneView(scene).orElseThrow().yaw() != book.yaw()
                        && manual().sceneView(scene).orElseThrow().pitch() != book.pitch(),
                        "dragged across and down, it turns round and tilts")
                .then(0, () -> {
                    final int[] at = manual().scenePoint(scene, false);
                    manual().mouseScrolled(at[0], at[1], 0.0, 1.0);
                })
                .thenAssert(SETTLE, () -> manual().sceneView(scene).orElseThrow().zoom() > book.zoom(),
                        "the wheel over it brings it nearer, and turns no page")
                .thenScreenshot(2, "manual_scene_turned")
                .then(0, () -> ctx.click(manual().scenePoint(scene, true)[0], manual().scenePoint(scene, true)[1]))
                .thenAssert(SETTLE, () -> manual().sceneView(scene).orElseThrow().equals(book),
                        "Reset brings back the book's own view")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_peelsASceneLayerByLayer(final ClientTestContext ctx) {
        final String scene = TestGuide.SCENE + "#1";
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.SCENE))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, () -> toScene(scene))
                .thenAssert(SETTLE, () -> manual().sceneLayers(scene).orElse(0) == 2,
                        "the scene shows both its layers, the grass and what stands on it")
                .then(0, () -> ctx.click(manual().sceneLayerPoint(scene, false)[0],
                        manual().sceneLayerPoint(scene, false)[1]))
                .thenAssert(SETTLE, () -> manual().sceneLayers(scene).orElse(0) == 1,
                        "the minus hides the top layer, the furnace and the chest")
                .thenScreenshot(2, "manual_scene_layer")
                .then(0, () -> ctx.click(manual().sceneLayerPoint(scene, true)[0],
                        manual().sceneLayerPoint(scene, true)[1]))
                .thenAssert(SETTLE, () -> manual().sceneLayers(scene).orElse(0) == 2, "and the plus shows it again")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 600)
    public static void manual_playsASceneStepByStepAndShowsItInTheWorld(final ClientTestContext ctx) {
        final String scene = TestGuide.STEPS + "#1";
        ctx.afterTest(() -> MultiblockShows.shownInWorld().ifPresent(MultiblockShows::toggle));
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.STEPS))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, () -> toScene(scene))
                .thenAssert(SETTLE, () -> manual().sceneStep(scene).orElse(-1) == 0,
                        "the scene starts at its first step")
                .then(0, () -> {
                    if (!manual().scenePlaying(scene)) {
                        // Client tests ask for less movement, so the scene waits for Play.
                        final int[] play = manual().sceneTimelinePoint(scene, 1);
                        ctx.click(play[0], play[1]);
                    }
                })
                .thenWaitUntil(() -> manual().sceneStep(scene).orElse(-1) == 1, TestGuide.STEP_TICKS * 3,
                        "the scene plays on to its second step")
                .thenScreenshot(2, "manual_steps_second")
                .thenWaitUntil(() -> manual().sceneStep(scene).orElse(-1) == 2 && !manual().scenePlaying(scene),
                        TestGuide.STEP_TICKS * 3, "it stops at its last step")
                .thenScreenshot(2, "manual_steps_last")
                .then(0, () -> ctx.click(manual().sceneTimelinePoint(scene, 0)[0],
                        manual().sceneTimelinePoint(scene, 0)[1]))
                .thenAssert(SETTLE, () -> manual().sceneStep(scene).orElse(-1) == 1, "the step before goes back")
                .then(0, () -> {
                    final int[] world = manual().sceneInWorldPoint(scene);
                    ctx.assertTrue(world != null, "a scene has In world, the projector being installed");
                    ctx.click(world[0], world[1]);
                })
                .thenAssert(SETTLE, () -> MultiblockShows.shownInWorld().isPresent(),
                        "In world shows the scene's blocks in front of the player")
                .then(0, () -> ctx.click(manual().sceneInWorldPoint(scene)[0], manual().sceneInWorldPoint(scene)[1]))
                .thenAssert(SETTLE, () -> MultiblockShows.shownInWorld().isEmpty(), "and a second click puts it away")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 600)
    public static void manual_keepsTheReadersBookmarksNotesAndWhatIsNew(final ClientTestContext ctx) {
        ctx.afterTest(() -> ManualMarks.forget(TestGuide.MANUAL));
        ctx.then(0, () -> ManualMarks.forget(TestGuide.MANUAL))
                .then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.SCENE))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> ManualMarks.seen(TestGuide.MANUAL).contains(TestGuide.STEPS),
                        "opened the first time, what the manual holds is what the reader finds there")
                .then(0, () -> {
                    // The scene in steps comes into the book after the reader last read it.
                    ManualMarks.unsee(TestGuide.MANUAL, TestGuide.STEPS);
                    manual().goToPage(manual().book().contents());
                })
                .thenScreenshot(SETTLE, "manual_marks_contents")
                .thenAssert(0, () -> !ManualMarks.seen(TestGuide.MANUAL).contains(TestGuide.STEPS),
                        "the contents do not see the entry for the reader")
                .then(0, () -> ctx.click(manual().bookmarksButtonPoint()[0], manual().bookmarksButtonPoint()[1]))
                .thenAssert(SETTLE, () -> manual().bookmarkListOpen(), "the ribbon button opens the list")
                .then(0, () -> ctx.click(manual().markPagePoint()[0], manual().markPagePoint()[1]))
                .thenAssert(SETTLE, () -> ManualMarks.bookmarks(TestGuide.MANUAL).size() == 1,
                        "Mark this page puts a ribbon on the page shown")
                .thenScreenshot(2, "manual_marks_list")
                .then(0, () -> ctx.click(manual().pagePoint(true)[0], manual().pagePoint(true)[1]))
                .thenAssert(SETTLE, () -> !manual().bookmarkListOpen(), "a click off the list closes it")
                .then(0, () -> {
                    final int[] page = manual().pagePoint(true);
                    manual().mouseClicked(page[0], page[1], GLFW.GLFW_MOUSE_BUTTON_RIGHT);
                })
                .thenAssert(SETTLE, () -> manual().writingNote(), "a right click on a page writes a note there")
                .then(0, () -> {
                    for (final char letter : NOTE.toCharArray()) {
                        manual().charTyped(letter, 0);
                    }
                })
                .thenScreenshot(2, "manual_marks_note_writing")
                .then(0, () -> manual().keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0))
                .thenAssert(SETTLE, () -> !manual().writingNote() && ManualMarks.bookmarks(TestGuide.MANUAL)
                        .stream().findFirst().map(bookmark -> ManualMarks.note(TestGuide.MANUAL, bookmark.place())
                                .isEmpty()).orElse(false), "Enter keeps the note, on the right page, not the left")
                .thenScreenshot(2, "manual_marks_note")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN)
                .then(0, ManualMarks::readAgain)
                .thenAssert(0, () -> ManualMarks.bookmarks(TestGuide.MANUAL).size() == 1,
                        "the bookmark is kept on the disk, the game closed and opened in between");
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_drawsAStructureSavedWithTheStructureBlock(final ClientTestContext ctx) {
        final String scene = TestGuide.STRUCTURE + "#1";
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.STRUCTURE))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, () -> toScene(scene))
                .thenAssert(SETTLE, () -> manual().scenePoint(scene, false) != null
                        && manual().sceneLayers(scene).orElse(0) == 4,
                        "the hut is drawn from its file, its floor, two rows of walls and its roof")
                .thenScreenshot(2, "manual_structure")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_showsAMovingPicturesFramesInTurn(final ClientTestContext ctx) {
        final boolean reduced = MotionClock.reduced();
        ctx.afterTest(() -> MotionClock.setReduced(reduced));
        ctx.then(0, () -> {
            MotionClock.setReduced(false);
            GuideClient.open(TestGuide.MANUAL, TestGuide.ANIMATION);
        }).thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> manual().book().pages().stream().flatMap(page -> page.pieces().stream())
                        .anyMatch(piece -> piece instanceof GuidePiece.Picture picture && picture.frames() == 2
                                && picture.frameTicks() == TestGuide.ANIMATION_TICKS),
                        "the picture is laid out as a strip of two frames")
                .thenScreenshot(0, "manual_animation_a")
                .thenScreenshot(TestGuide.ANIMATION_TICKS, "manual_animation_b")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_printsACommandsPageFromTheCommand(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.COMMAND))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> shows(TestGuide.COMMAND) && manual().book().pages().stream()
                        .flatMap(page -> page.pieces().stream())
                        .anyMatch(piece -> piece instanceof GuidePiece.Text text && text.text().contains("dir")),
                        "the command's page is printed from MC-DOS's dir")
                .thenScreenshot(2, "manual_command")
                .then(0, () -> manual().turn(1))
                .thenScreenshot(SETTLE, "manual_command_on")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_soundsLikeABook(final ClientTestContext ctx) {
        final long[] since = {0L};
        ctx.then(0, () -> since[0] = Util.getMillis())
                .then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.PRESS))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> heard(GuideSounds.OPEN, since[0]), "the manual is heard opening")
                .then(0, () -> manual().turn(1))
                .thenAssert(SETTLE, () -> heard(GuideSounds.PAGE, since[0]), "a page is heard turning")
                .then(0, () -> ctx.click(manual().contentsButtonPoint()[0], manual().contentsButtonPoint()[1]))
                .thenAssert(SETTLE, () -> heard(GuideSounds.CLICK, since[0]), "the contents button clicks")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN)
                .thenAssert(SETTLE, () -> heard(GuideSounds.CLOSE, since[0]), "and the manual is heard closing");
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_backAndForwardReturnWhereTheReaderJumpedFrom(final ClientTestContext ctx) {
        final AtomicInteger from = new AtomicInteger(-2);
        final AtomicInteger to = new AtomicInteger(-2);
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.PRESS))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, () -> {
                    from.set(manual().spread());
                    manual().goTo(TestGuide.TABLES);
                    to.set(manual().spread());
                })
                .thenAssert(0, () -> to.get() != from.get() && manual().back() && manual().spread() == from.get(),
                        "Back returns to the press's page the link was followed from")
                .thenAssert(0, () -> manual().forward() && manual().spread() == to.get(),
                        "and Forward comes back to the tables")
                .thenAssert(0, () -> manual().mouseClicked(0.0, 0.0, GLFW.GLFW_MOUSE_BUTTON_4)
                        && manual().spread() == from.get(), "the mouse's back button goes back too")
                .then(0, () -> manual().goTo(TestGuide.TABLES))
                .thenAssert(0, () -> manual().keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, 0)
                        && manual().spread() == from.get(), "and so does Backspace")
                .then(0, () -> manual().goTo(TestGuide.TABLES))
                .then(0, () -> ctx.pointAt(manual().backButtonPoint()[0], manual().backButtonPoint()[1]))
                .thenAssert(2, () -> manual().pointing(), "the pointer is a hand over Back, which leads somewhere")
                .thenScreenshot(1, "manual_back_hover")
                .then(0, () -> ctx.click(manual().backButtonPoint()[0], manual().backButtonPoint()[1]))
                .thenAssert(SETTLE, () -> manual().spread() == from.get() && !manual().back(),
                        "clicking Back goes back, and then there is nowhere further back")
                .then(0, () -> ctx.pointAt(manual().backButtonPoint()[0], manual().backButtonPoint()[1]))
                .thenAssert(2, () -> !manual().pointing(), "Back leading nowhere takes no click, and shows no hand")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN)
                .thenAssert(0, () -> !PointerShape.pointing(), "the arrow comes back when the manual closes");
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_runsALongEntryOnToThePagesAfterItsFirst(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.LONG))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> {
                    final GuideBook book = manual().book();
                    final int first = book.pageOf(TestGuide.LONG).orElse(-1);
                    return first >= 0 && first + 1 < book.pages().size()
                            && book.pages().get(first + 1).chapter().equals("jstests")
                            && !book.pages().get(first + 1).pieces().isEmpty();
                }, "the long entry runs on to its next page")
                .then(0, () -> manual().turn(1))
                .thenScreenshot(2, "manual_long")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_laysATableThatRunsOnAndOneThatStacks(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.TABLES))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> tableText("dir").size() == TextSize.CODE,
                        "a command is written in the code font")
                .thenAssert(0, () -> tablePieces().stream().anyMatch(text -> text.text().contains(
                        GameText.resolve(GuideTexts.TABLE_CONTINUED.with("")).strip())),
                        "the table of commands runs on under its caption, continued")
                .thenAssert(0, () -> tableText("S1001").colour().equals(GuideStyle.HEADING),
                        "the table of errors is stacked, each row opening with its code")
                .thenAssert(0, ManualClientTests::nothingBelowTheFoot, "and no table runs past a page's foot")
                .thenAssert(0, () -> String.join(" ", tablePieces().stream().map(GuidePiece.Text::text).toList())
                        .contains("spends " + TestPress.ENERGY_PER_TICK + " FE/t while")
                        && tablePieces().stream().noneMatch(text -> text.text().contains("{{")),
                        "a sentence reads the press's figure from the test mod as the page is laid out")
                .thenAssert(0, () -> tableText(TestGuide.MANUAL_ITEM.get().getDescription().getString())
                        .size() == TextSize.SMALL, "and a table reads every item of the test mod from the game")
                .thenScreenshot(2, "manual_tables")
                .then(0, () -> manual().turn(1))
                .thenScreenshot(2, "manual_tables_on")
                .then(0, () -> manual().turn(1))
                .thenScreenshot(2, "manual_tables_read")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manualKey_opensTheItemsPageAndItsTooltipSaysSo(final ClientTestContext ctx) {
        ctx.thenAssert(0, () -> {
                    final Minecraft minecraft = Minecraft.getInstance();
                    final List<Component> lines = new ItemStack(TestPress.UPGRADE.get()).getTooltipLines(
                            Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.Default.NORMAL);
                    final String hold = GameText.resolve(GuideTexts.HOLD_TO_OPEN.with("M"));
                    return lines.stream().anyMatch(line -> line.getString().equals(hold));
                }, "the upgrade's tooltip says the key opens its page")
                .thenAssert(0, () -> GuideClient.openPageOf(new ItemStack(TestPress.UPGRADE.get())),
                        "the upgrade has a page")
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> shows(TestGuide.PRESS), "and the key opens the manual there")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_opensAgainWhereItWasClosed(final ClientTestContext ctx) {
        final AtomicInteger left = new AtomicInteger(-2);
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.LONG))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, () -> {
                    manual().turn(1);
                    left.set(manual().spread());
                })
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN)
                // Forgetting what is held and reading the disk again is the game closed and opened anew.
                .then(0, ManualPlaces::readAgain)
                .then(0, () -> GuideClient.open(TestGuide.MANUAL, ""))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> left.get() > 0 && manual().spread() == left.get(),
                        "the manual opens at the spread it was closed at, the game closed in between")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manualKey_heldOverTheItemInHandFillsABarAndOpensItsPage(final ClientTestContext ctx) {
        ctx.thenGive(0, new ItemStack(TestPress.UPGRADE.get()))
                .then(2, () -> ctx.selectHotbar(0))
                .then(2, () -> manualKey().setDown(true))
                .thenWaitUntil(() -> GuideClient.holdProgress() > HALF_HELD, OPEN,
                        "the bar fills while the key is held")
                .thenScreenshot(0, "manual_hold_bar_plain")
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> shows(TestGuide.PRESS), "and the item's page opens when it is full")
                .then(0, () -> manualKey().setDown(false))
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN)
                .thenGive(0);
    }

    /*
     * In an inventory the bar fills inside the item's tooltip, right under the line that says to hold the key; it
     * was once drawn under the slot, outside the tooltip.
     */
    @ClientTest(timeoutTicks = 300)
    public static void manualKey_heldOverAnItemInAnInventoryFillsTheBarInItsTooltip(final ClientTestContext ctx) {
        final ItemStack upgrade = new ItemStack(TestPress.UPGRADE.get());
        ctx.thenGive(0, upgrade)
                .thenWaitUntil(() -> ctx.player().getInventory().getItem(0).is(TestPress.UPGRADE.get()), OPEN,
                        "the upgrade to reach the player's hotbar")
                // The test player is in creative, where the inventory opens as the creative one.
                .then(0, () -> ctx.mc().setScreen(new InventoryScreen(ctx.player())))
                .thenAwaitScreen(AbstractContainerScreen.class, OPEN)
                .then(SETTLE, () -> {
                    final AbstractContainerScreen<?> screen = ctx.screen(AbstractContainerScreen.class);
                    final Slot slot = screen.getMenu().slots.stream()
                            .filter(s -> s.container == ctx.player().getInventory() && s.getContainerSlot() == 0)
                            .findFirst().orElseThrow(() -> new ClientTestFailure("no slot for the first hotbar slot"));
                    ctx.pointAt(screen.getGuiLeft() + slot.x + 8.5, screen.getGuiTop() + slot.y + 8.5);
                })
                .then(2, () -> manualKey().setDown(true))
                .thenWaitUntil(() -> GuideClient.holdProgress() > HALF_HELD, OPEN,
                        "the bar fills while the key is held over the slot")
                .thenAssert(0, () -> barUnderHoldLine(upgrade), "and it is a line of the tooltip, under the key's")
                .thenScreenshot(0, "manual_hold_bar_tooltip")
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> shows(TestGuide.PRESS), "and the item's page opens when it is full")
                .then(0, () -> manualKey().setDown(false))
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN)
                .thenGive(0);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manualKey_fillsTheBarOfTheManualItOpens(final ClientTestContext ctx) {
        ctx.thenGive(0, new ItemStack(ComputersGuide.MANUAL.get()), new ItemStack(IndustrialGuide.MANUAL.get()))
                .then(2, () -> ctx.selectHotbar(0))
                .then(2, () -> manualKey().setDown(true))
                .thenWaitUntil(() -> GuideClient.holdProgress() > HALF_HELD, OPEN, "the Guide to Operations' bar fills")
                .thenScreenshot(0, "manual_hold_bar_blocks")
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(0, () -> manualKey().setDown(false))
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN)
                .then(2, () -> ctx.selectHotbar(1))
                .then(2, () -> manualKey().setDown(true))
                .thenWaitUntil(() -> GuideClient.holdProgress() > HALF_HELD, OPEN, "the Plant Drawings' bar fills")
                .thenScreenshot(0, "manual_hold_bar_hazard")
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> manual().manual().id().equals(IndustrialGuide.PLANT_DRAWINGS),
                        "and the drawings open")
                .then(0, () -> manualKey().setDown(false))
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN)
                .thenGive(0);
    }

    /*
     * A book holds the chapters it names: a link to a page only another book holds says which book it is, and a click
     * on it opens that book there; that book prints the chapter it names first as its chapter 1.
     */
    @ClientTest(timeoutTicks = 300)
    public static void manual_linkToAPageOfAnotherBookOpensThatBookThere(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.OTHER_BOOK))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> shows(TestGuide.OTHER_BOOK)
                                && manual().book().pageOf(TestGuide.SHARED_PAGE).isEmpty(),
                        "the test manual opens at the link's entry, and does not hold the shared page")
                .thenAssert(0, () -> linkWords(TestGuide.OTHER_BOOK, TestGuide.SHARED_PAGE).contains(
                        "Second Test Book"), "the link names the book that holds its page")
                .thenScreenshot(2, "manual_link_to_another_book")
                .then(0, () -> {
                    final int[] at = manual().linkPoint(TestGuide.SHARED_PAGE);
                    if (at == null) {
                        throw new ClientTestFailure("the spread shows no link to the shared page");
                    }
                    manual().mouseClicked(at[0], at[1], 0);
                })
                .thenAssert(SETTLE, () -> manual().manual().id().equals(TestGuide.SECOND_BOOK)
                        && shows(TestGuide.SHARED_PAGE), "the click opens the second book at the shared page")
                .thenAssert(0, () -> manual().book().pages().get(manual().book().pageOf(TestGuide.SHARED.id())
                        .orElseThrow()).folio().equals("1-1"), "the second book prints the shared chapter first")
                .thenScreenshot(2, "manual_second_book")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN)
                .then(0, () -> GuideClient.open(CoreGuide.TECHNICAL_REFERENCE, ""))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> manual().book().pageOf(TestGuide.SHARED.id()).isEmpty()
                                && manual().book().pageOf("jstests").isPresent(),
                        "the reference holds the test mod's main chapter and leaves the shared one out")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    /* The manual key over a stack whose page its mod chose opens that page, in the book that names its chapter. */
    @ClientTest(timeoutTicks = 300)
    public static void manualKey_overAStackOpensThePageItsModChose(final ClientTestContext ctx) {
        final ItemStack named = new ItemStack(Items.PAPER);
        named.set(DataComponents.CUSTOM_NAME, Component.literal(TestGuide.SHARED_PAPER));
        ctx.thenGive(0, named)
                .then(2, () -> ctx.selectHotbar(0))
                .then(2, () -> manualKey().setDown(true))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> manual().manual().id().equals(TestGuide.SECOND_BOOK)
                        && shows(TestGuide.SHARED_PAGE), "the named paper opens the shared page in the second book")
                .then(0, () -> manualKey().setDown(false))
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN)
                .thenGive(0);
    }

    @ClientTest(timeoutTicks = 300)
    public static void manualItem_opensItsManual(final ClientTestContext ctx) {
        ctx.then(0, () -> {
                    final Minecraft minecraft = Minecraft.getInstance();
                    TestGuide.MANUAL_ITEM.get().use(minecraft.level, minecraft.player, InteractionHand.MAIN_HAND);
                })
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> manual().manual().id().equals(TestGuide.MANUAL),
                        "using the manual item opens its manual")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    private static ManualScreen manual() {
        return (ManualScreen) Minecraft.getInstance().screen;
    }

    /* Shows the spread holding the scene of that id: a scene too tall for what is left of a page starts the next. */
    private static void toScene(final String id) {
        final GuideBook book = manual().book();
        for (int page = 0; page < book.pages().size(); page++) {
            if (book.pages().get(page).pieces().stream()
                    .anyMatch(piece -> piece instanceof GuidePiece.Scene scene && scene.id().equals(id))) {
                manual().goToPage(page);
                return;
            }
        }
    }

    /** Whether the spread shown holds the page where that target starts. */
    private static boolean shows(final String target) {
        final ManualScreen screen = manual();
        final int page = screen.book().pageOf(target).orElse(-2);
        return page >= 0 && screen.spread() == page / 2;
    }

    private static void close() {
        Minecraft.getInstance().setScreen(null);
    }

    /* The first piece of code reading those words on the pages of the code entry. */
    private static GuidePiece.Text codePiece(final String words) {
        final GuideBook book = manual().book();
        return book.pages().subList(book.pageOf(TestGuide.CODE).orElseThrow(), book.indexAt()).stream()
                .flatMap(page -> page.pieces().stream()).filter(piece -> piece instanceof GuidePiece.Text text
                        && text.size() == TextSize.CODE && text.text().equals(words))
                .map(piece -> (GuidePiece.Text) piece).findFirst().orElseThrow();
    }

    /* The listing of that file on the pages of the code entry. */
    private static GuidePiece.Listing listingPiece(final String name) {
        final GuideBook book = manual().book();
        return book.pages().subList(book.pageOf(TestGuide.CODE).orElseThrow(), book.indexAt()).stream()
                .flatMap(page -> page.pieces().stream()).filter(piece -> piece instanceof GuidePiece.Listing listing
                        && listing.name().equals(name)).map(piece -> (GuidePiece.Listing) piece).findFirst()
                .orElseThrow();
    }

    /* Whether that sound was let through to the speakers since the time given. */
    private static boolean heard(final SoundKey sound, final long since) {
        return AudioMixer.recent(Util.getMillis() - since + 1).contains(sound.id().toString());
    }

    /* The text on the pages of the tables' entry, from its first page to the end of its chapter. */
    private static List<GuidePiece.Text> tablePieces() {
        final GuideBook book = manual().book();
        return book.pages().subList(book.pageOf(TestGuide.TABLES).orElseThrow(), book.indexAt()).stream()
                .flatMap(page -> page.pieces().stream()).filter(piece -> piece instanceof GuidePiece.Text)
                .map(piece -> (GuidePiece.Text) piece).toList();
    }

    private static GuidePiece.Text tableText(final String words) {
        return tablePieces().stream().filter(text -> text.text().equals(words)).findFirst().orElseThrow();
    }

    /* Whether every piece of the tables' pages stands above the foot of the binder's page. */
    private static boolean nothingBelowTheFoot() {
        final GuideBook book = manual().book();
        final int foot = CoreGuide.PAGE_HEIGHT - GuideLayout.FOOTER_ROOM;
        return book.pages().subList(book.pageOf(TestGuide.TABLES).orElseThrow(), book.indexAt()).stream()
                .flatMap(page -> page.pieces().stream()).allMatch(piece -> piece.y() <= foot);
    }

    /* The words of every piece of an entry's first page that leads to a target, joined as they read. */
    private static String linkWords(final String entry, final String target) {
        final GuideBook book = manual().book();
        final List<String> words = new ArrayList<>();
        for (final GuidePiece piece : book.pages().get(book.pageOf(entry).orElseThrow()).pieces()) {
            if (piece instanceof GuidePiece.Text text && text.link().equals(target)) {
                words.add(text.text().strip());
            }
        }
        return String.join(" ", words);
    }

    /* The manual key's binding, held down and let go as the player's finger would. */
    private static KeyMapping manualKey() {
        return KeyActionsClient.mapping(CoreKeys.OPEN_IN_MANUAL);
    }

    /* Whether the stack's tooltip, gathered as the game gathers it to draw, has a picture line under the key's. */
    private static boolean barUnderHoldLine(final ItemStack stack) {
        final Minecraft minecraft = Minecraft.getInstance();
        final List<Either<FormattedText, TooltipComponent>> elements = new ArrayList<>();
        for (final Component line : Screen.getTooltipFromItem(minecraft, stack)) {
            elements.add(Either.left(line));
        }
        NeoForge.EVENT_BUS.post(new RenderTooltipEvent.GatherComponents(stack,
                minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(), elements, -1));
        final String hold = GameText.resolve(GuideTexts.HOLD_TO_OPEN.with("M"));
        for (int i = 0; i + 1 < elements.size(); i++) {
            if (elements.get(i).left().map(text -> text.getString().equals(hold)).orElse(false)) {
                return elements.get(i + 1).right().isPresent();
            }
        }
        return false;
    }
}
