/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.dimension.Wind;
import dev.jstech.core.dimension.Winds;
import dev.jstech.core.heat.HeatClimate;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The sealed rooms kit at work in every dimension of the server: told of every block that changes, it works rooms out
 * again a few thousand blocks a tick, away from the rest of the tick's work; every second it runs their gas, their
 * living things and what their air does; every tick, the rush of air out through their holes.
 */
public final class Rooms {

    private static final Map<ServerLevel, LevelRooms> LEVELS = new WeakHashMap<>();
    /* How many blocks the kit reads a tick, in each dimension, working rooms out. */
    private static final int BUDGET = 8_192;
    private static final int TICKS_A_SECOND = 20;

    private Rooms() {
    }

    /** Hands the buses what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        Seals.listen(modBus);
        RoomLiving.listen();
        RoomViews.listen(modBus);
        // A sealed room keeps its warmth and its own pressure, and no wind blows in it.
        HeatClimate.source((level, pos) -> level instanceof ServerLevel server ? SealedRooms.roomBeside(server, pos)
                .map(room -> new HeatClimate.Air(GasMath.TEMPERED, room.pressure())).orElse(null) : null);
        Winds.source((level, at) -> level instanceof ServerLevel server
                && SealedRooms.roomAt(server, BlockPos.containing(at)).isPresent() ? Optional.of(Wind.STILL)
                : Optional.empty());
        NeoForge.EVENT_BUS.addListener(LevelEvent.Load.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                LEVELS.put(level, new LevelRooms(level));
            }
        });
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                LEVELS.remove(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(ChunkEvent.Load.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                final LevelRooms rooms = LEVELS.get(level);
                if (rooms != null) {
                    rooms.loaded(event.getChunk().getPos());
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                tick(level);
            }
        });
    }

    /**
     * Notes that the block at {@code pos} of {@code level} changed from {@code before} to {@code after}: the game calls
     * this for every block that changes, by any means (the Core's reach into {@code ServerLevel#onBlockStateChange}).
     */
    public static void noteChange(final ServerLevel level, final BlockPos pos, final BlockState before,
                                  final BlockState after) {
        final LevelRooms rooms = LEVELS.get(level);
        if (rooms != null && !rooms.changing()) {
            rooms.note(pos, before, after);
        }
    }

    /** The rooms kit of {@code level}. */
    static LevelRooms of(final ServerLevel level) {
        return LEVELS.computeIfAbsent(level, LevelRooms::new);
    }

    private static void tick(final ServerLevel level) {
        final LevelRooms rooms = LEVELS.get(level);
        if (rooms == null) {
            return;
        }
        rooms.work(BUDGET);
        RoomConsequences.tick(rooms);
        if (level.getGameTime() % TICKS_A_SECOND == 0) {
            rooms.second();
            RoomPhysics.second(rooms);
            RoomViews.second(rooms);
        }
    }
}
