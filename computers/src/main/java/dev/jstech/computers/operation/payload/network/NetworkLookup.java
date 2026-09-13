/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.network;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Finds the Mainframe of a network and the names shown for the network, its servers and its computers.
 */
public final class NetworkLookup {

    private NetworkLookup() {
    }

    public static String networkLabel(final NetworkUuid net) {
        return "jsc-net-" + dev.jstech.core.util.ShortId.of(net.asString());
    }

    public static MainframeBlockEntity resolveMainframe(final ServerLevel level, final NetworkUuid network) {
        final java.util.Optional<Long> pos = NetworkSystem.get(level).mainframePositionOf(network);
        if (pos.isEmpty()) {
            return null;
        }
        return level.getBlockEntity(net.minecraft.core.BlockPos.of(pos.get())) instanceof MainframeBlockEntity mf
                ? mf : null;
    }

    public static String pcLabel(final PersonalComputerBlockEntity pc, final NodeUuid node) {
        return pc.customName().isEmpty() ? "PC-" + ShortId.of(node.asString()) : pc.customName();
    }

    public static String serverLabel(final ServerLevel level, final NodeUuid node) {
        final String fallback = "SRV-" + ShortId.of(node.asString());
        return dev.jstech.core.network.NetworkSystem.get(level).locationOf(node)
                .map(loc -> level.getBlockEntity(net.minecraft.core.BlockPos.of(loc.rackPos()))
                        instanceof dev.jstech.computers.blockentity.ServerRackBlockEntity rack
                        ? rack.getServers().getStackInSlot(loc.slot()) : ItemStack.EMPTY)
                .map(dev.jstech.computers.item.ServerItem::customName)
                .filter(name -> !name.isEmpty())
                .orElse(fallback);
    }
}
