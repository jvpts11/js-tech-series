/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.IStructureTerminal;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.hands.HandGeometry;
import dev.jstech.core.multiblock.hands.HandUsePayload;
import dev.jstech.core.multiblock.hands.StructureHands;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The parts of formed structures a player works by hand, on their game: the part the crosshair is on, looked for once
 * the game has aimed each frame, outlined by its own box in place of the block's outline, and a use of it sent to the
 * server, which checks it again. A part counts as looked at when nothing else stands in front of it, the structure's
 * own blocks aside, so a door is outlined through the block of the structure that holds it; but no deeper into the
 * structure than its own thickness, so nothing is reached through its far wall.
 */
public final class HandsAim {

    /* The terminals of structures worked by hand drawn lately, so the aim looks only at those. */
    private static final Set<BlockEntity> DRAWN = Collections.newSetFromMap(new WeakHashMap<>());
    /* How far round the player a structure is looked at, in blocks; how far behind its own block a part may lie. */
    private static final double NEAR = 32;
    private static final double DEPTH = 1.5;
    private static final float OUTLINE_ALPHA = 0.4F;
    private static final int[][] EDGES = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4},
            {1, 5}, {2, 6}, {3, 7}};

    private static @Nullable Aimed aimed;

    private HandsAim() {
    }

    /** Hands the game's bus what this answers, once, from the Core's client. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderHighlightEvent.Block.class, event -> {
            if (aimed != null) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, HandsAim::outline);
        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, HandsAim::onUse);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> {
            DRAWN.clear();
            aimed = null;
        });
    }

    /** Notes that the terminal {@code terminal} of a structure worked by hand was drawn, so it may be aimed at. */
    public static void drawn(final BlockEntity terminal) {
        DRAWN.add(terminal);
    }

    /** The part the crosshair is on now, if any; for a test to read. */
    public static Optional<Aimed> aimed() {
        return Optional.ofNullable(aimed);
    }

    /** Looks for the part the crosshair is on, once the game has aimed for the frame. */
    public static void afterPick(final Minecraft minecraft, final float partialTicks) {
        aimed = null;
        final LocalPlayer player = minecraft.player;
        final ClientLevel level = minecraft.level;
        if (player == null || level == null || minecraft.screen != null || player.isSpectator()) {
            return;
        }
        final Vec3 eye = player.getEyePosition(partialTicks);
        final double reach = player.blockInteractionRange();
        final Vec3 end = eye.add(player.getViewVector(partialTicks).scale(reach));
        final HitResult game = minecraft.hitResult;
        final double gameAt = game == null || game.getType() == HitResult.Type.MISS ? reach
                : game.getLocation().distanceTo(eye);
        for (final BlockEntity entity : new ArrayList<>(DRAWN)) {
            if (entity.isRemoved() || entity.getLevel() != level || !(entity instanceof IStructureTerminal terminal)
                    || !terminal.merged() || entity.getBlockPos().distToCenterSqr(eye) > NEAR * NEAR) {
                continue;
            }
            final Optional<StructureHands> hands = terminal.hands();
            final Optional<Orientation> standing = terminal.formedOrientation();
            if (hands.isEmpty() || standing.isEmpty()) {
                continue;
            }
            final BlockPos pos = entity.getBlockPos();
            final Matrix4f placed = HandGeometry.placed(standing.get(),
                    (float) hands.get().model().model().definition().detail().scale(), new Matrix4f());
            final Matrix4f back = new Matrix4f(placed).invert();
            final HandGeometry geometry = hands.get().model().geometry();
            final float[] open = hands.get().openness(level.getGameTime() + partialTicks);
            final Optional<HandGeometry.Aim> aim = geometry.aim(back.transformPosition(from(eye, pos)),
                    back.transformPosition(from(end, pos)), open);
            if (aim.isEmpty()) {
                continue;
            }
            final double at = aim.get().along() * reach;
            final boolean ours = ownBlock(level, game, pos);
            if ((ours ? at > gameAt + DEPTH : at > gameAt) || aimed != null && at >= aimed.distance()) {
                continue;
            }
            final List<Vector3f> corners = geometry.outline(aim.get().part(), open).corners();
            final Vector3f[] outline = new Vector3f[corners.size()];
            for (int i = 0; i < outline.length; i++) {
                outline[i] = placed.transformPosition(corners.get(i));
            }
            aimed = new Aimed(pos.immutable(), aim.get().part(), at, outline);
        }
    }

    /* Draws the outline of the part aimed at, after the glass, as the game outlines a block, and hides it as well. */
    private static void outline(final RenderLevelStageEvent event) {
        final Aimed shown = aimed;
        if (shown == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || Minecraft.getInstance().options.hideGui) {
            return;
        }
        final Vec3 camera = event.getCamera().getPosition();
        final MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        final PoseStack.Pose pose = event.getPoseStack().last();
        final float x = (float) (shown.terminal().getX() - camera.x);
        final float y = (float) (shown.terminal().getY() - camera.y);
        final float z = (float) (shown.terminal().getZ() - camera.z);
        for (final int[] edge : EDGES) {
            final Vector3f from = shown.outline()[edge[0]];
            final Vector3f to = shown.outline()[edge[1]];
            final Vector3f way = new Vector3f(to).sub(from);
            if (way.lengthSquared() == 0) {
                continue;
            }
            way.normalize();
            lines.addVertex(pose, x + from.x, y + from.y, z + from.z).setColor(0, 0, 0, OUTLINE_ALPHA)
                    .setNormal(pose, way.x, way.y, way.z);
            lines.addVertex(pose, x + to.x, y + to.y, z + to.z).setColor(0, 0, 0, OUTLINE_ALPHA)
                    .setNormal(pose, way.x, way.y, way.z);
        }
        buffers.endBatch(RenderType.lines());
    }

    /* A use while a part is aimed at works the part, on the server, in place of what the game would do. */
    private static void onUse(final InputEvent.InteractionKeyMappingTriggered event) {
        final Aimed at = aimed;
        if (at == null || !event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(true);
        PacketDistributor.sendToServer(new HandUsePayload(at.terminal(), at.part()));
    }

    /* Whether what the game aimed at is a block of the structure of the terminal at {@code terminal}. */
    private static boolean ownBlock(final ClientLevel level, @Nullable final HitResult game, final BlockPos terminal) {
        if (!(game instanceof BlockHitResult block) || game.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        final BlockPos pos = block.getBlockPos();
        return pos.equals(terminal) || level.getBlockEntity(pos) instanceof FormedPartBlockEntity part
                && terminal.equals(part.terminal());
    }

    /* Where {@code at} lies from the lowest corner of the block {@code terminal}, taken apart before it is a float. */
    private static Vector3f from(final Vec3 at, final BlockPos terminal) {
        return new Vector3f((float) (at.x - terminal.getX()), (float) (at.y - terminal.getY()),
                (float) (at.z - terminal.getZ()));
    }

    /**
     * A part looked at: its structure's terminal, the part by its place among its model's, how far from the eye, in
     * blocks, and the eight corners of the box it is outlined by, in blocks from the terminal's lowest corner, the
     * first four at its lowest.
     *
     * @param terminal the structure's terminal
     * @param part     the part
     * @param distance how far from the eye
     * @param outline  its outline's corners
     */
    public record Aimed(BlockPos terminal, int part, double distance, Vector3f[] outline) {
    }
}
