/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import dev.jstech.core.JsCore;
import dev.jstech.core.energy.CoreEnergy;
import dev.jstech.core.energy.Energies;
import dev.jstech.core.energy.Energy;
import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.energy.EnergyRatio;
import dev.jstech.core.energy.EnergyUnit;
import dev.jstech.core.energy.IWrongClassRule;
import dev.jstech.core.energy.WrongClass;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * J's Energy, JE for short, the series' energy, built only from the Core's energy kit: counted in joules, shown as power
 * in watts, its lines in the five voltage classes ({@link VoltageClass}), each a grid of its own that never joins
 * another's. A joule is worth one FE unless the server sets otherwise ({@link JsEnergySettings}).
 *
 * <p>A port on a line one class above its own burns, two or more an arc flash, and one below is short of voltage; the
 * server can turn the damage off, so a block on a class too high only refuses to start, or let every block take every
 * class. Its lines lose by the cables they are made of, not by their class.
 */
@TextHolder
public final class JsEnergy {

    public static final TextKey NAME = TextKey.of("jscore.energy.je", "J's Energy");
    public static final TextKey JOULE_NAME = TextKey.of("jscore.energy.joule", "Joule");
    public static final TextKey JOULE_SYMBOL = TextKey.of("jscore.energy.joule.symbol", "J");
    public static final TextKey WATT_SYMBOL = TextKey.of("jscore.energy.watt.symbol", "W");
    public static final TextKey LOW_NAME = TextKey.of("jscore.energy.class.low", "Low voltage");
    public static final TextKey LOW_RATING = TextKey.of("jscore.energy.class.low.rating", "400 V");
    public static final TextKey MEDIUM_NAME = TextKey.of("jscore.energy.class.medium", "Medium voltage");
    public static final TextKey MEDIUM_RATING = TextKey.of("jscore.energy.class.medium.rating", "11 kV");
    public static final TextKey HIGH_NAME = TextKey.of("jscore.energy.class.high", "High voltage");
    public static final TextKey HIGH_RATING = TextKey.of("jscore.energy.class.high.rating", "138 kV");
    public static final TextKey EXTRA_HIGH_NAME = TextKey.of("jscore.energy.class.extra_high", "Extra-high voltage");
    public static final TextKey EXTRA_HIGH_RATING = TextKey.of("jscore.energy.class.extra_high.rating", "500 kV");
    public static final TextKey ULTRA_HIGH_NAME = TextKey.of("jscore.energy.class.ultra_high", "Ultra-high voltage");
    public static final TextKey ULTRA_HIGH_RATING = TextKey.of("jscore.energy.class.ultra_high.rating",
            "±1,100 kV DC");

    /** What touching a live bare wire of the low and the medium class does, in the game's points of damage. */
    public static final float LOW_SHOCK = 4F;
    public static final float MEDIUM_SHOCK = 12F;

    private static final DeferredRegister<EnergyUnit> UNITS = DeferredRegister.create(CoreEnergy.UNITS_KEY,
            JsCore.MODID);
    /** The joule, J's Energy's unit, worth one FE as it is registered; the server's rate decides at run time. */
    public static final DeferredHolder<EnergyUnit, EnergyUnit> JOULE = UNITS.register("joule",
            () -> new EnergyUnit(JOULE_NAME, JOULE_SYMBOL, EnergyRatio.ONE));

    public static final EnergyClass LOW = EnergyClass.rated(VoltageClass.LOW.path(), LOW_NAME, LOW_RATING);
    public static final EnergyClass MEDIUM = EnergyClass.rated(VoltageClass.MEDIUM.path(), MEDIUM_NAME,
            MEDIUM_RATING);
    public static final EnergyClass HIGH = EnergyClass.rated(VoltageClass.HIGH.path(), HIGH_NAME, HIGH_RATING);
    public static final EnergyClass EXTRA_HIGH = EnergyClass.rated(VoltageClass.EXTRA_HIGH.path(), EXTRA_HIGH_NAME,
            EXTRA_HIGH_RATING);
    public static final EnergyClass ULTRA_HIGH = EnergyClass.rated(VoltageClass.ULTRA_HIGH.path(), ULTRA_HIGH_NAME,
            ULTRA_HIGH_RATING);

    /** J's Energy. */
    public static final Energy ENERGY = Energies.builder(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "je"),
                    NAME).unit(JOULE).power(WATT_SYMBOL).classes(LOW, MEDIUM, HIGH, EXTRA_HIGH, ULTRA_HIGH)
            .feRate(JsEnergySettings::feRate).acceptsFe(JsEnergy::speaksFe).wrongClass(JsEnergy::meeting)
            // A bare wire hurts more the higher its class, and from high voltage up it kills.
            .shock(LOW, LOW_SHOCK).shock(MEDIUM, MEDIUM_SHOCK).shock(HIGH, Energy.KILLS)
            .shock(EXTRA_HIGH, Energy.KILLS).shock(ULTRA_HIGH, Energy.KILLS)
            .shocks(JsEnergySettings::electricShock).surges(JsEnergySettings::lightningSurges).declare();

    private static final Map<VoltageClass, EnergyClass> CLASSES = new EnumMap<>(Map.of(VoltageClass.LOW, LOW,
            VoltageClass.MEDIUM, MEDIUM, VoltageClass.HIGH, HIGH, VoltageClass.EXTRA_HIGH, EXTRA_HIGH,
            VoltageClass.ULTRA_HIGH, ULTRA_HIGH));

    private JsEnergy() {
    }

    /** Hands the joule to the Core's event bus, from the Core's constructor. */
    public static void register(final IEventBus modEventBus) {
        UNITS.register(modEventBus);
    }

    /** The class of line of {@code voltage}. */
    public static EnergyClass classOf(final VoltageClass voltage) {
        return CLASSES.get(voltage);
    }

    /** The voltage class {@code of} is, or none for a class of another energy. */
    public static Optional<VoltageClass> voltageOf(final EnergyClass of) {
        for (final Map.Entry<VoltageClass, EnergyClass> each : CLASSES.entrySet()) {
            if (each.getValue() == of) {
                return Optional.of(each.getKey());
            }
        }
        return Optional.empty();
    }

    /**
     * Whether the series' blocks speak FE: when J's Energy is off, every one of them does, and when it is on, those
     * of J's Energy do too if the server lets them take FE directly.
     */
    public static boolean speaksFe() {
        return !JsEnergySettings.enabled() || JsEnergySettings.acceptFe();
    }

    /* What befalls a port {@code steps} classes below its line, as the server's settings say. */
    private static WrongClass meeting(final int steps) {
        if (JsEnergySettings.acceptAnyVoltage()) {
            return WrongClass.NONE;
        }
        if (steps > 0 && !JsEnergySettings.overvoltageDamages()) {
            return WrongClass.OVERVOLTAGE;
        }
        return IWrongClassRule.STANDARD.at(steps);
    }
}
