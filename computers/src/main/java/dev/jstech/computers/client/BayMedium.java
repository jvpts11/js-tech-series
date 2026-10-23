/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaBay;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the medium in a device's bay as the very item the player put in, at the bone the model marks its place
 * with, so it moves with the clip that takes it in or out. A disk or a disc lies flat at half its size, the end that
 * goes in first pointing into the machine; a USB stick is its own model at half size, turned so its plug goes into
 * the port, with the joint of plug and body on the port's face, and turned the way its port is: standing up in an
 * upright port, lying down in one that is wide across.
 */
final class BayMedium {

    /** Where the stick's plug meets its body, along the model's length, from the model's middle. */
    static final float USB_JOINT = 3F / 16F;
    /** How far the stick's middle stands above the middle of its model. */
    static final float USB_RISE = 0.5F / 16F;
    private static final float HALF = 0.5F;

    private BayMedium() {
    }

    /**
     * Draws {@code medium} in the bay of the device drawn as {@code instance}, the pose stack standing where its model
     * stands; nothing for anything but a medium.
     *
     * @param portLies whether the device's USB port is wide across rather than upright
     */
    static void draw(final ItemStack medium, final boolean portLies, final KitInstance instance, final PoseStack poses,
                     final MultiBufferSource buffers, final int light, final int overlay,
                     @Nullable final Level level) {
        if (!(medium.getItem() instanceof FormattedMediaItem item)) {
            return;
        }
        final boolean usb = item.format() == MediaFormat.USB;
        poses.pushPose();
        if (KitRenderer.toBone(instance, usb ? MediaBay.USB_BONE : MediaBay.MEDIUM_BONE, poses)) {
            poses.scale(HALF, HALF, HALF);
            if (usb) {
                /*
                 * The model's plug runs along its +x, wide in y and thin in z: turned so the plug goes in, it stands
                 * up, and a quarter turn about its own length lays it down for a port that is wide across.
                 */
                poses.mulPose(Axis.YP.rotationDegrees(-90));
                if (portLies) {
                    poses.mulPose(Axis.XP.rotationDegrees(90));
                }
                poses.translate(-USB_JOINT, -USB_RISE, 0);
            } else {
                poses.mulPose(Axis.YP.rotationDegrees(180));
                poses.mulPose(Axis.XP.rotationDegrees(-90));
            }
            Minecraft.getInstance().getItemRenderer().renderStatic(medium, ItemDisplayContext.NONE, light, overlay,
                    poses, buffers, level, 0);
        }
        poses.popPose();
    }
}
