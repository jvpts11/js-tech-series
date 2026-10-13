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
import dev.jstech.computers.blockentity.HbwInterfaceBlockEntity;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.BlockEntityTickers;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The HBW Interface: the single point where a Supercomputer cluster meets the network. Each one has an era: the
 * Standard's, and the Advanced's that takes the OSFP fabric.
 */
@TextHolder
public class HbwInterfaceBlock extends Block implements EntityBlock, IFaceConnector {

    private final HardwareEra era;
    /* The backbone on one side, the cluster fabric on the other, each up to the cable of the interface's era. */
    private final FacePorts ports;

    public static final MapCodec<HbwInterfaceBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            StableCodecs.byName(HardwareEra.class).fieldOf("era").forGetter(HbwInterfaceBlock::era)
    ).apply(i, HbwInterfaceBlock::new));

    private static final TextKey TOOLTIP =
            TextKey.of("item.jsc.hbw_interface.tooltip", "Uplinks a node cluster to the HBW backbone");

    public HbwInterfaceBlock(final Properties properties, final HardwareEra era) {
        super(properties);
        this.era = era;
        this.ports = FacePorts.everyFace(DataLines.upTo(era, DataLine.BACKBONE, DataLine.HPC));
    }

    /** The era the interface belongs to: the newest backbone and fabric cables it takes. */
    public HardwareEra era() {
        return era;
    }

    @Override
    protected MapCodec<HbwInterfaceBlock> codec() {
        return CODEC;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(GameText.component(TOOLTIP).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public FacePorts ports() {
        return ports;
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof HbwInterfaceBlockEntity be) {
            be.onBroken(serverLevel);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new HbwInterfaceBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return BlockEntityTickers.create(type, ComputingModule.HBW_INTERFACE_BE.get(),
                HbwInterfaceBlockEntity::serverTick);
    }

}
