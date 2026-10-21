/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Declaring a mod's content once: its blocks and items, the block entities its blocks make and the menus they open, its
 * fluids, cables, parts, entities, sounds, fonts and models, and its creative tabs. Each declaration carries the name,
 * look, drops, tab and tags of what it registers, and the data generator and the checks of the series read them from
 * there, so nothing is a second list kept in step by hand.
 *
 * <p>A mod keeps one {@link ModContent}, made before any declaration and handed its event bus by
 * {@link ModContent#register}. A block is declared with a {@link BlockBuilder}, which gives back a {@link BlockEntry},
 * and an item with an {@link ItemBuilder}, which gives back an {@link ItemEntry}. How a block looks is an
 * {@link IBlockLook} made of {@link IBlockModel} files, how an item looks an {@link IItemLook}, and what a block drops
 * one of the {@link Drops}. A {@link ContentTab} shows, section by section, whatever is declared into it, in the order
 * it was declared. A block that makes a block entity, ticks it and opens its menu is a {@link DeviceBlock} made from a
 * {@link Device}. Fluids, cables, parts, entities, sounds and their cues, fonts and models have builders of their own
 * on the same {@code ModContent}: {@link FluidBuilder}, {@link CableBuilder}, {@link PartBuilder},
 * {@link EntityBuilder}, {@link SoundBuilder}, {@link CueBuilder}, {@link FontBuilder} and {@link ModelBuilder}. The
 * data generation ({@link ContentData}) writes the block states, models, English names, loot tables and tags from the
 * declarations.
 *
 * <p>How to declare each, from zero, is in {@code core/docs/CONTENT.md}, once {@code core/docs/GETTING_STARTED.md} has
 * set up the {@code ModContent}. An example that compiles is J's Example's content
 * ({@code example/src/main/java/dev/jstech/example/ExampleContent.java}): its machines, tank, oil, energy cable, fluid
 * pipe, level sensor and logbook, each declared once.
 */
@ApiStatus.Experimental
package dev.jstech.core.content;

import dev.jstech.core.datagen.ContentData;
import org.jetbrains.annotations.ApiStatus;
