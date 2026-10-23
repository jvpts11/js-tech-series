/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import java.util.Optional;

/**
 * What a substance is hazardous by, as the real world classifies it: the classes of the Globally Harmonized System of
 * Classification and Labelling of Chemicals, gathered by the pictogram each calls for, and the two hazards a safety
 * data sheet marks outside it, a gas that only takes the place of the air and a substance that is radioactive. A
 * material carries its classes as real data; its pictograms follow from them ({@link GhsPictograms}).
 *
 * <p>Pure: no game types.
 */
@TextHolder
public enum HazardClass {
    /** Unstable explosives, self-reactive substances that detonate, organic peroxides that do. */
    EXPLOSIVE("explosive", TextKey.of("jscore.hazard_class.explosive", "Explosive"), GhsPictogram.EXPLOSIVE),
    /**
     * Flammable gases, aerosols, liquids and solids; pyrophoric and self-heating substances; those that give off
     * flammable gas in water.
     */
    FLAMMABLE("flammable", TextKey.of("jscore.hazard_class.flammable", "Flammable"), GhsPictogram.FLAME),
    /** Oxidising gases, liquids and solids, which make other things burn. */
    OXIDISING("oxidising", TextKey.of("jscore.hazard_class.oxidising", "Oxidising"),
            GhsPictogram.FLAME_OVER_CIRCLE),
    /** Corrosive to the skin or to metals; serious damage to the eyes. */
    CORROSIVE("corrosive", TextKey.of("jscore.hazard_class.corrosive", "Corrosive"), GhsPictogram.CORROSION),
    /** Acute toxicity of the first three categories: fatal or toxic swallowed, on the skin or breathed in. */
    ACUTELY_TOXIC("acutely_toxic", TextKey.of("jscore.hazard_class.acutely_toxic", "Acutely toxic"),
            GhsPictogram.SKULL),
    /**
     * Harmful: acute toxicity of the fourth category, irritation of the skin, the eyes or the airways, sensitisation
     * of the skin, drowsiness.
     */
    HARMFUL("harmful", TextKey.of("jscore.hazard_class.harmful", "Harmful"), GhsPictogram.EXCLAMATION_MARK),
    /**
     * A health hazard over time: carcinogenic, mutagenic, toxic to reproduction, a sensitiser of the airways, harm to
     * organs, deadly if it enters the airways.
     */
    HEALTH_HAZARD("health_hazard", TextKey.of("jscore.hazard_class.health_hazard", "Health hazard"),
            GhsPictogram.HEALTH_HAZARD),
    /** Toxic to life in water. */
    ENVIRONMENTAL("environmental", TextKey.of("jscore.hazard_class.environmental", "Hazardous to the environment"),
            GhsPictogram.ENVIRONMENT),
    /** A gas that harms only by taking the place of the air: in a closed room, it suffocates. No GHS pictogram. */
    ASPHYXIANT("asphyxiant", TextKey.of("jscore.hazard_class.asphyxiant", "Asphyxiant"), null),
    /** Radioactive, which a safety data sheet marks with the trefoil, outside the GHS. */
    RADIOACTIVE("radioactive", TextKey.of("jscore.hazard_class.radioactive", "Radioactive"), null);

    private final String serializedName;
    private final TextKey name;
    private final GhsPictogram pictogram;

    HazardClass(final String serializedName, final TextKey name, final GhsPictogram pictogram) {
        this.serializedName = serializedName;
        this.name = name;
        this.pictogram = pictogram;
    }

    /** The class written as {@code name} in a data pack: {@code "acutely_toxic"}, or none. */
    public static Optional<HazardClass> byName(final String name) {
        final String wanted = name.toLowerCase(Locale.ROOT);
        for (final HazardClass hazard : values()) {
            if (hazard.serializedName.equals(wanted)) {
                return Optional.of(hazard);
            }
        }
        return Optional.empty();
    }

    /** How a data pack writes it: {@code "acutely_toxic"}. */
    public String serializedName() {
        return this.serializedName;
    }

    /** What it is called. */
    public TextKey nameKey() {
        return this.name;
    }

    /** The GHS pictogram it calls for, or none for the two hazards outside the GHS. */
    public Optional<GhsPictogram> pictogram() {
        return Optional.ofNullable(this.pictogram);
    }
}
