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
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.physics.Laws;
import dev.jstech.core.physics.Physics;
import dev.jstech.core.physics.PhysicsArea;
import dev.jstech.core.physics.PhysicsMath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Portals with no edge: an area whose way in leads elsewhere in its own dimension, said to be seamless, shows on its
 * faces what lies at the far side, as seen from where the player would come out, so walking in looks like walking on.
 * Before each frame the far side is drawn from that place into a picture the size of the screen: its sky, the land the
 * game has already built there, the things standing on it, its particles and its clouds. The faces facing the player
 * then show it where it falls on the screen. One portal at a time, the nearest within reach.
 *
 * <p>The far side is drawn by the portal itself from the land the game keeps built, never by the game's own drawing of
 * the world from a second place: that would unsettle what the game works out it can see from the player's eye, and
 * the land near the player would go missing. What only the game's own drawing adds (its weather, and other mods'
 * drawing at its stages) does not show through a portal.
 */
public final class PortalViews {

    /* How near a portal must be for its far side to show, in blocks, and how finely its faces are cut. */
    private static final double REACH = 48.0;
    private static final int CUTS = 8;
    /* The game's own nearest fog for land, in blocks, and half a section, to its middle. */
    private static final float LEAST_FOG = 32.0F;
    private static final int HALF_SECTION = 8;
    private static final FeedCamera CAMERA = new FeedCamera();
    private static @Nullable TextureTarget feed;
    private static @Nullable PhysicsArea shown;
    /* The projection the game drew the last frame with: its field of view as the player has it, bobbing and all. */
    private static @Nullable Matrix4f lastProjection;
    private static boolean feeding;

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
        if (level == null || minecraft.player == null || feeding || minecraft.levelRenderer.viewArea == null) {
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
        feeding = true;
        try {
            screen.bindWrite(true);
            final GameRenderer renderer = minecraft.gameRenderer;
            renderer.lightTexture().updateLightTexture(partial);
            CAMERA.place(level, minecraft.player, there, yRot, xRot, partial);
            final Matrix4f projection = lastProjection != null ? new Matrix4f(lastProjection)
                    : renderer.getProjectionMatrix(minecraft.options.fov().get());
            renderer.resetProjectionMatrix(projection);
            final Matrix4f rotation = new Matrix4f().rotation(CAMERA.rotation().conjugate(new Quaternionf()));
            drawFarSide(minecraft, level, rotation, projection, partial);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, screen.frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, feed.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, screen.width, screen.height, 0, 0, feed.width, feed.height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            screen.bindWrite(true);
            shown = portal;
        } finally {
            feeding = false;
        }
    }

    /*
     * The far side as the game draws a world, from the camera placed there: the sky, the solid land near to far, the
     * things standing there, the clear land far to near, the particles and the clouds.
     */
    private static void drawFarSide(final Minecraft minecraft, final ClientLevel level, final Matrix4f rotation,
                                    final Matrix4f projection, final float partial) {
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
        layer(minecraft, RenderType.solid(), sections, at, rotation, projection, false);
        final TextureAtlas blocks = minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        blocks.setBlurMipmap(false, minecraft.options.mipmapLevels().get() > 0);
        layer(minecraft, RenderType.cutoutMipped(), sections, at, rotation, projection, false);
        blocks.restoreLastBlurMipmap();
        layer(minecraft, RenderType.cutout(), sections, at, rotation, projection, false);
        if (level.effects().constantAmbientLight()) {
            Lighting.setupNetherLevel();
        } else {
            Lighting.setupLevel();
        }
        final Matrix4fStack view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.mul(rotation);
        RenderSystem.applyModelViewMatrix();
        things(minecraft, level, sections, frustum, at, partial);
        layer(minecraft, RenderType.translucent(), sections, at, rotation, projection, true);
        layer(minecraft, RenderType.tripwire(), sections, at, rotation, projection, true);
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
    private static void layer(final Minecraft minecraft, final RenderType type,
                              final List<SectionRenderDispatcher.RenderSection> sections, final Vec3 at,
                              final Matrix4f rotation, final Matrix4f projection, final boolean farFirst) {
        type.setupRenderState();
        // With Fabulous graphics the game draws clear land to a picture of its own; the far side is one picture.
        minecraft.getMainRenderTarget().bindWrite(false);
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

    /*
     * The entities and the block entities of the far side. The player stands on the near side, so they never show at
     * the eye they look through.
     */
    private static void things(final Minecraft minecraft, final ClientLevel level,
                               final List<SectionRenderDispatcher.RenderSection> sections, final Frustum frustum,
                               final Vec3 at, final float partial) {
        final EntityRenderDispatcher entities = minecraft.getEntityRenderDispatcher();
        final BlockEntityRenderDispatcher blocks = minecraft.getBlockEntityRenderDispatcher();
        entities.prepare(level, CAMERA, null);
        blocks.prepare(level, CAMERA, minecraft.hitResult);
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final PoseStack pose = new PoseStack();
        for (final Entity entity : level.entitiesForRendering()) {
            if (entity == minecraft.player || !entities.shouldRender(entity, frustum, at.x, at.y, at.z)) {
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

    /* The faces of the portal facing the eye, each showing the far side where it falls on the screen. */
    private static void onStage(final RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            lastProjection = new Matrix4f(event.getProjectionMatrix());
        }
        final PhysicsArea portal = shown;
        final TextureTarget picture = feed;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || feeding || portal == null
                || picture == null) {
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

    /* A camera placed anywhere: the game's camera, set up on the player, then moved and turned. */
    private static final class FeedCamera extends Camera {

        void place(final BlockGetter level, final Entity anchor, final Vec3 at, final float yRot, final float xRot,
                   final float partialTick) {
            setup(level, anchor, true, false, partialTick);
            setRotation(yRot, xRot);
            setPosition(at);
        }
    }
}
