/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.conveyor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.core.conveyor.BeltLane;
import dev.jstech.core.conveyor.ConveyorBlockEntity;
import dev.jstech.core.conveyor.Conveyors;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Draws the items on a block of conveyor lying on its belt, moving along it as fast as the server last said the belt
 * moves, from where they stood when it said so, each held a gap behind the one ahead and the first at the block's end
 * until the server hands it on. A player's game moves them by itself between one word from the server and the next.
 */
public final class ConveyorRenderer implements BlockEntityRenderer<ConveyorBlockEntity> {

    private static final float SCALE = 0.4F;
    private static final float MIDDLE = 0.5F;
    private static final float LIFT = 0.04F;
    private static final float PIXELS = 16F;
    private static final float FLAT = 90F;

    public ConveyorRenderer(final BlockEntityRendererProvider.Context context) {
        // Nothing to keep: the game's item renderer is asked for when drawing.
    }

    /** Hands the mod's bus the renderer of every conveyor of every mod, once, from the Core's client. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
            for (final BlockEntityType<? extends ConveyorBlockEntity> type : Conveyors.entityTypes()) {
                event.registerBlockEntityRenderer(type, ConveyorRenderer::new);
            }
        });
    }

    @Override
    public void render(final ConveyorBlockEntity belt, final float partialTick, final PoseStack pose,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        if (belt.getLevel() == null) {
            return;
        }
        final List<BeltLane.Entry<ItemStack>> entries = belt.lane().entries();
        final double moved = belt.moving() * (belt.getLevel().getGameTime() - belt.sentAt() + partialTick);
        final double gap = belt.lane().gap();
        final Direction facing = belt.facing();
        final float height = belt.type().height() / PIXELS + LIFT;
        double ahead = Double.POSITIVE_INFINITY;
        for (int i = 0; i < entries.size(); i++) {
            final BeltLane.Entry<ItemStack> entry = entries.get(i);
            final double most = i == 0 ? 1.0 : ahead - gap;
            final double at = Math.max(entry.at(), Math.min(entry.at() + Math.max(0.0, moved), most));
            ahead = at;
            final float along = (float) at - MIDDLE;
            pose.pushPose();
            pose.translate(MIDDLE + facing.getStepX() * along, height, MIDDLE + facing.getStepZ() * along);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            pose.mulPose(Axis.XP.rotationDegrees(FLAT));
            pose.scale(SCALE, SCALE, SCALE);
            Minecraft.getInstance().getItemRenderer().renderStatic(entry.item(), ItemDisplayContext.FIXED, light,
                    overlay, pose, buffers, belt.getLevel(), i);
            pose.popPose();
        }
    }
}
