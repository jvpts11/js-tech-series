/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.variant;

import dev.jstech.core.text.Text;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Makes the block of one variant, for a kind whose blocks are more than a plain block: a casing that is part of a
 * multiblock, a frame that turns. It is handed the properties of the block the variant is made from, copied whole
 * (hardness, sound, the tool it asks for), the material, and the name the variant goes by, which the block shows as
 * its own.
 */
@FunctionalInterface
public interface IVariantBlockMaker {

    /** A plain block, named as the variant: the default. */
    IVariantBlockMaker PLAIN = (properties, material, name) -> new VariantBlock(properties, name);

    /** The block of the variant of {@code material}, made with {@code properties} and called {@code name}. */
    Block make(BlockBehaviour.Properties properties, FoundMaterial material, Text name);
}
