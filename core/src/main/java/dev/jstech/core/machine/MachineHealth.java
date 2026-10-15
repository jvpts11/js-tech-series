/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.blockentity.BlockEntityFields;
import dev.jstech.core.blockentity.IntField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * How whole a machine is. A machine does not wear: it is damaged only by overvoltage, an explosion, sabotage or
 * overheating, and then looks cracked and works worse until it is repaired, with a repair kit used on it for a few
 * seconds ({@link IRepairKit}). At no health at all it does not work.
 */
public final class MachineHealth {

    private final IntField health;

    /** A machine's whole health. */
    public static final int FULL = 100;
    /** How often the cracks of a damaged machine are sent again to the players near it, in ticks. */
    public static final int CRACKS_EVERY = 200;
    /* The cracks the game draws over a block, from the first stage to the last. */
    private static final int CRACK_STAGES = 10;

    private MachineHealth(final BlockEntityFields fields) {
        this.health = fields.integer("Health", FULL).save().toClient();
    }

    /** The health of a machine, kept in {@code fields}, saved with them and sent to the players who see it. */
    public static MachineHealth in(final BlockEntityFields fields) {
        return new MachineHealth(fields);
    }

    /** Its health, from none to {@link #FULL}. */
    public int health() {
        return this.health.get();
    }

    /** Whether it is damaged at all. */
    public boolean damaged() {
        return this.health.get() < FULL;
    }

    /** Whether it is so damaged that it does not work. */
    public boolean wrecked() {
        return this.health.get() <= 0;
    }

    /** Takes {@code amount} of its health; how much it took. */
    public int damage(final int amount) {
        final int before = this.health.get();
        this.health.set(Math.max(0, before - Math.max(0, amount)));
        return before - this.health.get();
    }

    /** Gives back {@code amount} of its health, up to whole; how much it gave back. */
    public int repair(final int amount) {
        final int before = this.health.get();
        this.health.set(Math.min(FULL, before + Math.max(0, amount)));
        return this.health.get() - before;
    }

    /**
     * How fast a machine this damaged works, as a share of its full speed, when its mod says nothing else: from whole
     * at full health down to half just before it stops, and nothing at none.
     */
    public double workShare() {
        final int now = this.health.get();
        return now <= 0 ? 0.0 : 0.5 + 0.5 * now / FULL;
    }

    /**
     * Shows the players near the machine at {@code pos} its cracks, as many as its damage; none once it is whole. The
     * game forgets cracks it is not told of again within twenty seconds, so a damaged machine tells it every
     * {@link #CRACKS_EVERY} ticks.
     */
    public void showCracks(final ServerLevel level, final BlockPos pos) {
        final int now = this.health.get();
        final int stage = now >= FULL ? -1 : Math.min(CRACK_STAGES - 1, (FULL - now) * CRACK_STAGES / FULL);
        level.destroyBlockProgress(cracksId(pos), pos, stage);
    }

    /* The breaker's id the cracks of the machine at {@code pos} go under: below every entity's, one for each place. */
    private static int cracksId(final BlockPos pos) {
        return Integer.MIN_VALUE + (int) (pos.asLong() & 0x3FFF_FFFFL);
    }
}
