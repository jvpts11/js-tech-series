/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import com.mojang.serialization.Codec;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;

/**
 * A danger a world holds for whoever goes there unprotected, as J's Space's table of every world's dangers names
 * them; each world's are listed in {@link WorldDangers}.
 */
public enum WorldDanger implements IStableName {
    /** No air at all: suffocation when the suit's air runs out. A pressure suit, a sealed room. */
    VACUUM("vacuum"),
    /** Air too thin to breathe. A pressure suit, a sealed room. */
    THIN_AIR("thin_air"),
    /** Air thick enough to crush. The hard suit. */
    CRUSHING_PRESSURE("crushing_pressure"),
    /** Heat that burns. The right suit, a sealed room. */
    HEAT("heat"),
    /** Cold that freezes. The right suit, a sealed room. */
    COLD("cold"),
    /** Air that poisons: carbon dioxide, sulfur dioxide, methane and ammonia, cyanogen. A mask or a suit. */
    POISONOUS_AIR("poisonous_air"),
    /** The sky's radiation, a dose that builds up slowly in whoever stays long. The radiation vest, a shielded home. */
    COSMIC_RADIATION("cosmic_radiation"),
    /** The Sun's storms, a strong dose in little time, with a warning. Shelter, or the radiation vest. */
    SOLAR_STORMS("solar_storms"),
    /** Belts of radiation, lethal in minutes. Heavy shielding. */
    RADIATION_BELTS("radiation_belts"),
    /** Violent stars: pulsars, flares, X-rays. Heavy shielding. */
    VIOLENT_STARS("violent_stars"),
    /** Strong fields, where instruments fail. The anti-field module. */
    STRONG_FIELDS("strong_fields"),
    /** Fine sharp dust that comes indoors on the suit. A suitport, the anti-dust module. */
    DUST("dust"),
    /** Perchlorates in the soil, which poison whoever farms in it. Washing the soil first. */
    PERCHLORATES("perchlorates"),
    /** A pull other than the Earth's: falls and jumps by the local gravity. The relativistic module. */
    GRAVITY("gravity");

    public static final Codec<WorldDanger> CODEC = StableCodecs.byName(WorldDanger.class);

    private final String serializedName;

    WorldDanger(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }
}
