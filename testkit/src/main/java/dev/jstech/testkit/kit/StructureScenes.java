/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.kit;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.testkit.gametest.Expect;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * A scene for moving structures: the blocks of a box of the arena made a structure, a failure saying why when they
 * are refused; a structure moved to a place at a speed; and checked to stand where it should.
 *
 * <pre>{@code
 * final MovingStructure cart = StructureScenes.assemble(helper, new BlockPos(2, 2, 2), new BlockPos(3, 2, 2));
 * StructureScenes.moveBy(cart, new Vec3(4, 0, 0), 4.0);
 * helper.runAfterDelay(40, () -> {
 *     StructureScenes.expectAt(helper, cart, ...);
 *     MovingStructures.takeAway(cart);
 *     helper.succeed();
 * });
 * }</pre>
 *
 * <p>A structure lives in a yard of its own, away from the arena: take it away at the end of the test.
 */
public final class StructureScenes {

    /** How close a structure stands to where it should to be said to stand there, in blocks. */
    public static final double CLOSE = 0.05;

    private static final int WARM_CHUNKS = 2;

    private StructureScenes() {
    }

    /**
     * Makes the blocks from the arena's place {@code from} to {@code to} a moving structure; a failure saying why
     * when they are refused.
     */
    public static MovingStructure assemble(final GameTestHelper helper, final BlockPos from, final BlockPos to) {
        final ServerLevel level = helper.getLevel();
        warmYard(level);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(from),
                helper.absolutePos(to));
        if (!made.ok() || made.structure() == null) {
            throw Expect.failure(helper, from, "a moving structure assembled: refused, "
                    + (made.refusal() == null ? "for no reason given" : made.refusal().english()));
        }
        return made.structure();
    }

    /** Sets {@code structure} moving by {@code by} from where it stands, at {@code speed} blocks a second. */
    public static void moveBy(final MovingStructure structure, final Vec3 by, final double speed) {
        final StructurePose now = structure.pose();
        MovingStructures.move(structure, new TowardMotion(now.movedTo(now.x() + by.x, now.y() + by.y,
                now.z() + by.z), speed, 0));
    }

    /**
     * That {@code structure}'s middle stands at the arena's point {@code expected}, within {@link #CLOSE}; else a
     * failure saying where it stands.
     */
    public static void expectAt(final GameTestHelper helper, final MovingStructure structure, final Vec3 expected) {
        final StructurePose pose = structure.pose();
        final Vec3 there = helper.absoluteVec(expected);
        final double off = there.distanceTo(new Vec3(pose.x(), pose.y(), pose.z()));
        if (off > CLOSE) {
            throw Expect.failure(helper, BlockPos.containing(expected), "the moving structure: expected at "
                    + there + ", found at " + pose.x() + ", " + pose.y() + ", " + pose.z() + ", off by " + off);
        }
    }

    /* Loads the chunks round the yard's next plot, so the structure is made at once rather than when they load. */
    private static void warmYard(final ServerLevel level) {
        final int plot = StructureYard.nextPlot(level);
        final int cx = YardLayout.centreX(plot) >> 4;
        final int cz = YardLayout.centreZ(plot) >> 4;
        for (int dx = -WARM_CHUNKS; dx <= WARM_CHUNKS; dx++) {
            for (int dz = -WARM_CHUNKS; dz <= WARM_CHUNKS; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
    }
}
