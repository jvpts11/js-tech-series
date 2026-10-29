/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanReach;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.multiblock.shape.MeshCells;
import dev.jstech.space.api.RocketKits;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.rocket.RocketLayout;
import dev.jstech.space.rocket.RocketStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The plan a Rocket Hangar builds a rocket by, from its design: every block of every part's kit where it goes, laid
 * out from the middle of the rocket's foot, each part of the stack a part of the plan, each block merging as its cell
 * of its model, turned as its part was set; and where a part's model reaches into the blocks of the parts beside it,
 * as a separation ring's skirt rises round the engine on it, those blocks take its shape too once it is formed.
 */
public final class RocketPlans {

    private RocketPlans() {
    }

    /** The plan of {@code stack}, of the parts and kits {@code level} knows, its origin the middle of its foot. */
    public static BuildPlan of(final Level level, final RocketStack stack) {
        final List<RocketLayout.Laid> laid = RocketLayout.of(RocketParts.all(level), RocketKits.all(level), stack);
        final List<PlanSpot> spots = new ArrayList<>();
        final Map<BlockPos, Integer> byPlace = new HashMap<>();
        for (final RocketLayout.Laid model : laid) {
            for (final RocketLayout.Cell cell : model.cells()) {
                final BlockPos at = model.at(cell.x(), cell.layer(), cell.z());
                byPlace.put(at, spots.size());
                spots.add(new PlanSpot(at, cell.block(), model.placed().section(), Optional.of(model.place(
                        MeshCells.key(cell.x(), cell.layer(), cell.z()))), List.of()));
            }
        }
        // Where a model's faces reach into the blocks of another model, of its part or another, they are told of it.
        final Map<Integer, List<PlanReach>> reaches = new HashMap<>();
        for (final RocketLayout.Laid model : laid) {
            final Optional<FormedShape> shape = FormedShapes.get(level, model.placed().model());
            if (shape.isEmpty()) {
                continue;
            }
            final Set<Long> own = model.keys();
            for (final long cell : shape.get().cells().keySet()) {
                if (own.contains(cell)) {
                    continue;
                }
                final Integer index = byPlace.get(beside(model, cell));
                if (index != null) {
                    reaches.computeIfAbsent(index, any -> new ArrayList<>()).add(new PlanReach(
                            model.placed().section(), model.place(cell)));
                }
            }
        }
        reaches.forEach((index, reaching) -> {
            final PlanSpot spot = spots.get(index);
            spots.set(index, new PlanSpot(spot.at(), spot.block(), spot.part(), spot.place(), reaching));
        });
        return new BuildPlan(stack.sections().size(), spots);
    }

    /* Where the cell {@code cell} of {@code model}, outside its own, is from the middle of the rocket's foot. */
    private static BlockPos beside(final RocketLayout.Laid model, final long cell) {
        return model.at(MeshCells.x(cell), MeshCells.y(cell), MeshCells.z(cell));
    }
}
