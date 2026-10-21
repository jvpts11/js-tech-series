/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import dev.jstech.core.text.Text;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

/**
 * The game an MCP server runs in, as the server needs it: which side it is, the version it reports, why it answers
 * nothing now (its developer mode off), asking the player whether a client may come in, and being told when a call
 * that acts begins and ends, so the screen says so.
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
