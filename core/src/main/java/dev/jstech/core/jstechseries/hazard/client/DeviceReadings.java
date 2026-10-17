/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard.client;

import dev.jstech.core.client.hazard.HazardsClient;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.hazard.HazardKind;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.jstechseries.hazard.HazardDevices;
import dev.jstech.core.text.GameText;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * What the hazard devices read on the player's game, from what the server last told of the player's hazards: the
 * series' radiation (its rate here, as it comes before what is worn, and the dose taken), and the figures as the
 * devices print them.
 */
public final class DeviceReadings {

    private DeviceReadings() {
    }

    /** The series' radiation, when it is declared and on; the devices read nothing without it. */
    public static Optional<HazardKind> radiation() {
        return on(HazardDevices.RADIATION);
    }

    /** The hazard {@code id}, when it is declared and on. */
    public static Optional<HazardKind> on(final ResourceLocation id) {
        return Hazards.find(id).filter(HazardKind::enabled);
    }

    /** How fast {@code kind} comes at the player where they stand, before what they wear, in its rate's unit. */
    public static double rateHere(final HazardKind kind) {
        return kind.shownRate(HazardsClient.exposure(kind.id()));
    }

    /** How much of {@code kind} has built up in the player, in its unit. */
    public static double level(final HazardKind kind) {
        return HazardsClient.level(kind.id());
    }

    /** The unit {@code kind}'s rate is shown in, in words. */
    public static String rateUnit(final HazardKind kind) {
        return kind.rateUnit().map(GameText::resolve)
                .orElseGet(() -> GameText.resolve(HazardDevices.PER_SECOND.with(kind.unit().text())));
    }

    /** {@code value} as a device's face prints it: two decimals, fewer as it grows, in the player's language. */
    public static String figure(final double value) {
        final String pattern = value >= 1000 ? "%.0f" : value >= 100 ? "%.1f" : "%.2f";
        return String.format(UnitFormatter.forCurrentLocale().locale(), pattern, value);
    }
}
