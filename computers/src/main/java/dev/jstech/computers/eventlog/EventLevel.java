/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.eventlog;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * How much an event of the network's log matters: something that happened, something to look at, something that
 * went wrong. Saved and sent by its id.
 */
@TextHolder
public enum EventLevel implements IStableId {

    INFO(0, TextKey.of("jsc.event_log.level.info", "INFO")),
    WARN(1, TextKey.of("jsc.event_log.level.warn", "WARN")),
    ERROR(2, TextKey.of("jsc.event_log.level.error", "ERROR"));

    private final int id;
    private final TextKey label;

    /** The levels, from the least to the most that matters: the order a filter offers them in. */
    public static final List<EventLevel> ALL = List.of(INFO, WARN, ERROR);

    private static final StableIds<EventLevel> IDS = StableIds.of(EventLevel.class);

    EventLevel(final int id, final TextKey label) {
        this.id = id;
        this.label = label;
    }

    /** The level that declares {@code id}; an id no level declares reads as INFO. */
    public static EventLevel byId(final int id) {
        return IDS.byId(id, INFO);
    }

    @Override
    public int id() {
        return this.id;
    }

    /** What the log's level column shows. */
    public TextKey label() {
        return this.label;
    }
}
