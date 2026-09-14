/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.core.language.ExecutionBalance;
import java.util.HashSet;
import java.util.Set;
import java.util.function.LongSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * How long each machine may run programs this tick, by the clock.
 *
 * <p>Two clocks bound a tick: the machine's own, so no one computer takes more than its share of real
 * time, and the server's, shared by every machine that ticks, so all of them together cannot take the
 * tick with them. A machine that finds the server's time already spent runs nothing, and is let run
 * next tick whatever the others do, so being late in the tick order is a delay of one tick and never a
 * standstill.
 *
 * <p>Neither clock changes what a machine's processors are worth: a slow machine stays slow next to a
 * fast one however busy the server is. They only decide how much of that worth fits in a tick.
 */
public final class ServerTickDeadline {

    /** What a machine gets when the server has no time for it this tick. */
    public static final long NONE = Long.MIN_VALUE;

    private static final ServerTickDeadline SHARED =
            new ServerTickDeadline(ExecutionBalance::serverNanos, ExecutionBalance::machineNanos);

    private final LongSupplier serverNanos;
    private final LongSupplier machineNanos;
    private long tick = Long.MIN_VALUE;
    private long serverDeadline;
    private Set<GlobalPos> starvedBefore = new HashSet<>();
    private Set<GlobalPos> starvedNow = new HashSet<>();

    /** One with clocks of its own, for running it against something other than the balance. */
    public ServerTickDeadline(final LongSupplier serverNanos, final LongSupplier machineNanos) {
        this.serverNanos = serverNanos;
        this.machineNanos = machineNanos;
    }

    /** The one every machine in the world claims from. */
    public static ServerTickDeadline shared() {
        return SHARED;
    }

    /** Claims this tick's deadline for the machine at that place, on the server's clock. */
    public long claim(final ServerLevel level, final BlockPos at) {
        return this.claim(level.getServer().getTickCount(), level.dimension(), at, System.nanoTime());
    }

    /**
     * The same with the tick and the time given.
     *
     * <p>The first claim of a tick sets the server's deadline from that moment; every claim after it in
     * the same tick shares it. Once the server's time is spent the machine is noted, and its next claim
     * gets the machine's own deadline without looking at the server's.
     *
     * @return when the machine's programs must stop on that clock, or {@link #NONE}
     */
    public long claim(final long tickCount, final ResourceKey<Level> dimension, final BlockPos at,
                      final long now) {
        if (tickCount != this.tick) {
            this.tick = tickCount;
            this.serverDeadline = now + this.serverNanos.getAsLong();
            final Set<GlobalPos> gone = this.starvedBefore;
            this.starvedBefore = this.starvedNow;
            this.starvedNow = gone;
            this.starvedNow.clear();
        }
        final long own = now + this.machineNanos.getAsLong();
        /*
         * Only a machine the server had no time for last tick is in this set, and on most ticks nobody is: the key is
         * built only when there is someone to find, or someone to note down.
         */
        if (!this.starvedBefore.isEmpty() && this.starvedBefore.remove(GlobalPos.of(dimension, at.immutable()))) {
            return own;
        }
        if (now >= this.serverDeadline) {
            this.starvedNow.add(GlobalPos.of(dimension, at.immutable()));
            return NONE;
        }
        return Math.min(own, this.serverDeadline);
    }
}
