/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.nextgre;

import dev.jstech.computers.crafting.NetworkCraftOperation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * What NextgreIQL keeps on one Mainframe: the time each recipe was measured to take on its network, which of its
 * planner's rules are switched off, when its statistics were last gathered, and the plans it made lately.
 *
 * <p>It lives in the Mainframe's own save, under the engine's package, so it goes with the Mainframe and stays when
 * another engine runs a while. The crafts it is watching run are only held while they run.
 */
final class NextgreState {

    /** Each recipe's measured time, by {@link #timingKey}: the ticks and runs measured, and over how many crafts. */
    private final Map<String, Timing> timings = new LinkedHashMap<>();
    /** The rules switched off, by id: the engine's own and other mods' alike. */
    private final Set<String> off = new HashSet<>();
    private final Deque<NextgrePlanView> history = new ArrayDeque<>();
    /** The crafts the plans in the history became, by plan number, while they run. */
    private final Map<Integer, NetworkCraftOperation> running = new HashMap<>();
    private long analyzedAt = -1L;
    private int itemTypes;
    private int servers;
    private int nextId = 1;

    /** How many plans are kept. */
    static final int HISTORY = 16;
    /** How many crafts a recipe's time is averaged over before the oldest weigh half as much. */
    static final int SAMPLES = 50;

    /** Whether rule {@code id} is switched on. */
    boolean on(final String id) {
        return !off.contains(id);
    }

    /** Switches rule {@code id} the other way; answers whether it is on now. */
    boolean toggle(final String id) {
        if (!off.remove(id)) {
            off.add(id);
        }
        return on(id);
    }

    /** The rules switched off. */
    Set<String> off() {
        return Set.copyOf(off);
    }

    /** The measured time a run of each recipe takes, in ticks, by {@link #timingKey}. */
    Map<String, Long> perRun() {
        final Map<String, Long> out = new HashMap<>();
        timings.forEach((key, timing) -> out.put(key, timing.perRun()));
        return out;
    }

    /** Every recipe timed, with what was measured. */
    Map<String, Timing> timings() {
        return Map.copyOf(timings);
    }

    /** Adds what a step of a craft took: {@code ticks} for {@code runs} runs of the recipe under {@code key}. */
    void measured(final String key, final long ticks, final long runs) {
        if (ticks < 0L || runs <= 0L) {
            return;
        }
        final Timing was = timings.getOrDefault(key, Timing.NONE);
        final Timing now = was.samples() >= SAMPLES
                ? new Timing(was.ticks() / 2 + ticks, was.runs() / 2 + runs, was.samples() / 2 + 1)
                : new Timing(was.ticks() + ticks, was.runs() + runs, was.samples() + 1);
        timings.put(key, now);
    }

    /** Sets what ANALYZE gathered. */
    void analyzed(final long at, final int types, final int serverCount) {
        this.analyzedAt = at;
        this.itemTypes = types;
        this.servers = serverCount;
    }

    long analyzedAt() {
        return analyzedAt;
    }

    int itemTypes() {
        return itemTypes;
    }

    int servers() {
        return servers;
    }

    /** The number the next plan gets. */
    int takeId() {
        return nextId++;
    }

    /** Keeps a plan, the newest first, letting the oldest go past {@link #HISTORY}. */
    void remember(final NextgrePlanView plan, @Nullable final NetworkCraftOperation craft) {
        history.removeIf(kept -> kept.id() == plan.id());
        history.addFirst(plan);
        while (history.size() > HISTORY) {
            final NextgrePlanView gone = history.removeLast();
            running.remove(gone.id());
        }
        if (craft != null) {
            running.put(plan.id(), craft);
        }
    }

    /**
     * Swaps the kept plan with the same number for {@code plan}, leaving it where it stands in the history, so a
     * plan that settles late does not jump ahead of newer ones. A plan that is no longer kept stays gone.
     */
    void replace(final NextgrePlanView plan) {
        final List<NextgrePlanView> kept = new ArrayList<>(history);
        for (int i = 0; i < kept.size(); i++) {
            if (kept.get(i).id() == plan.id()) {
                kept.set(i, plan);
                history.clear();
                history.addAll(kept);
                return;
            }
        }
    }

