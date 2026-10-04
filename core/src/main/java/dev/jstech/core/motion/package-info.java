/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
/**
 * Motion: how the things on a screen move, as data. A system's profile says, for each kind of thing it moves, the way
 * it moves, for how long, along which curve and under which of its switches; the mods declare each system's profile
 * with the real system's timings, the data generation writes it as a file a resource pack can replace, and every
 * motion is read against one clock each frame.
 */
package dev.jstech.core.motion;
