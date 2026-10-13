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
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A speaker: a peripheral on the computer's peripheral cable that carries its system's sound somewhere other than the
 * monitor, and adds to it. Two of them beside a monitor play a stereo recording a side each. A Legacy speaker samples
 * at 22 kHz and loses the bass and the treble; a Transition satellite loses only the bass, which a subwoofer against
 * one of its computer's satellites gives back; a Standard or an Advanced one plays the whole range.
 */
public class SpeakerBlock extends DeviceBlock implements IFaceConnector, IEraChassisBlock {

    private final HardwareEra era;
    private final FacePorts ports;

    public static final MapCodec<SpeakerBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            StableCodecs.byName(HardwareEra.class).fieldOf("era").forGetter(b -> b.era)
    ).apply(i, SpeakerBlock::new));

    /** Whether its power light is on, which its computer running decides. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** What a Legacy speaker reproduces: sampled at 22 kHz, nothing under 150 Hz or over 7 kHz. */
    private static final FrequencyResponse LEGACY_RESPONSE = new FrequencyResponse(22_050, 0, 150, 7_000);
    /** What a Transition satellite reproduces on its own: everything but the bass under 150 Hz. */
    private static final FrequencyResponse SATELLITE_RESPONSE = new FrequencyResponse(0, 0, 150, 0);

    /** The speaker's block entity, ticking to keep its link. */
    private static final Device<SpeakerBlockEntity> DEVICE =
            Device.of(() -> ComputingModule.SPEAKER_BE.get()).ticks(SpeakerBlockEntity::serverTick);

    public SpeakerBlock(final Properties properties, final HardwareEra era) {
        super(properties, DEVICE);
        this.era = era;
        this.ports = PeripheralSockets.back(era);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    /** The era of this speaker, which decides how well it plays. */
    public HardwareEra era() {
        return era;
    }

    /**
     * What this speaker reproduces of a recording, given whether a subwoofer stands against one of its computer's
     * Transition satellites, which matters to a Transition speaker alone.
     */
    public FrequencyResponse response(final boolean subwoofer) {
        if (era == HardwareEra.LEGACY) {
            return LEGACY_RESPONSE;
        }
        if (era == HardwareEra.TRANSITION && !subwoofer) {
            return SATELLITE_RESPONSE;
        }
        return FrequencyResponse.FULL;
    }

    @Override
    public HardwareEra chassisEra() {
        return era;
    }

    /** The audio jack of its era, in the middle of its back. */
    @Override
    public FacePorts ports() {
        return ports;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    /* Its screen opens with its name and its computer's, which the plain device menu does not carry. */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof SpeakerBlockEntity speaker) {
            final SpeakerMenu.Opening opening = speaker.opening((ServerLevel) level);
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new SpeakerMenu(id, inventory,
                    speaker, opening), getName()), opening::write);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
