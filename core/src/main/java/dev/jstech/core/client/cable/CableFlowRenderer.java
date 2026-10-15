/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.cable;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.cable.WireFlow;
import dev.jstech.core.grid.GridKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Draws the items a tube carries moving inside it: in a line of items whose jacket has windows, what its wire carries is
 * drawn small, sliding through the block toward the face it goes, one or more at once by how full the wire is.
 * Liquids and gases are drawn with the block itself.
 */
public final class CableFlowRenderer implements BlockEntityRenderer<CableBlockEntity> {

    /* How many ticks an item takes to cross a block. */
    private static final float CROSSING = 20F;
    private static final float SCALE = 0.3F;
    private static final float MIDDLE = 0.5F;

    public CableFlowRenderer(final BlockEntityRendererProvider.Context context) {
        // Nothing to keep: the game's item renderer is asked for when drawing.
    }

    /** Hands the mod's bus the renderer, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> event.registerBlockEntityRenderer(
                CoreCables.BLOCK_ENTITY.get(), CableFlowRenderer::new));
    }

    @Override
    public void render(final CableBlockEntity cable, final float partialTick, final PoseStack pose,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        if (cable.getLevel() == null) {
            return;
        }
        for (final Wire wire : cable.wires()) {
            if (wire.type().grid() != GridKind.ITEM || wire.type().window().isEmpty()) {
                continue;
            }
            final WireFlow flow = cable.flow(wire.slot());
            final ResourceLocation id = flow.isEmpty() ? null : ResourceLocation.tryParse(flow.what());
            final Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
            if (item == Items.AIR) {
                continue;
            }
            final ItemStack stack = new ItemStack(item);
            final float time = (cable.getLevel().getGameTime() + partialTick) / CROSSING;
            final Direction toward = flow.toward();
            final int shown = Math.max(1, flow.level());
            for (int k = 0; k < shown; k++) {
                final float along = (time + (float) k / shown) % 1F - MIDDLE;
                pose.pushPose();
                pose.translate(MIDDLE + toward.getStepX() * along, MIDDLE + toward.getStepY() * along,
                        MIDDLE + toward.getStepZ() * along);
                pose.scale(SCALE, SCALE, SCALE);
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light,
                        overlay, pose, buffers, cable.getLevel(), 0);
                pose.popPose();
            }
        }
    }
}
