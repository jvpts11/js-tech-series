/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.block.MonitorPanel;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.monitor.MonitorPictureCache;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.theme.MonitorFrameStyle;
import dev.jstech.computers.gui.layout.PowerStripLayout;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.monitor.IMonitorPicture;
import dev.jstech.core.client.live.LiveScreens;
import dev.jstech.core.gui.Tube;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The monitors as a player sees them: every era's face from its real counterpart, the machine's screen live on the
 * glass of one nearby and gone from a distance, the Vintage systems in the tube's own colours (amber on an amber
 * monitor, sixteen colours on a CGA), flat panels joined into one screen, and the power strip on an opened screen
 * shutting the machine down.
 */
public final class MonitorClientTests {

    private static final int SETTLE = 4;
    private static final int BOOT_WAIT = 1_200;
    private static final int LIGHT_WAIT = 400;
    private static final ResourceLocation MC_DOS = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos");
    /* The eight monitors in a row along x, two blocks apart, their screens toward the player to the south. */
    private static final BlockPos ROW = new BlockPos(2, 2, 2);
    private static final BlockPos ROW_VIEW = new BlockPos(9, 2, 8);
    private static final int STANDARD_INDEX = 6;
    private static final BlockPos FAR = new BlockPos(9, 2, 50);
    /* A wall of flat monitors three wide and two tall, a computer behind its bottom-left corner. */
    private static final BlockPos WALL = new BlockPos(3, 2, 3);
    private static final BlockPos WALL_VIEW = new BlockPos(4, 2, 8);
    /* A monitor on a machine, with the player standing at it to open it. */
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos SCREEN = new BlockPos(6, 2, 2);
    private static final BlockPos AT_SCREEN = new BlockPos(8, 2, 2);

