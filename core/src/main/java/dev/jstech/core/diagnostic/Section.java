/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import dev.jstech.core.log.LogSystem;
import dev.jstech.core.util.DeclarationError;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

/**
 * A named piece of a system's work, such as moving a dimension's energy through its wires. Declared once, it runs the
 * work it is handed and, around it:
 *
 * <ul>
 *   <li>times it, for the debug screen (F3) and {@code /jstech health};</li>
 *   <li>runs it in a frame named after the section, so Spark, or any profiler that samples the threads, shows the
 *       section by name;</li>
 *   <li>lets the watchdog see it, which writes down the stack and the system of a section that runs too long, once,
 *       and tells the operators;</li>
 *   <li>keeps it to its budget, when it has one: a section that takes more than its budget a tick, averaged over a
 *       second, is slowed to run one tick in two, then in four, and so on, and says so; once well within it again,
 *       it speeds back up.</li>
 * </ul>
 *
 * <pre>{@code
 * public static final Section SPREAD = Section.builder(rl("fallout_spread"), LOG)
 *         .budget(Duration.ofMillis(1)).declare();
 * ...
 * if (SPREAD.due(level.getGameTime())) {
 *     SPREAD.run(() -> fallout.spread(level, SPREAD.stride()));
 * }
 * }</pre>
 *
 * <p>A system with a budget asks {@link #due} before its work and makes up the ticks it skipped by doing
 * {@link #stride} ticks' worth at once, so it goes on at its pace, only coarser.
 */
public final class Section {

    private final ResourceLocation id;
    private final LogSystem system;
    private final long budgetNanos;
    private final long stallNanos;
    private final String timing;
    private final ISectionFrame frame;
    /* What the section took this tick so far, added to from any thread it runs on. */
    private final AtomicLong spentThisTick = new AtomicLong();
    /* What each of the last ticks took, kept on the server's thread at the end of each tick. */
    private final long[] lastTicks = new long[WINDOW_TICKS];
    private final AtomicInteger stalls = new AtomicInteger();
    private final AtomicLong longestStallNanos = new AtomicLong();
    private final AtomicBoolean stallSaid = new AtomicBoolean();
    private int nextTick;
    private int ticksKept;
    private int ticksSincePaced;
    private volatile long averageNanos;
    private volatile int stride = 1;

    /** How many ticks a section's cost is averaged over: a second's. */
    public static final int WINDOW_TICKS = 20;
    /** The slowest a section is held to by its budget: one tick in this many. */
    public static final int SLOWEST_STRIDE = 64;

    private Section(final Builder builder) {
        this.id = builder.id;
        this.system = builder.system;
        this.budgetNanos = builder.budgetNanos;
        this.stallNanos = builder.stallNanos;
        this.timing = builder.id.toString();
        this.frame = SectionFrames.named(builder.id);
    }

    /** A section of {@code system}'s work called {@code id}, to be declared once, as a constant. */
    public static Builder builder(final ResourceLocation id, final LogSystem system) {
        return new Builder(id, system);
    }

    public ResourceLocation id() {
        return this.id;
    }

    /** The system whose work it is, whose log the watchdog writes to. */
    public LogSystem system() {
        return this.system;
    }

    /** The most the section may take a tick, averaged over a second, in nanoseconds; 0 when it has no budget. */
    public long budgetNanos() {
        return this.budgetNanos;
    }

    /** How long one run may take before the watchdog writes it down, in nanoseconds; 0 when the settings say. */
    public long stallNanos() {
        return this.stallNanos;
    }

    /** Runs {@code work} as this section, on this thread. */
    public void run(final Runnable work) {
        final Sections.Running running = Sections.enter(this);
        try {
            this.frame.run(work);
        } finally {
            Sections.leave(running);
        }
    }

    /** Runs {@code work} as this section, on this thread, and hands back what it gives. */
    @SuppressWarnings("unchecked")
    public <T> T call(final Supplier<? extends T> work) {
        final Sections.Running running = Sections.enter(this);
        try {
            return (T) this.frame.call(work);
        } finally {
            Sections.leave(running);
        }
    }

