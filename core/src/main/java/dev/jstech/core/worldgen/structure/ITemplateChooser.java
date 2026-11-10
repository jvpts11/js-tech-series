/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;

/**
 * Picks which of a {@link ChosenElement}'s templates stands at a place, as it is placed: by its distance from the
 * world's origin, by a setting, by anything the world says there. A mod registers one with
 * {@link TemplateChoosers#register}.
 *
 * <p>A structure is placed a chunk at a time, so the chooser is asked once in every chunk the piece reaches: it must
 * answer the same each time for the same place, which the {@code random} it is handed, seeded from the world's seed
 * and the place, does.
 */
@FunctionalInterface
public interface ITemplateChooser {

    /**
     * The template that stands at {@code at}, one of {@code templates}, and the processors placed with it besides the
     * pool's own.
     *
     * @param level     the world being generated
     * @param at        where the piece's corner stands
     * @param random    a random seeded from the world's seed and {@code at}, the same in every chunk
     * @param templates the templates it picks among, in the order declared
     */
    Choice choose(WorldGenLevel level, BlockPos at, RandomSource random, List<ResourceLocation> templates);

    /**
     * What a chooser picked.
     *
     * @param template   the template that stands
     * @param processors the processors placed with it besides the pool's own; none for none
     */
    record Choice(ResourceLocation template, List<StructureProcessor> processors) {

        public Choice {
            processors = List.copyOf(processors);
        }
    }
}
