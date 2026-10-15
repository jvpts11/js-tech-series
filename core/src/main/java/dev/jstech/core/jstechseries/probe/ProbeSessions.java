/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.energy.Energies;
import dev.jstech.core.energy.Energy;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.LineSurvey;
import dev.jstech.core.mechanical.LevelMechanics;
import dev.jstech.core.mechanical.RotationMath;
import dev.jstech.core.mechanical.ShaftNetwork;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * What each player's network probe has read since it was linked, kept on the server while the probe is held and on:
 * the part of the line's flow each second of the last minute, and each block it plugs into, what it gave and took and
 * so what it is to the line; and the reading sent to the player a few times a second. Nothing here is saved: a probe
 * linked again, or put down, starts again.
 */
public final class ProbeSessions {

    /** How often a reading is sent, in ticks. */
    public static final int SEND_EVERY = 10;
    /** A second, in ticks: the flow is shown as each second's mean. */
    public static final int SECOND = 20;

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private ProbeSessions() {
    }

    /** One tick of {@code player} holding the probe {@code stack}: it reads, and now and then sends what it read. */
    public static void tick(final ServerPlayer player, final ItemStack stack) {
        final ProbeLink link = stack.get(NetworkProbe.LINK.get());
        if (link == null || !NetworkProbe.state(stack).on()) {
            SESSIONS.remove(player.getUUID());
            return;
        }
        final long now = player.serverLevel().getGameTime();
        final Session session = SESSIONS.compute(player.getUUID(), (id, known) -> known != null
                && known.link.equals(link) ? known : new Session(link));
        if (session.lastTick == now) {
            return;
        }
        session.lastTick = now;
        final ServerLevel level = player.server.getLevel(link.dimension());
        final Optional<Read> read = level == null || !level.isLoaded(link.pos()) ? Optional.empty()
                : read(level, link);
        session.add(read);
        if (now - session.lastSent >= SEND_EVERY) {
            session.lastSent = now;
            PacketDistributor.sendToPlayer(player, level == null ? ProbeReading.nothing() : session.reading(level));
        }
    }

    /** What {@code player}'s probe read last, while it is held and on; nothing otherwise. */
    public static Optional<ProbeReading> lastReading(final ServerPlayer player) {
        final Session session = SESSIONS.get(player.getUUID());
        final ServerLevel level = session == null ? null : player.server.getLevel(session.link.dimension());
        return session == null || level == null ? Optional.empty() : Optional.of(session.reading(level));
    }

    /** Forgets what {@code player}'s probe read, as they leave. */
    public static void forget(final UUID player) {
        SESSIONS.remove(player);
    }

    /**
     * What a probe linked as {@code link} reads now: the wire's cable, and its part of the line's last tick; or, linked
     * to a part that turns, its network of shafts.
     */
    public static Optional<Read> read(final ServerLevel level, final ProbeLink link) {
        if (NetworkProbe.MECHANICAL.equals(link.cable())) {
            return shafts(level, link.pos());
        }
        final Optional<CableType> type = CoreCables.REGISTRY.getOptional(link.cable());
        if (type.isEmpty() || !(level.getBlockEntity(link.pos()) instanceof CableBlockEntity cable)
                || !cable.holds(type.get())) {
            return Optional.empty();
        }
        final OptionalLong number = CoreGrids.places(level).find(cable.place(type.get()));
        if (number.isEmpty()) {
            return Optional.empty();
        }
        return CoreGrids.mover(level, type.get().grid()).flatMap(mover -> mover.survey(number.getAsLong()))
                .map(survey -> new Read(type.get(), survey));
    }

    /** What a line of {@code grid} carries, as the probe counts it. */
    public static ProbeReading.Unit unitOf(final GridKind grid) {
        if (Energies.ofGrid(grid).isPresent()) {
            return ProbeReading.Unit.ENERGY;
        }
        if (grid == GridKind.ITEM) {
            return ProbeReading.Unit.ITEMS;
        }
        return grid.policy().routesEnds() ? ProbeReading.Unit.FLUID : ProbeReading.Unit.NONE;
    }

