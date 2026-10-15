/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.containment;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A block of containment: the containment of exotic matter, a whole block, or the Antimatter Trap, a Penning trap on its
 * base plate, its chamber under the magnet's cap.
 */
public final class ContainmentBlock extends DeviceBlock {

    private final boolean antimatter;
    private final MapCodec<ContainmentBlock> codec;

    private static final Device<ContainmentBlockEntity> DEVICE = Device.of(
            () -> SharedContainment.CONTAINMENT_BE.get()).ticks((level, pos, state, containment) -> containment.tick());
    private static final VoxelShape TRAP = Shapes.or(Block.box(1, 0, 1, 15, 2, 15), Block.box(3, 2, 3, 13, 13, 13),
            Block.box(2, 13, 2, 14, 15, 14));

    /** A block of containment of antimatter alone, the trap, or of the other exotic forms. */
    public ContainmentBlock(final Properties properties, final boolean antimatter) {
        super(properties, DEVICE);
        this.antimatter = antimatter;
        this.codec = simpleCodec(made -> new ContainmentBlock(made, antimatter));
    }

    /** Whether it keeps antimatter alone, as the trap. */
    public boolean antimatter() {
        return this.antimatter;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return this.antimatter ? TRAP : Shapes.block();
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
    }
}
