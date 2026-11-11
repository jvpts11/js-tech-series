/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.block.MonitorKind;
import dev.jstech.computers.block.MonitorPanel;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.monitor.TouchGesture;
import dev.jstech.computers.operation.payload.TouchMonitorPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The player's finger on a touch monitor's glass (computers/docs/design/peripherals.md): the use key, with the
 * crosshair on the picture of a touch monitor, touches it there instead of using the block or the item in hand, and
 * while it stays down the finger follows the crosshair. The first touch of a monitor whose screen the game holds none
 * of sits the player down at it, as a click on a monitor does; the touches after it reach that screen as a tap, a drag
 * or, held still a second, the right button. Sneaking uses the block as usual.
 */
public final class TouchFinger {

    private static final TouchGesture GESTURE = new TouchGesture();
    /* The touch monitor's screen the finger is on, and the last point of its picture it was on. */
    @Nullable
    private static BlockPos on;
    private static double lastX;
    private static double lastY;

    private TouchFinger() {
    }

    /** Hands the game's bus what this answers, once, from the mod's client constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, TouchFinger::onUse);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, TouchFinger::onTick);
    }

    /**
     * Where the crosshair is on the picture of the touch monitor whose screen stands at {@code monitor}, in its pixels,
     * as {@code {x, y}}; null when it is elsewhere.
     */
    @Nullable
    public static double[] pointOn(final BlockPos monitor) {
        final Aim aim = aim();
        return aim == null || !aim.screen().equals(monitor) ? null : new double[] {aim.x(), aim.y()};
    }

    /**
     * The finger goes down where the crosshair is, from the use key or from the typing line's button: false when the
     * crosshair is on no touch monitor's picture, and the press is the game's.
     */
    static boolean press() {
        if (GESTURE.down()) {
            return true;
        }
        final Aim aim = aim();
        if (aim == null) {
            return false;
        }
        if (TouchScreens.holds(aim.screen())) {
            on = aim.screen();
            lastX = aim.x();
            lastY = aim.y();
            GESTURE.press(aim.x(), aim.y());
        } else if (!TouchScreens.waiting(aim.screen())) {
            TouchScreens.expect(aim.screen());
            PacketDistributor.sendToServer(new TouchMonitorPayload(aim.block()));
        }
        return true;
    }

    /** Lets go of the finger without a word to the screen under it, which went away. */
    static void cancel() {
        GESTURE.cancel();
        on = null;
    }

    /*
     * The use key with the crosshair on a touch monitor's picture is a touch, and nothing else happens; while the
     * finger is down, the key's repeats are swallowed, the finger already being on the glass.
     */
    private static void onUse(final InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        final boolean repeat = GESTURE.down();
        if (repeat || press()) {
            event.setCanceled(true);
            event.setSwingHand(!repeat);
        }
    }

    /* While the finger is down it follows the crosshair over the glass, and lifts when the key comes up. */
    private static void onTick(final ClientTickEvent.Post event) {
        if (!GESTURE.down() || on == null) {
            return;
        }
        final BlockPos screen = on;
        if (!fingerDown()) {
            on = null;
            TouchScreens.deliver(screen, GESTURE.release());
            return;
        }
        final double[] at = pointOn(screen);
        if (at != null) {
            lastX = at[0];
            lastY = at[1];
        }
        TouchScreens.deliver(screen, GESTURE.held(lastX, lastY));
    }

    /* Whether the finger is still down: the use key in the world, or the typing line's button while it is up. */
    private static boolean fingerDown() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof TouchTypingScreen line) {
            return line.fingerDown();
        }
        return minecraft.screen == null && minecraft.options.keyUse.isDown();
    }

    /* The touch monitor's picture under the crosshair, and where on it, or null. */
    @Nullable
    private static Aim aim() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.player.isSecondaryUseActive()) {
            return null;
        }
        final HitResult hit = minecraft.player.pick(minecraft.player.blockInteractionRange(), 1.0F, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        final BlockPos pos = block.getBlockPos();
        final BlockState state = minecraft.level.getBlockState(pos);
        if (!(state.getBlock() instanceof MonitorBlock monitor) || !monitor.kind().touch()
                || !(minecraft.level.getBlockEntity(pos) instanceof MonitorBlockEntity entity)) {
            return null;
        }
        final double[] front = MonitorBlock.frontPoint(state, pos, block);
        if (front == null) {
            return null;
        }
        // A big screen is one picture: the point is counted from its top left, across all of its monitors.
        final MonitorPanel panel = entity.panel();
        final int wide = panel == null ? 1 : panel.width();
        final int tall = panel == null ? 1 : panel.height();
        final int across = panel == null ? 0 : (pos.getX() - panel.origin().getX()) * panel.right().getStepX()
                + (pos.getZ() - panel.origin().getZ()) * panel.right().getStepZ();
        final int up = panel == null ? 0 : pos.getY() - panel.origin().getY();
        final double x = across * MonitorKind.FRONT + front[0];
        final double y = (tall - 1 - up) * MonitorKind.FRONT + front[1];
        final float[] picture = MonitorScreenRenderer.picture(monitor.kind(), wide, tall);
        final double fx = (x - picture[0]) / (picture[2] - picture[0]);
        final double fy = (y - picture[1]) / (picture[3] - picture[1]);
        if (fx < 0 || fx >= 1 || fy < 0 || fy >= 1) {
            return null;
        }
        return new Aim(pos.immutable(), entity.screen().getBlockPos().immutable(), fx * MonitorGlass.WIDTH,
                fy * MonitorGlass.HEIGHT);
    }

    /**
     * The crosshair on a touch monitor's picture.
     *
     * @param block  the monitor block under it
     * @param screen the monitor whose screen the picture is, the one a big screen's link runs through
     * @param x      where across the picture, in its pixels
     * @param y      where down it
     */
    private record Aim(BlockPos block, BlockPos screen, double x, double y) {
    }
}
