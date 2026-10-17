/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.WorldData;

/**
 * What a dimension other than the overworld reads its weather and its time from: the overworld's, as the game's own
 * dimensions do, unless the dimension's rules hold its weather clear, raining or storming, or give it a clock of its
 * own, whose time it then keeps here ({@link DayClocks} moves and saves it). Asked each tick, so a rule changed by a
 * reload takes hold at once.
 */
final class RuledLevelData extends DerivedLevelData {

    private final ResourceKey<Level> dimension;
    private long ownTime;
    private float ownFraction;

    RuledLevelData(final WorldData world, final ResourceKey<Level> dimension) {
        super(world, world.overworldData());
        this.dimension = dimension;
    }

    @Override
    public boolean isRaining() {
        return switch (DimensionRulesData.of(dimension).weather()) {
            case NATURAL -> super.isRaining();
            case CLEAR -> false;
            case RAIN, THUNDER -> true;
        };
    }

    @Override
    public boolean isThundering() {
        return switch (DimensionRulesData.of(dimension).weather()) {
            case NATURAL -> super.isThundering();
            case CLEAR, RAIN -> false;
            case THUNDER -> true;
        };
    }

    /* Its own time when it keeps a clock, held at the hour of its sun when it is locked to its star. */
    @Override
    public long getDayTime() {
        final Optional<DimensionClock> clock = clock();
        if (clock.isEmpty()) {
            return super.getDayTime();
        }
        return clock.get().locked() ? clock.get().lockedAt() : this.ownTime;
    }

    @Override
    public void setDayTime(final long time) {
        if (clock().isPresent()) {
            this.ownTime = time;
        } else {
            super.setDayTime(time);
        }
    }

    @Override
    public float getDayTimeFraction() {
        return clock().isPresent() ? this.ownFraction : super.getDayTimeFraction();
    }

    @Override
    public void setDayTimeFraction(final float fraction) {
        if (clock().isPresent()) {
            this.ownFraction = fraction;
        } else {
            super.setDayTimeFraction(fraction);
        }
    }

    /* How far its clock moves a tick: what the game sends the players with its time, so their sun moves as fast. */
    @Override
    public float getDayTimePerTick() {
        final Optional<DimensionClock> clock = clock();
        return clock.isPresent() ? clock.get().perTick() : super.getDayTimePerTick();
    }

    /* A clock of its own is set by its rules alone; the overworld's speed is the overworld's to set. */
    @Override
    public void setDayTimePerTick(final float perTick) {
        if (clock().isEmpty()) {
            super.setDayTimePerTick(perTick);
        }
    }

    /** Its clock, if its rules give it one. */
    Optional<DimensionClock> clock() {
        return DimensionRulesData.of(this.dimension).clock();
    }

    /** The time its own clock shows, before a locked world holds it still. */
    long ownTime() {
        return this.ownTime;
    }

    /** Takes back the time its own clock had when the world was saved. */
    void restore(final long time, final float fraction) {
        this.ownTime = time;
        this.ownFraction = fraction;
    }
}
