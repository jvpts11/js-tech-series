/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import java.util.Optional;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Places a structure where a mod or a story says, as the world would have placed it: laid out from the chunk asked, as
 * its own rules say (where the land suits it, sunk, worn, on foundations), built into the chunks it covers, and known
 * to the world afterwards: its start and its references kept in those chunks, so the instruments find it, what it
 * remembers is kept ({@link StructureStates}), and a settlement grows ({@link Settlements}). The game's own command
 * builds the blocks and forgets the structure; this keeps it. Its biomes are not asked: the caller chose the place.
 *
 * <pre>{@code
 * StructurePlacer.place(level, rl("crashed_probe"), new ChunkPos(player.blockPosition()))
 *         .ifPresent(start -> MapMarkers.show(player, Marker.point("probe", FOUND.id(), level.dimension(),
 *                 start.getBoundingBox().getCenter(), name)));
 * }</pre>
 */
public final class StructurePlacer {

    private StructurePlacer() {
    }

    /**
     * Places the structure {@code id} from the chunk {@code at} of {@code level}, loading the chunks it covers.
     *
     * @return what was placed; empty when no such structure is registered or it does not stand there (the land does
     *         not suit it, or its layout fails)
     */
    public static Optional<StructureStart> place(final ServerLevel level, final ResourceLocation id,
                                                 final ChunkPos at) {
        final Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
        if (structure == null) {
            return Optional.empty();
        }
        final ChunkGenerator generator = level.getChunkSource().getGenerator();
        final StructureStart start = structure.generate(level.registryAccess(), generator,
                generator.getBiomeSource(), level.getChunkSource().randomState(), level.getStructureManager(),
                level.getSeed(), at, 0, level, biome -> true);
        if (!start.isValid()) {
            return Optional.empty();
        }
        final LevelChunk first = level.getChunk(at.x, at.z);
        level.structureManager().setStartForStructure(SectionPos.bottomOf(first), structure, start, first);
        final BoundingBox box = start.getBoundingBox();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                final LevelChunk chunk = level.getChunk(cx, cz);
                level.structureManager().addReferenceForStructure(SectionPos.bottomOf(chunk), structure,
                        at.toLong(), chunk);
                final ChunkPos pos = chunk.getPos();
                start.placeInChunk(level, level.structureManager(), generator, RandomSource.create(level.getSeed()
                        ^ pos.toLong()), new BoundingBox(pos.getMinBlockX(), level.getMinBuildHeight(),
                        pos.getMinBlockZ(), pos.getMaxBlockX(), level.getMaxBuildHeight() - 1, pos.getMaxBlockZ()),
                        pos);
                chunk.setUnsaved(true);
            }
        }
        level.onStructureStartsAvailable(first);
        Settlements.look(level, first);
        return Optional.of(start);
    }
}
