/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.datacenter.DatacenterSection;
import dev.jstech.computers.datacenter.RackTraffic;
import dev.jstech.computers.datacenter.SectionAlert;
import dev.jstech.computers.datacenter.SectionDiagnostics;
import dev.jstech.computers.datacenter.SectionReading;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvents;
import dev.jstech.computers.operation.NetworkIndex;
import dev.jstech.computers.storage.ServerStore;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * What a Server Router watches of each of its datacenter sections, once a second: what the section's racks moved and
 * how long their servers took to answer, its base latency and the most it can move, and its alerts, each of which goes
 * into the network's log as it is raised (computers/docs/design/servers-and-racks.md). What it gathers goes into each
 * section's {@link SectionDiagnostics}, which the Cluster Manager shows.
 */
@TextHolder
final class SectionWatch {

    private final Map<Direction, SectionDiagnostics> diagnostics = new EnumMap<>(Direction.class);
    private final Map<Direction, SectionReading> readings = new EnumMap<>(Direction.class);
    private int readIn;

    private static final TextKey DISKS_FULL = TextKey.of("jsc.section_alert.disks_full", "%s: disks %s%% full");
    private static final TextKey RACK_HOT = TextKey.of("jsc.section_alert.rack_hot", "Rack %s over its thermal budget");
    private static final TextKey RACK_THROTTLED = TextKey.of("jsc.section_alert.rack_throttled",
            "Rack %s over its thermal budget (THROTTLED %s%%)");
    private static final TextKey AT_BUDGET = TextKey.of("jsc.section_alert.at_budget",
            "Router at its budget: %s of its %s racks");
    private static final TextKey LATENCY_HIGH = TextKey.of("jsc.section_alert.latency_high",
            "Latency over twice the base of %s ticks");
    /* What the network's log says of an alert raised: the section's name, then the alert. */
    private static final TextKey LOGGED = TextKey.of("jsc.section_alert.logged", "%s: %s");
    private static final int READ_EVERY = 20;
    private static final int TICKS_A_SECOND = 20;
    private static final int DISKS_WARN_PERCENT = 90;
    private static final int DISKS_ERROR_PERCENT = 98;

    /** Once a second, reads every section of {@code router}, on {@code network}. */
    void tick(final ServerLevel level, final NetworkUuid network, final ServerRouterBlockEntity router) {
        if (--readIn > 0) {
            return;
        }
        readIn = READ_EVERY;
        final long now = level.getGameTime();
        for (final DatacenterSection section : router.sections()) {
            read(level, network, router, section, now);
        }
    }

    /** What a section has been doing, or null for a face that never had one. */
    @Nullable
    SectionDiagnostics diagnostics(final Direction face) {
        return diagnostics.get(face);
    }

    /** The last reading of a section, or null before its first. */
    @Nullable
    SectionReading reading(final Direction face) {
        return readings.get(face);
    }

    void save(final CompoundTag tag) {
        final CompoundTag all = new CompoundTag();
        diagnostics.forEach((face, diag) -> {
            final SectionDiagnostics.Saved saved = diag.saved();
            final CompoundTag one = new CompoundTag();
            one.put("Latency", new IntArrayTag(saved.latency()));
            one.put("Throughput", new IntArrayTag(saved.throughput()));
            final CompoundTag since = new CompoundTag();
            saved.since().forEach(since::putLong);
            one.put("Since", since);
            one.putLong("LastOverEnded", saved.lastOverEnded());
            one.putLong("LastOverTicks", saved.lastOverTicks());
            all.put(face.getName(), one);
        });
        if (!all.isEmpty()) {
            tag.put("Diagnostics", all);
        }
    }

    void load(final CompoundTag tag) {
        diagnostics.clear();
        readings.clear();
        final CompoundTag all = tag.getCompound("Diagnostics");
        for (final Direction face : Direction.values()) {
            if (!all.contains(face.getName())) {
                continue;
            }
            final CompoundTag one = all.getCompound(face.getName());
            final Map<String, Long> since = new LinkedHashMap<>();
            final CompoundTag stood = one.getCompound("Since");
            for (final String key : stood.getAllKeys()) {
                since.put(key, stood.getLong(key));
            }
            final SectionDiagnostics diag = new SectionDiagnostics();
            diag.restore(new SectionDiagnostics.Saved(ints(one.getIntArray("Latency")),
                    ints(one.getIntArray("Throughput")), since, one.getLong("LastOverEnded"),
                    one.getLong("LastOverTicks")));
            diagnostics.put(face, diag);
        }
    }

