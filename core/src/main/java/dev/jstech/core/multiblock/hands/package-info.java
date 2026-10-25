/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Formed multiblocks worked by hand: the parts of a structure's formed model a player opens, fills and presses in the
 * world, without its screen. A mod names them by the model's bones in an {@link InteractiveModel}: doors that swing on
 * a {@link Hinge}, places that each show a slot of the terminal's inventory and take the thing in hand, reached only
 * through their door's doorway once it is wholly open, and controls that do an {@link IHandControl}'s action. The
 * terminal keeps whether each door is open in its {@link StructureHands}, saved with it and sent to whoever sees it.
 *
 * <p>{@link HandGeometry} is where the parts stand, the same on the server and on the player's game, and free of the
 * game: which part a line of sight meets first, the box a part is outlined by, the doors turned as far as they are
 * open, and the model placed round its terminal. On the player's game the part under the crosshair is outlined in
 * place of the block, and a click on it is sent as a {@link HandUsePayload}; {@link HandUse} works it on the server
 * after checking that the player may open the terminal's screen and that their own line of sight meets the part.
 *
 * <p>How to use it is in {@code core/docs/MULTIBLOCKS.md}, under "Worked by hand". A working example is the
 * Computational Research Centre of J's Industrial ({@code industrial/src/main/java/dev/jstech/industrial/crc/
 * CrcContent.java}), with its game test
 * ({@code tests/src/main/java/dev/jstech/tests/gametest/industrial/CrcGameTests.java}) and the test of what a player
 * sees ({@code tests/src/main/java/dev/jstech/tests/clienttest/industrial/CrcHandsClientTests.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.multiblock.hands;

import org.jetbrains.annotations.ApiStatus;
