/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.shipyard;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.space.client.ship.ShipPartsRenderer;
import dev.jstech.space.rocket.CabinReadings;
import dev.jstech.space.ship.ShipPartModels;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

/**
 * The Starship Design's view of the ship, as the approved design draws it: the ship on a floor in a dark void, the
 * floor a cross every second block and a line along and across the ship's middle, fading into the void far off, seen
 * through a camera turning round the ship's middle at a height, as an editor's does. It draws the ship's models, and a
 * part held over it see-through in cyan; and it finds where a point of the ship falls on the screen.
 */
final class ShipDesignView {

    private float yaw = START_YAW;
    private float pitch = START_PITCH;
    private double distance = START_DISTANCE;
    private Vec3 target = Vec3.ZERO;
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();

    /* The approved design's camera: before the nose, to the ship's right; its height, how far, how wide it sees. */
    private static final float START_YAW = 218.0F;
    private static final float START_PITCH = 30.0F;
    private static final double START_DISTANCE = 28.0;
    private static final float FOV = 52.0F;
    private static final double NEAREST = 6.0;
    private static final double FURTHEST = 80.0;
    private static final float LOWEST = 5.0F;
    private static final float HIGHEST = 85.0F;
    private static final float NEAR = 0.1F;
    private static final float FAR = 400.0F;
    /* The floor: how far round the ship it is laid, its tile, a cross's arm and thickness, the axes' width. */
    private static final int FLOOR_REACH = 40;
    private static final int TILE = 4;
    private static final int CROSS_EVERY = 2;
    private static final float ARM = 0.2F;
    private static final float THICK = 0.03F;
    private static final float AXIS = 0.04F;
    private static final float ABOVE = 0.002F;
    /* Where the floor fades into the void, from the camera, in blocks. */
    private static final double FOG_FROM = 40;
    private static final double FOG_TO = 110;

    /** Turns the camera by the mouse's move, in degrees. */
    void turn(final float yawBy, final float pitchBy) {
        this.yaw += yawBy;
        this.pitch = Math.clamp(this.pitch + pitchBy, LOWEST, HIGHEST);
    }

    /** Brings the camera nearer, or takes it further, by {@code times}. */
    void zoom(final double times) {
        this.distance = Math.clamp(this.distance * times, NEAREST, FURTHEST);
    }

    /** Points the camera at {@code middle}, the ship's. */
    void aim(final Vec3 middle) {
        this.target = middle;
    }

