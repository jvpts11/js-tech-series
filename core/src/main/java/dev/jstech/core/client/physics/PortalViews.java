/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.physics;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.client.view.WorldViews;
import dev.jstech.core.physics.Laws;
import dev.jstech.core.physics.Physics;
import dev.jstech.core.physics.PhysicsArea;
import dev.jstech.core.physics.PhysicsMath;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Portals with no edge: an area whose way in leads elsewhere in its own dimension, said to be seamless, shows on its
 * faces what lies at the far side, as seen from where the player would come out, so walking in looks like walking on.
 * Before each frame the far side is drawn from that place into a picture the size of the screen ({@link WorldViews}):
 * its sky, the land the game has already built there, the things standing on it, its particles and its clouds. The
 * faces facing the player then show it where it falls on the screen. One portal at a time, the nearest within reach.
 */
public final class PortalViews {

    /* How near a portal must be for its far side to show, in blocks, and how finely its faces are cut. */
    private static final double REACH = 48.0;
    private static final int CUTS = 8;
    private static @Nullable TextureTarget feed;
    private static @Nullable PhysicsArea shown;
    /* The projection the game drew the last frame with: its field of view as the player has it, bobbing and all. */
    private static @Nullable Matrix4f lastProjection;

    private PortalViews() {
    }

    /** Hands the game's bus what this answers, once, from the Core's client. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, PortalViews::beforeFrame);
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, PortalViews::onStage);
    }

    /** The portal whose far side shows now, if any. */
    public static @Nullable PhysicsArea shown() {
        return shown;
    }

    /* Draws the far side of the nearest seamless portal into the picture its faces show. */
    private static void beforeFrame(final RenderFrameEvent.Pre event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        shown = null;
        if (level == null || minecraft.player == null || WorldViews.drawing()
                || minecraft.levelRenderer.viewArea == null) {
            return;
        }
        final DeltaTracker delta = event.getPartialTick();
        final float partial = delta.getGameTimeDeltaPartialTick(true);
        final Camera main = minecraft.gameRenderer.getMainCamera();
        // The game places its camera later in the frame; in the first person the eye is where the player looks from.
        final boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        final Vec3 eye = firstPerson ? minecraft.player.getEyePosition(partial) : main.getPosition();
        final float yRot = firstPerson ? minecraft.player.getViewYRot(partial) : main.getYRot();
        final float xRot = firstPerson ? minecraft.player.getViewXRot(partial) : main.getXRot();
        final PhysicsArea portal = nearest(level, eye);
        if (portal == null) {
            return;
        }
        final RenderTarget screen = minecraft.getMainRenderTarget();
        if (feed == null || feed.width != screen.width || feed.height != screen.height) {
            if (feed != null) {
                feed.destroyBuffers();
            }
            feed = new TextureTarget(screen.width, screen.height, true, Minecraft.ON_OSX);
        }
        final Laws.Space space = portal.laws().space().orElseThrow();
        final AABB from = portal.bounds();
        final AABB to = new PhysicsArea(portal.id(), space.target().orElseThrow(), Laws.NONE, 0).bounds();
        final Vec3 there = new Vec3(PhysicsMath.map(eye.x, from.minX, from.getXsize(), to.minX, to.getXsize()),
                PhysicsMath.map(eye.y, from.minY, from.getYsize(), to.minY, to.getYsize()),
                PhysicsMath.map(eye.z, from.minZ, from.getZsize(), to.minZ, to.getZsize()));
        final GameRenderer renderer = minecraft.gameRenderer;
        final Matrix4f projection = lastProjection != null ? new Matrix4f(lastProjection)
                : renderer.getProjectionMatrix(minecraft.options.fov().get());
        WorldViews.draw(minecraft, level, screen, there, yRot, xRot, projection, partial, false);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, screen.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, feed.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, screen.width, screen.height, 0, 0, feed.width, feed.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        screen.bindWrite(true);
        shown = portal;
    }

