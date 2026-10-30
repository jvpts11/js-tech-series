/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.space.ship.ShipPartModels;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Draws a starship's formed parts, one model a piece of a part's kit, each where its anchor stands along the spine,
 * the pose stack standing on the ship's nose, on its centre line at the hull's floor, the ship lying toward the south:
 * for whatever holds a ship to be drawn, the shipyard while it is built, the moving structure that carries it. Each
 * model is an instance of its own, kept with the block entity that draws it.
 */
public final class ShipPartsRenderer {

    private ShipPartsRenderer() {
    }

    /** Draws {@code placed}, the models of a ship's parts, for {@code owner}. */
    public static void draw(final BlockEntity owner, final List<ShipPartModels.Placed> placed, final PoseStack poses,
                            final MultiBufferSource buffers, final int light, final int overlay) {
        for (final ShipPartModels.Placed part : placed) {
            final Optional<KitModel> model = KitModels.get(part.model());
            if (model.isEmpty()) {
                continue;
            }
            final KitInstance instance = KitAnimations.part(owner, part.model().getPath() + "@" + part.z(),
                    model.get());
            poses.pushPose();
            poses.translate(0, 0, part.z());
            KitRenderer.draw(instance, ModelDefinition.Variant.NONE, owner, poses, buffers, light, overlay, null,
                    owner.getLevel(), true);
            poses.popPose();
        }
    }
}
