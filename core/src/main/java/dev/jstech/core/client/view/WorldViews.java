/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.view;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

/**
 * The world drawn from somewhere other than the player's eye, into a picture: the far side of a seamless portal, what
 * a camera in the world sees for a screen to show. It is drawn from the land the game keeps built, never by the
 * game's own drawing of the world from a second place: that would unsettle what the game works out it can see from
 * the player's eye, and the land near the player would go missing. So a view shows only what the game has built,
 * within the player's view distance, and what only the game's own drawing adds (its weather, and other mods' drawing
 * at its stages) does not show in it.
 *
 * <p>A view is drawn before the frame, from {@code RenderFrameEvent.Pre}; while it is drawn, {@link #drawing()} says
 * so, for whatever would otherwise draw itself into it.
 */
public final class WorldViews {

    /* The game's own nearest fog for land, in blocks, and half a section, to its middle. */
    private static final float LEAST_FOG = 32.0F;
    private static final int HALF_SECTION = 8;
    private static final ViewCamera CAMERA = new ViewCamera();
    private static boolean drawing;

    private WorldViews() {
    }

    /** Whether a view is being drawn now. */
    public static boolean drawing() {
        return drawing;
    }

    /**
     * Draws the world as seen from {@code at}, looking {@code yRot} and {@code xRot} in the game's degrees, through
     * {@code projection}, into {@code target}: its sky, the solid land near to far, the things standing there, the
     * clear land far to near, the particles and the clouds; the player among the things only when {@code player}, as
     * a camera elsewhere sees them, never at a view from their own eye. The target is left bound; the caller binds the
     * screen's own again when it is done.
     */
    public static void draw(final Minecraft minecraft, final ClientLevel level, final RenderTarget target,
                            final Vec3 at, final float yRot, final float xRot, final Matrix4f projection,
                            final float partial, final boolean player) {
        if (minecraft.player == null || minecraft.levelRenderer.viewArea == null || drawing) {
            return;
        }
        drawing = true;
        try {
            target.bindWrite(true);
            final GameRenderer renderer = minecraft.gameRenderer;
            renderer.lightTexture().updateLightTexture(partial);
            CAMERA.place(level, minecraft.player, at, yRot, xRot, partial);
            renderer.resetProjectionMatrix(projection);
            final Matrix4f rotation = new Matrix4f().rotation(CAMERA.rotation().conjugate(new Quaternionf()));
            drawFrom(minecraft, level, target, rotation, projection, partial, player);
        } finally {
            drawing = false;
        }
    }

    /** The projection a view of {@code fov} degrees, {@code width} by {@code height}, is drawn through. */
    public static Matrix4f projection(final Minecraft minecraft, final double fov, final int width,
                                      final int height) {
        return new Matrix4f().setPerspective((float) Math.toRadians(fov), (float) width / Math.max(1, height),
                GameRenderer.PROJECTION_Z_NEAR, minecraft.gameRenderer.getDepthFar());
    }

