/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.team.CoreTeams;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The air signals by frequency travel on, across the whole server: every loaded radio, grouped by the frequency it is
 * on. A frequency carries one value from its one transmitter to every receiver within reach of it, or of a relay of
 * its team that hears it; a second transmitter on it is a clash, and its receivers give out nothing.
 *
 * <p>Nothing is worked out on every tick: who hears whom is worked out again only when a radio on a frequency comes,
 * goes or is tuned again, and every {@link #RECHECK_EVERY} ticks for the walls built and broken meanwhile; a new value
 * is only handed to the receivers already known to hear it. Everything waits for the end of the server's tick, so a
 * frequency changed many times in one tick is worked out once, and a receiver is told only of a reception that changed.
 * Radios are on the air while their chunks are loaded. A radio aboard a moving structure is heard from where the
 * structure carries it, so one going away is heard less as the frequencies are worked out again.
 */
public final class SignalRadio {

    /** How often who hears whom is worked out again for every frequency, for the walls built meanwhile, in ticks. */
    public static final int RECHECK_EVERY = 100;
    /* Whose a radio nobody placed is. */
    private static final UUID NOBODY = new UUID(0, 0);
    /* How finely the way between two radios is looked over for walls, in pieces of a block. */
    private static final int CHECKS_A_BLOCK = 4;

    private static final Map<GlobalPos, Station> STATIONS = new HashMap<>();
    private static final Map<Frequency, Channel> CHANNELS = new LinkedHashMap<>();
    private static final Set<GlobalPos> RELAYS = new LinkedHashSet<>();
    /* The frequencies whose reach is worked out again at the end of the tick. */
    private static final Set<Frequency> RECOUNT = new LinkedHashSet<>();
    /* The frequencies whose value is handed out again at the end of the tick. */
    private static final Set<Frequency> RESEND = new LinkedHashSet<>();
    /* The radios that joined or were tuned again, whose frequency is worked out at the end of the tick. */
    private static final Set<GlobalPos> RETUNED = new LinkedHashSet<>();

    private SignalRadio() {
    }

    /** Hands the game's bus what this answers, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> flush(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> clear());
    }

    /** The radio {@code radio} at {@code pos} in {@code level} comes on the air, as its block entity loads. */
    public static void join(final ServerLevel level, final BlockPos pos, final IRadio radio) {
        final GlobalPos at = GlobalPos.of(level.dimension(), pos.immutable());
        leave(at);
        STATIONS.put(at, new Station(radio, at));
        if (radio.role() == IRadio.Role.RELAY) {
            RELAYS.add(at);
            RECOUNT.addAll(CHANNELS.keySet());
        } else {
            RETUNED.add(at);
        }
    }

    /** The radio at {@code pos} in {@code level} goes off the air, broken or unloaded. */
    public static void leave(final ServerLevel level, final BlockPos pos) {
        leave(GlobalPos.of(level.dimension(), pos));
    }

    /** The radio at {@code pos} in {@code level} was tuned again, changed hands, or changed its power or partner. */
    public static void retune(final ServerLevel level, final BlockPos pos) {
        final GlobalPos at = GlobalPos.of(level.dimension(), pos);
        final Station station = STATIONS.get(at);
        if (station == null) {
            return;
        }
        if (station.radio.role() == IRadio.Role.RELAY) {
            RECOUNT.addAll(CHANNELS.keySet());
        } else {
            RETUNED.add(at);
        }
    }

    /** The transmitter at {@code pos} in {@code level} sends a new value, handed out at the end of the tick. */
    public static void sent(final ServerLevel level, final BlockPos pos) {
        final Station station = STATIONS.get(GlobalPos.of(level.dimension(), pos));
        if (station != null && station.frequency != null) {
            RESEND.add(station.frequency);
        }
    }

    /** The frequency the radio at {@code pos} in {@code level} is on; none for a relay or a radio off the air. */
    public static Optional<Frequency> frequencyAt(final ServerLevel level, final BlockPos pos) {
        final Station station = STATIONS.get(GlobalPos.of(level.dimension(), pos));
        return station == null ? Optional.empty() : Optional.ofNullable(station.frequency);
    }

    /** Whether the transmitter at {@code pos} in {@code level} clashes with another on its frequency. */
    public static boolean clashes(final ServerLevel level, final BlockPos pos) {
        final Station station = STATIONS.get(GlobalPos.of(level.dimension(), pos));
        return station != null && station.clashing;
    }

    /** What the receiver at {@code pos} in {@code level} hears now; nothing for one off the air. */
    public static Reception reception(final ServerLevel level, final BlockPos pos) {
        final Station station = STATIONS.get(GlobalPos.of(level.dimension(), pos));
        return station == null || station.last == null ? Reception.NOTHING : station.last;
    }

    /**
     * Every frequency heard at {@code pos} in {@code level}, whoever's it is, as a signal probe shows them: by band and
     * number, each from where it is heard best; a clash once for each of its transmitters heard. With them, for a
     * {@code listener}, the frequencies of their team not heard there, with no bars.
     */
    public static List<Heard> heardAt(final ServerLevel level, final BlockPos pos, @Nullable final UUID listener) {
        final MinecraftServer server = level.getServer();
        final GlobalPos at = GlobalPos.of(level.dimension(), pos);
        final String team = listener == null ? null : CoreTeams.teamOf(server, listener);
        final List<Heard> heard = new ArrayList<>();
        for (final Map.Entry<Frequency, Channel> entry : CHANNELS.entrySet()) {
            final Channel channel = entry.getValue();
            final Map<GlobalPos, Emission> best = new LinkedHashMap<>();
            final Map<GlobalPos, Integer> bestBars = new HashMap<>();
            for (final Emission emission : channel.emissions) {
                final int bars = bars(server, emission, at);
                if (bars > bestBars.getOrDefault(emission.origin, 0)) {
                    best.put(emission.origin, emission);
                    bestBars.put(emission.origin, bars);
                }
            }
            for (final GlobalPos origin : channel.transmitters) {
                final Station sender = STATIONS.get(origin);
                final Emission emission = best.get(origin);
                if (emission != null) {
                    heard.add(new Heard(entry.getKey(), sender.radio.owner(), emission.at, origin, emission.relayed,
                            bestBars.get(origin), channel.clash ? 0 : channel.value, channel.clash));
                } else if (team != null && CoreTeams.teamOf(server, ownerOf(sender.radio)).equals(team)) {
                    heard.add(new Heard(entry.getKey(), sender.radio.owner(), origin, origin, false, 0,
                            channel.clash ? 0 : channel.value, channel.clash));
                }
            }
        }
        heard.sort(Comparator.comparing((Heard one) -> one.frequency().band().serializedName()).thenComparingInt(one ->
                one.frequency().number()).thenComparing(one -> one.frequency().whose()));
        return heard;
    }

    /* Whose a radio is on the air, nobody's when nobody placed it. */
    private static UUID ownerOf(final IRadio radio) {
        final UUID owner = radio.owner();
        return owner == null ? NOBODY : owner;
    }

    private static void leave(final GlobalPos at) {
        final Station station = STATIONS.remove(at);
        RETUNED.remove(at);
        if (station == null) {
            return;
        }
        if (RELAYS.remove(at)) {
            RECOUNT.addAll(CHANNELS.keySet());
        }
        unfile(station);
    }

    /* Takes a radio out of its frequency's channel, which is worked out again or forgotten when it is left empty. */
    private static void unfile(final Station station) {
        if (station.frequency == null) {
            return;
        }
        final Channel channel = CHANNELS.get(station.frequency);
        if (channel != null) {
            channel.transmitters.remove(station.at);
            channel.receivers.remove(station.at);
            if (channel.transmitters.isEmpty() && channel.receivers.isEmpty()) {
                CHANNELS.remove(station.frequency);
                RECOUNT.remove(station.frequency);
                RESEND.remove(station.frequency);
            } else {
                RECOUNT.add(station.frequency);
            }
        }
        station.frequency = null;
    }

    /* Puts a radio in its frequency's channel, worked out again at once. */
    private static void file(final MinecraftServer server, final Station station) {
        final Frequency frequency = station.radio.tuning().frequency(server, ownerOf(station.radio));
        if (frequency.equals(station.frequency)) {
            return;
        }
        unfile(station);
        station.frequency = frequency;
        final Channel channel = CHANNELS.computeIfAbsent(frequency, key -> new Channel());
        if (station.radio.role() == IRadio.Role.TRANSMITTER) {
            channel.transmitters.add(station.at);
        } else {
            channel.receivers.add(station.at);
        }
        RECOUNT.add(frequency);
    }

    /*
     * At the end of the server's tick: the radios that joined or were tuned go on their frequencies, every frequency
     * now and then, and those changed, are worked out again, and the new values are handed out.
     */
    private static void flush(final MinecraftServer server) {
        if (server.getTickCount() % RECHECK_EVERY == 0) {
            RETUNED.addAll(STATIONS.keySet());
            RECOUNT.addAll(CHANNELS.keySet());
        }
        if (RETUNED.isEmpty() && RECOUNT.isEmpty() && RESEND.isEmpty()) {
            return;
        }
        for (final GlobalPos at : List.copyOf(RETUNED)) {
            final Station station = STATIONS.get(at);
            if (station != null && station.radio.role() != IRadio.Role.RELAY) {
                file(server, station);
            }
        }
        RETUNED.clear();
        for (final Frequency frequency : List.copyOf(RECOUNT)) {
            final Channel channel = CHANNELS.get(frequency);
            if (channel != null) {
                recount(server, channel);
                RESEND.add(frequency);
            }
        }
        RECOUNT.clear();
        for (final Frequency frequency : List.copyOf(RESEND)) {
            final Channel channel = CHANNELS.get(frequency);
            if (channel != null) {
                resend(channel);
            }
        }
        RESEND.clear();
    }

    /* Works out again who hears a frequency: whether it clashes, where it is sent from, and each receiver's bars. */
    private static void recount(final MinecraftServer server, final Channel channel) {
        final boolean clash = channel.transmitters.size() > 1;
        channel.clash = clash;
        channel.emissions.clear();
        for (final GlobalPos at : channel.transmitters) {
            final Station transmitter = STATIONS.get(at);
            if (transmitter.clashing != clash) {
                transmitter.clashing = clash;
                transmitter.radio.clash(clash);
            }
            if (clash) {
                channel.emissions.add(new Emission(at, at, transmitter.radio.power(), false));
            }
        }
        if (channel.transmitters.size() == 1) {
            spread(server, STATIONS.get(channel.transmitters.iterator().next()), channel.emissions);
        }
        channel.bars.clear();
        for (final GlobalPos at : channel.receivers) {
            int best = 0;
            if (!clash) {
                for (final Emission emission : channel.emissions) {
                    best = Math.max(best, bars(server, emission, at));
                }
            }
            channel.bars.put(at, best);
        }
    }

    /*
     * Where a transmitter's signal is sent from: its own place, then every relay of its owner's team that hears it,
     * and that relay's partner, outward until no relay is left that hears it.
     */
    private static void spread(final MinecraftServer server, final Station transmitter,
                               final List<Emission> emissions) {
        final String team = CoreTeams.teamOf(server, ownerOf(transmitter.radio));
        final Set<GlobalPos> reached = new HashSet<>();
        final Deque<Emission> waiting = new ArrayDeque<>();
        waiting.add(new Emission(transmitter.at, transmitter.at, transmitter.radio.power(), false));
        while (!waiting.isEmpty()) {
            final Emission emission = waiting.poll();
            emissions.add(emission);
            for (final GlobalPos at : RELAYS) {
                final Station relay = STATIONS.get(at);
                if (reached.contains(at) || bars(server, emission, at) == 0
                        || !CoreTeams.teamOf(server, ownerOf(relay.radio)).equals(team)) {
                    continue;
                }
                reached.add(at);
                waiting.add(new Emission(at, transmitter.at, relay.radio.power(), true));
                final GlobalPos partner = relay.radio.partner();
                final Station other = partner == null ? null : STATIONS.get(partner);
                if (other != null && other.radio.role() == IRadio.Role.RELAY && reached.add(partner)) {
                    waiting.add(new Emission(partner, transmitter.at, other.radio.power(), true));
                }
            }
        }
    }

    /* Hands a frequency's value to each receiver that hears it, telling only those whose reception changed. */
    private static void resend(final Channel channel) {
        final Station transmitter = channel.transmitters.size() == 1
                ? STATIONS.get(channel.transmitters.iterator().next()) : null;
        channel.value = transmitter == null ? 0 : Math.max(0, Math.min(Reading.FULL, transmitter.radio.value()));
        for (final GlobalPos at : channel.receivers) {
            final Station receiver = STATIONS.get(at);
            final int bars = channel.bars.getOrDefault(at, 0);
            final Reception reception = channel.clash ? Reception.CLASH
                    : bars > 0 ? new Reception(channel.value, bars, false) : Reception.NOTHING;
            if (!reception.equals(receiver.last)) {
                receiver.last = reception;
                receiver.radio.hear(reception);
            }
        }
    }

    /* How well {@code emission} is heard at {@code at}: 0 in another dimension, out of reach or behind much wall. */
    private static int bars(final MinecraftServer server, final Emission emission, final GlobalPos at) {
        if (!emission.at.dimension().equals(at.dimension())) {
            return 0;
        }
        if (emission.at.equals(at)) {
            return RadioReach.MOST_BARS;
        }
        final ServerLevel level = server.getLevel(at.dimension());
        if (level == null) {
            return 0;
        }
        // A radio aboard a moving structure is heard from where the structure has it in the world, not its yard.
        final Vec3 from = MovingStructures.inWorld(level, Vec3.atCenterOf(emission.at.pos()));
        final Vec3 to = MovingStructures.inWorld(level, Vec3.atCenterOf(at.pos()));
        final double distance = from.distanceTo(to);
        if (!RadioReach.heard(emission.power, distance, 0)) {
            return 0;
        }
        return RadioReach.bars(emission.power, distance, walls(level, from, to, BlockPos.containing(from),
                BlockPos.containing(to)));
    }

    /*
     * The solid blocks between two radios, each counted once, the radios' own blocks and the unloaded ones left out:
     * the way is looked over a quarter of a block at a time.
     */
    private static int walls(final ServerLevel level, final Vec3 from, final Vec3 to, final BlockPos start,
                             final BlockPos end) {
        final double distance = from.distanceTo(to);
        final int steps = (int) Math.ceil(distance * CHECKS_A_BLOCK);
        final Set<BlockPos> counted = new HashSet<>();
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int walls = 0;
        for (int step = 1; step < steps; step++) {
            final Vec3 point = from.lerp(to, step / (double) steps);
            cursor.set(point.x, point.y, point.z);
            if (cursor.equals(start) || cursor.equals(end) || !level.isLoaded(cursor)
                    || !level.getBlockState(cursor).isSolidRender(level, cursor)) {
                continue;
            }
            if (counted.add(cursor.immutable())) {
                walls++;
            }
        }
        return walls;
    }

    private static void clear() {
        STATIONS.clear();
        CHANNELS.clear();
        RELAYS.clear();
        RECOUNT.clear();
        RESEND.clear();
        RETUNED.clear();
    }

    /* A radio on the air, with the frequency it was filed under and what it was last told. */
    private static final class Station {

        private final IRadio radio;
        private final GlobalPos at;
        @Nullable
        private Frequency frequency;
        @Nullable
        private Reception last;
        private boolean clashing;

        private Station(final IRadio radio, final GlobalPos at) {
            this.radio = radio;
            this.at = at;
        }
    }

    /* The radios on one frequency, where it is sent from, the value it carries and how well each receiver hears it. */
    private static final class Channel {

        private final Set<GlobalPos> transmitters = new LinkedHashSet<>();
        private final Set<GlobalPos> receivers = new LinkedHashSet<>();
        private final List<Emission> emissions = new ArrayList<>();
        private final Map<GlobalPos, Integer> bars = new HashMap<>();
        private boolean clash;
        private int value;
    }

    /* A place a frequency is sent from, with what power, for the transmitter at {@code origin}. */
    private record Emission(GlobalPos at, GlobalPos origin, double power, boolean relayed) {
    }
}
