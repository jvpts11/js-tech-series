/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.network;

import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * What a data network may read of the machines of any mod on it, as state tables ({@link StateTable}): the rows of a
 * table, one for each member on the network that has one, written as it is now, and the tables the network has rows
 * in. What reads them, J's Computers' SELECT or a mod's own screen, asks here and depends on no mod whose machines it
 * reads.
 */
public final class NetworkTables {

    private NetworkTables() {
    }

    /** The rows of {@code table} on {@code network}, one for each loaded member with one, in the order they joined. */
    public static List<StateRow> rows(final ServerLevel level, final NetworkUuid network, final StateTable table) {
        final List<StateRow> rows = new ArrayList<>();
        for (final NetworkSystem.MemberNode node : NetworkSystem.get(level).membersOf(network)) {
            if (!node.tables().contains(table.id())) {
                continue;
            }
            final BlockPos pos = BlockPos.of(node.pos());
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof INetworkMember member)) {
                continue;
            }
            final StateRow.Writer row = new StateRow.Writer(table);
            member.fill(table, row);
            rows.add(row.done(node.nodeUuid(), node.pos()));
        }
        return rows;
    }

    /** The tables {@code network} has a row in, in the order the mods declared them. */
    public static List<StateTable> tablesOf(final ServerLevel level, final NetworkUuid network) {
        final Set<ResourceLocation> named = new LinkedHashSet<>();
        for (final NetworkSystem.MemberNode node : NetworkSystem.get(level).membersOf(network)) {
            named.addAll(node.tables());
        }
        final List<StateTable> tables = new ArrayList<>();
        for (final StateTable table : StateTables.all()) {
            if (named.contains(table.id())) {
                tables.add(table);
            }
        }
        return tables;
    }
}
