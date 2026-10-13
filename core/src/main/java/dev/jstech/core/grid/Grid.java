/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A grid: positions joined into connected runs, each position holding a {@link GridMember}, a cable or a device that
 * passes the grid through. One grid serves every line of one kind (power, fluid, heat, gas, motion, data); cables of
 * different lines, generations or colours share it and never join, as {@link GridMember#joins} says.
 *
 * <p>What depends on the shape of the grid is worked out when the grid changes and kept until it changes again, never
 * on every tick: which positions make up each connected part, the slowest cable on the best way between two sets of
 * positions, and how long each run of one cable is, against the range its cable reaches.
 *
 * <p>Devices that join runs without standing on a position of the grid (a machine touching cables on several faces)
 * {@link #bridge} them: their runs join, and a way through the grid crosses the device.
 *
 * <p>Positions are numbers, as a block's position packs into one; this holds nothing of the game.
 */
public final class Grid {

    private final DisjointSetUnion dsu = new DisjointSetUnion();
    private final Map<Long, Integer> posToId = new HashMap<>();
    private final Map<Long, GridMember> members = new HashMap<>();
    private final Map<Long, Set<Long>> adjacency = new HashMap<>();
    /* Each connected part's positions, by its root, kept until the grid changes. */
    private final Map<Integer, Set<Long>> componentCache = new HashMap<>();
    private final Map<Long, Set<Long>> bridges = new HashMap<>();
    private final Map<Long, Set<Long>> bridgedBy = new HashMap<>();
    /* The slowest cable between two sets of positions, kept until the grid changes, a few hundred at most. */
    private final Map<Between, OptionalLong> slowestCache = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<Between, OptionalLong> eldest) {
            return size() > MOST_REMEMBERED;
        }
    };
    /* Each cable's run, and each run's length, worked out on the first ask after a change. */
    private final Map<Long, Integer> runOf = new HashMap<>();
    private final List<Integer> runLengths = new ArrayList<>();
    private boolean runsKnown;
    /* Each position's part once the runs longer than their cable reaches are taken as carrying nothing. */
    private final Map<Long, Integer> livePartOf = new HashMap<>();
    private boolean livePartsKnown;
    private long version;

    private static final int MOST_REMEMBERED = 256;

    /** Whether {@code pos} is in the grid. */
    public boolean contains(final long pos) {
        return this.posToId.containsKey(pos);
    }

    /** How many positions the grid holds. */
    public int size() {
        return this.posToId.size();
    }

    /** How many connected parts the grid is in. */
    public int componentCount() {
        return this.dsu.componentCount();
    }

    /** How many times the grid has changed; what is worked out from its shape is kept for one version. */
    public long version() {
        return this.version;
    }

    /** What stands at {@code pos}, or null when it is not in the grid. */
    public GridMember memberOf(final long pos) {
        return this.members.get(pos);
    }

    /** The root of the connected part {@code pos} is in, or -1 when it is not in the grid. */
    public int rootOf(final long pos) {
        final Integer id = this.posToId.get(pos);
        return id == null ? -1 : this.dsu.find(id);
    }

    /** Whether two positions are in one connected part. */
    public boolean connected(final long a, final long b) {
        final Integer idA = this.posToId.get(a);
        final Integer idB = this.posToId.get(b);
        return idA != null && idB != null && this.dsu.connected(idA, idB);
    }

    /** The positions {@code pos} touches and joins. */
    public Set<Long> neighbours(final long pos) {
        return Collections.unmodifiableSet(this.adjacency.getOrDefault(pos, Set.of()));
    }

    /** Every position of the grid. */
    public Set<Long> positions() {
        return Collections.unmodifiableSet(this.posToId.keySet());
    }

    /** Every position in the connected part {@code pos} is in. */
    public Set<Long> componentPositions(final long pos) {
        final Integer id = this.posToId.get(pos);
        if (id == null) {
            return Set.of();
        }
        final int root = this.dsu.find(id);
        Set<Long> cached = this.componentCache.get(root);
        if (cached == null) {
            final Set<Long> found = new LinkedHashSet<>();
            for (final Map.Entry<Long, Integer> entry : this.posToId.entrySet()) {
                if (this.dsu.find(entry.getValue()) == root) {
                    found.add(entry.getKey());
                }
            }
            cached = Collections.unmodifiableSet(found);
            this.componentCache.put(root, cached);
        }
        return cached;
    }

    /**
     * Puts {@code member} at {@code pos}, joined to each of {@code candidates} already in the grid that it joins.
     *
     * @return the roots the joined neighbours' parts had before they became one, each once
     */
    public Set<Integer> place(final long pos, final GridMember member, final Collection<Long> candidates) {
        if (this.posToId.containsKey(pos)) {
            throw new IllegalStateException("a position already in the grid: " + pos);
        }
        changed();
        final int id = this.dsu.makeSet();
        this.posToId.put(pos, id);
        this.members.put(pos, Objects.requireNonNull(member, "member"));
        final Set<Long> edges = new LinkedHashSet<>();
        this.adjacency.put(pos, edges);
        final Set<Integer> joinedRoots = new LinkedHashSet<>();
        for (final long neighbour : candidates) {
            final Integer neighbourId = this.posToId.get(neighbour);
            if (neighbourId == null || neighbour == pos || !member.joins(this.members.get(neighbour))) {
                continue;
            }
            edges.add(neighbour);
            this.adjacency.get(neighbour).add(pos);
            joinedRoots.add(this.dsu.find(neighbourId));
        }
        for (final long neighbour : edges) {
            this.dsu.union(id, this.posToId.get(neighbour));
        }
        return joinedRoots;
    }

    /**
     * Takes {@code pos} out; the connected part it was in may fall apart, and the parts are worked out again.
     *
     * @return the positions of the part it was in, and how many parts they are in now
     */
    public Removal remove(final long pos) {
        if (!this.posToId.containsKey(pos)) {
            throw new IllegalStateException("a position not in the grid: " + pos);
        }
        final Set<Long> affected = collectComponent(pos);
        changed();
        for (final long neighbour : this.adjacency.getOrDefault(pos, Set.of())) {
            final Set<Long> edges = this.adjacency.get(neighbour);
            if (edges != null) {
                edges.remove(pos);
            }
        }
        this.adjacency.remove(pos);
        this.members.remove(pos);
        final Set<Long> survivors = new LinkedHashSet<>(this.posToId.keySet());
        survivors.remove(pos);
        rebuild(survivors);
        final Set<Integer> fragments = new HashSet<>();
        for (final long each : affected) {
            final Integer id = this.posToId.get(each);
            if (id != null) {
                fragments.add(this.dsu.find(id));
            }
        }
        return new Removal(Collections.unmodifiableSet(affected), fragments.size());
    }

    /**
     * Joins the runs a device at {@code device}, which is no position of the grid, touches, and remembers that it does
     * so a way through the grid crosses it. Reporting the same positions again changes nothing.
     *
     * @return whether two parts became one
     */
    public boolean bridge(final long device, final Collection<Long> positions) {
        final Set<Long> touched = Set.copyOf(positions);
        if (!touched.equals(this.bridges.get(device))) {
            forgetBridge(device);
            if (!touched.isEmpty()) {
                changed();
                this.bridges.put(device, touched);
                for (final long pos : touched) {
                    this.bridgedBy.computeIfAbsent(pos, key -> new HashSet<>()).add(device);
                }
            }
        }
        final List<Integer> ids = new ArrayList<>();
        for (final long pos : positions) {
            final Integer id = this.posToId.get(pos);
            if (id != null) {
                ids.add(id);
            }
        }
        boolean merged = false;
        for (int k = 1; k < ids.size(); k++) {
            merged |= this.dsu.union(ids.get(0), ids.get(k));
        }
        if (merged) {
            changed();
        }
        return merged;
    }

    /** The positions the device at {@code device} last said it touches. */
    public Set<Long> bridgedBy(final long device) {
        final Set<Long> touched = this.bridges.get(device);
        return touched == null ? Set.of() : touched;
    }

    /** The device at {@code device} is gone: it joins nothing any more on a way through the grid. */
    public void forgetBridge(final long device) {
        final Set<Long> touched = this.bridges.remove(device);
        if (touched == null) {
            return;
        }
        changed();
        for (final long pos : touched) {
            final Set<Long> devices = this.bridgedBy.get(pos);
            if (devices != null) {
                devices.remove(device);
                if (devices.isEmpty()) {
                    this.bridgedBy.remove(pos);
                }
            }
        }
        // The union-find cannot split a join, so the parts are worked out again from the cables and the devices left.
        rebuild(new LinkedHashSet<>(this.posToId.keySet()));
    }

    /** Every position reached from {@code start} along the grid without stepping on any of {@code blocked}. */
    public Set<Long> reachableFrom(final long start, final Set<Long> blocked) {
        final Set<Long> visited = new LinkedHashSet<>();
        if (blocked.contains(start) || !this.posToId.containsKey(start)) {
            return visited;
        }
        final Deque<Long> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            final long current = queue.poll();
            for (final long neighbour : this.adjacency.getOrDefault(current, Set.of())) {
                if (!blocked.contains(neighbour) && visited.add(neighbour)) {
                    queue.add(neighbour);
                }
            }
        }
        return visited;
    }

    /**
     * The position of the slowest cable on the best way from any of {@code from} to any of {@code to}: of every way,
     * the one whose slowest cable carries the most, the way traffic takes the best route it has. Devices limit
     * nothing; through a bridging device the way goes from any position it touches to any other. Worked out once a
     * version of the grid for each pair asked.
     *
     * @return the slowest cable's position, or empty when nothing joins them or only devices lie between
     */
    public OptionalLong slowestBetween(final Collection<Long> from, final Collection<Long> to) {
        final Between key = new Between(Set.copyOf(from), Set.copyOf(to));
        final OptionalLong known = this.slowestCache.get(key);
        if (known != null) {
            return known;
        }
        final OptionalLong found = searchSlowest(key.from(), key.to());
        this.slowestCache.put(key, found);
        return found;
    }

    /** How many cables the run {@code pos} is on has, its own line, generation and colour unbroken; 0 for a device. */
    public int runLength(final long pos) {
        knowRuns();
        final Integer run = this.runOf.get(pos);
        return run == null ? 0 : this.runLengths.get(run);
    }

    /** Whether the run {@code pos} is on is longer than its cable reaches, so it carries nothing across. */
    public boolean runTooLong(final long pos) {
        final GridMember member = this.members.get(pos);
        return member != null && member.range() > 0 && runLength(pos) > member.range();
    }

    /**
     * The part {@code pos} is in when every run longer than its cable reaches carries nothing: what is joined to it
     * along cables and through bridging devices without crossing such a run. Two positions in one part reach each
     * other; a position on a run too long is in no part.
     *
     * @return the part's number, or -1 for a position on a run too long, or not in the grid
     */
    public int livePartOf(final long pos) {
        knowLiveParts();
        return this.livePartOf.getOrDefault(pos, -1);
    }

    /** Forgets everything. */
    public void clear() {
        changed();
        this.dsu.clear();
        this.posToId.clear();
        this.members.clear();
        this.adjacency.clear();
        this.bridges.clear();
        this.bridgedBy.clear();
    }

    /* The grid changed: what was worked out from its shape no longer holds. */
    private void changed() {
        this.version++;
        this.componentCache.clear();
        this.slowestCache.clear();
        this.runsKnown = false;
        this.livePartsKnown = false;
    }

    private void rebuild(final Set<Long> survivors) {
        this.dsu.clear();
        this.posToId.clear();
        for (final long pos : survivors) {
            this.posToId.put(pos, this.dsu.makeSet());
        }
        for (final long pos : survivors) {
            for (final long neighbour : this.adjacency.getOrDefault(pos, Set.of())) {
                final Integer neighbourId = this.posToId.get(neighbour);
                if (neighbourId != null) {
                    this.dsu.union(this.posToId.get(pos), neighbourId);
                }
            }
        }
        // The devices still standing keep their joins; only the ones that are gone stop joining.
        for (final Set<Long> touched : this.bridges.values()) {
            Integer first = null;
            for (final long pos : touched) {
                final Integer id = this.posToId.get(pos);
                if (id == null) {
                    continue;
                }
                if (first == null) {
                    first = id;
                } else {
                    this.dsu.union(first, id);
                }
            }
        }
    }

    /* The positions joined to {@code start} by cables alone. */
    private Set<Long> collectComponent(final long start) {
        final Set<Long> visited = new LinkedHashSet<>();
        final Deque<Long> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            final long current = queue.poll();
            for (final long neighbour : this.adjacency.getOrDefault(current, Set.of())) {
                if (visited.add(neighbour)) {
                    queue.add(neighbour);
                }
            }
        }
        return visited;
    }

    /* The positions a way through the grid steps to from {@code pos}: its neighbours, and across its devices. */
    private Set<Long> neighboursOf(final long pos) {
        final Set<Long> devices = this.bridgedBy.get(pos);
        if (devices == null) {
            return this.adjacency.getOrDefault(pos, Set.of());
        }
        final Set<Long> out = new HashSet<>(this.adjacency.getOrDefault(pos, Set.of()));
        for (final long device : devices) {
            out.addAll(this.bridges.getOrDefault(device, Set.of()));
        }
        out.remove(pos);
        return out;
    }

    private OptionalLong searchSlowest(final Set<Long> from, final Set<Long> to) {
        final Set<Long> goals = new HashSet<>();
        for (final long pos : to) {
            if (this.posToId.containsKey(pos)) {
                goals.add(pos);
            }
        }
        if (goals.isEmpty()) {
            return OptionalLong.empty();
        }
        /*
         * A widest-path search: each position is first taken off the queue along the way whose slowest cable carries
         * the most, so the first goal taken off is reached as fast as it can be.
         */
        final Map<Long, Long> best = new HashMap<>();
        final Map<Long, Long> slowest = new HashMap<>();
        final PriorityQueue<Reach> queue = new PriorityQueue<>(Comparator.comparingLong(Reach::throughput).reversed());
        for (final long pos : from) {
            if (this.posToId.containsKey(pos)) {
                offer(pos, null, best, slowest, queue);
            }
        }
        final Set<Long> done = new HashSet<>();
        while (!queue.isEmpty()) {
            final Reach reach = queue.poll();
            if (!done.add(reach.pos())) {
                continue;
            }
            if (goals.contains(reach.pos())) {
                final Long at = slowest.get(reach.pos());
                return at == null ? OptionalLong.empty() : OptionalLong.of(at);
            }
            final Long sofar = slowest.get(reach.pos());
            for (final long next : neighboursOf(reach.pos())) {
                if (!done.contains(next) && this.posToId.containsKey(next)) {
                    offer(next, sofar, best, slowest, queue);
                }
            }
        }
        return OptionalLong.empty();
    }

    /* Offers {@code pos} to the search, reached along a way whose slowest cable so far stands at {@code before}. */
    private void offer(final long pos, final Long before, final Map<Long, Long> best, final Map<Long, Long> slowest,
                       final PriorityQueue<Reach> queue) {
        final long here = throughputAt(pos);
        final long sofar = before == null ? Long.MAX_VALUE : throughputAt(before);
        final Long limit = here < sofar ? Long.valueOf(pos) : before;
        final long throughput = Math.min(here, sofar);
        if (throughput > best.getOrDefault(pos, -1L)) {
            best.put(pos, throughput);
            if (limit == null) {
                slowest.remove(pos);
            } else {
                slowest.put(pos, limit);
            }
            queue.add(new Reach(pos, throughput));
        }
    }

    private long throughputAt(final long pos) {
        final GridMember member = this.members.get(pos);
        return member == null || member.isDevice() ? Long.MAX_VALUE : member.throughput();
    }

    /* Works out every cable's run: the cables joined to it of its own line, generation and colour, unbroken. */
    private void knowRuns() {
        if (this.runsKnown) {
            return;
        }
        this.runOf.clear();
        this.runLengths.clear();
        for (final Map.Entry<Long, GridMember> entry : this.members.entrySet()) {
            final long start = entry.getKey();
            final GridMember member = entry.getValue();
            if (member.isDevice() || this.runOf.containsKey(start)) {
                continue;
            }
            final int run = this.runLengths.size();
            int length = 0;
            final Deque<Long> queue = new ArrayDeque<>();
            this.runOf.put(start, run);
            queue.add(start);
            while (!queue.isEmpty()) {
                final long current = queue.poll();
                length++;
                for (final long neighbour : this.adjacency.getOrDefault(current, Set.of())) {
                    if (!this.runOf.containsKey(neighbour) && member.sameRun(this.members.get(neighbour))) {
                        this.runOf.put(neighbour, run);
                        queue.add(neighbour);
                    }
                }
            }
            this.runLengths.add(length);
        }
        this.runsKnown = true;
    }

    /* Works out each position's live part once a version: a walk through the grid, the runs too long left out. */
    private void knowLiveParts() {
        if (this.livePartsKnown) {
            return;
        }
        this.livePartOf.clear();
        int next = 0;
        final Deque<Long> queue = new ArrayDeque<>();
        for (final long start : this.posToId.keySet()) {
            if (this.livePartOf.containsKey(start) || runTooLong(start)) {
                continue;
            }
            final int part = next++;
            this.livePartOf.put(start, part);
            queue.add(start);
            while (!queue.isEmpty()) {
                for (final long neighbour : neighboursOf(queue.poll())) {
                    if (!this.livePartOf.containsKey(neighbour) && this.posToId.containsKey(neighbour)
                            && !runTooLong(neighbour)) {
                        this.livePartOf.put(neighbour, part);
                        queue.add(neighbour);
                    }
                }
            }
        }
        this.livePartsKnown = true;
    }

    /**
     * What taking a position out did.
     *
     * @param affected  the positions of the part it was in, itself included
     * @param fragments how many parts those that are left are in now
     */
    public record Removal(Set<Long> affected, int fragments) {
    }

    /** Two sets of positions a way is asked between. */
    private record Between(Set<Long> from, Set<Long> to) {
    }

    /** A position the search has reached, and how much the best way to it carries. */
    private record Reach(long pos, long throughput) {
    }
}
