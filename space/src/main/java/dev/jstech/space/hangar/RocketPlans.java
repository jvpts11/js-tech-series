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
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.launcher.LauncherLayout;
import dev.jstech.space.rocket.RocketLayout;
import dev.jstech.space.rocket.RocketStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The plan a Rocket Hangar builds a rocket by, from its design: every block of every part's kit where it goes, laid
 * out from the middle of the rocket's foot, each part of the stack a part of the plan, each block merging as its cell
 * of its model, turned as its part was set; and where a part's model reaches into the blocks of the parts beside it,
 * as a separation ring's skirt rises round the engine on it, those blocks take its shape too once it is formed. Where
 * the bay has room for one, the rocket stands on a mobile launcher laid in the same plan under it.
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

    /**
     * The plan of {@code stack} standing on a mobile launcher whose tower is on {@code side}, the launcher's parts
     * after the rocket's (its deck, its tower, then each arm); the rocket alone where {@code side} is null.
     */
    public static BuildPlan of(final Level level, final RocketStack stack, final @Nullable Direction side) {
        final BuildPlan rocket = of(level, stack);
        if (side == null) {
            return rocket;
        }
        final LauncherLayout.Laid launcher = launcher(level, stack, rocket, side);
        final Set<BlockPos> taken = new HashSet<>();
        rocket.spots().forEach(spot -> taken.add(spot.at()));
        final List<PlanSpot> spots = new ArrayList<>(rocket.spots());
        for (final PlanSpot spot : launcher.spots()) {
            // Nothing of the launcher goes where the rocket stands.
            if (taken.add(spot.at())) {
                spots.add(spot);
            }
        }
        return new BuildPlan(rocket.parts() + launcher.pieces().size(), spots);
    }

    /** What each of the launcher's parts is in the plan of {@code stack}, its tower on {@code side}. */
    public static List<LauncherLayout.Piece> launcherPieces(final Level level, final RocketStack stack,
                                                           final Direction side) {
        return launcher(level, stack, of(level, stack), side).pieces();
    }

    /**
     * The side of a mobile launcher's tower for {@code stack} in {@code bay}: its deck's length pointing at the bay's
     * door, so the crawler comes in through the door and under it, the tower at the end away from the door; rocket
     * and launcher whole inside the bay and under its hoist, the rocket on the deck on its pedestals. None when the
     * bay has no door to come in by, or no room for a launcher: the rocket stands on its floor.
     */
    public static Optional<Direction> launcherSide(final Level level, final RocketStack stack, final HangarBay bay) {
        final BuildPlan rocket = of(level, stack);
        final Optional<Direction> door = doorSide(level, bay);
        if (rocket.spots().isEmpty() || door.isEmpty()) {
            return Optional.empty();
        }
        final Direction side = door.get().getOpposite();
        final BlockPos origin = bay.pad().above(LauncherLayout.RAISE);
        final List<PlanSpot> all = new ArrayList<>(rocket.spots());
        all.addAll(launcher(level, stack, rocket, side).spots());
        return all.stream().allMatch(spot -> fits(bay, origin.offset(spot.at()))) ? Optional.of(side)
                : Optional.empty();
    }

    /*
     * The wall of {@code bay} its door stands in, as seen from the middle of its floor: of the walls with a Bay Door
     * on the floor, the one whose door is nearest the line out from the middle.
     */
    private static Optional<Direction> doorSide(final Level level, final HangarBay bay) {
        final BlockPos pad = bay.pad();
        Direction best = null;
        int nearest = Integer.MAX_VALUE;
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (final Direction wall : Direction.Plane.HORIZONTAL) {
            final boolean alongX = wall.getAxis() == Direction.Axis.Z;
            final int from = alongX ? bay.low().getX() : bay.low().getZ();
            final int to = alongX ? bay.high().getX() : bay.high().getZ();
            final int fixed = wall == Direction.NORTH ? bay.low().getZ() - 1 : wall == Direction.SOUTH
                    ? bay.high().getZ() + 1 : wall == Direction.WEST ? bay.low().getX() - 1 : bay.high().getX() + 1;
            for (int k = from; k <= to; k++) {
                at.set(alongX ? k : fixed, bay.low().getY(), alongX ? fixed : k);
                final int off = Math.abs(k - (alongX ? pad.getX() : pad.getZ()));
                if (level.getBlockState(at).is(HangarBlocks.BAY_DOOR.get()) && off < nearest) {
                    nearest = off;
                    best = wall;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /* The launcher under the rocket {@code rocket} of {@code stack}, its tower on {@code side}. */
    private static LauncherLayout.Laid launcher(final Level level, final RocketStack stack, final BuildPlan rocket,
                                                final Direction side) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(level);
        final List<RocketStack.Section> sections = stack.sections();
        return LauncherLayout.of(rocket, part -> part < sections.size() && Optional.ofNullable(parts.get(
                sections.get(part).part().id())).map(found -> found.kind() == RocketPart.Kind.TANK).orElse(false),
                side, rocket.parts());
    }

    /* Whether {@code at} is inside {@code bay} and under its hoist. */
    private static boolean fits(final HangarBay bay, final BlockPos at) {
        return bay.holds(at) && at.getY() < bay.hoist().getY();
    }

    /* Where the cell {@code cell} of {@code model}, outside its own, is from the middle of the rocket's foot. */
    private static BlockPos beside(final RocketLayout.Laid model, final long cell) {
        return model.at(MeshCells.x(cell), MeshCells.y(cell), MeshCells.z(cell));
    }
}
