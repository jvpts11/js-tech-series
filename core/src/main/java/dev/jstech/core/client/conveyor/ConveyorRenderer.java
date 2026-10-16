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
import dev.jstech.core.conveyor.ConveyorGeometry;
import dev.jstech.core.conveyor.Conveyors;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
    private static final double MIDDLE = 0.5;
    private static final float LIFT = 0.04F;
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
        final ConveyorGeometry line = belt.geometry();
        final int held = belt.held();
        double ahead = Double.POSITIVE_INFINITY;
        for (int i = 0; i < entries.size(); i++) {
            final BeltLane.Entry<ItemStack> entry = entries.get(i);
            double most = i == 0 ? 1.0 : ahead - gap;
            if (i == held) {
                most = Math.min(most, Math.max(entry.at(), MIDDLE));
            }
            final double at = Math.max(entry.at(), Math.min(entry.at() + Math.max(0.0, moved), most));
            ahead = at;
            final Vec3 spot = line.at(at);
            pose.pushPose();
            pose.translate(spot.x, spot.y + LIFT, spot.z);
            pose.mulPose(Axis.YP.rotationDegrees(-line.facing().toYRot()));
            pose.mulPose(Axis.XP.rotationDegrees(FLAT - line.pitch()));
            pose.scale(SCALE, SCALE, SCALE);
            Minecraft.getInstance().getItemRenderer().renderStatic(entry.item(), ItemDisplayContext.FIXED, light,
                    overlay, pose, buffers, belt.getLevel(), i);
            pose.popPose();
        }
    }

    /* A rising belt's items reach into the block ahead and above: it is drawn while any of that is in view. */
    @Override
    public AABB getRenderBoundingBox(final ConveyorBlockEntity belt) {
        return new AABB(belt.getBlockPos()).inflate(1.0);
    }
}
