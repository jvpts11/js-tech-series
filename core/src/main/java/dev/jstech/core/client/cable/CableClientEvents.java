/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.cable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableBlock;
import dev.jstech.core.cable.CableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * What a player's game does with a cable block: outline the one wire or part looked at rather than the whole block, and
 * not take the whole block away the moment it is struck when the server will take only that piece.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class CableClientEvents {

    private static final float OUTLINE_ALPHA = 0.4F;
    private static final float SHORTEST_EDGE = 1.0e-5F;

    private CableClientEvents() {
    }

    @SubscribeEvent
    public static void onLeftClick(final PlayerInteractEvent.LeftClickBlock event) {
        final Level level = event.getLevel();
        /*
         * Only a creative player's game breaks a block the moment it is struck; there it is told not to, and the
         * server's break takes the one piece. A survival player mines the block as any other, or the server would
         * never hear the break finish, and the piece taken out is drawn back the moment the server says so.
         */
        if (!level.isClientSide() || !event.getEntity().getAbilities().instabuild) {
            return;
        }
        if (!CableBlock.strikeAimOf(level, event.getPos(), event.getEntity()).isNothing()) {
            event.setCanceled(true);
            event.setUseBlock(TriState.FALSE);
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onBlockHighlight(final RenderHighlightEvent.Block event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        final BlockPos pos = event.getTarget().getBlockPos();
        final CableBlockEntity.Aim aim = CableBlock.aimOf(minecraft.level, pos, minecraft.player);
        if (aim.isNothing()) {
            return;
        }
        final Vec3 camera = event.getCamera().getPosition();
        drawOutline(event.getPoseStack(), event.getMultiBufferSource().getBuffer(RenderType.lines()), aim.outline(),
                pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        event.setCanceled(true);
    }

    private static void drawOutline(final PoseStack poseStack, final VertexConsumer consumer, final VoxelShape shape,
                                    final double ox, final double oy, final double oz) {
        final PoseStack.Pose pose = poseStack.last();
        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            float nx = (float) (x2 - x1);
            float ny = (float) (y2 - y1);
            float nz = (float) (z2 - z1);
            final float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            if (length < SHORTEST_EDGE) {
                return;
            }
            nx /= length;
            ny /= length;
            nz /= length;
            consumer.addVertex(pose, (float) (x1 + ox), (float) (y1 + oy), (float) (z1 + oz))
                    .setColor(0.0F, 0.0F, 0.0F, OUTLINE_ALPHA).setNormal(pose, nx, ny, nz);
            consumer.addVertex(pose, (float) (x2 + ox), (float) (y2 + oy), (float) (z2 + oz))
                    .setColor(0.0F, 0.0F, 0.0F, OUTLINE_ALPHA).setNormal(pose, nx, ny, nz);
        });
    }
}
