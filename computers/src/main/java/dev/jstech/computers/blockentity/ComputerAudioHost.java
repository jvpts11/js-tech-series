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
import dev.jstech.computers.audio.SystemSound;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.SoundCardSpec;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.AudioDevice;
import dev.jstech.core.audio.AudioDevices;
import dev.jstech.core.audio.IAudioHost;
import dev.jstech.core.audio.SoundContext;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A computer as the source of its system's sound: the device that plays it and where it comes out. A sound card, or
 * the sound built into a Standard board, plays out of the monitors linked to the machine. With no monitor there is
 * only the speaker inside the case, which beeps and plays no recording.
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
        if (monitors().isEmpty()) {
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
        final List<Vec3> monitors = monitors();
        return monitors.isEmpty() ? List.of(Vec3.atCenterOf(machine.getBlockPos())) : monitors;
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

    /* The monitors linked to the machine, where its sound comes out. */
    private List<Vec3> monitors() {
        final Level level = machine.getLevel();
        final List<Vec3> found = new ArrayList<>();
        if (level == null) {
            return found;
        }
        for (final long endpoint : machine.peripheralEndpoints()) {
            final BlockPos pos = BlockPos.of(endpoint);
            if (level.getBlockEntity(pos) instanceof MonitorBlockEntity) {
                found.add(Vec3.atCenterOf(pos));
            }
        }
        return found;
    }
}
