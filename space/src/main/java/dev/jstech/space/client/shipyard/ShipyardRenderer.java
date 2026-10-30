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
import dev.jstech.core.client.multiblock.PlanArmsRenderer;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.space.client.ship.ShipHoles;
import dev.jstech.space.client.ship.ShipPartsRenderer;
import dev.jstech.space.rocket.CabinReadings;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.shipyard.ShipyardBlocks;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Draws a formed Small Shipyard whole from its terminal: the dock's model, as its approved drawing has it, and with it
 * the gantry crane's hook hanging over the bay and the control booth's glass, which shape none of its blocks. Nothing
 * of the dock is drawn while it is not formed: its blocks show. Formed, its robot arms at work on their rails. And the
 * ship its plan builds, each part formed into its model where the plan lays it, painted as the plan says, whether or
 * not the dock stands whole, a part just formed glowing round its box for a moment.
 */
public final class ShipyardRenderer implements BlockEntityRenderer<ShipyardTerminalBlockEntity> {

    private final KitBlockEntityRenderer<ShipyardTerminalBlockEntity> dock;

    /* What is drawn with the dock and shapes none of its blocks: the hook, then the glass, last for it is clear. */
    private static final List<ResourceLocation> EXTRAS = List.of(ShipyardBlocks.HOOK.id(), ShipyardBlocks.GLASS.id());
    /* How far it is drawn from, in blocks: as far as a dock is seen across a spaceport. */
    private static final int SEEN_FROM = 192;
    /* How far from its terminal the dock reaches, and the ship it builds, in blocks. */
    private static final int DOCK_REACH = 28;

    private ShipyardRenderer() {
        this.dock = KitBlockEntityRenderer.<ShipyardTerminalBlockEntity>formedOnly(ShipyardBlocks.MODEL.id())
                .extras(ShipyardRenderer::extras)
                .viewDistance(SEEN_FROM)
                .build();
    }

    /** Hands the mod's bus the renderer, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> event.registerBlockEntityRenderer(
                ShipyardBlocks.TERMINAL_ENTITY.get(), context -> new ShipyardRenderer()));
    }

    @Override
    public void render(final ShipyardTerminalBlockEntity terminal, final float partialTick, final PoseStack poses,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        this.dock.render(terminal, partialTick, poses, buffers, light, overlay);
        if (terminal.formed()) {
            PlanArmsRenderer.draw(terminal, terminal.arms(), ShipyardBlocks.ARM.id(), poses, buffers, overlay,
                    partialTick);
        }
        ShipHolograms.drawFormed(terminal, poses, buffers);
        final Optional<ShipDesign> plan = terminal.plan();
        final Optional<BlockPos> at = terminal.planAt();
        final List<Integer> formed = terminal.formedParts();
        if (plan.isEmpty() || at.isEmpty() || formed.isEmpty() || terminal.getLevel() == null) {
            return;
        }
        ShipPartsRenderer.drawShip(terminal, plan.get(), formed::contains, ShipHoles.of(terminal, plan.get(),
                terminal.planTurn(), terminal.holes()), at.get().subtract(terminal.getBlockPos()),
                terminal.planTurn(), CabinReadings.AT_REST, poses, buffers,
                KitBlockEntityRenderer.lightAround(terminal.getLevel(), at.get(), light), overlay);
    }

    @Override
    public int getViewDistance() {
        return SEEN_FROM;
    }

    @Override
    public boolean shouldRenderOffScreen(final ShipyardTerminalBlockEntity terminal) {
        return false;
    }

    /* The dock's box, and with a plan the box round the terminal the dock and its ship stand in. */
    @Override
    public AABB getRenderBoundingBox(final ShipyardTerminalBlockEntity terminal) {
        final AABB box = this.dock.getRenderBoundingBox(terminal);
        return terminal.plan().isEmpty() ? box : box.minmax(new AABB(terminal.getBlockPos()).inflate(DOCK_REACH));
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
