/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.gateway.GatewayManager;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The Gateways linked to a machine, through which what runs on it reaches the ComputerCraft side.
 *
 * <p>A Gateway is a device of the machine's, so a program asks the machine for it: which ones there are, and the one
 * the program chose. They are looked up every time a program asks, which is how a Gateway linked or taken away since
 * the last call is noticed.
 */
public final class GatewayBridgeService {

    private final AbstractComputerBlockEntity machine;
    private final ServerLevel level;

    GatewayBridgeService(final AbstractComputerBlockEntity machine, final ServerLevel level) {
        this.machine = machine;
        this.level = level;
    }

    /** The Gateways linked to the machine, in the order the machine lists them. */
    public List<NetworkGatewayBlockEntity> all() {
        return GatewayManager.gatewaysOf(this.level, this.machine);
    }

    /**
     * The Gateway a program chose, found by its name whatever its case, or the first the machine lists when it chose
     * none.
     *
     * @return null when the machine has no such Gateway
     */
    @Nullable
    public NetworkGatewayBlockEntity pick(final String chosen) {
        final List<NetworkGatewayBlockEntity> mine = this.all();
        if (chosen.isEmpty()) {
            return mine.isEmpty() ? null : mine.getFirst();
        }
        for (final NetworkGatewayBlockEntity gateway : mine) {
            if (gateway.name().equalsIgnoreCase(chosen)) {
                return gateway;
            }
        }
        return null;
    }
}
