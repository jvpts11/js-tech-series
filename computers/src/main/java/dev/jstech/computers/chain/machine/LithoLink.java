/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.machine.ProcessingMachineBlockEntity.Processing;
import dev.jstech.core.multiblock.ShapeMatch;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * How a Lithography Station from the Legacy on works through its computer (computers/docs/design/survival.md): only
 * linked by peripheral cable, on its peripheral port, to a computer of the era before or later, switched on and
 * running, which runs the exposure as its {@code litho} process, holding memory by the station's era; a computer two
 * eras newer than the one asked for exposes in half the time. A station of the Vintage, and every other machine, works
 * on its own.
 */
public final class LithoLink {

    private final ChainMachine machine;

    /** The process the computer runs the exposure as. */
    static final String PROCESS = "litho";
    /* What the process holds on the computer, in megabytes, by the station's era. */
    private static final Map<Era, Integer> MEMORY_MB = Map.of(HardwareEra.LEGACY, 2, HardwareEra.TRANSITION, 32,
            HardwareEra.STANDARD, 256, HardwareEra.ADVANCED, 2_048);
    /* How long the process stays held after the station last asked, in ticks: a second. */
    private static final long HELD_TICKS = 20;
    private static final int TWO_ERAS = 2;

    LithoLink(final ChainMachine machine) {
        this.machine = machine;
    }

    /** What the computer holds while it exposes a recipe of {@code era}, in megabytes; 0 for an era that needs none. */
    public static int memoryMb(final Era era) {
        return MEMORY_MB.getOrDefault(era, 0);
    }

    /**
     * The work of a recipe of {@code era} as the station may do it: as it is when it needs no computer, faster on a
     * computer two eras newer than the one asked for, none while it has no computer that may run it.
     */
    Optional<Processing> allows(final Level level, final ChainMachineBlockEntity station, final Processing work,
                                @Nullable final Era era) {
        if (!machine.linked() || era == null || !(level instanceof ServerLevel server)) {
            return Optional.of(work);
        }
        final Optional<AbstractComputerBlockEntity> computer = computer(server, station);
        final Era asked = before(era);
        if (computer.isEmpty() || !computer.get().isRunning() || !computer.get().displayEra().isAtLeast(asked)) {
            return Optional.empty();
        }
        final AbstractComputerBlockEntity running = computer.get();
        final int mb = memoryMb(era);
        if (!running.hosts(PROCESS, server.getGameTime()) && !running.ramLedger().fits(mb)) {
            return Optional.empty();
        }
        running.host(PROCESS, mb, server.getGameTime() + HELD_TICKS);
        final int newer = HardwareEra.SERIES.indexOf(running.displayEra()) - HardwareEra.SERIES.indexOf(asked);
        return Optional.of(newer >= TWO_ERAS ? work.withTicks(Math.max(1, work.ticks() / 2)) : work);
    }

    /* The computer its peripheral port is linked to, if any. */
    private static Optional<AbstractComputerBlockEntity> computer(final ServerLevel level,
                                                                  final ChainMachineBlockEntity station) {
        final Optional<ShapeMatch.Formed> formed = station.formed() ? station.structure().structure()
                : Optional.empty();
        if (formed.isEmpty()) {
            return Optional.empty();
        }
        for (final long at : formed.get().parts()) {
            if (!level.getBlockState(BlockPos.of(at)).is(ChainMachineBlocks.PERIPHERAL_PORT.get())) {
                continue;
            }
            final OptionalLong owner = PeripheralLinks.discoverOwner(level, at);
            if (owner.isPresent() && level.getBlockEntity(BlockPos.of(owner.getAsLong()))
                    instanceof AbstractComputerBlockEntity computer) {
                return Optional.of(computer);
            }
        }
        return Optional.empty();
    }

    /* The era before {@code era}: whose computers make its chips. */
    private static Era before(final Era era) {
        final int at = HardwareEra.SERIES.indexOf(era);
        return at > 0 ? HardwareEra.SERIES.get(at - 1) : era;
    }
}
