/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.overhead;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.JsCore;
import dev.jstech.core.overhead.Catenary;
import dev.jstech.core.overhead.HungWire;
import dev.jstech.core.overhead.OverheadLines;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
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
import org.joml.Matrix4f;

/**
 * Draws the overhead wires of the player's dimension, as the server sent them: each sagging along its curve between
 * its insulators, as two thin crossed ribbons so it reads from every side, lit by the light where it hangs.
 */
@PaletteHolder
public final class OverheadWireRenderer {

    /** The colour of a bare wire: aluminium. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "overhead_wire", new Colours(
            0xFF << 24 | 0xA4AAB0));
    /** How far away a wire is still drawn, in blocks. */
    public static final double RANGE = 160.0;
    /* Half the wire's thickness, in blocks: a wire a sixteenth of a block across. */
    private static final double HALF = 1.0 / 32.0;
    /* The plain texture the wire is drawn with, tinted with its colour. */
    private static final ResourceLocation PLAIN = ResourceLocation.withDefaultNamespace("block/white_concrete");

    private OverheadWireRenderer() {
    }

    /** Hands the game's bus the drawing, once, from the Core's client. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, OverheadWireRenderer::onRender);
    }

    private static void onRender(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final List<HungWire> wires = OverheadLines.WIRES.client().wires();
        if (level == null || wires.isEmpty()) {
            return;
        }
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack pose = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final RenderType type = RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS);
        final VertexConsumer out = buffers.getBuffer(type);
        final TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(PLAIN);
        final int colour = PALETTE.get().wire();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        final Matrix4f matrix = pose.last().pose();
        for (final HungWire wire : wires) {
            if (wire.a().add(wire.b()).scale(0.5).distanceToSqr(camera) > RANGE * RANGE) {
                continue;
            }
            final List<Catenary.Point> points = Catenary.points(wire.start(), wire.end());
            for (int k = 1; k < points.size(); k++) {
                piece(out, matrix, pose, level, sprite, colour, points.get(k - 1), points.get(k));
            }
        }
        pose.popPose();
        buffers.endBatch(type);
    }

    /* One piece of wire, as a flat ribbon and an upright one crossing along it. */
    private static void piece(final VertexConsumer out, final Matrix4f matrix, final PoseStack pose,
                              final ClientLevel level, final TextureAtlasSprite sprite, final int colour,
                              final Catenary.Point from, final Catenary.Point to) {
        final Vec3 a = new Vec3(from.x(), from.y(), from.z());
        final Vec3 b = new Vec3(to.x(), to.y(), to.z());
        final Vec3 along = b.subtract(a).normalize();
        Vec3 side = along.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        final Vec3 up = side.cross(along).normalize();
        final int lightA = LevelRenderer.getLightColor(level, BlockPos.containing(a));
        final int lightB = LevelRenderer.getLightColor(level, BlockPos.containing(b));
        ribbon(out, matrix, pose, sprite, colour, a, b, side.scale(HALF), up, lightA, lightB);
        ribbon(out, matrix, pose, sprite, colour, a, b, up.scale(HALF), side, lightA, lightB);
    }

    private static void ribbon(final VertexConsumer out, final Matrix4f matrix, final PoseStack pose,
                               final TextureAtlasSprite sprite, final int colour, final Vec3 a, final Vec3 b,
                               final Vec3 half, final Vec3 normal, final int lightA, final int lightB) {
        vertex(out, matrix, pose, a.subtract(half), sprite.getU0(), sprite.getV0(), colour, lightA, normal);
        vertex(out, matrix, pose, a.add(half), sprite.getU1(), sprite.getV0(), colour, lightA, normal);
        vertex(out, matrix, pose, b.add(half), sprite.getU1(), sprite.getV1(), colour, lightB, normal);
        vertex(out, matrix, pose, b.subtract(half), sprite.getU0(), sprite.getV1(), colour, lightB, normal);
    }

    private static void vertex(final VertexConsumer out, final Matrix4f matrix, final PoseStack pose, final Vec3 at,
                               final float u, final float v, final int colour, final int light, final Vec3 normal) {
        out.addVertex(matrix, (float) at.x, (float) at.y, (float) at.z).setColor(colour).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose.last(), (float) normal.x, (float) normal.y, (float) normal.z);
    }

    /**
     * The colour of a bare wire.
     *
     * @param wire its colour
     */
    public record Colours(int wire) {
    }
}
