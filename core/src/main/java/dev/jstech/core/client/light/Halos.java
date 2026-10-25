/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.light;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.light.LampBlockEntity;
import dev.jstech.core.light.LightMath;
import dev.jstech.core.light.LightingSettings;
import dev.jstech.core.stepaside.StepAside;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The glow of bright things, drawn without a shader: a soft halo about each lamp, each block that gives light of a
 * colour or bright light, and each glowing part of a model that casts light, in its colour, as strong as it shines.
 * It is the glare the eye sees, drawn over the picture whole, never cut by the floor or a wall it stands near; it
 * fades only as the light itself is hidden, by as much of it as the blocks between hide, glass and what lets light
 * through hiding none. Light added to what lies behind it, the nearest few drawn; as strong as the player's setting.
 */
final class Halos {

    /* How much of each glow is seen, by where it stands, kept from frame to frame so it fades rather than jumps. */
    private static final Long2ObjectOpenHashMap<Seen> SEEN = new Long2ObjectOpenHashMap<>();
    /* The most halos a frame draws, the nearest; their size and strength. */
    private static final int MOST = 192;
    private static final float LEAST_SIZE = 0.6F;
    private static final float MORE_SIZE = 1.4F;
    private static final double STRENGTH = 0.45;
    /* The rings and segments a halo is drawn in, and how quickly its bell falls. */
    private static final int RINGS = 5;
    private static final int SEGMENTS = 16;
    private static final double SPREAD = 4.0;
    /*
     * Where a light is looked at from the eye, how much of it is seen: its middle and four points about it as far as a
     * small lamp reaches, across the view. How many glows are looked at again a frame at most, the rest kept as they
     * were seen; how long, in seconds, a glow takes to fade most of the way to what is seen now; and below what share
     * of it seen it is not drawn.
     */
    private static final double[][] LOOKED_AT = {{0, 0}, {0.3, 0}, {-0.3, 0}, {0, 0.3}, {0, -0.3}};
    private static final int LOOKS_A_FRAME = 32;
    private static final double FADE_SECONDS = 0.08;
    private static final double UNSEEN = 0.01;
    /* A shape thinner than this, in blocks, is flat: glow lichen, a layer, a pane. */
    private static final double FLAT = 0.1;
    private static final double NANOS_PER_SECOND = 1.0e9;
    /* The colour of the glow of light that has none: a lamp's warm white. */
    private static final int WARM_WHITE = 0xFFF2DC;
    private static final int CHANNEL = 255;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;

    private static long frame;
    private static long lastNanos;

    private Halos() {
    }

