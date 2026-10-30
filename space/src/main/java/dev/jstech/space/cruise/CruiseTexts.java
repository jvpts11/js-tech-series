/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.cruise;

import dev.jstech.core.notification.NotificationKind;
import dev.jstech.core.notification.NotificationLevel;
import dev.jstech.core.notification.Notifications;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import net.minecraft.resources.ResourceLocation;

/** What the crew is told on the way between worlds: setting out, arriving, and what happens on the way. */
@TextHolder
public final class CruiseTexts {

    public static final TextKey DEPARTED_NAME = TextKey.of("jsspace.cruise.departed", "Transfer burn");
    public static final TextKey DEPARTED = TextKey.of("jsspace.cruise.departed.body",
            "On the way to %s, arriving in %s.");
    public static final TextKey ARRIVED_NAME = TextKey.of("jsspace.cruise.arrived", "Arrived");
    public static final TextKey ARRIVED = TextKey.of("jsspace.cruise.arrived.body", "In the orbit of %s.");
    public static final TextKey AEROBRAKED = TextKey.of("jsspace.cruise.aerobraked",
            "Aerobraked into the orbit of %s.");
    public static final TextKey MISSED_NAME = TextKey.of("jsspace.cruise.missed", "Not captured");
    public static final TextKey NOT_CAPTURED = TextKey.of("jsspace.cruise.missed.body",
            "Too little delta-v to slow into the orbit of %s: the rocket flew past it.");
    public static final TextKey SLEPT = TextKey.of("jsspace.cruise.slept", "You slept through the cruise.");
    public static final TextKey STORM_NAME = TextKey.of("jsspace.cruise.storm", "Solar storm");
    public static final TextKey STORM_COMING = TextKey.of("jsspace.cruise.storm.coming",
            "A solar storm reaches the rocket in %s seconds: shelter behind the water and the cargo.");
    public static final TextKey HOLED_NAME = TextKey.of("jsspace.cruise.holed", "Micrometeoroid");
    public static final TextKey HOLED = TextKey.of("jsspace.cruise.holed.body",
            "A micrometeoroid holed the hull: patch the hole before the air runs out.");
    public static final TextKey FAILURE_NAME = TextKey.of("jsspace.cruise.failure", "Failure");
    public static final TextKey FAILURE = TextKey.of("jsspace.cruise.failure.body",
            "A part burnt through and lost a block: put it back to mend it.");
    /** Setting out on a transfer, and arriving. */
    public static final NotificationKind DEPARTURE = Notifications.kind(id("departure"), NotificationLevel.INFO,
            DEPARTED_NAME).declare();
    public static final NotificationKind ARRIVAL = Notifications.kind(id("arrival"), NotificationLevel.INFO,
            ARRIVED_NAME).declare();
    /** Arriving without the delta-v to be captured. */
    public static final NotificationKind MISSED = Notifications.kind(id("missed"), NotificationLevel.ALERT,
            MISSED_NAME).declare();
    /** A solar storm coming, and past. */
    public static final NotificationKind STORM = Notifications.kind(id("solar_storm"), NotificationLevel.ALERT,
            STORM_NAME).declare();
    /** A hole in the hull, and a failure. */
    public static final NotificationKind HOLE = Notifications.kind(id("micrometeoroid"), NotificationLevel.ALERT,
            HOLED_NAME).declare();
    public static final NotificationKind BREAKDOWN = Notifications.kind(id("failure"), NotificationLevel.WARNING,
            FAILURE_NAME).declare();

    private CruiseTexts() {
    }

    /** Loads the texts and the kinds of notification, once, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "cruise_" + name);
    }
}
