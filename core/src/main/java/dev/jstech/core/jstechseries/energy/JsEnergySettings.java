/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.energy.EnergyRatio;

/**
 * J's Energy's settings, {@code jstech-energy.toml} beside the world, the server's alone: whether J's Energy is on,
 * what a port of the wrong class suffers, whether its blocks take FE directly, and what a joule is worth in FE. A
 * player never sets the rate, since a converter that changed it would make energy from nothing.
 */
public final class JsEnergySettings {

    public static final String ENERGY = "energy";
    private static final int MOST_JOULES_PER_FE = 1_000;

    /** Whether J's Energy is on; off, the default until a mod builds on it, every block of the series speaks FE. */
    public static final ConfigKey<Boolean> ENABLED = ConfigKey.flag(ENERGY + ".enabled", false)
            .comment("Whether J's Energy is on, in joules, watts and voltage classes. Off, every block of the series "
                    + "takes and gives FE directly, as the rest of a pack does.")
            .named("J's Energy");

    /** Whether a block on a class above its port burns. */
    public static final ConfigKey<Boolean> OVERVOLTAGE_DAMAGES = ConfigKey.flag(ENERGY + ".overvoltage_damages", true)
            .comment("Whether a block on a line of a class above its port burns, or is ruined by an arc flash two "
                    + "classes up. Off, it just does not start.")
            .named("Overvoltage damages").activeWhen(ENABLED);

    /** Whether every block takes every class. */
    public static final ConfigKey<Boolean> ACCEPT_ANY_VOLTAGE = ConfigKey.flag(ENERGY + ".accept_any_voltage", false)
            .comment("Whether every block takes energy from a line of any class, as if it were its own.")
            .named("Accept any voltage").activeWhen(ENABLED);

    /** Whether J's Energy blocks take and give FE without a converter. */
    public static final ConfigKey<Boolean> ACCEPT_FE = ConfigKey.flag(ENERGY + ".accept_fe", false)
            .comment("Whether the blocks of J's Energy take and give FE directly, both ways, with no converter, so "
                    + "the series speaks FE with the rest of a pack.")
            .named("Accept FE directly").activeWhen(ENABLED);

    /** How many joules one FE is worth. */
    public static final ConfigKey<Integer> JOULES_PER_FE = ConfigKey.whole(ENERGY + ".joules_per_fe", 1)
            .range(1, MOST_JOULES_PER_FE)
            .comment("How many joules one FE is worth. At one, an FE a tick is 20 watts.")
            .named("Joules per FE").unit("J").activeWhen(ENABLED);

    /** The file, each setting passed into what it sets. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-energy", ConfigSide.SERVER, ConfigFormats.TOML)
            .comment("J's Energy: whether it is on, what a block on the wrong voltage suffers, and how it speaks FE.",
                    "Values outside their range are pulled to the nearer end when the file is read.")
            .sectionComment(ENERGY, "J's Energy, the series' energy in joules, watts and voltage classes.")
            .sectionNamed(ENERGY, "J's Energy")
            .key(ENABLED, JsEnergySettings::setEnabled)
            .key(OVERVOLTAGE_DAMAGES, JsEnergySettings::setOvervoltageDamages)
            .key(ACCEPT_ANY_VOLTAGE, JsEnergySettings::setAcceptAnyVoltage)
            .key(ACCEPT_FE, JsEnergySettings::setAcceptFe)
            .key(JOULES_PER_FE, JsEnergySettings::setJoulesPerFe)
            .build();

    private static volatile boolean enabled;
    private static volatile boolean overvoltageDamages = true;
    private static volatile boolean acceptAnyVoltage;
    private static volatile boolean acceptFe;
    private static volatile EnergyRatio feRate = EnergyRatio.ONE;

    private JsEnergySettings() {
    }

    /** Whether J's Energy is on. */
    public static boolean enabled() {
        return enabled;
    }

    /** Whether a block on a class above its port burns, rather than refusing to start. */
    public static boolean overvoltageDamages() {
        return overvoltageDamages;
    }

    /** Whether every block takes every class. */
    public static boolean acceptAnyVoltage() {
        return acceptAnyVoltage;
    }

    /** Whether J's Energy blocks take and give FE directly. */
    public static boolean acceptFe() {
        return acceptFe;
    }

    /** What a joule is worth in FE. */
    public static EnergyRatio feRate() {
        return feRate;
    }

    public static void setEnabled(final boolean value) {
        enabled = value;
    }

    public static void setOvervoltageDamages(final boolean value) {
        overvoltageDamages = value;
    }

    public static void setAcceptAnyVoltage(final boolean value) {
        acceptAnyVoltage = value;
    }

    public static void setAcceptFe(final boolean value) {
        acceptFe = value;
    }

    public static void setJoulesPerFe(final int joules) {
        feRate = new EnergyRatio(Math.max(1, joules), 1L);
    }
}