    /** Draws the halos of the frame into {@code buffers}, the pose stack standing at the camera. */
    static void draw(final ClientLevel level, final Camera camera, final PoseStack poses,
                     final MultiBufferSource buffers) {
        final double bloom = LightingSettings.bloom();
        // The glow of bright things goes with coloured light to Shimmer, which draws its own bloom.
        if (bloom <= 0 || StepAside.COLOURED_LIGHT.steps()) {
            SEEN.clear();
            return;
        }
        final Vec3 eye = camera.getPosition();
        final double reach = LightingSettings.reach();
        // A glowing block aboard a moving structure is out in the yard; it glows where the structure draws it.
        final float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        final List<Glow> glows = new ArrayList<>();
        for (final ColouredLights.Source source : ColouredLights.glowing()) {
            addBlock(glows, level, new BlockPos(source.x(), source.y(), source.z()), source.level(), source.colour(),
                    partialTick);
        }
        for (final LampBlockEntity lamp : LampBlockEntity.seen()) {
            if (lamp.getLevel() == level && lamp.lit() && lamp.shining() > 0) {
                glows.add(new Glow(ClientMovingStructures.drawnInWorld(Vec3.atCenterOf(lamp.getBlockPos()),
                        partialTick), lamp.shining(), lamp.kind().colour(), 1));
            }
        }
        for (final KitGlows.Glowing part : KitGlows.last()) {
            if (part.level() == level) {
                glows.add(new Glow(ClientMovingStructures.drawnInWorld(part.at(), partialTick), part.light(),
                        part.colour(), 1));
            }
        }
        // A structure set down glows where it landed until the land is drawn with its blocks, and glows itself.
        for (final IDynamicLightSource.Light lamp : ClientMovingStructures.landedLamps()) {
            if (ColouredLights.glows(lamp.level(), lamp.colour())) {
                glows.add(new Glow(lamp.at(), lamp.level(), lamp.colour(), 1));
            }
        }
        glows.removeIf(glow -> glow.at().distanceToSqr(eye) > reach * reach);
        glows.sort(Comparator.comparingDouble(glow -> glow.at().distanceToSqr(eye)));
        final Vector3f left = camera.getLeftVector();
        final Vector3f up = camera.getUpVector();
        final VertexConsumer consumer = buffers.getBuffer(LightRenderTypes.GLARE);
        final Matrix4f pose = poses.last().pose();
        final long now = System.nanoTime();
        final double fade = lastNanos == 0 ? 1 : Math.min(1, (now - lastNanos) / NANOS_PER_SECOND / FADE_SECONDS);
        lastNanos = now;
        frame++;
        final int drawn = Math.min(MOST, glows.size());
        // Each glow is looked at again once in as many frames as it takes to look at them all, a few a frame.
        final int every = Math.max(1, (drawn + LOOKS_A_FRAME - 1) / LOOKS_A_FRAME);
        for (int i = 0; i < drawn; i++) {
            final Glow glow = glows.get(i);
            final BlockPos own = BlockPos.containing(glow.at());
            final long key = own.asLong();
            Seen seen = SEEN.get(key);
            if (seen == null) {
                seen = new Seen();
                seen.share = seenOf(level, eye, glow.at(), own, left, up);
                seen.target = seen.share;
                SEEN.put(key, seen);
            } else if (Math.floorMod(frame + key, every) == 0) {
                seen.target = seenOf(level, eye, glow.at(), own, left, up);
            }
            seen.share += (seen.target - seen.share) * fade;
            seen.frame = frame;
            if (seen.share < UNSEEN) {
                continue;
            }
            final float share = (float) glow.level() / LightMath.BRIGHTEST;
            final float size = (LEAST_SIZE + MORE_SIZE * share) * glow.size();
            halo(consumer, pose, glow.at().subtract(eye), left, up, size,
                    glow.colour() == LightMath.WHITE ? WARM_WHITE : glow.colour(),
                    Math.min(1.0, STRENGTH * bloom * share * seen.share));
        }
        SEEN.values().removeIf(each -> each.frame != frame);
    }

