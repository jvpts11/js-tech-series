/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.PortableItems;
import dev.jstech.computers.block.AccessPointBlock;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.portable.MobileAppState;
import dev.jstech.computers.client.portable.MobileSession;
import dev.jstech.computers.client.portable.MobileTap;
import dev.jstech.computers.client.portable.PortableApps;
import dev.jstech.computers.client.portable.PortableScreen;
import dev.jstech.computers.client.portable.PortableViews;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload.Action;
import dev.jstech.computers.operation.payload.portable.PortableNetworkPayload;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.PortableBoot;
import dev.jstech.computers.portable.PortableDevices;
import dev.jstech.computers.portable.PortableItem;
import dev.jstech.computers.portable.Portables;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.wireless.AccessPointMount;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The portables' apps (computers/docs/design/wireless.md), each in one of the families that has it, as the K0.11 apps
 * page drew them: the network's Stock on a qPhone 5 and its camera; an item, the Device tab, prospecting and the
 * Messenger with no service on a Sunsung Nebula S10; the Pocket Pro's list with its Menu; the alerts and the gallery on
 * a Hightide Wave; Maps on a qPhone X; and the Craft tab on a qPad Pro.
 */
public final class PortableAppsClientTests {

    private static final int SETTLE = 6;
    private static final int DATA = 50;
    private static final int WAIT = 200;
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos RACK = new BlockPos(2, 2, 1);
    private static final BlockPos ACCESS_POINT = new BlockPos(5, 2, 2);
    private static final BlockPos PLAYER = new BlockPos(5, 2, 8);

    private PortableAppsClientTests() {
    }

    @ClientTest(timeoutTicks = 4800)
    public static void apps_ofEachFamily(final ClientTestContext ctx) {
        ctx.thenBuild(0, PortableAppsClientTests::build)
                .thenTeleport(SETTLE, PLAYER, Direction.NORTH)
                .then(0, () -> {
                    ctx.give(0, new ItemStack(PortableItems.QPHONE_5.get()));
                    ctx.give(1, new ItemStack(PortableItems.SUNSUNG_NEBULA_S10.get()));
                    ctx.give(2, new ItemStack(PortableItems.HIGHTIDE_POCKET_PRO.get()));
                    ctx.give(3, new ItemStack(PortableItems.HIGHTIDE_WAVE.get()));
                    ctx.give(4, new ItemStack(PortableItems.QPHONE_X.get()));
                    ctx.give(5, new ItemStack(PortableItems.QPAD_PRO.get()));
                });
        raise(ctx, 0);
        open(ctx, MobileApp.NETWORK).thenScreenshot(DATA, "qphone5_network_stock");
        open(ctx, MobileApp.CAMERA).thenScreenshot(DATA, "qphone5_camera");
        put(ctx);
        raise(ctx, 1);
        open(ctx, MobileApp.NETWORK)
                .then(DATA, () -> apps(ctx).choose(StorageKey.of(Items.IRON_INGOT)))
                .thenScreenshot(SETTLE, "s10_network_item")
                .then(0, () -> tap(ctx, MobileTap.run(() -> {
                    PacketDistributor.sendToServer(PortableActionPayload.item(device(ctx), Action.REQUEST,
                            StorageKey.of(Items.IRON_INGOT), 16));
                    apps(ctx).networkTab(MobileAppState.DEVICE);
                })))
                .thenWaitUntil(() -> stored(ctx) > 0, WAIT, "the request to land on the device")
                .thenScreenshot(SETTLE, "s10_network_device");
        // Prospecting comes from the store; the device gets it as the store would have given it.
        ctx.thenWaitUntilServer(level -> PortableDevices.get(level.getServer()).install(device(ctx),
                MobileApp.PROSPECTING) || PortableDevices.get(level.getServer()).find(device(ctx)).installed()
                .contains(MobileApp.PROSPECTING), WAIT, "prospecting installed", level -> "not installed");
        open(ctx, MobileApp.PROSPECTING).thenScreenshot(DATA, "s10_prospecting");
        put(ctx);
        raise(ctx, 2);
        open(ctx, MobileApp.NETWORK)
                .then(DATA, () -> apps(ctx).toggleMenu())
                .thenScreenshot(SETTLE, "pocket_pro_network_menu");
        put(ctx);
        raise(ctx, 3);
        open(ctx, MobileApp.ALERTS).thenScreenshot(DATA, "wave_alerts");
        open(ctx, MobileApp.GALLERY).thenScreenshot(DATA, "wave_gallery");
        put(ctx);
        raise(ctx, 4);
        open(ctx, MobileApp.MAPS).thenScreenshot(DATA, "qphone_x_maps");
        put(ctx);
        raise(ctx, 5);
        open(ctx, MobileApp.NETWORK)
                .then(DATA, () -> apps(ctx).networkTab(MobileAppState.CRAFT))
                .thenScreenshot(SETTLE, "qpad_pro_network_craft");
        put(ctx);
    }

