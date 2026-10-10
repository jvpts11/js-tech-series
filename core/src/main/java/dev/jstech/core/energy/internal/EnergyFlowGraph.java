/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy.internal;

import dev.jstech.core.energy.EnergyDistributionResult;
import dev.jstech.core.energy.EnergyLoss;
import dev.jstech.core.energy.EnergyNodeRole;
import dev.jstech.core.energy.IEnergyCable;
import dev.jstech.core.energy.IEnergyNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * One energy network's shape, taken once, and the energy it moves in a tick. The way from each supplier to each
 * receiver is the one that loses least, and of those the one through fewest cables; it is worked out the first time
 * the supplier gives and kept while the shape lasts, so a tick only shares the energy out.
 *
 * <p>A tick gives in three rounds: generators feed consumers; what generators have left fills storage; storage feeds
 * what consumers still want. In each round every supplier, in the order of its position, shares what it has among the
 * receivers it reaches in proportion to what they want, never more than the cables on the way still carry.
 */
public final class EnergyFlowGraph {

    private final Map<Long, IEnergyNode> nodes;
    private final Map<Long, IEnergyCable> cables;
    private final Map<Long, List<Long>> adjacency;
    private final Map<Long, Map<Long, Route>> routes = new HashMap<>();

    private static final Comparator<long[]> NEAREST =
            Comparator.<long[]>comparingLong(step -> step[0]).thenComparingLong(step -> step[1])
                    .thenComparingLong(step -> step[2]);

    public EnergyFlowGraph(
            final Map<Long, IEnergyNode> nodes,
            final Map<Long, IEnergyCable> cables,
            final Map<Long, List<Long>> adjacency) {
        this.nodes = new TreeMap<>(nodes);
        this.cables = Map.copyOf(cables);
        final Map<Long, List<Long>> copied = new HashMap<>();
        adjacency.forEach((pos, next) -> copied.put(pos, List.copyOf(next)));
        this.adjacency = copied;
    }

    /**
     * Raw result of one distribution: used internally by {@link EnergyNetwork} to build the
     * {@link EnergyDistributionResult} exposed to the caller.
     */
    public record FlowResult(
            long totalSupply,
            long totalDemand,
            long totalDelivered,
            Map<Long, Long> consumerDelivered,
            Map<Long, Long> cableUsage,
            long totalLost) {
    }

    /** The way from a supplier to a receiver: its cables in order, what it loses, and whether any cable limits it. */
    private record Route(long[] cables, int loss, boolean limited) {
    }

    public FlowResult distribute() {
        final Map<Long, Long> supplyLeft = new LinkedHashMap<>();
        final Map<Long, Long> demandLeft = new LinkedHashMap<>();
        long totalSupply = 0L;
        long totalDemand = 0L;
        for (final Map.Entry<Long, IEnergyNode> entry : this.nodes.entrySet()) {
            final IEnergyNode node = entry.getValue();
            final EnergyNodeRole role = node.role();
            if (role.canSupply()) {
                final long supply = Math.max(0L, node.supply());
                if (supply > 0) {
                    supplyLeft.put(entry.getKey(), supply);
                    totalSupply = saturatingAdd(totalSupply, supply);
                }
            }
            if (role.canConsume()) {
                final long demand = Math.max(0L, node.demand());
                if (demand > 0) {
                    demandLeft.put(entry.getKey(), demand);
                    totalDemand = saturatingAdd(totalDemand, demand);
                }
            }
        }
        if (totalSupply == 0L || totalDemand == 0L) {
            return new FlowResult(totalSupply, totalDemand, 0L, Map.of(), Map.of(), 0L);
        }

        final Tally tally = new Tally(supplyLeft, demandLeft);
        tally.round(EnergyNodeRole.GENERATOR, EnergyNodeRole.CONSUMER);
        tally.round(EnergyNodeRole.GENERATOR, EnergyNodeRole.STORAGE);
        tally.round(EnergyNodeRole.STORAGE, EnergyNodeRole.CONSUMER);

        for (final Map.Entry<Long, Long> sent : tally.sent.entrySet()) {
            this.nodes.get(sent.getKey()).onSupplied(sent.getValue());
        }
        for (final Map.Entry<Long, Long> got : tally.delivered.entrySet()) {
            this.nodes.get(got.getKey()).onConsumed(got.getValue());
        }
        return new FlowResult(totalSupply, totalDemand, tally.totalDelivered,
                Map.copyOf(tally.delivered), Map.copyOf(tally.cableUsage), tally.totalLost);
    }

