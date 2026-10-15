/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import dev.jstech.core.registry.CoreAttachments;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The heat of one dimension: the blocks that hold heat and are not in balance, each looked at in turn, a few hundred a
 * tick, for the seconds since it was last looked at. Each carries heat to and from its neighbours that hold heat, by
 * their materials' conductivity; each face that meets the air gives heat to it, as thick as the air is, and radiates;
 * each face that meets another block of the world gives heat into it, the world's blocks holding the place's
 * temperature. A block in balance with everything round it sleeps, and only a change wakes it: idle heat costs
 * nothing.
 */
public final class LevelHeat {

    private final LongLinkedOpenHashSet active = new LongLinkedOpenHashSet();
    private final Long2LongOpenHashMap lastLook = new Long2LongOpenHashMap();

    /** The most blocks looked at in one tick; the rest wait their turn, for the time they waited. */
    public static final int BATCH = 512;
    /** How close to balance a block is when it sleeps, in kelvin. */
    public static final double BALANCE = 0.05;
    /** The conductivity taken for a block of the world, which holds no heat of its own: stone's, near enough. */
    public static final double GROUND = 2.5;
    /* The longest step a block is moved by at once, in seconds, however long it waited. */
    private static final double LONGEST_STEP = 5.0;

    /** The heat of {@code level}. */
    public static LevelHeat of(final ServerLevel level) {
        return level.getData(CoreAttachments.HEAT);
    }

    /** Wakes the block at {@code pos} of {@code level}: something it holds or meets changed. */
    public static void wake(@Nullable final Level level, final BlockPos pos) {
        if (level instanceof ServerLevel server) {
            of(server).wake(pos.asLong());
        }
    }

    /** Wakes the block at {@code pos}. */
    public void wake(final long pos) {
        this.active.add(pos);
    }

    /** Whether the block at {@code pos} is awake. */
    public boolean awake(final BlockPos pos) {
        return this.active.contains(pos.asLong());
    }

    /** How many blocks are awake. */
    public int awake() {
        return this.active.size();
    }

    /** Looks at the blocks whose turn it is, from the head of the queue, and puts back those still out of balance. */
    public void tick(final ServerLevel level) {
        final long now = level.getGameTime();
        final int turns = Math.min(BATCH, this.active.size());
        for (int i = 0; i < turns && !this.active.isEmpty(); i++) {
            final long pos = this.active.removeFirstLong();
            if (look(level, BlockPos.of(pos), now)) {
                this.active.add(pos);
            } else {
                // What it moved may have woken it again: in balance, it sleeps all the same.
                this.active.remove(pos);
                this.lastLook.remove(pos);
            }
        }
    }

    /* Moves the block's heat for the time since it was last looked at; whether it stays awake. */
    private boolean look(final ServerLevel level, final BlockPos pos, final long now) {
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof IHeatHolder holder)) {
            return false;
        }
        final HeatBody body = holder.heatBody();
        final double ambient = HeatClimate.ambient(level, pos);
        if (!body.known()) {
            body.set(ambient);
        }
        final long last = this.lastLook.containsKey(pos.asLong()) ? this.lastLook.get(pos.asLong()) : now - 1;
        final double seconds = Math.min(LONGEST_STEP, Math.max(1L, now - last) * HeatMath.TICK_SECONDS);
        this.lastLook.put(pos.asLong(), now);
        boolean balanced = true;
        double exposed = body.extraArea();
        double ground = 0.0;
        for (final Direction face : Direction.values()) {
            final BlockPos beside = pos.relative(face);
            if (!level.isLoaded(beside)) {
                continue;
            }
            final IHeatPort port = HeatPorts.at(level, beside, face.getOpposite());
            if (port != null) {
                if (Double.isNaN(port.temperature())) {
                    continue;
                }
                if (Math.abs(port.temperature() - body.temperature()) > BALANCE) {
                    balanced = false;
                }
                // Each pair is moved once a turn: by the lower of the two, or by this one while the other sleeps.
                if (pos.asLong() < beside.asLong() || !this.active.contains(beside.asLong())) {
                    final double flow = HeatMath.exchange(body.temperature(), body.capacity(), port.temperature(),
                            port.capacity(), HeatMath.conductance(body.conductivity(), port.conductivity()), seconds);
                    if (flow != 0.0) {
                        body.exchange(-flow);
                        port.exchange(flow);
                    }
                }
                continue;
            }
            final BlockState state = level.getBlockState(beside);
            if (state.isAir() || !state.getFluidState().isEmpty() || !state.isSolid()) {
                exposed += 1.0;
            } else {
                ground += HeatMath.conductance(body.conductivity(), GROUND);
            }
        }
        final double pressure = HeatClimate.pressure(level, pos);
        final double watts = HeatMath.convection(body.temperature(), ambient, pressure, exposed)
                + HeatMath.radiation(body.temperature(), ambient, body.emissivity(), exposed)
                + ground * (body.temperature() - ambient);
        final double lost = HeatMath.loss(body.temperature(), body.capacity(), ambient, watts, seconds);
        if (lost != 0.0) {
            body.exchange(-lost);
        }
        final boolean losing = exposed > 0.0 || ground > 0.0;
        return !balanced || losing && Math.abs(body.temperature() - ambient) > BALANCE;
    }
}
