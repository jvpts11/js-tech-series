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
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.os.ActiveDesktop;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.files.FileTransferPayloads;
import dev.jstech.computers.os.DesktopEffects;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.OsMotions;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.motion.MotionKinds;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Each system moving as the real one did: Frames 11's windows growing in and fading, Frames 95's caption flying to
 * the taskbar, the desktop behind Frames XP's Turn Off draining to grey, CDE's busy light blinking while a program
 * starts, each console's cursor blinking to its own beat; the desktop's own pointer over the glass; and a copy that
 * takes time showing its window, Frames 95's flying paper, and Plasma's notification instead of a window.
 */
public final class SystemMotionClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 1_200;
    /** Long enough for any motion at eight times its length to be over. */
    private static final int MOTION_WAIT = 100;
    /** Long enough for the grey behind Turn Off, a second and a half at its own pace, and a copy of a few seconds. */
    private static final int LONG_WAIT = 400;
    private static final String CALCULATOR = "Calculator";
    /** The Settings program by its key, which every desktop knows whatever it calls the program. */
    private static final String SETTINGS = "jsc:settings";
    /** Every motion at eight times its own length, so a test sees it under way. */
    private static final DesktopEffects SLOW = new DesktopEffects(List.of(), 800);
    /*
     * Four files of a megabyte each on a disk of today, so the copies run four seconds onto a floppy. Each stays under
     * the longest word a block's saved data can hold, sixty-four kilobytes.
     */
    private static final int PART_CHARS = 60_000;
    private static final List<String> PARTS = List.of("part1.txt", "part2.txt", "part3.txt", "part4.txt");
    /** Long enough for a drive set beside a computer to link to it. */
    private static final int LINK_TICKS = 20;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos DRIVE = new BlockPos(4, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final ResourceLocation MC_DOS = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos");

    private SystemMotionClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void window_growsInFadingAndAClosedOneFadesAwayOnFrames11(final ClientTestContext ctx) {
        booted(ctx, "frames_11", null)
                .then(0, () -> moving(true))
                .then(SETTLE, () -> DesktopScreen.requestOpen(CALCULATOR))
                .thenWaitUntil(() -> desktop(ctx).windowFor(CALCULATOR) != null, SCREEN_WAIT, "the Calculator window")
                .thenAssert(0, () -> desktop(ctx).windowFading(CALCULATOR),
                        "the window grows in from clear, drawn whole off the glass first")
                .thenScreenshot(2, "frames11-window-fading-in")
                .thenWaitUntil(() -> !desktop(ctx).windowFading(CALCULATOR), MOTION_WAIT, "the window to arrive")
                .then(0, () -> moving(false));
    }

    @ClientTest(timeoutTicks = 2400)
    public static void minimize_fliesTheCaptionToTheTaskbarOnFrames95(final ClientTestContext ctx) {
        booted(ctx, "frames_95", null)
                .then(0, () -> moving(true))
                .then(SETTLE, () -> DesktopScreen.requestOpen(CALCULATOR))
                .thenWaitUntil(() -> desktop(ctx).windowFor(CALCULATOR) != null, SCREEN_WAIT, "the Calculator window")
                .then(SETTLE, () -> click(ctx, desktop(ctx).windowButtonPoint(CALCULATOR, 1)))
                .thenAssert(1, () -> desktop(ctx).windowCaptionFlying(CALCULATOR),
                        "minimized, only the title bar flies down to the taskbar")
                .thenScreenshot(2, "frames95-caption-flying")
                .thenWaitUntil(() -> !desktop(ctx).windowCaptionFlying(CALCULATOR), MOTION_WAIT,
                        "the caption to reach its button")
                .then(0, () -> moving(false));
    }

    @ClientTest(timeoutTicks = 2400)
    public static void turnOff_drainsTheDesktopToGreyOnFramesXp(final ClientTestContext ctx) {
        final int[] probe = new int[2];
        booted(ctx, "frames_xp", null)
                .then(0, () -> MotionClock.setReduced(false))
                .then(SETTLE, () -> desktop(ctx).openPowerDialog())
                .thenAssert(1, () -> desktop(ctx).powerGreyed() < 0.5F, "the grey comes on slowly")
                .thenWaitUntil(() -> desktop(ctx).powerGreyed() >= 1.0F, LONG_WAIT, "the desktop to go wholly grey")
                .then(SETTLE, () -> {
                    final int[] point = desktop(ctx).glassPoint(12, 12);
                    probe[0] = point[0];
                    probe[1] = point[1];
                })
                .thenScreenshot(2, "framesxp-turn-off-grey")
                .then(0, () -> {
                    final int rgb = ctx.pixel(probe[0], probe[1]);
                    final int r = rgb >> 16 & 0xFF;
                    final int g = rgb >> 8 & 0xFF;
                    final int b = rgb & 0xFF;
                    ctx.assertTrue(Math.abs(r - g) <= 3 && Math.abs(g - b) <= 3,
                            "the desktop behind the dialog has lost its colour; got " + Integer.toHexString(rgb));
                })
                .then(0, () -> MotionClock.setReduced(true));
    }

    @ClientTest(timeoutTicks = 3600)
    public static void busyLight_blinksWhileAProgramStartsOnCde(final ClientTestContext ctx) {
        final boolean[] seen = new boolean[2];
        atCde(ctx)
                .then(0, () -> MotionClock.setReduced(false))
                .thenAssert(SETTLE, () -> !desktop(ctx).busyLightLit(), "the busy light is dark with nothing starting")
                .then(SETTLE, () -> DesktopScreen.requestOpen(SETTINGS))
                .thenWaitUntil(() -> {
                    if (desktop(ctx).busyLightLit()) {
                        seen[0] = true;
                    } else if (seen[0]) {
                        seen[1] = true;
                    }
                    return seen[1];
                }, SCREEN_WAIT, "the busy light to light and go dark again as the program starts")
                .thenWaitUntil(() -> !desktop(ctx).busyLightLit(), SCREEN_WAIT, "the light to stay dark after")
                .then(0, () -> MotionClock.setReduced(true));
    }

    @ClientTest(timeoutTicks = 3600)
    public static void consoleCursor_blinksToItsConsolesOwnBeat(final ClientTestContext ctx) {
        final int[] turns = new int[1];
        final boolean[] last = new boolean[1];
        ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(MC_DOS);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .then(0, () -> {
                    ctx.assertTrue(OsMotions.console(Platform.MC_DOS).get().spec(MotionKinds.CARET_BLINK).duration()
                            == 229, "MC-DOS blinks as a VGA card does, every 229 ms");
                    ctx.assertTrue(OsMotions.console(Platform.LINUX).get().spec(MotionKinds.CARET_BLINK).duration()
                            == 200, "Linux's framebuffer console every 200 ms");
                    ctx.assertTrue(OsMotions.console(Platform.FRAMES).get().spec(MotionKinds.CARET_BLINK).duration()
                            == 530, "and the Frames console every 530 ms");
                    MotionClock.setReduced(false);
                    last[0] = ctx.screen(CommandPromptScreen.class).cursorLit();
                })
                // Two seconds at a VGA card's beat turn the cursor over about eight times.
                .thenWaitUntil(() -> {
                    final boolean lit = ctx.screen(CommandPromptScreen.class).cursorLit();
                    if (lit != last[0]) {
                        turns[0]++;
                        last[0] = lit;
                    }
                    return turns[0] >= 6;
                }, 60, "the cursor to blink at the VGA card's beat")
                .then(0, () -> MotionClock.setReduced(true))
                .thenAssert(SETTLE, () -> ctx.screen(CommandPromptScreen.class).cursorLit(),
                        "with motion reduced the cursor stands lit");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void pointer_isTheDesktopsOwnOverTheGlass(final ClientTestContext ctx) {
        booted(ctx, "frames_xp", null)
                .then(SETTLE, () -> {
                    final int[] middle = desktop(ctx).glassPoint(60, 60);
                    ctx.pointAt(middle[0], middle[1]);
                })
                .thenAssert(2, () -> desktop(ctx).osPointerHidden(), "over the glass the game's pointer is hidden")
                .thenAssert(0, () -> "arrow".equals(desktop(ctx).pointerState()), "and the desktop draws its arrow")
                .thenScreenshot(1, "framesxp-own-pointer")
                .then(SETTLE, () -> DesktopScreen.requestOpen(CALCULATOR))
                .thenWaitUntil(() -> "working".equals(desktop(ctx).pointerState()), 20,
                        "a program starting to turn it to the arrow with the hourglass")
                .then(SETTLE, () -> ctx.pointAt(1, 1))
                .thenAssert(2, () -> !desktop(ctx).osPointerHidden(), "off the glass the game's pointer is back");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void copy_thatTakesAWhileShowsTheFlyingPaperOnFrames95(final ClientTestContext ctx) {
        bigCopy(booted(ctx, "frames_95", null), ctx)
                .thenWaitUntil(() -> desktop(ctx).copyWindowUp(), LONG_WAIT,
                        "the copy window once the copy outlasts a moment")
                .thenScreenshot(SETTLE, "frames95-copy-window")
                .thenWaitUntil(() -> !desktop(ctx).copyUnderWay() && !desktop(ctx).copyWindowUp(), LONG_WAIT,
                        "the window to go when the copy has ended");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void copy_onPlasmaShowsANotificationAndNoWindow(final ClientTestContext ctx) {
        bigCopy(booted(ctx, "ubuntu", "jsc:kde_plasma"), ctx)
                .thenWaitUntil(() -> desktop(ctx).copyUnderWay(), LONG_WAIT, "the copy under way")
                .thenScreenshot(30, "plasma-copy-notification")
                .thenAssert(0, () -> !desktop(ctx).copyWindowUp(), "Plasma shows a copy over its panel, not a window")
                .thenWaitUntil(() -> !desktop(ctx).copyUnderWay(), LONG_WAIT, "the copy to end");
    }

    /*
     * A floppy drive with a floppy beside the machine, four files on its disk, and the files copied onto the floppy as
     * pasting them there does: a few seconds at the floppy's pace, one file after another.
     */
    private static ClientTestContext bigCopy(final ClientTestContext chain, final ClientTestContext ctx) {
        return chain.thenServer(SETTLE, level -> {
                    final TestWorldBuilder world = TestWorldBuilder.at(level, ctx.origin());
                    world.setBlock(DRIVE, ComputingModule.FLOPPY_DRIVE.get());
                    world.blockEntity(DRIVE, MediaReaderBlockEntity.class).mediaSlot()
                            .setStackInSlot(0, new ItemStack(ComputingModule.FLOPPY_DISK.get()));
                })
                .thenServer(LINK_TICKS, level -> {
                    final CraftingComputerBlockEntity computer = TestWorldBuilder.at(level, ctx.origin())
                            .blockEntity(COMPUTER, CraftingComputerBlockEntity.class);
                    final ItemStack disk = computer.systemDisk();
                    for (final String part : PARTS) {
                        DiskFilesystem.write(disk, part, FileType.TXT, "x".repeat(PART_CHARS), Long.MAX_VALUE,
                                FilesystemKind.HIERARCHICAL);
                        FileTransferPayloads.copy(level, ctx.serverPlayer(), ctx.abs(COMPUTER), part,
                                "media:" + ctx.abs(DRIVE).asLong());
                    }
                });
    }

    /** A machine at its desktop: {@code os} installed, and {@code desktopPackage} on top of it when one is given. */
    private static ClientTestContext booted(final ClientTestContext ctx, final String os,
                                            @Nullable final String desktopPackage) {
        return ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(jsc(os));
                    if (desktopPackage != null) {
                        computer.console().install(desktopPackage);
                    }
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !desktop(ctx).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop's programs to be listed");
    }

    /** A machine carrying UNIX and CDE, switched on, with the player at its desktop. */
    private static ClientTestContext atCde(final ClientTestContext ctx) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(COMPUTER, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(COMPUTER, MainframeBlockEntity.class);
                    final ItemStackHandler inv = machine.getInventory();
                    inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(ComputingModule.MOTHERBOARD_MTX_S_2011.get()));
                    inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                            new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
                    inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                            new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
                    inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
                    inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.GPU_HD_7970.get()));
                    inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
                    machine.installOs(jsc("unix"));
                    machine.console().install(jsc("cde").toString());
                    machine.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !desktop(ctx).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop's programs to be listed");
    }

    /** Lets every motion move again, slowed to eight times its length, or keeps it reduced. */
    private static void moving(final boolean move) {
        MotionClock.setReduced(!move);
        ActiveDesktop.applyLiveEffects(SLOW);
    }

    private static void click(final ClientTestContext ctx, final int[] point) {
        ctx.click(point[0], point[1]);
    }

    private static ResourceLocation jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }
}