    /* The faces of the portal facing the eye, each showing the far side where it falls on the screen. */
    private static void onStage(final RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY && !WorldViews.drawing()) {
            lastProjection = new Matrix4f(event.getProjectionMatrix());
        }
        final PhysicsArea portal = shown;
        final TextureTarget picture = feed;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || WorldViews.drawing()
                || portal == null || picture == null) {
            return;
        }
        final Vec3 eye = event.getCamera().getPosition();
        if (portal.contains(eye)) {
            return;
        }
        final AABB box = portal.bounds();
        final Matrix4f view = new Matrix4f(event.getModelViewMatrix());
        final Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        final PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-eye.x, -eye.y, -eye.z);
        final Matrix4f matrix = pose.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, picture.getColorTextureId());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX);
        for (final Direction face : Direction.values()) {
            final Vec3 middle = box.getCenter().add(face.getStepX() * box.getXsize() / 2,
                    face.getStepY() * box.getYsize() / 2, face.getStepZ() * box.getZsize() / 2);
            final Vec3 towards = eye.subtract(middle);
            if (towards.x * face.getStepX() + towards.y * face.getStepY() + towards.z * face.getStepZ() <= 0) {
                continue;
            }
            face(builder, matrix, box, face, eye, view, projection);
        }
        final MeshData mesh = builder.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        pose.popPose();
    }

    /* One face of the box, cut into a grid, each corner taking the picture where it falls on the screen. */
    private static void face(final BufferBuilder builder, final Matrix4f matrix, final AABB box,
                             final Direction face, final Vec3 eye, final Matrix4f view, final Matrix4f projection) {
        for (int i = 0; i < CUTS; i++) {
            for (int j = 0; j < CUTS; j++) {
                final Vec3[] corners = {corner(box, face, i, j), corner(box, face, i + 1, j),
                    corner(box, face, i + 1, j + 1), corner(box, face, i, j + 1)};
                for (final Vec3 at : corners) {
                    final float[] uv = onScreen(at, eye, view, projection);
                    builder.addVertex(matrix, (float) at.x, (float) at.y, (float) at.z).setUv(uv[0], uv[1]);
                }
            }
        }
    }

    /* The corner {@code i, j} of the grid of {@code face}. */
    private static Vec3 corner(final AABB box, final Direction face, final int i, final int j) {
        final double a = (double) i / CUTS;
        final double b = (double) j / CUTS;
        return switch (face.getAxis()) {
            case X -> new Vec3(face == Direction.EAST ? box.maxX : box.minX, box.minY + a * box.getYsize(),
                    box.minZ + b * box.getZsize());
            case Y -> new Vec3(box.minX + b * box.getXsize(), face == Direction.UP ? box.maxY : box.minY,
                    box.minZ + a * box.getZsize());
            case Z -> new Vec3(box.minX + a * box.getXsize(), box.minY + b * box.getYsize(),
                    face == Direction.SOUTH ? box.maxZ : box.minZ);
        };
    }

    /* Where {@code at} falls on the screen, from 0 to 1 across and up, as the picture is laid. */
    private static float[] onScreen(final Vec3 at, final Vec3 eye, final Matrix4f view, final Matrix4f projection) {
        final Vector4f clip = new Vector4f((float) (at.x - eye.x), (float) (at.y - eye.y), (float) (at.z - eye.z),
                1.0F);
        view.transform(clip);
        projection.transform(clip);
        final float w = Math.max(clip.w, 1e-4F);
        return new float[] {clip.x / w * 0.5F + 0.5F, clip.y / w * 0.5F + 0.5F};
    }

    /* The seamless portal nearest the eye within reach, leading elsewhere in this dimension. */
    private static @Nullable PhysicsArea nearest(final ClientLevel level, final Vec3 eye) {
        PhysicsArea best = null;
        double nearest = REACH * REACH;
        for (final PhysicsArea area : Physics.areas(level)) {
            final Laws.Space space = area.laws().space().orElse(null);
            if (space == null || space.kind() != Laws.Crossing.ENTER || !space.seamless() || space.target().isEmpty()
                    || space.dimension().map(dimension -> !dimension.equals(level.dimension())).orElse(false)) {
                continue;
            }
            final double away = area.bounds().distanceToSqr(eye);
            if (away < nearest && !area.contains(eye)) {
                nearest = away;
                best = area;
            }
        }
        return best;
    }
}
