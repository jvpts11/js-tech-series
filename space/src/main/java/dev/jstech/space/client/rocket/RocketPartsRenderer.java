/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.rocket;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitClock;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.space.rocket.CabinReadings;
import dev.jstech.space.rocket.RocketPartModels;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a rocket's formed parts, one model a part, each standing on the one under it, the pose stack standing on the
 * middle of the rocket's foot: for whatever holds a rocket to be drawn, the hangar's terminal while it is built, the
 * moving structure that carries it. Each model is an instance of its own, kept with the block entity that draws it. A
 * part with a live cabin, a capsule or the lander's cabin, has it drawn over its model as the readings say
 * ({@link RocketCabinRenderer}).
 */
public final class RocketPartsRenderer {

    /* A part turned a quarter turn is turned this many degrees, clockwise seen from above. */
    private static final float QUARTER = 90.0F;

    private RocketPartsRenderer() {
    }

    /** Draws {@code placed}, the models of a rocket's parts, for {@code owner}, its cabins showing {@code readings}. */
    public static void draw(final BlockEntity owner, final List<RocketPartModels.Placed> placed,
                            final CabinReadings readings, final PoseStack poses, final MultiBufferSource buffers,
                            final int light, final int overlay) {
        final Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        for (final RocketPartModels.Placed part : placed) {
            final Optional<KitModel> model = KitModels.get(part.model());
            if (model.isEmpty()) {
                continue;
            }
            final String name = part.section() + "@" + part.y();
            final KitInstance instance = KitAnimations.part(owner, name, model.get());
            final Optional<RocketCabins.Cabin> cabin = RocketCabins.of(part.model());
            cabin.ifPresent(live -> instance.animate(KitClock.frame(), KitClock.now(),
                    RocketCabinRenderer.driver(live), readings, IAnimationListener.NONE));
            poses.pushPose();
            poses.translate(0, part.y(), 0);
            poses.mulPose(Axis.YN.rotationDegrees(QUARTER * part.turns()));
            KitRenderer.draw(instance, ModelDefinition.Variant.NONE, owner, poses, buffers, light, overlay, null,
                    owner.getLevel(), true);
            if (cabin.isPresent()) {
                final double distance = eye.distanceToSqr(Vec3.atCenterOf(owner.getBlockPos()).add(0, part.y(), 0));
                RocketCabinRenderer.draw(owner, name, instance, cabin.get(), readings, poses, buffers, light,
                        distance);
            }
            poses.popPose();
        }
    }
}
