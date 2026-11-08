/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.defrag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * What a machine keeps of its Defrag, saved with its console: the pass under way, how the last one ended, when a pass
 * last ended on each disk, and whether the weekly schedule is on, as it is out of the box.
 */
public final class MachineDefrag {

    @Nullable
    private DefragJob job;
    @Nullable
    private DefragState.Result last;
    private final Map<UUID, Long> lastRuns = new HashMap<>();
    private boolean schedule = true;
    private long scheduledAt;

    /** The pass under way, or null. */
    @Nullable
    public DefragJob job() {
        return this.job;
    }

    public void begin(final DefragJob started) {
        this.job = started;
    }

    /** The pass ended, the way {@code result} says, on the disk of {@code volume}, at game time {@code now}. */
    public void ended(final DefragState.Result result, final UUID volume, final long now) {
        this.job = null;
        this.last = result;
        this.lastRuns.put(volume, now);
    }

    /** The pass stopped where it stood, with what it did so far kept. */
    public void stopped() {
        this.job = null;
    }

    @Nullable
    public DefragState.Result last() {
        return this.last;
    }

    /** When a pass last ended on the disk of {@code volume}, or -1. */
    public long lastRun(@Nullable final UUID volume) {
        return volume == null ? -1L : this.lastRuns.getOrDefault(volume, -1L);
    }

    public boolean schedule() {
        return this.schedule;
    }

    public void schedule(final boolean on) {
        this.schedule = on;
    }

    /** When the schedule last looked for a disk to repair, in game time. */
    public long scheduledAt() {
        return this.scheduledAt;
    }

    public void scheduledAt(final long when) {
        this.scheduledAt = when;
    }

    public void save(final CompoundTag tag) {
        final CompoundTag out = new CompoundTag();
        if (this.job != null) {
            final DefragJob j = this.job;
            final CompoundTag pass = new CompoundTag();
            pass.putUUID("Volume", j.volume());
            pass.putInt("Disk", j.disk());
            pass.putInt("Total", j.ticksTotal());
            pass.putInt("Done", j.ticksDone());
            pass.putInt("Chance", j.chance());
            pass.putInt("Work", j.work());
            pass.putInt("Tried", j.tried());
            pass.putLong("Found", j.found());
            pass.putLong("Recovered", j.recovered());
            pass.putLong("Lost", j.lost());
            pass.putInt("BadBefore", j.badBefore());
            pass.putBoolean("Paused", j.paused());
            out.put("Pass", pass);
        }
        if (this.last != null) {
            final DefragState.Result r = this.last;
            out.putIntArray("Last", new int[] {r.disk(), r.badBefore(), r.badAfter()});
            out.putLongArray("LastCounts", new long[] {r.found(), r.recovered(), r.lost()});
        }
        final ListTag runs = new ListTag();
        this.lastRuns.forEach((volume, when) -> {
            final CompoundTag one = new CompoundTag();
            one.putUUID("Volume", volume);
            one.putLong("At", when);
            runs.add(one);
        });
        if (!runs.isEmpty()) {
            out.put("Runs", runs);
        }
        out.putBoolean("Schedule", this.schedule);
        out.putLong("ScheduledAt", this.scheduledAt);
        tag.put("Defrag", out);
    }

    public void load(final CompoundTag tag) {
        this.job = null;
        this.last = null;
        this.lastRuns.clear();
        this.schedule = true;
        this.scheduledAt = 0L;
        if (!tag.contains("Defrag")) {
            return;
        }
        final CompoundTag in = tag.getCompound("Defrag");
        if (in.contains("Pass")) {
            final CompoundTag p = in.getCompound("Pass");
            if (p.hasUUID("Volume")) {
                this.job = new DefragJob(p.getUUID("Volume"), p.getInt("Disk"), p.getInt("Total"), p.getInt("Chance"),
                        p.getInt("Work"), p.getLong("Found"), p.getInt("BadBefore"), p.getInt("Done"),
                        p.getInt("Tried"), p.getLong("Recovered"), p.getLong("Lost"));
                this.job.paused(p.getBoolean("Paused"));
            }
        }
        final int[] last = in.getIntArray("Last");
        final long[] counts = in.getLongArray("LastCounts");
        if (last.length == 3 && counts.length == 3) {
            this.last = new DefragState.Result(last[0], counts[0], counts[1], counts[2], last[1], last[2]);
        }
        for (final Tag element : in.getList("Runs", Tag.TAG_COMPOUND)) {
            final CompoundTag one = (CompoundTag) element;
            if (one.hasUUID("Volume")) {
                this.lastRuns.put(one.getUUID("Volume"), one.getLong("At"));
            }
        }
        this.schedule = !in.contains("Schedule") || in.getBoolean("Schedule");
        this.scheduledAt = in.getLong("ScheduledAt");
    }
}
