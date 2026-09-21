/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.CraftingSwitchBlockEntity;
import dev.jstech.computers.menu.CraftingSwitchMenu;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.network.IDataNetworkConnectable;
import dev.jstech.core.util.BlockEntityTickers;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Crafting Switch block: declares up to five adjacent machines for the network, and carries one crafting
 * cable face to its Crafting Computer. Holds a {@link CraftingSwitchBlockEntity} that surveys its faces and
 * discovers the computer over the crafting cable.
 */
public class CraftingSwitchBlock extends Block
        implements EntityBlock, IDataNetworkConnectable {

    public CraftingSwitchBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public Set<DataTier> acceptedCableTiers() {
        // Only the crafting cable attaches (on any face); the switch is not a data-network device.
        return Set.of(DataTier.CRAFTING);
    }

    @Override
    protected InteractionResult useWithoutItem(
            final BlockState state, final Level level, final BlockPos pos,
            final Player player,
            final BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CraftingSwitchBlockEntity) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new CraftingSwitchMenu(
                                    id, inventory, level, pos),
                            Component.translatable("block.jsc.crafting_switch")),
                    buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new CraftingSwitchBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return BlockEntityTickers.create(type, ComputingModule.CRAFTING_SWITCH_BE.get(),
                CraftingSwitchBlockEntity::serverTick);
    }
}
