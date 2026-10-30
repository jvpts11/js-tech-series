/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launcher;

import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.space.rocket.RocketKitBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Where the blocks of a mobile launcher go under a rocket, as the approved launcher stands, sized by the rocket: its
 * deck three blocks thick, the rocket's foot on its top; the exhaust opening through it, as wide as the rocket's lowest
 * layer and a block more each side, lined with exhaust frame; a hold-down arm at each corner of the opening and a tail
 * service mast at the middle of each side but the tower's; the umbilical tower beside the rocket, five blocks square
 * and four blocks out from its widest point, a block taller than it, its crane's boom out over its nose, its elevator
 * up its shaft to the crew access arm, a doorway into the shaft at the deck and at that arm; a swing arm
 * out to each tank and each Umbilical Plate, and the crew access arm to the capsule's hatch.
 *
 * <p>Laid out from the middle of the rocket's foot, the tower on the side {@code side}; the deck is the launcher's
 * first part, the tower its second, then each arm.
 */
public final class LauncherLayout {

    /** How thick the deck is, so how far above the launcher's foot the rocket stands. */
    public static final int DECK = 3;
    /* How far the deck reaches past the opening's lining, and the tower's side square and the gap before it. */
    private static final int MARGIN = 4;
    private static final int TOWER = 5;
    private static final int TOWER_GAP = 4;
    /* How far past the rocket's axis the crane's boom reaches. */
    private static final int BOOM_PAST = 3;

    private LauncherLayout() {
    }

    /**
     * The launcher under {@code rocket}, its tower on {@code side}, with {@code tank} saying which of the rocket's
     * parts are tanks; its parts numbered from {@code firstPart}.
     */
    public static Laid of(final BuildPlan rocket, final IntPredicate tank, final Direction side, final int firstPart) {
        final Frame frame = new Frame(side);
        final List<PlanSpot> spots = new ArrayList<>();
        final List<Piece> pieces = new ArrayList<>();
        int lowest = Integer.MAX_VALUE;
        int top = 0;
        for (final PlanSpot spot : rocket.spots()) {
            lowest = Math.min(lowest, spot.at().getY());
            top = Math.max(top, spot.at().getY());
        }
        int base = 0;
        int widest = 0;
        for (final PlanSpot spot : rocket.spots()) {
            if (spot.at().getY() == lowest) {
                base = Math.max(base, Math.max(Math.abs(spot.at().getX()), Math.abs(spot.at().getZ())));
            }
            widest = Math.max(widest, frame.u(spot.at()));
        }
        final int open = base + 1;
        final int near = widest + TOWER_GAP;
        final int far = near + TOWER - 1;
        // The elevator runs up the shaft just inside its doorways, so the crew steps off it onto the arm.
        final int lane = near + 1;
        final int reach = open + 1 + MARGIN - 1;
        final int crew = hatch(rocket);
        // The deck, the opening's lining, the hold-down arms and the masts: the launcher's first part.
        final int deck = firstPart;
        pieces.add(Piece.LAUNCHER);
        for (int y = -DECK; y < 0; y++) {
            for (int u = -reach; u <= far + 1; u++) {
                for (int v = -reach; v <= reach; v++) {
                    final int ring = Math.max(Math.abs(u), Math.abs(v));
                    if (ring <= open) {
                        continue;
                    }
                    spots.add(spot(frame.at(u, y, v), ring == open + 1 ? LauncherBlocks.EXHAUST_FRAME.getId()
                            : LauncherBlocks.LAUNCHER_DECK.getId(), deck));
                }
            }
        }
        for (final int u : new int[] {-open - 1, open + 1}) {
            for (final int v : new int[] {-open - 1, open + 1}) {
                spots.add(spot(frame.at(u, 0, v), LauncherBlocks.HOLD_DOWN_ARM.getId(), deck));
            }
        }
        for (final int[] mast : new int[][] {{-open - 1, 0}, {0, open + 1}, {0, -open - 1}}) {
            for (int y = 0; y < 2; y++) {
                spots.add(spot(frame.at(mast[0], y, mast[1]), LauncherBlocks.TAIL_SERVICE_MAST.getId(), deck));
            }
        }
        // The tower, its elevator and its crane's boom: the second part.
        final int tower = firstPart + 1;
        pieces.add(Piece.TOWER);
        // A block taller than the rocket, and two over the crew's doorway, so the crane house stands on the tower.
        final int towerTop = Math.max(top + 1, crew + 3);
        final int lift = crew >= 0 ? crew : towerTop - 1;
        for (int y = 0; y <= towerTop; y++) {
            for (int u = near; u <= far; u++) {
                for (int v = -TOWER / 2; v <= TOWER / 2; v++) {
                    final boolean wall = u == near || u == far || Math.abs(v) == TOWER / 2;
                    final boolean door = u == near && v == 0 && (y <= 1 || crew >= 0 && (y == crew || y == crew + 1));
                    if (wall && !door) {
                        spots.add(spot(frame.at(u, y, v), LauncherBlocks.TOWER_FRAME.getId(), tower));
                    } else if (u == lane && v == 0 && y <= lift) {
                        spots.add(spot(frame.at(u, y, v), LauncherBlocks.TOWER_ELEVATOR.getId(), tower));
                    }
                }
            }
        }
        for (int u = -BOOM_PAST; u < near; u++) {
            spots.add(spot(frame.at(u, towerTop, 0), LauncherBlocks.TOWER_FRAME.getId(), tower));
        }
        // An arm out to each tank and each Umbilical Plate, and the crew access arm to the hatch: a part each.
        final TreeSet<Integer> heights = new TreeSet<>();
        final List<List<PlanSpot>> byPart = rocket.byPart().stream().map(indices -> indices.stream()
                .map(rocket.spots()::get).toList()).toList();
        for (int part = 0; part < byPart.size(); part++) {
            if (tank.test(part) && !byPart.get(part).isEmpty()) {
                final int low = byPart.get(part).stream().mapToInt(spot -> spot.at().getY()).min().orElse(0);
                final int high = byPart.get(part).stream().mapToInt(spot -> spot.at().getY()).max().orElse(0);
                heights.add((low + high) / 2);
            }
        }
        for (final PlanSpot spot : rocket.spots()) {
            if (spot.block().equals(RocketKitBlocks.UMBILICAL_PLATE.getId())) {
                heights.add(spot.at().getY());
            }
        }
        heights.removeIf(y -> y <= 1 || crew >= 0 && Math.abs(y - crew) <= 1);
        int next = firstPart + 2;
        for (final int y : heights) {
            if (arm(rocket, frame, y, near, LauncherBlocks.SWING_ARM.getId(), next, spots)) {
                pieces.add(Piece.SWING_ARM);
                next++;
            }
        }
        if (crew >= 0 && arm(rocket, frame, crew, near, LauncherBlocks.CREW_ACCESS_ARM.getId(), next, spots)) {
            pieces.add(Piece.CREW_ARM);
        }
        return new Laid(spots, pieces);
    }

