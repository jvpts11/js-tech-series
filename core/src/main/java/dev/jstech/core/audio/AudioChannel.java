/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

/**
 * One of the channels the series' sounds are mixed in: machines, devices, the interface, alerts and the rest.
 *
 * <p>A channel rides on one of the game's own sound categories, so the game's sliders still govern it, and has a
 * volume of its own on top that the player sets apart from every other channel. The series declares its channels in
 * {@link AudioChannels}; an addon may declare more there, which is why this is a value and not a fixed list.
 *
 * @param id     what the channel is known by, in the player's settings and in the data that names it
 * @param source the game's sound category it plays under
 * @param name   what the channel is called where a player sets its volume
 */
public record AudioChannel(ResourceLocation id, SoundSource source, TextKey name) {
}
