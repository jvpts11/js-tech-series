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
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * Going from one dimension to another with what one rides: a player in a vehicle goes with the vehicle and everyone
 * riding it, and they arrive together at a safe place near where they were sent ({@link SafeLanding}), the land there
 * loaded before they arrive. A mod can show the passage through a screen of its own, the fire of a re-entry, instead
 * of the game's "loading terrain", or through none at all ({@link #SEAMLESS}, {@link #STILL}).
 *
 * <pre>{@code
 * DimensionTravel.travel(player, mars, new BlockPos(0, 90, 0), MySpace.REENTRY_SCREEN);
 * }</pre>
 *
 * Between two dimensions a mod groups ({@link #group}) every passage shows no screen, however it is made: through this,
 * by the mod moving the player itself, by a portal or by a command.
 */
public final class DimensionTravel {

    /**
     * The passage shown through no screen at all: the world goes on being drawn while the land loads, for a passage
     * where both ends show only sky, as a climb to orbit does high over the ground.
     */
    public static final ResourceLocation SEAMLESS = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "seamless");
    /**
     * The passage shown through no screen of its own: the player goes on seeing the last picture drawn before it, held
     * still, until the land round where they arrive is there to be seen, and then that land; never the game's "loading
     * terrain", nor a world drawn with nothing in it yet. Every passage between two dimensions of one group is shown
     * so, unless the mod making it names another screen for it.
     */
    public static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "still");
    private static final String VERSION = "1";

    private static final List<IDimensionGroups> GROUPS = new CopyOnWriteArrayList<>();
    /* When each player's game was last told how their next passage is shown, by the server's tick. */
    private static final Map<ServerPlayer, Integer> ANNOUNCED = new WeakHashMap<>();

    private DimensionTravel() {
    }

    /**
     * Tells {@code player}'s game that its next passage to another dimension is shown through the screen declared as
     * {@code screen}, or through none for {@link #SEAMLESS} and {@link #STILL}; for a passage the mod makes itself, at
     * once, so no group's passage stands in for it.
     */
    public static void announce(final ServerPlayer player, final ResourceLocation screen) {
        ANNOUNCED.put(player, player.server.getTickCount());
        PacketDistributor.sendToPlayer(player, new TravelScreenPayload(screen));
    }

    /**
     * Declares groups of dimensions: every passage between two dimensions {@code groups} puts in one group is shown
     * {@link #STILL}. Once, from the mod's constructor.
     */
    public static void group(final IDimensionGroups groups) {
        GROUPS.add(groups);
    }

    /** Whether {@code from} and {@code to} are two dimensions of one group on {@code server}. */
    public static boolean grouped(final MinecraftServer server, final ResourceKey<Level> from,
                                  final ResourceKey<Level> to) {
        if (from.equals(to)) {
            return false;
        }
        for (final IDimensionGroups groups : GROUPS) {
            final ResourceLocation group = groups.groupOf(server, from);
            if (group != null && group.equals(groups.groupOf(server, to))) {
                return true;
            }
        }
        return false;
    }

    /** Hands the buses what this answers and the payload it sends, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(VERSION)
                .playToClient(TravelScreenPayload.TYPE, TravelScreenPayload.STREAM_CODEC, DimensionTravel::onScreen));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, EntityTravelToDimensionEvent.class,
                DimensionTravel::onTravel);
    }

    /**
     * Sends {@code traveller}, with whatever it rides and everyone riding with it, to {@code to}, landing at a safe
     * place near {@code near}. The players among them see the passage through the screen declared as {@code screen}
     * on their game ({@code TravelScreens}), or, when it is null, through the game's own, or {@link #STILL} between two
     * dimensions of one group.
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

    /*
     * A player about to pass between two dimensions of one group sees it still, unless the mod sending them told their
     * game of it as it sent them. Heard last of all, and not when another listener stopped the passage, so a passage
     * that does not happen is never told.
     */
    private static void onTravel(final EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer
                || player.connection == null) {
            return;
        }
        final Integer told = ANNOUNCED.get(player);
        if (told != null && told == player.server.getTickCount()) {
            return;
        }
        if (grouped(player.server, player.level().dimension(), event.getDimension())) {
            announce(player, STILL);
        }
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
