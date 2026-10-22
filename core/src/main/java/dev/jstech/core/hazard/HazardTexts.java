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

/** What the hazard kit says: the kinds of harm, the hazards of carried things, the lungs, and the tooltips. */
@TextHolder
public final class HazardTexts {

    public static final TextKey RADIATION = TextKey.of("jscore.damage_kind.radiation", "Radiation");
    public static final TextKey ELECTRICAL = TextKey.of("jscore.damage_kind.electrical", "Electrical");
    public static final TextKey CRYOGENIC = TextKey.of("jscore.damage_kind.cryogenic", "Cryogenic");
    public static final TextKey ACID = TextKey.of("jscore.damage_kind.acid", "Acid");
    public static final TextKey HEAT = TextKey.of("jscore.damage_kind.heat", "Heat");
    public static final TextKey BLAST = TextKey.of("jscore.damage_kind.blast", "Blast");
    public static final TextKey PRESSURE = TextKey.of("jscore.damage_kind.pressure", "Pressure");
    public static final TextKey INHALED = TextKey.of("jscore.damage_kind.inhaled", "Dust");
    public static final TextKey TOXIC = TextKey.of("jscore.damage_kind.toxic", "Toxic");
    public static final TextKey BLINDING = TextKey.of("jscore.damage_kind.blinding", "Glare");

    public static final TextKey RADIOACTIVE = TextKey.of("jscore.item_hazard.radioactive", "Radioactive");
    public static final TextKey RADIOACTIVE_GUARD = TextKey.of("jscore.item_hazard.radioactive.guard",
            "Shielded containers and suits protect");
    public static final TextKey HOT = TextKey.of("jscore.item_hazard.hot", "Hot");
    public static final TextKey HOT_GUARD = TextKey.of("jscore.item_hazard.hot.guard",
            "Heat-resistant gloves protect");
    public static final TextKey WATER_REACTIVE = TextKey.of("jscore.item_hazard.water_reactive",
            "Reacts with water");
    public static final TextKey WATER_REACTIVE_GUARD = TextKey.of("jscore.item_hazard.water_reactive.guard",
            "Keep it dry, or under oil");
    public static final TextKey PYROPHORIC = TextKey.of("jscore.item_hazard.pyrophoric", "Catches fire in air");
    public static final TextKey PYROPHORIC_GUARD = TextKey.of("jscore.item_hazard.pyrophoric.guard",
            "Keep it under an inert gas or oil");
    public static final TextKey UNSTABLE = TextKey.of("jscore.item_hazard.unstable", "Unstable");
    public static final TextKey UNSTABLE_GUARD = TextKey.of("jscore.item_hazard.unstable.guard",
            "Handle with care: fire, a blast or a fall sets it off");
    public static final TextKey TOXIC_TOUCH = TextKey.of("jscore.item_hazard.toxic", "Toxic to touch");
    public static final TextKey TOXIC_GUARD = TextKey.of("jscore.item_hazard.toxic.guard", "Gloves protect");
    public static final TextKey CORROSIVE = TextKey.of("jscore.item_hazard.corrosive", "Corrosive");
    public static final TextKey CORROSIVE_GUARD = TextKey.of("jscore.item_hazard.corrosive.guard", "Gloves protect");
    public static final TextKey BLINDING_LIGHT = TextKey.of("jscore.item_hazard.blinding", "Blinding");
    public static final TextKey BLINDING_GUARD = TextKey.of("jscore.item_hazard.blinding.guard",
            "A dark visor protects");

    public static final TextKey DIED_LUNGS = TextKey.of("death.attack.jscore.lung_failure",
            "%1$s's lungs gave out");
    public static final TextKey DIED_LUNGS_FIGHTING = TextKey.of("death.attack.jscore.lung_failure.player",
            "%1$s's lungs gave out while fighting %2$s");
    public static final TextKey DIED_HOT_ITEM = TextKey.of("death.attack.jscore.hot_item",
            "%1$s held on to something too hot");
    public static final TextKey DIED_HOT_ITEM_FIGHTING = TextKey.of("death.attack.jscore.hot_item.player",
            "%1$s held on to something too hot while fighting %2$s");
    public static final TextKey DIED_CORROSIVE = TextKey.of("death.attack.jscore.corrosive_item",
            "%1$s was eaten away by something corrosive");
    public static final TextKey DIED_CORROSIVE_FIGHTING = TextKey.of("death.attack.jscore.corrosive_item.player",
            "%1$s was eaten away by something corrosive while fighting %2$s");
    public static final TextKey DIED_COLD_BURN = TextKey.of("death.attack.jscore.cold_burn",
            "%1$s was burned by something far too cold");
    public static final TextKey DIED_COLD_BURN_FIGHTING = TextKey.of("death.attack.jscore.cold_burn.player",
            "%1$s was burned by something far too cold while fighting %2$s");
    public static final TextKey DIED_PRESSURE = TextKey.of("death.attack.jscore.pressure",
            "%1$s was crushed by the pressure");
    public static final TextKey DIED_PRESSURE_FIGHTING = TextKey.of("death.attack.jscore.pressure.player",
            "%1$s was crushed by the pressure while fighting %2$s");

    /** A carried thing's hazard on its tooltip: its name, then what protects from it. */
    public static final TextKey TOOLTIP_HAZARD = TextKey.of("jscore.item_hazard.tooltip", "%s: %s");
    /** A worn thing's resistance on its tooltip: the kind of harm, then the share of it it stops. */
    public static final TextKey TOOLTIP_RESISTANCE = TextKey.of("jscore.resistance.tooltip", "%s: stops %s%%");

    private HazardTexts() {
    }
}
