/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * The world by where things are: a spatial index of boxes filed region by region, kept per dimension with the world,
 * and the chunks machines keep loaded for their owners, within a limit per owner and the server's budget across every
 * owner together.
 *
 * <p>A mod declares a {@link RegionIndex} once, with its id and the codec of what each entry holds, and keeps values
 * over boxes of a dimension ({@link Box}) under keys: {@code put}, {@code get} and {@code remove} by key,
 * {@code containing} a place, {@code within} an area, {@code nearest} a place within a radius. Each box is filed under
 * every region of 512 by 512 blocks it touches, so a search reads only the regions round the place, however many boxes
 * the world holds; {@link SpatialIndex} is the same index without the world, for code that keeps its own. A machine
 * that must work while nobody is near asks {@link ChunkLoaders} to {@code load} chunks for its owner, ticking or only
 * loaded, and the answer ({@link ChunkLoaders.LoadOutcome}) says when the owner's number or the server's budget is
 * reached. Breaking the machine should {@code releaseAll}, and a source that holds nothing when the world loads again
 * has its chunks let go. The numbers are the balance file's ({@link ChunkLoadingBalance}), and the chunks each source
 * keeps are an area on its owner's team's maps ({@link LoadedChunkMarkers}).
 *
 * <p>Both are explained, from zero, in {@code core/docs/WORLD.md}. Examples that compile are the Core's tests of the
 * kit ({@code tests/src/main/java/dev/jstech/tests/gametest/core/RegionGameTests.java}) and the Core's Space Persistor,
 * a machine that keeps an area round it loaded
 * ({@code core/src/main/java/dev/jstech/core/jstechseries/persistor/SpacePersistorBlockEntity.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.region;

import org.jetbrains.annotations.ApiStatus;
