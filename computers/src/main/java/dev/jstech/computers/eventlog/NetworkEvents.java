/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.eventlog;

import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextBounds;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextLists;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * What writes into a network's event log, and the words it writes: a machine switched on or off, a conflict over the
 * network starting and ending. Each is written where the change itself happens, never found by looking for it.
 */
@TextHolder
public final class NetworkEvents {

    public static final TextKey TURNED_ON = TextKey.of("jsc.event_log.turned_on", "Turned on");
    public static final TextKey TURNED_OFF = TextKey.of("jsc.event_log.turned_off", "Turned off");
    public static final TextKey POWER_LOST = TextKey.of("jsc.event_log.power_lost", "Power lost: no energy reached it");
    public static final TextKey UNSTABLE_POWER = TextKey.of("jsc.event_log.unstable_power",
            "Power lost: the line gave less than it draws (unstable power)");
    public static final TextKey THERMAL_TRIP = TextKey.of("jsc.event_log.thermal_trip",
            "Thermal trip at %s °C: shut down until it cools below %s °C");
    public static final TextKey ITEMS_LOST = TextKey.of("jsc.event_log.items_lost",
            "Lost in transit when it went dark: %s");
    public static final TextKey CONFLICT = TextKey.of("jsc.event_log.conflict",
            "Network conflict: a second Mainframe on the network, near %s");
    public static final TextKey CONFLICT_RESOLVED = TextKey.of("jsc.event_log.conflict_resolved",
            "Network conflict resolved");
    public static final TextKey ITEMS_LOST_CONFLICT = TextKey.of("jsc.event_log.items_lost_conflict",
            "Lost in transit in the network conflict: %s");
    public static final TextKey ITEMS_LOST_SPLIT = TextKey.of("jsc.event_log.items_lost_split",
            "Lost in transit when the network split: %s");
    public static final TextKey STORAGE_CORRUPTED = TextKey.of("jsc.event_log.storage_corrupted",
            "The network conflict corrupted the storage of %s machines: the Defrag can try to give it back");
    public static final TextKey DISKS_CORRUPTED = TextKey.of("jsc.event_log.disks_corrupted",
            "The thermal trip corrupted %s of its disks: the Defrag can try to give it back");
    public static final TextKey DEFRAG_DONE = TextKey.of("jsc.event_log.defrag_done",
            "Defrag of %s: %s corrupted, %s recovered, %s lost; bad sectors %s, now %s");
    public static final TextKey ANCHOR_SHORT = TextKey.of("jsc.event_log.anchor_short",
            "The Network Anchor Card keeps %s of its %s chunks loaded: the owner's quota or the server's is full");
    /** What the machine column shows for something of the whole network. */
    public static final TextKey WHOLE_NETWORK = TextKey.of("jsc.event_log.network", "network");
    /* One kind lost: so many of an item, so many millibuckets of a fluid or a chemical; and the kinds not named. */
    private static final TextKey LOST_ITEMS = TextKey.of("jsc.event_log.lost_items", "%s %s");
    private static final TextKey LOST_DATA = TextKey.of("jsc.event_log.lost_data", "%s mB of %s");
    private static final TextKey LOST_AND_MORE = TextKey.of("jsc.event_log.lost_and_more", "%s and %s more");

    /** The longest machine name a log keeps. */
    public static final int MOST_NAME = 48;
    /* The kinds a loss names, the largest first; the rest are counted. */
    private static final int LOST_NAMED = 3;

    private NetworkEvents() {
    }

    /**
     * Writes into {@code network}'s log that {@code message} happened to {@code machine}, called {@code machineName},
     * or to the whole network when {@code machine} is null; nothing for a machine on no network.
     */
    public static void record(final ServerLevel level, @Nullable final NetworkUuid network, final EventLevel what,
                              @Nullable final NodeUuid machine, final String machineName, final Text message) {
        if (network == null) {
            return;
        }
        NetworkEventLog.of(level).add(network, new NetworkEvent(level.getGameTime(), level.getDayTime(), what, machine,
                machine == null ? "" : TextBounds.clip(machineName, MOST_NAME), message));
    }

    /** Writes that a machine was switched on or off. */
    public static void switched(final ServerLevel level, @Nullable final NetworkUuid network,
                                @Nullable final NodeUuid machine, final String machineName, final boolean on) {
        if (machine != null) {
            record(level, network, EventLevel.INFO, machine, machineName, (on ? TURNED_ON : TURNED_OFF).text());
        }
    }

    /**
     * Writes what a machine's Operations were carrying when it went dark suddenly, {@code lost} by kind: how much of
     * the largest few, and how many kinds more.
     */
    public static void itemsLost(final ServerLevel level, @Nullable final NetworkUuid network,
                                 @Nullable final NodeUuid machine, final String machineName,
                                 final Map<StorageKey, Long> lost) {
        itemsLost(level, network, machine, machineName, lost, ITEMS_LOST);
    }

    /** The same, in the words of how they were lost: {@code wording} takes the list of what. */
    public static void itemsLost(final ServerLevel level, @Nullable final NetworkUuid network,
                                 @Nullable final NodeUuid machine, final String machineName,
                                 final Map<StorageKey, Long> lost, final TextKey wording) {
        if (machine == null || lost.isEmpty()) {
            return;
        }
        final List<Map.Entry<StorageKey, Long>> kinds = new ArrayList<>(lost.entrySet());
        kinds.sort(Map.Entry.<StorageKey, Long>comparingByValue().reversed());
        final List<Text> named = new ArrayList<>();
        for (final Map.Entry<StorageKey, Long> kind : kinds.subList(0, Math.min(LOST_NAMED, kinds.size()))) {
            final String amount = String.format(Locale.ROOT, "%,d", kind.getValue());
            final String name = kind.getKey().displayName().getString();
            named.add((kind.getKey().isItem() ? LOST_ITEMS : LOST_DATA).with(amount, name));
        }
        final Text list = TextLists.join(", ", named);
        final int more = kinds.size() - named.size();
        record(level, network, EventLevel.ERROR, machine, machineName,
                wording.with(more > 0 ? LOST_AND_MORE.with(list, more) : list));
    }

    /**
     * What the log calls a machine: the name a player gave it, else its id ({@link #machineId}), as the network names
     * its servers ({@code SRV-3fa2c1}).
     */
    public static String machineName(final String customName, final HostScope machine, final NodeUuid node) {
        return customName.isEmpty() ? machineId(machine, node) : customName;
    }

    /**
     * A machine's id, which tells it apart from machines of the same name: its kind's short mark and its short id
     * ({@code CC-4a91e2}).
     */
    public static String machineId(final HostScope machine, final NodeUuid node) {
        final String mark;
        if (machine == HostScope.MAINFRAME) {
            mark = "MF";
        } else if (machine == HostScope.SERVER) {
            mark = "SRV";
        } else if (machine == HostScope.CRAFTING_COMPUTER) {
            mark = "CC";
        } else if (machine == HostScope.CLUSTER_MANAGEMENT_COMPUTER) {
            mark = "CMC";
        } else {
            mark = "PC";
        }
        return mark + "-" + ShortId.of(node.asString());
    }
}
