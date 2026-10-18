/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fire;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The names of the classes of fire and of the agents that put them out, as players read them. */
@TextHolder
public final class FireTexts {

    public static final TextKey SOLIDS = TextKey.of("jscore.fire_class.solids", "Solids");
    public static final TextKey LIQUIDS = TextKey.of("jscore.fire_class.liquids", "Flammable liquids");
    public static final TextKey ELECTRICAL = TextKey.of("jscore.fire_class.electrical", "Electrical");
    public static final TextKey METALS = TextKey.of("jscore.fire_class.metals", "Metals");
    public static final TextKey COOKING_OILS = TextKey.of("jscore.fire_class.cooking_oils", "Cooking oils");
    public static final TextKey WATER = TextKey.of("jscore.extinguishing_agent.water", "Water");
    public static final TextKey FOAM = TextKey.of("jscore.extinguishing_agent.foam", "Foam");
    public static final TextKey CARBON_DIOXIDE = TextKey.of("jscore.extinguishing_agent.carbon_dioxide",
            "Carbon dioxide");
    public static final TextKey DRY_POWDER = TextKey.of("jscore.extinguishing_agent.dry_powder", "Dry powder");

    private FireTexts() {
    }
}
