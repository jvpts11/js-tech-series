/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.block.HubBlock;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.peripheral.IPeripheralHub;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.peripheral.PeripheralLink;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Behind a hub: its link to the computer it hangs from, on one of that computer's device ports, or on a port of the
 * hub before it. The devices cabled to it are linked to the same computer and kept there with this hub's position; the
 * hub itself keeps no list of them.
 */
public class HubBlockEntity extends SyncedBlockEntity implements IPeripheralHub {

    private final PeripheralLink link = new PeripheralLink(fields(), PeripheralCableType.COMPUTING,
            PeripheralLinks.COMPUTING);

    public HubBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.HUB_BE.get(), pos, state);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final HubBlockEntity hub) {
        if (level instanceof ServerLevel server) {
            hub.link.tick(server, pos);
        }
    }

    @Override
    public int hubPorts() {
        return getBlockState().getBlock() instanceof HubBlock hub ? hub.hubPorts() : 0;
    }

    @Override
    public PeripheralCableType cableType() {
        return link.cableType();
    }

    @Override
    public Optional<Long> linkedOwner() {
        return link.linkedOwner();
    }

    @Override
    public void onOwnerLinked(final long ownerPos) {
        link.linked(ownerPos);
    }

    @Override
    public void onOwnerUnlinked() {
        link.unlinked();
    }

    /** The computer it hangs from, or null while it hangs from none. */
    @Nullable
    public BlockPos ownerPos() {
        return link.ownerPos();
    }
}
