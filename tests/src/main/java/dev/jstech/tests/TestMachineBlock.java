/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A processing machine of the test mod, which works in a few ticks and needs no power, so the autocraft tests can run
 * real recipes through Crafting Interfaces and Receiving Buses instead of placing outputs by hand.
 */
public final class TestMachineBlock extends Block implements EntityBlock {

    private final TestMachineBlockEntity.Kind kind;

    public TestMachineBlock(final Properties properties, final TestMachineBlockEntity.Kind kind) {
        super(properties);
        this.kind = kind;
    }

    /** What the machine makes and through which faces it takes its inputs. */
    public TestMachineBlockEntity.Kind kind() {
        return kind;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new TestMachineBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        if (level.isClientSide() || type != TestMachines.MACHINE.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> ((TestMachineBlockEntity) entity).serverTick();
    }
}
