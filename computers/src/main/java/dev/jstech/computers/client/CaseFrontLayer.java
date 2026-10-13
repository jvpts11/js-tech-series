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
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.CaseFront;
import dev.jstech.computers.os.media.FormattedMediaItem;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Draws what a small computer's front holds as the very items the player put in: the disc on the tray of the drive in
 * a bay, at the anchor the drive's model marks on its tray (so it rides out and in with it), and the USB flash drive in
 * a port, at the port's anchor. A disc lies flat at half its size; a stick is its own model at half size, turned so its
 * plug goes into the port, as an external drive draws them.
 *
 * @param <T> which of the small computers
 */
public final class CaseFrontLayer<T extends AbstractSmallComputerBlockEntity> extends BlockAndItemGeoLayer<T> {

    /** Where the stick's plug meets its body, along the model's length, from the model's middle. */
    private static final float USB_JOINT = 3F / 16F;
    /** How far the stick's middle stands above the middle of its model. */
    private static final float USB_RISE = 0.5F / 16F;
    private static final String DISC = "disc_";
    private static final String USB = "usb_";
    /* What each of the model's bones is an anchor for, worked out once a name. */
    private static final Map<String, Anchor> ANCHORS = new ConcurrentHashMap<>();

    public CaseFrontLayer(final GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    @Nullable
    protected ItemStack getStackForBone(final GeoBone bone, final T computer) {
        final Anchor anchor = ANCHORS.computeIfAbsent(bone.getName(), CaseFrontLayer::anchorOf);
        if (anchor == Anchor.NONE) {
            return null;
        }
        final CaseFront front = computer.front();
        final ItemStack drawn;
        if (anchor.port()) {
            drawn = front.stick(anchor.index());
        } else {
            // Only the drive the bay shows carries the disc: the others are hidden, their anchors with them.
            drawn = anchor.kind().equals(front.shown(anchor.index())) ? front.held(anchor.index()) : ItemStack.EMPTY;
        }
        return drawn.getItem() instanceof FormattedMediaItem ? drawn : null;
    }

    @Override
    protected void renderStackForBone(final PoseStack poseStack, final GeoBone bone, final ItemStack stack,
                                      final T computer, final MultiBufferSource bufferSource, final float partialTick,
                                      final int packedLight, final int packedOverlay) {
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        if (bone.getName().startsWith(USB)) {
            /*
             * The model's plug runs along its +x, wide in y and thin in z, its light strip on its -z face. Turned so the
             * plug goes into the case (+z), lying as the slot does, wide across and thin up and down, the strip up.
             */
            poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.translate(-USB_JOINT, -USB_RISE, 0);
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        }
        super.renderStackForBone(poseStack, bone, stack, computer, bufferSource, partialTick, packedLight,
                packedOverlay);
        poseStack.popPose();
    }

    /* A bone's name read as an anchor: disc_<bay>_<kind> on a drive's tray, usb_<port> in a port, or neither. */
    private static Anchor anchorOf(final String name) {
        try {
            if (name.startsWith(USB)) {
                return new Anchor(true, Integer.parseInt(name.substring(USB.length())), "");
            }
            if (name.startsWith(DISC)) {
                final String rest = name.substring(DISC.length());
                final int cut = rest.indexOf('_');
                return cut < 0 ? Anchor.NONE
                        : new Anchor(false, Integer.parseInt(rest.substring(0, cut)), rest.substring(cut + 1));
            }
        } catch (final NumberFormatException notAnAnchor) {
            return Anchor.NONE;
        }
        return Anchor.NONE;
    }

    /**
     * What a bone anchors.
     *
     * @param port  whether it is a port's, else a drive's tray
     * @param index which port or which bay
     * @param kind  the drive it belongs to, as the model names it; empty for a port
     */
    private record Anchor(boolean port, int index, String kind) {

        static final Anchor NONE = new Anchor(false, -1, "");
    }
}
