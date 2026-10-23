/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.os.media.DiscTray;
import dev.jstech.computers.os.media.MediaBay;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.IKitExtras;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Draws a Pattern Encoder as its era's device. It states what it is doing with its lamps: the power lamp lit while a
 * computer is on the other end of its cable, and the activity lamp blinking while a pattern is written and held lit
 * on an error. The medium in the bay is the very item the player put in, drawn where the model marks its place: on
 * the tray, in the slot or in the port, moving with the clip that takes it in or out.
 */
public final class PatternEncoderRenderer implements IKitDriver<PatternEncoderBlockEntity>,
        IKitExtras<PatternEncoderBlockEntity> {

    private static final DiscTray.Clips TRAY = DiscTray.Clips.of("pattern_encoder");

    private PatternEncoderRenderer() {
    }

    /** The renderer of the Pattern Encoders. */
    public static BlockEntityRenderer<PatternEncoderBlockEntity> create(
            final BlockEntityRendererProvider.Context context) {
        final PatternEncoderRenderer drawn = new PatternEncoderRenderer();
        return KitBlockEntityRenderer.<PatternEncoderBlockEntity>look(ComputingLooks.PATTERN_ENCODER)
                .driver(drawn)
                .extras(drawn)
                .build();
    }

    /*
     * The body moves in two ways. A floppy sliding through its slot plays once, set off by the server when the bay's
     * slot fills or empties; the tray rides out and in as its button is pressed, and stands where it was left.
     */
    @Override
    public void drive(final PatternEncoderBlockEntity encoder, final ModelAnimator animator, final double seconds) {
        DeviceClips.play(animator, DiscTray.CONTROLLER, encoder.tray().playing(TRAY, encoder.getLevel()), seconds);
    }

    @Override
    public void move(final PatternEncoderBlockEntity encoder, final ModelPose pose, final double seconds) {
        DeviceLamps.show(pose, MediaBay.POWER_LAMP, encoder.ownerPos() != null);
        DeviceLamps.show(pose, MediaBay.BUSY_LAMP, activityLit(encoder));
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    /* The Standard's USB port stands upright; the Advanced's USB-C lies wide across. */
    @Override
    public void draw(final PatternEncoderBlockEntity encoder, final KitInstance instance, final PoseStack poses,
                     final MultiBufferSource buffers, final int light, final int overlay, final float partialTick) {
        BayMedium.draw(encoder.drawnMedium(), encoder.era() == HardwareEra.ADVANCED, instance, poses, buffers, light,
                overlay, encoder.getLevel());
    }

    /* The activity lamp: blinking while a pattern is written, held lit on an error, dark otherwise. */
    private static boolean activityLit(final PatternEncoderBlockEntity encoder) {
        if (encoder.busy()) {
            return DeviceLamps.blinkLit(encoder.getLevel());
        }
        return encoder.phase() == PatternEncoderBlockEntity.Phase.ERROR;
    }
}
