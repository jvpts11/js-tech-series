/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.space.rocket.RocketPartModels;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Draws a rocket's formed parts, one model a part, each standing on the one under it, the pose stack standing on the
 * middle of the rocket's foot: for whatever holds a rocket to be drawn, the hangar's terminal while it is built, the
 * moving structure that carries it. Each model is an instance of its own, kept with the block entity that draws it.
 */
public final class RocketPartsRenderer {

    private RocketPartsRenderer() {
    }

    /** Draws {@code placed}, the models of a rocket's parts, for {@code owner}. */
    public static void draw(final BlockEntity owner, final List<RocketPartModels.Placed> placed,
                            final PoseStack poses, final MultiBufferSource buffers, final int light,
                            final int overlay) {
        for (final RocketPartModels.Placed part : placed) {
            final Optional<KitModel> model = KitModels.get(part.model());
            if (model.isEmpty()) {
                continue;
            }
            poses.pushPose();
            poses.translate(0, part.y(), 0);
            KitRenderer.draw(KitAnimations.part(owner, part.section() + "@" + part.y(), model.get()),
                    ModelDefinition.Variant.NONE, owner, poses, buffers, light, overlay, null, owner.getLevel(),
                    true);
            poses.popPose();
        }
    }
}
