/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import java.util.List;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

/**
 * The last lines of chat the player's game was sent, a couple of hundred, each with whether a player wrote it, for an
 * agent to read: what a player wrote goes back to it marked as theirs.
 */
public final class ChatLines {

    private static final int KEPT = 200;
    private static final LastLines<Line> LINES = new LastLines<>(KEPT);

    private ChatLines() {
    }

    /** Hands the game's bus the chat, once, from the player's game. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientChatReceivedEvent.class, event -> LINES.add(new Line(
                System.currentTimeMillis(), !event.isSystem(), event.getMessage().getString())));
    }

    /** The lines kept, the oldest first. */
    public static List<Line> lines() {
        return LINES.all();
    }

    /** How many lines have come so far: the moment a wait for a line asks {@link #since} from. */
    static long added() {
        return LINES.added();
    }

    /** The lines that came after the {@code count}th, as many of them as are still kept. */
    static List<Line> since(final long count) {
        return LINES.since(count);
    }

    /**
     * A line of chat.
     *
     * @param millis   when it came, in milliseconds since the epoch
     * @param byPlayer whether a player wrote it, rather than the game
     * @param text     what it says
     */
    public record Line(long millis, boolean byPlayer, String text) {
    }
}
