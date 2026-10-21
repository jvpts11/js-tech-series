/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * What each face of a block lets in and out, which items pass a filter, and moving things from one store to another:
 * the inventory kit a machine, a bus or a pipe uses beside the game's own item, fluid and energy handlers.
 *
 * <p>A block that lets its owner choose what each face does keeps a {@link FaceConfig} among its fields: each face,
 * named from the block's own front, set to a {@link FaceMode} (closed, in, out, or in and out), so turning the block
 * keeps what each face does, and its item handler handed out through it one face at a time. {@link FaceItemHandler},
 * {@link FaceFluidHandler} and {@link FaceEnergyStorage} are a block's own stores as one face gives them out. An
 * {@link ItemFilter} is a list of rules (an exact item, an item whatever its damage, every item of a tag, everything a
 * mod adds), each with an amount, that name what passes or what does not; {@link ItemFilters} applies one to the game's
 * stacks and saves and sends it, and the same filter looks at fluids and materials by their ids
 * ({@link IFilterSubject}: a {@link StackSubject}, a {@link FluidSubject}, a {@link NamedSubject}). {@link Transfers}
 * moves the items that pass a filter, a fluid or energy from one store to another as one batch, so what leaves one
 * store arrives in the other or stays where it was. Lines that carry items move them, end by end, by the same filters
 * ({@link LevelItems}).
 *
 * <p>How to use it, from zero, is in {@code core/docs/ENERGY_AND_RESOURCES.md}. An example that compiles is the test
 * mod's inventory tests ({@code tests/src/main/java/dev/jstech/tests/gametest/core/InventoryGameTests.java}): filters
 * on the game's stacks and their save, faces that turn with the block, and moves from one store to another.
 */
@ApiStatus.Experimental
package dev.jstech.core.inventory;

import org.jetbrains.annotations.ApiStatus;