    private MonitorClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void monitors_wearTheirFacesAndShowTheirMachineLiveNearby(final ClientTestContext ctx) {
        final Block[] kinds = kinds();
        final BlockPos standard = ROW.east(STANDARD_INDEX * 2);
        ctx.thenBuild(0, world -> {
                    for (int i = 0; i < kinds.length; i++) {
                        world.placeMonitor(ROW.east(i * 2), Direction.NORTH, kinds[i]);
                    }
                })
                .thenTeleport(SETTLE, ROW_VIEW, Direction.NORTH)
                .thenScreenshot(20, "all-faces")
                .thenBuild(0, world -> world.placeRunningPersonalComputer(standard.north()))
                .thenWaitUntil(() -> lit(ctx, standard), LIGHT_WAIT, "the Standard monitor to light")
                .thenWaitUntil(() -> MonitorPictureCache.of(ctx.abs(standard)) instanceof IMonitorPicture.Desktop,
                        BOOT_WAIT, "the server to say the face shows the machine's desktop")
                .thenWaitUntil(() -> LiveScreens.held() > 0, 100, "the face to be drawn")
                .thenScreenshot(30, "live-desktop")
                // From further than anybody could read it, only the light shows and the picture is let go.
                .thenTeleport(0, FAR, Direction.NORTH)
                .thenWaitUntil(() -> LiveScreens.held() == 0, 300, "the picture to be let go far off");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void tube_showsMcDosInAmberOnAnAmberMonitor(final ClientTestContext ctx) {
        openVintagePrompt(ctx, ComputingModule.AMBER_MONITOR.get())
                .thenScreenshot(SETTLE, "mc-dos-amber")
                .thenAssert(0, () -> glassIs(ctx, MonitorClientTests::amber), "every pixel of the glass is amber");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void tube_showsMcDosInSixteenColoursOnACga(final ClientTestContext ctx) {
        final Set<Integer> sixteen = new HashSet<>();
        for (final int colour : Tube.sixteenColours()) {
            sixteen.add(colour);
        }
        openVintagePrompt(ctx, ComputingModule.CGA_MONITOR.get())
                .thenScreenshot(SETTLE, "mc-dos-cga")
                .thenAssert(0, () -> glassIs(ctx, rgb -> sixteen.contains(rgb)),
                        "every pixel of the glass is one of the sixteen colours");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void panel_ofFlatMonitorsShowsOneScreenWithOneButton(final ClientTestContext ctx) {
        final BlockPos[] wall = new BlockPos[6];
        ctx.thenBuild(0, world -> {
                    for (int v = 0; v < 2; v++) {
                        for (int u = 0; u < 3; u++) {
                            wall[v * 3 + u] = WALL.east(u).above(v);
                            world.placeMonitor(wall[v * 3 + u], Direction.NORTH,
                                    ComputingModule.TRANSITION_MONITOR.get());
                        }
                    }
                    world.placeRunningPersonalComputer(WALL.north());
                })
                .thenTeleport(SETTLE, WALL_VIEW, Direction.NORTH)
                .thenWaitUntil(() -> {
                    for (final BlockPos member : wall) {
                        if (!lit(ctx, member)) {
                            return false;
                        }
                    }
                    return true;
                }, BOOT_WAIT, "the whole screen to light")
                .thenScreenshot(40, "panel-3x2")
                .thenAssert(0, () -> {
                    int buttons = 0;
                    for (final BlockPos member : wall) {
                        if (ctx.mc().level.getBlockState(ctx.abs(member)).getValue(MonitorBlock.BUTTON)) {
                            buttons++;
                        }
                    }
                    return buttons == 1;
                }, "only the corner monitor shows its power button")
                .thenAssert(0, () -> {
                    final MonitorPanel panel = clientMonitor(ctx, WALL).panel();
                    return panel != null && panel.width() == 3 && panel.height() == 2;
                }, "the player's game sees one screen three by two");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void powerStrip_shutsTheMachineDownFromItsOpenedScreen(final ClientTestContext ctx) {
        final PersonalComputerBlockEntity[] pc = new PersonalComputerBlockEntity[1];
        final int[] power = new int[2];
        ctx.thenBuild(0, world -> {
                    pc[0] = world.placeRunningPersonalComputer(COMPUTER);
                    world.placeMonitor(SCREEN, Direction.WEST);
                })
                .thenTeleport(SETTLE, AT_SCREEN, Direction.WEST)
                .thenWaitUntilServer(level -> !pc[0].needsPost() && !pc[0].booting(), BOOT_WAIT,
                        "the machine to come up", level -> "still starting")
                .thenRightClick(SETTLE, SCREEN)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> {
                    final MonitorFrameStyle.Geometry frame = ctx.screen(DesktopScreen.class).frameBounds();
                    power[0] = PowerStripLayout.stripX(frame.x()) + PowerStripLayout.buttonX()
                            + PowerStripLayout.BUTTON / 2;
                    power[1] = PowerStripLayout.stripY(frame.y()) + PowerStripLayout.POWER_Y
                            + PowerStripLayout.BUTTON / 2;
                    ctx.pointAt(power[0], power[1]);
                })
                .thenScreenshot(SETTLE, "power-strip-tooltip")
                .then(0, () -> {
                    // The way the game hands a click to a screen: the strip is asked before the screen is.
                    final DesktopScreen screen = ctx.screen(DesktopScreen.class);
                    if (!ClientHooks.onScreenMouseClickedPre(screen, power[0], power[1], 0)) {
                        screen.mouseClicked(power[0], power[1], 0);
                    }
                })
                .thenWaitUntilServer(level -> pc[0].goingDown() || !pc[0].isRunning(), 200,
                        "the Power button to shut the machine down", level -> "the machine is still up");
    }

    /* A Vintage machine at its MC-DOS prompt on that monitor, the prompt opened. */
    private static ClientTestContext openVintagePrompt(final ClientTestContext ctx, final Block monitor) {
        final PersonalComputerBlockEntity[] pc = new PersonalComputerBlockEntity[1];
        return ctx.thenBuild(0, world -> {
                    world.setBlock(COMPUTER, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
                    pc[0] = world.blockEntity(COMPUTER, PersonalComputerBlockEntity.class);
                    buildVintage(pc[0]);
                    pc[0].installOs(MC_DOS);
                    pc[0].togglePower();
                    world.placeMonitor(SCREEN, Direction.WEST, monitor);
                })
                .thenTeleport(SETTLE, AT_SCREEN, Direction.WEST)
                .thenWaitUntilServer(level -> !pc[0].needsPost() && !pc[0].booting(), BOOT_WAIT,
                        "the machine to come up", level -> "still starting")
                .thenRightClick(SETTLE, SCREEN)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT);
    }

    /* Whether every pixel on a grid over the open screen's glass passes the test. */
    private static boolean glassIs(final ClientTestContext ctx, final IntPredicate test) {
        if (!(ctx.mc().screen instanceof AbstractContainerScreen<?> screen)) {
            return false;
        }
        final int columns = 12;
        final int rows = 8;
        for (int c = 1; c < columns; c++) {
            for (int r = 1; r < rows; r++) {
                final int x = screen.getGuiLeft() + screen.getXSize() * c / columns;
                final int y = screen.getGuiTop() + screen.getYSize() * r / rows;
                final int rgb = ctx.pixel(x, y) & 0xFFFFFF;
                if (!test.test(rgb)) {
                    return false;
                }
            }
        }
        return true;
    }

    /* Red over green over blue, a hair of rounding allowed: the amber phosphor at any brightness. */
    private static boolean amber(final int rgb) {
        final int r = (rgb >> 16) & 0xFF;
        final int g = (rgb >> 8) & 0xFF;
        final int b = rgb & 0xFF;
        return r + 2 >= g && g + 2 >= b;
    }

    private static boolean lit(final ClientTestContext ctx, final BlockPos at) {
        return ctx.mc().level != null && ctx.mc().level.getBlockState(ctx.abs(at)).getBlock() instanceof MonitorBlock
                && ctx.mc().level.getBlockState(ctx.abs(at)).getValue(MonitorBlock.LIT);
    }

    private static MonitorBlockEntity clientMonitor(final ClientTestContext ctx, final BlockPos at) {
        return (MonitorBlockEntity) ctx.mc().level.getBlockEntity(ctx.abs(at));
    }

    private static Block[] kinds() {
        return new Block[] {ComputingModule.MONO_I_MONITOR.get(), ComputingModule.VINTAGE_MONITOR.get(),
            ComputingModule.AMBER_MONITOR.get(), ComputingModule.CGA_MONITOR.get(),
            ComputingModule.LEGACY_MONITOR.get(), ComputingModule.TRANSITION_MONITOR.get(),
            ComputingModule.MONITOR.get(), ComputingModule.COLOR_MONITOR.get()};
    }

    private static void buildVintage(final PersonalComputerBlockEntity computer) {
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SIMM_4.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_300.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(HardwareItems.GPU_VGA_256.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
    }
}