    /**
     * The side the tower stands on for {@code rocket}: the side its capsule's hatch looks out of, so the crew access
     * arm meets it; else the side of its Umbilical Plate; else east.
     */
    public static Direction side(final BuildPlan rocket) {
        for (final ResourceLocation block : List.of(RocketKitBlocks.CAPSULE_HATCH.getId(),
                RocketKitBlocks.UMBILICAL_PLATE.getId())) {
            for (final PlanSpot spot : rocket.spots()) {
                if (spot.block().equals(block) && (spot.at().getX() != 0 || spot.at().getZ() != 0)) {
                    final int x = spot.at().getX();
                    final int z = spot.at().getZ();
                    return Math.abs(x) >= Math.abs(z) ? x > 0 ? Direction.EAST : Direction.WEST
                            : z > 0 ? Direction.SOUTH : Direction.NORTH;
                }
            }
        }
        return Direction.EAST;
    }

    /*
     * Lays an arm at height {@code y} from the tower's face to the rocket's side there, as part {@code part}; false
     * when there is no room for one.
     */
    private static boolean arm(final BuildPlan rocket, final Frame frame, final int y, final int near,
                               final ResourceLocation block, final int part, final List<PlanSpot> spots) {
        int side = Integer.MIN_VALUE;
        for (final PlanSpot spot : rocket.spots()) {
            if (spot.at().getY() == y && Math.abs(frame.v(spot.at())) <= 1) {
                side = Math.max(side, frame.u(spot.at()));
            }
        }
        if (side == Integer.MIN_VALUE || side + 1 > near - 1) {
            return false;
        }
        for (int u = side + 1; u < near; u++) {
            spots.add(spot(frame.at(u, y, 0), block, part));
        }
        return true;
    }

    /* The height of the capsule's hatch in {@code rocket}, or -1 when it has none. */
    private static int hatch(final BuildPlan rocket) {
        for (final PlanSpot spot : rocket.spots()) {
            if (spot.block().equals(RocketKitBlocks.CAPSULE_HATCH.getId())) {
                return spot.at().getY();
            }
        }
        return -1;
    }

    private static PlanSpot spot(final BlockPos at, final ResourceLocation block, final int part) {
        return new PlanSpot(at, block, part, Optional.empty(), List.of());
    }

    /** What each part of a launcher is, by its number from its first. */
    public enum Piece {
        LAUNCHER, TOWER, SWING_ARM, CREW_ARM
    }

    /**
     * A launcher laid out.
     *
     * @param spots  its blocks, from the middle of the rocket's foot
     * @param pieces what each of its parts is, in their order
     */
    public record Laid(List<PlanSpot> spots, List<Piece> pieces) {

        public Laid {
            spots = List.copyOf(spots);
            pieces = List.copyOf(pieces);
        }
    }

    /* The launcher's own axes: u out toward the tower, v across it, turned to the world's. */
    private record Frame(Direction side) {

        BlockPos at(final int u, final int y, final int v) {
            final Direction across = this.side.getClockWise();
            return new BlockPos(this.side.getStepX() * u + across.getStepX() * v, y,
                    this.side.getStepZ() * u + across.getStepZ() * v);
        }

        int u(final BlockPos at) {
            return at.getX() * this.side.getStepX() + at.getZ() * this.side.getStepZ();
        }

        int v(final BlockPos at) {
            final Direction across = this.side.getClockWise();
            return at.getX() * across.getStepX() + at.getZ() * across.getStepZ();
        }
    }
}
