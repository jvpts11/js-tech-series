/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fire;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.dimension.Wind;
import dev.jstech.core.dimension.Winds;
import dev.jstech.core.effect.Effects;
import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.core.util.Loaded;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import java.util.function.LongConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/*
 * The fires of one level the fire kit knows of, by the section they burn in, so what is near a place is found among
 * a few; what each burnt block leaves when its fire goes out; and the fires that may light again. Once a second it
 * looks at a share of the fires: their smoke, the wind carrying them ahead, an inferno in air rich in oxygen, and
 * what the watchers do. Touched on the server's thread only.
 */
final class LevelFires {

    private final ServerLevel level;
    private final Long2ObjectOpenHashMap<LongOpenHashSet> bySection = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<BlockState> remains = new Long2ObjectOpenHashMap<>();
    private final LongArrayList due = new LongArrayList();
    private final Long2LongOpenHashMap rekindles = new Long2LongOpenHashMap();
    /* The fires to look at this second, taken once a second and looked at a share each tick. */
    private final LongArrayList thisSecond = new LongArrayList();
    private int burning;
    private int cursor;
    private int turn;
    private int smoked;

    /* How many fires are looked at a tick at most, and each how many gives off a puff of smoke, at most so many. */
    private static final int MOST_A_TICK = 16;
    private static final int SMOKE_EVERY = 4;
    private static final int MOST_SMOKE_A_SECOND = 24;
    /* How many places round it an inferno lights a second, and how far round. */
    private static final int INFERNO_LIGHTS = 4;
    private static final int INFERNO_REACH = 2;
    /* The fire spread of a block at which a wind of 20 m/s surely carries the fire to it. */
    private static final double SURE_SPREAD = 30;
    private static final double SURE_WIND = 20;
    private static final long TICKS_A_SECOND = 20;

    LevelFires(final ServerLevel level) {
        this.level = level;
    }

    /* A block changed: a fire lit or went out there; a fire going out over burnt wood leaves its remains. */
    void note(final BlockPos pos, final BlockState before, final BlockState after) {
        final boolean wasFire = before.getBlock() instanceof BaseFireBlock;
        final boolean isFire = after.getBlock() instanceof BaseFireBlock;
        if (isFire) {
            add(pos.asLong());
        } else if (wasFire) {
            remove(pos.asLong());
            if (this.remains.containsKey(pos.asLong())) {
                this.due.add(pos.asLong());
            }
        }
    }

    /* The block {@code was} at {@code pos} burnt: its remains now, or where its fire goes out. */
    void burned(final BlockPos pos, final BlockState was, final boolean becameFire) {
        final BlockState rest = Fires.remainsOf(was);
        if (rest == null) {
            return;
        }
        this.remains.put(pos.asLong(), rest);
        if (!becameFire) {
            this.due.add(pos.asLong());
        }
    }

    /* The fire at {@code pos} may light again {@code ticks} from now. */
    void rekindle(final BlockPos pos, final long ticks) {
        this.rekindles.put(pos.asLong(), this.level.getGameTime() + ticks);
    }

    /* How many fires it knows of. */
    int burning() {
        return this.burning;
    }

    /* Hands {@code each} the fires in the sections the box round {@code centre}, {@code reach} wide, touches. */
    void near(final BlockPos centre, final int reach, final LongConsumer each) {
        final int fromX = SectionPos.blockToSectionCoord(centre.getX() - reach);
        final int toX = SectionPos.blockToSectionCoord(centre.getX() + reach);
        final int fromY = SectionPos.blockToSectionCoord(centre.getY() - reach);
        final int toY = SectionPos.blockToSectionCoord(centre.getY() + reach);
        final int fromZ = SectionPos.blockToSectionCoord(centre.getZ() - reach);
        final int toZ = SectionPos.blockToSectionCoord(centre.getZ() + reach);
        for (int x = fromX; x <= toX; x++) {
            for (int y = fromY; y <= toY; y++) {
                for (int z = fromZ; z <= toZ; z++) {
                    final LongOpenHashSet fires = this.bySection.get(SectionPos.asLong(x, y, z));
                    if (fires != null) {
                        fires.forEach(each);
                    }
                }
            }
        }
    }

    /*
     * Each tick: the remains of fires gone out, the fires due to light again, and a share of the fires of this second,
     * so the cost of many fires is spread over the second instead of falling on one tick.
     */
    void tick() {
        leaveRemains();
        lightAgain();
        if (Math.floorMod(this.level.getGameTime(), TICKS_A_SECOND) == 0) {
            // More fires than a second's shares hold are looked at in turn: each second starts where the last left.
            final LongArrayList all = new LongArrayList(this.burning);
            this.bySection.values().forEach(all::addAll);
            final int start = all.isEmpty() ? 0 : Math.floorMod(this.turn + this.cursor, all.size());
            this.thisSecond.clear();
            this.thisSecond.addAll(all.subList(start, all.size()));
            this.thisSecond.addAll(all.subList(0, start));
            this.turn = start;
            this.cursor = 0;
            this.smoked = 0;
        }
        if (this.cursor < this.thisSecond.size()) {
            lookAtShare();
        }
    }

    private void leaveRemains() {
        for (int i = 0; i < this.due.size(); i++) {
            final long key = this.due.getLong(i);
            final BlockState rest = this.remains.remove(key);
            final BlockPos pos = BlockPos.of(key);
            // What could not stand there (ash where leaves were, with nothing under it) falls away and is gone.
            if (rest != null && FireBalance.remains() && Loaded.at(this.level, pos)
                    && this.level.getBlockState(pos).isAir() && rest.canSurvive(this.level, pos)) {
                this.level.setBlockAndUpdate(pos, rest);
            }
        }
        this.due.clear();
    }

