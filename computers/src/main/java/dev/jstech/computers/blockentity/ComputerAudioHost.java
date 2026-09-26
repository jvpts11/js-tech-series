/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.audio.ComputingAudioDevices;
import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.audio.SpeakerSides;
import dev.jstech.computers.audio.SystemSound;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.SoundCardSpec;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.AudioDevice;
import dev.jstech.core.audio.AudioDevices;
import dev.jstech.core.audio.AudioOutput;
import dev.jstech.core.audio.IAudioHost;
import dev.jstech.core.audio.SoundContext;
import dev.jstech.core.audio.StereoSide;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A computer as the source of its system's sound: the device that plays it and where it comes out. A sound card, or
 * the sound built into a Standard board, plays out of the monitors linked to the machine and out of its speakers,
 * which add to the monitors. Two or more speakers play a stereo recording a side each, by where they stand against
 * the monitor; a Legacy speaker plays it coarser than a Standard one. With no monitor and no speaker there is only
 * the speaker inside the case, which beeps and plays no recording.
 */
final class ComputerAudioHost implements IAudioHost {

    private final AbstractComputerBlockEntity machine;

    ComputerAudioHost(final AbstractComputerBlockEntity machine) {
        this.machine = machine;
    }

    @Override
    public AudioDevice audioDevice() {
        final ComputerBuild build = machine.currentBuild();
        if (build == null) {
            return AudioDevices.NONE;
        }
        if (monitors().isEmpty() && speakers().isEmpty()) {
            return ComputingAudioDevices.PC_SPEAKER;
        }
        for (final IExpansionCardSpec card : build.cardsOfKind(ExpansionCardKind.SOUND)) {
            if (card instanceof SoundCardSpec sound) {
                return ComputingAudioDevices.of(sound);
            }
        }
        return build.motherboard().era() == HardwareEra.STANDARD
                ? ComputingAudioDevices.ON_BOARD : ComputingAudioDevices.PC_SPEAKER;
    }

    @Override
    public float audioVolume() {
        return 1.0F;
    }

    @Override
    public List<Vec3> audioOutputs() {
        return outputs().stream().map(AudioOutput::position).toList();
    }

    @Override
    public List<AudioOutput> outputs() {
        final List<AudioOutput> outputs = new ArrayList<>();
        for (final BlockPos monitor : monitors()) {
            outputs.add(AudioOutput.at(Vec3.atCenterOf(monitor)));
        }
        final List<SpeakerBlockEntity> speakers = speakers();
        for (final SpeakerBlockEntity speaker : speakers) {
            outputs.add(new AudioOutput(Vec3.atCenterOf(speaker.getBlockPos()),
                    sideOf(speaker.getBlockPos(), speakers.size()), speaker.response()));
        }
        if (outputs.isEmpty()) {
            outputs.add(AudioOutput.at(Vec3.atCenterOf(machine.getBlockPos())));
        }
        return outputs;
    }

    @Override
    public SoundContext soundContext() {
        SoundContext context = SoundContext.EMPTY;
        final HardwareEra era = machine.installedEra();
        if (era != null) {
            context = context.with(SoundContext.ERA, era.serializedName());
        }
        final ResourceLocation system = machine.installedOsId();
        if (system != null) {
            context = context.with(ComputingSounds.SYSTEM, system.toString());
        }
        return context;
    }

    /** Plays one of the system's own sounds, when the machine's sound hardware plays recordings at all. */
    void play(final ServerLevel level, final SystemSound sound) {
        if (audioDevice().samples()) {
            Audio.cue(level, this, sound.cue());
        }
    }

    /** How many speakers the machine has. */
    int speakerCount() {
        return speakers().size();
    }

    /** Which side of a stereo recording the speaker at {@code speaker} plays for this machine. */
    StereoSide sideOf(final BlockPos speaker) {
        return sideOf(speaker, speakerCount());
    }

    /*
     * The side, by where the speaker stands against the first monitor: the one on the left of someone sitting at the
     * screen plays the left. The monitor faces away from whoever placed it, so they look along its facing.
     */
    private StereoSide sideOf(final BlockPos speaker, final int speakers) {
        final List<BlockPos> monitors = monitors();
        final Level level = machine.getLevel();
        if (speakers < 2 || monitors.isEmpty() || level == null) {
            return StereoSide.BOTH;
        }
        final BlockPos monitor = monitors.getFirst();
        final BlockState state = level.getBlockState(monitor);
        if (!state.hasProperty(MonitorBlock.FACING)) {
            return StereoSide.BOTH;
        }
        final Direction left = state.getValue(MonitorBlock.FACING).getCounterClockWise();
        return SpeakerSides.sideOf(speakers, left.getStepX(), left.getStepZ(), speaker.getX() - monitor.getX(),
                speaker.getZ() - monitor.getZ());
    }

    /* The monitors linked to the machine, in a fixed order, where its sound comes out. */
    private List<BlockPos> monitors() {
        final Level level = machine.getLevel();
        final List<BlockPos> found = new ArrayList<>();
        if (level == null) {
            return found;
        }
        for (final long endpoint : machine.peripheralEndpoints()) {
            final BlockPos pos = BlockPos.of(endpoint);
            if (level.getBlockEntity(pos) instanceof MonitorBlockEntity) {
                found.add(pos);
            }
        }
        found.sort(Comparator.naturalOrder());
        return found;
    }

    /* The speakers linked to the machine, in a fixed order. */
    private List<SpeakerBlockEntity> speakers() {
        final Level level = machine.getLevel();
        final List<SpeakerBlockEntity> found = new ArrayList<>();
        if (level == null) {
            return found;
        }
        for (final long endpoint : machine.peripheralEndpoints()) {
            if (level.getBlockEntity(BlockPos.of(endpoint)) instanceof SpeakerBlockEntity speaker) {
                found.add(speaker);
            }
        }
        found.sort(Comparator.comparing(SpeakerBlockEntity::getBlockPos));
        return found;
    }
}
