/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */

/**
 * What J's Space promises to a mod built on it, beside what J's Core promises every mod: for now, the number of the
 * shape of this promise ({@link JsSpaceApi#VERSION}). Each registry an add-on adds to (worlds, their generators and
 * modifiers, landmarks, rocket parts, starship sections, kinds of satellite and probe, what can be discovered of a
 * world) joins it with the model it holds, and raises that number.
 *
 * <p>Most of what an add-on builds with is J's Core's: materials, multiblocks, moving structures, the sky, sealed
 * rooms. Everything here is declared explicitly, through builders and constructors, while the game loads; nothing
 * registers itself by annotation. Everything else in the mod is its own business and changes without warning. All of
 * it is new, and so experimental until it has been through a release.
 */
@ApiStatus.Experimental
package dev.jstech.space.api;

import org.jetbrains.annotations.ApiStatus;
