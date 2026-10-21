/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.live;

import dev.jstech.core.live.ScreenFace;
import dev.jstech.core.live.ScreenTouchPayload;
import dev.jstech.core.live.ScreenTouches;
import dev.jstech.core.live.TouchArea;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.ApiStatus;

/**
 * Screens in the world on the player's game: the pixel of a block's screen under the crosshair, which the screen's
 * picture reads to light what is under it as the mouse lights a button ({@link #pointerOn}), and a right click there,
 * which presses the screen instead of using the block or the item in hand.
 */
public final class ScreenTouchClient {

    private ScreenTouchClient() {
    }

    /** Listens for the use key, once, from the Core's client. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, ScreenTouchClient::onUse);
    }

    /** The screen pixel the crosshair is on now, if it is on a block's screen. */
    public static Optional<Pointed> pointed() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        final BlockPos pos = hit.getBlockPos();
        final Direction face = hit.getDirection();
        return ScreenTouches.at(minecraft.level.getBlockState(pos), face).flatMap(touch -> {
            final Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            final double[] point = ScreenFace.point(ScreenTouches.sideOf(face), local.x, local.y, local.z);
            return ScreenFace.pixel(touch.area(), point).map(pixel -> new Pointed(pos, face, pixel[0], pixel[1],
                    touch.area()));
        });
    }

    /**
     * Where the crosshair is on the screen of the block at {@code pos}, as {@code {x, y}} in its pixels: what its
     * picture hands its components as the mouse, so the one under the crosshair lights; empty when it is elsewhere.
     */
    public static Optional<int[]> pointerOn(final BlockPos pos) {
        return pointed().filter(at -> at.pos().equals(pos)).map(at -> new int[] {at.x(), at.y()});
    }

    /* A right click with the crosshair on a screen presses it there, and nothing else happens. */
    private static void onUse(final InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        pointed().ifPresent(at -> {
            event.setCanceled(true);
            event.setSwingHand(true);
            PacketDistributor.sendToServer(new ScreenTouchPayload(at.pos(), at.face(), at.x(), at.y()));
        });
    }

    /**
     * The pixel of a block's screen the crosshair is on.
     *
     * @param pos  the block
     * @param face the face its screen is on
     * @param x    the pixel across
     * @param y    the pixel down
     * @param area where on the face the screen is
     */
    public record Pointed(BlockPos pos, Direction face, int x, int y, TouchArea area) {
    }
}