    private static void drawFrom(final Minecraft minecraft, final ClientLevel level, final RenderTarget target,
                                 final Matrix4f rotation, final Matrix4f projection, final float partial,
                                 final boolean player) {
        final LevelRenderer world = minecraft.levelRenderer;
        final GameRenderer renderer = minecraft.gameRenderer;
        final Vec3 at = CAMERA.getPosition();
        final float distance = renderer.getRenderDistance();
        final boolean foggy = level.effects().isFoggyAt(Mth.floor(at.x), Mth.floor(at.y))
                || minecraft.gui.getBossOverlay().shouldCreateWorldFog();
        FogRenderer.setupColor(CAMERA, partial, level, minecraft.options.getEffectiveRenderDistance(),
                renderer.getDarkenWorldAmount(partial));
        FogRenderer.levelFogColor();
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        FogRenderer.setupFog(CAMERA, FogRenderer.FogMode.FOG_SKY, distance, foggy, partial);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        world.renderSky(rotation, projection, partial, CAMERA, foggy,
                () -> FogRenderer.setupFog(CAMERA, FogRenderer.FogMode.FOG_SKY, distance, foggy, partial));
        FogRenderer.setupFog(CAMERA, FogRenderer.FogMode.FOG_TERRAIN, Math.max(distance, LEAST_FOG), foggy, partial);
        final Frustum frustum = new Frustum(rotation, projection);
        frustum.prepare(at.x, at.y, at.z);
        final List<SectionRenderDispatcher.RenderSection> sections = sectionsSeen(world.viewArea,
                LevelRenderer.offsetFrustum(frustum), at);
        layer(minecraft, target, RenderType.solid(), sections, at, rotation, projection, false);
        final TextureAtlas blocks = minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        blocks.setBlurMipmap(false, minecraft.options.mipmapLevels().get() > 0);
        layer(minecraft, target, RenderType.cutoutMipped(), sections, at, rotation, projection, false);
        blocks.restoreLastBlurMipmap();
        layer(minecraft, target, RenderType.cutout(), sections, at, rotation, projection, false);
        if (level.effects().constantAmbientLight()) {
            Lighting.setupNetherLevel();
        } else {
            Lighting.setupLevel();
        }
        final Matrix4fStack view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.mul(rotation);
        RenderSystem.applyModelViewMatrix();
        things(minecraft, level, sections, frustum, at, partial, player);
        layer(minecraft, target, RenderType.translucent(), sections, at, rotation, projection, true);
        layer(minecraft, target, RenderType.tripwire(), sections, at, rotation, projection, true);
        minecraft.particleEngine.render(renderer.lightTexture(), CAMERA, partial, frustum, type -> true);
        if (minecraft.options.getCloudsType() != CloudStatus.OFF) {
            world.renderClouds(new PoseStack(), rotation, projection, partial, at.x, at.y, at.z);
        }
        view.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    /* The sections the game has built that the camera at {@code at} sees within {@code frustum}, nearest first. */
    private static List<SectionRenderDispatcher.RenderSection> sectionsSeen(final ViewArea area, final Frustum frustum,
                                                                           final Vec3 at) {
        final List<SectionRenderDispatcher.RenderSection> seen = new ArrayList<>();
        for (final SectionRenderDispatcher.RenderSection section : area.sections) {
            if (frustum.isVisible(section.getBoundingBox())) {
                seen.add(section);
            }
        }
        seen.sort(Comparator.comparingDouble(section -> {
            final BlockPos origin = section.getOrigin();
            final double x = origin.getX() + HALF_SECTION - at.x;
            final double y = origin.getY() + HALF_SECTION - at.y;
            final double z = origin.getZ() + HALF_SECTION - at.z;
            return x * x + y * y + z * z;
        }));
        return seen;
    }

    /* One layer of the built land, as the game draws it: near to far, or far to near for what is clear. */
    private static void layer(final Minecraft minecraft, final RenderTarget target, final RenderType type,
                              final List<SectionRenderDispatcher.RenderSection> sections, final Vec3 at,
                              final Matrix4f rotation, final Matrix4f projection, final boolean farFirst) {
        type.setupRenderState();
        // With Fabulous graphics the game draws clear land to a picture of its own; a view is one picture.
        target.bindWrite(false);
        final ShaderInstance shader = RenderSystem.getShader();
        if (shader == null) {
            type.clearRenderState();
            return;
        }
        shader.setDefaultUniforms(VertexFormat.Mode.QUADS, rotation, projection, minecraft.getWindow());
        shader.apply();
        final Uniform offset = shader.CHUNK_OFFSET;
        final int count = sections.size();
        for (int i = 0; i < count; i++) {
            final SectionRenderDispatcher.RenderSection section = sections.get(farFirst ? count - 1 - i : i);
            if (section.getCompiled().isEmpty(type)) {
                continue;
            }
            final BlockPos origin = section.getOrigin();
            if (offset != null) {
                offset.set((float) (origin.getX() - at.x), (float) (origin.getY() - at.y),
                        (float) (origin.getZ() - at.z));
                offset.upload();
            }
            final VertexBuffer buffer = section.getBuffer(type);
            buffer.bind();
            buffer.draw();
        }
        if (offset != null) {
            offset.set(0F, 0F, 0F);
        }
        shader.clear();
        VertexBuffer.unbind();
        type.clearRenderState();
    }

    /* The entities and the block entities seen; the player among them only when {@code player}. */
    private static void things(final Minecraft minecraft, final ClientLevel level,
                               final List<SectionRenderDispatcher.RenderSection> sections, final Frustum frustum,
                               final Vec3 at, final float partial, final boolean player) {
        final EntityRenderDispatcher entities = minecraft.getEntityRenderDispatcher();
        final BlockEntityRenderDispatcher blocks = minecraft.getBlockEntityRenderDispatcher();
        entities.prepare(level, CAMERA, null);
        blocks.prepare(level, CAMERA, minecraft.hitResult);
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final PoseStack pose = new PoseStack();
        for (final Entity entity : level.entitiesForRendering()) {
            if (!player && entity == minecraft.player || !entities.shouldRender(entity, frustum, at.x, at.y, at.z)) {
                continue;
            }
            entities.render(entity, Mth.lerp(partial, entity.xOld, entity.getX()) - at.x,
                    Mth.lerp(partial, entity.yOld, entity.getY()) - at.y,
                    Mth.lerp(partial, entity.zOld, entity.getZ()) - at.z,
                    Mth.lerp(partial, entity.yRotO, entity.getYRot()), partial, pose, buffers,
                    entities.getPackedLightCoords(entity, partial));
        }
        for (final SectionRenderDispatcher.RenderSection section : sections) {
            for (final BlockEntity block : section.getCompiled().getRenderableBlockEntities()) {
                if (!ClientHooks.isBlockEntityRendererVisible(blocks, block, frustum)) {
                    continue;
                }
                final BlockPos pos = block.getBlockPos();
                pose.pushPose();
                pose.translate(pos.getX() - at.x, pos.getY() - at.y, pos.getZ() - at.z);
                blocks.render(block, partial, pose, buffers);
                pose.popPose();
            }
        }
        buffers.endBatch();
    }

    /* A camera placed anywhere: the game's camera, set up on the player, then moved and turned. */
    private static final class ViewCamera extends Camera {

        void place(final BlockGetter level, final Entity anchor, final Vec3 at, final float yRot, final float xRot,
                   final float partialTick) {
            setup(level, anchor, true, false, partialTick);
            setRotation(yRot, xRot);
            setPosition(at);
        }
    }
}
