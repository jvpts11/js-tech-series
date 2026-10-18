/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fire;

import dev.jstech.core.JsCore;
import dev.jstech.core.air.AirMix;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.dimension.Wind;
import dev.jstech.core.dimension.Winds;
import dev.jstech.core.room.SealedRooms;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Fire that spreads by what burns, by the air's oxygen and by the wind, and leaves what it burnt behind. The game's
 * own fire does the burning; the Core changes how fast it spreads toward each place (faster downwind and in air rich
 * in oxygen, slower upwind and in thin air, never with too little), how fast it eats what it burns, and what a block
 * becomes when it burns away: the data map {@code jscore:burns_into} of blocks
 * ({@code data/<namespace>/data_maps/block/burns_into.json}) says it, a log to charred wood, wool to ash, keeping the
 * properties the two share (a log's axis). Once a second the fires give off smoke, leap ahead of a strong wind and,
 * in air half or more oxygen, light everything round them; a mod watches them with {@link #watch}.
 *
 * <pre>{@code
 * { "values": { "#minecraft:logs_that_burn": "mymod:charred_log", "#minecraft:wool": "mymod:ash" } }
 * }</pre>
 */
public final class Fires {

    /** The smoke a fire gives off, which rises and drifts with the wind. */
    public static final ResourceLocation SMOKE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "smoke");
    /** What each block becomes when it burns away. */
    public static final DataMapType<Block, Block> BURNS_INTO = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "burns_into"), Registries.BLOCK,
            BuiltInRegistries.BLOCK.byNameCodec()).build();
    /* The fires of each level; touched on the server's thread only. */
    private static final Map<ResourceKey<Level>, LevelFires> LEVELS = new HashMap<>();
    /* The watchers, replaced whole on each one added, so the second's pass reads it without a lock. */
    private static volatile List<IFireWatcher> watchers = List.of();
    /* The air and the wind of the last fire asked about, read once for the dozens of places round it it spreads to. */
    @Nullable
    private static ResourceKey<Level> askedLevel;
    private static long askedPos = Long.MIN_VALUE;
    private static long askedTime = Long.MIN_VALUE;
    private static double askedOxygen = 1;
    private static Wind askedWind = Wind.STILL;

    private Fires() {
    }

    /** Hands the mod's bus and the game's what the fire kit answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        FireClasses.listen(modBus);
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(BURNS_INTO));
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                of(level).tick();
            }
        });
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                LEVELS.remove(level.dimension());
            }
        });
    }

    /** Has {@code watcher} told of each fire burning, once a second; from a mod's constructor or setup. */
    public static synchronized void watch(final IFireWatcher watcher) {
        final List<IFireWatcher> more = new ArrayList<>(watchers);
        more.add(Objects.requireNonNull(watcher, "watcher"));
        watchers = List.copyOf(more);
    }

    /** What the block {@code burnt} becomes when it burns away, its shared properties kept; none to leave nothing. */
    @Nullable
    public static BlockState remainsOf(final BlockState burnt) {
        final Block into = burnt.getBlockHolder().getData(BURNS_INTO);
        if (into == null) {
            return null;
        }
        BlockState rest = into.defaultBlockState();
        for (final Property<?> property : burnt.getProperties()) {
            if (rest.hasProperty(property)) {
                rest = copy(burnt, rest, property);
            }
        }
        return rest;
    }

    /** How many fires burn in {@code level} that the fire kit knows of. */
    public static int burning(final ServerLevel level) {
        return of(level).burning();
    }

    /**
     * How many times as fast as the game's own the fire at {@code fire} spreads to {@code target}: by the oxygen of
     * the air there and by the wind toward the target.
     */
    @ApiStatus.Internal
    public static double spread(final ServerLevel level, final BlockPos fire, final BlockPos target) {
        ask(level, fire);
        final double wind = FireBalance.wind() ? FireMath.wind(askedWind.x(), askedWind.z(),
                target.getX() - fire.getX(), target.getZ() - fire.getZ()) : 1;
        return askedOxygen * wind;
    }

    /** How many times as fast as the game's own a fire beside {@code pos} eats what burns there: by the oxygen. */
    @ApiStatus.Internal
    public static double burnRate(final ServerLevel level, final BlockPos pos) {
        ask(level, pos);
        return askedOxygen;
    }

    /** The block {@code was} at {@code pos} burnt away, into air or into the fire that took its place. */
    @ApiStatus.Internal
    public static void burned(final ServerLevel level, final BlockPos pos, final BlockState was,
                              final boolean becameFire) {
        if (FireBalance.remains()) {
            of(level).burned(pos, was, becameFire);
        }
    }

    /** A block of {@code level} changed, by any means: the game calls this for every change, so it only notes. */
    @ApiStatus.Internal
    public static void noteChange(final ServerLevel level, final BlockPos pos, final BlockState before,
                                  final BlockState after) {
        // A fire that ages is noted too: a fire burning since the world was loaded is known from its first change.
        if (before.getBlock() instanceof BaseFireBlock || after.getBlock() instanceof BaseFireBlock) {
            of(level).note(pos, before, after);
        }
    }

    static List<IFireWatcher> watchers() {
        return watchers;
    }

    static LevelFires of(final ServerLevel level) {
        return LEVELS.computeIfAbsent(level.dimension(), key -> new LevelFires(level));
    }

    /* Reads the air and the wind at the fire {@code pos}, once a tick for each fire. */
    private static void ask(final ServerLevel level, final BlockPos pos) {
        if (level.dimension() == askedLevel && askedPos == pos.asLong() && askedTime == level.getGameTime()) {
            return;
        }
        askedLevel = level.dimension();
        askedPos = pos.asLong();
        askedTime = level.getGameTime();
        final Climate climate = SealedRooms.climateAt(level, pos);
        askedOxygen = FireMath.oxygen(SealedRooms.oxygenAt(level, pos),
                SealedRooms.airOf(climate).share(AirMix.OXYGEN));
        askedWind = Winds.at(level, Vec3.atCenterOf(pos));
    }

    private static <T extends Comparable<T>> BlockState copy(final BlockState from, final BlockState to,
                                                             final Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }
}
