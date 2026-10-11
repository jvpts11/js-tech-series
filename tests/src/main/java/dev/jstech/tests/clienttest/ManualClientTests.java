/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import com.mojang.datafixers.util.Either;
import dev.jstech.computers.guide.ComputersGuide;
import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.client.guide.ManualScreen;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideBook;
import dev.jstech.core.guide.GuideLayout;
import dev.jstech.core.guide.GuidePiece;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.guide.TextSize;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.text.GameText;
import dev.jstech.industrial.guide.IndustrialGuide;
import dev.jstech.tests.TestGuide;
import dev.jstech.tests.TestPress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
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

/**
 * The Core's manuals as a player reads them: the test manual opened at its cover and turned to its contents, opened
 * at an entry, a link and a tab followed, its index searched as the player types, a long entry run on to the pages
 * after its first, a table run on and a table stacked, the manual opened again where it was closed, an item's page opened by the manual key held over
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
                .thenScreenshot(2, "manual_tables")
                .then(0, () -> manual().turn(1))
                .thenScreenshot(2, "manual_tables_on")
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

    /** Whether the spread shown holds the page where that target starts. */
    private static boolean shows(final String target) {
        final ManualScreen screen = manual();
        final int page = screen.book().pageOf(target).orElse(-2);
        return page >= 0 && screen.spread() == page / 2;
    }

    private static void close() {
        Minecraft.getInstance().setScreen(null);
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

    /* Whether the stack's tooltip, gathered as the game gathers it to draw, has a picture line right under the key's. */
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
