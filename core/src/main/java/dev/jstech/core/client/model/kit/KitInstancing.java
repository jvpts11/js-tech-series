/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.client.instancing.InstanceData;
import dev.jstech.core.client.instancing.InstanceTexels;
import dev.jstech.core.client.instancing.Instancing;
import dev.jstech.core.client.light.KitGlows;
import dev.jstech.core.model.BakedGeometry;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL31;

/**
 * The Core's models drawn many at once in the world: every thing drawn with one model (block entities, entities,
 * turning parts) gives the model's shape on the graphics card its light, its overlay and the place of each of its
 * bones, and once the world's block entities are drawn each model is drawn in one call for each of its pictures, with
 * every thing's pose and light; a hidden bone is a place of nothing. From the sky being drawn until then, a thing
 * drawn as the world draws it is taken here; one drawn anywhere else, with the cracks of being broken or an outline,
 * see-through, bent or morphed by a mesh, with layers laid over it, or damaged ({@link KitDamage}), is drawn one by one
 * as before, and so is all of it while {@link Instancing#on} says no.
 */
public final class KitInstancing {

    private static final Map<KitModel, Batch> BATCHES = new IdentityHashMap<>();
    private static final Map<KitModel, KitMesh> MESHES = new IdentityHashMap<>();
    private static final Map<KitModel, List<KitMesh.Glow>> GLOWS = new IdentityHashMap<>();
    private static final InstanceTexels TEXELS = new InstanceTexels();
    private static final String[] SAMPLERS = {"Sampler0", "Sampler1", "Sampler2"};
    private static final int LOW = 0xFFFF;
    private static final int HIGH = 16;
    /* The texel of light and overlay, and the three of each bone's place. */
    private static final int ROWS = 3;
    private static final Matrix4f PLACE = new Matrix4f();
    private static final Vector3f MIDDLE = new Vector3f();

    private static boolean collecting;
    private static int drawnLast;
    private static int callsLast;

    private KitInstancing() {
    }

    /** Hands the game's bus what this answers, once, from the Core's client. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, KitInstancing::onStage);
    }

    /** How many things were drawn many at once in the last frame. */
    public static int drawnLastFrame() {
        return drawnLast;
    }

    /** How many calls drew them. */
    public static int callsLastFrame() {
        return callsLast;
    }

    /**
     * Takes {@code instance} to be drawn with the others of its model, the pose stack standing where its model's
     * origin is in pixels; whether it was taken, or is to be drawn one by one.
     *
     * @param model   the model it is drawn as, its own or, far away, its model of fewer triangles
     * @param hidden  the bones hidden this frame
     * @param painted the variant's paint for its first picture, or none
     * @param glowsIn the world its glowing parts light, or none
     */
    static boolean take(final KitInstance instance, final KitModel model, final boolean[] hidden,
                        @Nullable final ResourceLocation painted, final PoseStack poses,
                        final MultiBufferSource buffers, final int light, final int overlay,
                        @Nullable final Level glowsIn) {
        if (!collecting || buffers != Minecraft.getInstance().renderBuffers().bufferSource()
                || model.definition().translucent() || !instance.deformers().isEmpty() || KitDamage.damaged(model)
                || !KitLayers.of(model.id()).isEmpty()) {
            return false;
        }
        final Batch batch = batch(model, painted);
        final InstanceData data = batch.data;
        data.put(light & LOW, light >>> HIGH, overlay & LOW, overlay >>> HIGH);
        final Matrix4f pose = poses.last().pose();
        final Matrix4f[] world = instance.world();
        final List<List<BakedGeometry.Part>> bones = model.geometry().bones();
        for (int i = 0; i < world.length; i++) {
            if (hidden[i] || i >= bones.size() || bones.get(i).isEmpty()) {
                data.putZeros(ROWS);
            } else {
                data.putRows(PLACE.set(pose).mul(world[i]));
            }
        }
        batch.count++;
        if (glowsIn != null) {
            report(model, hidden, pose, world, glowsIn);
        }
        return true;
    }

    /** Lets every model's shape on the graphics card go, as the models are read again. On the render thread. */
    static void forget() {
        MESHES.values().forEach(KitMesh::close);
        MESHES.clear();
        GLOWS.clear();
        for (final Batch head : BATCHES.values()) {
            for (Batch batch = head; batch != null; batch = batch.next) {
                batch.data.close();
            }
        }
        BATCHES.clear();
    }

