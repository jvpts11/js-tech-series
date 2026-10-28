/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */

/**
 * What J's Space promises to a mod built on it, beside what J's Core promises every mod: the number of the shape of
 * this promise ({@link JsSpaceApi#VERSION}), the tabs of its advancements, which an add-on hangs its own under
 * ({@link SpaceAdvancementTab}), and its worlds: the world model ({@link SpaceWorld}), the worlds themselves
 * ({@link SpaceWorlds}, which data packs add to as well), and the generators and modifiers that shape them
 * ({@link WorldGenerators}, {@link WorldModifiers}), and what can be known of a world ({@link WorldProperties}). Each
 * other registry an add-on adds to (landmarks, rocket parts, starship sections, kinds of satellite and probe) joins it
 * with the model it holds, and raises that number.
 *
 * <p>Most of what an add-on builds with is J's Core's: materials, multiblocks, moving structures, the sky, sealed
 * rooms. Everything here is declared explicitly, through builders and constructors, while the game loads; nothing
 * registers itself by annotation. Everything else in the mod is its own business and changes without warning. All of
 * it is new, and so experimental until it has been through a release.
 */
@ApiStatus.Experimental
package dev.jstech.space.api;

import org.jetbrains.annotations.ApiStatus;
