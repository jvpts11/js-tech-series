/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

/**
 * What damage does to a machine's work: how fast it works, as a share of whole, and what its upgrades come to while it
 * is damaged. A machine of the Core's slows down with its damage, from whole down to half just before it stops
 * ({@link #GRADUAL}); a mod whose damaged machines run at a set speed and draw more, their upgrades idle until they are
 * repaired, says so with {@link #flat}. A machine gives its rule from {@link MachineBlockEntity#damageRule()}.
 */
public interface IMachineDamage {

    /** Slower with the damage, from whole down to half, its upgrades as they are. */
    IMachineDamage GRADUAL = new Gradual();

    /**
     * Damaged at all, it works at {@code speed} of whole and draws {@code energy} times as much a tick, and its
     * upgrades do nothing; at no health it does not work.
     */
    static IMachineDamage flat(final double speed, final double energy) {
        return new Flat(speed, energy);
    }

    /** How fast a machine of {@code health} works, as a share of whole; 0 for one that does not work. */
    double workShare(MachineHealth health);

    /** What {@code upgrades} come to on a machine of {@code health}. */
    UpgradeEffect effect(MachineHealth health, UpgradeEffect upgrades);

    /** Slower with the damage, its upgrades as they are. */
    record Gradual() implements IMachineDamage {

        @Override
        public double workShare(final MachineHealth health) {
            return health.workShare();
        }

        @Override
        public UpgradeEffect effect(final MachineHealth health, final UpgradeEffect upgrades) {
            return upgrades;
        }
    }

    /**
     * At a set speed and drawing more while damaged, its upgrades idle.
     *
     * @param speed  how fast it works while damaged, as a share of whole
     * @param energy how many times as much a tick it draws while damaged
     */
    record Flat(double speed, double energy) implements IMachineDamage {

        public Flat {
            if (!(speed > 0.0 && speed <= 1.0) || !(energy > 0.0) || Double.isInfinite(energy)) {
                throw new IllegalArgumentException("a damaged machine works at a share of whole above 0, drawing a"
                        + " finite share above 0; got " + speed + " and " + energy);
            }
        }

        @Override
        public double workShare(final MachineHealth health) {
            return health.wrecked() ? 0.0 : health.damaged() ? this.speed : 1.0;
        }

        @Override
        public UpgradeEffect effect(final MachineHealth health, final UpgradeEffect upgrades) {
            return health.damaged() ? UpgradeEffect.NONE.withEnergy(this.energy) : upgrades;
        }
    }
}
