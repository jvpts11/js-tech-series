/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.machine;

import dev.jstech.core.hazard.Pollution;
import dev.jstech.core.hazard.Radiation;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * A machine's emissions at work: the pollutions it puts on its chunk, counted by the ticks it worked and put there a
 * second's worth at a time, and the radiation it gives off while it works and nothing is holding it in, stopped as it
 * stops and as it is broken.
 */
public final class MachineEmissions {

    private final List<Emission> emissions;
    private int workedTicks;
    private boolean radiating;

    private static final int TICKS_A_SECOND = 20;

    /** The emissions of a machine that gives off {@code emissions}. */
    public MachineEmissions(final List<Emission> emissions) {
        this.emissions = List.copyOf(emissions);
    }

    /**
     * A tick of the machine at {@code pos} settled: whether it worked, and whether its radiation is held in. Each
     * second of work puts its pollution on its chunk; its radiation follows whether it works unheld.
     */
    public void settle(final ServerLevel level, final BlockPos pos, final boolean worked, final boolean shielded) {
        if (this.emissions.isEmpty()) {
            return;
        }
        if (worked) {
            this.workedTicks++;
        }
        if (this.workedTicks >= TICKS_A_SECOND) {
            for (final Emission emission : this.emissions) {
                if (emission instanceof Emission.Pollutes pollutes) {
                    Pollution.emit(level, pos, pollutes.kind(), pollutes.perSecond());
                }
            }
            this.workedTicks -= TICKS_A_SECOND;
        }
        radiate(level, pos, worked && !shielded);
    }

    /** Stops what it gives off, as the machine at {@code pos} is broken. */
    public void stop(final ServerLevel level, final BlockPos pos) {
        this.radiating = true;
        radiate(level, pos, false);
    }

    /* Gives its radiation off, or stops it, when that changes. */
    private void radiate(final ServerLevel level, final BlockPos pos, final boolean now) {
        if (now == this.radiating) {
            return;
        }
        this.radiating = now;
        for (final Emission emission : this.emissions) {
            if (emission instanceof Emission.Radiates radiates) {
                Radiation.emit(level, pos, radiates.kind(), now ? radiates.strength() : 0.0);
            }
        }
    }
}
