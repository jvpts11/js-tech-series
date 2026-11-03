/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import com.mojang.logging.LogUtils;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.multiblock.shape.CellSpace;
import dev.jstech.core.multiblock.shape.FormedDoors;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.IFormedPlaced;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipPartKind;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.crew.ShipDoorRules;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/**
 * A player walks a small ship from its cockpit to its tail, through every part a crew goes into: from the middle of the
 * section behind the cockpit, the places a player stands on the floor reach, a step at a time and stepping up no higher
 * than a player does walking, into the cockpit's doorway, its floor flush with the threshold and its pilot's seat in
 * reach (the flight deck behind is the seat's and the consoles'), and along every row of every section, on one level.
 * A reactor's room is one a crew looks into rather than walks through, its fusion drum standing in its middle: the
 * sections behind it are walked from their own middle. Five ships between them hold every cockpit and every part of the
 * spine.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipWalkGameTests {

    private static final String ARENA = "shipyard";
    private static final String BATCH = "jsspace_ship_walk";
    private static final BlockPos CORNER = new BlockPos(1, 2, 1);
    private static final int WAIT = 600;
    /* A standing player: half their width and their height; how far up they step walking. */
    private static final double HALF_WIDTH = 0.3;
    private static final double HEIGHT = 1.8;
    private static final double STEP_UP = 0.6;
    /* The places a player is tried at, a sixteenth apart, as far out as the hull's inside. */
    private static final double GRID = 1.0 / 16;
    private static final double WIDE = 1.875;
    /* Where a floor is looked for, up the ship from its hull's foot, and a hair over a floor. */
    private static final double FLOOR_LOW = 0.0;
    private static final double FLOOR_HIGH = 1.3;
    private static final double OVER = 1.0e-3;
    /* A player in a doorway stands no further from its bulkhead than half their width; their eyes this high. */
    private static final double EYES = 1.62;
    /* How far a player reaches to use a block. */
    private static final double REACH = 4.5;
    /* How far a floor along the way lies from the first section's, at most: a sixteenth, as shapes are made. */
    private static final double LEVEL = 1.0 / 16 + 1.0e-6;
    /* How far into the walkway's middle a floor is held to the first's level. */
    private static final double MIDDLE = 0.5;
    /* The section of the spine whose pressure door is shut: the workshop, behind the airlock. */
    private static final int SHUT_SECTION = 2;
    private static final Logger LOGGER = LogUtils.getLogger();

    private ShipWalkGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void walk_aShipOfThreeModulesFromItsCockpitToItsTail(final GameTestHelper helper) {
        walk(helper, new ShipDesign("JS-W1", List.of(part("orbiter_cockpit"), part("airlock"), part("workshop"),
                part("lab"), part("quarters"), part("reactor_flush")), List.of(part("twin_bells"),
                part("blended_delta"), part("twin_canted_fins"), part("wheels"))));
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void walk_aShipOfTheOtherModulesFromItsCockpitToItsTail(final GameTestHelper helper) {
        walk(helper, new ShipDesign("JS-W2", List.of(part("cab_cockpit"), part("docking"), part("corridor"),
                part("computing"), part("small_cargo"), part("reactor_fins")), List.of(part("cluster"),
                part("stub_wings"), part("single_fin"), part("legs"))));
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void walk_aShipWithASideDoorHoldFromItsCockpitToItsTail(final GameTestHelper helper) {
        walk(helper, new ShipDesign("JS-W3", List.of(part("greenhouse_cockpit"), part("airlock"), part("side_door"),
                part("reactor_flush")), List.of(part("twin_bells"), part("swept_wings"), part("wheels"))));
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void walk_aShipWithABayFromItsCockpitToItsTail(final GameTestHelper helper) {
        walk(helper, new ShipDesign("JS-W4", List.of(part("cab_cockpit"), part("docking"), part("bay"),
                part("reactor_fins")), List.of(part("cluster"), part("stub_wings"), part("legs"))));
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void walk_aShipWithARampFromItsCockpitToItsTail(final GameTestHelper helper) {
        walk(helper, new ShipDesign("JS-W5", List.of(part("orbiter_cockpit"), part("airlock"), part("reactor_flush"),
                part("ramp")), List.of(part("magnetic_nozzle"), part("stub_wings"), part("legs"))));
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = WAIT)
    public static void walk_stopsAtAShutPressureDoor(final GameTestHelper helper) {
        final ShipDesign design = new ShipDesign("JS-W6", List.of(part("orbiter_cockpit"), part("airlock"),
                part("workshop"), part("reactor_flush")), List.of(part("twin_bells"), part("blended_delta"),
                part("wheels")));
        walk(helper, design, (level, plan) -> {
            final BlockPos door = plan.where(plan.plan().spots().stream().filter(spot -> spot.part() == SHUT_SECTION
                    && spot.block().equals(ShipKitBlocks.PRESSURE_DOOR.getId())).findFirst().orElseThrow());
            for (final FormedPlace place : ((IFormedPlaced) level.getBlockEntity(door)).places()) {
                FormedDoors.named(level, door, place, ShipDoorRules.PRESSURE_DOOR).ifPresent(shut ->
                        FormedDoors.set(level, shut, true));
            }
        }, wrong -> wrong.stream().anyMatch(line -> line.startsWith("small_workshop is not walked to")),
                "the workshop behind its shut door is not walked to");
    }

    /* The ship of {@code design} built whole in a dock, then walked from its cockpit to its tail. */
    private static void walk(final GameTestHelper helper, final ShipDesign design) {
        walk(helper, design, (level, plan) -> { }, List::isEmpty, "walked from its cockpit to its tail");
    }

    /*
     * The ship of {@code design} built whole in a dock, {@code before} done to it, then walked: what keeps a player
     * from walking it as {@code wanted} says, or the test fails saying {@code what}.
     */
    private static void walk(final GameTestHelper helper, final ShipDesign design,
                             final BiConsumer<ServerLevel, PlannedBuild> before, final Predicate<List<String>> wanted,
                             final String what) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final PlannedBuild[] plan = new PlannedBuild[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getBlockEntity(terminal)
                        instanceof ShipyardTerminalBlockEntity dock && dock.formed(), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    dock.design(design);
                    helper.assertTrue(dock.confirm(), "the ship " + design.name() + " is sent to the dock: "
                            + design.problems(ShipParts.all(level)));
                    plan[0] = dock.build().orElseThrow();
                    for (final PlanSpot spot : plan[0].plan().spots()) {
                        level.setBlockAndUpdate(plan[0].where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                                .defaultBlockState());
                    }
                })
                .thenWaitUntil(() -> helper.assertTrue(plan[0].complete(), "the ship stands whole and formed"))
                .thenExecute(() -> {
                    before.accept(level, plan[0]);
                    final List<String> wrong = new Walk(level, plan[0], design).wrong();
                    helper.assertTrue(wanted.test(wrong), design.name() + " " + what + ": "
                            + String.join("; ", wrong));
                })
                .thenSucceed();
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }

    /*
     * A ship walked: where a player stands on the grid over its inside, in the ship's own frame (across from its
     * middle line, along from its nose), what floor each place has, and which places a player walks to.
     */
    private static final class Walk {

        private final ServerLevel level;
        private final PlannedBuild plan;
        private final ShipDesign design;
        private final Map<ResourceLocation, ShipPart> parts;
        private final PlanSpot nose;
        private final double[] fronts;
        private final int across;
        private final int along;
        private final double[][] floors;
        private final boolean[][] reached;

        Walk(final ServerLevel level, final PlannedBuild plan, final ShipDesign design) {
            this.level = level;
            this.plan = plan;
            this.design = design;
            this.parts = ShipParts.all(level);
            this.nose = plan.plan().spots().stream().filter(spot -> spot.part() == 0 && spot.place().isPresent())
                    .findFirst().orElseThrow();
            this.fronts = new double[design.spine().size() + 1];
            for (int k = 0; k < design.spine().size(); k++) {
                this.fronts[k + 1] = this.fronts[k] + this.parts.get(design.spine().get(k)).length();
            }
            this.across = (int) Math.round(2 * WIDE / GRID) + 1;
            this.along = (int) Math.floor(this.fronts[this.fronts.length - 1] / GRID) + 1;
            this.floors = new double[this.across][this.along];
            this.reached = new boolean[this.across][this.along];
            for (int i = 0; i < this.across; i++) {
                for (int j = 0; j < this.along; j++) {
                    this.floors[i][j] = standing(x(i), z(j));
                }
            }
        }

        /* What keeps a player from walking the ship; nothing when they walk it. */
        List<String> wrong() {
            final List<String> out = new ArrayList<>();
            // The walk starts in the middle of the section behind the cockpit, and of each behind a reactor.
            final List<Integer> seeds = new ArrayList<>();
            seeds.add(1);
            for (int k = 1; k + 1 < this.design.spine().size(); k++) {
                if (reactor(k)) {
                    seeds.add(k + 1);
                }
            }
            Double ground = null;
            for (final int seed : seeds) {
                final int i = column(0);
                final int j = row(middle(seed));
                if (Double.isNaN(this.floors[i][j])) {
                    out.add(name(seed) + " has no room to stand in its middle: " + blockers(0, middle(seed)));
                    continue;
                }
                ground = ground == null ? this.floors[i][j] : ground;
                reach(i, j);
            }
            if (!inCockpit()) {
                out.add(name(0) + " is not walked into its doorway, its pilot's seat in reach: "
                        + blockers(0, this.fronts[1]));
            }
            for (int k = 1; k < this.design.spine().size(); k++) {
                if (reactor(k)) {
                    continue;
                }
                for (double r = this.fronts[k]; r < this.fronts[k + 1] - 1.0e-6; r += 1) {
                    if (!rowReached(r, r + 1)) {
                        out.add(name(k) + " is not walked to between " + (r - this.fronts[k]) + " and "
                                + (r + 1 - this.fronts[k]) + " along it: " + blockers(0, r + 1.0 / 2));
                    }
                }
                if (ground != null) {
                    final Set<Double> off = offLevel(k, ground);
                    if (!off.isEmpty()) {
                        out.add(name(k) + " has its floor off the first section's " + ground + " at " + off);
                    }
                }
            }
            if (!out.isEmpty()) {
                LOGGER.info("{}, walked (o) and stood on (.):\n{}", this.design.name(), map());
            }
            return out;
        }

        /* Reaches every place a player walks to from the place (i, j). */
        private void reach(final int i0, final int j0) {
            final Deque<int[]> next = new ArrayDeque<>();
            this.reached[i0][j0] = true;
            next.add(new int[] {i0, j0});
            final int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            while (!next.isEmpty()) {
                final int[] at = next.poll();
                for (final int[] step : steps) {
                    final int i = at[0] + step[0];
                    final int j = at[1] + step[1];
                    if (i < 0 || j < 0 || i >= this.across || j >= this.along || this.reached[i][j]
                            || Double.isNaN(this.floors[i][j])
                            || this.floors[i][j] - this.floors[at[0]][at[1]] > STEP_UP) {
                        continue;
                    }
                    this.reached[i][j] = true;
                    next.add(new int[] {i, j});
                }
            }
        }

        /* Whether a player walks into the cockpit's doorway, the pilot's seat in reach of their hand. */
        private boolean inCockpit() {
            final Vec3 seat = Vec3.atCenterOf(this.plan.where(this.plan.plan().spots().stream()
                    .filter(spot -> spot.block().equals(ShipKitBlocks.PILOT_SEAT.getId())).findFirst().orElseThrow()));
            for (int i = 0; i < this.across; i++) {
                for (int j = 0; j <= row(this.fronts[1] + HALF_WIDTH); j++) {
                    if (this.reached[i][j]
                            && world(x(i), this.floors[i][j] + EYES, z(j)).distanceTo(seat) <= REACH) {
                        return true;
                    }
                }
            }
            return false;
        }

        /* Whether a player walks to some place between {@code from} and {@code to} along the ship. */
        private boolean rowReached(final double from, final double to) {
            for (int j = row(from); j <= row(to) && j < this.along; j++) {
                if (z(j) < from || z(j) > to) {
                    continue;
                }
                for (int i = 0; i < this.across; i++) {
                    if (this.reached[i][j]) {
                        return true;
                    }
                }
            }
            return false;
        }

        /* The floors of the places walked to in the walkway's middle of section {@code k} off {@code level}. */
        private Set<Double> offLevel(final int k, final double level) {
            final Set<Double> out = new LinkedHashSet<>();
            for (int i = column(-MIDDLE); i <= column(MIDDLE); i++) {
                for (int j = row(this.fronts[k]); j <= row(this.fronts[k + 1]) && j < this.along; j++) {
                    if (this.reached[i][j] && Math.abs(this.floors[i][j] - level) > LEVEL) {
                        out.add(this.floors[i][j]);
                    }
                }
            }
            return out;
        }

        /*
         * The floor's height in the ship's frame under a player standing at ({@code x}, {@code z}), where there is
         * room to stand on it; NaN where there is none.
         */
        private double standing(final double x, final double z) {
            final Vec3 foot = world(x, FLOOR_LOW, z);
            final Vec3 top = world(x, FLOOR_HIGH, z);
            final double low = Math.min(foot.y, top.y);
            final double high = Math.max(foot.y, top.y);
            final AABB column = new AABB(foot.x - HALF_WIDTH, low, foot.z - HALF_WIDTH, foot.x + HALF_WIDTH, high,
                    foot.z + HALF_WIDTH);
            Double floor = null;
            for (final VoxelShape shape : this.level.getBlockCollisions(null, column)) {
                for (final AABB box : shape.toAabbs()) {
                    if (box.intersects(column) && box.maxY <= high) {
                        floor = floor == null ? box.maxY : Math.max(floor, box.maxY);
                    }
                }
            }
            if (floor == null || !this.level.noCollision(body(foot, floor))) {
                return Double.NaN;
            }
            return floor - low;
        }

        /* The blocks in the way of a player standing at ({@code x}, {@code z}), by their ids and cells. */
        private String blockers(final double x, final double z) {
            final Vec3 foot = world(x, FLOOR_LOW, z);
            final int i = column(x);
            final int j = row(z);
            final double floor = Double.isNaN(this.floors[i][j]) ? 0.375 : this.floors[i][j];
            final AABB body = body(foot, foot.y + floor);
            final List<String> out = new ArrayList<>();
            BlockPos.betweenClosedStream(body).forEach(pos -> {
                final VoxelShape shape = this.level.getBlockState(pos).getCollisionShape(this.level, pos);
                for (final AABB box : shape.toAabbs()) {
                    if (box.move(pos).intersects(body)) {
                        out.add(BuiltInRegistries.BLOCK.getKey(this.level.getBlockState(pos).getBlock()).getPath()
                                + this.plan.spotAt(pos).flatMap(PlanSpot::place)
                                .map(place -> "(" + place.x() + "," + place.y() + "," + place.z() + ")").orElse("")
                                + " from " + String.format("%.2f", box.minY + pos.getY() - foot.y));
                        return;
                    }
                }
            });
            return out.isEmpty() ? "no floor" : String.join(", ", out);
        }

        /* A standing player's box over {@code floor}, at {@code foot}. */
        private static AABB body(final Vec3 foot, final double floor) {
            return new AABB(foot.x - HALF_WIDTH, floor + OVER, foot.z - HALF_WIDTH, foot.x + HALF_WIDTH,
                    floor + HEIGHT, foot.z + HALF_WIDTH);
        }

        /* The grid seen from above, a character a place: walked to, stood on, or neither. */
        private String map() {
            final StringBuilder out = new StringBuilder();
            for (int j = 0; j < this.along; j += 2) {
                out.append(String.format("%6.2f ", z(j)));
                for (int i = 0; i < this.across; i += 2) {
                    out.append(this.reached[i][j] ? 'o' : Double.isNaN(this.floors[i][j]) ? '#' : '.');
                }
                out.append('\n');
            }
            return out.toString();
        }

        private boolean reactor(final int k) {
            return this.parts.get(this.design.spine().get(k)).kind() == ShipPartKind.REACTOR;
        }

        private String name(final int k) {
            return this.design.spine().get(k).getPath();
        }

        private double middle(final int k) {
            return (this.fronts[k] + this.fronts[k + 1]) / 2;
        }

        private double x(final int i) {
            return -WIDE + i * GRID;
        }

        private double z(final int j) {
            return j * GRID;
        }

        private int column(final double x) {
            return (int) Math.round((x + WIDE) / GRID);
        }

        private int row(final double z) {
            return (int) Math.round(z / GRID);
        }

        /* Where the point ({@code x}, {@code y}, {@code z}) of the ship's own frame is in the world. */
        private Vec3 world(final double x, final double y, final double z) {
            final FormedPlace place = this.nose.place().orElseThrow();
            final double[] turned = new double[3];
            CellSpace.toWorld(new double[] {x - place.x(), y - place.y(), z - place.z()}, place.turn(), turned);
            final BlockPos block = this.plan.where(this.nose);
            return new Vec3(block.getX() + turned[0], block.getY() + turned[1], block.getZ() + turned[2]);
        }
    }
}
