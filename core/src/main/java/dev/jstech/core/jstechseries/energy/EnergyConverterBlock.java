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
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * An energy converter's block: a cabinet that faces whoever placed it, its FE side on their left as they face it and its
 * J's Energy side on their right, as its screen shows them. A fixed converter is of one class; a configurable one shows
 * the class it is set to on its J's Energy side, as its block state's voltage says.
 */
public final class EnergyConverterBlock extends DeviceBlock {

    private final @Nullable VoltageClass fixed;
    private final MapCodec<EnergyConverterBlock> codec;

    /** The rank of the class it is set to, 1 for the low class to 5 for the ultra-high one. */
    public static final IntegerProperty VOLTAGE = IntegerProperty.create("voltage", 1, 5);
    private static final Device<EnergyConverterBlockEntity> DEVICE = Device.of(
            () -> EnergyConverters.CONVERTER_BE.get()).ticks((level, pos, state, converter) -> converter.tick())
            .opensMenu(EnergyConverterMenu::new);

    /** A converter of {@code fixed}, or a configurable one for none. */
    public EnergyConverterBlock(final Properties properties, final @Nullable VoltageClass fixed) {
        super(properties, DEVICE);
        this.fixed = fixed;
        this.codec = simpleCodec(made -> new EnergyConverterBlock(made, fixed));
        registerDefaultState(defaultBlockState().setValue(VOLTAGE, (fixed == null ? VoltageClass.LOW : fixed).rank()));
    }

    /** Its class, for a fixed converter; none for a configurable one. */
    public Optional<VoltageClass> fixedClass() {
        return Optional.ofNullable(this.fixed);
    }

    /** The face of a converter facing {@code facing} that speaks FE: the left of whoever faces it. */
    public static Direction feFace(final Direction facing) {
        return facing.getClockWise();
    }

    /** The face that speaks J's Energy: the right of whoever faces it. */
    public static Direction jeFace(final Direction facing) {
        return facing.getCounterClockWise();
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VOLTAGE);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
    }
}