    /** Whether the section's work is due in the tick {@code gameTime}: every tick, unless its budget slowed it. */
    public boolean due(final long gameTime) {
        return gameTime % this.stride == 0L;
    }

    /** How many ticks the section's work is spread over now: 1 at full pace, more when its budget slowed it. */
    public int stride() {
        return this.stride;
    }

    /** Whether its budget slowed the section. */
    public boolean slowed() {
        return this.stride > 1;
    }

    /** What the section took a tick over the last second, on average, in nanoseconds. */
    public long averageNanos() {
        return this.averageNanos;
    }

    /** How many times a run of the section went on past what the watchdog lets it. */
    public int stalls() {
        return this.stalls.get();
    }

    /** The longest of those runs, in nanoseconds; 0 for none, or while the first still runs. */
    public long longestStallNanos() {
        return this.longestStallNanos.get();
    }

    @Override
    public String toString() {
        return this.timing;
    }

    /* How long one run may take now: the section's own, or what the settings say. */
    long stallLimit() {
        return this.stallNanos > 0L ? this.stallNanos : DiagnosticBalance.stallNanos();
    }

    /* A run of the section ended after {@code nanos}, and whether the watchdog had seen it stall. */
    void spent(final long nanos, final boolean stalled) {
        this.spentThisTick.addAndGet(nanos);
        Diagnostics.timings().record(this.timing, nanos);
        if (stalled) {
            this.longestStallNanos.accumulateAndGet(nanos, Math::max);
        }
    }

    /* The watchdog saw a run go on too long; whether it is the first, which is the one written down. */
    boolean stalledFirst() {
        this.stalls.incrementAndGet();
        return this.stallSaid.compareAndSet(false, true);
    }

    /*
     * The end of a server tick, on its thread: keeps what the tick took, and once a second paces the section by its
     * budget, slowing it while it is over and speeding it back up while it takes less than half. Hands back the
     * stride it had before, to tell a change by.
     */
    int endTick() {
        final int before = this.stride;
        this.lastTicks[this.nextTick] = this.spentThisTick.getAndSet(0L);
        this.nextTick = (this.nextTick + 1) % WINDOW_TICKS;
        this.ticksKept = Math.min(WINDOW_TICKS, this.ticksKept + 1);
        long sum = 0L;
        for (int i = 0; i < this.ticksKept; i++) {
            sum += this.lastTicks[i];
        }
        this.averageNanos = sum / this.ticksKept;
        if (this.budgetNanos <= 0L || ++this.ticksSincePaced < WINDOW_TICKS) {
            return before;
        }
        this.ticksSincePaced = 0;
        if (this.averageNanos > this.budgetNanos && before < SLOWEST_STRIDE) {
            this.stride = before * 2;
        } else if (this.averageNanos * 2L < this.budgetNanos && before > 1) {
            this.stride = before / 2;
        }
        return before;
    }

    /** How a section is declared: its budget and how long it may run, both optional. */
    public static final class Builder {

        private final ResourceLocation id;
        private final LogSystem system;
        private long budgetNanos;
        private long stallNanos;

        private Builder(final ResourceLocation id, final LogSystem system) {
            this.id = Objects.requireNonNull(id, "id");
            this.system = Objects.requireNonNull(system, "system");
        }

        /** The most the section may take a tick, averaged over a second, before it slows itself. */
        public Builder budget(final Duration perTick) {
            this.budgetNanos = Math.max(0L, perTick.toNanos());
            return this;
        }

        /** How long one run may take before the watchdog writes it down, instead of what the settings say. */
        public Builder stallAfter(final Duration time) {
            this.stallNanos = Math.max(0L, time.toNanos());
            return this;
        }

        /**
         * Declares the section.
         *
         * @throws DeclarationError when a section of that id is declared already
         */
        public Section declare() {
            return Sections.declare(new Section(this));
        }
    }
}
