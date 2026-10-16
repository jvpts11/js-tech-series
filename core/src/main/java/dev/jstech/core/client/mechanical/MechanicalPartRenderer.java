/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mechanical;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitClock;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.mechanical.BeltPath;
import dev.jstech.core.mechanical.MechanicalPartBlock;
import dev.jstech.core.mechanical.MechanicalPartBlockEntity;
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelPose;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws a part of mechanical power turning at its real speed, as its network turns it: the angle follows the speed
 * the server sent, frame by frame, so a part speeding up or slowing down does so smoothly; two meshed gears stand half
 * a tooth apart, so their teeth meet; and the belt or chain from a pulley to the pulley it joins runs as fast as the
 * pulley's rim. Every part drawn with one model shares that model's shape.
 *
 * @param <T> the parts' block entity
 */
final class MechanicalPartRenderer<T extends MechanicalPartBlockEntity> implements BlockEntityRenderer<T> {

    /* Each part's angle, in degrees, and when it was last worked out, by the model's clock. */
    private final Map<MechanicalPartBlockEntity, Spin> spins = new WeakHashMap<>();
    private final Map<KitInstance, Boolean> moved = new WeakHashMap<>();

    /* Degrees a second at one revolution a minute. */
    private static final double DEGREES_A_SECOND = 6.0;
    private static final double FULL_TURN = 360.0;
    /* A belt's run is drawn as pieces of this length, in blocks, each moving along it as the belt runs. */
    private static final double PIECE = 0.25;
    /* How wide a belt is, along the pulleys' axle, and how thick, out from the rims, in blocks. */
    private static final float BELT_WIDTH = 0.3F;
    private static final float BELT_THICKNESS = 0.04F;
    /* The belt's picture is drawn as it is, untinted. */
    private static final int FULL = 255;
    /* Past this many frames' time a part's angle is not carried on but begun again, as after the game was away. */
    private static final double LONGEST_STEP = 1.0;

    MechanicalPartRenderer() {
    }

    @Override
    public void render(final T part, final float partialTick, final PoseStack poses, final MultiBufferSource buffers,
                       final int light, final int overlay) {
        final BlockState state = part.getBlockState();
        final double seconds = KitClock.seconds(partialTick);
        final double angle = turn(part, seconds);
        final Direction.Axis axis = state.hasProperty(RotatedPillarBlock.AXIS)
                ? state.getValue(RotatedPillarBlock.AXIS) : Direction.Axis.Y;
        final Optional<TurningParts.Look> look = TurningParts.of(state.getBlock());
        if (look.isPresent()) {
            final Optional<KitModel> model = KitModels.get(look.get().model());
            if (model.isPresent()) {
                drawPart(part, model.get(), look.get(), axis, angle, seconds, poses, buffers, light, overlay);
            }
        }
        final BlockPos other = part.linked();
        if (other != null && part.getBlockPos().compareTo(other) < 0 && part.getLevel() != null
                && part.getLevel().getBlockEntity(other) instanceof MechanicalPartBlockEntity pulley) {
            drawBelt(part, pulley, axis, seconds, poses, buffers, light);
        }
    }

    @Override
    public AABB getRenderBoundingBox(final T part) {
        final BlockPos pos = part.getBlockPos();
        final BlockPos other = part.linked();
        final AABB own = new AABB(pos).inflate(1);
        return other == null ? own : own.minmax(new AABB(other).inflate(1));
    }

    /* Its angle now: carried on from its last by the speed it turns at, and half a tooth on for every other gear. */
    private double turn(final T part, final double seconds) {
        final Spin spin = this.spins.computeIfAbsent(part, key -> new Spin());
        final double step = Double.isNaN(spin.last) ? 0 : seconds - spin.last;
        if (step > 0 && step < LONGEST_STEP) {
            spin.angle = (spin.angle + part.rpm() * DEGREES_A_SECOND * step) % FULL_TURN;
        }
        spin.last = seconds;
        final int teeth = part.getBlockState().getBlock() instanceof MechanicalPartBlock block ? block.spec().teeth()
                : 0;
        final BlockPos pos = part.getBlockPos();
        final boolean odd = ((pos.getX() + pos.getY() + pos.getZ()) & 1) == 1;
        spin.drawn = teeth > 0 && odd ? spin.angle + FULL_TURN / teeth / 2 : spin.angle;
        return spin.drawn;
    }

