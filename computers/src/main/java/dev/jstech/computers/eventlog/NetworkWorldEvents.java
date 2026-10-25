/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.eventlog;

import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.network.NetworkTables;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.world.event.WorldEvent;
import dev.jstech.core.world.event.WorldEventStage;
import dev.jstech.core.world.event.WorldEventType;
import dev.jstech.core.world.event.WorldEvents;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The world events a data network sees, and what its event log is told of them. An event under way is seen by every
 * network it reaches: one over the whole dimension reaches them all, one round a place reaches a network whose
 * Mainframe stands within it. An event still to come is seen only by a network with its instrument on it (a member
 * that says it is that event's instrument), once the event's warning has gone out, as a player carrying the
 * instrument is warned.
 */
@TextHolder
public final class NetworkWorldEvents {

    public static final TextKey COMING = TextKey.of("jsc.event_log.world_event.coming", "%s expected in %s");
    public static final TextKey UNDER_WAY = TextKey.of("jsc.event_log.world_event.under_way", "%s under way");
    public static final TextKey OVER = TextKey.of("jsc.event_log.world_event.over", "%s is over");

    private NetworkWorldEvents() {
    }

    /** Hears the world events' steps, so each network that sees one writes it into its event log. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(WorldEventStage.Warned.class, stage -> {
            final long ticks = stage.event().startsAt() - stage.level().getGameTime();
            write(stage, true, EventLevel.WARN, COMING.with(stage.type().title().text(), WorldEvents.clock(ticks)));
        });
        NeoForge.EVENT_BUS.addListener(WorldEventStage.Started.class, stage ->
                write(stage, false, EventLevel.WARN, UNDER_WAY.with(stage.type().title().text())));
        NeoForge.EVENT_BUS.addListener(WorldEventStage.Ended.class, stage ->
                write(stage, false, EventLevel.INFO, OVER.with(stage.type().title().text())));
    }

    /** Whether {@code network} sees {@code event} under way at the world's game time {@code now}. */
    public static boolean underWay(final ServerLevel level, final NetworkUuid network, final WorldEvent event,
                                   final long now) {
        return event.activeAt(now) && reaches(level, network, event);
    }

    /** Whether {@code network} sees {@code event} coming at the world's game time {@code now}. */
    public static boolean coming(final ServerLevel level, final NetworkUuid network, final WorldEvent event,
                                 final long now) {
        return now < event.startsAt() && event.warned() && reaches(level, network, event)
                && NetworkTables.hasInstrument(level, network, event.type());
    }

    /* Whether {@code event} reaches where {@code network} stands: anywhere for one over the whole dimension. */
    private static boolean reaches(final ServerLevel level, final NetworkUuid network, final WorldEvent event) {
        final Optional<WorldEventType> type = WorldEvents.get(level, event.type());
        if (type.isEmpty() || !type.get().local()) {
            return true;
        }
        return NetworkSystem.get(level).mainframePositionOf(network)
                .map(pos -> event.covers(BlockPos.of(pos), type.get().radius())).orElse(false);
    }

    /*
     * Writes {@code message} into the log of every network that sees the step's event; a warning needs the
     * instrument.
     */
    private static void write(final WorldEventStage stage, final boolean warning, final EventLevel what,
                              final Text message) {
        final ServerLevel level = stage.level();
        for (final NetworkUuid network : NetworkSystem.get(level).networks()) {
            if (reaches(level, network, stage.event())
                    && (!warning || NetworkTables.hasInstrument(level, network, stage.event().type()))) {
                NetworkEvents.record(level, network, what, null, "", message);
            }
        }
    }
}
