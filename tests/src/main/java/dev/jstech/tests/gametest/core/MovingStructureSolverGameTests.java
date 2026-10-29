/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.dimension.Wind;
import dev.jstech.core.dimension.Winds;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructureEvent;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureBalance;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestPhysics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3d;

/**
 * How steady the solver of moving structures is: a stack of five structures dropped one on another settles, falls
 * asleep and stays still; a stack of twelve set down at rest stands, sleeps and hears no blow, and stands as well
 * waiting on a physics budget of none; a crate thrown onto a stack waiting on the budget pushes none of it while it
 * waits; stacks falling on themselves never sink into themselves as each lands, whichever of two meeting works their
 * meeting; a great hull let down on the ground comes to rest flat without trembling and sleeps on; a body flying at a
 * wall, fast or very fast, stops at it and never goes through; and a cart on its wheels, set going, rolls on where a
 * sled set going as fast slides to a stop. Each arena's air is still, so no wind moves what rests.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MovingStructureSolverGameTests {

    private static final String ARENA = "empty";
    /* An arena 32 high, for the stack. */
    private static final String TALL = "tall";
    /* An arena 48 across, for the hull, the wall and the cart. */
    private static final String WIDE = "bench";
    private static final String BATCH = "jscore_moving_solver";
    /* How close, in blocks, a resting structure is to where it should rest. */
    private static final double RESTS = 0.03;
    /*
     * How far aside of the one below, in blocks, a structure dropped on others may come to rest, three quarters of it
     * still on it: their blows, landing on what still moves, are never quite even.
     */
    private static final double DRIFTS = 0.25;
    /* How far, in blocks, what sleeps may be found from where it was a while before. */
    private static final double STILL = 0.001;
    /* The floor's top, in the arena's places. */
    private static final int FLOOR = 2;
    /* How many structures are stacked, and the gap each falls before it meets the one below. */
    private static final int STACKED = 5;
    /* How long what came to rest is watched staying still, in ticks: past several of its looks about. */
    private static final int WATCHED = 60;
    /* How many of the last ticks watched a failure tells. */
    private static final int LATELY = 3;
    /* How fast the top going sideways is told, in metres a second. */
    private static final double ASIDE = 0.5;
    /*
     * How many crates the tall stack set down at rest stands, and the fastest, in metres a second, any of it may fall
     * as it settles into its joints: well short of a blow's metre a second, which it once came within a hair of.
     */
    private static final int TALL_STACKED = 12;
    private static final double SETTLING = 0.75;
    /*
     * The swaying stack, in its batch of its own: how many ticks after it is set down it is set swaying, and how fast
     * each crate goes east and north for each crate under it, in metres a second, so its top goes about a tenth of a
     * metre a second, as the stack once seen swaying for good did.
     */
    private static final String SWAYING_BATCH = "jscore_moving_solver_swaying";
    private static final int SWAY_AFTER = 3;
    private static final double SWAY_EAST = 0.01;
    private static final double SWAY_NORTH = 0.008;
    /*
     * The landing test, in its batch of its own: four stacks of three, far apart, so that in some the one standing
     * still and not the one falling onto it works their meeting; watched while they fall and settle; how deep one may
     * sink into the one below, a contact's own few millimetres with room to spare.
     */
    private static final String LANDING_BATCH = "jscore_moving_solver_landing";
    private static final int LANDING_STACKS = 4;
    private static final int LANDING_STACKED = 3;
    private static final int LANDING_APART = 11;
    private static final int LANDING_WATCHED = 200;
    private static final double SUNK_AT_MOST = 0.02;
    /*
     * The waiting test, in its batch of its own, as it takes the dimension's physics budget away: how long the stack is
     * watched waiting every other tick.
     */
    private static final String WAITING_BATCH = "jscore_moving_solver_waiting";
    private static final int WAITING_WATCHED = 60;
    /*
     * The landing on a stack waiting, in a batch of its own too: how many crates the stack stands, how far above it
     * the crate thrown onto it starts, in blocks, and how fast it is thrown down, in metres a second: a block and a
     * half a tick, so the tick before it lands it is still beyond the stack's reach and goes on alone.
     */
    private static final String WAITING_LANDING_BATCH = "jscore_moving_solver_waiting_landing";
    private static final int WAITING_STACKED = 4;
    private static final int WAITING_DROP = 3;
    private static final double WAITING_THROWN = 30;
    /* How much a structure's speed may change in a tick it waited, in metres a second: only rounding. */
    private static final double PUSHED_WAITING = 1.0e-6;
    /* The wall the fast bodies fly at: its west face, in the arena's places. */
    private static final int WALL = 40;
    /*
     * How fast the two bodies fly at it, in metres a second: three blocks a tick, and seven and a half, more than a
     * tick's steps of half a block each carry a body.
     */
    private static final double FAST = 60;
    private static final double FASTER = 150;
    /* How fast the cart and the sled are set going east, in metres a second. */
    private static final double SET_GOING = 3;
    /* The arenas whose air is still, sheltered from the wind. */
    private static final Set<AABB> SHELTERED = ConcurrentHashMap.newKeySet();
    private static boolean shelterDeclared;

    private MovingStructureSolverGameTests() {
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 600)
    public static void stack_fiveStructuresSettleSleepAndStayStill(final GameTestHelper helper) {
        final AABB sheltered = shelter(helper);
        final List<MovingStructure> stack = new ArrayList<>();
        for (int i = 0; i < STACKED; i++) {
            stack.add(made(helper, List.of(new BlockPos(5, FLOOR + 2 * i, 5)), Blocks.OAK_PLANKS));
        }
        stack.forEach(structure -> structure.move(PhysicsMotion.AT_REST));
        final double[] rested = new double[STACKED];
        // A failure is told by the first check that fails: one failing in the same tick as another takes its place.
        final boolean[] settled = new boolean[1];
        // The ticks the top went aside as they settled, and the last few ticks watched, for a failure to tell.
        final List<String> aside = new ArrayList<>();
        final List<String> lately = new ArrayList<>();
        helper.startSequence()
                .thenWaitUntil(() -> {
                    final StructurePhysics top = stack.getLast().physics();
                    if (top != null && aside.size() < LATELY && Math.hypot(top.body().velocity().x(),
                            top.body().velocity().z()) > ASIDE) {
                        aside.add(watched(helper, stack));
                    }
                    helper.assertTrue(stack.stream().allMatch(MovingStructureSolverGameTests::asleep),
                            "all five come to rest: " + boxes(stack));
                })
                .thenExecute(() -> {
                    helper.assertTrue(Math.abs(stack.get(0).worldBox().minY - y(helper, FLOOR)) < RESTS,
                            "the lowest rests on the floor: " + boxes(stack));
                    rested[0] = stack.get(0).worldBox().minY;
                    for (int i = 1; i < STACKED; i++) {
                        final AABB box = stack.get(i).worldBox();
                        final AABB below = stack.get(i - 1).worldBox();
                        helper.assertTrue(Math.abs(box.minY - below.maxY) < RESTS
                                && Math.abs(box.minX - below.minX) < DRIFTS && Math.abs(box.minZ - below.minZ)
                                < DRIFTS, "each rests squarely on the one below: " + boxes(stack)
                                + "; the top went aside at: " + String.join(" | ", aside));
                        rested[i] = box.minY;
                    }
                    settled[0] = true;
                })
                .thenExecuteFor(WATCHED, () -> {
                    if (!settled[0]) {
                        return;
                    }
                    lately.add(watched(helper, stack));
                    if (lately.size() > LATELY) {
                        lately.removeFirst();
                    }
                    for (int i = 0; i < STACKED; i++) {
                        helper.assertTrue(asleep(stack.get(i))
                                && Math.abs(stack.get(i).worldBox().minY - rested[i]) < STILL,
                                "the stack sleeps on, still: " + boxes(stack) + "; the last ticks: "
                                        + String.join(" | ", lately));
                    }
                })
                .thenExecute(() -> {
                    stack.forEach(MovingStructures::takeAway);
                    SHELTERED.remove(sheltered);
                })
                .thenSucceed();
    }

    /*
     * A tall stack set swaying as a whole, turning slowly about its foot, its top going a tenth of a metre a second, as
     * a hair of the solver's own may set one going: it comes still and falls asleep, upright where it stood, its sway
     * dying away rather than growing with every swing. In its batch of its own, so nothing else takes its budget.
     */
    @GameTest(template = TALL, batch = SWAYING_BATCH, timeoutTicks = 400)
    public static void stack_aTallStackSwayingComesStillAndSleeps(final GameTestHelper helper) {
        final AABB sheltered = shelter(helper);
        final List<MovingStructure> stack = new ArrayList<>();
        for (int i = 0; i < TALL_STACKED; i++) {
            stack.add(made(helper, List.of(new BlockPos(5, FLOOR + i, 5)), Blocks.OAK_PLANKS));
        }
        stack.forEach(structure -> structure.move(PhysicsMotion.AT_REST));
        helper.startSequence()
                .thenExecuteAfter(SWAY_AFTER, () -> {
                    for (int i = 0; i < stack.size(); i++) {
                        final StructurePhysics physics = stack.get(i).physics();
                        helper.assertTrue(physics != null, "each crate moves by its physics");
                        final Vec3 middle = stack.get(i).worldBox().getCenter();
                        // Each crate as fast as the tower turning about its foot carries it: faster the higher it is.
                        physics.push(new Vector3d(physics.mass() * SWAY_EAST * i, 0, -physics.mass() * SWAY_NORTH * i),
                                new Vector3d(middle.x, middle.y, middle.z));
                    }
                })
                .thenWaitUntil(() -> helper.assertTrue(stack.stream().allMatch(MovingStructureSolverGameTests::asleep),
                        "the swaying stack comes still and falls asleep: " + watched(helper, stack)))
                .thenExecute(() -> {
                    final BlockPos foot = helper.absolutePos(new BlockPos(5, FLOOR, 5));
                    for (final MovingStructure crate : stack) {
                        final AABB box = crate.worldBox();
                        helper.assertTrue(Math.abs(box.minX - foot.getX()) < DRIFTS
                                && Math.abs(box.minZ - foot.getZ()) < DRIFTS,
                                "upright where it stood: " + boxes(stack));
                    }
                    stack.forEach(MovingStructures::takeAway);
                    SHELTERED.remove(sheltered);
                })
                .thenSucceed();
    }

    /*
     * A tall stack set down touching, at rest from the first, square on its four corners each, held up from the lowest
     * to the highest and back each pass, its contacts read before any starts from its push of the step before: it
     * stands as it was set and falls asleep, and none of it reads the stack closing as a blow.
     */
    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 300)
    public static void stack_aTallStackSetDownAtRestSleepsAndHearsNoBlow(final GameTestHelper helper) {
        final AABB sheltered = shelter(helper);
        final List<MovingStructure> stack = new ArrayList<>();
        for (int i = 0; i < TALL_STACKED; i++) {
            stack.add(made(helper, List.of(new BlockPos(5, FLOOR + i, 5)), Blocks.OAK_PLANKS));
        }
        final List<String> heard = new CopyOnWriteArrayList<>();
        final Consumer<MovingStructureEvent.Collision> ear = event -> {
            if (stack.contains(event.structure())) {
                heard.add(String.format(Locale.ROOT, "t%d %.2f m/s at %.1f", helper.getTick(), event.speed(),
                        event.at().y - y(helper, FLOOR)));
            }
        };
        NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Collision.class, ear);
        final double[] fastest = {0};
        final String[] when = {""};
        stack.forEach(structure -> structure.move(PhysicsMotion.AT_REST));
        helper.startSequence()
                .thenWaitUntil(() -> {
                    for (final MovingStructure each : stack) {
                        final StructurePhysics physics = each.physics();
                        final double down = physics == null ? 0 : -physics.body().velocity().y();
                        if (down > fastest[0]) {
                            fastest[0] = down;
                            when[0] = watched(helper, stack);
                        }
                    }
                    helper.assertTrue(stack.stream().allMatch(MovingStructureSolverGameTests::asleep),
                            "the stack falls asleep: " + boxes(stack));
                })
                .thenExecute(() -> {
                    NeoForge.EVENT_BUS.unregister(ear);
                    stack.forEach(MovingStructures::takeAway);
                    SHELTERED.remove(sheltered);
                    helper.assertTrue(heard.isEmpty(), "set down at rest, no blow is heard: " + heard);
                    helper.assertTrue(fastest[0] <= SETTLING, "set down at rest, none of it falls as fast as a blow: "
                            + String.format(Locale.ROOT, "%.3f m/s", fastest[0]) + " at " + when[0]);
                })
                .thenSucceed();
    }

    /*
     * The tall stack set down at rest with no physics budget at all, in its batch of its own, so each crate waits every
     * other tick and those beside it go on without it: one waiting stands as it is for those that go, as one asleep
     * does, never pushed down with nothing of the crate below under it. No crate sinks into the one below and no blow
     * is heard; given its budget again, the stack falls asleep.
     */
    @GameTest(template = TALL, batch = WAITING_BATCH, timeoutTicks = 400)
    public static void stack_aStackWaitingOnTheBudgetStandsAndHearsNoBlow(final GameTestHelper helper) {
        final AABB sheltered = shelter(helper);
        final List<MovingStructure> stack = new ArrayList<>();
        for (int i = 0; i < TALL_STACKED; i++) {
            stack.add(made(helper, List.of(new BlockPos(5, FLOOR + i, 5)), Blocks.OAK_PLANKS));
        }
        final List<String> heard = new CopyOnWriteArrayList<>();
        final Consumer<MovingStructureEvent.Collision> ear = event -> {
            if (stack.contains(event.structure())) {
                heard.add(String.format(Locale.ROOT, "t%d %.2f m/s at %.1f", helper.getTick(), event.speed(),
                        event.at().y - y(helper, FLOOR)));
            }
        };
        final double budget = StructureBalance.physicsMillis();
        final double[] sunk = {0};
        final String[] deepest = {""};
        NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Collision.class, ear);
        StructureBalance.physicsMillis(0);
        stack.forEach(structure -> structure.move(PhysicsMotion.AT_REST));
        helper.startSequence()
                .thenExecuteFor(WAITING_WATCHED, () -> {
                    for (int i = 1; i < stack.size(); i++) {
                        final double into = stack.get(i - 1).worldBox().maxY - stack.get(i).worldBox().minY;
                        if (into > sunk[0]) {
                            sunk[0] = into;
                            deepest[0] = watched(helper, stack);
                        }
                    }
                })
                .thenExecute(() -> StructureBalance.physicsMillis(budget))
                .thenWaitUntil(() -> helper.assertTrue(stack.stream().allMatch(MovingStructureSolverGameTests::asleep),
                        "given its budget again, the stack falls asleep: " + watched(helper, stack)))
                .thenExecute(() -> {
                    NeoForge.EVENT_BUS.unregister(ear);
                    stack.forEach(MovingStructures::takeAway);
                    SHELTERED.remove(sheltered);
                    helper.assertTrue(heard.isEmpty(), "waiting on its budget, no blow is heard: " + heard);
                    helper.assertTrue(sunk[0] <= SUNK_AT_MOST, "no crate sinks into the one below while they wait: "
                            + String.format(Locale.ROOT, "%.3f", sunk[0]) + " at " + deepest[0]);
                })
                .thenSucceed();
    }

    /*
     * A crate dropped onto a stack with no physics budget at all, in its batch of its own: the stack, let go a tick
     * before the crate and worked first each tick, as it was made first, waits every tick the crate goes, so the
     * crate lands on it while it waits. The stack waiting stands as it is for the crate, as the ground does, and the
     * landing never shoves its top crate down into the one below with nothing of the stack under it.
     */
    @GameTest(template = TALL, batch = WAITING_LANDING_BATCH, timeoutTicks = 300)
    public static void landing_aCrateLandingOnAStackWaitingOnTheBudgetNeverShovesItDown(final GameTestHelper helper) {
        final AABB sheltered = shelter(helper);
        final List<MovingStructure> stack = new ArrayList<>();
        for (int i = 0; i < WAITING_STACKED; i++) {
            stack.add(made(helper, List.of(new BlockPos(5, FLOOR + i, 5)), Blocks.IRON_BLOCK));
        }
        final MovingStructure crate = made(helper, List.of(new BlockPos(5, FLOOR + WAITING_STACKED + WAITING_DROP,
                5)), Blocks.IRON_BLOCK);
        final double budget = StructureBalance.physicsMillis();
        final Map<MovingStructure, double[]> before = new HashMap<>();
        final List<String> pushed = new ArrayList<>();
        StructureBalance.physicsMillis(0);
        stack.forEach(structure -> structure.move(PhysicsMotion.AT_REST));
        final List<MovingStructure> all = new ArrayList<>(stack);
        all.add(crate);
        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> crate.move(PhysicsMotion.going(0, -WAITING_THROWN, 0)))
                .thenExecuteFor(WAITING_WATCHED, () -> {
                    // One that stood still since the last tick waited, and was to be pushed by nothing meanwhile.
                    for (final MovingStructure each : stack) {
                        final StructurePhysics physics = each.physics();
                        if (physics == null) {
                            continue;
                        }
                        final AABB box = each.worldBox();
                        final double[] now = {box.minX, box.minY, box.minZ, physics.body().velocity().x(),
                                physics.body().velocity().y(), physics.body().velocity().z()};
                        final double[] last = before.put(each, now);
                        if (last != null && last[0] == now[0] && last[1] == now[1] && last[2] == now[2]
                                && Math.abs(last[4] - now[4]) + Math.abs(last[3] - now[3]) + Math.abs(last[5] - now[5])
                                > PUSHED_WAITING && pushed.isEmpty()) {
                            pushed.add(watched(helper, all));
                        }
                    }
                })
                .thenExecute(() -> {
                    StructureBalance.physicsMillis(budget);
                    all.forEach(MovingStructures::takeAway);
                    SHELTERED.remove(sheltered);
                    helper.assertTrue(crate.worldBox().minY < y(helper, FLOOR + WAITING_STACKED) + RESTS,
                            "the crate lands on the stack: " + watched(helper, all));
                    helper.assertTrue(pushed.isEmpty(), "a crate of the stack waiting is pushed by none: " + pushed);
                })
                .thenSucceed();
    }

    /*
     * Each of a stack falling onto the one below, landed just before it: which of two meeting works their meeting goes
     * by their ids, so in some stacks it is the one standing still, which finds the one falling where it stood as the
     * tick began. Met however far it came within the tick, it never sinks into the one below.
     */
    @GameTest(template = WIDE, batch = LANDING_BATCH, timeoutTicks = 400)
    public static void landing_aStackFallingOnItselfNeverSinksIntoItself(final GameTestHelper helper) {
        final AABB sheltered = shelter(helper);
        final List<List<MovingStructure>> stacks = new ArrayList<>();
        for (int column = 0; column < LANDING_STACKS; column++) {
            final List<MovingStructure> stack = new ArrayList<>();
            for (int i = 0; i < LANDING_STACKED; i++) {
                stack.add(made(helper, List.of(new BlockPos(5 + column * LANDING_APART, FLOOR + 2 * i, 5)),
                        Blocks.OAK_PLANKS));
            }
            stacks.add(stack);
        }
        final double[] deepest = new double[1];
        final String[] where = {""};
        helper.startSequence()
                .thenExecute(() -> stacks.forEach(stack -> stack.forEach(structure -> structure.move(
                        PhysicsMotion.AT_REST))))
                .thenExecuteFor(LANDING_WATCHED, () -> {
                    for (final List<MovingStructure> stack : stacks) {
                        for (int i = 1; i < stack.size(); i++) {
                            final double sunk = stack.get(i - 1).worldBox().maxY - stack.get(i).worldBox().minY;
                            if (sunk > deepest[0]) {
                                deepest[0] = sunk;
                                where[0] = "t" + helper.getTick() + " " + boxes(stack);
                            }
                        }
                    }
                })
                .thenExecute(() -> {
                    stacks.forEach(stack -> stack.forEach(MovingStructures::takeAway));
                    SHELTERED.remove(sheltered);
                    helper.assertTrue(deepest[0] < SUNK_AT_MOST, "none sinks into the one below as it lands: "
                            + deepest[0] + " at " + where[0]);
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE, batch = BATCH, timeoutTicks = 600)
    public static void hull_aGreatHullRestsFlatOnTheGroundWithoutTrembling(final GameTestHelper helper) {
        final AABB sheltered = shelter(helper);
        final List<BlockPos> places = new ArrayList<>();
        for (int y = FLOOR + 1; y <= FLOOR + 2; y++) {
            places.addAll(square(4, y, 4, 19, 11));
        }
        final MovingStructure hull = made(helper, places, Blocks.OAK_PLANKS);
        hull.move(PhysicsMotion.AT_REST);
        final double[] rested = new double[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(asleep(hull), "it comes to rest: " + hull.worldBox()))
                .thenExecute(() -> {
                    helper.assertTrue(Math.abs(hull.worldBox().minY - y(helper, FLOOR)) < RESTS,
                            "on the ground: " + hull.worldBox());
                    helper.assertTrue(hull.pose().angleTo(StructurePose.at(0, 0, 0)) < 0.01,
                            "lying flat: " + hull.pose());
                    rested[0] = hull.worldBox().minY;
                })
                // Asleep, it looks about twice a second: one trembling on its many points would wake.
                .thenExecuteFor(WATCHED, () -> helper.assertTrue(asleep(hull)
                        && Math.abs(hull.worldBox().minY - rested[0]) < STILL, "it sleeps on, still: "
                        + hull.worldBox()))
                .thenExecute(() -> {
                    MovingStructures.takeAway(hull);
                    SHELTERED.remove(sheltered);
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE, batch = BATCH, timeoutTicks = 200)
    public static void wall_aFastBodyStopsAtAWallAndNeverGoesThrough(final GameTestHelper helper) {
        final AABB sheltered = shelter(helper);
        for (int y = FLOOR; y <= FLOOR + 5; y++) {
            for (int z = 16; z <= 32; z++) {
                helper.setBlock(new BlockPos(WALL, y, z), Blocks.BEDROCK);
            }
        }
        // Netherite holds a blow at 150 metres a second, so what is proved is the stop, not a block broken.
        final MovingStructure fast = made(helper, List.of(new BlockPos(4, FLOOR + 2, 20)), Blocks.NETHERITE_BLOCK);
        final MovingStructure faster = made(helper, List.of(new BlockPos(4, FLOOR + 2, 28)),
                Blocks.NETHERITE_BLOCK);
        fast.move(PhysicsMotion.going(FAST, 0, 0));
        faster.move(PhysicsMotion.going(FASTER, 0, 0));
        final double wall = helper.absolutePos(new BlockPos(WALL, 0, 0)).getX();
        final List<MovingStructure> bodies = List.of(fast, faster);
        final boolean[] thrownBack = new boolean[bodies.size()];
        helper.startSequence()
                .thenExecuteFor(40, () -> {
                    for (int i = 0; i < bodies.size(); i++) {
                        final MovingStructure body = bodies.get(i);
                        helper.assertTrue(body.worldBox().maxX < wall + RESTS, "it never goes through: "
                                + body.worldBox() + ", the wall at " + wall);
                        final StructurePhysics physics = body.physics();
                        thrownBack[i] |= physics != null && physics.body().velocity().x < 0
                                && body.worldBox().maxX > wall - FASTER / StructurePhysics.TICKS;
                    }
                })
                .thenExecute(() -> {
                    helper.assertTrue(thrownBack[0] && thrownBack[1], "both struck the wall and were thrown back: "
                            + fast.worldBox() + " and " + faster.worldBox());
                    MovingStructures.takeAway(fast);
                    MovingStructures.takeAway(faster);
                    SHELTERED.remove(sheltered);
                })
                .thenSucceed();
    }

    @GameTest(template = WIDE, batch = BATCH, timeoutTicks = 300)
    public static void wheels_aCartSetGoingRollsOnWhereASledSlidesToAStop(final GameTestHelper helper) {
        final AABB sheltered = shelter(helper);
        for (final BlockPos wheel : List.of(new BlockPos(4, FLOOR, 4), new BlockPos(6, FLOOR, 4),
                new BlockPos(4, FLOOR, 6), new BlockPos(6, FLOOR, 6))) {
            helper.setBlock(wheel, TestPhysics.WHEEL.get().defaultBlockState()
                    .setValue(DirectionalBlock.FACING, Direction.EAST));
        }
        for (final BlockPos deck : square(4, FLOOR + 1, 4, 6, 6)) {
            helper.setBlock(deck, TestPhysics.LIGHT.get());
        }
        final MovingStructure cart = assembled(helper, new BlockPos(4, FLOOR, 4), new BlockPos(6, FLOOR + 1, 6));
        final MovingStructure sled = made(helper, square(4, FLOOR, 20, 6, 22), TestPhysics.LIGHT.get());
        cart.move(PhysicsMotion.AT_REST);
        sled.move(PhysicsMotion.AT_REST);
        final double[] from = new double[2];
        helper.startSequence()
                .thenExecuteAfter(40, () -> { })
                .thenWaitUntil(() -> helper.assertTrue(cart.physics() != null
                        && Math.abs(cart.physics().body().velocity().y) < 0.1, "the cart settles on its springs"))
                .thenExecute(() -> {
                    from[0] = cart.worldBox().minX;
                    from[1] = sled.worldBox().minX;
                    setGoing(cart);
                    setGoing(sled);
                })
                .thenExecuteAfter(30, () -> {
                    final double rolled = cart.worldBox().minX - from[0];
                    final double slid = sled.worldBox().minX - from[1];
                    helper.assertTrue(rolled > 2, "the cart rolls on: " + rolled + " blocks");
                    helper.assertTrue(slid < 1.5 && slid < rolled / 2, "the sled slides to a stop: " + slid
                            + " blocks, the cart " + rolled);
                    final StructurePhysics sliding = sled.physics();
                    helper.assertTrue(sliding != null && sliding.body().velocity().length() < 0.1,
                            "and has stopped");
                    MovingStructures.takeAway(cart);
                    MovingStructures.takeAway(sled);
                    SHELTERED.remove(sheltered);
                })
                .thenSucceed();
    }

    /* Makes the blocks {@code places} of {@code block} into a structure, its own box. */
    private static MovingStructure made(final GameTestHelper helper, final List<BlockPos> places, final Block block) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (final BlockPos place : places) {
            helper.setBlock(place, block);
            minX = Math.min(minX, place.getX());
            minY = Math.min(minY, place.getY());
            minZ = Math.min(minZ, place.getZ());
            maxX = Math.max(maxX, place.getX());
            maxY = Math.max(maxY, place.getY());
            maxZ = Math.max(maxZ, place.getZ());
        }
        return assembled(helper, new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
    }

    /* Makes every block of the arena's box from {@code from} to {@code to} into a structure. */
    private static MovingStructure assembled(final GameTestHelper helper, final BlockPos from, final BlockPos to) {
        warm(helper.getLevel());
        final StructureResult made = MovingStructures.assemble(helper.getLevel(), helper.absolutePos(from),
                helper.absolutePos(to));
        helper.assertTrue(made.ok() && made.structure() != null, "a structure is made: "
                + (made.refusal() == null ? "done" : made.refusal().english()));
        return made.structure();
    }

    /* The places of a square layer at {@code y} from {@code (fromX, fromZ)} to {@code (toX, toZ)}. */
    private static List<BlockPos> square(final int fromX, final int y, final int fromZ, final int toX,
                                         final int toZ) {
        final List<BlockPos> places = new ArrayList<>();
        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                places.add(new BlockPos(x, y, z));
            }
        }
        return places;
    }

    /* Stills the air of the test's arena until the test takes it out of {@link #SHELTERED}; the arena's box. */
    private static AABB shelter(final GameTestHelper helper) {
        synchronized (SHELTERED) {
            if (!shelterDeclared) {
                shelterDeclared = true;
                Winds.source((level, at) -> SHELTERED.stream().anyMatch(box -> box.contains(at))
                        ? Optional.of(Wind.STILL) : Optional.empty());
            }
        }
        final AABB box = helper.getBounds().inflate(1);
        SHELTERED.add(box);
        return box;
    }

    /* Pushes the structure east at its centre of mass, as hard as sets its whole mass going {@link #SET_GOING}. */
    private static void setGoing(final MovingStructure structure) {
        final StructurePhysics physics = structure.physics();
        if (physics != null) {
            physics.push(new Vector3d(physics.mass() * SET_GOING, 0, 0), new Vector3d(physics.body().position()));
        }
    }

    /* Whether the structure moves by its physics and has fallen asleep, at rest. */
    private static boolean asleep(final MovingStructure structure) {
        final StructurePhysics physics = structure.physics();
        return physics != null && physics.asleep();
    }

    /* The structures' boxes, for a test's failure to tell. */
    private static String boxes(final List<MovingStructure> structures) {
        final StringBuilder said = new StringBuilder();
        for (final MovingStructure structure : structures) {
            final AABB box = structure.worldBox();
            // How tall its box stands tells how far it leans: a block upright is one high.
            said.append(String.format(Locale.ROOT, "[%.3f %.3f %.3f tall %.3f]", box.minX, box.minY, box.minZ,
                    box.getYsize()));
        }
        return said.toString();
    }

    /*
     * A tick of the stack, for a failure to tell: the test's tick, and each structure's sleep (z asleep, w awake, - no
     * physics), whether its entity is gone, its box's lowest corner in the arena, and how fast it goes east, up and
     * south, in metres a second.
     */
    private static String watched(final GameTestHelper helper, final List<MovingStructure> structures) {
        final StringBuilder said = new StringBuilder("t" + helper.getTick());
        final BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        for (final MovingStructure structure : structures) {
            final StructurePhysics physics = structure.physics();
            final AABB box = structure.worldBox();
            said.append(String.format(Locale.ROOT, " %s%s(%.3f %.3f %.3f v %.2f %.2f %.2f)",
                    physics == null ? "-" : physics.asleep() ? "z" : "w", structure.entity().isRemoved() ? "!" : "",
                    box.minX - origin.getX(), box.minY - origin.getY(), box.minZ - origin.getZ(),
                    physics == null ? 0 : physics.body().velocity().x(),
                    physics == null ? 0 : physics.body().velocity().y(),
                    physics == null ? 0 : physics.body().velocity().z()));
        }
        return said.toString();
    }

    /* The world's height of the arena's {@code y}. */
    private static double y(final GameTestHelper helper, final int y) {
        return helper.absolutePos(new BlockPos(0, y, 0)).getY();
    }

    /* Loads the chunks round the yard's next plot, so a structure made comes into it at once. */
    private static void warm(final ServerLevel level) {
        final int plot = StructureYard.nextPlot(level);
        final int cx = YardLayout.centreX(plot) >> 4;
        final int cz = YardLayout.centreZ(plot) >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
    }
}
