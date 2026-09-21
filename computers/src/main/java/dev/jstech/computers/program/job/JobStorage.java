/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.job;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Writing a machine's jobs down and reading them back.
 *
 * <p>Apart from the jobs themselves so that what a job is, and when it is due, stays arithmetic anybody can
 * hold to account without a world: the hours are the same whether they are in a save or in a test.
 */
public final class JobStorage {

    private JobStorage() {
    }

    public static void save(final MachineJobs jobs, final CompoundTag tag) {
        final ListTag list = new ListTag();
        for (final MachineJobs.Job job : jobs.all()) {
            final CompoundTag one = new CompoundTag();
            one.putInt("Id", job.id());
            one.putString("Line", job.line());
            one.putLong("LastRun", job.lastRun());
            one.putInt("Hour", job.when().hour());
            one.putIntArray("Days", job.when().dayNumbers());
            list.add(one);
        }
        tag.put("Jobs", list);
        tag.putInt("NextJob", jobs.nextId());
    }

    public static void load(final MachineJobs jobs, final CompoundTag tag) {
        final ListTag list = tag.getList("Jobs", Tag.TAG_COMPOUND);
        final List<MachineJobs.Job> kept = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag one = list.getCompound(i);
            kept.add(new MachineJobs.Job(one.getInt("Id"), one.getString("Line"),
                    JobWhen.of(one.getInt("Hour"), one.getIntArray("Days")), one.getLong("LastRun"), true));
        }
        jobs.restore(kept, tag.getInt("NextJob"));
    }
}
