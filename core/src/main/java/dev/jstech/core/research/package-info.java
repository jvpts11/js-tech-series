/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Research, for any mod: discoveries a team makes with research computers, each a piece of knowledge the team holds in
 * part while it is researched and whole once it is made, when the recipes that ask for it open.
 *
 * <p>A mod declares its kinds of data ({@link DataKind}, {@link DataKinds}) with an item for each
 * ({@link ResearchDataItem}), and its discoveries with a builder ({@link Discovery#builder}): what each is called and
 * when it was made, what it costs in points and in data ({@link ResearchCost}, which a data pack may change), and what
 * it comes after. Its research computers work them on with {@link Research#work}, once a second, by the points they
 * make; each kind of data a discovery asks for is spent a part of the way at a time ({@link ResearchMath}). A mod's
 * own setting may turn its research off ({@link Discoveries#gate}).
 *
 * <p>How a mod uses it: {@code core/docs/PROGRESSION.md}.
 */
@ApiStatus.Experimental
package dev.jstech.core.research;

import org.jetbrains.annotations.ApiStatus;
