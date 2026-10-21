/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inspect;

import dev.jstech.core.client.inspect.InspectClient;
import dev.jstech.core.diagnostic.Bandwidth;
import dev.jstech.core.diagnostic.TickLedger;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.ApiStatus;

/**
 * The live inspection: a player watching a place is sent, once a second, what the systems say of it and what the
 * server sent them in its last tick, which their debug screen shows ({@code /jstech inspect watch}).
 */
public final class InspectWatch {

    /** How often a place watched is read again, in ticks. */
    public static final int EVERY = 20;
    /** How many kinds of packet of the last tick are named. */
    public static final int KINDS_NAMED = 8;

    private static final String VERSION = "1";
    private static final String SEPARATOR = ", ";
    private static final String BYTES = " bytes";
    private static final Map<UUID, Watched> WATCHING = new ConcurrentHashMap<>();

    private InspectWatch() {
    }

    /** Hands the buses what this answers, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(VERSION)
                .playToClient(InspectionPayload.TYPE, InspectionPayload.STREAM_CODEC, InspectWatch::onInspection));
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            if (event.getServer().getTickCount() % EVERY == 0) {
                tick(event.getServer());
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class,
                event -> WATCHING.remove(event.getEntity().getUUID()));
    }

    /** Has {@code player} watch {@code pos} of the level they are in, and sends them what it is now. */
    public static void watch(final ServerPlayer player, final BlockPos pos) {
        final Watched watched = new Watched(player.level().dimension(), pos.immutable());
        WATCHING.put(player.getUUID(), watched);
        send(player, payloadOf(player, watched));
    }

    /** Stops {@code player} watching, and clears what their debug screen shows. */
    public static void stop(final ServerPlayer player) {
        WATCHING.remove(player.getUUID());
        send(player, InspectionPayload.stopped());
    }

    /** The place {@code player} watches, when they watch one. */
    public static Optional<BlockPos> watched(final ServerPlayer player) {
        return Optional.ofNullable(WATCHING.get(player.getUUID())).map(Watched::pos);
    }

    /** What {@code player} is sent of the place they watch now: the inspection, and their last tick's packets. */
    public static InspectionPayload payloadOf(final ServerPlayer player, final BlockPos pos) {
        return payloadOf(player, new Watched(player.level().dimension(), pos));
    }

    /** The inspection's parts as lines that travel: each system's name, and its lines under it. */
    public static List<InspectionPayload.Line> linesOf(final List<Inspectors.Part> parts) {
        final List<InspectionPayload.Line> out = new ArrayList<>();
        for (final Inspectors.Part part : parts) {
            out.add(new InspectionPayload.Line(0, GameText.component(Text.literal(part.system().id())), ""));
            for (final Inspectors.Line line : part.lines()) {
                out.add(new InspectionPayload.Line(line.depth() + 1, GameText.component(line.name()), line.value()));
            }
        }
        return out;
    }

    /** What a tick sent, as lines that travel: its count, and its kinds of packet under it, the most first. */
    public static List<InspectionPayload.Line> linesOf(final TickLedger.Tick tick) {
        final List<InspectionPayload.Line> out = new ArrayList<>();
        out.add(new InspectionPayload.Line(0, GameText.component(InspectTexts.PAYLOADS_TICK.with(tick.tick(),
                tick.packets(), tick.bytes())), ""));
        for (final TickLedger.Sent sent : tick.sent().subList(0, Math.min(KINDS_NAMED, tick.sent().size()))) {
            out.add(new InspectionPayload.Line(1, GameText.component(Text.literal(sent.kind())), sent.packets()
                    + SEPARATOR + sent.bytes() + BYTES));
        }
        return out;
    }

    private static void tick(final MinecraftServer server) {
        WATCHING.forEach((id, watched) -> {
            final ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                WATCHING.remove(id);
                return;
            }
            send(player, payloadOf(player, watched));
        });
    }

    private static InspectionPayload payloadOf(final ServerPlayer player, final Watched watched) {
        final ServerLevel level = player.server.getLevel(watched.dimension());
        final List<InspectionPayload.Line> lines = level == null || !level.isLoaded(watched.pos()) ? List.of()
                : linesOf(Inspectors.read(level, watched.pos()));
        final List<InspectionPayload.Line> sent = Bandwidth.byTick(player.getUUID())
                .flatMap(ticks -> ticks.tick(Bandwidth.tick() - 1)).map(InspectWatch::linesOf).orElse(List.of());
        return new InspectionPayload(watched.pos(), lines, sent);
    }

    private static void send(final ServerPlayer player, final InspectionPayload payload) {
        if (!(player instanceof FakePlayer) && player.connection != null
                && player.connection.hasChannel(InspectionPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /* Runs on a client only: the payload is only ever sent to one. */
    private static void onInspection(final InspectionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> InspectClient.told(payload));
    }

    /* A place watched, in its dimension. */
    private record Watched(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
