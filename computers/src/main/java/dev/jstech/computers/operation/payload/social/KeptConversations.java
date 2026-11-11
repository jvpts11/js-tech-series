/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.social;

import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.program.MessengerLog;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.cli.IKeptConversations;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The conversations one server's Messenger Service keeps, for whoever is at that server: what {@code msgsvc} and the
 * Kept conversations window reach. A deletion is written onto the Server item at once and every open Messenger is sent
 * what it shows now, so the conversation simply is not there any more, with no word about where it went.
 */
public final class KeptConversations implements IKeptConversations {

    private final ServerLevel level;
    private final ServerServices.Host host;
    private final MessengerLog log;

    private KeptConversations(final ServerLevel level, final ServerServices.Host host, final MessengerLog log) {
        this.level = level;
        this.host = host;
        this.log = log;
    }

    /** What the server at {@code host} keeps, or null when it has the Messenger Service not installed. */
    @Nullable
    public static KeptConversations at(final ServerLevel level, final ServerServices.Host host) {
        final MessengerLog log = host.rack().messengerAt(host.slot());
        return log == null || !host.rack().hasService(host.slot(), Programs.MESSENGER_SERVICE) ? null
                : new KeptConversations(level, host, log);
    }

    @Override
    public List<MessengerLog.Conversation> list() {
        return log.conversations();
    }

    @Override
    @Nullable
    public MessengerLog.Conversation delete(final String room) {
        final MessengerLog.Conversation gone = log.delete(room);
        if (gone != null) {
            changed();
        }
        return gone;
    }

    @Override
    public List<MessengerLog.Conversation> deleteAll() {
        final List<MessengerLog.Conversation> gone = log.deleteAll();
        if (!gone.isEmpty()) {
            changed();
        }
        return gone;
    }

    @Override
    public Status status() {
        final boolean running = host.rack().serviceUp(host.slot(), Programs.MESSENGER_SERVICE)
                && host.rack().unitRunning(host.slot());
        return new Status(host.hostname(), running, log.historyBytes(), log.ramMb(), log.connected().size());
    }

    /* The clock has run as long as the game since then, unless a night was slept through in between. */
    @Override
    public long clockAt(final long at) {
        return level.getDayTime() - (level.getGameTime() - at);
    }

    private void changed() {
        host.changed();
        SocialPayloads.tellEveryone(level, host, log);
    }
}
