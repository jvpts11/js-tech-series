/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.JsCore;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.LevelMovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.joint.JointEnd;
import dev.jstech.core.moving.joint.JointKind;
import dev.jstech.core.moving.joint.StructureJoint;
import dev.jstech.core.moving.joint.StructureJoints;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Draws the joints of moving structures that are seen between their ends: a rope, sagging as far as its slack lets it,
 * hemp coloured; a chain of the game's own links; a spring coiled from end to end; and a bar. Each end is where its
 * structure is drawn this frame, so a rope from a ship to the ground never lags its ship. A hinge, a slider, a ball
 * joint and a weld are inside the blocks they join, and draw nothing of their own.
 */
@PaletteHolder
public final class JointRenderer {

    /** The colours of the joints drawn: hemp rope, a steel spring, an iron bar. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "structure_joints", new Colours(
            0xFF << 24 | 0x9C7A4E, 0xFF << 24 | 0xA3ABB3, 0xFF << 24 | 0x6B7077));
    /** How far away a joint is still drawn, in blocks. */
    public static final double RANGE = 128.0;
    /* Half a rope's thickness, a chain's and a bar's, in blocks. */
    private static final double ROPE = 1.0 / 32.0;
    private static final double CHAIN = 3.0 / 32.0;
    private static final double BAR = 1.0 / 16.0;
    /* A spring's radius, how many coils it has a metre, and the pieces of each coil. */
    private static final double COIL = 0.15;
    private static final double COILS_A_METRE = 4;
    private static final int COIL_PIECES = 8;
    /* The plain texture a rope, a spring and a bar are drawn with, tinted; and the chain's own. */
    private static final ResourceLocation PLAIN = ResourceLocation.withDefaultNamespace("block/white_concrete");
    private static final ResourceLocation CHAIN_LINKS = ResourceLocation.withDefaultNamespace("block/chain");
    private static final int WHITE = 0xFF << 24 | 0xFFFFFF;
    /* How many pieces a metre of rope is drawn in, and the fewest. */
    private static final double PIECES_A_METRE = 3;
    private static final int FEWEST = 6;
    /* A parabola's sag from its slack: a rope of length L between points d apart sags about sqrt(3 d (L - d) / 8). */
    private static final double SAG = 3.0 / 8.0;
    /* A chain's link texture: its two planes' strips, in sixteenths of the texture. */
    private static final float LINK_WIDE = 3.0F / 16.0F;

    private JointRenderer() {
    }

    /** Hands the game's bus the drawing, once, from the Core's client. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, JointRenderer::onRender);
    }

    /**
     * Where the end {@code end} of a joint is drawn this frame, {@code partialTick} through the tick: on its structure
     * as that is drawn, or where it is on the world; null for an end on a structure the player's game has not.
     */
    public static @Nullable Vec3 drawnEnd(final ClientLevel level, final JointEnd end, final float partialTick) {
        final Vec3 place = end.place();
        if (end.onWorld()) {
            return place;
        }
        final IStructureBody body = LevelMovingStructures.of(level).byId(end.structure().get());
        if (!(body instanceof ClientStructure structure) || !structure.ready()) {
            return null;
        }
        final StructureFrame frame = new StructureFrame(structure.drawnAt(partialTick), structure.pivotX(),
                structure.pivotY(), structure.pivotZ());
        return new Vec3(frame.worldX(place.x, place.y, place.z), frame.worldY(place.x, place.y, place.z),
                frame.worldZ(place.x, place.y, place.z));
    }

    /**
     * The points a rope of {@code length} between {@code a} and {@code b} hangs through, one more than its pieces:
     * straight when taut, sagging below the line between its ends as far as its slack lets it.
     */
    public static List<Vec3> ropePoints(final Vec3 a, final Vec3 b, final double length) {
        final double apart = a.distanceTo(b);
        final double sag = length > apart ? Math.sqrt(SAG * apart * (length - apart)) : 0;
        final int pieces = Math.max(FEWEST, (int) Math.ceil(apart * PIECES_A_METRE));
        final List<Vec3> points = new ArrayList<>(pieces + 1);
        for (int k = 0; k <= pieces; k++) {
            final double t = (double) k / pieces;
            points.add(a.lerp(b, t).subtract(0, sag * 4 * t * (1 - t), 0));
        }
        return points;
    }

