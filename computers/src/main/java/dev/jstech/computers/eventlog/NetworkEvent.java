/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.eventlog;

import dev.jstech.core.text.Text;
import dev.jstech.core.uuid.NodeUuid;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * One line of the network's event log: when it happened, how much it matters, the machine it happened to, and what
 * happened, kept as text each reader sees in their own language.
 *
 * @param time        the world's game time it happened at, which the log keeps its order and age by
 * @param dayTime     the world's clock at that moment, which the log shows
 * @param level       how much it matters
 * @param machine     the machine it happened to, or null for something of the whole network
 * @param machineName what the machine was called then, or empty for the whole network
 * @param message     what happened
 */
public record NetworkEvent(long time, long dayTime, EventLevel level, @Nullable NodeUuid machine,
                           String machineName, Text message) {

    public NetworkEvent {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(machineName, "machineName");
        Objects.requireNonNull(message, "message");
    }
}
