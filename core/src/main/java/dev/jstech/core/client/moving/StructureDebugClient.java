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
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.moving.StructureForcesPayload;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

/**
 * The physics of the moving structures near made visible in the world, while the player turns it on with its key:
 * the box each one fills, its centre of mass, and each force on it as a line from where it acts, coloured as the
 * diagram of forces colours it, with their sum from the centre in white. The forces are those the server tells, as
 * it tells the diagram, for every structure within reach.
 */
final class StructureDebugClient {

    /* The structures looked at for the overlay, by their entity. */
    private static final IntSet WATCHED = new IntOpenHashSet();
    /* How near a structure is looked at, in blocks, and how often the near ones are found again, in ticks. */
    private static final double REACH = 64;
    private static final int EVERY = 20;
    /* The longest line of a force, in blocks, the shortest as a share of it, and the half side of the centre. */
    private static final double LONGEST = 4;
    private static final double SHORTEST = 0.15;
    private static final double MARK = 0.3;
    private static final double HEAD = 0.25;

    private static boolean shown;
    private static int ticks;

    private StructureDebugClient() {
    }

    /** Hands the key and the game's bus what this answers, once, from the Core's client. */
    static void listen() {
        KeyActionsClient.onPress(CoreKeys.SHOW_STRUCTURE_DEBUG, StructureDebugClient::toggle);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, StructureDebugClient::render);
    }

    /** Whether the overlay is on. */
    static boolean shown() {
        return shown;
    }

    /* Turns the overlay on, looking at the structures near, or off, letting them go. */
    private static void toggle() {
        shown = !shown;
        if (shown) {
            near();
        } else {
            WATCHED.forEach(StructureForcesClient::stopLooking);
            WATCHED.clear();
        }
    }

    private static void tick() {
        if (shown && ++ticks % EVERY == 0) {
            near();
        }
    }

    /* Looks at the structures now within reach and lets go of those gone out of it. */
    private static void near() {
        final Minecraft game = Minecraft.getInstance();
        if (game.player == null) {
            return;
        }
        final IntSet now = new IntOpenHashSet();
        for (final ClientStructure structure : ClientMovingStructures.all()) {
            if (structure.level() == game.player.level()
                    && structure.worldBox().distanceToSqr(game.player.position()) <= REACH * REACH) {
                now.add(structure.entity().getId());
            }
        }
        for (final int gone : new IntOpenHashSet(WATCHED)) {
            if (!now.contains(gone)) {
                StructureForcesClient.stopLooking(gone);
                WATCHED.remove(gone);
            }
        }
        for (final int added : now) {
            if (WATCHED.add(added)) {
                StructureForcesClient.look(added);
            }
        }
    }

    private static void render(final RenderLevelStageEvent event) {
        if (!shown || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        final Minecraft game = Minecraft.getInstance();
        final ForceDiagramPalette.Colours colours = ForceDiagramPalette.get();
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack pose = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = game.renderBuffers().bufferSource();
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        final Matrix4f matrix = pose.last().pose();
        for (final int entity : WATCHED) {
            final ClientStructure structure = ClientMovingStructures.byEntity(entity);
            if (structure == null) {
                continue;
            }
            final AABB box = structure.worldBox();
            LevelRenderer.renderLineBox(pose, lines, box, red(colours.chosen()), green(colours.chosen()),
                    blue(colours.chosen()), 1F);
            final StructureForcesPayload told = StructureForcesClient.of(entity);
            if (told == null) {
                continue;
            }
            final Vec3 sum = told.sum();
            double most = sum.length();
            for (final StructureForcesPayload.Force force : told.forces()) {
                most = Math.max(most, force.push().length());
            }
            for (final StructureForcesPayload.Force force : told.forces()) {
                arrow(lines, pose, matrix, force.at(), force.push(), most, colours.of(force.kind()));
            }
            arrow(lines, pose, matrix, told.centre(), sum, most, colours.sum());
            final Vec3 c = told.centre();
            line(lines, pose, matrix, c.add(-MARK, 0, 0), c.add(MARK, 0, 0), colours.sum());
            line(lines, pose, matrix, c.add(0, -MARK, 0), c.add(0, MARK, 0), colours.sum());
            line(lines, pose, matrix, c.add(0, 0, -MARK), c.add(0, 0, MARK), colours.sum());
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    /* A force as a line from where it acts, as long as it weighs against the strongest, with a head. */
    private static void arrow(final VertexConsumer lines, final PoseStack pose, final Matrix4f matrix,
                              final Vec3 from, final Vec3 push, final double most, final int colour) {
        final double strength = push.length();
        if (strength <= 0 || most <= 0) {
            return;
        }
        final Vec3 way = push.scale(1 / strength);
        final Vec3 to = from.add(way.scale(LONGEST * (SHORTEST + (1 - SHORTEST) * Math.sqrt(strength / most))));
        line(lines, pose, matrix, from, to, colour);
        Vec3 side = way.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : side.normalize();
        final Vec3 back = to.subtract(way.scale(HEAD));
        line(lines, pose, matrix, to, back.add(side.scale(HEAD * 0.6)), colour);
        line(lines, pose, matrix, to, back.subtract(side.scale(HEAD * 0.6)), colour);
    }

    private static void line(final VertexConsumer lines, final PoseStack pose, final Matrix4f matrix,
                             final Vec3 from, final Vec3 to, final int colour) {
        final Vec3 along = to.subtract(from);
        final Vec3 way = along.lengthSqr() < 1.0e-12 ? new Vec3(0, 1, 0) : along.normalize();
        lines.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z).setColor(colour)
                .setNormal(pose.last(), (float) way.x, (float) way.y, (float) way.z);
        lines.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z).setColor(colour)
                .setNormal(pose.last(), (float) way.x, (float) way.y, (float) way.z);
    }

    private static float red(final int colour) {
        return (colour >> 16 & 0xFF) / 255F;
    }

    private static float green(final int colour) {
        return (colour >> 8 & 0xFF) / 255F;
    }

    private static float blue(final int colour) {
        return (colour & 0xFF) / 255F;
    }
}
