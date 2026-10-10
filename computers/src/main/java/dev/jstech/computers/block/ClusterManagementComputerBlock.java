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
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.menu.ClusterManagementComputerMenu;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.peripheral.PeripheralLine;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * The Cluster Management Computer: a full computer whose job is the racks. It sits on the data network
 * like any other machine and, with a Cluster Interface Card installed, drives every supercomputer
 * fabric and datacenter section its network reaches, installing systems and programs on all their
 * nodes, switching bays, watching queues. Without the card it is an ordinary computer. A cluster
 * works without one; the computer makes it one machine to run.
 */
public class ClusterManagementComputerBlock extends AbstractSmallComputerBlock<ClusterManagementComputerBlockEntity> {

    public static final MapCodec<ClusterManagementComputerBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            StableCodecs.byName(HardwareEra.class).fieldOf("era").forGetter(ClusterManagementComputerBlock::era),
            StableCodecs.byName(CaseStyle.class).fieldOf("case").forGetter(ClusterManagementComputerBlock::caseStyle)
    ).apply(i, ClusterManagementComputerBlock::new));

    /*
     * A management machine lives on the network, on its back: its era's access line through a router, or the
     * backbone directly, the fibre only with an Optical Network Card. Never the compute fabric; the racks are reached
     * over the network.
     */
    public ClusterManagementComputerBlock(final Properties properties, final HardwareEra era,
                                          final CaseStyle caseStyle) {
        super(properties, era, caseStyle,
                FacePorts.builder()
                        .port(FaceRule.BACK, DataLines.upTo(era, DataLine.ACCESS, DataLine.BACKBONE))
                        .port(FaceRule.EVERY, PeripheralLine.of(era))
                        .build(),
                ClusterManagementComputerBlockEntity.class, ComputingModule.CLUSTER_MANAGEMENT_COMPUTER_BE::get,
                ClusterManagementComputerBlockEntity::serverTick);
        registerDefaultState(defaultBlockState().setValue(OpticalPort.OPTICAL, false));
    }

    @Override
    public String machineName() {
        return "cluster_management_computer";
    }

    @Override
    public boolean accepts(final BlockState state, final Direction face, final Connection offered) {
        return super.accepts(state, face, offered) && OpticalPort.admits(state, offered);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new ClusterManagementComputerBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends ClusterManagementComputerBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OpticalPort.OPTICAL);
    }

    @Override
    protected AbstractContainerMenu createMenu(final int containerId, final Inventory inventory,
                                               final ClusterManagementComputerBlockEntity computer) {
        return new ClusterManagementComputerMenu(containerId, inventory, computer);
    }

    @Override
    protected Component menuTitle() {
        return Component.translatable("block.jsc.cluster_management_computer");
    }
}
