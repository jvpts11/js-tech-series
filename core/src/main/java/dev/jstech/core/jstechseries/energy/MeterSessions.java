/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.energy.EnergyRatio;
import dev.jstech.core.energy.EnergyReadings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * What each player's clamp meter has read since it was clamped, kept on the server while the meter is held: what
 * crossed the wire in all, the most and the least in a tick; and the reading sent to the player a few times a second.
 * Nothing here is saved: a meter clamped again, or put down, starts again.
 */
public final class MeterSessions {

    /** How often a reading is sent, in ticks. */
    public static final int SEND_EVERY = 5;

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private MeterSessions() {
    }

    /** One tick of {@code player} holding the meter {@code stack}: it reads, and now and then sends what it read. */
    public static void tick(final ServerPlayer player, final ItemStack stack) {
        final MeterClamp clamp = stack.get(ClampMeter.CLAMP.get());
        if (clamp == null || ClampMeter.state(stack).dial() == MeterState.Dial.OFF) {
            SESSIONS.remove(player.getUUID());
            return;
        }
        final long now = player.serverLevel().getGameTime();
        final Session session = SESSIONS.compute(player.getUUID(), (id, known) -> known != null
                && known.clamp.equals(clamp) ? known : new Session(clamp, now));
        if (session.lastTick == now) {
            return;
        }
        session.lastTick = now;
        final ServerLevel level = player.server.getLevel(clamp.dimension());
        final MeterReading reading = level == null || !level.isLoaded(clamp.pos()) ? MeterReading.nothing()
                : read(level, session, now);
        session.last = reading;
        if (now - session.lastSent >= SEND_EVERY) {
            session.lastSent = now;
            PacketDistributor.sendToPlayer(player, reading);
        }
    }

    /** What {@code player}'s meter read last, while it is held and switched on. */
    public static Optional<MeterReading> lastReading(final UUID player) {
        final Session session = SESSIONS.get(player);
        return session == null ? Optional.empty() : Optional.ofNullable(session.last);
    }

    /** Forgets what {@code player}'s meter read, as they leave. */
    public static void forget(final UUID player) {
        SESSIONS.remove(player);
    }

    /* What the meter clamped as {@code session} says reads now, and adds it to what it read since it was clamped. */
    private static MeterReading read(final ServerLevel level, final Session session, final long now) {
        final MeterClamp clamp = session.clamp;
        if (clamp.onLine()) {
            final Optional<CableType> type = clamp.cable().flatMap(CoreCables.REGISTRY::getOptional);
            final Optional<EnergyReadings.LineReading> line = type.flatMap(found -> EnergyReadings.line(level,
                    clamp.pos(), found));
            if (line.isEmpty()) {
                return MeterReading.nothing();
            }
            final EnergyReadings.LineReading reading = line.get();
            session.add(reading.perTick());
            final EnergyRatio ratio = reading.energy().ratio();
            return new MeterReading(true, true, reading.energy().id(), reading.energy().classless() ? ""
                    : reading.lineClass().path(), BuiltInRegistries.ITEM.getKey(reading.cable().item()), ratio.units(),
                    ratio.fe(),
                    reading.perTick(), Optional.ofNullable(reading.toward()), reading.ceiling(), session.total,
                    now - session.since, session.most, session.least == Long.MAX_VALUE ? 0L : session.least, 0L, 0L,
                    List.of());
        }
        final Optional<EnergyReadings.BlockReading> block = EnergyReadings.block(level, clamp.pos());
        if (block.isEmpty()) {
            return MeterReading.nothing();
        }
        final EnergyReadings.BlockReading reading = block.get();
        final List<MeterReading.Face> faces = new ArrayList<>(reading.faces().size());
        for (final EnergyReadings.FaceReading face : reading.faces()) {
            faces.add(new MeterReading.Face(face.face(), face.kind(), face.perTick()));
        }
        final EnergyRatio ratio = reading.energy().ratio();
        return new MeterReading(true, false, reading.energy().id(), reading.portClass() == null
                || reading.energy().classless() ? "" : reading.portClass().path(),
                BuiltInRegistries.BLOCK.getKey(level.getBlockState(clamp.pos()).getBlock()), ratio.units(),
                ratio.fe(), reading.net(), Optional.empty(), 0L, 0L, now - session.since, 0L, 0L, reading.stored(),
                reading.capacity(), faces);
    }

    /* One player's meter since it was clamped. */
    private static final class Session {

        private final MeterClamp clamp;
        private final long since;
        private long lastTick = Long.MIN_VALUE;
        private long lastSent = Long.MIN_VALUE / 2;
        private long total;
        private long most;
        private long least = Long.MAX_VALUE;
        private @Nullable MeterReading last;

        private Session(final MeterClamp clamp, final long since) {
            this.clamp = clamp;
            this.since = since;
        }

        private void add(final long perTick) {
            this.total = this.total > Long.MAX_VALUE - perTick ? Long.MAX_VALUE : this.total + perTick;
            this.most = Math.max(this.most, perTick);
            this.least = Math.min(this.least, perTick);
        }
    }
}
