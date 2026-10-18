/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fire;

import com.mojang.serialization.Codec;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * The real classes of fire, by what burns: each is put out by its own agents, and the wrong one can make it worse.
 * The class of a fire is that of the worst of what feeds it.
 */
public enum FireClass implements StringRepresentable {

    /** Wood, cloth, paper, plants: what burns to embers and ash. */
    SOLIDS(FireTexts.SOLIDS),
    /** Flammable liquids: oil, fuel, solvents; water spreads them. */
    LIQUIDS(FireTexts.LIQUIDS),
    /** A fire on something live with current: water and foam carry the current to whoever sprays them. */
    ELECTRICAL(FireTexts.ELECTRICAL),
    /** Burning metals, sodium and magnesium: water makes them burst; only dry powder smothers them. */
    METALS(FireTexts.METALS),
    /** Cooking oils and fats, hotter than other liquids: water makes them flare up. */
    COOKING_OILS(FireTexts.COOKING_OILS);

    private final TextKey name;

    /** Each one as data writes it: {@code solids}, {@code liquids}, {@code electrical}, and so on. */
    public static final Codec<FireClass> CODEC = StringRepresentable.fromEnum(FireClass::values);

    FireClass(final TextKey name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Its name, as shown. */
    public TextKey title() {
        return this.name;
    }

    /** Whether it is worse than {@code other}: what makes a fire of mixed fuel the harder to put out. */
    public boolean worseThan(final FireClass other) {
        return rank() > other.rank();
    }

    private int rank() {
        return switch (this) {
            case SOLIDS -> 0;
            case LIQUIDS -> 1;
            case COOKING_OILS -> 2;
            case ELECTRICAL -> 3;
            case METALS -> 4;
        };
    }
}
