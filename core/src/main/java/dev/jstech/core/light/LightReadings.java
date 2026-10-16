/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import dev.jstech.core.signal.Reading;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Light as a sensor reads it and speaks it in redstone: how bright a place is, the game's own levels from 0 to 15,
 * whatever its colour, and the hue of the light there, from the colours of the lamps and the blocks that light it.
 *
 * <pre>{@code
 * public List<Reading> readings() {   // a light sensor
 *     return LightReadings.of(level, worldPosition.above());
 * }
 * }</pre>
 */
@TextHolder
public final class LightReadings {

    public static final TextKey LIGHT = TextKey.of("jscore.light.reading.light", "Light");
    public static final TextKey HUE = TextKey.of("jscore.light.reading.hue", "Hue");
    public static final TextKey DEGREES = TextKey.of("jscore.light.reading.degrees", "°");

    private static final double FULL_TURN = 360.0;
    private static final int REACH = LightMath.BRIGHTEST;

    private LightReadings() {
    }

    /** What a light sensor at {@code pos} reads: its light, and its hue. */
    public static List<Reading> of(final Level level, final BlockPos pos) {
        return List.of(intensity(level, pos), hue(level, pos));
    }

    /** How bright {@code pos} is, block light or sky light, the brighter, from 0 to 15. */
    public static Reading intensity(final Level level, final BlockPos pos) {
        return new Reading(LIGHT, level.getMaxLocalRawBrightness(pos), 0, LightMath.BRIGHTEST, null);
    }

    /** The hue of the light at {@code pos}, from 0 (red) round to 360; at 0 where the light there is white. */
    public static Reading hue(final Level level, final BlockPos pos) {
        final double hue = LightMath.hue(colourAt(level, pos));
        return new Reading(HUE, Math.max(0, hue), 0, FULL_TURN, DEGREES);
    }

    /** The colour of the light at {@code pos}: what the lamps, blocks and beams around it bring, mixed; else white. */
    public static int colourAt(final Level level, final BlockPos pos) {
        final List<LightMath.Arrival> arriving = new ArrayList<>();
        final int sectionX = SectionPos.blockToSectionCoord(pos.getX());
        final int sectionY = SectionPos.blockToSectionCoord(pos.getY());
        final int sectionZ = SectionPos.blockToSectionCoord(pos.getZ());
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int cx = sectionX - 1; cx <= sectionX + 1; cx++) {
            for (int cz = sectionZ - 1; cz <= sectionZ + 1; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                final LevelChunk chunk = level.getChunk(cx, cz);
                for (final BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof LampBlockEntity lamp && lamp.lit()) {
                        add(arriving, pos, entity.getBlockPos(), lamp.shining(), lamp.kind().colour());
                    }
                }
                for (int cy = sectionY - 1; cy <= sectionY + 1; cy++) {
                    final int index = chunk.getSectionIndexFromSectionY(cy);
                    if (index < 0 || index >= chunk.getSectionsCount()) {
                        continue;
                    }
                    blocks(chunk.getSection(index), cx, cy, cz, pos, at, arriving);
                }
            }
        }
        if (level instanceof ServerLevel server) {
            final Optional<BeamLights.Lit> beam = BeamLights.at(server, pos);
            beam.ifPresent(lit -> arriving.add(new LightMath.Arrival(lit.level(), lit.colour())));
        }
        // Blended with white by its share of the light, the mix keeps its hue.
        return LightMath.tint(arriving, level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(
                LightLayer.SKY, pos));
    }

    /* The blocks of a section that give coloured light, as they arrive at the place. */
    private static void blocks(final LevelChunkSection section, final int cx, final int cy, final int cz,
                               final BlockPos pos, final BlockPos.MutableBlockPos at,
                               final List<LightMath.Arrival> arriving) {
        if (section.hasOnlyAir() || !section.maybeHas(state -> state.getLightEmission() > 0
                && LightColours.of(state) != LightMath.WHITE)) {
            return;
        }
        for (int y = 0; y < SectionPos.SECTION_SIZE; y++) {
            for (int z = 0; z < SectionPos.SECTION_SIZE; z++) {
                for (int x = 0; x < SectionPos.SECTION_SIZE; x++) {
                    final BlockState state = section.getBlockState(x, y, z);
                    if (state.getLightEmission() > 0 && LightColours.of(state) != LightMath.WHITE) {
                        at.set(SectionPos.sectionToBlockCoord(cx) + x, SectionPos.sectionToBlockCoord(cy) + y,
                                SectionPos.sectionToBlockCoord(cz) + z);
                        add(arriving, pos, at, state.getLightEmission(), LightColours.of(state));
                    }
                }
            }
        }
    }

    private static void add(final List<LightMath.Arrival> arriving, final BlockPos pos, final BlockPos source,
                            final int level, final int colour) {
        final int steps = pos.distManhattan(source);
        if (steps <= REACH) {
            arriving.add(new LightMath.Arrival(LightMath.arriving(level, steps), colour));
        }
    }
}
