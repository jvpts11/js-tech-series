/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.landing;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.space.landing.ShipLandingBlocks;
import dev.jstech.space.landing.ShipPadBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Draws a formed ship landing pad whole from its terminal, as the approved raised pad has it: the round deck on its
 * truss, its touchdown ring and hazard band, the stair, the light masts, the flush lights round its rim and the
 * floodlights lit. Nothing of it is drawn while it is not formed: its blocks show.
 */
public final class ShipPadRenderer implements BlockEntityRenderer<ShipPadBlockEntity> {

    private final KitBlockEntityRenderer<ShipPadBlockEntity> pad;

    /* How far it is drawn from, in blocks: as far as a ship coming down is seen. */
    private static final int SEEN_FROM = 160;
    /* How far from its terminal the pad reaches, its stair and its masts too, in blocks. */
    private static final int PAD_REACH = 2 * ShipLandingBlocks.DECK_RADIUS;

    private ShipPadRenderer() {
        this.pad = KitBlockEntityRenderer.<ShipPadBlockEntity>formedOnly(ShipLandingBlocks.MODEL.id())
                .viewDistance(SEEN_FROM)
                .build();
    }

    /** Hands the mod's bus the renderer, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> event.registerBlockEntityRenderer(
                ShipLandingBlocks.ENTITY.get(), context -> new ShipPadRenderer()));
    }

    @Override
    public void render(final ShipPadBlockEntity terminal, final float partialTick, final PoseStack poses,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        this.pad.render(terminal, partialTick, poses, buffers, light, overlay);
    }

    @Override
    public AABB getRenderBoundingBox(final ShipPadBlockEntity terminal) {
        return new AABB(terminal.getBlockPos()).inflate(PAD_REACH);
    }

    @Override
    public int getViewDistance() {
        return SEEN_FROM;
    }

    @Override
    public boolean shouldRenderOffScreen(final ShipPadBlockEntity terminal) {
        return true;
    }
}
