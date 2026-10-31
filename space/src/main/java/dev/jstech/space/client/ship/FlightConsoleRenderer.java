/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipPartKind;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.rocket.CabinReadings;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipAboard;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipKitBlocks;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntPredicate;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Draws a small ship that has left its dock from its flight console, wherever it goes: each part formed into its
 * model, where its plan laid it from the console. The console draws nothing while its ship is still in its dock, whose
 * terminal draws it.
 */
public final class FlightConsoleRenderer implements BlockEntityRenderer<AirReserveEntity> {

    /* How far it is drawn from, in blocks: a ship is seen as far as a dock is. */
    private static final int SEEN_FROM = 192;
    /* How far from its console a small ship reaches, in blocks. */
    private static final int SHIP_REACH = 28;

    private FlightConsoleRenderer() {
    }

    /** Hands the mod's bus the renderer, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> event.registerBlockEntityRenderer(
                ShipKitBlocks.AIR_RESERVE_ENTITY.get(), context -> new FlightConsoleRenderer()));
    }

    @Override
    public void render(final AirReserveEntity console, final float partialTick, final PoseStack poses,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        final Optional<ShipAboard> ship = console.ship();
        if (ship.isEmpty() || console.getLevel() == null) {
            return;
        }
        ShipExhaust.seen(console);
        // Its gear up in flight is drawn stowed: not at all, as the approved flight draws it.
        final ShipDesign design = ship.get().design();
        final boolean gearDown = console.flight().gearDown();
        final Map<ResourceLocation, ShipPart> parts = ShipParts.all(console.getLevel());
        final IntPredicate drawn = part -> ship.get().formed().contains(part) && (gearDown || !gear(design, parts,
                part));
        ShipPartsRenderer.drawShip(console, design, drawn, ShipHoles.of(console, design, ship.get().orientation(),
                console.holes()), ship.get().nose(), ship.get().orientation(), CabinReadings.AT_REST, poses, buffers,
                light, overlay);
    }

    @Override
    public int getViewDistance() {
        return SEEN_FROM;
    }

    @Override
    public boolean shouldRenderOffScreen(final AirReserveEntity console) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(final AirReserveEntity console) {
        return new AABB(console.getBlockPos()).inflate(SHIP_REACH);
    }

    /* Whether the part numbered {@code part} of a ship of {@code design} is its gear: the spine's first, then hung. */
    private static boolean gear(final ShipDesign design, final Map<ResourceLocation, ShipPart> parts,
                                final int part) {
        final int spine = design.spine().size();
        final ResourceLocation id = part < spine ? design.spine().get(part)
                : part - spine < design.hung().size() ? design.hung().get(part - spine) : null;
        final ShipPart found = id == null ? null : parts.get(id);
        return found != null && found.kind() == ShipPartKind.GEAR;
    }
}
