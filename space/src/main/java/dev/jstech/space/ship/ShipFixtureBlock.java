/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.multiblock.plan.IPlanMember;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A block of a starship's kit that works aboard, a suit locker, an oxygen panel, a bunk, so it never merges into its
 * part's formed model: once its part is formed the model draws it with the furniture round it, so the block draws
 * nothing and lets the light through, while it still stands where it is and a click still works it. Its block entity
 * keeps the block of the model it stands for ({@link IFixtureEntity}), so it collides, is aimed at and is outlined as
 * the model draws it there, not as a whole block.
 */
public abstract class ShipFixtureBlock extends Block implements IPlanMember, EntityBlock {

    /** Whether its part is formed, its model drawing it. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    protected ShipFixtureBlock(final Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    public void partFormed(final ServerLevel level, final BlockPos pos, final boolean formed) {
        final BlockState state = level.getBlockState(pos);
        if (state.hasProperty(FORMED) && state.getValue(FORMED) != formed) {
            level.setBlock(pos, state.setValue(FORMED, formed), Block.UPDATE_CLIENTS);
        }
    }

    /* Formed, it keeps the block of its part's model it stands for; come apart, it lets it go. */
    @Override
    public void partFormed(final ServerLevel level, final BlockPos pos, final boolean formed,
                           final @Nullable FormedPlace place) {
        partFormed(level, pos, formed);
        if (level.getBlockEntity(pos) instanceof IFixtureEntity fixture) {
            fixture.place(formed ? place : null);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new ShipFixtureEntity(pos, state);
    }

    /* Formed, it stands as its block of the model, as the model draws it there; whole otherwise. */
    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        if (state.getValue(FORMED) && level.getBlockEntity(pos) instanceof IFixtureEntity fixture
                && fixture instanceof BlockEntity entity && entity.getLevel() != null) {
            final VoxelShape shape = shape(entity.getLevel(), fixture);
            if (shape != null) {
                return shape;
            }
        }
        return super.getShape(state, level, pos, context);
    }

    @Override
    protected VoxelShape getOcclusionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return state.getValue(FORMED) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }

    /* The shape of the block of the model {@code fixture} stands for, where its shape file is known; none else. */
    private static @Nullable VoxelShape shape(final Level level, final IFixtureEntity fixture) {
        for (final FormedPlace place : fixture.places()) {
            final Optional<FormedShape> file = FormedShapes.get(level, place.model());
            if (file.isPresent() && file.get().cells().containsKey(place.cell())) {
                return file.get().shape(place);
            }
        }
        return null;
    }
}
