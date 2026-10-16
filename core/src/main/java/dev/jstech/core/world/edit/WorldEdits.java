/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.edit;

import dev.jstech.core.JsCore;
import dev.jstech.core.diagnostic.Section;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.task.Tasks;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Large edits of the world that never stall the server: a crater blasted, a city levelled, a terraformed valley. The
 * edit is planned away from the server's thread, from a snapshot of the blocks it reads ({@link RegionSnapshot}), and
 * its blocks are set on the server's thread a share of each tick, within the work's budget, the edits under way taking
 * turns in the order they began.
 *
 * <pre>{@code
 * WorldEdit crater = WorldEdits.plan(level, BoundingBox.fromCorners(from, to), blocks -> {
 *     EditPlan.Builder plan = EditPlan.builder();
 *     // read blocks.get(x, y, z) freely, set what the crater leaves
 *     return plan.build();
 * });
 * crater.whenOver(edit -> report(edit.state()));     // DONE, STOPPED or FAILED
 * }</pre>
 */
public final class WorldEdits {

    /** The setting of the edits' blocks, timed, watched and kept to its budget. */
    public static final Section SECTION = Section.builder(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "world_edits"), CoreLogs.WORLD).budget(Duration.ofMillis(2)).declare();

    private static final Logger LOGGER = CoreLogs.WORLD.logger();
    private static final List<WorldEdit> SETTING = new ArrayList<>();
    /* How long a tick's share may run, in nanoseconds, and how many blocks are set between looks at the clock. */
    private static final long SLICE_NANOS = 1_500_000L;
    private static final int BETWEEN_LOOKS = 64;

    private WorldEdits() {
    }

    /** Hands the game's bus the end of each tick and the server's stop, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            if (!SETTING.isEmpty() && SECTION.due(event.getServer().getTickCount())) {
                SECTION.run(() -> set(SLICE_NANOS * SECTION.stride()));
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> {
            List.copyOf(SETTING).forEach(WorldEdit::stop);
            SETTING.clear();
        });
    }

    /**
     * Snapshots {@code box} of {@code level} now, plans the edit from it away from the server's thread with
     * {@code planner}, and sets what it plans, a share of each tick. On the server's thread.
     */
    public static WorldEdit plan(final ServerLevel level, final BoundingBox box,
                                 final Function<RegionSnapshot, EditPlan> planner) {
        Objects.requireNonNull(planner, "planner");
        final RegionSnapshot snapshot = RegionSnapshot.of(level, box);
        final WorldEdit edit = new WorldEdit(level);
        edit.planning(Tasks.onServer(() -> planner.apply(snapshot), made -> {
            if (!edit.over()) {
                begin(edit, made);
            }
        }, failure -> {
            LOGGER.warn("A large edit of {} could not be planned", level.dimension().location(), failure);
            edit.end(WorldEdit.State.FAILED);
        }));
        return edit;
    }

    /** Sets the blocks of {@code plan}, made already, in {@code level}, a share each tick. On the server's thread. */
    public static WorldEdit apply(final ServerLevel level, final EditPlan plan) {
        final WorldEdit edit = new WorldEdit(level);
        begin(edit, Objects.requireNonNull(plan, "plan"));
        return edit;
    }

    /** The edits under way, in the order they began. */
    public static List<WorldEdit> underWay() {
        return List.copyOf(SETTING);
    }

    /**
     * Sets blocks of the edits under way until {@code budgetNanos} are spent: what each tick does. Returns how many
     * were set.
     */
    public static int set(final long budgetNanos) {
        final long start = System.nanoTime();
        int setNow = 0;
        while (!SETTING.isEmpty() && System.nanoTime() - start < budgetNanos) {
            final WorldEdit edit = SETTING.getFirst();
            final EditPlan plan = edit.plan();
            if (edit.over() || plan == null) {
                SETTING.removeFirst();
                continue;
            }
            final int from = edit.set();
            final int to = Math.min(plan.size(), from + BETWEEN_LOOKS);
            final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int i = from; i < to; i++) {
                edit.level().setBlock(at.set(plan.place(i)), plan.block(i), plan.flags());
            }
            edit.advance(to - from);
            setNow += to - from;
            if (edit.set() >= plan.size()) {
                SETTING.removeFirst();
                edit.end(WorldEdit.State.DONE);
            }
        }
        return setNow;
    }

    private static void begin(final WorldEdit edit, final EditPlan plan) {
        edit.planned(plan);
        if (plan.size() == 0) {
            edit.end(WorldEdit.State.DONE);
        } else {
            SETTING.add(edit);
        }
    }
}
