/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.HubBlockEntity;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.peripheral.PeripheralLine;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.BlockEntityTickers;
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
 * A hub of the peripheral line, a plain block with six equal faces. Cabled to a computer, it takes one of the
 * computer's device ports and offers its own, and every device cabled to it takes one of those instead: the Vintage
 * one is a switch box of two parallel ports, then a USB hub of four on the Legacy and the Transition and of seven on
 * the Standard and the Advanced. Screens and speakers do not pass through it, as no hub carries a video or an audio
 * signal.
 *
 * <p>Every face takes the peripheral cable of its era and of every earlier one, the cable up to the computer and the
 * cables down to the devices alike, and the cable after a hub reaches as far again as the cable before it.
 */
public class HubBlock extends Block implements EntityBlock, IFaceConnector {

    private final HardwareEra era;
    private final FacePorts ports;

    public static final MapCodec<HubBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            StableCodecs.byName(HardwareEra.class).fieldOf("era").forGetter(HubBlock::era)
    ).apply(i, HubBlock::new));

    public HubBlock(final Properties properties, final HardwareEra era) {
        super(properties);
        this.era = era;
        this.ports = FacePorts.builder().port(FaceRule.EVERY, PeripheralLine.of(era)).build();
    }

    /** How many devices a hub of {@code era} offers ports to. The numbers are first estimates. */
    public static int hubPorts(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> 2;
            case LEGACY, TRANSITION -> 4;
            case STANDARD, ADVANCED, EXA, SINGULARITY -> 7;
        };
    }

    /** The era the hub belongs to: the newest peripheral cable it takes. */
    public HardwareEra era() {
        return era;
    }

    /** How many devices this hub offers ports to. */
    public int hubPorts() {
        return hubPorts(era);
    }

    @Override
    public FacePorts ports() {
        return ports;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new HubBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return BlockEntityTickers.create(type, ComputingModule.HUB_BE.get(), HubBlockEntity::serverTick);
    }

    @Override
    protected MapCodec<? extends HubBlock> codec() {
        return CODEC;
    }

    /* A hub broken or replaced frees its port on its computer, so the devices behind it find their way gone. */
    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState,
                            final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SyncedBlockEntity synced) {
            synced.fields().broken(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
