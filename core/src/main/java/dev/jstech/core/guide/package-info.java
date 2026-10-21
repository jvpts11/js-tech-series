/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Manuals for any mod: the book a player opens in the game to learn what each block and item is, what it is for, how
 * to get it, how to use it and what can go wrong. The Core draws the book in its style, numbers it as technical
 * manuals are numbered, builds its contents and its index, searches it, opens it at an item's page when the player
 * holds the manual key over the item, and reads it as text or as a website. A mod writes what the pages say.
 *
 * <p>A mod writes through the {@link ModGuide} its {@link ModContent} gives it: a {@link GuideChapter}, its
 * {@link GuideSection}s, and a {@link GuideEntry} for each thing a player looks up, made of {@link GuideBlock}s
 * (paragraphs, figures, tables, recipes, steps, warnings, scenes of blocks), all declared once, in English, and
 * written out by the data generation as files and keys any language translates. A {@link GuideManual} holds whole
 * chapters, so an entry written once shows in the mod's own manual and in the series' Technical Reference
 * ({@link CoreGuide}), each drawn in its {@link GuideStyle}, which is only data. A {@link ManualItem} opens a manual,
 * and {@link GuideGifts} hands one to each player once, as they first join a world. {@link GuideFacts} gives a page
 * the figures and the rows of tables it reads from the mod as it is laid out, so they are never out of date, and
 * {@link GuidePages} says which entry a stack is the page of. The layout knows nothing of the game
 * ({@link GuideLayout}, measuring its words through an {@link IGuideText}), so it is tested with no game around it.
 *
 * <p>How to write one, from zero, is in {@code core/docs/MANUALS.md}. An example that compiles is J's Industrial's
 * chapter and its Plant Drawings ({@code industrial/src/main/java/dev/jstech/industrial/guide/IndustrialGuide.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.guide;

import dev.jstech.core.content.ModContent;
import org.jetbrains.annotations.ApiStatus;
