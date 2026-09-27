/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Draws a Pattern Encoder as its era's device. It states what it is doing with its lamps: the power lamp lit while a
 * computer is on the other end of its cable, and the activity lamp blinking while a pattern is written and held lit
 * on an error. The medium in the bay is the very item the player put in, drawn where the model marks its place: on
 * the tray, in the slot or in the port, moving with the clip that takes it in or out.
 */
public final class PatternEncoderRenderer extends GeoBlockRenderer<PatternEncoderBlockEntity> {

    /** Ticks a blinking lamp stays lit, and then dark: twice a second. */
    private static final int BLINK_TICKS = 5;

    public PatternEncoderRenderer(final BlockEntityRendererProvider.Context context) {
        super(new PatternEncoderGeoModel());
        addRenderLayer(new MediumLayer(this));
    }

    @Override
    public void preRender(final PoseStack poseStack, final PatternEncoderBlockEntity encoder,
                          final BakedGeoModel model, final MultiBufferSource bufferSource,
                          final VertexConsumer buffer, final boolean isReRender, final float partialTick,
                          final int packedLight, final int packedOverlay, final int colour) {
        super.preRender(poseStack, encoder, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
        // The baked model is shared by every encoder of this era, so every flag is set on every render.
        show(model, PatternEncoderBlockEntity.POWER_LAMP, encoder.ownerPos() != null);
        show(model, PatternEncoderBlockEntity.BUSY_LAMP, activityLit(encoder));
    }

    /* The activity lamp: blinking while a pattern is written, held lit on an error, dark otherwise. */
    private static boolean activityLit(final PatternEncoderBlockEntity encoder) {
        if (encoder.busy()) {
            final Level level = encoder.getLevel();
            return level == null || (level.getGameTime() / BLINK_TICKS) % 2 == 0;
        }
        return encoder.phase() == PatternEncoderBlockEntity.Phase.ERROR;
    }

    private static void show(final BakedGeoModel model, final String bone, final boolean visible) {
        model.getBone(bone).ifPresent(b -> b.setHidden(!visible));
    }

    /**
     * The medium in the bay, drawn as its own item at the bone that marks its place. A disk or a disc lies flat at
     * half its size, the end that goes in first pointing into the machine; a USB stick is its own model at half size,
     * turned so its plug goes into the port, with the joint of plug and body on the port's face.
     */
    private static final class MediumLayer extends BlockAndItemGeoLayer<PatternEncoderBlockEntity> {

        /** Where the stick's plug meets its body, along the model's length, from the model's middle. */
        private static final float USB_JOINT = 3F / 16F;
        /** How far the stick's middle stands above the middle of its model. */
        private static final float USB_RISE = 0.5F / 16F;

        MediumLayer(final GeoRenderer<PatternEncoderBlockEntity> renderer) {
            super(renderer);
        }

        @Override
        @Nullable
        protected ItemStack getStackForBone(final GeoBone bone, final PatternEncoderBlockEntity encoder) {
            final ItemStack medium = encoder.drawnMedium();
            if (!(medium.getItem() instanceof FormattedMediaItem item)) {
                return null;
            }
            final boolean usb = item.format() == MediaFormat.USB;
            return switch (bone.getName()) {
                case "medium" -> usb ? null : medium;
                case "usb" -> usb ? medium : null;
                default -> null;
            };
        }

        @Override
        protected void renderStackForBone(final PoseStack poseStack, final GeoBone bone, final ItemStack stack,
                                          final PatternEncoderBlockEntity encoder,
                                          final MultiBufferSource bufferSource, final float partialTick,
                                          final int packedLight, final int packedOverlay) {
            poseStack.pushPose();
            poseStack.scale(0.5F, 0.5F, 0.5F);
            if ("usb".equals(bone.getName())) {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                poseStack.translate(-USB_JOINT, -USB_RISE, 0);
            } else {
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            }
            super.renderStackForBone(poseStack, bone, stack, encoder, bufferSource, partialTick, packedLight,
                    packedOverlay);
            poseStack.popPose();
        }
    }
}
