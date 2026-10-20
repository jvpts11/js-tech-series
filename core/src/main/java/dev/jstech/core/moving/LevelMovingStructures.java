/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.registry.CoreAttachments;
import dev.jstech.core.uuid.StructureUuid;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The moving structures of one level loaded now, on the server and on a player's game alike: found by their id, by
 * the plot of the yard their blocks are in, and by the columns of the world they fill. The game asks this of nearly
 * every entity that moves and every ray it casts, so a level with no structure answers at once, and one with some
 * looks only in the chunk columns asked about.
 */
public final class LevelMovingStructures {

    private final Map<StructureUuid, IStructureBody> byId = new HashMap<>();
    private final Int2ObjectMap<IStructureBody> byPlot = new Int2ObjectOpenHashMap<>();
    private final Long2ObjectMap<List<IStructureBody>> byColumn = new Long2ObjectOpenHashMap<>();
    /* The chunk columns each structure is filed under: min x, min z, max x, max z. */
    private final Map<StructureUuid, int[]> filed = new HashMap<>();

    /* How many structures are loaded in every level of this game together, so a game with none asks nothing more. */
    private static final AtomicInteger LOADED = new AtomicInteger();

    /** The structures of {@code level}. */
    public static LevelMovingStructures of(final Level level) {
        return level.getData(CoreAttachments.MOVING_STRUCTURES.get());
    }

    /**
     * Whether any structure is loaded in any level of this game: asked before anything else on the paths the game
     * takes for every entity and every ray, so a game with no moving structure pays nothing on them.
     */
    public static boolean anyLoaded() {
        return LOADED.get() > 0;
    }

    /** Whether no structure is loaded in this level: the question asked first, everywhere. */
    public boolean isEmpty() {
        return byId.isEmpty();
    }

    /** Files {@code body}, in place of any filed under its id before. */
    public void add(final IStructureBody body) {
        remove(body.id());
        byId.put(body.id(), body);
        byPlot.put(body.plot(), body);
        file(body);
        LOADED.incrementAndGet();
    }

    /** Takes out the structure filed under {@code id}; nothing filed changes nothing. */
    public void remove(final StructureUuid id) {
        final IStructureBody body = byId.remove(id);
        if (body == null) {
            return;
        }
        LOADED.decrementAndGet();
        if (byPlot.get(body.plot()) == body) {
            byPlot.remove(body.plot());
        }
        unfile(id, body);
    }

    /** Files {@code body} again under the columns it fills now, after it moved. */
    public void moved(final IStructureBody body) {
        if (byId.get(body.id()) != body) {
            return;
        }
        final int[] now = columns(body.worldBox());
        final int[] before = filed.get(body.id());
        if (before != null && before[0] == now[0] && before[1] == now[1] && before[2] == now[2]
                && before[3] == now[3]) {
            return;
        }
        unfile(body.id(), body);
        file(body);
    }

    /** The structure filed under {@code id}, or null when none is loaded. */
    @Nullable
    public IStructureBody byId(final StructureUuid id) {
        return byId.get(id);
    }

    /** The structure whose blocks are in plot {@code plot} of the yard, or null when none is loaded. */
    @Nullable
    public IStructureBody byPlot(final int plot) {
        return plot < 0 ? null : byPlot.get(plot);
    }

    /** The structure whose blocks hold the yard's place at {@code (x, z)}, or null for a place no loaded one holds. */
    @Nullable
    public IStructureBody atYard(final int x, final int z) {
        return YardLayout.isYardBlock(x) ? byPlot(YardLayout.plotOfBlock(x, z)) : null;
    }

    /**
     * The loaded structures whose blocks may fill some of the world's box {@code box}: those filed under its
     * columns whose box meets it. An empty list, made once, when there are none.
     */
    public List<IStructureBody> near(final AABB box) {
        if (byId.isEmpty()) {
            return List.of();
        }
        final int minX = ((int) Math.floor(box.minX)) >> 4;
        final int minZ = ((int) Math.floor(box.minZ)) >> 4;
        final int maxX = ((int) Math.floor(box.maxX)) >> 4;
        final int maxZ = ((int) Math.floor(box.maxZ)) >> 4;
        List<IStructureBody> found = null;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                final List<IStructureBody> here = byColumn.get(ChunkPos.asLong(x, z));
                if (here == null) {
                    continue;
                }
                for (final IStructureBody body : here) {
                    if (body.worldBox().intersects(box) && (found == null || !found.contains(body))) {
                        if (found == null) {
                            found = new ArrayList<>(2);
                        }
                        found.add(body);
                    }
                }
            }
        }
        return found == null ? List.of() : found;
    }

    /** Every loaded structure. */
    public Collection<IStructureBody> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    private void file(final IStructureBody body) {
        final int[] columns = columns(body.worldBox());
        filed.put(body.id(), columns);
        for (int x = columns[0]; x <= columns[2]; x++) {
            for (int z = columns[1]; z <= columns[3]; z++) {
                byColumn.computeIfAbsent(ChunkPos.asLong(x, z), any -> new ArrayList<>(1)).add(body);
            }
        }
    }

    private void unfile(final StructureUuid id, final IStructureBody body) {
        final int[] columns = filed.remove(id);
        if (columns == null) {
            return;
        }
        for (int x = columns[0]; x <= columns[2]; x++) {
            for (int z = columns[1]; z <= columns[3]; z++) {
                final long key = ChunkPos.asLong(x, z);
                final List<IStructureBody> here = byColumn.get(key);
                if (here != null) {
                    here.remove(body);
                    if (here.isEmpty()) {
                        byColumn.remove(key);
                    }
                }
            }
        }
    }

    /* The chunk columns a world box covers, with a block round it so what touches its edge is found. */
    private static int[] columns(final AABB box) {
        return new int[] {((int) Math.floor(box.minX - 1)) >> 4, ((int) Math.floor(box.minZ - 1)) >> 4,
                ((int) Math.floor(box.maxX + 1)) >> 4, ((int) Math.floor(box.maxZ + 1)) >> 4};
    }
}
