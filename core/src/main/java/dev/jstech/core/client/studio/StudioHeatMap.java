/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

/**
 * The studio's heat map in the world: each machine its last measure timed, in the dimension the player is in, glowing
 * as dear as it is, the dearest red and the cheapest yellow, while the map is on.
 */
public final class StudioHeatMap {

    private static volatile List<Place> places = List.of();
    private static volatile boolean on;

    /** How far round a block its glow reaches, and how much it shows. */
    private static final double GROW = 0.02;
    private static final float GLOW = 0.35F;

    private StudioHeatMap() {
    }

    /** Hands the game's bus the frames it draws in, once, from the Core's client. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, StudioHeatMap::onRender);
    }

    /** Shows {@code measured} from now on, each place with how dear it is of the dearest, from 0 to 1. */
    static void show(final List<Place> measured) {
        places = List.copyOf(measured);
    }

    /** Whether the map is on. */
    static boolean on() {
        return on;
    }

    /** Turns the map on or off. */
    static void on(final boolean shown) {
        on = shown;
    }

    /** The colour of a place as dear as {@code heat} of the dearest: yellow to red. */
    static int colourOf(final double heat) {
        final int green = (int) Math.round(0xFF * (1.0 - Math.max(0.0, Math.min(1.0, heat))));
        final int blue = (int) Math.round(0x50 * (1.0 - Math.max(0.0, Math.min(1.0, heat))));
        return 0xFF << 24 | 0xFF << 16 | green << 8 | blue;
    }

    private static void onRender(final RenderLevelStageEvent event) {
        if (!on || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        final String dimension = minecraft.level.dimension().location().toString();
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack pose = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final VertexConsumer fill = buffers.getBuffer(RenderType.debugFilledBox());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (final Place place : places) {
            if (!place.dimension().equals(dimension)) {
                continue;
            }
            final int colour = colourOf(place.heat());
            final AABB box = new AABB(place.pos()).inflate(GROW);
            LevelRenderer.addChainedFilledBoxVertices(pose, fill, box.minX, box.minY, box.minZ, box.maxX, box.maxY,
                    box.maxZ, (colour >> 16 & 0xFF) / 255.0F, (colour >> 8 & 0xFF) / 255.0F, (colour & 0xFF) / 255.0F,
                    GLOW);
        }
        pose.popPose();
        buffers.endBatch(RenderType.debugFilledBox());
    }

    /**
     * A place measured.
     *
     * @param dimension its dimension's id
     * @param pos       where it is
     * @param heat      how dear it is of the dearest, from 0 to 1
     */
    record Place(String dimension, BlockPos pos, double heat) {
    }
}
