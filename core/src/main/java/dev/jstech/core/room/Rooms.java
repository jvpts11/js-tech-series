/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.JsCore;
import dev.jstech.core.diagnostic.Section;
import dev.jstech.core.dimension.Wind;
import dev.jstech.core.dimension.Winds;
import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.moving.BlockMove;
import dev.jstech.core.moving.StructureBlocksMovedEvent;
import it.unimi.dsi.fastutil.longs.LongIterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
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

    /** The rooms of a level worked out, filled and leaking each tick, timed. */
    public static final Section SECTION = Section.builder(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "rooms"), CoreLogs.ROOMS).declare();

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
        NeoForge.EVENT_BUS.addListener(StructureBlocksMovedEvent.class, event -> carry(event.move()));
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

    /** How many sealed rooms the kit holds in {@code level}. */
    public static int count(final ServerLevel level) {
        final LevelRooms rooms = LEVELS.get(level);
        final RoomStore store = rooms == null ? null : rooms.store();
        return store == null ? 0 : store.rooms().size();
    }

    /*
     * Blocks moved together, as a moving structure is made, set down or taken to another dimension: each room their
     * blocks closed in leaves where they were at once, not left behind as a room of nothing for the next blocks there
     * to find; and, the blocks each moved as far as the others and not turned, its air goes with them, to where its
     * blocks of air are now, so a ship's cabin keeps its air in space.
     */
    private static void carry(final BlockMove move) {
        if (!(move.from() instanceof ServerLevel from) || !(move.to() instanceof ServerLevel to)
                || move.moved().isEmpty()) {
            return;
        }
        final LevelRooms rooms = LEVELS.get(from);
        final RoomStore store = rooms == null ? null : rooms.store();
        if (store == null || store.rooms().isEmpty()) {
            return;
        }
        BlockPos shift = null;
        boolean together = move.rotation() == Rotation.NONE;
        final BlockPos.MutableBlockPos low = new BlockPos.MutableBlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE);
        final BlockPos.MutableBlockPos high = new BlockPos.MutableBlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE,
                Integer.MIN_VALUE);
        for (final Map.Entry<BlockPos, BlockPos> each : move.moved().entrySet()) {
            final BlockPos by = each.getValue().subtract(each.getKey());
            if (shift == null) {
                shift = by;
            } else if (!shift.equals(by)) {
                together = false;
            }
            final BlockPos at = each.getKey();
            low.set(Math.min(low.getX(), at.getX()), Math.min(low.getY(), at.getY()),
                    Math.min(low.getZ(), at.getZ()));
            high.set(Math.max(high.getX(), at.getX()), Math.max(high.getY(), at.getY()),
                    Math.max(high.getZ(), at.getZ()));
        }
        final LevelRooms there = of(to);
        for (final Room room : List.copyOf(store.rooms())) {
            if (room.cells().isEmpty() || !within(room, low, high)) {
                continue;
            }
            if (together) {
                there.carry(BlockPos.of(room.cells().iterator().nextLong()).offset(shift).asLong(), room.gases());
            }
            rooms.drop(room);
        }
    }

    /* Whether every block of {@code room} lies in the box from {@code low} to {@code high}. */
    private static boolean within(final Room room, final BlockPos low, final BlockPos high) {
        final LongIterator each = room.cells().iterator();
        while (each.hasNext()) {
            final long cell = each.nextLong();
            final int x = BlockPos.getX(cell);
            final int y = BlockPos.getY(cell);
            final int z = BlockPos.getZ(cell);
            if (x < low.getX() || x > high.getX() || y < low.getY() || y > high.getY() || z < low.getZ()
                    || z > high.getZ()) {
                return false;
            }
        }
        return true;
    }

    private static void tick(final ServerLevel level) {
        final LevelRooms rooms = LEVELS.get(level);
        if (rooms != null) {
            SECTION.run(() -> work(level, rooms));
        }
    }

    private static void work(final ServerLevel level, final LevelRooms rooms) {
        rooms.work(BUDGET);
        RoomConsequences.tick(rooms);
        if (level.getGameTime() % TICKS_A_SECOND == 0) {
            rooms.second();
            RoomPhysics.second(rooms);
            RoomViews.second(rooms);
        }
    }
}