    /*
     * Reads one section: sums what its racks moved, works out its base latency and the most it can move from its
     * servers, finds its alerts, and tells the network's log of each alert newly raised.
     */
    private void read(final ServerLevel level, final NetworkUuid network, final ServerRouterBlockEntity router,
                      final DatacenterSection section, final long now) {
        final NetworkSystem system = NetworkSystem.get(level);
        final List<SectionAlert> alerts = new ArrayList<>();
        RackTraffic.Sample sample = RackTraffic.Sample.NONE;
        int rackIndex = 0;
        for (final long rackPos : section.rackPositions()) {
            rackIndex++;
            if (Loaded.blockEntity(level, BlockPos.of(rackPos)) instanceof ServerRackBlockEntity rack) {
                sample = sample.plus(rack.traffic().read(now));
                if (rack.thermalLoadWatts() > rack.thermalBudgetWatts()) {
                    final Text hot = rack.thermalThrottled()
                            ? RACK_THROTTLED.with(rackIndex, rack.thermalThrottlePercent()) : RACK_HOT.with(rackIndex);
                    alerts.add(new SectionAlert("heat:" + rackPos, EventLevel.WARN, hot));
                }
            }
        }
        long answerTicks = 0L;
        int servers = 0;
        long most = 0L;
        final Map<Long, Long> linkCaps = new HashMap<>();
        for (final NodeUuid server : section.servers()) {
            final Optional<NetworkSystem.ServerLocation> at = system.locationOf(server);
            if (at.isEmpty() || !(Loaded.blockEntity(level, BlockPos.of(at.get().rackPos()))
                    instanceof ServerRackBlockEntity rack)) {
                continue;
            }
            final int slot = at.get().slot();
            answerTicks += NetworkIndex.answerTicks(rack, slot);
            servers++;
            // Servers in one rack share the way to the Mainframe, so the slowest cable on it is found once a rack.
            final long link = linkCaps.computeIfAbsent(at.get().rackPos(),
                    pos -> NetworkIndex.serverLinkCap(level, network, server));
            most = saturatedAdd(most, Math.min(NetworkIndex.serverThroughputCap(level, server), link));
            final ServerStore store = rack.getServerStorage(slot);
            final int full = store == null || store.capacityWeight() <= 0L ? 0
                    : (int) Math.min(100L, store.usedWeight() * 100L / store.capacityWeight());
            if (full >= DISKS_WARN_PERCENT) {
                final EventLevel severity = full >= DISKS_ERROR_PERCENT ? EventLevel.ERROR : EventLevel.WARN;
                alerts.add(new SectionAlert("disks:" + server.value() + ":" + severity.id(), severity,
                        DISKS_FULL.with(rack.machineNameIn(slot), full)));
            }
        }
        final int maxRacks = router.maxRacks();
        if (maxRacks > 0 && router.managedRackCount() >= maxRacks) {
            alerts.add(new SectionAlert("budget", EventLevel.WARN,
                    AT_BUDGET.with(router.managedRackCount(), maxRacks)));
        }
        final int baseTenths = servers == 0 ? 0 : (int) ((answerTicks * 10L + servers / 2) / servers);
        final SectionDiagnostics diag = diagnostics.computeIfAbsent(section.face(), face -> new SectionDiagnostics());
        diag.second(now, sample, baseTenths);
        if (diag.latencyOver(baseTenths)) {
            alerts.add(new SectionAlert("latency", EventLevel.WARN, LATENCY_HIGH.with(baseTenths / 10.0)));
        }
        final Map<String, SectionAlert> byKey = new LinkedHashMap<>();
        alerts.forEach(alert -> byKey.put(alert.key(), alert));
        for (final String raised : diag.settle(now, byKey.keySet())) {
            final SectionAlert alert = byKey.get(raised);
            NetworkEvents.record(level, network, alert.level(), null, "",
                    LOGGED.with(router.sectionLabel(section.face()), alert.what()));
        }
        final long perSecond = saturatedMultiply(most, TICKS_A_SECOND);
        readings.put(section.face(), new SectionReading(baseTenths, (int) Math.min(Integer.MAX_VALUE, perSecond),
                alerts));
        router.diagnosticsChanged();
    }

    /* {@code a} plus {@code b}, held at the largest long rather than wrapping: a cap of no limit is that largest. */
    private static long saturatedAdd(final long a, final long b) {
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }

    private static long saturatedMultiply(final long a, final int b) {
        return a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b;
    }

    private static List<Integer> ints(final int[] values) {
        final List<Integer> out = new ArrayList<>(values.length);
        for (final int value : values) {
            out.add(value);
        }
        return out;
    }
}
