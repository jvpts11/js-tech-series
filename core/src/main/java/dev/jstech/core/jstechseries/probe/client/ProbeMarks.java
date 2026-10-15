/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The blocks a network probe was asked to show in the world: each outlined in the colour of what it is to the line,
 * seen through whatever stands between, for a few seconds, so a player finds the machines of a line across a base.
 */
public final class ProbeMarks {

    /** How long the blocks stay outlined, in ticks. */
    public static final int SHOWN_FOR = 200;

    private static final List<Mark> MARKS = new ArrayList<>();
    private static final double GROW = 0.002;

    private ProbeMarks() {
    }

    /** Outlines the blocks at {@code places} in {@code colour}, in place of what was outlined before. */
    public static void show(final List<Long> places, final int colour) {
        MARKS.clear();
        final Minecraft minecraft = Minecraft.getInstance();
        final long until = minecraft.level == null ? 0L : minecraft.level.getGameTime() + SHOWN_FOR;
        for (final long place : places) {
            MARKS.add(new Mark(BlockPos.of(place), colour, until));
        }
    }

    /** The blocks outlined now, for a test to look at. */
    public static List<BlockPos> shown() {
        return MARKS.stream().map(Mark::pos).toList();
    }

    /** Hands the game's bus what this answers, once, from the Core's client. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, ProbeMarks::onRender);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> MARKS.clear());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.Clone.class, event -> MARKS.clear());
    }

    public static void onRender(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || MARKS.isEmpty()) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        final long now = minecraft.level.getGameTime();
        MARKS.removeIf(mark -> mark.until() < now);
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack pose = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final VertexConsumer lines = buffers.getBuffer(SeenThrough.LINES);
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (final Mark mark : MARKS) {
            LevelRenderer.renderLineBox(pose, lines, new AABB(mark.pos()).inflate(GROW),
                    (mark.colour() >> 16 & 0xFF) / 255F, (mark.colour() >> 8 & 0xFF) / 255F,
                    (mark.colour() & 0xFF) / 255F, 1F);
        }
        pose.popPose();
        buffers.endBatch(SeenThrough.LINES);
    }

    /** One block outlined: where, in what colour, and until when. */
    private record Mark(BlockPos pos, int colour, long until) {
    }

    /* Lines drawn over everything, as a mark seen through walls is: the game's own lines with no depth test. */
    private static final class SeenThrough extends RenderType {

        private static final RenderType LINES = create("jscore_probe_marks", DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINES, 1536, CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new LineStateShard(OptionalDouble.of(2.0)))
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .createCompositeState(false));

        /* Never made: the class only reaches the game's render states, which only a render type may. */
        private SeenThrough(final String name, final VertexFormat format, final VertexFormat.Mode mode,
                            final int bufferSize, final boolean crumbling, final boolean sort, final Runnable setup,
                            final Runnable clear) {
            super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
        }
    }
}