    private static void onStage(final RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            collecting = Instancing.on();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            collecting = false;
            draw();
        }
    }

    /* The batch of a model, with a paint or without. */
    private static Batch batch(final KitModel model, @Nullable final ResourceLocation painted) {
        Batch batch = BATCHES.get(model);
        if (batch == null) {
            batch = new Batch(model, painted);
            BATCHES.put(model, batch);
            return batch;
        }
        while (!Objects.equals(batch.painted, painted)) {
            if (batch.next == null) {
                batch.next = new Batch(model, painted);
            }
            batch = batch.next;
        }
        return batch;
    }

    /* Tells the world of a thing's glowing parts, where they are, as they are drawn one by one. */
    private static void report(final KitModel model, final boolean[] hidden, final Matrix4f pose,
                               final Matrix4f[] world, final Level level) {
        final List<KitMesh.Glow> glows = GLOWS.computeIfAbsent(model, KitMesh::glowsOf);
        if (glows.isEmpty()) {
            return;
        }
        final Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        for (final KitMesh.Glow glow : glows) {
            if (hidden[glow.bone()]) {
                continue;
            }
            PLACE.set(pose).mul(world[glow.bone()]).transformPosition(glow.x(), glow.y(), glow.z(), MIDDLE);
            KitGlows.report(level, camera.add(MIDDLE.x(), MIDDLE.y(), MIDDLE.z()), model.definition().glowLight(),
                    model.definition().glowColour());
        }
    }

    /* Draws every batch taken this frame, and empties it for the next. */
    private static void draw() {
        final ShaderInstance shader = Instancing.kit();
        int drawn = 0;
        int calls = 0;
        final long tick = KitClock.tick();
        for (final Batch head : BATCHES.values()) {
            for (Batch batch = head; batch != null; batch = batch.next) {
                if (batch.count > 0 && shader != null) {
                    calls += drawBatch(shader, batch, tick);
                    drawn += batch.count;
                }
                batch.count = 0;
                batch.data.clear();
            }
        }
        drawnLast = drawn;
        callsLast = calls;
    }

    /* Draws a batch, as many at a time as the graphics card reads in one go; the calls it took. */
    private static int drawBatch(final ShaderInstance shader, final Batch batch, final long tick) {
        final KitModel model = batch.model;
        final KitMesh mesh = MESHES.computeIfAbsent(model, KitMesh::of);
        final int stride = 1 + ROWS * model.data().bones().size();
        final int most = Math.max(1, Instancing.mostTexels() / stride);
        int calls = 0;
        for (int first = 0; first < batch.count; first += most) {
            final int count = Math.min(most, batch.count - first);
            TEXELS.send(batch.data, first * stride, count * stride);
            for (final KitMesh.Group group : mesh.groups()) {
                final ResourceLocation location = KitRenderer.location(model, group.picture(), batch.painted);
                final KitTextures.Look look = KitTextures.of(location);
                drawGroup(shader, KitRenderTypes.cutout(location, false), group, stride, count, look.frames(), tick,
                        false);
                calls++;
                if (look.glowing() != null) {
                    drawGroup(shader, RenderType.entityTranslucentEmissive(look.glowing()), group, stride, count,
                            look.frames(), tick, true);
                    calls++;
                }
            }
        }
        return calls;
    }

    /* One call: a group's triangles for {@code count} things, in the render states of {@code type}. */
    private static void drawGroup(final ShaderInstance shader, final RenderType type, final KitMesh.Group group,
                                  final int stride, final int count, final TextureFrames frames, final long tick,
                                  final boolean glow) {
        type.setupRenderState();
        for (int i = 0; i < SAMPLERS.length; i++) {
            shader.setSampler(SAMPLERS[i], RenderSystem.getShaderTexture(i));
        }
        shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, RenderSystem.getModelViewMatrix(),
                RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
        shader.safeGetUniform("KitStride").set(stride);
        shader.safeGetUniform("KitFrame").set(frames.vScale(), frames.vOffset(tick));
        shader.safeGetUniform("KitGlow").set(glow ? 1.0F : 0.0F);
        shader.apply();
        TEXELS.bind(shader);
        GlStateManager._glBindVertexArray(group.vertexArray());
        GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, 0, group.corners(), count);
        GlStateManager._glBindVertexArray(0);
        // The game remembers which of its own it bound last; it binds its own again.
        BufferUploader.invalidate();
        TEXELS.unbind();
        shader.clear();
        type.clearRenderState();
    }

    /* The things of one model, with one paint, taken this frame: their numbers, and the batch with the next paint. */
    private static final class Batch {

        private final KitModel model;
        @Nullable
        private final ResourceLocation painted;
        private final InstanceData data = new InstanceData();
        private int count;
        @Nullable
        private Batch next;

        private Batch(final KitModel model, @Nullable final ResourceLocation painted) {
            this.model = model;
            this.painted = painted;
        }
    }
}
