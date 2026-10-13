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
import dev.jstech.computers.blockentity.RepeaterBlockEntity;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A repeater of the data network, a plain block with six equal faces: laid in a run of cable, it renews the run's
 * range, so the cable after it starts counting again.
 *
 * <p>It takes every line of its era and of every earlier one but the long distance, whose two ends are its gateways.
 * Each line passes through it on its own: a repeater carries a line on, never joins one line to another, which is a
 * router's work. Two eras of a line meet here, at the repeater of the newer one.
 */
public class RepeaterBlock extends Block implements EntityBlock, IFaceConnector {

    /** The lines a repeater carries on. */
    public static final List<DataLine> LINES = List.of(DataLine.ACCESS, DataLine.BACKBONE, DataLine.HPC,
            DataLine.CRAFTING);

    private final HardwareEra era;
    private final FacePorts ports;

    public static final MapCodec<RepeaterBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            StableCodecs.byName(HardwareEra.class).fieldOf("era").forGetter(RepeaterBlock::era)
    ).apply(i, RepeaterBlock::new));

    public RepeaterBlock(final Properties properties, final HardwareEra era) {
        super(properties);
        this.era = era;
        this.ports = FacePorts.everyFace(DataLines.upTo(era, LINES.toArray(DataLine[]::new)));
    }

    /** The era the repeater belongs to: the newest cables it takes. */
    public HardwareEra era() {
        return era;
    }

    @Override
    protected MapCodec<? extends RepeaterBlock> codec() {
        return CODEC;
    }

    @Override
    public FacePorts ports() {
        return ports;
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            DataWires.removeRepeater(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new RepeaterBlockEntity(pos, state);
    }
}