    /**
     * Draws the void, the floor at {@code floorY}, the ship's {@code models} for {@code owner}, and the {@code held}
     * part's models see-through in {@code holo}, moved by {@code heldAt}; over the whole screen {@code width} by
     * {@code height}, in its scaled pixels.
     */
    void draw(final GuiGraphics g, final int width, final int height, final BlockEntity owner,
              final List<ShipPartModels.Placed> models, final List<ShipPartModels.Placed> held, final Vec3 heldAt,
              final double floorY) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        g.fillGradient(0, 0, width, height, c.skyHigh(), c.skyLow());
        g.flush();
        matrices(width, height);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(this.projection, VertexSorting.DISTANCE_TO_ORIGIN);
        final Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.enableDepthTest();
        // Lit as the world lights a model, its tops brightest, as the approved design draws the ship.
        Lighting.setupLevel();
        final MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        final PoseStack pose = new PoseStack();
        pose.last().pose().mul(this.view);
        try {
            floor(pose, buffers, floorY, c);
            buffers.endBatch(RenderType.debugQuads());
            ShipPartsRenderer.draw(owner, models, CabinReadings.AT_REST, pose, buffers, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY);
            buffers.endBatch();
            pose.pushPose();
            pose.translate(heldAt.x, heldAt.y, heldAt.z);
            for (final ShipPartModels.Placed part : held) {
                final Optional<KitModel> model = KitModels.get(part.model());
                if (model.isEmpty()) {
                    continue;
                }
                pose.pushPose();
                pose.translate(0, 0, part.z());
                if (part.flipped()) {
                    pose.scale(-1, 1, 1);
                }
                KitRenderer.drawTinted(KitAnimations.part(owner, "held:" + part.model() + "@" + part.z(),
                        model.get()), pose, buffers, c.holo(), LightTexture.FULL_BRIGHT);
                pose.popPose();
            }
            pose.popPose();
            buffers.endBatch();
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            Lighting.setupFor3DItems();
        }
    }

    /**
     * Where the point ({@code x}, {@code y}, {@code z}) of the ship falls on a screen {@code width} by {@code height},
     * in its scaled pixels; none behind the camera.
     */
    Optional<float[]> project(final double x, final double y, final double z, final int width, final int height) {
        matrices(width, height);
        final Vector4f at = new Vector4f((float) x, (float) y, (float) z, 1.0F);
        this.view.transform(at);
        this.projection.transform(at);
        if (at.w <= NEAR) {
            return Optional.empty();
        }
        return Optional.of(new float[] {(at.x / at.w + 1.0F) / 2.0F * width, (1.0F - at.y / at.w) / 2.0F * height});
    }

    /* The camera's matrices for a screen {@code width} by {@code height}. */
    private void matrices(final int width, final int height) {
        this.projection.setPerspective((float) Math.toRadians(FOV), width / (float) Math.max(1, height), NEAR, FAR);
        this.view.identity()
                .translate(0, 0, (float) -this.distance)
                .rotateX((float) Math.toRadians(this.pitch))
                .rotateY((float) Math.toRadians(this.yaw))
                .translate((float) -this.target.x, (float) -this.target.y, (float) -this.target.z);
    }

    /* The floor round the ship's middle: tiles fading into the void, a cross every second block, the two axes. */
    private void floor(final PoseStack pose, final MultiBufferSource buffers, final double floorY,
                       final ShipDesignArt.Colours c) {
        final VertexConsumer out = buffers.getBuffer(RenderType.debugQuads());
        final Matrix4f m = pose.last().pose();
        final float y = (float) floorY;
        final int cx = (int) Math.round(this.target.x);
        final int cz = (int) Math.round(this.target.z);
        for (int x = cx - FLOOR_REACH; x < cx + FLOOR_REACH; x += TILE) {
            for (int z = cz - FLOOR_REACH; z < cz + FLOOR_REACH; z += TILE) {
                quad(out, m, x, y, z, x + TILE, z + TILE, c.floor(), c);
            }
        }
        final float lift = y + ABOVE;
        for (int x = cx - FLOOR_REACH; x <= cx + FLOOR_REACH; x += CROSS_EVERY) {
            for (int z = cz - FLOOR_REACH; z <= cz + FLOOR_REACH; z += CROSS_EVERY) {
                if (x == 0 || z == 0) {
                    continue;
                }
                cross(out, m, x, lift, z, c);
            }
        }
        quad(out, m, -AXIS, lift, cz - FLOOR_REACH, AXIS, cz + FLOOR_REACH, c.axis(), c);
        quad(out, m, cx - FLOOR_REACH, lift, -AXIS, cx + FLOOR_REACH, AXIS, c.axis(), c);
    }

    /* A cross on the floor at ({@code x}, {@code z}): its two arms, each a thin quad on the slant. */
    private void cross(final VertexConsumer out, final Matrix4f m, final float x, final float y, final float z,
                       final ShipDesignArt.Colours c) {
        final int colour = fogged(c.floorMark(), x, y, z, c);
        for (final int sign : new int[] {1, -1}) {
            vertex(out, m, x - ARM - THICK, y, z - sign * ARM + THICK * sign, colour);
            vertex(out, m, x - ARM + THICK, y, z - sign * ARM - THICK * sign, colour);
            vertex(out, m, x + ARM + THICK, y, z + sign * ARM - THICK * sign, colour);
            vertex(out, m, x + ARM - THICK, y, z + sign * ARM + THICK * sign, colour);
        }
    }

    /* A flat quad on the floor from ({@code x0}, {@code z0}) to ({@code x1}, {@code z1}), fogged at each corner. */
    private void quad(final VertexConsumer out, final Matrix4f m, final float x0, final float y, final float z0,
                      final float x1, final float z1, final int colour, final ShipDesignArt.Colours c) {
        vertex(out, m, x0, y, z0, fogged(colour, x0, y, z0, c));
        vertex(out, m, x0, y, z1, fogged(colour, x0, y, z1, c));
        vertex(out, m, x1, y, z1, fogged(colour, x1, y, z1, c));
        vertex(out, m, x1, y, z0, fogged(colour, x1, y, z0, c));
    }

    /* {@code colour} faded toward the void's horizon by how far the point is from the camera. */
    private int fogged(final int colour, final float x, final float y, final float z, final ShipDesignArt.Colours c) {
        final double yawRad = Math.toRadians(this.yaw);
        final double pitchRad = Math.toRadians(this.pitch);
        final double ex = this.target.x - Math.sin(yawRad) * Math.cos(pitchRad) * this.distance;
        final double ey = this.target.y + Math.sin(pitchRad) * this.distance;
        final double ez = this.target.z + Math.cos(yawRad) * Math.cos(pitchRad) * this.distance;
        final double far = Math.sqrt((x - ex) * (x - ex) + (y - ey) * (y - ey) + (z - ez) * (z - ez));
        final float share = (float) Math.clamp((far - FOG_FROM) / (FOG_TO - FOG_FROM), 0.0, 1.0);
        return FastColor.ARGB32.lerp(share, colour, c.skyLow());
    }

    private static void vertex(final VertexConsumer out, final Matrix4f m, final float x, final float y,
                               final float z, final int colour) {
        out.addVertex(m, x, y, z).setColor(colour);
    }
}
