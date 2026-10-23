/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.network;

import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridMember;
import dev.jstech.core.grid.INetworkGrids;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.ApiStatus;

/**
 * The data network keeps the grid of data cables with itself, in each dimension's {@link ConnectivityIndex}: a run of
 * data cable takes the identity of the network it is joined into, and the network knows how far it reaches.
 */
public final class DataGrids implements INetworkGrids {

    private DataGrids() {
    }

    /** Hands the Core's grids the data network as the keeper of the grids that carry networks, once. */
    @ApiStatus.Internal
    public static void declare() {
        CoreGrids.keepNetworksIn(new DataGrids());
    }

    @Override
    public Grid grid(final ServerLevel level) {
        return index(level).grid();
    }

    @Override
    public void place(final ServerLevel level, final long number, final List<Long> joined, final GridMember member) {
        index(level).place(number, joined, member);
    }

    @Override
    public void remove(final ServerLevel level, final long number) {
        index(level).onCableRemovedIfRegistered(number);
    }

    @Override
    public Optional<NetworkUuid> reached(final ServerLevel level, final long number) {
        return index(level).networkOf(number);
    }

    @Override
    public Optional<NetworkUuid> joined(final ServerLevel level, final long number) {
        return index(level).joinedNetwork(number);
    }

    @Override
    public void assign(final ServerLevel level, final long number, final NetworkUuid network) {
        if (index(level).joinedNetwork(number).isEmpty()) {
            index(level).assignUuid(number, network);
        }
    }

    private static ConnectivityIndex index(final ServerLevel level) {
        return NetworkSystem.get(level).connectivity();
    }
}
