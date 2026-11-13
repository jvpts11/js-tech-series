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
import dev.jstech.computers.client.portable.MobileTap;
import dev.jstech.computers.client.portable.PortableApps;
import dev.jstech.computers.client.portable.PortableScreen;
import dev.jstech.computers.client.portable.PortableViews;
import dev.jstech.computers.operation.payload.portable.PortableWalletPayload;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.PortableBattery;
import dev.jstech.computers.portable.PortableBoot;
import dev.jstech.computers.portable.PortableDevices;
import dev.jstech.computers.portable.PortableItem;
import dev.jstech.computers.portable.PortableModel;
import dev.jstech.computers.portable.Portables;
import dev.jstech.computers.teracoin.FoundBlock;
import dev.jstech.computers.teracoin.TeracoinChain;
import dev.jstech.computers.teracoin.TeracoinState;
import dev.jstech.computers.teracoin.Trc;
import dev.jstech.computers.wireless.AccessPointMount;
import dev.jstech.core.client.studio.StudioClient;
import dev.jstech.core.client.studio.StudioScreen;
import dev.jstech.core.client.studio.StudioSheet;
import dev.jstech.core.client.studio.StudioTexts;
import dev.jstech.core.client.studio.StudioTool;
import dev.jstech.core.text.GameText;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import dev.jstech.testkit.client.ClientTestHelper;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.network.PacketDistributor;
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
    /*
     * The blocks paid into the wallet, each fifty coins: a thousand in all; a phone is sent the newest eight, more than
     * its three history rows hold.
     */
    private static final int PAYMENTS = 20;
    private static final int REWARD = 50;
    /* How far from the glass's right edge the balance's unit must stop, in GUI pixels. */
    private static final int CLEAR = 3;
    /* Blockwright's gold, which the unit is written in, and how near a pixel must come to it to be it. */
    private static final int GOLD = 0xF0B23A;
    private static final int NEAR = 24;

    private PortableClientTests() {
    }

    /**
     * The studio reads a portable as the game draws it: its item has a view of its own for the inventory, a picture
     * there, and is drawn by code in the hand, so nothing of it is a texture missing.
     */
    @ClientTest(timeoutTicks = 300)
    public static void portables_haveNoTextureMissingInTheStudio(final ClientTestContext ctx) {
        final String missing = GameText.resolve(StudioTexts.NO_TEXTURE);
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.then(0, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.THINGS)))
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .then(SETTLE, () -> {
                    final StudioScreen studio = ctx.screen(StudioScreen.class);
                    for (final ResourceLocation portable : List.of(PortableItems.QPHONE.getId(),
                            PortableItems.QPAD_PRO.getId())) {
                        ctx.assertTrue(studio.choose(portable), portable + " is among the things");
                        final List<String> found = studio.sheet().orElseThrow().checks().stream()
                                .map(StudioSheet.Check::title).toList();
                        ctx.assertTrue(!found.contains(missing), portable + " has no texture missing: " + found);
                    }
                });
    }

    /**
     * A qPhone's Crypto Wallet with twenty blocks paid into it (computers/docs/design/teracoin.md): its thousand coins
     * whole on the card, their unit clear of the glass's edge, and a turn of the wheel over the history bringing the
     * older payments up.
     */
    @ClientTest(timeoutTicks = 1600)
    public static void wallet_showsABigBalanceWholeAndScrollsItsHistory(final ClientTestContext ctx) {
        final UUID[] id = new UUID[1];
        final long[] before = new long[1];
        ctx.thenBuild(0, PortableClientTests::network)
                .thenTeleport(SETTLE, PLAYER, Direction.NORTH)
                .then(0, () -> {
                    ctx.give(0, new ItemStack(PortableItems.QPHONE.get()));
                    ctx.selectHotbar(0);
                })
                .thenWaitUntil(() -> ready(ctx), WAIT, "the qPhone to finish its start")
                .then(0, () -> id[0] = Objects.requireNonNull(Portables.id(ctx.player().getMainHandItem())))
                .thenServer(0, level -> pay(level, id[0]))
                .then(0, () -> use(ctx))
                .thenAwaitScreen(PortableScreen.class, WAIT)
                .then(SETTLE, () -> PortableViews.session(id[0]).tap(MobileTap.UNLOCK, PacketDistributor::sendToServer))
                .then(SETTLE, () -> PortableViews.session(id[0]).tap(MobileTap.open(MobileApp.CRYPTO_WALLET),
                        PacketDistributor::sendToServer))
                .thenWaitUntil(() -> PortableApps.wallet(id[0]) != null && PortableApps.wallet(id[0]).moves().size()
                        >= PortableWalletPayload.MOST_MOVES, WAIT, "the wallet's newest payments, all a phone is sent")
                .then(SETTLE, () -> {
                    final PortableScreen screen = ctx.screen(PortableScreen.class);
                    final double edge = screen.guiX(PortableModel.QPHONE.screenWidth());
                    ctx.assertTrue(goldReach(ctx, screen) < edge - CLEAR, "a thousand coins and their unit fit the "
                            + "card, the unit clear of the glass's edge at " + edge + ": gold reaches "
                            + goldReach(ctx, screen));
                    before[0] = history(ctx, screen);
                    ctx.scroll(screen.guiX(PortableModel.QPHONE.screenWidth() / 2.0),
                            screen.guiY(PortableModel.QPHONE.screenHeight() * 0.8), -3);
                })
                .then(SETTLE, () -> ctx.assertTrue(history(ctx, ctx.screen(PortableScreen.class)) != before[0],
                        "the wheel over the history brings the older payments up; the list is scrolled "
                                + PortableViews.session(id[0]).scroll() + " rows"))
                .then(0, () -> swipeUp(ctx, PortableModel.QPHONE))
                .thenAssert(SETTLE, () -> page(ctx) == MobilePage.APP, "a swipe up from the foot of a qPhone that has "
                        + "its home button leaves the app open, as only the qPhone X's goes home");
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

    /* A running Mainframe and its access line to a Transition desk access point, its cable coming in from behind. */
    private static void network(final TestWorldBuilder world) {
        world.placeRunningMainframe(MAINFRAME);
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE);
        world.setBlock(ACCESS_POINT, ComputingModule.TRANSITION_ACCESS_POINT.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
                .setValue(AccessPointBlock.MOUNT, AccessPointMount.DESK));
    }

    /* The device's Crypto Wallet installed, a wallet kept on it, and twenty blocks found alone paid into that. */
    private static void pay(final ServerLevel level, final UUID device) {
        final PortableDevices devices = PortableDevices.get(level.getServer());
        devices.install(device, MobileApp.CRYPTO_WALLET);
        final TeracoinState teracoin = TeracoinState.of(level.getServer());
        final TeracoinChain.Keys keys = teracoin.chain().createWallet(RandomGenerator.getDefault());
        devices.keepWallet(device, keys.address(), keys.key());
        // Each found at a height of its own, so each row of the history reads differently.
        final int height = teracoin.chain().height();
        for (int n = 0; n < PAYMENTS; n++) {
            teracoin.chain().creditAlone(new FoundBlock(height + n, level.getGameTime(), 0L, Trc.of(REWARD), 1L,
                    "test", "test", keys.address(), "", false));
        }
        teracoin.setDirty();
    }

    /* The rightmost GUI pixel of the device's glass, over its upper half, drawn in Blockwright's gold; -1 for none. */
    private static int goldReach(final ClientTestContext ctx, final PortableScreen screen) {
        final int left = (int) screen.guiX(0);
        final int right = (int) screen.guiX(PortableModel.QPHONE.screenWidth());
        final int top = (int) screen.guiY(0);
        final int bottom = (int) screen.guiY(PortableModel.QPHONE.screenHeight() / 2.0);
        int reach = -1;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = top; y < bottom; y++) {
                for (int x = left; x < right; x++) {
                    if (near(frame.pixel(x, y), GOLD)) {
                        reach = Math.max(reach, x);
                    }
                }
            }
        }
        return reach;
    }

    /* What the lower part of the device's glass, where the history's rows are, looks like, as one number. */
    private static long history(final ClientTestContext ctx, final PortableScreen screen) {
        final int left = (int) screen.guiX(4);
        final int right = (int) screen.guiX(PortableModel.QPHONE.screenWidth() - 4);
        final int top = (int) screen.guiY(PortableModel.QPHONE.screenHeight() * 0.62);
        final int bottom = (int) screen.guiY(PortableModel.QPHONE.screenHeight() - 4);
        long seen = 0L;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = top; y < bottom; y++) {
                for (int x = left; x < right; x++) {
                    seen = seen * 31 + frame.pixel(x, y);
                }
            }
        }
        return seen;
    }

    /* Whether an ARGB pixel comes near enough an RGB colour to be it, as text drawn in that colour is. */
    private static boolean near(final int argb, final int rgb) {
        return Math.abs((argb >> 16 & 0xFF) - (rgb >> 16 & 0xFF)) < NEAR
                && Math.abs((argb >> 8 & 0xFF) - (rgb >> 8 & 0xFF)) < NEAR
                && Math.abs((argb & 0xFF) - (rgb & 0xFF)) < NEAR;
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