    /* The ways from source to every node it reaches, worked out once while the shape lasts. */
    private Map<Long, Route> routesFrom(final long source) {
        return this.routes.computeIfAbsent(source, this::findRoutes);
    }

    /*
     * Least loss first, then fewest cables, then the lower position, so the way picked is the same on every run. Only
     * cables are passed through: every other node is where a way ends, and a node right beside the source with no
     * cable between them is not reached at all, so no way ever skips a cable's limit.
     */
    private Map<Long, Route> findRoutes(final long source) {
        final Map<Long, long[]> best = new HashMap<>();
        final Map<Long, Long> parent = new HashMap<>();
        final PriorityQueue<long[]> queue = new PriorityQueue<>(NEAREST);
        best.put(source, new long[] {0L, 0L});
        queue.add(new long[] {0L, 0L, source});
        final Map<Long, Route> found = new LinkedHashMap<>();
        while (!queue.isEmpty()) {
            final long[] step = queue.poll();
            final long pos = step[2];
            final long[] known = best.get(pos);
            if (known[0] != step[0] || known[1] != step[1]) {
                continue;
            }
            if (pos != source && this.nodes.containsKey(pos)) {
                found.put(pos, routeTo(parent, source, pos, (int) step[0]));
                continue;
            }
            for (final long next : this.adjacency.getOrDefault(pos, List.of())) {
                final IEnergyCable cable = this.cables.get(next);
                if (cable == null && (pos == source || !this.nodes.containsKey(next))) {
                    continue;
                }
                final long loss = cable == null ? step[0] : EnergyLoss.along((int) step[0], cable.loss());
                final long hops = step[1] + 1;
                final long[] before = best.get(next);
                if (before == null || loss < before[0] || loss == before[0] && hops < before[1]) {
                    best.put(next, new long[] {loss, hops});
                    parent.put(next, pos);
                    queue.add(new long[] {loss, hops, next});
                }
            }
        }
        return found;
    }

    private Route routeTo(final Map<Long, Long> parent, final long source, final long target, final int loss) {
        final List<Long> path = new ArrayList<>();
        boolean limited = false;
        long current = parent.get(target);
        while (current != source) {
            final IEnergyCable cable = this.cables.get(current);
            path.add(current);
            limited |= cable.maxThroughput() != Long.MAX_VALUE;
            current = parent.get(current);
        }
        Collections.reverse(path);
        final long[] steps = new long[path.size()];
        for (int i = 0; i < steps.length; i++) {
            steps[i] = path.get(i);
        }
        return new Route(steps, loss, limited);
    }

    private static long saturatingAdd(final long a, final long b) {
        final long sum = a + b;
        return sum < 0 ? Long.MAX_VALUE : sum;
    }

    /* What one tick gives and takes, kept across its three rounds. */
    private final class Tally {

        private final Map<Long, Long> supplyLeft;
        private final Map<Long, Long> demandLeft;
        private final Map<Long, Long> throughputLeft = new HashMap<>();
        private final Map<Long, Long> sent = new LinkedHashMap<>();
        private final Map<Long, Long> delivered = new LinkedHashMap<>();
        private final Map<Long, Long> cableUsage = new HashMap<>();
        private long totalDelivered;
        private long totalLost;

        private Tally(final Map<Long, Long> supplyLeft, final Map<Long, Long> demandLeft) {
            this.supplyLeft = supplyLeft;
            this.demandLeft = demandLeft;
        }