    /* A running Mainframe, a server stocked beside its cable, a router, the access line and an access point. */
    private static void build(final TestWorldBuilder world) {
        world.placeRunningMainframe(MAINFRAME);
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        final ServerRackBlockEntity rack = world.placeSeededRack(RACK);
        rack.getServerStorage(0).insert(Items.IRON_INGOT, 64);
        rack.getServerStorage(0).insert(Items.REDSTONE, 256);
        rack.getServerStorage(0).insert(Items.GLASS, 40);
        rack.getServerStorage(0).insert(Items.COBBLESTONE, 512);
        rack.getServerStorage(0).insert(Items.OAK_LOG, 96);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE);
        // Its back, where its cable comes in, to the access line.
        world.setBlock(ACCESS_POINT, ComputingModule.TRANSITION_ACCESS_POINT.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
                .setValue(AccessPointBlock.MOUNT, AccessPointMount.DESK));
    }

    /* Takes the device in the hotbar's slot {@code slot}, waits for its start and holds it up, unlocked. */
    private static ClientTestContext raise(final ClientTestContext ctx, final int slot) {
        return ctx.then(0, () -> ctx.selectHotbar(slot))
                .thenWaitUntil(() -> ready(ctx), WAIT, "the device to finish its start")
                .then(2, () -> Objects.requireNonNull(ctx.mc().gameMode, "no game mode")
                        .useItem(ctx.player(), InteractionHand.MAIN_HAND))
                .thenAwaitScreen(PortableScreen.class, WAIT)
                .then(SETTLE, () -> tap(ctx, MobileTap.UNLOCK));
    }

    private static ClientTestContext open(final ClientTestContext ctx, final MobileApp app) {
        return ctx.then(SETTLE, () -> tap(ctx, MobileTap.open(app)));
    }

    /* Puts the device down. */
    private static void put(final ClientTestContext ctx) {
        ctx.then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE)).thenAwaitNoScreen(WAIT);
    }

    private static void tap(final ClientTestContext ctx, final MobileTap tap) {
        session(ctx).tap(tap, PacketDistributor::sendToServer);
    }

    private static MobileSession session(final ClientTestContext ctx) {
        return PortableViews.session(device(ctx));
    }

    private static MobileAppState apps(final ClientTestContext ctx) {
        return session(ctx).apps();
    }

    private static UUID device(final ClientTestContext ctx) {
        return Objects.requireNonNull(Portables.id(ctx.player().getMainHandItem()), "a device carried");
    }

    /* How many items the device in the hand holds, as its network app was last told. */
    private static long stored(final ClientTestContext ctx) {
        final PortableNetworkPayload view = PortableApps.network(device(ctx));
        return view == null ? 0L : view.stored().stream().mapToLong(NetworkItemEntry::total).sum();
    }

    /* Whether the device in the player's hand on their game has finished its start. */
    private static boolean ready(final ClientTestContext ctx) {
        final ItemStack held = ctx.player().getMainHandItem();
        return held.getItem() instanceof PortableItem && PortableBoot.stage(Portables.onSince(held),
                ctx.mc().level.getGameTime()) == PortableBoot.Stage.READY;
    }
}
