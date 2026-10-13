/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.peripheral.PortKind;
import dev.jstech.core.tier.HardwareEra;

/**
 * How many ports of each kind a machine's hardware gives it, as on a real computer: the board has its device ports
 * (serial and parallel on the Vintage, then more USB each era), each graphics card its video outputs, and the sound
 * card its audio output, until the boards carry their own sound from the Transition on. A monitor takes a video
 * output, a pair of speakers an audio output, and every other peripheral a device port.
 *
 * <p>The numbers are first estimates, to be tuned in play. An era after the Advanced has the Advanced's.
 */
public final class MachinePorts {

    /** An audio output drives a pair of speakers, the left and the right. */
    public static final int SPEAKERS_PER_AUDIO_OUTPUT = 2;

    private MachinePorts() {
    }

    /** How many peripherals of {@code kind} a machine built as {@code build} can have linked at once. */
    public static int of(final ComputerBuild build, final PortKind kind) {
        return switch (kind) {
            case DEVICE -> devicePorts(build.motherboard().era());
            case VIDEO -> videoOutputs(build);
            case AUDIO -> audioOutputs(build) * SPEAKERS_PER_AUDIO_OUTPUT;
        };
    }

    /** The device ports on a board of {@code era}. */
    public static int devicePorts(final HardwareEra era) {
        return byEra(era, 2, 4, 6, 8, 10);
    }

    /** The video outputs on one graphics card of {@code era}. */
    public static int videoOutputs(final HardwareEra era) {
        return byEra(era, 1, 2, 2, 4, 4);
    }

    /**
     * Every video output of the machine: each graphics card's, by the card's own era; on a server board one of its
     * own, the plain screen output of the management controller that server boards carry for their console; and the
     * board's own output when its processor carries graphics on its die, which drives a screen without a card.
     */
    public static int videoOutputs(final ComputerBuild build) {
        int outputs = build.motherboard().formFactor() == FormFactor.EEB ? 1 : 0;
        for (final GpuSpec gpu : build.gpus()) {
            outputs += videoOutputs(gpu.era());
        }
        for (final CpuSpec cpu : build.cpus()) {
            if (cpu.hasIntegratedGraphics()) {
                // One output on the board however many processors drive it: a board has the one connector.
                outputs++;
                break;
            }
        }
        return outputs;
    }

    /** The machine's audio outputs: its sound card's, and its board's own from the Transition on. */
    public static int audioOutputs(final ComputerBuild build) {
        int outputs = build.motherboard().hasOnBoardAudio() ? 1 : 0;
        for (final IExpansionCardSpec card : build.pcieCards()) {
            if (card instanceof SoundCardSpec) {
                outputs++;
            }
        }
        return outputs;
    }

    private static int byEra(final HardwareEra era, final int vintage, final int legacy, final int transition,
                             final int standard, final int advanced) {
        return switch (era) {
            case VINTAGE -> vintage;
            case LEGACY -> legacy;
            case TRANSITION -> transition;
            case STANDARD -> standard;
            case ADVANCED, EXA, SINGULARITY -> advanced;
        };
    }
}
