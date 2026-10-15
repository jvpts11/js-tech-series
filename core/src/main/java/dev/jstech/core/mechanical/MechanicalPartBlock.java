/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.connect.Neighbours;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A part of the Core's kit of mechanical power: a shaft, a flywheel, a gear, a gearbox, a clutch, a brake, a speed
 * controller, a shear pin or a pulley ({@link MechanicalPartSpec}), set along the axis of the face it was placed
 * against, and its block entity of the type its mod registers. A sheared pin is pinned again by using it. Its model
 * and recipe are its mod's.
 *
 * <pre>{@code
 * BlockEntry<MechanicalPartBlock> IRON_SHAFT = CONTENT.block("iron_shaft", properties -> new MechanicalPartBlock(
 *         properties, MechanicalPartSpec.shaft(0.005, 0.1), () -> PART.get())).named("Iron Shaft").register();
 * DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalPartBlockEntity>> PART = CONTENT.blockEntity("part",
 *         (pos, state) -> new MechanicalPartBlockEntity(PART.get(), pos, state), IRON_SHAFT, GEAR, GEARBOX);
 * }</pre>
 */
public class MechanicalPartBlock extends RotatedPillarBlock implements EntityBlock {

    private final MechanicalPartSpec spec;
    private final Supplier<? extends BlockEntityType<? extends MechanicalPartBlockEntity>> type;

    public MechanicalPartBlock(final Properties properties, final MechanicalPartSpec spec,
                               final Supplier<? extends BlockEntityType<? extends MechanicalPartBlockEntity>> type) {
        super(properties);
        this.spec = Objects.requireNonNull(spec, "spec");
        this.type = Objects.requireNonNull(type, "type");
    }

    /** What the part is. */
    public MechanicalPartSpec spec() {
        return this.spec;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return this.type.get().create(pos, state);
    }

    /* The part hears which of its faces saw its neighbour change, so its network is joined again. */
    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                   final Block neighbourBlock, final BlockPos neighbourPos,
                                   final boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbourBlock, neighbourPos, movedByPiston);
        final Direction face = Neighbours.faceTowards(pos, neighbourPos);
        if (face != null && level.getBlockEntity(pos) instanceof SyncedBlockEntity synced) {
            synced.fields().neighbourChanged(level, face);
        }
    }

    /* A sheared pin is pinned again by hand, as a fuse is changed. */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MechanicalPartBlockEntity part && part.sheared()) {
            if (!level.isClientSide()) {
                part.repin();
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState,
                            final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SyncedBlockEntity synced) {
            synced.fields().broken(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
