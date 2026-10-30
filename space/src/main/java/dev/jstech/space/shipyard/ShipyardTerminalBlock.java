/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.shipyard;

import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Small Shipyard's terminal block: once its dock stands whole it opens the Starship Design, its builder; till then,
 * the machine's screen, which says what the dock still lacks.
 */
public final class ShipyardTerminalBlock extends DeviceBlock {

    public ShipyardTerminalBlock(final Properties properties, final Device<?> device) {
        super(properties, device);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ShipyardTerminalBlockEntity terminal && terminal.formed()) {
            if (!level.isClientSide() && player instanceof ServerPlayer server) {
                ShipDesigner.open(server, terminal);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
