/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.descent;

import dev.jstech.core.notification.NotificationKind;
import dev.jstech.core.notification.NotificationLevel;
import dev.jstech.core.notification.Notifications;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import net.minecraft.resources.ResourceLocation;

/** What a rocket's crew is told of its coming down to a world, and of leaving it again. */
@TextHolder
public final class DescentTexts {

    public static final TextKey DESCENDING_NAME = TextKey.of("jsspace.descent.descending", "Deorbit burn");
    public static final TextKey DESCENDING = TextKey.of("jsspace.descent.descending.body", "Coming down to %s.");
    public static final TextKey LANDED_NAME = TextKey.of("jsspace.descent.landed", "Landed");
    public static final TextKey LANDED = TextKey.of("jsspace.descent.landed.body", "On %s.");
    public static final TextKey CRASHED_NAME = TextKey.of("jsspace.descent.crashed", "Crash landing");
    public static final TextKey CRASHED = TextKey.of("jsspace.descent.crashed.body",
            "The rocket came down too fast and crashed on %s.");
    public static final TextKey BURNT_NAME = TextKey.of("jsspace.descent.burnt", "Burning up");
    public static final TextKey BURNT = TextKey.of("jsspace.descent.burnt.body",
            "With no heat shield, the rocket burns up in the air of %s.");
    public static final TextKey HELD_NAME = TextKey.of("jsspace.descent.held", "Landing held");
    public static final TextKey NO_ESSENTIALS = TextKey.of("jsspace.descent.held.essentials",
            "The crew may not land on %s before its atmosphere, temperature, radiation, gravity and relief are known.");
    public static final TextKey NO_SHIELD = TextKey.of("jsspace.descent.held.shield",
            "No heat shield fit for the air of %s: the rocket stays in its orbit.");
    public static final TextKey NO_LEGS = TextKey.of("jsspace.descent.held.legs",
            "No legs, or no propellant to brake on them, for %s: the rocket stays in its orbit.");
    public static final TextKey LIFTED_NAME = TextKey.of("jsspace.descent.lifted", "Lift-off");
    public static final TextKey LIFTED = TextKey.of("jsspace.descent.lifted.body", "Leaving %s.");
    /** Coming down, and standing on the ground. */
    public static final NotificationKind DESCENT = Notifications.kind(id("descent"), NotificationLevel.INFO,
            DESCENDING_NAME).declare();
    public static final NotificationKind LANDING = Notifications.kind(id("landing"), NotificationLevel.INFO,
            LANDED_NAME).declare();
    /** Crashing, and burning up. */
    public static final NotificationKind CRASH = Notifications.kind(id("crash"), NotificationLevel.ALERT,
            CRASHED_NAME).declare();
    public static final NotificationKind BURNING = Notifications.kind(id("burning"), NotificationLevel.ALERT,
            BURNT_NAME).declare();
    /** A landing that cannot begin. */
    public static final NotificationKind HELD = Notifications.kind(id("held"), NotificationLevel.WARNING,
            HELD_NAME).declare();
    /** Leaving a world's ground again. */
    public static final NotificationKind LIFT_OFF = Notifications.kind(id("lift_off"), NotificationLevel.INFO,
            LIFTED_NAME).declare();

    private DescentTexts() {
    }

    /** Loads the texts and the kinds of notification, once, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "descent_" + name);
    }
}