    private void lightAgain() {
        if (this.rekindles.isEmpty()) {
            return;
        }
        final long now = this.level.getGameTime();
        final LongArrayList ready = new LongArrayList();
        this.rekindles.long2LongEntrySet().forEach(entry -> {
            if (entry.getLongValue() <= now) {
                ready.add(entry.getLongKey());
            }
        });
        for (int i = 0; i < ready.size(); i++) {
            this.rekindles.remove(ready.getLong(i));
            final BlockPos pos = BlockPos.of(ready.getLong(i));
            if (Loaded.at(this.level, pos) && this.level.getBlockState(pos).isAir() && feeds(pos) > 0) {
                this.level.setBlockAndUpdate(pos, BaseFireBlock.getState(this.level, pos));
            }
        }
    }

    /* This tick's share of the fires of the second: each one's smoke, the wind, an inferno, and the watchers. */
    private void lookAtShare() {
        final int share = (int) Math.min(MOST_A_TICK, Math.ceilDiv(this.thisSecond.size(), TICKS_A_SECOND));
        final int end = Math.min(this.thisSecond.size(), this.cursor + share);
        final List<IFireWatcher> watchers = Fires.watchers();
        final boolean spreads = this.level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK);
        for (int index = this.cursor; index < end; index++) {
            final BlockPos pos = BlockPos.of(this.thisSecond.getLong(index));
            if (!Loaded.at(this.level, pos) || !(this.level.getBlockState(pos).getBlock() instanceof BaseFireBlock)) {
                remove(pos.asLong());
                continue;
            }
            if (FireBalance.smoke() && index % SMOKE_EVERY == 0 && this.smoked < MOST_SMOKE_A_SECOND) {
                Effects.at(this.level, Fires.SMOKE, Vec3.atCenterOf(pos)).send();
                this.smoked++;
            }
            if (spreads) {
                if (FireBalance.wind()) {
                    leap(pos);
                }
                inferno(pos);
            }
            for (final IFireWatcher watcher : watchers) {
                watcher.burning(this.level, pos);
            }
        }
        this.cursor = end;
    }

    /* The wind carries the fire ahead of it, onto what burns a block or more downwind. */
    private void leap(final BlockPos pos) {
        final Wind wind = Winds.at(this.level, Vec3.atCenterOf(pos));
        final int leap = FireMath.leap(wind.speed());
        if (leap <= 0) {
            return;
        }
        final double unitX = wind.x() / wind.speed();
        final double unitZ = wind.z() / wind.speed();
        for (int k = leap; k >= 1; k--) {
            final BlockPos target = pos.offset((int) Math.round(unitX * k), 0, (int) Math.round(unitZ * k));
            if (target.equals(pos) || !Loaded.at(this.level, target) || !this.level.getBlockState(target).isAir()
                    || this.level.isRainingAt(target)) {
                continue;
            }
            final int spread = feeds(target);
            final double chance = Math.min(1, wind.speed() / SURE_WIND) * Math.min(1, spread / SURE_SPREAD);
            if (spread > 0 && this.level.random.nextDouble() < chance && light(pos, target)) {
                return;
            }
        }
    }

    /* In air half or more oxygen, outside a sealed room (which keeps its own), the fire lights what burns round it. */
    private void inferno(final BlockPos pos) {
        final Climate climate = SealedRooms.climateAt(this.level, pos);
        final AirMix air = SealedRooms.airOf(climate);
        if (air.share(AirMix.OXYGEN) < FireMath.INFERNO_SHARE
                || air.partial(AirMix.OXYGEN, climate.pressure()) < FireMath.OXYGEN_FIRE_NEEDS
                || SealedRooms.roomAt(this.level, pos).isPresent()) {
            return;
        }
        int lit = 0;
        for (final BlockPos target : BlockPos.betweenClosed(pos.offset(-INFERNO_REACH, -INFERNO_REACH,
                -INFERNO_REACH), pos.offset(INFERNO_REACH, INFERNO_REACH, INFERNO_REACH))) {
            if (lit >= INFERNO_LIGHTS) {
                return;
            }
            if (Loaded.at(this.level, target) && this.level.getBlockState(target).isAir() && feeds(target) > 0
                    && light(pos, target.immutable())) {
                lit++;
            }
        }
    }

    /* Lights {@code target}, if the world may set it alight from a fire at {@code from}. */
    private boolean light(final BlockPos from, final BlockPos target) {
        if (!Protection.may(Actor.world(from), ProtectedAction.BURN, this.level, target)) {
            return false;
        }
        return this.level.setBlockAndUpdate(target, BaseFireBlock.getState(this.level, target));
    }

    /* How readily the blocks round the empty place {@code pos} catch fire from it: the highest fire spread of them. */
    private int feeds(final BlockPos pos) {
        int most = 0;
        for (final Direction way : Direction.values()) {
            final BlockPos beside = pos.relative(way);
            if (Loaded.at(this.level, beside)) {
                most = Math.max(most, this.level.getBlockState(beside).getFireSpreadSpeed(this.level, beside,
                        way.getOpposite()));
            }
        }
        return most;
    }

    private void add(final long pos) {
        if (this.bySection.computeIfAbsent(SectionPos.blockToSection(pos), key -> new LongOpenHashSet()).add(pos)) {
            this.burning++;
        }
    }

    private void remove(final long pos) {
        final long section = SectionPos.blockToSection(pos);
        final LongOpenHashSet fires = this.bySection.get(section);
        if (fires != null && fires.remove(pos)) {
            this.burning--;
            if (fires.isEmpty()) {
                this.bySection.remove(section);
            }
        }
    }
}
