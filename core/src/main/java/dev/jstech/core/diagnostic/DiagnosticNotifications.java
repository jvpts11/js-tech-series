/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import dev.jstech.core.JsCore;
import dev.jstech.core.notification.NotificationKind;
import dev.jstech.core.notification.NotificationLevel;
import dev.jstech.core.notification.Notifications;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;

/** What the operators are told of the series' work: a part that stalled, slowed itself or is back to its pace. */
@TextHolder
public final class DiagnosticNotifications {

    public static final TextKey STALLED_NAME = TextKey.of("jscore.notification.kind.stalled", "Work stalled");
    public static final TextKey SLOWED_NAME = TextKey.of("jscore.notification.kind.slowed", "Work slowed");
    public static final TextKey RECOVERED_NAME = TextKey.of("jscore.notification.kind.recovered", "Work at pace");

    /** A section ran far longer than it may. */
    public static final NotificationKind STALLED = Notifications.kind(id("stalled"), NotificationLevel.WARNING,
            STALLED_NAME).declare();
    /** A section went over its budget and slowed itself. */
    public static final NotificationKind SLOWED = Notifications.kind(id("slowed"), NotificationLevel.WARNING,
            SLOWED_NAME).declare();
    /** A section slowed by its budget is back to every tick. */
    public static final NotificationKind RECOVERED = Notifications.kind(id("recovered"), NotificationLevel.INFO,
            RECOVERED_NAME).declare();

    private DiagnosticNotifications() {
    }

    /** Declares the kinds, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
