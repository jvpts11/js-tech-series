/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.shipyard;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.space.shipyard.ShipyardBlocks;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Draws a formed Small Shipyard whole from its terminal: the dock's model, as its approved drawing has it, and with it
 * the gantry crane's hook hanging over the bay and the control booth's glass, which shape none of its blocks. Nothing
 * is drawn while it is not formed: its blocks show.
 */
public final class ShipyardRenderer {

    /* What is drawn with the dock and shapes none of its blocks: the hook, then the glass, last for it is clear. */
    private static final List<ResourceLocation> EXTRAS = List.of(ShipyardBlocks.HOOK.id(), ShipyardBlocks.GLASS.id());
    /* How far it is drawn from, in blocks: as far as a dock is seen across a spaceport. */
    private static final int SEEN_FROM = 192;

    private ShipyardRenderer() {
    }

    /** Hands the mod's bus the renderer, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> event.registerBlockEntityRenderer(
                ShipyardBlocks.TERMINAL_ENTITY.get(), context -> KitBlockEntityRenderer
                        .<ShipyardTerminalBlockEntity>formedOnly(ShipyardBlocks.MODEL.id())
                        .extras(ShipyardRenderer::extras)
                        .viewDistance(SEEN_FROM)
                        .build()));
    }

    /* The hook and the glass, where the dock's model stands. */
    private static void extras(final ShipyardTerminalBlockEntity terminal, final KitInstance dock,
                               final PoseStack poses, final MultiBufferSource buffers, final int light,
                               final int overlay, final float partialTick) {
        for (final ResourceLocation id : EXTRAS) {
            final Optional<KitModel> model = KitModels.get(id);
            if (model.isEmpty()) {
                continue;
            }
            final KitInstance instance = KitAnimations.part(terminal, id.getPath(), model.get());
            KitRenderer.draw(instance, ModelDefinition.Variant.NONE, terminal, poses, buffers, light, overlay, null,
                    terminal.getLevel(), true);
        }
    }
}
