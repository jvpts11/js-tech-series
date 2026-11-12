/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.QuinceItems;
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Quince's systems on its own computers (computers/docs/design/quince.md), each as its mock draws it: the Quince System
 * on the Russet 128K, Quince OS 9 on the qRusset G3, Quince OS X in Pulp on the white qRusset and gone flat on the slim
 * one, and quinceOS on the coloured qRusset, each machine showing its desktop on its own glass with the startup disk's
 * window open.
 */
public final class QuinceClientTests {

    private static final int SETTLE = 4;
    private static final int BOOT_WAIT = 1_600;
    private static final BlockPos MACHINE = new BlockPos(3, 2, 3);
    private static final BlockPos PLAYER = MACHINE.north(2);
    /* A point every Quince all-in-one has glass at, in sixty-fourths of its front: the middle of the 128K's. */
    private static final int ON_THE_GLASS_X = 32;
    private static final int ON_THE_GLASS_Y = 22;
    /* The scale Quince's desktops are designed at: a desktop pixel to a pixel of the game's interface. */
    private static final int DESIGNED_SCALE = 100;
    private static final int WINDOW_WAIT = 200;
    private static final String FILES = "jsc:files";
    /* The startup disk's root, as its icon opens it. */
    private static final String ROOT = "";
    private static final String TEXTPAD = "jsc:editor";
    private static final String CALCULATOR = "jsc:calculator";
    private static final String QTUNES = "jsc:qtunes";
    private static final String EXPEDITION = "jsc:expedition";
    private static final String THIS_COMPUTER = "jsc:this_pc";
    /* The bar's titles: the fruit's menu first, then the File menu on the classic systems. */
    private static final int FRUIT = 0;
    private static final int FILE_MENU = 1;
    private static final String ABOUT = "About This Computer";
    private static final String GET_INFO = "Get Info";
    /* How far down a window's title bar a click lands. */
    private static final int TITLE_DY = 5;

