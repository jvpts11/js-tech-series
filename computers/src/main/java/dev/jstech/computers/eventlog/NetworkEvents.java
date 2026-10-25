/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.eventlog;

import dev.jstech.computers.os.HostScope;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextBounds;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
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
    public static final TextKey CONFLICT = TextKey.of("jsc.event_log.conflict",
            "Network conflict: a second Mainframe on the network, near %s");
    public static final TextKey CONFLICT_RESOLVED = TextKey.of("jsc.event_log.conflict_resolved",
            "Network conflict resolved");
    /** What the machine column shows for something of the whole network. */
    public static final TextKey WHOLE_NETWORK = TextKey.of("jsc.event_log.network", "network");

    /** The longest machine name a log keeps. */
    public static final int MOST_NAME = 48;

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
     * What the log calls a machine: the name a player gave it, else its kind's short mark and its short id, as the
     * network names its servers ({@code SRV-3fa2c1}).
     */
    public static String machineName(final String customName, final HostScope machine, final NodeUuid node) {
        if (!customName.isEmpty()) {
            return customName;
        }
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
