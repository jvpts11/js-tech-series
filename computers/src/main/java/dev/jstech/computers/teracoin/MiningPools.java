/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/** The Mining Pools a network has: the rack servers running the service, up, and what each one keeps. */
public final class MiningPools {

    private MiningPools() {
    }

    /** Every pool on {@code network}, in the order its servers are found. */
    public static List<ServerServices.Host> on(final ServerLevel level, @Nullable final NetworkUuid network) {
        return network == null ? List.of() : ServerServices.all(level, network, Programs.MINING_POOL);
    }

    /**
     * The pool a miner on {@code network} chose by {@code wanted}, its name or its server's, or the network's first
     * pool when it chose none; null when there is none such.
     */
    @Nullable
    public static ServerServices.Host chosen(final ServerLevel level, @Nullable final NetworkUuid network,
                                             final String wanted) {
        final List<ServerServices.Host> pools = on(level, network);
        if (wanted.isEmpty()) {
            return pools.isEmpty() ? null : pools.get(0);
        }
        final String sought = wanted.toLowerCase(Locale.ROOT);
        for (final ServerServices.Host host : pools) {
            if (name(host).toLowerCase(Locale.ROOT).equals(sought) || host.name().toLowerCase(Locale.ROOT)
                    .equals(sought) || host.hostname().toLowerCase(Locale.ROOT).equals(sought)) {
                return host;
            }
        }
        return null;
    }

    /** What the pool on {@code host} keeps. */
    public static MiningPool state(final ServerServices.Host host) {
        final MiningPool pool = host.rack().miningPoolAt(host.slot());
        return pool == null ? new MiningPool() : pool;
    }

    /** The pool's name: the one its owner gave it, or its server's until they do. */
    public static String name(final ServerServices.Host host) {
        final String given = state(host).name();
        return given.isEmpty() ? server(host) : given;
    }

    /**
     * The pool's server as the network lists it: the name a player gave it, or the one it goes by on the network,
     * so a pool on a server nobody named is still seen and told apart.
     */
    public static String server(final ServerServices.Host host) {
        return host.name().isEmpty() ? host.hostname() : host.name();
    }
}
