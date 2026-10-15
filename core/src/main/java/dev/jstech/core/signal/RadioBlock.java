/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import dev.jstech.core.team.Ownership;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A radio for signals by frequency, as a mod gives one: a transmitter sends the strongest redstone fed into it, a
 * receiver gives out what it hears on every face, a relay sends again what it hears of its owner's team. Its power is
 * how far it sends in open air; each solid block in the way takes {@link RadioReach#OBSTACLE_COST} of it. Whoever
 * places it owns it, and its private and team frequencies are theirs.
 *
 * <p>A mod registers one {@link RadioBlockEntity} type for all its radios and hands it to each block.
 */
public class RadioBlock extends Block implements EntityBlock {

    private final IRadio.Role role;
    private final double power;
    private final Supplier<? extends BlockEntityType<? extends RadioBlockEntity>> type;

    public RadioBlock(final Properties properties, final IRadio.Role role, final double power,
                      final Supplier<? extends BlockEntityType<? extends RadioBlockEntity>> type) {
        super(properties);
        this.role = role;
        this.power = power;
        this.type = type;
    }

    /** What it does on the air. */
    public IRadio.Role role() {
        return role;
    }

    /** How far it sends in open air, in blocks. */
    public double power() {
        return power;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return type.get().create(pos, state);
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
                            @Nullable final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
            radio.own(Ownership.of(player.getUUID()));
        }
    }

    @Override
    protected boolean isSignalSource(final BlockState state) {
        return role == IRadio.Role.RECEIVER;
    }

    @Override
    protected int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos,
                            final Direction direction) {
        return role == IRadio.Role.RECEIVER && level.getBlockEntity(pos) instanceof RadioBlockEntity radio
                ? radio.signal() : 0;
    }

    /* Its block entity is made just after it is placed: a transmitter reads what is fed into it on the next tick. */
    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState old,
                           final boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (role == IRadio.Role.TRANSMITTER && !level.isClientSide()) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos,
                        final RandomSource random) {
        listen(level, pos);
    }

    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block neighbour,
                                   final BlockPos from, final boolean moved) {
        super.neighborChanged(state, level, pos, neighbour, from, moved);
        listen(level, pos);
    }

    /* A transmitter sends the strongest redstone fed into it from around it. */
    private void listen(final Level level, final BlockPos pos) {
        if (role == IRadio.Role.TRANSMITTER && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
            radio.send(level.getBestNeighborSignal(pos));
        }
    }
}
