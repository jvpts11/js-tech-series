/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.network;

import dev.jstech.core.inspect.InspectTexts;
import dev.jstech.core.inspect.Inspectors;
import dev.jstech.core.log.CoreLogs;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.ApiStatus;

/** The data network's part of an inspection: the node a member is, the network it is on and how many share it. */
public final class NetworkInspection {

    private NetworkInspection() {
    }

    /** Adds the part, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void declare() {
        Inspectors.provide(CoreLogs.NETWORK, NetworkInspection::network);
    }

    private static void network(final ServerLevel level, final BlockPos pos, final Inspectors.Inspection report) {
        final Optional<NetworkMembership> member = NetworkMembers.at(level, pos);
        if (member.isEmpty()) {
            return;
        }
        report.under(InspectTexts.NETWORK.text(), lines -> {
            lines.line(InspectTexts.NODE.text(), member.get().node().value());
            member.get().network().ifPresentOrElse(network -> {
                lines.line(InspectTexts.NETWORK.text(), network.value());
                lines.line(InspectTexts.MEMBERS.text(), NetworkMembers.on(level, network));
            }, () -> lines.line(InspectTexts.OFF_NETWORK.text()));
        });
    }
}
