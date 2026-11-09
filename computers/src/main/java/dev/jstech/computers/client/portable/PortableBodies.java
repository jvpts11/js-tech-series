/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.portable.PortableModel;
import dev.jstech.core.client.live.ILivePainter;
import dev.jstech.core.client.live.LiveScreen;
import dev.jstech.core.client.live.LiveScreens;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.gui.Tube;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.model.ModelDefinition;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A portable's body as the world draws it, in a hand and standing in a dock: its model, standing on its bottom edge in
 * the middle of its block of room, its front to the north, and its screen, lit, laid on the glass of its front where
 * the K0.10 models have it. In a dock it stands on the dock's seat, the bottom of its back on the seat's line, leant
 * back as the dock holds it.
 */
public final class PortableBodies {

    /* A model's pixels to a block. */
    private static final float PIXELS = 16.0F;
    /* A screen's picture stands this far in front of the glass, in pixels, so the two never share a plane. */
    private static final float LIFT = 0.02F;
    /* Where every device's bottom edge stands, in the generator's units: a sixteenth of a block, two of them. */
    private static final float BOTTOM = 2.0F;
    /* The screen's texture has two pixels to each of the GUI's. */
    private static final int SCREEN_SCALE = 2;

    private PortableBodies() {
    }

    /**
     * Draws the device in {@code stack} with its origin at the pose's: its model, and over its glass the picture
     * {@code painter} paints, known as {@code key}, at its screen's size; no picture when {@code painter} is null.
     */
    public static void draw(final ItemStack stack, final PortableModel model, final ItemDisplayContext context,
                            final PoseStack poses, final MultiBufferSource buffers, final int light, final int overlay,
                            final Object key, @Nullable final ILivePainter painter) {
        final Optional<KitModel> found = KitModels.look(ComputingLooks.PORTABLE.modelFile(model.serializedName()),
                ComputingLooks.PORTABLE.textureFile(model.serializedName()),
                ComputingLooks.PORTABLE.animationFile());
        if (found.isEmpty()) {
            return;
        }
        KitRenderer.draw(KitAnimations.of(stack, context, found.get()), ModelDefinition.Variant.NONE, stack, poses,
                buffers, light, overlay, null, Minecraft.getInstance().level, false);
        if (painter == null) {
            return;
        }
        final Glass glass = Glass.of(model);
        final LiveScreen live = LiveScreens.ask(key, painter, model.screenWidth(), model.screenHeight(),
                SCREEN_SCALE, Tube.COLOUR, 0.0);
        poses.pushPose();
        // The picture's units run across the front as it is seen, from its top left (the model's east) down and right.
        poses.translate(0.0F, 0.0F, (glass.front() - LIFT) / PIXELS);
        poses.scale(-1.0F / PIXELS, -1.0F / PIXELS, 1.0F);
        LiveScreens.draw(poses, buffers, live, -glass.right(), -glass.top(), -glass.left(), -glass.bottom(),
                LightTexture.FULL_BRIGHT);
        poses.popPose();
    }

    /**
     * Moves the pose from the middle of a dock's floor of {@code era} onto its seat: the device's back on the seat's
     * line, leant back as the dock holds it.
     */
    public static void seat(final PoseStack poses, final Era era, final PortableModel model) {
        final Seat seat = Seat.of(era);
        final float pivotY = seat.y() / 2.0F / PIXELS;
        final float pivotZ = (8.0F - seat.z() / 2.0F) / PIXELS;
        poses.translate(0.0F, pivotY, pivotZ);
        poses.mulPose(Axis.XP.rotationDegrees(seat.lean()));
        poses.translate(0.0F, -pivotY, -pivotZ);
        poses.translate(0.0F, (seat.y() - BOTTOM) / 2.0F / PIXELS, -(seat.z() - Glass.of(model).back()) / 2.0F
                / PIXELS);
    }

    /*
     * Where a device's screen is on its model, in pixels from the middle of its floor (left and right across, bottom
     * and top up, the front of its glass toward the north), and where its back is, in the generator's units, which
     * its seat in a dock is measured from.
     */
    private record Glass(float left, float right, float bottom, float top, float front, float back) {

        static Glass of(final PortableModel model) {
            return switch (model) {
                case HIGHTIDE_POCKET -> new Glass(-2.75F, 2.75F, 5.0F, 12.25F, -0.75F, 14.5F);
                case HIGHTIDE_POCKET_PRO -> new Glass(-2.75F, 2.75F, 4.75F, 12.25F, -0.75F, 14.5F);
                case QPHONE -> new Glass(-2.375F, 2.375F, 3.5F, 10.625F, -0.5F, 15.0F);
                case MOTOROLLER_LANDMARK -> new Glass(-2.375F, 2.375F, 3.8F, 12.25F, -0.875F, 14.25F);
                case QPHONE_5 -> new Glass(-2.25F, 2.25F, 3.5F, 11.5F, -0.375F, 15.25F);
                case HIGHTIDE_WAVE -> new Glass(-2.875F, 2.875F, 3.35F, 13.6F, -0.5F, 15.0F);
                case QPAD_AIR -> new Glass(-3.75F, 3.75F, 2.75F, 12.75F, -0.25F, 15.5F);
                case SUNSUNG_NEBULA_TAB -> new Glass(-6.0F, 6.0F, 1.9F, 9.4F, -0.3F, 15.4F);
                case QPHONE_X -> new Glass(-2.625F, 2.625F, 1.375F, 12.875F, -0.375F, 15.25F);
                case SUNSUNG_NEBULA_S10 -> new Glass(-2.7F, 2.7F, 1.375F, 13.125F, -0.375F, 15.25F);
                case QPAD_PRO -> new Glass(-4.125F, 4.125F, 1.625F, 13.375F, -0.25F, 15.5F);
                case SUNSUNG_NEBULA_TAB_S7 -> new Glass(-6.5F, 6.5F, 1.5F, 9.75F, -0.25F, 15.5F);
            };
        }
    }

    /* A dock's seat, in the generator's units (a thirty-second of a block), and how far back it leans the device. */
    private record Seat(float y, float z, float lean) {

        static Seat of(final Era era) {
            if (era.isAtMost(HardwareEra.LEGACY)) {
                return new Seat(3.0F, 15.2F, 10.0F);
            }
            if (era.isAtMost(HardwareEra.TRANSITION)) {
                return new Seat(3.5F, 15.5F, 6.0F);
            }
            return era.isAtMost(HardwareEra.STANDARD) ? new Seat(1.5F, 15.0F, 15.0F) : new Seat(2.0F, 15.6F, 20.0F);
        }
    }
}
