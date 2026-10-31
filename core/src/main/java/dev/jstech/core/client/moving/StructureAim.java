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
import dev.jstech.core.client.multiblock.FormedMeshes;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.YardLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;

/**
 * The player aiming at a block of a moving structure: the game's aim hits the block through the rays that see
 * structures, with the place it was hit at in the world; the player's click must name the place on the block itself,
 * as the server checks it, so the aim's place is turned back into the yard's. And the outline round the block aimed at
 * is drawn where the block is, turned as the structure is.
 */
public final class StructureAim {

    /* The outline's colour, as the game's own: black, less than half seen. */
    private static final float OUTLINE_ALPHA = 0.4F;
    /* How far into its tick the frame whose aim is being found is drawn; none while no aim is being found. */
    private static float aiming = Float.NaN;

    private StructureAim() {
    }

    static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderHighlightEvent.Block.class, StructureAim::onHighlight);
    }

    /**
     * The game is about to find the player's aim for a frame drawn {@code partialTick} into its tick: the rays meet
     * the structures as that frame draws them ({@code IStructureBody#aimFrame}), not as they stand at its tick's end,
     * which, going, would put the crosshair on another block than the one drawn under it.
     */
    public static void beforePick(final float partialTick) {
        aiming = partialTick;
    }

    /** The game found the player's aim for the frame. */
    public static void aimed() {
        aiming = Float.NaN;
    }

    /** After the game aimed: an aim at a structure's block names the place on it in the yard's places. */
    public static void afterPick(final Minecraft game) {
        if (!(game.hitResult instanceof BlockHitResult hit) || game.level == null) {
            return;
        }
        final BlockPos block = hit.getBlockPos();
        final Vec3 at = hit.getLocation();
        if (!YardLayout.isYardBlock(block.getX()) || YardLayout.isYard(at.x)) {
            return;
        }
        final ClientStructure structure = ClientMovingStructures.byPlot(YardLayout.plotOfBlock(block.getX(),
                block.getZ()));
        if (structure == null) {
            return;
        }
        final StructureFrame frame = structure.aimFrame();
        final Vec3 onBlock = new Vec3(frame.yardX(at.x, at.y, at.z), frame.yardY(at.x, at.y, at.z),
                frame.yardZ(at.x, at.y, at.z));
        game.hitResult = new BlockHitResult(onBlock, hit.getDirection(), block, hit.isInside());
    }

    /* How far into its tick the frame whose aim is being found is drawn; not a number while none is. */
    static float aimingAt() {
        return aiming;
    }

    private static void onHighlight(final RenderHighlightEvent.Block event) {
        final BlockPos block = event.getTarget().getBlockPos();
        if (!YardLayout.isYardBlock(block.getX())) {
            return;
        }
        final Minecraft game = Minecraft.getInstance();
        final Level level = game.level;
        final ClientStructure structure = level == null ? null : ClientMovingStructures.byPlot(
                YardLayout.plotOfBlock(block.getX(), block.getZ()));
        event.setCanceled(true);
        if (structure == null || MovingStructures.at(level, block) != structure) {
            return;
        }
        final BlockState state = level.getBlockState(block);
        if (state.isAir()) {
            return;
        }
        final VoxelShape shape = state.getShape(level, block, CollisionContext.of(event.getCamera().getEntity()));
        final Vec3 camera = event.getCamera().getPosition();
        final StructurePose drawn = structure.drawnAt(event.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        final PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(drawn.x() - camera.x, drawn.y() - camera.y, drawn.z() - camera.z);
        pose.mulPose(new Quaternionf((float) drawn.qx(), (float) drawn.qy(), (float) drawn.qz(), (float) drawn.qw()));
        pose.translate((float) (block.getX() - structure.pivotX()), (float) (block.getY() - structure.pivotY()),
                (float) (block.getZ() - structure.pivotZ()));
        final PoseStack.Pose at = pose.last();
        final VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
        // A block of a formed structure aboard is outlined by its model's faces, as on the ground.
        if (FormedMeshes.outline(level, block, state, (x1, y1, z1, x2, y2, z2) -> FormedMeshes.line(lines, at,
                (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2))) {
            pose.popPose();
            return;
        }
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            final float dx = (float) (x2 - x1);
            final float dy = (float) (y2 - y1);
            final float dz = (float) (z2 - z1);
            final float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            lines.addVertex(at, (float) x1, (float) y1, (float) z1).setColor(0F, 0F, 0F, OUTLINE_ALPHA)
                    .setNormal(at, dx / length, dy / length, dz / length);
            lines.addVertex(at, (float) x2, (float) y2, (float) z2).setColor(0F, 0F, 0F, OUTLINE_ALPHA)
                    .setNormal(at, dx / length, dy / length, dz / length);
        });
        pose.popPose();
    }
}
