/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

/**
 * The world's rules for its hazards, as its settings file gives them: whether there is radiation at all, whether
 * minerals with uranium or thorium in them are naturally radioactive, whether creatures suffer hazards as players do,
 * whether dusts harm the lungs, whether what is carried harms its carrier, and whether contamination withers the land.
 */
public final class HazardBalance {

    private static volatile boolean radiation = true;
    private static volatile boolean naturalRadioactivity = true;
    private static volatile boolean mobsSuffer = true;
    private static volatile boolean lungs = true;
    private static volatile boolean itemHazards = true;
    private static volatile boolean withering = true;

    private HazardBalance() {
    }

    /**
     * Whether there is radiation at all: off, no kind of the radiation family builds up, contaminates or is given off,
     * and the instruments read nothing.
     */
    public static boolean radiation() {
        return radiation;
    }

    public static void setRadiation(final boolean value) {
        radiation = value;
    }

    /** Whether minerals with uranium or thorium in them are naturally radioactive. */
    public static boolean naturalRadioactivity() {
        return naturalRadioactivity;
    }

    public static void setNaturalRadioactivity(final boolean value) {
        naturalRadioactivity = value;
    }

    /** Whether creatures suffer hazards as players do: animals sicken in a contaminated place. */
    public static boolean mobsSuffer() {
        return mobsSuffer;
    }

    public static void setMobsSuffer(final boolean value) {
        mobsSuffer = value;
    }

    /** Whether dusts breathed harm the lungs. */
    public static boolean lungs() {
        return lungs;
    }

    public static void setLungs(final boolean value) {
        lungs = value;
    }

    /** Whether what a player carries harms them: a hot ingot, a radioactive rod, sodium in the rain. */
    public static boolean itemHazards() {
        return itemHazards;
    }

    public static void setItemHazards(final boolean value) {
        itemHazards = value;
    }

    /** Whether the world shows contamination: grass dying and plants withering where it is high. */
    public static boolean withering() {
        return withering;
    }

    public static void setWithering(final boolean value) {
        withering = value;
    }
}
