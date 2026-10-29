/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.space.api.SpaceSuit;
import dev.jstech.space.api.SpaceSuits;
import net.minecraft.resources.ResourceLocation;

/**
 * A pressure suit of J's Space for the tests, made of the game's iron armour, so its life support is proven before its
 * own suits stand: a minute of air in its tanks, and the cold and heat of the Moon's day and night.
 */
public final class TestSpaceSuits {

    /** The test suit's id. */
    public static final ResourceLocation SUIT = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_suit");
    /** How many seconds of breathing its tanks hold. */
    public static final int AIR = 60;
    /** The coldest and the hottest it stands, in degrees Celsius. */
    public static final double COLDEST = -150;
    public static final double HOTTEST = 120;

    private TestSpaceSuits() {
    }

    /** Declares the suit, before the game has loaded. */
    public static void declare() {
        SpaceSuits.declare(SUIT, new SpaceSuit("jstests.suit.test",
                ResourceLocation.withDefaultNamespace("iron_helmet"),
                ResourceLocation.withDefaultNamespace("iron_chestplate"),
                ResourceLocation.withDefaultNamespace("iron_leggings"),
                ResourceLocation.withDefaultNamespace("iron_boots"), AIR, COLDEST, HOTTEST));
    }
}
