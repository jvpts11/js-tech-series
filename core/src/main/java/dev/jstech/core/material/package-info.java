/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * The one catalogue of what the world is made of, shared by every mod: materials (elements, isotopes, alloys,
 * minerals, compounds, liquids, gases, exotic matter), the forms they are made into, the real data they carry, the
 * rocks ores sit in and the chemistry minerals are processed by. A mod that makes tin dust and a mod that asks for it
 * agree on what tin dust is, and neither depends on the other.
 *
 * <p>A mod declares its materials from its constructor with {@link Materials}, each a {@link Material} of a
 * {@link MaterialKind}, with a colour and the values of its properties ({@link MaterialProperties}); a material has
 * no tier. A form of a material has a thing in the game only when an installed mod says it uses it, with
 * {@link MaterialUses}: the Core registers the union, each thing once, in its common tags, named from its form
 * ("Tin Plate") and drawn in its material's colour as the game loads, and each block mined with a pickaxe and given
 * its drops. The forms are open to any mod ({@link Forms}), and so are the rocks ores sit in ({@link HostRocks}) and
 * the families of minerals ({@link MineralFamilies}). A material's liquid exists the same way, once a mod uses it
 * ({@link MaterialLiquids}), and so does a generic component that is no material ({@link CatalogueItems}). A data
 * pack gives properties' values over a material's own ({@link MaterialValues}), and a mod marks the things of some
 * materials in their tooltips ({@link MaterialMarks}).
 *
 * <p>How to use it, from zero, is in {@code core/docs/CONTENT.md}, under Materials. Examples that compile are the test
 * mod's materials and uses ({@code tests/src/main/java/dev/jstech/tests/TestMaterials.java}) and the series' own
 * catalogue ({@code core/src/main/java/dev/jstech/core/jstechseries/SeriesMaterials.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.material;

import org.jetbrains.annotations.ApiStatus;
