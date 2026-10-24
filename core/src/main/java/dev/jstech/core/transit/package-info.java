/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Pneumatic post: stations on a line of tubes, each with an address, that send the items put into them in capsules to
 * the station of an address, by the routes they are set to, pushing only: a station never asks for anything. A capsule
 * takes time by the distance it travels and is kept with the world while it is on its way
 * ({@link CapsuleTransit}); it unloads into the station it reaches ({@link CapsuleStationBlockEntity}), which unloads
 * into the block it faces. A mod gives its stations its rules ({@link CapsuleRules}), its tube line, a grid that
 * groups the stations a run joins, and what a station spends to send.
 */
@ApiStatus.Experimental
package dev.jstech.core.transit;

import org.jetbrains.annotations.ApiStatus;