        private void round(final EnergyNodeRole from, final EnergyNodeRole to) {
            for (final Map.Entry<Long, Long> supplier : this.supplyLeft.entrySet()) {
                final long source = supplier.getKey();
                if (supplier.getValue() > 0 && EnergyFlowGraph.this.nodes.get(source).role() == from) {
                    give(source, to);
                }
            }
        }

        /*
         * Shares are worked out from each way's limit before any energy moves, so a clamp to what a shared cable still
         * carries can leave energy unsent that another receiver could take. The rest is split again among the ways
         * that still carry; every pass that clamps fills at least one way, so the passes end.
         */
        private void give(final long source, final EnergyNodeRole to) {
            final Map<Long, Route> reached = routesFrom(source);
            boolean again;
            do {
                again = giveOnce(source, reached, to);
            } while (again && this.supplyLeft.get(source) > 0);
        }

        /* One split of what the supplier has left; whether a clamp cut a share, so another pass may place the rest. */
        private boolean giveOnce(final long source, final Map<Long, Route> reached, final EnergyNodeRole to) {
            final Map<Long, Long> wants = new LinkedHashMap<>();
            final Map<Long, Long> caps = new HashMap<>();
            for (final Map.Entry<Long, Long> receiver : this.demandLeft.entrySet()) {
                final Route route = reached.get(receiver.getKey());
                if (route == null || receiver.getValue() <= 0 || route.loss() == EnergyLoss.WHOLE
                        || EnergyFlowGraph.this.nodes.get(receiver.getKey()).role() != to) {
                    continue;
                }
                final long cap = carries(route);
                if (cap > 0) {
                    wants.put(receiver.getKey(), EnergyLoss.toSend(receiver.getValue(), route.loss()));
                    caps.put(receiver.getKey(), cap);
                }
            }
            if (wants.isEmpty()) {
                return false;
            }
            final Map<Long, Long> shares = ProportionalSplitter.split(this.supplyLeft.get(source), wants, caps);
            long given = 0L;
            boolean clamped = false;
            for (final Long receiver : wants.keySet()) {
                final Route route = reached.get(receiver);
                /*
                 * Clamped to what the way still carries: the shares were worked out from each way's limit before any
                 * energy moved, and two ways through one cable would otherwise send more than it carries together.
                 */
                final long share = shares.getOrDefault(receiver, 0L);
                final long amount = Math.min(share, carries(route));
                clamped |= amount < share;
                if (amount <= 0) {
                    continue;
                }
                for (final long cable : route.cables()) {
                    final long left = left(cable);
                    if (left != Long.MAX_VALUE) {
                        this.throughputLeft.put(cable, left - amount);
                    }
                    this.cableUsage.merge(cable, amount, EnergyFlowGraph::saturatingAdd);
                }
                final long arrived = EnergyLoss.delivered(amount, route.loss());
                this.demandLeft.merge(receiver, -arrived, Long::sum);
                this.delivered.merge(receiver, arrived, EnergyFlowGraph::saturatingAdd);
                this.totalDelivered = saturatingAdd(this.totalDelivered, arrived);
                this.totalLost = saturatingAdd(this.totalLost, amount - arrived);
                given += amount;
            }
            if (given > 0) {
                this.supplyLeft.merge(source, -given, Long::sum);
                this.sent.merge(source, given, EnergyFlowGraph::saturatingAdd);
            }
            return clamped && given > 0;
        }

        /* The most the way still carries: the least any of its cables has left this tick. */
        private long carries(final Route route) {
            if (route.cables().length == 0) {
                return 0L;
            }
            if (!route.limited()) {
                return Long.MAX_VALUE;
            }
            long least = Long.MAX_VALUE;
            for (final long cable : route.cables()) {
                least = Math.min(least, left(cable));
            }
            return least;
        }

        /* What {@code cable} still carries this tick. */
        private long left(final long cable) {
            final Long left = this.throughputLeft.get(cable);
            return left != null ? left : EnergyFlowGraph.this.cables.get(cable).maxThroughput();
        }
    }
}
