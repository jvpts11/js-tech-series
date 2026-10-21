/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Block entities whose state is declared once, field by field, each field saying where it goes: into the save, to the
 * players who see the block, to the menu open on it, to the pipes, cables and machines beside it. The base writes the
 * saving, the syncing and the menu data from those declarations, so no block entity writes them by hand.
 *
 * <p>A block entity extends {@link SyncedBlockEntity} and declares its fields in its constructor, through
 * {@link BlockEntityFields}: a number ({@link IntField}, {@link LongField}), a true-or-false ({@link BoolField}), a
 * value worked out and never saved ({@link DerivedInt}), an inventory ({@link FieldItemHandler}), an energy store
 * ({@link FieldEnergyStorage}), a tank ({@link FieldFluidTank}), a value of any type a codec writes
 * ({@link ValueField}), or a piece that writes itself ({@link IFieldPart}, declared as a {@link PartField}). Each is
 * marked with where it goes: {@code save()}, {@code toClient()}, {@code toMenu()}, and for a store {@code exposed()}.
 * What changed in a tick is sent once, at its end. What is exposed is offered through the game's capabilities for every
 * block entity type declared through {@code ModContent.blockEntity} ({@link FieldCapabilities}), face by face for one
 * that is an {@link IFaceGate}.
 *
 * <p>How to declare one, from zero, is in {@code core/docs/CONTENT.md}. Examples that compile are J's Example's tank
 * ({@code example/src/main/java/dev/jstech/example/machine/TankBlockEntity.java}), whose tank is saved, sent and
 * exposed, and its generator, in the same folder, whose burning time is saved and shown to its screen.
 */
@ApiStatus.Experimental
package dev.jstech.core.blockentity;

import org.jetbrains.annotations.ApiStatus;
