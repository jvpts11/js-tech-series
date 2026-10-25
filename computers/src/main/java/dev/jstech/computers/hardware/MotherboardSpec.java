/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;

import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable specification of a motherboard, the chassis that bounds an assembly: which CPU socket and how many of
 * them, how many RAM slots and which generations they accept, how many expansion slots and their bus generation, and
 * how many disks. Its device ports come with its era, as {@link MachinePorts} counts them.
 *
 * <p>Some boards of a time when one bus was taking over from another carry slots of both, as a server board of the
 * mid-1990s had ISA slots beside its PCI ones: {@code olderBus} is the bus of those, or null on a board of one bus. A
 * card of either bus goes in one of its {@code pcieSlots}.
 *
 * @param olderBus the older bus the board also has slots of, or null when it has only {@code pcieGeneration}'s
 */
@ApiStatus.Experimental
public record MotherboardSpec(FormFactor formFactor,
                              Era era,
                              CpuSocketId socket,
                              int cpuSlots,
                              Set<RamGeneration> acceptedRam,
                              int ramSlots,
                              PcieGeneration pcieGeneration,
                              int pcieSlots,
                              int diskSlots,
                              @Nullable PcieGeneration olderBus) {

    /** A board with slots of one bus. */
    public MotherboardSpec(final FormFactor formFactor, final Era era, final CpuSocketId socket,
                           final int cpuSlots, final Set<RamGeneration> acceptedRam, final int ramSlots,
                           final PcieGeneration pcieGeneration, final int pcieSlots, final int diskSlots) {
        this(formFactor, era, socket, cpuSlots, acceptedRam, ramSlots, pcieGeneration, pcieSlots, diskSlots, null);
    }

    public MotherboardSpec {
        Objects.requireNonNull(formFactor, "formFactor must not be null");
        Objects.requireNonNull(era, "era must not be null");
        Objects.requireNonNull(socket, "socket must not be null");
        Objects.requireNonNull(pcieGeneration, "pcieGeneration must not be null");
        acceptedRam = Set.copyOf(acceptedRam); // defensive copy + null-checks elements
        if (acceptedRam.isEmpty()) {
            throw new IllegalArgumentException("acceptedRam must not be empty");
        }
        if (cpuSlots <= 0) {
            throw new IllegalArgumentException("cpuSlots must be > 0; got " + cpuSlots);
        }
        if (ramSlots <= 0) {
            throw new IllegalArgumentException("ramSlots must be > 0; got " + ramSlots);
        }
        if (pcieSlots < 0) {
            throw new IllegalArgumentException("pcieSlots must be >= 0; got " + pcieSlots);
        }
        if (diskSlots < 0) {
            throw new IllegalArgumentException("diskSlots must be >= 0; got " + diskSlots);
        }
    }

    /**
     * Whether the board has its sound built in, so a machine plays without a sound card. Boards had it from the
     * Transition on, as the HD Audio of the late 2000s; the boards before it give only the speaker in the case.
     */
    public boolean hasOnBoardAudio() {
        return era.isAtLeast(HardwareEra.TRANSITION);
    }

    /**
     * Whether the board drives a screen by itself: a server board carries a management controller with a plain screen
     * output for its console. Any other board shows a picture only through a graphics card, or through a processor
     * with graphics on its die.
     */
    public boolean hasOwnVideo() {
        return formFactor == FormFactor.EEB;
    }

    /** Whether {@code card} goes in one of the board's slots: of the board's own bus, or of the older one beside it. */
    public boolean takes(final IExpansionCardSpec card) {
        return card.fits(pcieGeneration) || olderBus != null && card.fits(olderBus);
    }

    /** The slot a card on {@code bus} sits in: one of the older bus when it is of that one, else of the board's own. */
    public PcieGeneration slotFor(final PcieGeneration bus) {
        return olderBus != null && bus.compatibleWith(olderBus) ? olderBus : pcieGeneration;
    }
}
