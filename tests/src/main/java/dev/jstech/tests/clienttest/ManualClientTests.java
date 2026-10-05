/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.client.guide.ManualScreen;
import dev.jstech.core.guide.GuideBook;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.text.GameText;
import dev.jstech.tests.TestGuide;
import dev.jstech.tests.TestPress;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * The Core's manuals as a player reads them: the test manual opened at its cover and turned to its contents, opened
 * at an entry, a link and a tab followed, its index searched as the player types, a long entry run on to the pages
 * after its first, an item's page opened by the manual key, and the manual item opening its manual.
 */
public final class ManualClientTests {

    private static final int SETTLE = 4;
    private static final int OPEN = 40;

    private ManualClientTests() {
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_opensAtItsCoverAndTurnsToItsContents(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, ""))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> manual().spread() == -1, "the manual opens at its cover")
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
}
