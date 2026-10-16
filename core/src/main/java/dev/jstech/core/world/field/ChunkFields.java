/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.field;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.field.ChunkFieldsClient;
import dev.jstech.core.diagnostic.Section;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.state.CoreStates;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The fields over chunks ({@link ChunkField}) the mods declare: each moved on a step every few ticks of each dimension,
 * the work done away from the server's thread, and a synced one sent to each player for the chunks round them,
 * {@link #REACH} out each way, whenever what they would see changes.
 *
 * <pre>{@code
 * public static final ChunkField FALLOUT = ChunkFields.field(rl("fallout")).spread(0.1).decay(0.02).every(40)
 *         .synced().declare();
 * FALLOUT.add(level, new ChunkPos(pos), 500);
 * float here = FALLOUT.get(level, new ChunkPos(player.blockPosition()));
 * }</pre>
 */
public final class ChunkFields {

    /** How far round a player a synced field is sent, in chunks each way. */
    public static final int REACH = 8;
    /** The steps of the fields, timed, watched and kept to their budget. */
    public static final Section SECTION = Section.builder(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "chunk_fields"), CoreLogs.WORLD).budget(Duration.ofMillis(1)).declare();

    private static final List<ChunkField> FIELDS = new CopyOnWriteArrayList<>();
    /* What each player was last sent of each field, so only a change is sent again. */
    private static final Map<UUID, Map<ResourceLocation, Integer>> SENT = new ConcurrentHashMap<>();
    private static final int SYNC_EVERY = 20;
    private static final String VERSION = "1";

    private ChunkFields() {
    }

    /** Hands the buses the steps, the sending and the payload, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(VERSION)
                .playToClient(ChunkFieldPayload.TYPE, ChunkFieldPayload.STREAM_CODEC, ChunkFields::onPayload));
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && !FIELDS.isEmpty()) {
                SECTION.run(() -> tick(level));
            }
        });
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                for (final ChunkField field : FIELDS) {
                    field.keep(level);
                    field.forget(level.dimension());
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> {
            FIELDS.forEach(field -> event.getServer().getAllLevels().forEach(level -> field.forget(
                    level.dimension())));
            SENT.clear();
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class,
                event -> SENT.remove(event.getEntity().getUUID()));
    }

    /** Starts declaring the field {@code id}. */
    public static ChunkFieldBuilder field(final ResourceLocation id) {
        return new ChunkFieldBuilder(id);
    }

    /** Every field declared. */
    public static List<ChunkField> all() {
        return List.copyOf(FIELDS);
    }

    /** The field {@code id}, or none. */
    public static Optional<ChunkField> byId(final ResourceLocation id) {
        return FIELDS.stream().filter(field -> field.id().equals(id)).findFirst();
    }

    /* Moves each field due a step on, away from the thread, and sends the synced ones to the players. */
    private static void tick(final ServerLevel level) {
        final long now = level.getGameTime();
        for (final ChunkField field : FIELDS) {
            if (field.every() > 0 && now % field.every() == 0) {
                field.stepAway(level);
            }
            if (field.synced() && now % SYNC_EVERY == 0) {
                for (final ServerPlayer player : level.players()) {
                    send(level, field, player);
                }
            }
        }
    }

    private static void send(final ServerLevel level, final ChunkField field, final ServerPlayer player) {
        final ChunkPos centre = player.chunkPosition();
        final float[] values = field.near(level, centre, REACH);
        final int seen = Objects.hash(centre.x, centre.z, Arrays.hashCode(values));
        final Map<ResourceLocation, Integer> sent = SENT.computeIfAbsent(player.getUUID(),
                key -> new ConcurrentHashMap<>());
        final Integer before = sent.put(field.id(), seen);
        if (before == null || before != seen) {
            PacketDistributor.sendToPlayer(player, new ChunkFieldPayload(field.id(), centre.x, centre.z, REACH,
                    values));
        }
    }

    /* Runs on a client only: the payload is only ever sent to one. */
    private static void onPayload(final ChunkFieldPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> ChunkFieldsClient.receive(payload));
    }

    /** A field being declared; it starts spreading a tenth a step, fading a fiftieth, every 40 ticks. */
    public static final class ChunkFieldBuilder {

        private final ResourceLocation id;
        private double spread = 0.1;
        private double decay = 0.02;
        private float least = 0.001F;
        private int every = 40;
        private boolean synced;

        private ChunkFieldBuilder(final ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /** The share of each chunk's value handed to its neighbours each step, 0 to 1. */
        public ChunkFieldBuilder spread(final double share) {
            this.spread = share;
            return this;
        }

        /** The share of everything that fades each step, 0 to 1. */
        public ChunkFieldBuilder decay(final double share) {
            this.decay = share;
            return this;
        }

        /** The least value kept; below it a chunk holds nothing. */
        public ChunkFieldBuilder least(final float value) {
            this.least = value;
            return this;
        }

        /** It moves on a step every {@code ticks}; 0 for one moved only by {@link ChunkField#stepNow}. */
        public ChunkFieldBuilder every(final int ticks) {
            this.every = ticks;
            return this;
        }

        /** It is sent to the players near enough to see it. */
        public ChunkFieldBuilder synced() {
            this.synced = true;
            return this;
        }

        /**
         * Declares the field, once, from the mod's constructor.
         *
         * @throws IllegalStateException when a field of that id is declared already, or its shares are not 0 to 1
         */
        public ChunkField declare() {
            if (byId(id).isPresent() || spread < 0 || spread > 1 || decay < 0 || decay > 1 || every < 0) {
                throw new IllegalStateException("the field " + id + " is declared once, its shares 0 to 1");
            }
            final ChunkField field = new ChunkField(id, spread, decay, least, every, synced);
            CoreStates.register(field.state());
            FIELDS.add(field);
            return field;
        }
    }
}
