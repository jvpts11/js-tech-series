/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.text.GameText;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A block of the Rocket Design table, on the step at the back of the Rocket Hangar's control room, where a rocket is
 * drawn: its top is dark glass until its hangar forms, when the hangar's terminal lights it with a blueprint. Used, it
 * opens its hangar's design screen, as the terminal does, or says it stands in no formed hangar.
 */
public final class RocketDesignTableBlock extends HangarFittingBlock {

    private final MapCodec<RocketDesignTableBlock> codec;

    /** Its shape: the step's half block under it, the table's body over that to its top. */
    public static final int[][] BOXES = {{0, 0, 0, 16, 22, 16}};

    public RocketDesignTableBlock(final BlockBehaviour.Properties properties) {
        super(properties, BOXES);
        this.codec = simpleCodec(RocketDesignTableBlock::new);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        final Optional<HangarTerminalBlockEntity> hangar = HangarTerminalBlockEntity.holding(level, pos);
        if (hangar.isPresent() && player instanceof ServerPlayer designer) {
            RocketDesigner.open(designer, hangar.get(), pos);
        } else {
            player.displayClientMessage(GameText.component(HangarTexts.NO_HANGAR), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
    }
}
