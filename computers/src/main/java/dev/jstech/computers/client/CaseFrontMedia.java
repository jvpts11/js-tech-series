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
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.ModelBone;
import dev.jstech.core.model.ModelData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Draws what a small computer's front holds as the very items the player put in: the disc on the tray of the drive in
 * a bay, at the anchor the drive's model marks on its tray (so it rides out and in with it), and the USB flash drive in
 * a port, at the port's anchor. A disc lies flat at half its size; a stick is its own model at half size, turned so its
 * plug goes into the port, as an external drive draws them.
 */
final class CaseFrontMedia {

    private static final String DISC = "disc_";
    private static final String USB = "usb_";
    private static final float HALF = 0.5F;
    /* The anchors of each case's model, worked out once from its bones' names. */
    private static final ModelFacts<List<Anchor>> ANCHORS = new ModelFacts<>(CaseFrontMedia::anchorsOf);

    private CaseFrontMedia() {
    }

    /** Draws what {@code computer}'s front holds, the pose stack standing where its case's model stands. */
    static void draw(final AbstractSmallComputerBlockEntity computer, final KitInstance instance,
                     final PoseStack poses, final MultiBufferSource buffers, final int light, final int overlay) {
        final CaseFront front = computer.front();
        for (final Anchor anchor : ANCHORS.of(instance.model().data())) {
            final ItemStack drawn;
            if (anchor.port()) {
                drawn = front.stick(anchor.index());
            } else {
                // Only the drive the bay shows carries the disc: the others are hidden, their anchors with them.
                drawn = anchor.kind().equals(front.shown(anchor.index())) ? front.held(anchor.index())
                        : ItemStack.EMPTY;
            }
            if (!(drawn.getItem() instanceof FormattedMediaItem)) {
                continue;
            }
            poses.pushPose();
            if (KitRenderer.toBone(instance, anchor.bone(), poses)) {
                poses.scale(HALF, HALF, HALF);
                if (anchor.port()) {
                    /*
                     * The model's plug runs along its +x, wide in y and thin in z, its light strip on its -z face.
                     * Turned so the plug goes into the case (+z), lying as the slot does, wide across and thin up and
                     * down, the strip up.
                     */
                    poses.mulPose(Axis.YP.rotationDegrees(-90));
                    poses.mulPose(Axis.XP.rotationDegrees(90));
                    poses.translate(-BayMedium.USB_JOINT, -BayMedium.USB_RISE, 0);
                } else {
                    poses.mulPose(Axis.YP.rotationDegrees(180));
                    poses.mulPose(Axis.XP.rotationDegrees(-90));
                }
                Minecraft.getInstance().getItemRenderer().renderStatic(drawn, ItemDisplayContext.NONE, light,
                        overlay, poses, buffers, computer.getLevel(), 0);
            }
            poses.popPose();
        }
    }

    /* Every bone of a case's model that is an anchor: disc_<bay>_<kind> on a drive's tray, usb_<port> in a port. */
    private static List<Anchor> anchorsOf(final ModelData model) {
        final List<Anchor> anchors = new ArrayList<>();
        for (final ModelBone bone : model.bones()) {
            final Anchor anchor = anchorOf(bone.name());
            if (anchor != null) {
                anchors.add(anchor);
            }
        }
        return List.copyOf(anchors);
    }

    /* A bone's name read as an anchor, or none. */
    private static @Nullable Anchor anchorOf(final String name) {
        try {
            if (name.startsWith(USB)) {
                return new Anchor(name, true, Integer.parseInt(name.substring(USB.length())), "");
            }
            if (name.startsWith(DISC)) {
                final String rest = name.substring(DISC.length());
                final int cut = rest.indexOf('_');
                return cut < 0 ? null
                        : new Anchor(name, false, Integer.parseInt(rest.substring(0, cut)), rest.substring(cut + 1));
            }
        } catch (final NumberFormatException notAnAnchor) {
            return null;
        }
        return null;
    }

    /**
     * What a bone anchors.
     *
     * @param bone  the bone's name
     * @param port  whether it is a port's, else a drive's tray
     * @param index which port or which bay
     * @param kind  the drive it belongs to, as the model names it; empty for a port
     */
    private record Anchor(String bone, boolean port, int index, String kind) {
    }
}
