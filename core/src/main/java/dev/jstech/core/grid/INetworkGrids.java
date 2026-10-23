/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import dev.jstech.core.uuid.NetworkUuid;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;

/**
 * Where a dimension keeps the grid of a kind that carries networks ({@link GridKind#carriesNetwork}), in place of its
 * own grids: a network keeps its cables with itself, so a run takes the identity of the network it is joined into
 * and a network knows how far it reaches. The mod whose networks they are hands its keeper to
 * {@link CoreGrids#keepNetworksIn}; the Core's cables put themselves in and take themselves out through
 * {@link CoreGrids} as with any other grid.
 */
public interface INetworkGrids {

    /** The grid of {@code level}'s networks. */
    Grid grid(ServerLevel level);

    /** Puts {@code member} in at {@code number}, joined to the places {@code joined}. */
    void place(ServerLevel level, long number, List<Long> joined, GridMember member);

    /** Takes the place {@code number} out; nothing when it was not in. */
    void remove(ServerLevel level, long number);

    /** The network the place {@code number} reaches the owner of, within how far its runs reach; none for none. */
    Optional<NetworkUuid> reached(ServerLevel level, long number);

    /** The network the place {@code number} is joined into, reached or not; none for none. */
    Optional<NetworkUuid> joined(ServerLevel level, long number);

    /**
     * Joins the place {@code number} into {@code network} again, as a cable saved on it comes back, unless it has been
     * joined into one already.
     */
    void assign(ServerLevel level, long number, NetworkUuid network);
}
