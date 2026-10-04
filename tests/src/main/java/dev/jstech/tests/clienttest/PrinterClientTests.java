/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.client.PrinterScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.EditorApp;
import dev.jstech.computers.client.os.PaintApp;
import dev.jstech.computers.client.os.PrintDialog;
import dev.jstech.computers.client.printer.PrintedPaperScreen;
import dev.jstech.computers.os.fs.PixImage;
import dev.jstech.computers.printer.PrintedDocument;
import dev.jstech.computers.printer.PrinterModel;
import dev.jstech.computers.printer.Printers;
import dev.jstech.computers.registry.ComputingComponents;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The printers as a player sees them: the five in a row, each printing, its page coming out and its lamp blinking;
 * each one's window in its era's skin; a printed sheet read page by page (the fanfold, the plain sheet, a picture);
 * a printed picture in an item frame; and printing from a program, through the classic Print window of Frames XP and
 * the one with a preview of Frames 11, the document coming out of the printer.
 */
public final class PrinterClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    private static final int BOOT_WAIT = 1_200;
    private static final BlockPos ROW = new BlockPos(1, 2, 2);
    /* Far enough back to see the whole row of five. */
    private static final BlockPos PLAYER_AT_ROW = new BlockPos(5, 2, 9);
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PRINTER = new BlockPos(5, 2, 3);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final BlockPos FRAME_WALL = new BlockPos(4, 2, 2);
    private static final String EDITOR = "jsc:editor";
    private static final String PAINT = "jsc:paint";
    private static final List<Block> PRINTERS = List.of(ComputingModule.VINTAGE_PRINTER.get(),
            ComputingModule.LEGACY_PRINTER.get(), ComputingModule.TRANSITION_PRINTER.get(),
            ComputingModule.PRINTER.get(), ComputingModule.ADVANCED_PRINTER.get());

    private PrinterClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void printers_eachPrintsInTheWorld(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    for (int i = 0; i < PRINTERS.size(); i++) {
                        world.setBlock(rowAt(i), facingSouth(PRINTERS.get(i)));
                    }
                })
                .thenServer(SETTLE, level -> {
                    for (int i = 0; i < PRINTERS.size(); i++) {
                        final PrinterBlockEntity printer = serverPrinter(ctx, level, rowAt(i));
                        printer.paper().setStackInSlot(0, new ItemStack(Items.PAPER, 64));
                        Printers.print(level, printer, page("report.txt"), 3, "dev");
                    }
                })
                .thenTeleport(SETTLE, PLAYER_AT_ROW, Direction.NORTH)
                .thenWaitUntil(() -> allPrinting(ctx), SCREEN_WAIT, "every printer to be printing on the client")
                .thenScreenshot(20, "five-printers-printing");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void printerWindow_wearsItsErasSkin(final ClientTestContext ctx) {
        final String[] names = {"vintage", "legacy", "transition", "standard", "advanced"};
        ctx.thenBuild(0, world -> {
                    for (int i = 0; i < PRINTERS.size(); i++) {
                        world.setBlock(rowAt(i), facingSouth(PRINTERS.get(i)));
                    }
                })
                .thenServer(SETTLE, level -> {
                    for (int i = 0; i < PRINTERS.size(); i++) {
                        final PrinterBlockEntity printer = serverPrinter(ctx, level, rowAt(i));
                        printer.paper().setStackInSlot(0, new ItemStack(Items.PAPER, 37));
                        Printers.print(level, printer, page("README.TXT"), 2, "dev");
                        Printers.print(level, printer, page("OPS.LOG"), 1, "dev");
                    }
                })
                .thenTeleport(SETTLE, PLAYER_AT_ROW, Direction.NORTH);
        for (int i = 0; i < PRINTERS.size(); i++) {
            final BlockPos at = rowAt(i);
            final String name = names[i];
            // Clicked at once: a printer opens its window whether or not a computer is linked, and here none is.
            ctx.thenTeleport(SETTLE, at.south(2), Direction.NORTH)
                    .then(SETTLE, () -> ctx.rightClick(at))
                    .thenAwaitScreen(PrinterScreen.class, SCREEN_WAIT)
                    .thenWaitUntil(() -> !ctx.screen(PrinterScreen.class).getMenu().printer().queueRows().isEmpty(),
                            SCREEN_WAIT, "the " + name + " printer's queue to show")
                    .thenScreenshot(SETTLE, name + "-printer-window")
                    .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                    .thenAwaitNoScreen(SCREEN_WAIT);
        }
    }

    @ClientTest(timeoutTicks = 1200)
    public static void printedPaper_isReadPageByPage(final ClientTestContext ctx) {
        final ItemStack fanfold = sheet(page("README.TXT").printedBy(PrinterModel.EPSILON_FX_80));
        final ItemStack plain = sheet(page("Network log").printedBy(PrinterModel.PAKARD_LASERJOT_1102));
        final ItemStack picture = sheet(picture().printedBy(PrinterModel.PAKARD_DESKJOT_940));
        ctx.thenServer(0, level -> ctx.hold(fanfold.copy()))
                .then(SETTLE, () -> use(ctx))
                .thenAwaitScreen(PrintedPaperScreen.class, SCREEN_WAIT)
                .thenAssert(SETTLE, () -> ctx.screen(PrintedPaperScreen.class).pages() == 2,
                        "the fanfold sheet holds both pages")
                .thenScreenshot(SETTLE, "fanfold-page-1")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_RIGHT))
                .thenAssert(SETTLE, () -> ctx.screen(PrintedPaperScreen.class).page() == 1, "the arrow turns the page")
                .thenScreenshot(SETTLE, "fanfold-page-2")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT)
                .thenServer(SETTLE, level -> ctx.hold(plain.copy()))
                .then(SETTLE, () -> use(ctx))
                .thenAwaitScreen(PrintedPaperScreen.class, SCREEN_WAIT)
                .thenScreenshot(SETTLE, "plain-sheet")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT)
                .thenServer(SETTLE, level -> ctx.hold(picture.copy()))
                .then(SETTLE, () -> use(ctx))
                .thenAwaitScreen(PrintedPaperScreen.class, SCREEN_WAIT)
                .thenAssert(SETTLE, () -> ctx.screen(PrintedPaperScreen.class).document().isPicture(),
                        "the picture sheet is read as a picture")
                .thenScreenshot(SETTLE, "picture-sheet")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    @ClientTest(timeoutTicks = 600)
    public static void printedPicture_showsInAnItemFrame(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> world.setBlock(FRAME_WALL, Blocks.STONE))
                .thenServer(0, level -> {
                    final ItemFrame frame = new ItemFrame(level, ctx.abs(FRAME_WALL.south()), Direction.SOUTH);
                    frame.setItem(sheet(picture().printedBy(PrinterModel.PAKARD_DESKJOT_940)));
                    level.addFreshEntity(frame);
                })
                .thenTeleport(SETTLE, FRAME_WALL.south(3), Direction.NORTH)
                .thenScreenshot(30, "picture-in-frame");
    }

    @ClientTest(timeoutTicks = 3000)
    public static void editor_printsThroughFramesXpsPrintWindow(final ClientTestContext ctx) {
        atDesktopWithPrinter(ctx, "frames_xp")
                .then(SETTLE, () -> DesktopScreen.requestOpen(EDITOR))
                .thenWaitUntil(() -> editor(ctx) != null, SCREEN_WAIT, "the Editor to open")
                .then(SETTLE, () -> ctx.type("A page for the printer"))
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_P, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntil(() -> editor(ctx).printDialog().isOpen()
                                && !editor(ctx).printDialog().printerNames().isEmpty(), SCREEN_WAIT,
                        "the Print window to open and list the printer")
                .thenAssert(0, () -> editor(ctx).printDialog().printerNames().getFirst().contains("LaserJot"),
                        "the laser printer is listed")
                .thenScreenshot(SETTLE, "xp-print-window")
                .then(SETTLE, () -> editor(ctx).printDialog().confirm())
                .thenWaitUntilServer(level -> !serverPrinter(ctx, level, PRINTER).output().getStackInSlot(0).isEmpty(),
                        600, "the page to come out of the printer", level -> "queue "
                                + serverPrinter(ctx, level, PRINTER).jobs().size());
    }

    @ClientTest(timeoutTicks = 3000)
    public static void printWindow_onFrames11ShowsAPreview(final ClientTestContext ctx) {
        atDesktopWithPrinter(ctx, "frames_11")
                .then(SETTLE, () -> DesktopScreen.requestOpen(EDITOR))
                .thenWaitUntil(() -> editor(ctx) != null, SCREEN_WAIT, "the Editor to open")
                .then(SETTLE, () -> ctx.type("Network log\nINSERT 64 Iron Ingot\nCRAFT 4 Fusion Reactor Frame"))
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_P, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntil(() -> editor(ctx).printDialog().isOpen()
                                && !editor(ctx).printDialog().printerNames().isEmpty(), SCREEN_WAIT,
                        "the Print window to open and list the printer")
                .thenScreenshot(SETTLE, "11-print-window-preview")
                .then(SETTLE, () -> editor(ctx).printDialog().close())
                .then(SETTLE, () -> DesktopScreen.requestOpen(PAINT))
                .thenWaitUntil(() -> paint(ctx) != null, SCREEN_WAIT, "Paint to open")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_P, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntil(() -> paint(ctx).printDialog().isOpen()
                                && !paint(ctx).printDialog().printerNames().isEmpty(), SCREEN_WAIT,
                        "Paint's Print window to open")
                .thenScreenshot(SETTLE, "11-print-picture-preview");
    }

    /* A running computer with that system, its monitor, a laser printer with paper beside it, and its desktop up. */
    private static ClientTestContext atDesktopWithPrinter(final ClientTestContext ctx, final String system) {
        return ctx.thenBuild(0, world -> {
                    world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system));
                    world.placeMonitor(MONITOR, Direction.EAST);
                    world.setBlock(PRINTER, ComputingModule.PRINTER.get());
                })
                .thenServer(SETTLE, level -> serverPrinter(ctx, level, PRINTER).paper()
                        .setStackInSlot(0, new ItemStack(Items.PAPER, 16)))
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs");
    }

    @Nullable
    private static EditorApp editor(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final DesktopWindow window = desktop == null ? null : desktop.windowFor(EDITOR);
        return window != null && window.app() instanceof EditorApp app ? app : null;
    }

    @Nullable
    private static PaintApp paint(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final DesktopWindow window = desktop == null ? null : desktop.windowFor(PAINT);
        return window != null && window.app() instanceof PaintApp app ? app : null;
    }

    private static BlockPos rowAt(final int i) {
        return ROW.east(i * 2);
    }

    private static BlockState facingSouth(final Block block) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
    }

    private static PrinterBlockEntity serverPrinter(final ClientTestContext ctx, final ServerLevel level,
                                                    final BlockPos at) {
        return (PrinterBlockEntity) level.getBlockEntity(ctx.abs(at));
    }

    /* Whether the client sees every printer of the row printing. */
    private static boolean allPrinting(final ClientTestContext ctx) {
        for (int i = 0; i < PRINTERS.size(); i++) {
            if (!(ctx.mc().level.getBlockEntity(ctx.abs(rowAt(i))) instanceof PrinterBlockEntity printer)
                    || !printer.printing()) {
                return false;
            }
        }
        return true;
    }

    private static PrintedDocument page(final String title) {
        return Printers.text(title, "lab", "Editor", "MINESWEEPER 1.0 FOR MC-DOS\n\n" + "a line of print\n".repeat(40),
                false, 0, 0);
    }

    private static PrintedDocument picture() {
        final PixImage image = new PixImage(48, 32);
        image.fillAll(13);
        image.ellipse(30, 4, 42, 16, 7);
        image.rectangle(8, 14, 24, 28, 3);
        return PrintedDocument.picture("house.pix", "studio", "Paint", image.encode(), "house.pix");
    }

    private static ItemStack sheet(final PrintedDocument document) {
        final ItemStack stack = new ItemStack(ComputingModule.PRINTED_PAPER.get());
        stack.set(ComputingComponents.PRINTED_DOCUMENT.get(), document);
        return stack;
    }

    /* Uses the item in the main hand, as a right click in the air does. */
    private static void use(final ClientTestContext ctx) {
        ctx.mc().gameMode.useItem(ctx.player(), InteractionHand.MAIN_HAND);
    }
}
