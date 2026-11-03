/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.multiblock.MultiblockPortBlock;
import dev.jstech.core.text.GameText;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Item Port's block: used, it opens the port's screen, its hold and what the build still lacks; on the server it
 * passes what its hold holds on to the dock's arms every tick.
 */
public final class ItemPortBlock extends MultiblockPortBlock {

    public ItemPortBlock(final Properties properties) {
        super(properties, () -> HangarBlocks.ITEM_PORT_ENTITY.get());
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ItemPortBlockEntity port)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer opener) {
            opener.openMenu(new SimpleMenuProvider((id, inventory, user) -> new ItemPortMenu(id, inventory, port),
                    GameText.component(HangarTexts.ITEM_PORT)), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                          final BlockEntityType<T> type) {
        if (level.isClientSide() || type != HangarBlocks.ITEM_PORT_ENTITY.get()) {
            return null;
        }
        return (at, pos, blockState, entity) -> ((ItemPortBlockEntity) entity).tick((ServerLevel) at);
    }
}
