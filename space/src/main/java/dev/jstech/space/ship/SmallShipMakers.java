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
import dev.jstech.space.api.ShipMaker;
import dev.jstech.space.api.ShipMakers;
import net.minecraft.resources.ResourceLocation;

/**
 * The three makers of the small starship's parts, each owning a family of the catalogue and giving the parts one goes
 * into its own interior: Meridian Orbital, the orbiter line, clean as a spaceflight cabin (padded white panels, a red
 * line at hand height, light panels overhead); Ironside Yards, the working line, industrial (bare plates on ribs,
 * grating floors); and Kosmozavod, Soviet in style (turquoise painted panels over beige quilted padding, a wooden
 * floor, dome lamps).
 */
@TextHolder
public final class SmallShipMakers {

    public static final TextKey MERIDIAN = TextKey.of("jsspace.ship_maker.meridian_orbital", "Meridian Orbital");
    public static final TextKey IRONSIDE = TextKey.of("jsspace.ship_maker.ironside_yards", "Ironside Yards");
    public static final TextKey KOSMOZAVOD = TextKey.of("jsspace.ship_maker.kosmozavod", "Kosmozavod");
    /** Their marks, three letters each. */
    public static final TextKey MERIDIAN_MARK = TextKey.of("jsspace.ship_maker.meridian_orbital.mark", "MER");
    public static final TextKey IRONSIDE_MARK = TextKey.of("jsspace.ship_maker.ironside_yards.mark", "IRN");
    public static final TextKey KOSMOZAVOD_MARK = TextKey.of("jsspace.ship_maker.kosmozavod.mark", "KZV");
    /** Meridian Orbital's id. */
    public static final ResourceLocation MERIDIAN_ORBITAL = id("meridian_orbital");
    /** Ironside Yards' id. */
    public static final ResourceLocation IRONSIDE_YARDS = id("ironside_yards");
    /** Kosmozavod's id. */
    public static final ResourceLocation KOSMOZAVOD_ID = id("kosmozavod");

    private SmallShipMakers() {
    }

    /** Declares the makers, once, from the mod's constructor. */
    public static void declare() {
        ShipMakers.declare(MERIDIAN_ORBITAL, new ShipMaker(MERIDIAN.key(), MERIDIAN_MARK.key()));
        ShipMakers.declare(IRONSIDE_YARDS, new ShipMaker(IRONSIDE.key(), IRONSIDE_MARK.key()));
        ShipMakers.declare(KOSMOZAVOD_ID, new ShipMaker(KOSMOZAVOD.key(), KOSMOZAVOD_MARK.key()));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
