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
import org.jetbrains.annotations.Nullable;

/**
 * The work a computer does with nobody at it: a line left running in the background, and a line to be run at an
 * hour.
 *
 * <p>This is what makes a machine worth leaving switched on. Until now a computer only ever did what somebody
 * typed while they stood there; with these it watches the network, fetches, builds and tidies on its own, and
 * a player who logs off comes back to work that was done.
 *
 * <p>One list for both, because they are the same thing seen twice: a line the machine will run, and when.
 * Each family has its own words for them, {@code &} and {@code jobs} on one side, {@code START} and {@code AT}
 * on the other, and both are this.
 */
public final class MachineJobs {

    private final List<Job> jobs = new ArrayList<>();
    private int nextId = 1;

    /** What one job costs the machine's memory, which is what says how many a computer may have at once. */
    public static final int JOB_MB = 1;

    /** How many ticks a game hour is, which is the shortest a schedule may repeat. */
    public static final int HOUR_TICKS = 1000;

    /** One job: what to run, when, and how it is getting on. */
    public record Job(int id, String line, JobWhen when, long lastRun, boolean running) {

        /** The same job, having just been run. */
        public Job ran(final long at) {
            return new Job(this.id, this.line, this.when, at, this.when.once() ? false : this.running);
        }
    }

    /** Puts a line in the list. */
    public Job add(final String line, final JobWhen when) {
        final Job job = new Job(this.nextId++, line, when, -1L, true);
        this.jobs.add(job);
        return job;
    }

    /** The jobs the machine has, oldest first. */
    public List<Job> all() {
        return List.copyOf(this.jobs);
    }

    /** The job of that number, or null. */
    @Nullable
    public Job byId(final int id) {
        for (final Job job : this.jobs) {
            if (job.id() == id) {
                return job;
            }
        }
        return null;
    }

    /** Takes a job off the list; true when there was one to take. */
    public boolean remove(final int id) {
        return this.jobs.removeIf(job -> job.id() == id);
    }

    /** How much of the machine's memory the jobs are holding. */
    public int heldMb() {
        return this.jobs.size() * JOB_MB;
    }

    public boolean isEmpty() {
        return this.jobs.isEmpty();
    }

    /**
     * The jobs that are due at that moment, in the order they were added, each marked as having run.
     *
     * <p>A job that runs once leaves the list when it has; one on a schedule stays and waits for its hour to
     * come round again. Nothing is run twice in the same hour however often this is asked.
     */
    public List<Job> due(final long dayTime) {
        final List<Job> ready = new ArrayList<>();
        for (int i = 0; i < this.jobs.size(); i++) {
            final Job job = this.jobs.get(i);
            if (!job.when().dueAt(dayTime, job.lastRun())) {
                continue;
            }
            ready.add(job);
            if (job.when().once()) {
                this.jobs.remove(i--);
            } else {
                this.jobs.set(i, job.ran(dayTime));
            }
        }
        return ready;
    }

    /** The number the next job will answer to, which is what a machine writes down beside them. */
    public int nextId() {
        return this.nextId;
    }

    /** Puts a list back as it was, which is what a machine that has just come back does. */
    public void restore(final List<Job> kept, final int nextId) {
        this.jobs.clear();
        this.jobs.addAll(kept);
        this.nextId = Math.max(1, nextId);
    }
}
