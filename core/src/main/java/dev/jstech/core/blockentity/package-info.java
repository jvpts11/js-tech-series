/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
/**
 * Block entities whose state is declared once, field by field, each field saying where it goes: into the save, to
 * the players who see the block, to the menu open on it. The base writes the saving, the syncing and the menu data
 * from those declarations, so no block entity writes them by hand.
 */
package dev.jstech.core.blockentity;
