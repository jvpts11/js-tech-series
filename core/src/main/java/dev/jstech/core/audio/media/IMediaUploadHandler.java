/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import dev.jstech.core.text.Text;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Whoever takes the recordings players bring for one purpose: a music player that puts a song on a computer's disk,
 * say. It may refuse one before a byte of it is sent, and it is handed the recording once the server keeps it.
 */
public interface IMediaUploadHandler {

    /**
     * Why the player may not bring this recording here, or null when they may.
     *
     * @param context what the player's side said to do with it, as that side wrote it
     */
    @Nullable
    Text refuse(ServerPlayer player, String context, String name, MediaId media);

    /**
     * The recording has come whole and the server keeps it: do with it what it was brought for.
     *
     * @return what the player is told it did, or {@link Text#EMPTY}
     */
    Text received(ServerPlayer player, String context, String name, MediaId media, MediaInfo info);
}
