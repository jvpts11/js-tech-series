/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The Lead-Acid Battery Bank's block: a store of J's Energy that shows its charge on the gauge of every side, as its
 * block state's charge says.
 */
public final class BatteryBankBlock extends DeviceBlock {

    /** How many steps its gauge shows. */
    public static final int STEPS = 4;
    /** Its charge, from empty to full, as its gauge shows it. */
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, STEPS);

    private static final Device<BatteryBankBlockEntity> DEVICE = Device.of(() -> SharedEnergy.BATTERY_BE.get());
    private static final MapCodec<BatteryBankBlock> CODEC = simpleCodec(BatteryBankBlock::new);

    public BatteryBankBlock(final Properties properties) {
        super(properties, DEVICE);
        registerDefaultState(defaultBlockState().setValue(CHARGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CHARGE);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
