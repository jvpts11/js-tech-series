/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.ShipFinish;
import dev.jstech.space.api.ShipFinishes;
import net.minecraft.resources.ResourceLocation;

/**
 * The three finishes of J's Space's starships: the orbiter's (white quilted blankets over black heat tiles, carbon
 * edges, the red band), bare metal (plates each of its own tone, patched, a crimson band and hazard stripes round its
 * doors) and enamel (crimson over cream, parted by a gold line).
 */
@TextHolder
public final class SmallShipFinishes {

    public static final TextKey ORBITER = TextKey.of("jsspace.ship_finish.orbiter", "Orbiter");
    public static final TextKey BARE_METAL = TextKey.of("jsspace.ship_finish.bare_metal", "Bare Metal");
    public static final TextKey ENAMEL = TextKey.of("jsspace.ship_finish.enamel", "Enamel");
    /** The finish a ship is painted in when none is chosen. */
    public static final ResourceLocation DEFAULT = id("orbiter");

    private SmallShipFinishes() {
    }

    /** Declares the finishes, once, from the mod's constructor. */
    public static void declare() {
        ShipFinishes.declare(id("orbiter"), new ShipFinish(ORBITER.key()));
        ShipFinishes.declare(id("bare_metal"), new ShipFinish(BARE_METAL.key()));
        ShipFinishes.declare(id("enamel"), new ShipFinish(ENAMEL.key()));
    }

    /** The id of J's Space's finish {@code path}. */
    public static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
