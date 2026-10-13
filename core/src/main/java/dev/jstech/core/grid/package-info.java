/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
/**
 * Grids: cables joined into runs and connected parts, one grid of each kind in each dimension shared by every line
 * of that kind, with the slowest cable on a way and each run's length against its range worked out when the grid
 * changes, never on every tick.
 */
package dev.jstech.core.grid;
