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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import java.util.function.Function;

/**
 * Draws the medium in a device's bay as the very item the player put in, at the bone the model marks its place
 * with, so it moves with the clip that takes it in or out. A disk or a disc lies flat at half its size, the end that
 * goes in first pointing into the machine; a USB stick is its own model at half size, turned so its plug goes into
 * the port, with the joint of plug and body on the port's face.
 *
 * @param <T> the device
 */
public final class BayMediumLayer<T extends BlockEntity & GeoAnimatable> extends BlockAndItemGeoLayer<T> {

    /** The medium a device draws in its bay. */
    private final Function<T, ItemStack> medium;

    /** Where the stick's plug meets its body, along the model's length, from the model's middle. */
    private static final float USB_JOINT = 3F / 16F;
    /** How far the stick's middle stands above the middle of its model. */
    private static final float USB_RISE = 0.5F / 16F;

    public BayMediumLayer(final GeoRenderer<T> renderer, final Function<T, ItemStack> medium) {
        super(renderer);
        this.medium = medium;
    }

    @Override
    @Nullable
    protected ItemStack getStackForBone(final GeoBone bone, final T device) {
        final ItemStack drawn = medium.apply(device);
        if (!(drawn.getItem() instanceof FormattedMediaItem item)) {
            return null;
        }
        final boolean usb = item.format() == MediaFormat.USB;
        return switch (bone.getName()) {
            case MediaBay.MEDIUM_BONE -> usb ? null : drawn;
            case MediaBay.USB_BONE -> usb ? drawn : null;
            default -> null;
        };
    }

    @Override
    protected void renderStackForBone(final PoseStack poseStack, final GeoBone bone, final ItemStack stack,
                                      final T device, final MultiBufferSource bufferSource, final float partialTick,
                                      final int packedLight, final int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        if (MediaBay.USB_BONE.equals(bone.getName())) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            poseStack.translate(-USB_JOINT, -USB_RISE, 0);
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        }
        super.renderStackForBone(poseStack, bone, stack, device, bufferSource, partialTick, packedLight,
                packedOverlay);
        poseStack.popPose();
    }
}
