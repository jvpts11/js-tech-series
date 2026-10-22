/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the moving structures' blocks with the world's own blocks, a kind at a time where the game draws that kind:
 * the solid ones with the solid land, the see-through ones with its water, so each mixes with the world as a block of
 * the ground does. The sections waiting to be built are built first each frame, within a few milliseconds. Under a
 * shader pack the blocks go with the entities instead ({@link MovingStructureRenderer}), which the pack draws as its
 * own, so the structure looks the same with or without it.
 */
final class StructureLayers {

    /* Under these mods a shader pack draws the world, and the structures are handed to it as entities are. */
    private static final boolean SHADER_PACKS = ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus");
    /* How long a frame may spend building sections, in nanoseconds. */
    private static final long BUILD_BUDGET = 3_000_000L;
    /* How far past the render distance a structure is still drawn, in blocks. */
    private static final double MARGIN = 64;

    private StructureLayers() {
    }

    /** Whether the structures go with the entities, for a shader pack to draw. */
    static boolean shaderPack() {
        return SHADER_PACKS;
    }

    /**
     * Whether a structure whose drawing fills {@code box} is near enough to {@code camera} to be drawn: within the
     * player's render distance and a margin past it, as the world's own sections are. One rule for its blocks, drawn
     * here or by a shader pack, and for its block entities, so a far fleet costs nothing however it is drawn.
     */
    static boolean withinReach(final AABB box, final Vec3 camera) {
        final double reach = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0 + MARGIN;
        return box.distanceToSqr(camera) <= reach * reach;
    }

    static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, StructureLayers::onStage);
    }

    private static void onStage(final RenderLevelStageEvent event) {
        final RenderLevelStageEvent.Stage stage = event.getStage();
        final Minecraft game = Minecraft.getInstance();
        final Vec3 camera = event.getCamera().getPosition();
        if (stage == RenderLevelStageEvent.Stage.AFTER_SKY) {
            final long deadline = System.nanoTime() + BUILD_BUDGET;
            for (final ClientStructure structure : ClientMovingStructures.all()) {
                structure.meshes().build(deadline, eye(structure, camera));
            }
            return;
        }
        final float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (stage == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS && game.level != null) {
            // The game has put the sections it built on the graphics card: the drawing changes hands now, if ready.
            StructureHandOffs.frame(game.levelRenderer, game.level.getGameTime());
        }
        if (stage == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            drawLanded(event, camera, partialTick);
            return;
        }
        final RenderType type = kindOf(stage);
        if (type == null || SHADER_PACKS) {
            return;
        }
        final Frustum frustum = event.getFrustum();
        final List<ClientStructure> drawn = new ArrayList<>(ClientMovingStructures.all());
        drawn.addAll(StructureHandOffs.landed());
        for (final ClientStructure structure : drawn) {
            final AABB box = structure.drawnBox();
            if (!structure.ready() || StructureHandOffs.hidden(structure) || !frustum.isVisible(box)
                    || !withinReach(box, camera)) {
                continue;
            }
            structure.meshes().draw(type, event.getModelViewMatrix(), event.getProjectionMatrix(), camera,
                    partialTick, eye(structure, camera));
        }
    }

    /*
     * Draws what the entity of a structure set down drew, now that it has gone, while the structure is drawn where it
     * landed: its block entities, and under a shader pack its blocks too.
     */
    private static void drawLanded(final RenderLevelStageEvent event, final Vec3 camera, final float partialTick) {
        final List<ClientStructure> landed = StructureHandOffs.landed();
        if (landed.isEmpty()) {
            return;
        }
        final MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        final PoseStack pose = event.getPoseStack();
        for (final ClientStructure structure : landed) {
            final AABB box = structure.drawnBox();
            if (!event.getFrustum().isVisible(box) || !withinReach(box, camera)) {
                continue;
            }
            final StructurePose at = structure.drawnAt(partialTick);
            pose.pushPose();
            pose.translate(at.x() - camera.x, at.y() - camera.y, at.z() - camera.z);
            MovingStructureRenderer.drawWithItsEntity(structure, at, partialTick, pose, buffers);
            pose.popPose();
        }
        buffers.endBatch();
    }

    /* Where the eye is among a structure's blocks, in the yard's places. */
    private static Vec3 eye(final ClientStructure structure, final Vec3 camera) {
        final StructureFrame frame = structure.frame();
        return new Vec3(frame.yardX(camera.x, camera.y, camera.z), frame.yardY(camera.x, camera.y, camera.z),
                frame.yardZ(camera.x, camera.y, camera.z));
    }

    private static @Nullable RenderType kindOf(final RenderLevelStageEvent.Stage stage) {
        if (stage == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return RenderType.solid();
        }
        if (stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS) {
            return RenderType.cutoutMipped();
        }
        if (stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            return RenderType.cutout();
        }
        if (stage == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return RenderType.translucent();
        }
        if (stage == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            return RenderType.tripwire();
        }
        return null;
    }
}
