/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.live;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * What one thing in the world shows, sent to the players near enough to see it, and only when it changes.
 *
 * <p>A screen in the world is drawn on each viewer's game from a description the server sends: what the machine
 * behind it shows. The feed keeps, for each player near, the description that player was last sent, and sends a new
 * one only when it differs, so a screen standing still sends nothing. With nobody near, the description is not even
 * worked out, so a machine in an empty room costs a look at the player list. A player who walks out of reach is
 * forgotten, and sent the description afresh on coming back.
 *
 * @param <T> the description; equal descriptions are the same picture
 */
public final class LiveFeed<T> {

    private final Map<UUID, T> sent = new HashMap<>();
    private final double reachSquared;

    /** A feed reaching {@link LiveRate#FAR} blocks, as far as a picture is drawn at all. */
    public LiveFeed() {
        this(LiveRate.FAR);
    }

    public LiveFeed(final double reach) {
        this.reachSquared = reach * reach;
    }

    /**
     * Sends what the thing at {@code at} shows to every player near who does not have it yet: {@code describe} works
     * it out, at most once and only with somebody near, and {@code send} sends it to one player.
     */
    public void tick(final ServerLevel level, final BlockPos at, final Supplier<T> describe,
                     final BiConsumer<ServerPlayer, T> send) {
        final double x = at.getX() + 0.5;
        final double y = at.getY() + 0.5;
        final double z = at.getZ() + 0.5;
        T now = null;
        boolean anyNear = false;
        for (final ServerPlayer player : level.players()) {
            final UUID id = player.getUUID();
            if (player.distanceToSqr(x, y, z) > reachSquared) {
                sent.remove(id);
                continue;
            }
            anyNear = true;
            if (now == null) {
                now = describe.get();
            }
            if (!now.equals(sent.get(id))) {
                send.accept(player, now);
                sent.put(id, now);
            }
        }
        if (!anyNear) {
            sent.clear();
        } else if (sent.size() > level.players().size()) {
            // Players who left the level altogether.
            sent.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
        }
    }

    /** Forgets what everybody was sent, so the next tick sends the description to all near afresh. */
    public void reset() {
        sent.clear();
    }

    /** How many players hold the description right now. */
    public int holders() {
        return sent.size();
    }
}
