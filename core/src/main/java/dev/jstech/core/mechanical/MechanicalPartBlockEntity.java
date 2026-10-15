/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block entity of a part of the Core's kit ({@link MechanicalPartBlock}): it turns with its network, its speed
 * saved and sent to the players who see it, and meets its neighbours as its block's spec and axis say.
 */
public class MechanicalPartBlockEntity extends SyncedBlockEntity implements IMechanicalPart {

    private final ValueField<Double> rpm;

    /* What a block that is no part of the kit is taken as: a shaft that weighs and holds back nothing. */
    private static final MechanicalPartSpec NOTHING = MechanicalPartSpec.shaft(0.0, 0.0);

    public MechanicalPartBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.rpm = fields().value("Rpm", Codec.DOUBLE, 0.0).save().toClient();
        MechanicalPorts.keep(this);
    }

    /** What the part is, as its block says. */
    public MechanicalPartSpec spec() {
        return getBlockState().getBlock() instanceof MechanicalPartBlock block ? block.spec() : NOTHING;
    }

    @Override
    public ShaftContact contact(final Direction face) {
        final BlockState state = getBlockState();
        return state.hasProperty(RotatedPillarBlock.AXIS) ? spec().contact(state.getValue(RotatedPillarBlock.AXIS),
                face) : ShaftContact.NONE;
    }

    @Override
    public double inertia() {
        return spec().inertia();
    }

    @Override
    public double friction() {
        return spec().friction();
    }

    @Override
    public double rpm() {
        return this.rpm.get();
    }

    @Override
    public void turning(final double rpm) {
        this.rpm.set(rpm);
    }
}
