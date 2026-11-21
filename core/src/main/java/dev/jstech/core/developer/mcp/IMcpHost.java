/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import com.google.gson.JsonObject;
import dev.jstech.core.text.Text;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

/**
 * The game an MCP server runs in, as the server needs it: which side it is, the version it reports, why it answers
 * nothing now (its developer mode off), asking the player whether a client may come in, being told when a call
 * that acts begins and ends, so the screen says so, and a glimpse of the game round every call.
 */
public interface IMcpHost {

    /** Which game it is. */
    McpSide side();

    /** The version the server reports of itself: the Core's. */
    String version();

    /** Why the server answers nothing now, or none while it may. */
    Optional<Text> closed();

    /**
     * Asks the player whether the client {@code name} of version {@code version} may come in, and what it may do;
     * answered within the time the player is given, refused when nobody answers.
     */
    CompletableFuture<Pairing> pair(String name, String version);

    /** A call that acts began. */
    void began(McpCall call);

    /** A call that acts ended, however it ended. */
    void ended(McpCall call);

    /**
     * The game as a call may change it: a few readings by name, each one JSON value, and the game's tick under
     * {@link McpChanges#TICK}, read on the game's own thread. Taken before and after every call, so the call's answer
     * tells the tick and what changed ({@link McpChanges}). Empty when there is nothing to read, as with no world
     * open, or when the game does not answer in time; a game that reads nothing need not say so.
     */
    default JsonObject glimpse() {
        return new JsonObject();
    }

    /**
     * What a call's answer tells from the glimpse {@code before} the call and the one {@code after} it: the tick and
     * what changed ({@link McpChanges#told}), to which a game may add what only it can tell, as the lines of chat
     * that came while the call ran.
     */
    default JsonObject told(final JsonObject before, final JsonObject after) {
        return McpChanges.told(before, after);
    }

    /**
     * What the player answered a pairing.
     *
     * @param permission what the client may do, or none when it was refused
     * @param owner      the player who answered, on a server; none on the player's own game
     * @param why        why it was refused, or none
     */
    record Pairing(Optional<McpPermission> permission, @Nullable UUID owner, @Nullable Text why) {

        /** Let in, to do {@code permission}, by {@code owner}. */
        public static Pairing allowed(final McpPermission permission, @Nullable final UUID owner) {
            return new Pairing(Optional.of(permission), owner, null);
        }

        /** Refused, because {@code why}. */
        public static Pairing refused(final Text why) {
            return new Pairing(Optional.empty(), null, why);
        }
    }
}
