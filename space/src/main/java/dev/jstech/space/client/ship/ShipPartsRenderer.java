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
import dev.jstech.core.client.model.kit.KitClock;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelPose;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.client.rocket.RocketCabins;
import dev.jstech.space.rocket.CabinReadings;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipPartModels;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.IntPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws a starship's formed parts, one model a piece of a part's kit, each where its anchor stands along the spine,
 * the pose stack standing on the ship's nose, on its centre line at the hull's floor, the ship lying toward the south:
 * for whatever holds a ship to be drawn, the shipyard while it is built, the moving structure that carries it. Each
 * model is an instance of its own, kept with the block entity that draws it. A cockpit's live cabin is drawn over its
 * model as the readings say ({@link ShipCabinRenderer}), its stick, throttle and globe moved by them.
 */
public final class ShipPartsRenderer {

    /* The navigator's glazed nose is Kosmozavod's, whose dials are black-faced. */
    private static final String BLACK_DIALS = "greenhouse";
    private static final double HALF = 0.5;
    /* The bones each model's instance hides, for the holes in its part. */
    private static final Map<KitInstance, Set<String>> HIDDEN = new WeakHashMap<>();

    private ShipPartsRenderer() {
    }

    /** Draws {@code placed}, the models of a ship's parts, for {@code owner}, its cockpit showing {@code readings}. */
    public static void draw(final BlockEntity owner, final List<ShipPartModels.Placed> placed,
                            final CabinReadings readings, final PoseStack poses, final MultiBufferSource buffers,
                            final int light, final int overlay) {
        draw(owner, placed, Map.of(), readings, poses, buffers, light, overlay);
    }

    /**
     * Draws {@code placed} as {@link #draw(BlockEntity, List, CabinReadings, PoseStack, MultiBufferSource, int, int)}
     * does, a block missing from a formed part shown as a hole, its bone hidden: {@code holes} the bones hidden, by
     * the part's number, then its piece's model's name ({@code ShipyardPlans.holes}).
     */
    public static void draw(final BlockEntity owner, final List<ShipPartModels.Placed> placed,
                            final Map<Integer, Map<String, Set<String>>> holes, final CabinReadings readings,
                            final PoseStack poses, final MultiBufferSource buffers, final int light,
                            final int overlay) {
        final Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        for (final ShipPartModels.Placed part : placed) {
            final Optional<KitModel> model = KitModels.get(part.model());
            if (model.isEmpty()) {
                continue;
            }
            final String name = part.model().getPath() + "@" + part.z();
            final KitInstance instance = KitAnimations.part(owner, name, model.get());
            final Optional<RocketCabins.Cabin> cabin = RocketCabins.of(cabinOf(part.model()));
            cabin.ifPresent(live -> instance.animate(KitClock.frame(), KitClock.now(),
                    ShipCabinRenderer.driver(live), readings, IAnimationListener.NONE));
            hide(instance, holes.getOrDefault(part.part(), Map.of()).getOrDefault(ShipPartModels.name(part.model()),
                    Set.of()));
            poses.pushPose();
            poses.translate(0, 0, part.z());
            if (part.flipped()) {
                // Flipped left for right about the ship's centre line; its faces are drawn from both sides.
                poses.scale(-1, 1, 1);
            }
            KitRenderer.draw(instance, ModelDefinition.Variant.NONE, owner, poses, buffers, light, overlay, null,
                    owner.getLevel(), true);
            if (cabin.isPresent()) {
                final double distance = eye.distanceToSqr(Vec3.atCenterOf(owner.getBlockPos()).add(0, 0, part.z()));
                ShipCabinRenderer.draw(owner, name, instance, cabin.get(), readings,
                        part.model().getPath().contains(BLACK_DIALS), poses, buffers, light, distance);
            }
            poses.popPose();
        }
    }

    /**
     * Draws the parts of the ship of {@code design} that {@code drawn} says, by their numbers, for {@code owner}, the
     * pose stack on the owner's block: the block of the ship's nose on its centre line at its hull's floor
     * {@code nose} from it, the ship's frame turned as {@code turn} says about that block's middle.
     */
    public static void drawShip(final BlockEntity owner, final ShipDesign design, final IntPredicate drawn,
                                final Map<Integer, Map<String, Set<String>>> holes, final BlockPos nose,
                                final Orientation turn, final CabinReadings readings, final PoseStack poses,
                                final MultiBufferSource buffers, final int light, final int overlay) {
        final Level level = owner.getLevel();
        if (level == null) {
            return;
        }
        final List<ShipPartModels.Placed> placed = ShipPartModels.of(ShipParts.all(level), ShipKits.all(level),
                design, drawn);
        poses.pushPose();
        poses.translate(nose.getX() + HALF, nose.getY() + HALF, nose.getZ() + HALF);
        poses.mulPose(turned(turn));
        poses.translate(-HALF, -HALF, -HALF);
        draw(owner, placed, holes, readings, poses, buffers, light, overlay);
        poses.popPose();
    }

    /* Hides the bones {@code bones} of {@code instance}, and shows again those it hid before that are not. */
    private static void hide(final KitInstance instance, final Set<String> bones) {
        final Set<String> was = HIDDEN.getOrDefault(instance, Set.of());
        if (bones.isEmpty() && was.isEmpty()) {
            return;
        }
        final ModelPose pose = instance.pose();
        for (final String bone : was) {
            final int index = pose.index(bone);
            if (index >= 0 && !bones.contains(bone)) {
                pose.setHidden(index, false);
            }
        }
        for (final String bone : bones) {
            final int index = pose.index(bone);
            if (index >= 0) {
                pose.setHidden(index, true);
            }
        }
        HIDDEN.put(instance, Set.copyOf(bones));
    }

    /* The turn {@code orientation} stands a frame in, each world axis taking one of the frame's. */
    private static Matrix4f turned(final Orientation orientation) {
        final Matrix4f turn = new Matrix4f().zero();
        turn.set(orientation.xFrom(), 0, orientation.xSign());
        turn.set(orientation.yFrom(), 1, orientation.ySign());
        turn.set(orientation.zFrom(), 2, orientation.zSign());
        turn.m33(1);
        return turn;
    }

    /* A model's cabin is read from beside its geometry, the same in every finish. */
    private static ResourceLocation cabinOf(final ResourceLocation model) {
        final String path = model.getPath();
        return ResourceLocation.fromNamespaceAndPath(model.getNamespace(), "ship/"
                + path.substring(path.lastIndexOf('/') + 1));
    }
}