    /*
     * The glow of the block at {@code pos}, giving light {@code light} of colour {@code colour}, as its drawing is: a
     * whole block glows about its middle, and so do fire and liquids, whose shapes are not what is drawn; a smaller
     * one, a lantern or a torch, about the middle of its shape and as big as its shape; a flat one, glow lichen or a
     * layer, glows in its own drawing alone, with no halo about it; and a light not drawn at all glows nowhere.
     */
    private static void addBlock(final List<Glow> into, final ClientLevel level, final BlockPos pos, final int light,
                                 final int colour, final float partialTick) {
        final BlockState state = level.getBlockState(pos);
        if (state.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        final VoxelShape shape = state.getBlock() instanceof BaseFireBlock || state.getBlock() instanceof LiquidBlock
                ? Shapes.block() : state.getShape(level, pos);
        if (shape.isEmpty() || Block.isShapeFullBlock(shape)) {
            into.add(new Glow(ClientMovingStructures.drawnInWorld(Vec3.atCenterOf(pos), partialTick), light, colour,
                    1));
            return;
        }
        final AABB box = shape.bounds();
        if (Math.min(box.getXsize(), Math.min(box.getYsize(), box.getZsize())) < FLAT) {
            return;
        }
        final Vec3 middle = Vec3.atLowerCornerOf(pos).add(box.getCenter());
        into.add(new Glow(ClientMovingStructures.drawnInWorld(middle, partialTick), light, colour,
                (float) Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize()))));
    }

    /*
     * How much of the light at {@code at}, in the block {@code own}, the eye at {@code eye} sees: the share of the
     * points about it no block between hides.
     */
    private static double seenOf(final ClientLevel level, final Vec3 eye, final Vec3 at, final BlockPos own,
                                 final Vector3f left, final Vector3f up) {
        int seen = 0;
        for (final double[] point : LOOKED_AT) {
            final Vec3 looked = at.add(left.x() * point[0] + up.x() * point[1], left.y() * point[0] + up.y() * point[1],
                    left.z() * point[0] + up.z() * point[1]);
            if (clear(level, eye, looked, own)) {
                seen++;
            }
        }
        return (double) seen / LOOKED_AT.length;
    }

    /*
     * Whether nothing hides {@code to} from {@code from}: no block between whose shape hides what is behind it, as a
     * wall does; glass, leaves and what draws nothing hide nothing, and the light's own block {@code own} neither.
     */
    private static boolean clear(final ClientLevel level, final Vec3 from, final Vec3 to, final BlockPos own) {
        return BlockGetter.traverseBlocks(from, to, level, (reads, at) -> {
            if (at.equals(own)) {
                return null;
            }
            final BlockState state = reads.getBlockState(at);
            if (!state.canOcclude() || state.getRenderShape() == RenderShape.INVISIBLE) {
                return null;
            }
            final VoxelShape shape = state.getOcclusionShape(reads, at);
            return !shape.isEmpty() && shape.clip(from, to, at) != null ? Boolean.FALSE : null;
        }, reads -> Boolean.TRUE);
    }

    /*
     * A disc facing the eye about {@code at}, in rings from its middle out, each ring as strong as a soft bell is
     * there, light scattering in the eye, and nothing at its rim.
     */
    private static void halo(final VertexConsumer consumer, final Matrix4f pose, final Vec3 at, final Vector3f left,
                             final Vector3f up, final float size, final int colour, final double strength) {
        final int red = colour >> RED_SHIFT & CHANNEL;
        final int green = colour >> GREEN_SHIFT & CHANNEL;
        final int blue = colour & CHANNEL;
        for (int ring = 0; ring < RINGS; ring++) {
            final float inner = (float) ring / RINGS;
            final float outer = (float) (ring + 1) / RINGS;
            final int innerAlpha = (int) Math.round(strength * bell(inner) * CHANNEL);
            final int outerAlpha = (int) Math.round(strength * bell(outer) * CHANNEL);
            for (int step = 0; step < SEGMENTS; step++) {
                final double a = 2 * Math.PI * step / SEGMENTS;
                final double b = 2 * Math.PI * (step + 1) / SEGMENTS;
                corner(consumer, pose, at, left, up, size * inner, a, red, green, blue, innerAlpha);
                corner(consumer, pose, at, left, up, size * outer, a, red, green, blue, outerAlpha);
                corner(consumer, pose, at, left, up, size * outer, b, red, green, blue, outerAlpha);
                corner(consumer, pose, at, left, up, size * inner, b, red, green, blue, innerAlpha);
            }
        }
    }

    /* How strong a glow is at {@code distance} from its middle, its rim at 1: a bell brought to nothing there. */
    private static double bell(final double distance) {
        return Math.exp(-distance * distance * SPREAD) * Math.max(0.0, 1.0 - distance);
    }

    private static void corner(final VertexConsumer consumer, final Matrix4f pose, final Vec3 at, final Vector3f left,
                               final Vector3f up, final float radius, final double angle, final int red,
                               final int green, final int blue, final int alpha) {
        final float across = (float) Math.cos(angle) * radius;
        final float upward = (float) Math.sin(angle) * radius;
        consumer.addVertex(pose, (float) at.x + left.x() * across + up.x() * upward,
                (float) at.y + left.y() * across + up.y() * upward,
                (float) at.z + left.z() * across + up.z() * upward).setColor(red, green, blue, alpha);
    }

    /* A thing that glows: where, as bright as, in what colour, and how big its halo is against a whole block's. */
    private record Glow(Vec3 at, int level, int colour, float size) {
    }

    /* How much of a glow is seen now, how much was last looked at, and the frame it was last drawn in. */
    private static final class Seen {

        private double share;
        private double target;
        private long frame;
    }
}
