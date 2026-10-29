/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.multiblock.shape.MeshCells;
import dev.jstech.space.api.RocketKits;
import dev.jstech.space.api.RocketParts;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * The shapes of the rocket parts' formed models, each worked out by the data run in the stacks it stands in: a part's
 * model may reach into the blocks of the parts beside it, as a separation ring's interstage skirt rises round the
 * vacuum engine on it, the instrument ring's adapter closes round the lunar lander, and the landing legs lie folded
 * along the tank over them, so each model is declared laid on its own blocks, free to reach into the blocks of the
 * parts round it in a stack where it stands. The data run stops when a block of a part has no face of its model, or
 * the model reaches out of the stack's blocks; but for the corners of a few round parts' layers, which their kits fill
 * and their hulls do not reach, merging with no shape. The models of legs spread to land are not built, so not shaped.
 */
public final class RocketShapes {

    /*
     * The layers whose corner blocks lie outside a round part's hull, so have none of its faces and merge with no
     * shape: the small capsule's nose, the large capsule's cabin, the lander's octagonal descent stage under its foil.
     */
    private static final Map<String, List<Integer>> HOLLOW_CORNERS = Map.of("rocket/capsule_3", List.of(2),
            "rocket/capsule_5", List.of(1), "rocket/lunar_lander_descent_stowed", List.of(1, 2));

    private RocketShapes() {
    }

    /** Declares the models laid, once, from the mod's constructor, after the parts, their kits and their models. */
    public static void declare() {
        final Map<ResourceLocation, Set<Long>> own = new LinkedHashMap<>();
        final Map<ResourceLocation, Set<Long>> reach = new LinkedHashMap<>();
        final Map<ResourceLocation, double[]> origins = new LinkedHashMap<>();
        for (final RocketStack stack : stacks()) {
            final List<RocketLayout.Laid> laid = RocketLayout.of(RocketParts.declared(), RocketKits.declared(),
                    stack);
            for (final RocketLayout.Laid model : laid) {
                final ResourceLocation id = model.placed().model();
                own.computeIfAbsent(id, any -> new HashSet<>()).addAll(model.keys());
                origins.put(id, model.origin());
                final Set<Long> beside = reach.computeIfAbsent(id, any -> new HashSet<>());
                for (final RocketLayout.Laid other : laid) {
                    if (other == model) {
                        continue;
                    }
                    for (final RocketLayout.Cell cell : other.cells()) {
                        final BlockPos at = other.at(cell.x(), cell.layer(), cell.z());
                        beside.add(model.cellOf(at));
                    }
                }
            }
        }
        own.forEach((id, cells) -> {
            final Set<Long> others = new HashSet<>(reach.get(id));
            others.removeAll(cells);
            final List<Integer> layers = HOLLOW_CORNERS.getOrDefault(id.getPath(), List.of());
            final int last = (int) Math.round(-2 * origins.get(id)[0]) - 1;
            final Set<Long> hollow = new HashSet<>();
            for (final long cell : cells) {
                final boolean corner = (MeshCells.x(cell) == 0 || MeshCells.x(cell) == last)
                        && (MeshCells.z(cell) == 0 || MeshCells.z(cell) == last);
                if (corner && layers.contains(MeshCells.y(cell))) {
                    hollow.add(cell);
                }
            }
            FormedShapes.declareLaid(id, origins.get(id), cells, others, hollow);
        });
    }

    /*
     * Stacks every built model stands in: a small and a large rocket, each with its interstage skirt and with a plain
     * ring, its legs folded along its tank, the large one with its lunar lander on its adapter.
     */
    private static List<RocketStack> stacks() {
        return List.of(
                RocketStack.of(part("sea_level_engine_3", 1), part("tank_3", 3), part("separation_ring_3", 1),
                        part("vacuum_engine_3", 1), part("tank_3", 1), part("instrument_ring_3", 1),
                        part("capsule_3", 1)),
                RocketStack.of(part("sea_level_engine_3", 1), part("landing_legs_3", 1), part("tank_3", 4),
                        part("separation_ring_3", 1), part("tank_3", 1), part("capsule_3", 1)),
                RocketStack.of(part("sea_level_engine_5", 1), part("tank_5", 3), part("separation_ring_5", 1),
                        part("vacuum_engine_5", 1), part("tank_5", 3), part("instrument_ring_5", 1),
                        part("lunar_lander_descent", 1), part("lunar_lander_ascent", 1)),
                RocketStack.of(part("sea_level_engine_5", 1), part("landing_legs_5", 1), part("tank_5", 4),
                        part("separation_ring_5", 1), part("tank_5", 1), part("instrument_ring_5", 1),
                        part("capsule_5", 1)));
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }
}
