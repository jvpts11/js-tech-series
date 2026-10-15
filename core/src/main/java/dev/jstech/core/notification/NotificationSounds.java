/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.notification;

import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.registry.CoreItems;
import net.minecraft.resources.ResourceLocation;

/**
 * What the notifications sound like, one sound for each level, played from the player's screen in the Interface
 * channel: the game's own toast coming in for information, a note block's pling for a warning and its bell for an
 * alert. None is a file of the Core's own.
 */
public final class NotificationSounds {

    /** A notification of information comes: the game's toast sliding in. */
    public static final SoundKey INFO = CoreItems.CONTENT.sound("notification/info").onScreen()
            .channel(AudioChannels.INTERFACE).file(game("ui/toast/in")).subtitle("Notification comes").register();

    /** A warning comes: a note block's pling. */
    public static final SoundKey WARNING = CoreItems.CONTENT.sound("notification/warning").onScreen()
            .channel(AudioChannels.INTERFACE).file(game("note/pling")).subtitle("Warning comes").register();

    /** An alert comes: a note block's bell. */
    public static final SoundKey ALERT = CoreItems.CONTENT.sound("notification/alert").onScreen()
            .channel(AudioChannels.INTERFACE).file(game("note/bell")).subtitle("Alert comes").register();

    private NotificationSounds() {
    }

    /** Declares the sounds while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The sound of {@code level}. */
    public static SoundKey of(final NotificationLevel level) {
        return switch (level) {
            case INFO -> INFO;
            case WARNING -> WARNING;
            case ALERT -> ALERT;
        };
    }

    private static ResourceLocation game(final String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
