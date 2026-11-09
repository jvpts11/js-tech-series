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
import dev.jstech.computers.blockentity.ChargingDockBlockEntity;
import dev.jstech.computers.client.portable.MobilePage;
import dev.jstech.computers.client.portable.PortableScreen;
import dev.jstech.computers.client.portable.PortableViews;
import dev.jstech.computers.portable.PortableBattery;
import dev.jstech.computers.portable.PortableBoot;
import dev.jstech.computers.portable.PortableItem;
import dev.jstech.computers.portable.PortableModel;
import dev.jstech.computers.portable.Portables;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.lwjgl.glfw.GLFW;

/**
 * The portables (computers/docs/design/wireless.md): a qPhone X in the hand, its screen lit; held up, its lock over
 * the blurred world in its own face, let go with a swipe up to its home; a Sunsung Nebula S10 with its shade pulled
 * down; a Hightide Pocket's Today; and a qPhone 5 standing in its Standard dock, its battery filling.
 */
public final class PortableClientTests {

    private static final int SETTLE = 4;
    private static final int WAIT = 200;
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos ACCESS_POINT = new BlockPos(5, 2, 2);
    private static final BlockPos DOCK = new BlockPos(5, 2, 6);
    private static final BlockPos PLAYER = new BlockPos(5, 2, 8);

    private PortableClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void portables_inTheHandHeldUpAndInTheirDock(final ClientTestContext ctx) {
        ctx.thenBuild(0, PortableClientTests::build)
                .thenTeleport(SETTLE, PLAYER, Direction.NORTH)
                .then(0, () -> {
                    ctx.give(0, new ItemStack(PortableItems.QPHONE_X.get()));
                    ctx.give(1, new ItemStack(PortableItems.SUNSUNG_NEBULA_S10.get()));
                    ctx.give(2, new ItemStack(PortableItems.HIGHTIDE_POCKET.get()));
                    ctx.selectHotbar(0);
                })
                .thenWaitUntil(() -> ready(ctx), WAIT, "the qPhone X to finish its start")
                .thenScreenshot(SETTLE, "in_hand")
                .then(0, () -> use(ctx))
                .thenAwaitScreen(PortableScreen.class, WAIT)
                .thenScreenshot(SETTLE, "qphone_x_lock")
                .then(SETTLE, () -> swipeUp(ctx, PortableModel.QPHONE_X))
                .thenAssert(SETTLE, () -> page(ctx) == MobilePage.HOME, "a swipe up lets the lock go")
                .thenScreenshot(SETTLE, "qphone_x_home")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT)
                .then(0, () -> ctx.selectHotbar(1))
                .thenWaitUntil(() -> ready(ctx), WAIT, "the Nebula S10 to finish its start")
                .then(2, () -> use(ctx))
                .thenAwaitScreen(PortableScreen.class, WAIT)
                .thenScreenshot(SETTLE, "nebula_lock")
                .then(SETTLE, () -> swipeUp(ctx, PortableModel.SUNSUNG_NEBULA_S10))
                .then(SETTLE, () -> pullDown(ctx))
                .thenAssert(SETTLE, () -> page(ctx) == MobilePage.ALERTS, "a pull from the top brings the shade")
                .thenScreenshot(SETTLE, "nebula_shade")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT)
                .then(0, () -> ctx.selectHotbar(2))
                .thenWaitUntil(() -> ready(ctx), WAIT, "the Hightide Pocket to finish its start")
                .then(2, () -> use(ctx))
                .thenAwaitScreen(PortableScreen.class, WAIT)
                .then(SETTLE, () -> {
                    final PortableScreen screen = ctx.screen(PortableScreen.class);
                    // Unlock, on the left soft key.
                    ctx.click(screen.guiX(20), screen.guiY(PortableModel.HIGHTIDE_POCKET.screenHeight() - 6));
                })
                .thenAssert(SETTLE, () -> page(ctx) == MobilePage.HOME, "the Unlock key lets the lock go")
                .thenScreenshot(SETTLE, "pocket_today")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT)
                .thenTeleport(SETTLE, DOCK.south(2), Direction.NORTH)
                .thenWaitUntil(() -> dockCharging(ctx), WAIT, "the dock to charge its qPhone 5")
                .thenScreenshot(20, "dock");
    }

    /*
     * A running Mainframe, a router onto the access line, a Transition desk access point at its end, and south of it a
     * Standard dock, facing the player, with a qPhone 5 half charged in it.
     */
    private static void build(final TestWorldBuilder world) {
        world.placeRunningMainframe(MAINFRAME);
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE);
        world.setBlock(ACCESS_POINT, ComputingModule.TRANSITION_ACCESS_POINT.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH)
                .setValue(AccessPointBlock.MOUNT, AccessPointMount.DESK));
        world.setBlock(DOCK, PortableItems.STANDARD_CHARGING_DOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        final ItemStack phone = new ItemStack(PortableItems.QPHONE_5.get());
        Portables.setBattery(phone, PortableModel.QPHONE_5, PortableBattery.capacity(PortableModel.QPHONE_5) / 2);
        world.blockEntity(DOCK, ChargingDockBlockEntity.class).put(phone);
    }

    /* Whether the device in the player's hand on their game has finished its start. */
    private static boolean ready(final ClientTestContext ctx) {
        final ItemStack held = ctx.player().getMainHandItem();
        return held.getItem() instanceof PortableItem && PortableBoot.stage(Portables.onSince(held),
                ctx.mc().level.getGameTime()) == PortableBoot.Stage.READY;
    }

    /* The page the system of the device in the player's hand is on. */
    private static MobilePage page(final ClientTestContext ctx) {
        final UUID id = Objects.requireNonNull(Portables.id(ctx.player().getMainHandItem()), "a device carried");
        return PortableViews.session(id).page();
    }

    /* Swipes up the device's screen from its foot, as its lock and its home bar ask. */
    private static void swipeUp(final ClientTestContext ctx, final PortableModel model) {
        final PortableScreen screen = ctx.screen(PortableScreen.class);
        final double x = screen.guiX(model.screenWidth() / 2.0);
        final double from = screen.guiY(model.screenHeight() - 4);
        final double to = screen.guiY(model.screenHeight() / 4.0);
        screen.mouseClicked(x, from, 0);
        screen.mouseDragged(x, to, 0, 0.0, to - from);
        screen.mouseReleased(x, to, 0);
    }

    /* Pulls the device's screen down from its top. */
    private static void pullDown(final ClientTestContext ctx) {
        final PortableScreen screen = ctx.screen(PortableScreen.class);
        final double x = screen.guiX(30);
        final double from = screen.guiY(4);
        final double to = screen.guiY(120);
        screen.mouseClicked(x, from, 0);
        screen.mouseDragged(x, to, 0, 0.0, to - from);
        screen.mouseReleased(x, to, 0);
    }

    /* Whether the dock has its qPhone 5 and is filling its battery, as the player's game has it. */
    private static boolean dockCharging(final ClientTestContext ctx) {
        return ctx.mc().level.getBlockEntity(ctx.abs(DOCK)) instanceof ChargingDockBlockEntity dock
                && !dock.held().isEmpty() && dock.isCharging();
    }

    private static void use(final ClientTestContext ctx) {
        Objects.requireNonNull(ctx.mc().gameMode, "no game mode").useItem(ctx.player(), InteractionHand.MAIN_HAND);
    }
}
