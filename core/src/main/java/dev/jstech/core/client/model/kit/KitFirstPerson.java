/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3fc;

/**
 * The player's own arms, drawn where an item's model of the Core's holds them in first person: a bone named
 * {@code player_right_arm} or {@code player_left_arm} has that arm of the player drawn at it, in the player's skin and
 * its sleeve, wide or slim as the skin is, so a tool or a weapon is held by hands that move with its animation.
 */
public final class KitFirstPerson {

    /** The bones the player's arms are drawn at. */
    public static final String RIGHT_ARM = "player_right_arm";
    public static final String LEFT_ARM = "player_left_arm";
    private static final float PIXEL = 1.0F / 16.0F;

    private KitFirstPerson() {
    }

    /**
     * Draws the player's arms at {@code instance}'s arm bones, the pose stack standing where its model's origin is, in
     * blocks.
     */
    public static void arms(final KitInstance instance, final PoseStack poses, final MultiBufferSource buffers,
                            final int light) {
        final int right = instance.pose().index(RIGHT_ARM);
        final int left = instance.pose().index(LEFT_ARM);
        final AbstractClientPlayer player = Minecraft.getInstance().player;
        if (right < 0 && left < 0 || player == null) {
            return;
        }
        final EntityRenderer<? super AbstractClientPlayer> renderer = Minecraft.getInstance()
                .getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer players)) {
            return;
        }
        final PlayerModel<AbstractClientPlayer> model = players.getModel();
        final ResourceLocation skin = player.getSkin().texture();
        if (right >= 0) {
            arm(instance, right, model.rightArm, model.rightSleeve, skin, poses, buffers, light);
        }
        if (left >= 0) {
            arm(instance, left, model.leftArm, model.leftSleeve, skin, poses, buffers, light);
        }
    }

    private static void arm(final KitInstance instance, final int bone, final ModelPart arm, final ModelPart sleeve,
                            final ResourceLocation skin, final PoseStack poses, final MultiBufferSource buffers,
                            final int light) {
        if (instance.pose().hidden(bone)) {
            return;
        }
        final PartPose armWas = arm.storePose();
        final PartPose sleeveWas = sleeve.storePose();
        // The arm hangs from the bone itself, not from where the game's own model holds it.
        arm.loadPose(PartPose.ZERO);
        sleeve.loadPose(PartPose.ZERO);
        final Vector3fc pivot = instance.model().data().bones().get(bone).pivot();
        poses.pushPose();
        poses.scale(PIXEL, PIXEL, PIXEL);
        // A bone's place turns the model's own space; the arm hangs from the bone's pivot within it.
        poses.mulPose(instance.world()[bone]);
        poses.translate(pivot.x(), pivot.y(), pivot.z());
        poses.scale(1 / PIXEL, 1 / PIXEL, 1 / PIXEL);
        // The game's limbs count Y down and X to the wearer's left; the Core's models count Y up.
        poses.scale(-1, -1, 1);
        arm.render(poses, buffers.getBuffer(RenderType.entitySolid(skin)), light, OverlayTexture.NO_OVERLAY);
        sleeve.render(poses, buffers.getBuffer(RenderType.entityTranslucent(skin)), light, OverlayTexture.NO_OVERLAY);
        poses.popPose();
        arm.loadPose(armWas);
        sleeve.loadPose(sleeveWas);
    }
}
