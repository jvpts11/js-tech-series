/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.dimension.TravelScreens;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * Going from one dimension to another with what one rides: a player in a vehicle goes with the vehicle and everyone
 * riding it, and they arrive together at a safe place near where they were sent ({@link SafeLanding}), the land there
 * loaded before they arrive. A mod can show the passage through a screen of its own, the fire of a re-entry, instead
 * of the game's "loading terrain", or through none at all ({@link #SEAMLESS}).
 *
 * <pre>{@code
 * DimensionTravel.travel(player, mars, new BlockPos(0, 90, 0), MySpace.REENTRY_SCREEN);
 * }</pre>
 */
public final class DimensionTravel {

    /**
     * The passage shown through no screen at all: the world goes on being drawn while the land loads, for a passage
     * where both ends show only sky, as a climb to orbit does high over the ground.
     */
    public static final ResourceLocation SEAMLESS = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "seamless");
    private static final String VERSION = "1";

    private DimensionTravel() {
    }

    /**
     * Tells {@code player}'s game that its next passage to another dimension is shown through the screen declared as
     * {@code screen}, or through none for {@link #SEAMLESS}; for a passage the mod makes itself.
     */
    public static void announce(final ServerPlayer player, final ResourceLocation screen) {
        PacketDistributor.sendToPlayer(player, new TravelScreenPayload(screen));
    }

    /** Hands the mod's bus the payload this sends, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(VERSION)
                .playToClient(TravelScreenPayload.TYPE, TravelScreenPayload.STREAM_CODEC, DimensionTravel::onScreen));
    }

    /**
     * Sends {@code traveller}, with whatever it rides and everyone riding with it, to {@code to}, landing at a safe
     * place near {@code near}. The players among them see the passage through the screen declared as {@code screen}
     * on their game ({@code TravelScreens}), or the game's own when it is null.
     *
     * @return the traveller as it arrived, or null when it could not go
     */
    public static @Nullable Entity travel(final Entity traveller, final ServerLevel to, final BlockPos near,
                                          @Nullable final ResourceLocation screen) {
        final Entity root = traveller.getRootVehicle();
        if (!(root.level() instanceof ServerLevel from) || root.isRemoved()) {
            return null;
        }
        to.getChunkAt(near);
        final BlockPos landing = SafeLanding.find(to, near, stackHeight(root));
        if (screen != null && from != to) {
            for (final ServerPlayer player : players(root)) {
                announce(player, screen);
            }
        }
        final UUID id = traveller.getUUID();
        final Entity arrived = root.changeDimension(new DimensionTransition(to, Vec3.atBottomCenterOf(landing),
                Vec3.ZERO, root.getYRot(), root.getXRot(), DimensionTransition.DO_NOTHING));
        if (arrived == null || arrived.getUUID().equals(id)) {
            return arrived;
        }
        for (final Entity rider : arrived.getIndirectPassengers()) {
            if (rider.getUUID().equals(id)) {
                return rider;
            }
        }
        return null;
    }

    /* How tall the whole of what travels stands, from the bottom of what everyone rides to the top of its riders. */
    private static double stackHeight(final Entity root) {
        double top = root.getBoundingBox().maxY;
        for (final Entity rider : root.getIndirectPassengers()) {
            top = Math.max(top, rider.getBoundingBox().maxY);
        }
        return top - root.getY();
    }

    /* The players among what travels, who have a game to show the passage on. */
    private static List<ServerPlayer> players(final Entity root) {
        final List<ServerPlayer> out = new ArrayList<>();
        if (root instanceof ServerPlayer player && player.connection != null) {
            out.add(player);
        }
        for (final Entity rider : root.getIndirectPassengers()) {
            if (rider instanceof ServerPlayer player && player.connection != null) {
                out.add(player);
            }
        }
        return out;
    }

    /*
     * Runs on a client only: the payload is only ever sent to one. It is handled on the game's thread in the order it
     * came, so it is noted at once: put off to later, it would be noted after the passage it announces had opened.
     */
    private static void onScreen(final TravelScreenPayload payload, final IPayloadContext context) {
        TravelScreens.expect(payload.screen());
    }
}
