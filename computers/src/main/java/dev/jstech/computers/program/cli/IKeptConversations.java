/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.MessengerLog;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * The conversations a server's Messenger Service keeps, as whoever is at that server manages them: {@code msgsvc} at
 * its prompt and the Kept conversations window of its Messenger. A conversation deleted here leaves every Messenger on
 * the network at once, with no word to anybody (computers/docs/design/programs.md).
 */
public interface IKeptConversations {

    /** Every conversation kept, in the order each was first spoken in. */
    List<MessengerLog.Conversation> list();

    /** Deletes the conversation in {@code room}; answers what went, or null when no such conversation is kept. */
    @Nullable
    MessengerLog.Conversation delete(String room);

    /** Deletes every conversation; answers what went. */
    List<MessengerLog.Conversation> deleteAll();

    /** What the service is right now, for {@code msgsvc status}. */
    Status status();

    /** The world's clock at the game time {@code at}, for saying when a conversation was last spoken in. */
    long clockAt(long at);

    /**
     * What the service is right now.
     *
     * @param host    the name of the server it runs on
     * @param running whether it is up, rather than installed and stopped
     * @param bytes   what everything it keeps weighs on the server's disk
     * @param ramMb   the memory it holds, which grows with each person who has a Messenger open
     * @param people  how many have a Messenger open
     */
    record Status(String host, boolean running, long bytes, int ramMb, int people) {
    }
}
