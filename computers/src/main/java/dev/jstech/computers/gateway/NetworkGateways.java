/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The Gateways a computer has on its peripheral ports, the way the Gateway Manager and the {@code gateway}
 * command find them.
 */
public final class NetworkGateways {

    private NetworkGateways() {
    }

    /**
     * Every Gateway linked to {@code owner}, in the order the owner lists its endpoints, the disabled ones too: what
     * numbers them, so a Gateway keeps its default name while another is disabled.
     */
    public static List<NetworkGatewayBlockEntity> linkedTo(final Level level, final IPeripheralOwner owner) {
        return gatewaysAt(level, owner.linkedEndpoints());
    }

    /** The Gateways {@code owner} reads and writes, in the order it lists its endpoints: every one not disabled. */
    public static List<NetworkGatewayBlockEntity> enabledOn(final Level level, final IPeripheralOwner owner) {
        return gatewaysAt(level, owner.enabledEndpoints());
    }

    /** The enabled Gateway on {@code owner} called {@code name} (any case), or null. */
    @Nullable
    public static NetworkGatewayBlockEntity named(final Level level, final IPeripheralOwner owner, final String name) {
        for (final NetworkGatewayBlockEntity gateway : enabledOn(level, owner)) {
            if (gateway.name().equalsIgnoreCase(name)) {
                return gateway;
            }
        }
        return null;
    }

    /** The enabled Gateway on {@code owner} standing at {@code pos}, or null when none of the owner's is there. */
    @Nullable
    public static NetworkGatewayBlockEntity at(final Level level, final IPeripheralOwner owner, final long pos) {
        for (final NetworkGatewayBlockEntity gateway : enabledOn(level, owner)) {
            if (gateway.getBlockPos().asLong() == pos) {
                return gateway;
            }
        }
        return null;
    }

    private static List<NetworkGatewayBlockEntity> gatewaysAt(final Level level, final List<Long> endpoints) {
        final List<NetworkGatewayBlockEntity> out = new ArrayList<>();
        for (final long endpoint : endpoints) {
            if (Loaded.blockEntity(level, BlockPos.of(endpoint)) instanceof NetworkGatewayBlockEntity gateway) {
                out.add(gateway);
            }
        }
        return out;
    }
}
