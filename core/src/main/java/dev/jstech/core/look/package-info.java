/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * What a block entity says to a player who looks at it: a machine at work and how far along, a tank and what it holds.
 * Its lines are worked out on the server, so they can say what only the server knows, and sent to the player's game for
 * the look-at tooltips to show: Jade's, when Jade is installed.
 *
 * <p>A block entity implements {@link IDescribed} and adds its lines, in the order they are read, and more of them
 * while the player holds the key for details. A block of several pieces, a cable with parts on it or a space of parts,
 * implements {@link IDescribedAt} and says what the piece the player's look picks is doing. The lines are {@link Text},
 * so each player reads them in their own language; {@link LookTexts} holds the sentences the Core's own block entities
 * say (working, idle, low power, owner, redstone), which a mod's block entities may say too.
 *
 * <p>How to use it, from zero, is in {@code core/docs/OVERLAYS.md}. Examples that compile are the Core's battery bank
 * ({@code core/src/main/java/dev/jstech/core/jstechseries/energy/BatteryBankBlockEntity.java}), which says its charge,
 * and J's Example's machines ({@code example/src/main/java/dev/jstech/example/machine}), which the machine kit
 * describes for them.
 */
@ApiStatus.Experimental
package dev.jstech.core.look;

import dev.jstech.core.text.Text;
import org.jetbrains.annotations.ApiStatus;
