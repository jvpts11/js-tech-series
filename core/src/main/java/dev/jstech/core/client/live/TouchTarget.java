/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.live;

import java.util.Optional;
import net.minecraft.network.chat.Component;

/**
 * A part of a block's screen in the world under the crosshair: its name, which a press carries to the server, and what
 * a press on it does, which the action bar says ("Right click: Start"); a part a press only reaches has no words.
 *
 * @param name what the server knows the part by
 * @param does what a press on it does, or none
 */
public record TouchTarget(String name, Optional<Component> does) {
}
