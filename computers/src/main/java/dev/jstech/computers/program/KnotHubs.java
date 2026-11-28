/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.wan.FederationPath;
import dev.jstech.computers.wan.WanFederation;
import dev.jstech.computers.wan.WanRight;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The KnotHub a machine reaches, in Knot's window or at its prompt: its own network's, or one on a network it is
 * federated with. Its own network's takes everything; across a federation the port of the hub's network decides, as
 * for items: a machine reads the repository (looks, pulls, reads the log) where it grants seeing, and sends it a
 * revision where it grants sending.
 */
public final class KnotHubs {

    private KnotHubs() {
    }

    /**
     * The KnotHub reached from {@code network}, or null when none is: its own network's first, then the nearest one a
     * federation reaches.
     */
    @Nullable
    public static Reach of(final ServerLevel level, @Nullable final NetworkUuid network) {
        if (network == null) {
            return null;
        }
        final ServerServices.Host own = ServerServices.find(level, network, Programs.KNOT_HUB);
        if (own != null) {
            return new Reach(own, "", true, true);
        }
        for (final FederationPath path : WanFederation.of(level).reachable(network, level.getGameTime())) {
            final ServerServices.Host far = ServerServices.find(level, path.target(), Programs.KNOT_HUB);
            if (far != null) {
                return new Reach(far, WanFederation.farName(level, path), path.allows(WanRight.SEE),
                        path.allows(WanRight.SEND));
            }
        }
        return null;
    }

    /**
     * A KnotHub reached: the machine running it, its network as the federation names it (empty when it is the
     * machine's own), and whether the machine may read it and send it a revision.
     */
    public record Reach(ServerServices.Host host, String network, boolean see, boolean send) {
    }
}
