/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Notifications a mod sends a player, drawn on the Core's HUD and not as the game's toasts, whose way of being drawn
 * changes with every version of the game. Each has a level with its icon, colour, sound and time on the screen; alike
 * ones become one with a count; a key opens the history of the recent ones; a click opens the right place; and the
 * player chooses where they appear, silences kinds and mods, and can ask not to be disturbed.
 *
 * <p>A mod declares each kind once, as a constant: {@link Notifications#kind} with its id, its
 * {@link NotificationLevel} and what the player calls it, then {@code declare()}, gives a {@link NotificationKind}.
 * From the server it makes a {@link Notification} of the kind, with a title and a line, adds what a click on it does if
 * anything, a {@link NotificationAction} that opens a manual's entry or shows a place on a map, and sends it:
 * {@link Notifications#send} to a player, {@link Notifications#sendTo} to a player by id when they are on,
 * {@link Notifications#sendToTeamOf} to every player on that player's team. Something the player's own game finds out
 * is shown there with {@code NotificationsClient.show}, in {@code dev.jstech.core.client.notification}. What shows on
 * the screen and what the history keeps is a {@link NotificationBoard}, pure and checked by a unit test; what the
 * player chose is in their own settings, {@link NotificationSettings}.
 *
 * <p>How to declare and send one, from zero, is in {@code core/docs/NOTIFICATIONS.md}. An example that compiles is the
 * test mod's notification tests, which declare a kind and send it every way
 * ({@code tests/src/main/java/dev/jstech/tests/gametest/core/NotificationGameTests.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.notification;

import org.jetbrains.annotations.ApiStatus;
