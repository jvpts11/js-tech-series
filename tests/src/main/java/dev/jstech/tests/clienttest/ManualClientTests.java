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
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.guide.GuideBook;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.text.GameText;
import dev.jstech.industrial.guide.IndustrialGuide;
import dev.jstech.tests.TestGuide;
import dev.jstech.tests.TestPress;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The Core's manuals as a player reads them: the test manual opened at its cover and turned to its contents, opened
 * at an entry, a link and a tab followed, its index searched as the player types, a long entry run on to the pages
 * after its first, the manual opened again where it was closed, an item's page opened by the manual key held over
 * it while a bar in the manual's look fills, and the manual item opening its manual.
 */
public final class ManualClientTests {

    private static final int SETTLE = 4;
    private static final int OPEN = 40;
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
                    final List<GuideBook.IndexLine> found = manual().found();
                    return manual().searching() && !found.isEmpty() && found.stream()
                            .allMatch(line -> line.text().toLowerCase(Locale.ROOT).contains("press"));
                }, "the search finds the press, and only what has its name")
                .thenScreenshot(2, "manual_search")
                .then(0, () -> manual().search("nothing by this name"))
                .thenAssert(SETTLE, () -> manual().found().isEmpty(), "and nothing for a name no entry has")
                .then(0, ManualClientTests::close)
                .thenAwaitNoScreen(OPEN);
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
                .then(0, () -> GuideClient.open(TestGuide.MANUAL, ""))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> left.get() > 0 && manual().spread() == left.get(),
                        "the manual opens at the spread it was closed at")
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

    /** Whether the spread shown holds the page where that target starts. */
    private static boolean shows(final String target) {
        final ManualScreen screen = manual();
        final int page = screen.book().pageOf(target).orElse(-2);
        return page >= 0 && screen.spread() == page / 2;
    }

    private static void close() {
        Minecraft.getInstance().setScreen(null);
    }

    /* The manual key's binding, held down and let go as the player's finger would. */
    private static KeyMapping manualKey() {
        return KeyActionsClient.mapping(CoreKeys.OPEN_IN_MANUAL);
    }
}
