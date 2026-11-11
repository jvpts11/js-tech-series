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
import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.blockentity.ScannerBlockEntity;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.model.AnimationController;
import dev.jstech.core.model.ModelAnimation;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws a scanner as its era's flatbed. At rest its lid lies closed; while it reads, the lid swings open on its hinges
 * and stays up, the light bar runs under the glass, and the busy lamp blinks; when it stops, the lid closes. The thing
 * on the glass is the real item lying there, drawn flat, where the model's stand-in would be.
 */
public final class ScannerRenderer implements IKitDriver<ScannerBlockEntity> {

    /** The lid's opening and closing, held on their last frame, and the bar's run, round and round. */
    private static final String LID = "scanner_lid";
    private static final String BAR = "scanner_bar";
    private static final String CLIP = "animation.scanner.";
    /** How large the item on the glass is drawn, about the glass's size. */
    private static final float ITEM_SCALE = 0.4F;
    /* A model unit is a thirty-second of a block; the item lies half a unit over the glass. */
    private static final float UNIT = 1.0F / 32.0F;
    private static final float OVER_GLASS = 0.5F;

    private ScannerRenderer() {
    }

    /** The renderer of the scanners. */
    public static BlockEntityRenderer<ScannerBlockEntity> create(final BlockEntityRendererProvider.Context context) {
        return KitBlockEntityRenderer.<ScannerBlockEntity>look(ComputingLooks.SCANNER)
                .driver(new ScannerRenderer())
                .extras(ScannerRenderer::onGlass)
                .build();
    }

    @Override
    public void drive(final ScannerBlockEntity scanner, final ModelAnimator animator, final double seconds) {
        final String era = HardwareEra.withParts(scanner.model().era()).path();
        final AnimationController lid = animator.controller(LID);
        final ModelAnimation playing = lid.playing();
        if (scanner.scanning()) {
            DeviceClips.play(animator, LID, CLIP + "open_" + era, seconds);
        } else if (playing != null && playing.name().equals(CLIP + "open_" + era)) {
            // Only a lid that opened closes: one at rest when its chunk comes into sight stays still.
            DeviceClips.play(animator, LID, CLIP + "close_" + era, seconds);
        }
        DeviceClips.play(animator, BAR, scanner.scanning() ? CLIP + "bar_" + era : null, seconds);
    }

    @Override
    public void move(final ScannerBlockEntity scanner, final ModelPose pose, final double seconds) {
        DeviceLamps.show(pose, ComputingLooks.SCANNER_BAR_LAMP, scanner.scanning());
        DeviceLamps.show(pose, ComputingLooks.SCANNER_BUSY_LAMP, scanner.scanning()
                ? DeviceLamps.blinkLit(scanner.getLevel()) : scanner.ownerPos() != null);
        DeviceLamps.show(pose, ComputingLooks.SCANNER_ITEM, false);
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    /* The item on the glass, lying flat on it, face down as a page to be read lies. */
    private static void onGlass(final ScannerBlockEntity scanner, final KitInstance instance, final PoseStack poses,
                                final MultiBufferSource buffers, final int light, final int overlay,
                                final float partialTick) {
        final ItemStack item = scanner.onGlass();
        if (item.isEmpty()) {
            return;
        }
        poses.pushPose();
        poses.translate(0.0F, (glass(scanner.model().era()) + OVER_GLASS) * UNIT, 0.0F);
        poses.mulPose(Axis.XP.rotationDegrees(90));
        poses.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.FIXED, light, overlay, poses,
                buffers, scanner.getLevel(), 0);
        poses.popPose();
    }

    /* How high the glass lies in each era's body, in model units, as its model is drawn. */
    private static float glass(final Era era) {
        final Era parts = HardwareEra.withParts(era);
        if (parts == HardwareEra.VINTAGE) {
            return 9;
        }
        if (parts == HardwareEra.LEGACY) {
            return 6;
        }
        if (parts == HardwareEra.TRANSITION) {
            return 5;
        }
        return parts == HardwareEra.STANDARD ? 7 : 4;
    }
}
