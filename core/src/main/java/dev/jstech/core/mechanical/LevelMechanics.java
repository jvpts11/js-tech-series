/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import dev.jstech.core.registry.CoreAttachments;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The mechanical power of one dimension: its parts joined into networks, each worked out again only when one of its
 * parts is placed, broken, loaded or changes how it meets its neighbours, and each turned once a tick as a whole, never
 * shaft by shaft. A network is told to its parts only when its speed changes, so a network at its speed costs no more
 * than the torque of its sources and loads; one with neither, standing still, costs nothing.
 */
public final class LevelMechanics {

    private final Long2ObjectOpenHashMap<Built> networkAt = new Long2ObjectOpenHashMap<>();
    private final List<Built> networks = new ArrayList<>();
    private final LongOpenHashSet changed = new LongOpenHashSet();

    /** The most parts one network takes; past them the rest are left out of it. */
    public static final int MOST_PARTS = 16_384;
    /* How far a part's speed moves, in revolutions a minute, before it is told again. */
    private static final double TELL_STEP = 0.01;
    /* The seconds in a tick. */
    private static final double TICK_SECONDS = 0.05;
    /* How far two ratios may differ and still agree. */
    private static final double AGREE = 1e-9;

    /** The mechanical power of {@code level}. */
    public static LevelMechanics of(final ServerLevel level) {
        return level.getData(CoreAttachments.MECHANICS);
    }

    /** Has the networks at and around {@code pos} of {@code level} worked out again before the next tick. */
    public static void changed(@Nullable final Level level, final BlockPos pos) {
        if (level instanceof ServerLevel server) {
            of(server).changed.add(pos.asLong());
        }
    }

    /** The network the part at {@code pos} turns in, or none. */
    public @Nullable ShaftNetwork networkAt(final BlockPos pos) {
        final Built built = this.networkAt.get(pos.asLong());
        return built == null ? null : built.network();
    }

    /** How many networks turn in it. */
    public int networks() {
        return this.networks.size();
    }

    /** Works out the changed networks again, then turns every network for a tick and tells its parts. */
    public void tick(final ServerLevel level) {
        if (!this.changed.isEmpty()) {
            rebuild(level);
        }
        for (final Built built : this.networks) {
            final ShaftNetwork network = built.network();
            if (!network.jammed() && (network.rpm() != 0.0 || !built.idle())) {
                network.step(TICK_SECONDS);
            }
            final double rpm = network.rpm();
            if (Math.abs(rpm - built.told()) > TELL_STEP || built.fresh()) {
                built.tell(rpm);
            }
        }
    }

    /*
     * Drops every network a changed place or its neighbours turned in, and builds anew from their parts and the
     * changed places themselves.
     */
    private void rebuild(final ServerLevel level) {
        final LongOpenHashSet seeds = new LongOpenHashSet();
        for (final long at : this.changed) {
            seeds.add(at);
            final BlockPos pos = BlockPos.of(at);
            drop(this.networkAt.get(at), seeds);
            for (final Direction face : Direction.values()) {
                drop(this.networkAt.get(pos.relative(face).asLong()), seeds);
            }
        }
        this.changed.clear();
        for (final long seed : seeds) {
            if (!this.networkAt.containsKey(seed)) {
                build(level, BlockPos.of(seed));
            }
        }
    }

    /* Forgets {@code built}, if any, keeping its parts as places to build from. */
    private void drop(@Nullable final Built built, final LongOpenHashSet seeds) {
        if (built == null) {
            return;
        }
        this.networks.remove(built);
        for (final ShaftNetwork.Member member : built.network().members()) {
            this.networkAt.remove(member.key());
            seeds.add(member.key());
        }
    }

    /* Joins every part reached from the part at {@code root} into one network, by the ratio each turns at. */
    private void build(final ServerLevel level, final BlockPos root) {
        if (!level.isLoaded(root)) {
            return;
        }
        final IMechanicalPart first = MechanicalPorts.at(level, root, null);
        if (first == null) {
            return;
        }
        final Long2DoubleOpenHashMap ratios = new Long2DoubleOpenHashMap();
        final List<ShaftNetwork.Member> members = new ArrayList<>();
        final List<IMechanicalPart> parts = new ArrayList<>();
        final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        boolean jammed = false;
        ratios.put(root.asLong(), 1.0);
        queue.enqueue(root.asLong());
        while (!queue.isEmpty() && members.size() < MOST_PARTS) {
            final long at = queue.dequeueLong();
            final BlockPos pos = BlockPos.of(at);
            final IMechanicalPart part = MechanicalPorts.at(level, pos, null);
            if (part == null) {
                continue;
            }
            final double ratio = ratios.get(at);
            members.add(new ShaftNetwork.Member(at, ratio, part.inertia(), part.friction(), part.torque()));
            parts.add(part);
            for (final Direction face : Direction.values()) {
                final ShaftContact contact = part.contact(face);
                if (contact.kind() == ShaftContact.Kind.NONE) {
                    continue;
                }
                final BlockPos beside = pos.relative(face);
                if (!level.isLoaded(beside)) {
                    continue;
                }
                final IMechanicalPart neighbour = MechanicalPorts.at(level, beside, face.getOpposite());
                final Double turn = neighbour == null ? null : contact.ratioTo(neighbour.contact(face.getOpposite()));
                if (turn == null) {
                    continue;
                }
                final double theirs = ratio * turn;
                final long next = beside.asLong();
                if (ratios.containsKey(next)) {
                    jammed |= Math.abs(ratios.get(next) - theirs) > AGREE * Math.max(1.0, Math.abs(theirs));
                } else {
                    ratios.put(next, theirs);
                    queue.enqueue(next);
                }
            }
        }
        final Built built = new Built(new ShaftNetwork(members, first.rpm(), jammed), parts);
        this.networks.add(built);
        for (final ShaftNetwork.Member member : members) {
            this.networkAt.put(member.key(), built);
        }
    }

    /* A network and the parts it tells its speed, by the ratio of each. */
    private static final class Built {

        private final ShaftNetwork network;
        private final List<IMechanicalPart> parts;
        private final boolean idle;
        private double told;
        private boolean fresh = true;

        private Built(final ShaftNetwork network, final List<IMechanicalPart> parts) {
            this.network = network;
            this.parts = parts;
            this.idle = network.members().stream().allMatch(member -> member.torque() == null);
        }

        private ShaftNetwork network() {
            return this.network;
        }

        /* Whether nothing in it drives or asks: with no speed, it has nothing to do. */
        private boolean idle() {
            return this.idle;
        }

        private double told() {
            return this.told;
        }

        private boolean fresh() {
            return this.fresh;
        }

        private void tell(final double rpm) {
            this.told = rpm;
            this.fresh = false;
            final List<ShaftNetwork.Member> members = this.network.members();
            for (int i = 0; i < members.size(); i++) {
                this.parts.get(i).turning(rpm * members.get(i).ratio());
            }
        }
    }
}
