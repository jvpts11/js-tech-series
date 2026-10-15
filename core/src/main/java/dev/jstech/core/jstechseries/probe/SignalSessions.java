/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe;

import dev.jstech.core.signal.Heard;
import dev.jstech.core.signal.SignalRadio;
import dev.jstech.core.team.CoreTeams;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * What each player's signal probe has read while it is held and on: the redstone on the block it is linked to, read
 * every tick for its peak since the link, and, sent a few times a second with it, the frequencies heard where the
 * player stands. Nothing here is saved: a probe linked again, or put down, starts again.
 */
public final class SignalSessions {

    /** How often a reading is sent, in ticks. */
    public static final int SEND_EVERY = 10;

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private SignalSessions() {
    }

    /** One tick of {@code player} holding the probe {@code stack}: it reads, and now and then sends what it read. */
    public static void tick(final ServerPlayer player, final ItemStack stack) {
        if (!SignalProbe.state(stack).on()) {
            SESSIONS.remove(player.getUUID());
            return;
        }
        final GlobalPos link = stack.get(SignalProbe.LINK.get());
        final long now = player.serverLevel().getGameTime();
        final Session session = SESSIONS.compute(player.getUUID(), (id, known) -> known != null
                && Objects.equals(known.link, link) ? known : new Session(link));
        if (session.lastTick == now) {
            return;
        }
        session.lastTick = now;
        session.strength = link == null ? OptionalInt.empty() : strengthAt(player.server, link);
        session.strength.ifPresent(strength -> session.peak = Math.max(session.peak, strength));
        if (now - session.lastSent >= SEND_EVERY) {
            session.lastSent = now;
            PacketDistributor.sendToPlayer(player, reading(player, session));
        }
    }

    /** What {@code player}'s probe read last, while it is held and on; nothing otherwise. */
    public static Optional<SignalReading> lastReading(final ServerPlayer player) {
        final Session session = SESSIONS.get(player.getUUID());
        return session == null ? Optional.empty() : Optional.of(reading(player, session));
    }

    /** Forgets what {@code player}'s probe read, as they leave. */
    public static void forget(final UUID player) {
        SESSIONS.remove(player);
    }

    /**
     * The redstone on the block at {@code at}: a wire's power, or for any other block the strongest it gives or is
     * fed; none while it is not loaded.
     */
    public static OptionalInt strengthAt(final MinecraftServer server, final GlobalPos at) {
        final ServerLevel level = server.getLevel(at.dimension());
        if (level == null || !level.isLoaded(at.pos())) {
            return OptionalInt.empty();
        }
        final BlockState state = level.getBlockState(at.pos());
        if (state.hasProperty(BlockStateProperties.POWER)) {
            return OptionalInt.of(state.getValue(BlockStateProperties.POWER));
        }
        int strongest = level.getBestNeighborSignal(at.pos());
        for (final Direction direction : Direction.values()) {
            strongest = Math.max(strongest, state.getSignal(level, at.pos(), direction));
        }
        return OptionalInt.of(strongest);
    }

    /* The reading as the player is sent it: the wire, and every frequency heard where they stand. */
    private static SignalReading reading(final ServerPlayer player, final Session session) {
        final ServerLevel level = player.serverLevel();
        final Vec3 standing = player.position();
        final List<SignalReading.Row> rows = new ArrayList<>();
        for (final Heard heard : SignalRadio.heardAt(level, player.blockPosition(), player.getUUID())) {
            final boolean here = heard.from().dimension().equals(level.dimension());
            final String dimension = heard.origin().dimension().equals(level.dimension()) ? ""
                    : heard.origin().dimension().location().toString();
            final double distance = here ? standing.distanceTo(Vec3.atCenterOf(heard.from().pos())) : 0.0;
            rows.add(new SignalReading.Row(heard.frequency().band(), heard.frequency().number(),
                    ownerName(player.server, heard.owner()), heard.from().pos().asLong(), dimension, distance,
                    heard.relayed(), heard.bars(), heard.value(), heard.clash()));
        }
        final boolean linked = session.link != null && session.strength.isPresent();
        return new SignalReading(linked, session.link == null ? 0L : session.link.pos().asLong(),
                session.strength.orElse(0), session.peak, rows);
    }

    private static String ownerName(final MinecraftServer server, @Nullable final UUID owner) {
        return owner == null ? "" : CoreTeams.nameOf(server, owner).orElse(owner.toString());
    }

    /* One player's probe since it was linked. */
    private static final class Session {

        @Nullable
        private final GlobalPos link;
        private long lastTick = Long.MIN_VALUE;
        private long lastSent = Long.MIN_VALUE / 2;
        private OptionalInt strength = OptionalInt.empty();
        private int peak;

        private Session(@Nullable final GlobalPos link) {
            this.link = link;
        }
    }
}
