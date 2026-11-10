/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.abandoned;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.worldgen.structure.ITemplateChooser;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;

/**
 * Picks an abandoned building's template as it is placed (computers/docs/design/survival.md, "Abandoned buildings"):
 * of the era its distance from the world's spawn gives it, the nearest one it may be of, and working as often as the
 * server says, a ruin otherwise.
 */
final class AbandonedChooser implements ITemplateChooser {

    @Override
    public Choice choose(final WorldGenLevel level, final BlockPos at, final RandomSource random,
                         final List<ResourceLocation> templates) {
        final AbandonedBuilding building = AbandonedBuildings.ofTemplate(templates.getFirst());
        // Counted from the world's spawn, where a player starts: the Vintage near it, the Advanced far away.
        final BlockPos spawn = level.getLevelData().getSpawnPos();
        final double dx = at.getX() - spawn.getX();
        final double dz = at.getZ() - spawn.getZ();
        final double distance = Math.sqrt(dx * dx + dz * dz);
        final Era era = nearest(AbandonedSettings.eras(building), AbandonedSettings.eraAt(distance));
        final boolean working = random.nextDouble() < AbandonedSettings.workingChance();
        return new Choice(AbandonedBuildings.template(building, era, working),
                List.of(new AbandonedMachines(building, era, working)));
    }

    /* Of {@code eras}, the one nearest {@code wanted}, the earlier of two as near. */
    private static Era nearest(final List<Era> eras, final Era wanted) {
        final int at = HardwareEra.WITH_PARTS.indexOf(wanted);
        Era best = eras.getFirst();
        for (final Era era : eras) {
            if (Math.abs(HardwareEra.WITH_PARTS.indexOf(era) - at) < Math.abs(HardwareEra.WITH_PARTS.indexOf(best)
                    - at)) {
                best = era;
            }
        }
        return best;
    }
}
