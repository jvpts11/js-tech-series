/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.DimensionState;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The time of each dimension that keeps a clock of its own ({@link DimensionClock}): moved a tick at a time, as fast
 * as its day says, while the game's daylight cycle runs, and kept with the world. The game moves only the overworld's
 * clock; every other dimension reads it, so a dimension of its own day is moved here, and the overworld's is never
 * touched. A dimension given a clock in a world already played starts from the overworld's time of day.
 */
public final class DayClocks {

    private static final DimensionState<Time> TIME = CoreState.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "day_clock"), Time.CODEC, Time.UNSET).dimension();

    private DayClocks() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor. */
    public static void listen() {
        CoreStates.register(TIME);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, DayClocks::onLevelTick);
    }

    /** The clock {@code level} keeps of its own, if it keeps one; never the overworld's. */
    public static Optional<DimensionClock> of(final Level level) {
        return level.dimension() == Level.OVERWORLD ? Optional.empty() : DimensionRulesData.of(level).clock();
    }

    /** Whether {@code level} keeps a clock of its own, which the Core moves. */
    public static boolean keepsOwn(final ServerLevel level) {
        return level.getLevelData() instanceof RuledLevelData ruled && ruled.clock().isPresent();
    }

    /** Moves {@code level}'s own clock one tick, as fast as its day says; nothing for one without a clock. */
    public static void advance(final ServerLevel level) {
        if (!(level.getLevelData() instanceof RuledLevelData ruled)) {
            return;
        }
        final Optional<DimensionClock> clock = ruled.clock();
        if (clock.isEmpty() || clock.get().locked()) {
            return;
        }
        final float step = ruled.getDayTimeFraction() + clock.get().perTick();
        final long whole = (long) step;
        ruled.restore(ruled.ownTime() + whole, step - whole);
        TIME.set(level, new Time(ruled.ownTime(), ruled.getDayTimeFraction()));
    }

    public static void onLevelTick(final LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() != Level.OVERWORLD
                && level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            advance(level);
        }
    }

    /**
     * Gives {@code level}'s own clock back the time it had when the world was saved, or the overworld's time of day for
     * one that never kept its own; called as the level loads, once its level data is the Core's.
     */
    static void restore(final ServerLevel level, final RuledLevelData ruled) {
        final Time saved = TIME.get(level);
        if (saved.time() >= 0) {
            ruled.restore(saved.time(), saved.fraction());
        } else {
            ruled.restore(level.getServer().overworld().getDayTime(), 0F);
        }
    }

    /**
     * What a dimension's own clock shows.
     *
     * @param time     its time, counted as the game's clock is; below 0 before it was ever kept
     * @param fraction the part of a tick of its clock it has moved besides
     */
    record Time(long time, float fraction) {

        static final Time UNSET = new Time(-1L, 0F);
        static final Codec<Time> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("time").forGetter(Time::time),
                Codec.FLOAT.optionalFieldOf("fraction", 0F).forGetter(Time::fraction))
                .apply(instance, Time::new));
    }
}
