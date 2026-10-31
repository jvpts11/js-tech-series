/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.landing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.space.JsSpace;
import dev.jstech.space.landing.ShipLandingBlocks;
import dev.jstech.space.landing.ShipPadBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Paints a formed ship landing pad across its plates: its yellow ring, its touchdown circle and the chevrons of the way
 * in, drawn as one picture over the whole octagon, and over it the flush lights round its edge, lit in the dark as
 * lamps are. Nothing is painted while it is not formed.
 */
public final class ShipPadRenderer implements BlockEntityRenderer<ShipPadBlockEntity> {

    /* The marks and the lights, each sixteen pixels a block, the whole pad wide. */
    private static final ResourceLocation MARKING = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "textures/block/ship_pad/marking.png");
    private static final ResourceLocation LIGHTS = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "textures/block/ship_pad/lights.png");
    /* Just above the plates' tops, the lights over the marks, so neither ever flickers with what is under it. */
    private static final float PAINT = 1.002F;
    private static final float LAMPS = 1.004F;
    private static final float MIDDLE = 0.5F;
    /* How far the pad reaches from the middle of its terminal, in blocks. */
    private static final float HALF = ShipLandingBlocks.PAD_REACH + MIDDLE;
    /* How far it is drawn from, in blocks: as far as a ship coming down is seen. */
    private static final int SEEN_FROM = 160;

    public ShipPadRenderer(final BlockEntityRendererProvider.Context context) {
        // Nothing to keep: the pictures are drawn as they are.
    }

    /** Hands the mod's bus the renderer, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerBlockEntityRenderer(ShipLandingBlocks.ENTITY.get(), ShipPadRenderer::new));
    }

    @Override
    public void render(final ShipPadBlockEntity pad, final float partialTick, final PoseStack pose,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        if (!pad.shownFormed() || pad.getLevel() == null) {
            return;
        }
        final int lit = LevelRenderer.getLightColor(pad.getLevel(), pad.getBlockPos().above());
        quad(buffers.getBuffer(RenderType.entityCutout(MARKING)), pose.last(), PAINT, lit);
        quad(buffers.getBuffer(RenderType.entityCutout(LIGHTS)), pose.last(), LAMPS, LightTexture.FULL_BRIGHT);
    }

    @Override
    public AABB getRenderBoundingBox(final ShipPadBlockEntity pad) {
        return new AABB(pad.getBlockPos()).inflate(ShipLandingBlocks.PAD_REACH, 0, ShipLandingBlocks.PAD_REACH);
    }

    @Override
    public int getViewDistance() {
        return SEEN_FROM;
    }

    /* A picture the whole pad wide, at {@code height} over the terminal's floor, lit {@code light}. */
    private static void quad(final VertexConsumer paint, final PoseStack.Pose at, final float height,
                             final int light) {
        final float lo = MIDDLE - HALF;
        final float hi = MIDDLE + HALF;
        vertex(paint, at, lo, hi, height, 0, 1, light);
        vertex(paint, at, hi, hi, height, 1, 1, light);
        vertex(paint, at, hi, lo, height, 1, 0, light);
        vertex(paint, at, lo, lo, height, 0, 0, light);
    }

    private static void vertex(final VertexConsumer paint, final PoseStack.Pose at, final float x, final float z,
                               final float y, final float u, final float v, final int light) {
        paint.addVertex(at, x, y, z).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(at, 0, 1, 0);
    }
}