    private QuinceClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void quinceSystem_showsItsDesktop(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.RUSSET_128K.get(), QuinceItems.BOARD_RUSSET_128K.get(), "quince_system")
                .thenScreenshot(SETTLE, "qs-desktop");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void quinceOs9_showsItsDesktop(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.QRUSSET_G3.getFirst().get(), QuinceItems.BOARD_QRUSSET_G3.get(), "quince_os_9")
                .thenScreenshot(SETTLE, "os9-desktop");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void quinceOsX_showsItsDesktopInPulp(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.QRUSSET_WHITE.get(), QuinceItems.BOARD_QRUSSET_WHITE.get(), "quince_os_x")
                .thenScreenshot(SETTLE, "osx-desktop");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void quinceOsX_showsItsDesktopGoneFlat(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.QRUSSET_SLIM.get(), QuinceItems.BOARD_QRUSSET_THIN.get(), "quince_os_x")
                .thenScreenshot(SETTLE, "flat-desktop");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void quinceOs_showsItsDesktop(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.QRUSSET_COLOURS.getFirst().get(), QuinceItems.BOARD_QRUSSET_COLOUR.get(),
                "quinceos")
                .thenScreenshot(SETTLE, "qos-desktop");
    }

    /*
     * About This Computer with TextPad and the Calculator open, each in its block of memory, then TextPad's Get Info
     * with its memory size.
     */
    @ClientTest(timeoutTicks = 3000)
    public static void quinceSystem_showsItsMemoryInAboutAndGetInfo(final ClientTestContext ctx) {
        memory(atDesktop(ctx, ComputingModule.RUSSET_128K.get(), QuinceItems.BOARD_RUSSET_128K.get(),
                "quince_system"), ctx, CALCULATOR, TEXTPAD, "qs");
    }

    /* The same on Quince OS 9, with qTunes and Expedition open, then Expedition's Get Info. */
    @ClientTest(timeoutTicks = 3000)
    public static void quinceOs9_showsItsMemoryInAboutAndGetInfo(final ClientTestContext ctx) {
        memory(atDesktop(ctx, ComputingModule.QRUSSET_G3.getFirst().get(), QuinceItems.BOARD_QRUSSET_G3.get(),
                "quince_os_9"), ctx, QTUNES, EXPEDITION, "os9");
    }

    /*
     * Opens the two programs, the second in front, then About This Computer from the fruit's menu, and the Get Info of
     * the program in front from the File menu, each shot for its mock.
     */
    private static void memory(final ClientTestContext steps, final ClientTestContext ctx, final String first,
                               final String front, final String label) {
        steps.then(SETTLE, () -> DesktopScreen.requestOpen(first))
                .thenWaitUntil(() -> desktop(ctx).windowFor(first) != null, WINDOW_WAIT, first + " to open")
                .then(SETTLE, () -> DesktopScreen.requestOpen(front))
                .thenWaitUntil(() -> desktop(ctx).windowFor(front) != null, WINDOW_WAIT, front + " to open")
                .then(SETTLE, () -> ctx.clickDesktop(desktop(ctx).quinceTitlePoint(FRUIT)))
                .thenWaitUntil(() -> desktop(ctx).quinceItemPoint(ABOUT) != null, WINDOW_WAIT, "the fruit's menu")
                .then(SETTLE, () -> ctx.clickDesktop(desktop(ctx).quinceItemPoint(ABOUT)))
                .thenWaitUntil(() -> desktop(ctx).windowFor(THIS_COMPUTER) != null, WINDOW_WAIT,
                        "About This Computer")
                .thenScreenshot(SETTLE * 4, label + "-about")
                .then(SETTLE, () -> ctx.clickDesktop(desktop(ctx).windowFor(THIS_COMPUTER)
                        .buttonCentre(DesktopWindow.BUTTON_CLOSE)))
                .then(SETTLE, () -> ctx.clickDesktop(titleOf(desktop(ctx).windowFor(front))))
                .then(SETTLE, () -> ctx.clickDesktop(desktop(ctx).quinceTitlePoint(FILE_MENU)))
                .thenWaitUntil(() -> desktop(ctx).quinceItemPoint(GET_INFO) != null, WINDOW_WAIT, "the File menu")
                .then(SETTLE, () -> ctx.clickDesktop(desktop(ctx).quinceItemPoint(GET_INFO)))
                .thenScreenshot(SETTLE * 4, label + "-getinfo");
    }

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }

    /* A point on a window's title bar, where a click brings it to the front. */
    private static int[] titleOf(final DesktopWindow window) {
        return new int[] {window.x() + window.width() / 2, window.y() + TITLE_DY};
    }

    /* The machine built on its board with that system, running, and its own glass open on the desktop. */
    private static ClientTestContext atDesktop(final ClientTestContext ctx, final Block block, final Item board,
                                               final String system) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, block.defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    final PersonalComputerBlockEntity machine = world.blockEntity(MACHINE,
                            PersonalComputerBlockEntity.class);
                    build(machine, board);
                    if (!machine.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system))) {
                        throw new IllegalStateException(system + " does not install on " + block);
                    }
                    machine.console().settings().setGuiScale(DESIGNED_SCALE);
                    machine.togglePower();
                })
                .thenTeleport(SETTLE, PLAYER, Direction.SOUTH)
                // On the glass, where a click opens the desktop; on the case it opens the machine's parts.
                .thenRightClickFront(SETTLE, MACHINE, ON_THE_GLASS_X, ON_THE_GLASS_Y)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE * 4, () -> DesktopScreen.requestOpenFiles(ROOT))
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(FILES) != null, WINDOW_WAIT,
                        "the startup disk's window")
                .then(SETTLE * 10, () -> ctx.mc().getToasts().clear());
    }

    /*
     * The parts in the slots the machine's own map has for them: the board, the strongest processor of its era, a
     * module of its memory where it takes one, its era's supply and disk.
     */
    private static void build(final PersonalComputerBlockEntity machine, final Item board) {
        final MotherboardSpec spec = EraParts.board(board);
        final ItemStackHandler hardware = machine.getHardware();
        final ComputerHardwareLayout map = machine.hardwareLayout();
        hardware.setStackInSlot(map.motherboardSlot(), new ItemStack(board));
        hardware.setStackInSlot(map.cpuSlot(0), new ItemStack(EraParts.cpuFor(board)));
        if (spec.ramSlots() > 0) {
            hardware.setStackInSlot(map.ramSlot(0), new ItemStack(EraParts.ramFor(board)));
        }
        hardware.setStackInSlot(map.psuSlot(), new ItemStack(EraParts.psuFor(spec.era())));
        hardware.setStackInSlot(map.diskStart(), new ItemStack(EraParts.diskFor(spec.era())));
    }
}
