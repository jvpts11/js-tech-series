/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import java.util.Objects;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * A port of a mod's energy seen as the game's FE storage, through its energy's ratio to FE: what a mod that speaks only
 * FE sees of it. Both ways round down, so no energy is made of a rounding: what goes in is worth no more than the FE
 * paid for it, and the FE that comes out is worth no more than what left the port.
 */
public final class FeBridge {

    private FeBridge() {
    }

    /** {@code port} as the game's FE storage. */
    public static IEnergyStorage view(final IEnergyPort port) {
        return new View(Objects.requireNonNull(port, "port"));
    }

    /**
     * The game's FE storage {@code storage} as a port of {@code energy} in {@code portClass}: what a line of an energy
     * that takes FE sees of a block that speaks only FE. Both ways round down, as the view does.
     */
    public static IEnergyPort port(final IEnergyStorage storage, final Energy energy, final EnergyClass portClass) {
        return new Port(Objects.requireNonNull(storage, "storage"), Objects.requireNonNull(energy, "energy"),
                Objects.requireNonNull(portClass, "portClass"));
    }

    /* A port seen as the game's FE storage. */
    private record View(IEnergyPort port) implements IEnergyStorage {

        @Override
        public int receiveEnergy(final int toReceive, final boolean simulate) {
            final Energy energy = this.port.energy();
            final long fe = energy.toFe(this.port.receive(energy.fromFe(Math.max(0, toReceive)), true));
            final long units = energy.fromFe(fe);
            if (!simulate && units > 0) {
                this.port.receive(units, false);
            }
            return (int) Math.min(Integer.MAX_VALUE, fe);
        }

        @Override
        public int extractEnergy(final int toExtract, final boolean simulate) {
            final Energy energy = this.port.energy();
            final long units = energy.fromFe(energy.toFe(this.port.extract(energy.fromFe(Math.max(0, toExtract)),
                    true)));
            if (!simulate && units > 0) {
                this.port.extract(units, false);
            }
            return (int) Math.min(Integer.MAX_VALUE, energy.toFe(units));
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, this.port.energy().toFe(this.port.stored()));
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, this.port.energy().toFe(this.port.capacity()));
        }

        @Override
        public boolean canExtract() {
            return this.port.canExtract();
        }

        @Override
        public boolean canReceive() {
            return this.port.canReceive();
        }
    }

    /* An FE storage seen as a port of an energy, in a class. */
    private record Port(IEnergyStorage storage, Energy energy, EnergyClass portClass) implements IEnergyPort {

        @Override
        public long stored() {
            return this.energy.fromFe(this.storage.getEnergyStored());
        }

        @Override
        public long capacity() {
            return this.energy.fromFe(this.storage.getMaxEnergyStored());
        }

        @Override
        public boolean canReceive() {
            return this.storage.canReceive();
        }

        @Override
        public boolean canExtract() {
            return this.storage.canExtract();
        }

        @Override
        public long receive(final long amount, final boolean simulate) {
            final int fe = (int) Math.min(Integer.MAX_VALUE, this.energy.toFe(Math.max(0L, amount)));
            return this.energy.fromFe(this.storage.receiveEnergy(fe, simulate));
        }

        @Override
        public long extract(final long amount, final boolean simulate) {
            final int fe = (int) Math.min(Integer.MAX_VALUE, this.energy.toFe(Math.max(0L, amount)));
            return this.energy.fromFe(this.storage.extractEnergy(fe, simulate));
        }

        @Override
        public void meetWrongClass(final WrongClass what, final EnergyClass line, final long gameTime) {
            // A block that speaks FE has no class, so no line is the wrong one for it.
        }
    }
}
