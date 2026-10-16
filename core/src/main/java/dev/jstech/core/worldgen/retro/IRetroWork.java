/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.retro;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.LevelChunk;

/** What is generated in a chunk made before it was part of the world's generation ({@link Retrogen}). */
@FunctionalInterface
public interface IRetroWork {

    /**
     * Generates in {@code chunk}, whose eight neighbours are loaded, so the work may reach a little into them.
     *
     * @param random the same each time for the same chunk and work, so a world generates alike however it is played
     */
    void generate(ServerLevel level, LevelChunk chunk, RandomSource random);
}
