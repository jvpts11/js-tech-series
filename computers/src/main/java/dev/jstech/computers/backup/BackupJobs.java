/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * The jobs a Backup Service keeps, on the server that runs it: they ride on the Server item with the rest of the
 * machine, so pulling the server takes its jobs with it, as a service belongs to the machine that runs it.
 */
public final class BackupJobs {

    private final List<BackupJob> jobs = new ArrayList<>();

    /** The most jobs one service keeps. */
    public static final int MOST = 32;
    private static final String KEY = "BackupJobs";

    /** Every job, in the order they were made. */
    public List<BackupJob> all() {
        return List.copyOf(jobs);
    }

    /** The job of that name, whatever its case, or null when there is none. */
    @Nullable
    public BackupJob named(final String name) {
        final String wanted = name.strip().toLowerCase(Locale.ROOT);
        for (final BackupJob job : jobs) {
            if (job.name().toLowerCase(Locale.ROOT).equals(wanted)) {
                return job;
            }
        }
        return null;
    }

    /** Keeps {@code job}, over the one of its name when there is one; false when the service is full. */
    public boolean put(final BackupJob job) {
        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).name().equalsIgnoreCase(job.name())) {
                jobs.set(i, job);
                return true;
            }
        }
        if (jobs.size() >= MOST) {
            return false;
        }
        jobs.add(job);
        return true;
    }

    /** Lets the job of that name go; false when there is none. */
    public boolean remove(final String name) {
        return jobs.removeIf(job -> job.name().equalsIgnoreCase(name));
    }

    /** Lets every job go, as uninstalling the service does. */
    public void clear() {
        jobs.clear();
    }

    /** Writes the jobs onto {@code tag}, or nothing at all when there are none. */
    public void save(final CompoundTag tag) {
        if (jobs.isEmpty()) {
            return;
        }
        final ListTag list = new ListTag();
        for (final BackupJob job : jobs) {
            BackupJob.CODEC.encodeStart(NbtOps.INSTANCE, job).result().ifPresent(list::add);
        }
        tag.put(KEY, list);
    }

    /**
     * Reads the jobs back from {@code tag}, in place of whatever was kept. A job that was under way is read as
     * interrupted: what waited at a library's drive is kept only while the world runs.
     */
    public void load(final CompoundTag tag) {
        jobs.clear();
        for (final Tag one : tag.getList(KEY, Tag.TAG_COMPOUND)) {
            BackupJob.CODEC.parse(NbtOps.INSTANCE, one).result()
                    .map(job -> job.result() == BackupJob.RunState.RUNNING ? job.ended(BackupJob.RunState.INTERRUPTED)
                            : job)
                    .ifPresent(jobs::add);
        }
    }
}
