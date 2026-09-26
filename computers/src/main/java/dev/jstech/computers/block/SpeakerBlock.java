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
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.computers.menu.SpeakerMenu;
import dev.jstech.core.audio.FrequencyResponse;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.peripheral.IPeripheralConnectable;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.BlockEntityTickers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A speaker: a peripheral on the computer's peripheral cable that carries its system's sound somewhere other than the
 * monitor, and adds to it. Two of them beside a monitor play a stereo recording a side each. A Legacy speaker samples
 * at 22 kHz and loses the bass and the treble; a Standard one plays the whole range.
 */
public class SpeakerBlock extends HorizontalDirectionalBlock implements EntityBlock, IPeripheralConnectable,
        IEraChassisBlock {

    private final HardwareEra era;

    public static final MapCodec<SpeakerBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            StableCodecs.byName(HardwareEra.class).fieldOf("era").forGetter(b -> b.era)
    ).apply(i, SpeakerBlock::new));

    /** What a Legacy speaker reproduces: sampled at 22 kHz, nothing under 150 Hz or over 7 kHz. */
    private static final FrequencyResponse LEGACY_RESPONSE = new FrequencyResponse(22_050, 0, 150, 7_000);

    public SpeakerBlock(final Properties properties, final HardwareEra era) {
        super(properties);
        this.era = era;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** The era of this speaker, which decides how well it plays. */
    public HardwareEra era() {
        return era;
    }

    /** What this speaker reproduces of a recording. */
    public FrequencyResponse response() {
        return era == HardwareEra.LEGACY ? LEGACY_RESPONSE : FrequencyResponse.FULL;
    }

    @Override
    public HardwareEra chassisEra() {
        return era;
    }

    @Override
    public PeripheralCableType peripheralType() {
        return PeripheralCableType.COMPUTING;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new SpeakerBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return BlockEntityTickers.create(type, ComputingModule.SPEAKER_BE.get(), SpeakerBlockEntity::serverTick);
    }

    @Override
    protected MapCodec<SpeakerBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof SpeakerBlockEntity speaker) {
            final SpeakerMenu.Opening opening = speaker.opening((ServerLevel) level);
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new SpeakerMenu(id, inventory,
                            speaker, opening), Component.translatable(getDescriptionId())),
                    opening::write);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof SpeakerBlockEntity speaker) {
            speaker.unlink(serverLevel);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