    /*
     * The network of shafts the part at {@code pos} turns in, as a line: each part that drives a source of what it
     * gives, each that asks a consumer of what it takes, in watts, the power the sources give, and what friction takes.
     */
    private static Optional<Read> shafts(final ServerLevel level, final BlockPos pos) {
        final ShaftNetwork network = LevelMechanics.of(level).networkAt(pos);
        if (network == null) {
            return Optional.empty();
        }
        final double rpm = network.rpm();
        final List<LineSurvey.Member> members = new ArrayList<>();
        long given = 0L;
        for (final ShaftNetwork.Member member : network.members()) {
            final double own = rpm * member.ratio();
            final double watts = member.torque() == null ? 0.0
                    : RotationMath.power(member.torque().torque(own), own);
            final long rounded = Math.round(Math.abs(watts));
            members.add(new LineSurvey.Member(member.key(), watts > 0.0 ? LineSurvey.Role.SOURCE
                    : watts < 0.0 ? LineSurvey.Role.CONSUMER : LineSurvey.Role.IDLE, watts > 0.0 ? rounded : 0L,
                    watts < 0.0 ? rounded : 0L));
            given += watts > 0.0 ? rounded : 0L;
        }
        final long lost = Math.round(network.friction() * RotationMath.radians(Math.abs(rpm)));
        return Optional.of(new Read(null, new LineSurvey(members, given, LineSurvey.UNKNOWN, lost,
                network.members().size(), Optional.empty())));
    }

    /**
     * One tick's reading of a linked wire, or of a network of shafts.
     *
     * @param cable  the wire's cable; none for a network of shafts
     * @param survey its part of the line's last tick
     */
    public record Read(@Nullable CableType cable, LineSurvey survey) {
    }

    /* One player's probe since it was linked. */
    private static final class Session {

        private final ProbeLink link;
        private final Deque<Long> history = new ArrayDeque<>();
        private final Map<Long, Member> members = new LinkedHashMap<>();
        private long lastTick = Long.MIN_VALUE;
        private long lastSent = Long.MIN_VALUE / 2;
        private Read last;
        private int ticks;
        private long movedSum;
        private long wantedSum;
        private long lostSum;
        private long moved;
        private long wanted = LineSurvey.UNKNOWN;
        private long lost;
        private boolean seconds;

        private Session(final ProbeLink link) {
            this.link = link;
        }

        /* Adds a tick's reading, and at the end of each second, that second's means. */
        private void add(final Optional<Read> read) {
            this.last = read.orElse(null);
            if (read.isEmpty()) {
                return;
            }
            final LineSurvey survey = read.get().survey();
            this.movedSum += survey.moved();
            this.wantedSum = survey.wanted() == LineSurvey.UNKNOWN || this.wantedSum == LineSurvey.UNKNOWN
                    ? LineSurvey.UNKNOWN : this.wantedSum + survey.wanted();
            this.lostSum += survey.lost();
            final Set<Long> present = new HashSet<>();
            for (final LineSurvey.Member member : survey.members()) {
                present.add(member.pos());
                this.members.computeIfAbsent(member.pos(), pos -> new Member()).add(member);
            }
            this.members.keySet().retainAll(present);
            this.ticks++;
            if (this.ticks >= SECOND) {
                closeSecond();
            } else if (!this.seconds) {
                // Until the first second is over, the means so far stand for it.
                this.moved = this.movedSum / this.ticks;
                this.wanted = this.wantedSum == LineSurvey.UNKNOWN ? LineSurvey.UNKNOWN : this.wantedSum / this.ticks;
                this.lost = this.lostSum / this.ticks;
            }
        }

        private void closeSecond() {
            this.moved = this.movedSum / this.ticks;
            this.wanted = this.wantedSum == LineSurvey.UNKNOWN ? LineSurvey.UNKNOWN : this.wantedSum / this.ticks;
            this.lost = this.lostSum / this.ticks;
            this.history.addLast(this.moved);
            while (this.history.size() > ProbeReading.SECONDS) {
                this.history.removeFirst();
            }
            for (final Member member : this.members.values()) {
                member.close(this.ticks);
            }
            this.ticks = 0;
            this.movedSum = 0L;
            this.wantedSum = 0L;
            this.lostSum = 0L;
            this.seconds = true;
        }

