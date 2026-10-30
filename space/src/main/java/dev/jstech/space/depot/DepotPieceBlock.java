/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.depot;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A piece of a propellant depot: a Sphere Shell, a Tank Leg or a Vent Stack, a block of its kit on its own; once its
 * depot is whole, formed, showing nothing of itself while the depot's transfer pump draws the sphere whole on its legs.
 */
public final class DepotPieceBlock extends Block {

    private static final MapCodec<DepotPieceBlock> CODEC = simpleCodec(DepotPieceBlock::new);

    /** Whether it is part of a whole depot. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public DepotPieceBlock(final Properties properties) {
        // Formed, it shows nothing, so it hides nothing of the blocks round it either.
        super(properties.noOcclusion());
        registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }
}
