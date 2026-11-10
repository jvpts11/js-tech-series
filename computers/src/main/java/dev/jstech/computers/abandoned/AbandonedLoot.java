/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.abandoned;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.jstechseries.tier.Era;
import net.minecraft.resources.ResourceLocation;

/**
 * The loot tables of the abandoned buildings (computers/docs/design/survival.md, "Abandoned buildings"): each
 * building's chests of each era under {@code jsc:chests/abandoned/<building>/<era>}, its main chest, which also holds
 * the way to a graphical system, under {@code .../<era>_main}, and the hardware boxes of each era and kind under
 * {@code jsc:chests/abandoned/hardware_box/<era>_<kind>}.
 */
public final class AbandonedLoot {

    private static final String ROOT = "chests/abandoned/";

    private AbandonedLoot() {
    }

    /** The table {@code building}'s chests of {@code era} draw from. */
    public static ResourceLocation table(final AbandonedBuilding building, final Era era) {
        return id(ROOT + building.serializedName() + "/" + era.path());
    }

    /** The table {@code building}'s main chest of {@code era} draws from: its chests' and a graphical system. */
    public static ResourceLocation main(final AbandonedBuilding building, final Era era) {
        return id(ROOT + building.serializedName() + "/" + era.path() + "_main");
    }

    /** The table a hardware box of {@code era} printed with {@code kind} draws from. */
    public static ResourceLocation box(final Era era, final HardwareBoxKind kind) {
        return id(ROOT + "hardware_box/" + era.path() + "_" + kind.getSerializedName());
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }
}
