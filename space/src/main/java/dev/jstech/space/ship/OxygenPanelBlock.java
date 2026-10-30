/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.room.RoomLiving;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.core.text.GameText;
import dev.jstech.space.api.SpaceSuit;
import dev.jstech.space.life.LifeSupport;
import dev.jstech.space.life.SuitAir;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The airlock's oxygen panel: a click fills the tanks of the suit its user wears from the oxygen of the cabin's air, as
 * an oxygen bottle fills from a sealed room, taking the oxygen out of it.
 */
public final class OxygenPanelBlock extends ShipFixtureBlock {

    public OxygenPanelBlock(final Properties properties) {
        super(properties);
    }

    /**
     * Fills the tanks of the suit {@code player} wears from the air of the cabin beside {@code pos}; how many seconds
     * of breathing went in.
     */
    public static int fill(final ServerLevel level, final BlockPos pos, final Player player) {
        final ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        final Optional<SpaceSuit> suit = SuitAir.suitOf(level, chest);
        if (suit.isEmpty()) {
            return 0;
        }
        final int missing = LifeSupport.capacity(level, chest, suit.get()) - LifeSupport.air(level, chest, suit.get());
        if (missing <= 0) {
            return 0;
        }
        final double moles = SealedRooms.take(level, pos, AirMix.OXYGEN, missing * RoomLiving.PLAYER_BREATH);
        return SuitAir.refill(level, chest, (int) Math.floor(moles / RoomLiving.PLAYER_BREATH));
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        final ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        final Optional<SpaceSuit> suit = SuitAir.suitOf(level, chest);
        if (suit.isEmpty()) {
            player.displayClientMessage(GameText.component(ShipFixtureTexts.NO_SUIT), true);
        } else if (LifeSupport.air(level, chest, suit.get()) >= LifeSupport.capacity(level, chest, suit.get())) {
            player.displayClientMessage(GameText.component(ShipFixtureTexts.FULL), true);
        } else {
            final int filled = fill(server, pos, player);
            player.displayClientMessage(GameText.component(filled > 0 ? ShipFixtureTexts.FILLED.with(filled)
                    : ShipFixtureTexts.NO_OXYGEN.text()), true);
        }
        return InteractionResult.CONSUME;
    }
}