    private static void onRender(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final List<StructureJoint> joints = StructureJoints.seen();
        if (level == null || joints.isEmpty()) {
            return;
        }
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack pose = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final RenderType type = RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS);
        final VertexConsumer out = buffers.getBuffer(type);
        final TextureAtlasSprite plain = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(PLAIN);
        final TextureAtlasSprite links = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(CHAIN_LINKS);
        final float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        final Colours colours = PALETTE.get();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        final Matrix4f matrix = pose.last().pose();
        for (final StructureJoint joint : joints) {
            if (!(joint.kind() instanceof JointKind.Rope) && !(joint.kind() instanceof JointKind.Spring)
                    && !(joint.kind() instanceof JointKind.Bar)) {
                continue;
            }
            final Vec3 a = drawnEnd(level, joint.a(), partialTick);
            final Vec3 b = drawnEnd(level, joint.b(), partialTick);
            if (a == null || b == null || camera.distanceToSqr(a.lerp(b, 0.5)) > RANGE * RANGE) {
                continue;
            }
            final Drawn drawn = new Drawn(out, matrix, pose, level);
            switch (joint.kind()) {
                case JointKind.Rope rope when rope.look() == JointKind.RopeLook.CHAIN ->
                        drawn.line(ropePoints(a, b, rope.length()), CHAIN, links, WHITE, true);
                case JointKind.Rope rope -> drawn.line(ropePoints(a, b, rope.length()), ROPE, plain,
                        colours.rope(), false);
                case JointKind.Spring spring -> drawn.line(coil(a, b), ROPE, plain, colours.spring(), false);
                default -> drawn.line(List.of(a, b), BAR, plain, colours.bar(), false);
            }
        }
        pose.popPose();
        buffers.endBatch(type);
    }

    /* The points of a spring's coils wound round the line from {@code a} to {@code b}. */
    private static List<Vec3> coil(final Vec3 a, final Vec3 b) {
        final Vec3 along = b.subtract(a);
        final double length = along.length();
        if (length < 1.0e-6) {
            return List.of(a, b);
        }
        final Vec3 way = along.scale(1 / length);
        Vec3 side = way.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : side.normalize();
        final Vec3 other = way.cross(side);
        final int coils = Math.max(2, (int) Math.round(length * COILS_A_METRE));
        final int pieces = coils * COIL_PIECES;
        final List<Vec3> points = new ArrayList<>(pieces + 1);
        for (int k = 0; k <= pieces; k++) {
            final double t = (double) k / pieces;
            final double angle = 2 * Math.PI * coils * t;
            points.add(a.add(along.scale(t)).add(side.scale(COIL * Math.cos(angle)))
                    .add(other.scale(COIL * Math.sin(angle))));
        }
        return points;
    }

    /* The buffer, the matrix and the level a frame's joints are drawn with. */
    private record Drawn(VertexConsumer out, Matrix4f matrix, PoseStack pose, ClientLevel level) {

        /*
         * A line through {@code points} as two thin crossed ribbons, {@code half} each side, so it reads from every
         * side, lit by the light where it passes; a chain's ribbons take its links, a block of them a block.
         */
        void line(final List<Vec3> points, final double half, final TextureAtlasSprite sprite, final int colour,
                  final boolean chain) {
            double walked = 0;
            for (int k = 1; k < points.size(); k++) {
                final Vec3 a = points.get(k - 1);
                final Vec3 b = points.get(k);
                final double length = a.distanceTo(b);
                if (length < 1.0e-6) {
                    continue;
                }
                final Vec3 along = b.subtract(a).scale(1 / length);
                Vec3 side = along.cross(new Vec3(0, 1, 0));
                side = side.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : side.normalize();
                final Vec3 up = side.cross(along).normalize();
                final int lightA = LevelRenderer.getLightColor(level, BlockPos.containing(a));
                final int lightB = LevelRenderer.getLightColor(level, BlockPos.containing(b));
                final float v0 = chain ? sprite.getV((float) (walked % 1.0)) : sprite.getV0();
                final float v1 = chain ? sprite.getV((float) Math.min(1.0, walked % 1.0 + length)) : sprite.getV1();
                ribbon(a, b, side.scale(half), up, sprite, colour, lightA, lightB, chain ? 0 : -1, v0, v1);
                ribbon(a, b, up.scale(half), side, sprite, colour, lightA, lightB, chain ? 1 : -1, v0, v1);
                walked += length;
            }
        }

        /* One ribbon from {@code a} to {@code b}; a chain's of its first or second strip of links. */
        private void ribbon(final Vec3 a, final Vec3 b, final Vec3 half, final Vec3 normal,
                            final TextureAtlasSprite sprite, final int colour, final int lightA, final int lightB,
                            final int strip, final float v0, final float v1) {
            final float u0 = strip < 0 ? sprite.getU0() : sprite.getU(strip * LINK_WIDE);
            final float u1 = strip < 0 ? sprite.getU1() : sprite.getU((strip + 1) * LINK_WIDE);
            vertex(a.subtract(half), u0, v0, colour, lightA, normal);
            vertex(a.add(half), u1, v0, colour, lightA, normal);
            vertex(b.add(half), u1, v1, colour, lightB, normal);
            vertex(b.subtract(half), u0, v1, colour, lightB, normal);
        }

        private void vertex(final Vec3 at, final float u, final float v, final int colour, final int light,
                            final Vec3 normal) {
            out.addVertex(matrix, (float) at.x, (float) at.y, (float) at.z).setColor(colour).setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(pose.last(), (float) normal.x, (float) normal.y, (float) normal.z);
        }
    }

    /**
     * The colours of the joints drawn.
     *
     * @param rope   a rope's
     * @param spring a spring's
     * @param bar    a bar's
     */
    public record Colours(int rope, int spring, int bar) {
    }
}
