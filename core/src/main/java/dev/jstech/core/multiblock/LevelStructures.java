/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.registry.CoreAttachments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A dimension's structures, by the places they watch: each terminal watches a box round itself (the farthest its
 * shape reaches, or its formed structure and the places beside it), filed by the chunks the box touches, so a block
 * that changes anywhere finds the few structures it concerns at once and tells them to check again. Never saved: each
 * terminal files itself again as it loads.
 */
public final class LevelStructures {

    private final Map<Long, List<Watch>> byChunk = new HashMap<>();
    private final Map<BlockPos, Watch> byTerminal = new HashMap<>();

    /** The structures of {@code level}. */
    public static LevelStructures of(final ServerLevel level) {
        return level.getData(CoreAttachments.STRUCTURES);
    }

    /** Hands the game's bus what this answers, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(BlockEvent.NeighborNotifyEvent.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                of(level).changed(event.getPos());
            }
        });
    }

    /**
     * Files the terminal at {@code terminal} as watching the box from {@code min} to {@code max}, both counted
     * in, in place of what it watched before.
     */
    public void watch(final BlockPos terminal, final BlockPos min, final BlockPos max,
                      final IStructureWatcher watcher) {
        forget(terminal);
        final Watch watch = new Watch(terminal.immutable(), min.immutable(), max.immutable(), watcher);
        this.byTerminal.put(watch.terminal(), watch);
        for (int cx = min.getX() >> 4; cx <= max.getX() >> 4; cx++) {
            for (int cz = min.getZ() >> 4; cz <= max.getZ() >> 4; cz++) {
                this.byChunk.computeIfAbsent(ChunkPos.asLong(cx, cz), key -> new ArrayList<>(1)).add(watch);
            }
        }
    }

    /** Stops watching for the terminal at {@code terminal}: broken, or its chunk gone. */
    public void forget(final BlockPos terminal) {
        final Watch gone = this.byTerminal.remove(terminal);
        if (gone == null) {
            return;
        }
        for (int cx = gone.min().getX() >> 4; cx <= gone.max().getX() >> 4; cx++) {
            for (int cz = gone.min().getZ() >> 4; cz <= gone.max().getZ() >> 4; cz++) {
                final long key = ChunkPos.asLong(cx, cz);
                final List<Watch> watches = this.byChunk.get(key);
                if (watches != null) {
                    watches.remove(gone);
                    if (watches.isEmpty()) {
                        this.byChunk.remove(key);
                    }
                }
            }
        }
    }

    /** The block at {@code pos} changed: every structure watching it checks itself again. */
    public void changed(final BlockPos pos) {
        final List<Watch> watches = this.byChunk.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        if (watches == null) {
            return;
        }
        for (final Watch watch : List.copyOf(watches)) {
            if (watch.contains(pos)) {
                watch.watcher().changed(pos);
            }
        }
    }

    /** The terminal of the formed structure made of the place {@code pos}, or none. */
    @Nullable
    public BlockPos terminalHolding(final BlockPos pos) {
        final List<Watch> watches = this.byChunk.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        if (watches == null) {
            return null;
        }
        final long packed = PatternMatcher.encodePosition(pos.getX(), pos.getY(), pos.getZ());
        for (final Watch watch : watches) {
            if (watch.contains(pos) && watch.watcher().holds(packed)) {
                return watch.terminal();
            }
        }
        return null;
    }

    /** How many terminals watch, for the diagnostics. */
    public int watching() {
        return this.byTerminal.size();
    }

    /** What one terminal watches. */
    private record Watch(BlockPos terminal, BlockPos min, BlockPos max, IStructureWatcher watcher) {

        boolean contains(final BlockPos pos) {
            return pos.getX() >= this.min.getX() && pos.getX() <= this.max.getX() && pos.getY() >= this.min.getY()
                    && pos.getY() <= this.max.getY() && pos.getZ() >= this.min.getZ()
                    && pos.getZ() <= this.max.getZ();
        }
    }
}
