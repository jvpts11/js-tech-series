/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.dimension.Winds;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.moving.ForeignMovers;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.physics.Physics;
import dev.jstech.core.room.RoomStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * The physics of the structures of a dimension worked beside the server's thread when they lie apart. At the start of
 * the dimension's tick, on the server's thread, each structure moving by its physics readies its island; the islands
 * are put in groups that lie apart, each group reading only the world round it, gathered for it into a
 * {@link PhysicsLens}; the groups are worked at once on the moving structures' threads, the islands of a group in
 * turn; then, on the server's thread again, what each would change is changed and each island finished, so every
 * structure takes its step in its turn as it always did. With one group, or with another mod's moving structures about
 * (whose own do not say they may be read beside the server's thread), nothing is worked early: each island is worked
 * in its turn, on the server's thread, as before.
 */
@ApiStatus.Internal
public final class PhysicsWorkers {

    private static final Logger LOGGER = CoreLogs.MOVING.logger();
    /* How many islands were worked beside the server's thread since the game started. */
    private static final AtomicLong BESIDE = new AtomicLong();

    private PhysicsWorkers() {
    }

    /** How many islands were worked beside the server's thread since the game started; for a test. */
    public static long workedBeside() {
        return BESIDE.get();
    }

    /**
     * Works the physics of {@code roots}, the structures of {@code level} moving by their physics and held by none,
     * beside the server's thread on {@code pool} when they lie apart; from the start of the dimension's tick, on the
     * server's thread.
     */
    public static void work(final ServerLevel level, final List<MovingStructure> roots, final ForkJoinPool pool) {
        if (roots.size() < 2 || ForeignMovers.any() || !apart(roots)) {
            return;
        }
        final List<PhysicsIsland> islands = new ArrayList<>();
        long expected = 0;
        for (final MovingStructure root : roots) {
            if (root.motion() instanceof PhysicsMotion motion) {
                final StructurePhysics physics = root.physicsMade();
                final PhysicsIsland island = physics.readyBeside(level, motion, expected);
                if (island != null) {
                    islands.add(island);
                    expected += physics.lastCost();
                }
            }
        }
        final List<List<PhysicsIsland>> groups = PhysicsIsland.apart(islands);
        if (groups.size() < 2) {
            // Worked as one, it is worked here and now, as it would be in its turn.
            for (final PhysicsIsland island : islands) {
                island.solve(level);
                finish(level, island);
            }
            return;
        }
        keepAtHand(level);
        final List<PhysicsLens> lenses = new ArrayList<>(groups.size());
        for (final List<PhysicsIsland> group : groups) {
            final PhysicsLens lens = new PhysicsLens(level);
            group.forEach(island -> island.lend(level, lens));
            lenses.add(lens);
        }
        pool.submit(() -> IntStream.range(0, groups.size()).parallel()
                .forEach(i -> solve(level, groups.get(i), lenses.get(i)))).join();
        for (int i = 0; i < groups.size(); i++) {
            lenses.get(i).runLater();
            for (final PhysicsIsland island : groups.get(i)) {
                finish(level, island);
            }
        }
    }

    /*
     * Whether {@code roots} lie in at least two places apart, by where each may reach this tick as it stands, before
     * anything of them is readied: what is worked as one is left to its turn.
     */
    private static boolean apart(final List<MovingStructure> roots) {
        AABB first = null;
        for (final MovingStructure root : roots) {
            final AABB reach = root.physicsMade().reachThisTick().inflate(PhysicsIsland.READ_REACH);
            if (first == null) {
                first = reach;
            } else if (!first.intersects(reach)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Has the dimension's saved data the physics reads loaded on the server's thread, its rules, its rooms, its laws
     * and its winds, so a thread beside it only reads what is already in memory and never reads them from disk.
     */
    private static void keepAtHand(final ServerLevel level) {
        DimensionRulesData.of(level);
        RoomStore.find(level);
        Physics.lawsAt(level, Vec3.ZERO);
        Winds.at(level, Vec3.ZERO);
    }

    /* Works the islands of one group in turn, through its lens, on a thread beside the server's. */
    private static void solve(final ServerLevel level, final List<PhysicsIsland> group, final PhysicsLens lens) {
        lens.hold();
        try {
            for (final PhysicsIsland island : group) {
                try {
                    island.solve(level);
                    BESIDE.incrementAndGet();
                } catch (final RuntimeException failed) {
                    LOGGER.warn("The physics of a moving structure in {} failed beside the server's thread; it waits",
                            level.dimension().location(), failed);
                    island.hold();
                }
            }
        } finally {
            lens.release();
        }
    }

    /* Finishes an island on the server's thread, its cost kept against the dimension's budget. */
    private static void finish(final ServerLevel level, final PhysicsIsland island) {
        island.finish(level);
        island.leader().spentBeside(level, island.cost());
    }
}