    private void drawPart(final T part, final KitModel model, final TurningParts.Look look,
                          final Direction.Axis axis, final double angle, final double seconds, final PoseStack poses,
                          final MultiBufferSource buffers, final int light, final int overlay) {
        final KitInstance instance = KitAnimations.of(part, model);
        final boolean bones = !look.bones().isEmpty();
        if (bones && this.moved.putIfAbsent(instance, Boolean.TRUE) == null) {
            final Spin spin = this.spins.get(part);
            instance.animator().mover((pose, now) -> turnBones(pose, look, spin));
        }
        instance.animate(KitClock.frame(), seconds, bones, IAnimationListener.NONE);
        poses.pushPose();
        poses.translate(0.5, 0.5, 0.5);
        lay(poses, axis);
        if (!bones) {
            poses.mulPose(Axis.YP.rotationDegrees((float) angle));
        }
        poses.translate(0, -0.5, 0);
        KitRenderer.draw(instance, ModelDefinition.Variant.NONE, part, poses, buffers, light, overlay, null,
                part.getLevel());
        poses.popPose();
    }

    /* The bones named turning about their own pivots, as the part does. */
    private static void turnBones(final ModelPose pose, final TurningParts.Look look, final Spin spin) {
        for (final String bone : look.bones()) {
            final int at = pose.index(bone);
            if (at >= 0) {
                pose.addEuler(at, 0, (float) spin.drawn, 0);
            }
        }
    }

    /* A part made standing up laid along its axis: its Y onto the world's X or Z. */
    private static void lay(final PoseStack poses, final Direction.Axis axis) {
        switch (axis) {
            case X -> poses.mulPose(Axis.ZN.rotationDegrees(90));
            case Z -> poses.mulPose(Axis.XP.rotationDegrees(90));
            default -> {
                // Standing up, as it is made.
            }
        }
    }

    /*
     * The belt from this pulley to the other, in the plane they turn in: its two runs, each drawn as pieces that move
     * along it as fast as the pulley's rim, one run each way.
     */
    private static void drawBelt(final MechanicalPartBlockEntity from, final MechanicalPartBlockEntity to,
                                 final Direction.Axis axis, final double seconds, final PoseStack poses,
                                 final MultiBufferSource buffers, final int light) {
        final double radiusFrom = from.spec().radius();
        final double radiusTo = to.spec().radius();
        final BlockPos offset = to.getBlockPos().subtract(from.getBlockPos());
        final double[] there = across(axis, offset.getX(), offset.getY(), offset.getZ());
        final Optional<BeltPath> path = BeltPath.open(0, 0, radiusFrom, there[0], there[1], radiusTo);
        if (path.isEmpty()) {
            return;
        }
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(from.chained() ? TurningParts.CHAIN : TurningParts.BELT);
        final VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        final double travel = BeltPath.travel(from.rpm(), radiusFrom, seconds);
        final BeltPath run = path.get();
        poses.pushPose();
        poses.translate(0.5, 0.5, 0.5);
        run(consumer, poses.last(), axis, run.fromAx(), run.fromAy(), run.toAx(), run.toAy(), travel, sprite, light);
        run(consumer, poses.last(), axis, run.toBx(), run.toBy(), run.fromBx(), run.fromBy(), travel, sprite, light);
        poses.popPose();
    }

    /* One run, from one point of the plane to another, its pieces moved on by {@code travel} blocks. */
    private static void run(final VertexConsumer consumer, final PoseStack.Pose pose, final Direction.Axis axis,
                            final double x1, final double y1, final double x2, final double y2, final double travel,
                            final TextureAtlasSprite sprite, final int light) {
        final double length = Math.hypot(x2 - x1, y2 - y1);
        if (length <= 0) {
            return;
        }
        final double ux = (x2 - x1) / length;
        final double uy = (y2 - y1) / length;
        final double phase = ((travel % PIECE) + PIECE) % PIECE;
        for (double start = phase - PIECE; start < length; start += PIECE) {
            final double a = Math.max(0, start);
            final double b = Math.min(length, start + PIECE);
            if (b <= a) {
                continue;
            }
            // A piece cut short at an end shows as much of its picture as is left of it.
            final float v0 = sprite.getV((float) ((a - start) / PIECE));
            final float v1 = sprite.getV((float) ((b - start) / PIECE));
            piece(consumer, pose, axis, x1 + ux * a, y1 + uy * a, x1 + ux * b, y1 + uy * b, sprite.getU0(),
                    sprite.getU1(), v0, v1, light);
        }
    }

