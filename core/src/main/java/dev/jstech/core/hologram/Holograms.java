/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hologram;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.hologram.HologramView;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Words floating in the world, facing whoever reads them: a machine's state over it, a warning over a reactor, a
 * name over a waypoint. The server puts one up with {@link #show} and the players whose games hold that chunk draw it,
 * each line turned towards them, until it is taken down, its time runs out, or they go too far.
 *
 * <p>A hologram is the server's word only; nothing of it is saved, so a mod that wants one standing after a restart
 * shows it again when its machine loads.
 *
 * <p>It reaches the players whose games hold its chunk at the moment it is sent, and no others: the server keeps no
 * record of what is standing, so a player who arrives later sees it only when the mod shows it again, as a machine
 * does on a timer or whenever its state changes.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class Holograms {

    private static final String NETWORK_VERSION = "1";

    private Holograms() {
    }

    /**
     * Puts up, or rewrites, the hologram {@code id} at {@code at} for every player near it, saying {@code lines} for
     * {@code ticks} ticks, or for as long as they are near with 0. Only the players tracking its chunk now are told;
     * show it again for those who arrive later.
     */
    public static void show(final ServerLevel level, final ResourceLocation id, final Vec3 at, final List<Text> lines,
                            final int ticks) {
        final List<Component> components = lines.stream().limit(HologramPayload.MOST_LINES)
                .<Component>map(GameText::component).toList();
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(BlockPos.containing(at)),
                new HologramPayload(id, at.x, at.y, at.z, components, Math.max(0, ticks)));
    }

    /** Takes the hologram {@code id} down for every player near {@code at}. */
    public static void hide(final ServerLevel level, final ResourceLocation id, final Vec3 at) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(BlockPos.containing(at)),
                new HologramPayload(id, at.x, at.y, at.z, List.of(), 0));
    }

    @SubscribeEvent
    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION).playToClient(HologramPayload.TYPE, HologramPayload.STREAM_CODEC,
                Holograms::onHologram);
    }

    /* Runs on a player's game: the payload is only ever sent to one. */
    private static void onHologram(final HologramPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> HologramView.accept(payload));
    }
}
