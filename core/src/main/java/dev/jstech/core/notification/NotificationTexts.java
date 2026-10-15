/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.notification;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the notifications, their history and the key that opens it say. */
@TextHolder
public final class NotificationTexts {

    public static final TextKey HISTORY_KEY = TextKey.of("key.jscore.notifications", "Notification history");
    public static final TextKey TITLE = TextKey.of("jscore.notification.title", "Notifications");
    public static final TextKey ALL = TextKey.of("jscore.notification.all", "All");
    public static final TextKey ALERTS = TextKey.of("jscore.notification.alerts", "Alerts");
    public static final TextKey WARNINGS = TextKey.of("jscore.notification.warnings", "Warnings");
    public static final TextKey INFORMATION = TextKey.of("jscore.notification.information", "Information");
    /* The mod the history shows, or every mod. */
    public static final TextKey MODS = TextKey.of("jscore.notification.mods", "Mods: %s");
    /* Where the notifications appear. */
    public static final TextKey PLACE = TextKey.of("jscore.notification.place", "Place: %s");
    public static final TextKey TOP_RIGHT = TextKey.of("jscore.notification.top_right", "Top right");
    public static final TextKey TOP_LEFT = TextKey.of("jscore.notification.top_left", "Top left");
    public static final TextKey BOTTOM_RIGHT = TextKey.of("jscore.notification.bottom_right", "Bottom right");
    public static final TextKey BOTTOM_LEFT = TextKey.of("jscore.notification.bottom_left", "Bottom left");
    public static final TextKey DO_NOT_DISTURB = TextKey.of("jscore.notification.do_not_disturb", "Do not disturb");
    public static final TextKey HINT = TextKey.of("jscore.notification.hint",
            "Right button on a line: silence its kind or its mod.");
    public static final TextKey DONE = TextKey.of("jscore.notification.done", "Done");
    public static final TextKey EMPTY = TextKey.of("jscore.notification.empty", "No notifications yet");
    /* The kind's name. */
    public static final TextKey SILENCE_KIND = TextKey.of("jscore.notification.silence_kind", "Silence %s");
    /* The mod's name. */
    public static final TextKey SILENCE_MOD = TextKey.of("jscore.notification.silence_mod", "Silence everything of %s");
    /* The kind's name. */
    public static final TextKey HEAR_KIND = TextKey.of("jscore.notification.hear_kind", "Hear %s again");
    /* The mod's name. */
    public static final TextKey HEAR_MOD = TextKey.of("jscore.notification.hear_mod", "Hear %s again");
    public static final TextKey FORGET = TextKey.of("jscore.notification.forget", "Take it off the list");
    /* How many alike came. */
    public static final TextKey COUNT = TextKey.of("jscore.notification.count", "x%s");
    public static final TextKey OPEN_PAGE = TextKey.of("jscore.notification.open_page", "Click: open its page");
    public static final TextKey SHOW_PLACE = TextKey.of("jscore.notification.show_place", "Click: show it on the map");
    /* The place's coordinates, its dimension and how far it is, in blocks. */
    public static final TextKey PLACE_AT = TextKey.of("jscore.notification.place_at", "%s in %s, %s blocks away");
    /* The place's coordinates and its dimension, another than the player's. */
    public static final TextKey PLACE_ELSEWHERE = TextKey.of("jscore.notification.place_elsewhere",
            "%s in %s, another dimension");
    public static final TextKey HERE = TextKey.of("jscore.notification.here", "It");
    public static final TextKey YOU = TextKey.of("jscore.notification.you", "You");
    public static final TextKey NORTH = TextKey.of("jscore.notification.north", "N");

    private NotificationTexts() {
    }
}