    /*
     * A piece of belt between two points of the plane on the pulleys' rims: its inner face on the rims, its outer
     * face as far out as the belt is thick, and its two edges, as wide apart as the belt along the axle.
     */
    private static void piece(final VertexConsumer consumer, final PoseStack.Pose pose, final Direction.Axis axis,
                              final double ax, final double ay, final double bx, final double by, final float u0,
                              final float u1, final float v0, final float v1, final int light) {
        final double length = Math.hypot(bx - ax, by - ay);
        // Out from the pulleys: the left of a run, as the runs are drawn.
        final double nx = -(by - ay) / length;
        final double ny = (bx - ax) / length;
        final double ox = nx * BELT_THICKNESS;
        final double oy = ny * BELT_THICKNESS;
        final float half = BELT_WIDTH / 2;
        final float edge = u0 + (u1 - u0) * BELT_THICKNESS / BELT_WIDTH;
        final float[] out = world(axis, nx, ny, 0);
        final float[] in = world(axis, -nx, -ny, 0);
        final float[] side = world(axis, 0, 0, 1);
        final float[] other = world(axis, 0, 0, -1);
        quad(consumer, pose, out, light, new float[][] {
            world(axis, ax + ox, ay + oy, -half), world(axis, ax + ox, ay + oy, half),
            world(axis, bx + ox, by + oy, half), world(axis, bx + ox, by + oy, -half)}, u0, u1, v0, v1);
        quad(consumer, pose, in, light, new float[][] {
            world(axis, ax, ay, -half), world(axis, ax, ay, half), world(axis, bx, by, half),
            world(axis, bx, by, -half)}, u0, u1, v0, v1);
        quad(consumer, pose, side, light, new float[][] {
            world(axis, ax, ay, half), world(axis, ax + ox, ay + oy, half), world(axis, bx + ox, by + oy, half),
            world(axis, bx, by, half)}, u0, edge, v0, v1);
        quad(consumer, pose, other, light, new float[][] {
            world(axis, ax, ay, -half), world(axis, ax + ox, ay + oy, -half), world(axis, bx + ox, by + oy, -half),
            world(axis, bx, by, -half)}, u0, edge, v0, v1);
    }

    /* One face of a piece: four corners, the first two at the run's start, with the picture across and along. */
    private static void quad(final VertexConsumer consumer, final PoseStack.Pose pose, final float[] normal,
                             final int light, final float[][] corners, final float u0, final float u1, final float v0,
                             final float v1) {
        final float[][] uvs = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
        for (int k = 0; k < corners.length; k++) {
            consumer.addVertex(pose, corners[k][0], corners[k][1], corners[k][2]).setColor(FULL, FULL, FULL, FULL)
                    .setUv(uvs[k][0], uvs[k][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(pose, normal[0], normal[1], normal[2]);
        }
    }

    /* A point given across the plane of the pulleys and along their axle, in the world's axes. */
    private static float[] world(final Direction.Axis axis, final double across, final double up,
                                 final double along) {
        return switch (axis) {
            case X -> new float[] {(float) along, (float) up, (float) across};
            case Y -> new float[] {(float) across, (float) along, (float) up};
            case Z -> new float[] {(float) across, (float) up, (float) along};
        };
    }

    /* An offset in the world's axes given across the plane of the pulleys, the first two numbers. */
    private static double[] across(final Direction.Axis axis, final int x, final int y, final int z) {
        return switch (axis) {
            case X -> new double[] {z, y};
            case Y -> new double[] {x, z};
            case Z -> new double[] {x, y};
        };
    }

    /* A part's angle, the angle it is drawn at, half a tooth on for every other gear, and when they were worked out. */
    private static final class Spin {

        private double angle;
        private double drawn;
        private double last = Double.NaN;
    }
}
