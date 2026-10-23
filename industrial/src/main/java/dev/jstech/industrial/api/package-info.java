/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */

/**
 * What J's Industrial promises to a mod built on it, beside what J's Core promises every mod: the number of the shape
 * of this promise ({@link JsIndustrialApi#VERSION}), the switch every consequence of the mod has
 * ({@link Consequences}), the four tabs of its advancements an add-on hangs its own under
 * ({@link IndustrialAdvancementTab}), and the groups its sounds are kept in ({@link IndustrialSoundGroup}).
 *
 * <p>Most of what an add-on builds with is J's Core's: materials, machines, multiblocks, energy, the cable block.
 * Everything here is declared explicitly, through builders and constructors, while the game loads; nothing registers
 * itself by annotation. Everything else in the mod is its own business and changes without warning. All of it is new,
 * and so experimental until it has been through a release.
 */
@ApiStatus.Experimental
package dev.jstech.industrial.api;

import org.jetbrains.annotations.ApiStatus;
