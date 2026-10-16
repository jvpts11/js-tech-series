/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.map;

import dev.jstech.core.map.Marker;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;

/** How a map mod is handed the markers to show: those of the dimension the player is in, in place of the last. */
public interface IMapAdapter {

    /** Shows {@code markers}, all of {@code level}'s dimension, in place of the markers it showed before. */
    void show(ClientLevel level, List<Marker> markers);
}