    /** The plan numbered {@code id}, or null when it is not kept. */
    @Nullable
    NextgrePlanView plan(final int id) {
        for (final NextgrePlanView plan : history) {
            if (plan.id() == id) {
                return plan;
            }
        }
        return null;
    }

    /** The newest plan, or null when none is kept. */
    @Nullable
    NextgrePlanView latest() {
        return history.peekFirst();
    }

    /** The plans kept, the newest first. */
    List<NextgrePlanView> history() {
        return List.copyOf(history);
    }

    /** The craft plan {@code id} became, while it runs. */
    @Nullable
    NetworkCraftOperation craftOf(final int id) {
        return running.get(id);
    }

    /** Lets go of the craft plan {@code id} became, once it settled. */
    void settled(final int id) {
        running.remove(id);
    }

    /** The key a recipe's time is kept under: what it makes, and whether a machine or a bench makes it. */
    static String timingKey(final String item, final boolean machine) {
        return (machine ? "machine:" : "bench:") + item;
    }

    void save(final CompoundTag tag) {
        final ListTag times = new ListTag();
        timings.forEach((key, timing) -> {
            final CompoundTag row = new CompoundTag();
            row.putString("Key", key);
            row.putLong("Ticks", timing.ticks());
            row.putLong("Runs", timing.runs());
            row.putInt("Samples", timing.samples());
            times.add(row);
        });
        tag.put("Timings", times);
        final ListTag switchedOff = new ListTag();
        off.forEach(id -> switchedOff.add(StringTag.valueOf(id)));
        tag.put("Off", switchedOff);
        tag.putLong("AnalyzedAt", analyzedAt);
        tag.putInt("ItemTypes", itemTypes);
        tag.putInt("Servers", servers);
        tag.putInt("NextId", nextId);
        final ListTag plans = new ListTag();
        for (final NextgrePlanView plan : history) {
            plans.add(plan.save());
        }
        tag.put("History", plans);
    }

    static NextgreState load(final CompoundTag tag) {
        final NextgreState state = new NextgreState();
        final ListTag times = tag.getList("Timings", Tag.TAG_COMPOUND);
        for (int i = 0; i < times.size(); i++) {
            final CompoundTag row = times.getCompound(i);
            state.timings.put(row.getString("Key"),
                    new Timing(row.getLong("Ticks"), row.getLong("Runs"), row.getInt("Samples")));
        }
        final ListTag switchedOff = tag.getList("Off", Tag.TAG_STRING);
        for (int i = 0; i < switchedOff.size(); i++) {
            state.off.add(switchedOff.getString(i));
        }
        state.analyzedAt = tag.contains("AnalyzedAt") ? tag.getLong("AnalyzedAt") : -1L;
        state.itemTypes = tag.getInt("ItemTypes");
        state.servers = tag.getInt("Servers");
        state.nextId = Math.max(1, tag.getInt("NextId"));
        final ListTag plans = tag.getList("History", Tag.TAG_COMPOUND);
        final List<NextgrePlanView> loaded = new ArrayList<>();
        for (int i = 0; i < plans.size() && i < HISTORY; i++) {
            final NextgrePlanView plan = NextgrePlanView.load(plans.getCompound(i));
            /*
             * A craft that was running when the Mainframe was saved is planned again on load by the network, not by
             * this plan; the plan is kept as it last stood.
             */
            loaded.add(plan.state() == NextgrePlanView.RUNNING
                    ? plan.with(plan.nodes(), plan.executionTicks(), NextgrePlanView.FAILED) : plan);
        }
        state.history.addAll(loaded);
        return state;
    }

    /**
     * What was measured of one recipe.
     *
     * @param ticks   the ticks its steps took, all together
     * @param runs    the runs those steps made
     * @param samples how many steps were measured
     */
    record Timing(long ticks, long runs, int samples) {

        static final Timing NONE = new Timing(0L, 0L, 0);

        /** The ticks a run took, on the whole; at least one. */
        long perRun() {
            return runs <= 0L ? 0L : Math.max(1L, ticks / runs);
        }
    }
}
