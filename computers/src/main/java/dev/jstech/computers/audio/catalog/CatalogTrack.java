/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio.catalog;

import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;

/**
 * One song of an album in the server's catalogue, with the name and the artist it is listed under.
 *
 * <p>This record is pure and carries no Minecraft dependency.
 *
 * @param file   the name of the file it was read from, in its album's folder
 * @param media  the recording the server keeps
 * @param info   what the recording is
 * @param title  what it is listed as: its own title, or its file's name
 * @param artist who it is listed as by: its own artist, or the album's
 * @param number its place on the album, counted from 1, or 0 when it does not say
 */
public record CatalogTrack(String file, MediaId media, MediaInfo info, String title, String artist, int number) {
}
