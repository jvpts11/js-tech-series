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
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.NetworkManagerApp;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.NetworkNodeInfo;
import dev.jstech.tests.testkit.ServerStacks;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The Network Manager telling how its nodes are linked, on a Frames XP Mainframe with an Optical Network Card: its
 * fibre runs to an optical router, from which one rack's fibre goes on straight and another's bends where no router
 * turns it, and a personal computer sits on Gigabit behind a Standard router. The Devices tab with its LINK column and
 * the line saying why the second rack lost its link, the Map drawn by line with the router, its legend and a node's
 * NETWORK card, and the Hardware tab's links.
 */
public final class NetworkLinksClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final int BOOT_WAIT = 600;

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final BlockPos ROUTER = new BlockPos(5, 2, 0);

    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");

    private NetworkLinksClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void links_showOnTheDevicesTheMapAndTheHardware(final ClientTestContext ctx) {
        ctx.thenBuild(0, NetworkLinksClientTests::build)
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> DesktopScreen.requestOpen(NetworkManagerApp.TITLE))
                .thenWaitUntil(() -> manager(ctx) != null, SCREEN_WAIT, "the Network Manager to open")
                .thenWaitUntil(() -> lost(ctx) != null, SCREEN_WAIT, "the rack whose fibre bends to be told")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE, "devices")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).tabPoint("map")))
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE, "map")
                .then(SETTLE, () -> pointAtNode(ctx, manager(ctx).nodesShown().getFirst().id()))
                .thenScreenshot(SETTLE, "map-card")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).tabPoint("hardware")))
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE, "hardware")
                .then(SETTLE, () -> {
                    for (int i = 0; i < 8; i++) {
                        manager(ctx).mouseScrolled(-1);
                    }
                })
                .thenScreenshot(SETTLE, "hardware-links");
    }

    /*
     * The Mainframe with a graphics card for the monitor and an Optical Network Card; north of it its fibre to an
     * optical router, the router's fibre on north to a rack and west to a run that bends to another rack; south of it
     * HBW to a Standard router and Gigabit to a personal computer.
     */
    private static void build(final TestWorldBuilder world) {
        world.setBlock(COMPUTER, ComputingModule.MAINFRAME.get());
        final MainframeBlockEntity machine = world.blockEntity(COMPUTER, MainframeBlockEntity.class);
        final ItemStackHandler inv = machine.getInventory();
        inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_MTX_S_2011.get()));
        inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START, new ItemStack(ComputingModule.GPU_HD_7970.get()));
        inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START + 1,
                new ItemStack(ComputingModule.OPTICAL_NETWORK_CARD.get()));
        inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
        machine.installOs(FRAMES_XP);
        machine.togglePower();
        world.placeMonitor(MONITOR, Direction.EAST);

        world.setBlock(COMPUTER.north(), ComputingModule.FIBRE_CABLE);
        world.setBlock(ROUTER, ComputingModule.OPTICAL_ROUTER.get());
        world.setBlock(ROUTER.north(), ComputingModule.FIBRE_CABLE);
        rack(world, ROUTER.north(2), Direction.NORTH);
        world.setBlock(ROUTER.west(), ComputingModule.FIBRE_CABLE);
        world.setBlock(ROUTER.west(2), ComputingModule.FIBRE_CABLE);
        world.setBlock(ROUTER.west(2).north(), ComputingModule.FIBRE_CABLE);
        world.setBlock(ROUTER.west(2).north(2), ComputingModule.FIBRE_CABLE);
        rack(world, ROUTER.west(2).north(3), Direction.NORTH);

        world.setBlock(COMPUTER.south(), ComputingModule.HBW_CABLE);
        world.setBlock(COMPUTER.south(2), ComputingModule.STANDARD_ROUTER.get());
        world.setBlock(COMPUTER.south(3), ComputingModule.GIGABIT_CABLE);
        world.placeRunningPersonalComputer(COMPUTER.south(4));
    }

    /* A Server Rack at {@code at} facing {@code facing}, its back to the cable, with a server holding a card. */
    private static void rack(final TestWorldBuilder world, final BlockPos at, final Direction facing) {
        world.setBlock(at, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing));
        final ServerRackBlockEntity rack = world.blockEntity(at, ServerRackBlockEntity.class);
        rack.getServers().setStackInSlot(0, ServerStacks.opticalServer());
        rack.insertDrive(0, new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1)));
    }

    /* Rests the pointer on a node's box on the Map, so its card shows. */
    private static void pointAtNode(final ClientTestContext ctx, final String id) {
        final int[] point = manager(ctx).mapNodePoint(id);
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        if (point != null && desktop != null) {
            ctx.pointAt(desktop.desktopX() + point[0] * desktop.desktopScale() + 0.5,
                    desktop.desktopY() + point[1] * desktop.desktopScale() + 0.5);
        }
    }

    @Nullable
    private static NetworkNodeInfo lost(final ClientTestContext ctx) {
        final NetworkManagerApp app = manager(ctx);
        if (app == null) {
            return null;
        }
        for (final NetworkNodeInfo node : app.nodesShown()) {
            if (!node.link().up()) {
                return node;
            }
        }
        return null;
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
