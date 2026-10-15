/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.energy.Energies;
import dev.jstech.core.energy.Energy;
import dev.jstech.core.energy.EnergyClass;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * What a machine runs on, as it declares it: FE, the game's own energy, or a mod's energy through a port of one class
 * ({@link Energies#builder}), with the fuse and the battery of its Protection tab.
 *
 * @param energy     the energy
 * @param portClass  the class of its port; none for FE, which has no classes
 * @param capacity   the most it holds
 * @param maxReceive the most it takes in a tick
 * @param maxExtract the most it gives in a tick; 0 for a machine that only spends
 */
public record MachineEnergy(Energy energy, @Nullable EnergyClass portClass, long capacity, long maxReceive,
                            long maxExtract) {

    public MachineEnergy {
        Objects.requireNonNull(energy, "energy");
        if (energy.classless() != (portClass == null)) {
            throw new IllegalArgumentException("a port of " + energy.id() + (portClass == null
                    ? " has a class" : " has no class"));
        }
        if (portClass != null && !energy.classes().contains(portClass)) {
            throw new IllegalArgumentException("the energy " + energy.id() + " has no class " + portClass.path());
        }
        if (capacity < 0 || maxReceive < 0 || maxExtract < 0) {
            throw new IllegalArgumentException("a machine holds, takes and gives no less than nothing");
        }
    }

    /** FE: holding {@code capacity}, taking {@code maxReceive} and giving {@code maxExtract} in a tick. */
    public static MachineEnergy fe(final int capacity, final int maxReceive, final int maxExtract) {
        return new MachineEnergy(Energies.FE, null, capacity, maxReceive, maxExtract);
    }

    /** {@code energy} through a port of {@code portClass}, holding {@code capacity} and taking {@code maxReceive}. */
    public static MachineEnergy of(final Energy energy, final EnergyClass portClass, final long capacity,
                                   final long maxReceive) {
        return new MachineEnergy(energy, portClass, capacity, maxReceive, 0L);
    }

    /** The same, giving at most {@code most} in a tick, as a generator does. */
    public MachineEnergy giving(final long most) {
        return new MachineEnergy(this.energy, this.portClass, this.capacity, this.maxReceive, most);
    }

    /** Whether it runs on FE. */
    public boolean fe() {
        return this.portClass == null;
    }
}