        /* The reading as the player is sent it: the blocks grouped by what they are to the line and by kind. */
        private ProbeReading reading(final ServerLevel level) {
            final Read read = this.last;
            if (read == null) {
                return ProbeReading.nothing();
            }
            final CableType type = read.cable();
            final GridKind grid = type == null ? null : type.grid();
            final ProbeReading.Unit unit = grid == null ? ProbeReading.Unit.MECHANICAL : unitOf(grid);
            final Optional<Energy> energy = grid == null ? Optional.empty() : Energies.ofGrid(grid);
            final String lineClass = grid == null ? "" : energy.filter(found -> !found.classless())
                    .flatMap(found -> found.classOf(grid)).map(found -> found.path()).orElse("");
            final boolean rolesKnown = this.wanted != LineSurvey.UNKNOWN;
            final Map<String, GroupSum> groups = new LinkedHashMap<>();
            for (final LineSurvey.Role role : List.of(LineSurvey.Role.SOURCE, LineSurvey.Role.CONSUMER,
                    LineSurvey.Role.STORE, LineSurvey.Role.IDLE)) {
                for (final Map.Entry<Long, Member> entry : this.members.entrySet()) {
                    final Member member = entry.getValue();
                    if (member.role(rolesKnown) != role) {
                        continue;
                    }
                    final BlockPos pos = BlockPos.of(entry.getKey());
                    final ResourceLocation block = level.isLoaded(pos) ? BuiltInRegistries.BLOCK.getKey(
                            level.getBlockState(pos).getBlock()) : BuiltInRegistries.BLOCK.getDefaultKey();
                    groups.computeIfAbsent(role.serializedName() + "|" + block, key -> new GroupSum(role, block))
                            .add(entry.getKey(), member.perTick(role));
                }
            }
            final List<ProbeReading.Group> out = new ArrayList<>(groups.size());
            for (final GroupSum sum : groups.values()) {
                out.add(new ProbeReading.Group(sum.role, sum.block, sum.count, sum.perTick, sum.places));
            }
            final ResourceLocation cable = type == null || type.id() == null ? this.link.cable() : type.id();
            return new ProbeReading(true, unit, energy.map(Energy::id), lineClass, cable, read.survey().cables(),
                    this.moved, this.wanted, this.lost, read.survey().narrowest(), List.copyOf(this.history), out);
        }
    }

    /* One block the line plugs into, as the probe has seen it: what it gave and took, and so what it is. */
    private static final class Member {

        private LineSurvey.Role surveyed = LineSurvey.Role.IDLE;
        private boolean everGave;
        private boolean everTook;
        private long gaveSum;
        private long tookSum;
        private long gave;
        private long took;
        private boolean closed;
        private int ticks;

        private void add(final LineSurvey.Member member) {
            this.surveyed = member.role();
            this.everGave |= member.gave() > 0;
            this.everTook |= member.took() > 0;
            this.gaveSum += member.gave();
            this.tookSum += member.took();
            this.ticks++;
            if (!this.closed) {
                this.gave = this.gaveSum / this.ticks;
                this.took = this.tookSum / this.ticks;
            }
        }

        private void close(final int ticks) {
            this.gave = this.gaveSum / Math.max(1, ticks);
            this.took = this.tookSum / Math.max(1, ticks);
            this.gaveSum = 0L;
            this.tookSum = 0L;
            this.ticks = 0;
            this.closed = true;
        }

        /* What it is to the line: as the line says, when it knows; by what it was seen to do, otherwise. */
        private LineSurvey.Role role(final boolean known) {
            return known ? this.surveyed : LineSurvey.Role.of(this.everGave, this.everTook);
        }

        /* What it gave, or took, or for a store took less gave, a second's mean a tick. */
        private long perTick(final LineSurvey.Role role) {
            return switch (role) {
                case SOURCE -> this.gave;
                case CONSUMER -> this.took;
                case STORE -> this.took - this.gave;
                case IDLE -> 0L;
            };
        }
    }

    /* The blocks of one kind that are one thing to the line, added up. */
    private static final class GroupSum {

        private final LineSurvey.Role role;
        private final ResourceLocation block;
        private final List<Long> places = new ArrayList<>();
        private int count;
        private long perTick;

        private GroupSum(final LineSurvey.Role role, final ResourceLocation block) {
            this.role = role;
            this.block = block;
        }

        private void add(final long pos, final long amount) {
            this.count++;
            this.perTick += amount;
            if (this.places.size() < ProbeReading.MOST_PLACES) {
                this.places.add(pos);
            }
        }
    }
}
