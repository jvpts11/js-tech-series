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
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.NetworkManagerApp;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.NetworkServicesPayload;
import dev.jstech.tests.testkit.TestEngines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The Network Manager's Services tab on a Frames XP Mainframe: the factory engine running, replacing it with another
 * installed engine through the dialog and its steps, and the card once the engine is stopped.
 */
public final class NetworkServicesClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final int BOOT_WAIT = 600;
    private static final int REPLACE_WAIT = 400;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");

    private NetworkServicesClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void services_showTheEngineAndReplaceItThroughItsSteps(final ClientTestContext ctx) {
        mainframeDesktop(ctx)
                .then(SETTLE, () -> DesktopScreen.requestOpen(NetworkManagerApp.TITLE))
                .thenWaitUntil(() -> manager(ctx) != null, SCREEN_WAIT, "the Network Manager to open")
                .then(SETTLE * 4, () -> ctx.clickDesktop(manager(ctx).servicesTabPoint()))
                .thenWaitUntil(() -> manager(ctx).servicesReady(), SCREEN_WAIT,
                        "the Services tab to hear from the Mainframe")
                // The advancements of building the machine would cover the window's right end in the pictures.
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE, "services")
                .then(0, () -> ctx.assertTrue(manager(ctx).servicesEngineState()
                        == NetworkServicesPayload.ENGINE_RUNNING, "the factory engine runs"))
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).servicesButtonPoint("replace")))
                .thenWaitUntil(() -> manager(ctx).servicesChooserOpen(), SCREEN_WAIT, "the Replace dialog to open")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).servicesOptionPoint(TestEngines.PLAIN.toString())))
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE, "replace-dialog")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).servicesConfirmPoint()))
                .thenWaitUntil(() -> manager(ctx).servicesProgressOpen(), SCREEN_WAIT, "the steps to show")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(8, "replacing")
                .then(0, () -> ctx.assertTrue(manager(ctx).servicesReplacementStep() != null,
                        "a replacement is under way"))
                .thenWaitUntil(() -> manager(ctx).servicesEngineState() == NetworkServicesPayload.ENGINE_RUNNING
                        && !manager(ctx).servicesProgressOpen(), REPLACE_WAIT, "the new engine to come up")
                .thenScreenshot(SETTLE, "replaced")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).servicesButtonPoint("stop")))
                .thenWaitUntil(() -> manager(ctx).servicesEngineState() == NetworkServicesPayload.ENGINE_STOPPED,
                        SCREEN_WAIT, "the engine to stop")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE, "no-engine");
    }

    /** A Frames XP Mainframe with a second engine installed, brought up at its desktop on a monitor. */
    private static ClientTestContext mainframeDesktop(final ClientTestContext ctx) {
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
                    // A graphics card gives the machine peripheral ports, which is what the monitor links to.
                    inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.GPU_HD_7970.get()));
                    inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
                    machine.installOs(FRAMES_XP);
                    machine.installEngine(TestEngines.PLAIN);
                    machine.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT);
    }

    @Nullable
    private static NetworkManagerApp manager(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        if (desktop == null) {
            return null;
        }
        final DesktopWindow window = desktop.windowFor(NetworkManagerApp.TITLE);
        return window != null && window.app() instanceof NetworkManagerApp app ? app : null;
    }
}
