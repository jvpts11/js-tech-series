/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.region;

import dev.jstech.core.JsCore;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;

/**
 * Chunks kept loaded for somebody: a machine keeps the chunks it works in loaded while nobody is near, under the name
 * of the player or the team that owns it, and each owner may keep only so many loaded across the whole world. A
 * source is the block that asked; breaking it should release what it holds, and a source that no longer holds
 * anything when the world loads again has its chunks let go of.
 */
public final class ChunkLoaders {

    /** What the loaded chunks are kept under in the game's own forced-chunk file. */
    public static final TicketController CONTROLLER = new TicketController(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "owned"), ChunkLoaders::validate);
    private static final String FILE = "jstech_chunk_loaders";

    private ChunkLoaders() {
    }

    /** What asking to load a chunk came to. */
    public enum LoadOutcome {
        /** The chunk is loaded now. */
        LOADED,
        /** The source was keeping it loaded already. */
        ALREADY,
        /**
         * The owner keeps as many chunks loaded as they may; nothing new is loaded. A source that changed owner or
         * ticking with this request has already let go of its old chunks, as it starts again under its new terms.
         */
        LIMIT_REACHED
    }

    /** Hands the game the controller, on the mod's event bus. */
    public static void onRegisterControllers(final RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    /**
     * Keeps {@code chunk} of {@code level} loaded for {@code owner}, on behalf of the block at {@code source}; with
     * {@code ticking} its block entities and entities go on working as if a player were near.
     */
    public static LoadOutcome load(final ServerLevel level, final BlockPos source, final UUID owner,
                                   final ChunkPos chunk, final boolean ticking) {
        final Ledger ledger = ledger(level);
        Source held = ledger.sources.get(source);
        // A source handed to another owner, or switched between ticking and not, starts again under its new terms.
        if (held != null && (!held.owner.equals(owner) || held.ticking != ticking)) {
            releaseAll(level, source);
            held = null;
        }
        if (held != null && held.chunks.contains(chunk.toLong())) {
            return LoadOutcome.ALREADY;
        }
        if (loadedBy(level.getServer(), owner) >= ChunkLoadingBalance.limit()) {
            return LoadOutcome.LIMIT_REACHED;
        }
        final Source kept = held != null ? held : new Source(owner, ticking, new LongLinkedOpenHashSet());
        kept.chunks.add(chunk.toLong());
        ledger.sources.put(source.immutable(), kept);
        ledger.setDirty();
        CONTROLLER.forceChunk(level, source, chunk.x, chunk.z, true, ticking);
        return LoadOutcome.LOADED;
    }

    /** Lets go of one chunk the source kept loaded. */
    public static void release(final ServerLevel level, final BlockPos source, final ChunkPos chunk) {
        final Ledger ledger = ledger(level);
        final Source held = ledger.sources.get(source);
        if (held == null || !held.chunks.remove(chunk.toLong())) {
            return;
        }
        CONTROLLER.forceChunk(level, source, chunk.x, chunk.z, false, held.ticking);
        if (held.chunks.isEmpty()) {
            ledger.sources.remove(source);
        }
        ledger.setDirty();
    }

    /** Lets go of every chunk the source kept loaded, as a broken machine does. */
    public static void releaseAll(final ServerLevel level, final BlockPos source) {
        final Ledger ledger = ledger(level);
        final Source held = ledger.sources.remove(source);
        if (held == null) {
            return;
        }
        for (final long chunk : held.chunks) {
            CONTROLLER.forceChunk(level, source, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, held.ticking);
        }
        ledger.setDirty();
    }

    /**
     * Lets go of every chunk {@code owner} keeps loaded, in every dimension: what an operator does for a player who
     * left machines holding the world open.
     *
     * @return how many chunks were let go of
     */
    public static int releaseOwnedBy(final MinecraftServer server, final UUID owner) {
        int released = 0;
        for (final ServerLevel level : server.getAllLevels()) {
            for (final Map.Entry<BlockPos, Source> source : List.copyOf(ledger(level).sources.entrySet())) {
                if (source.getValue().owner().equals(owner)) {
                    released += source.getValue().chunks().size();
                    releaseAll(level, source.getKey());
                }
            }
        }
        return released;
    }

    /** How many chunks {@code owner} keeps loaded, across every dimension. */
    public static int loadedBy(final MinecraftServer server, final UUID owner) {
        int count = 0;
        for (final ServerLevel level : server.getAllLevels()) {
            for (final Source source : ledger(level).sources.values()) {
                if (source.owner.equals(owner)) {
                    count += source.chunks.size();
                }
            }
        }
        return count;
    }

    /** The chunks the block at {@code source} keeps loaded. */
    public static Set<ChunkPos> loadedFrom(final ServerLevel level, final BlockPos source) {
        final Source held = ledger(level).sources.get(source);
        final Set<ChunkPos> out = new LinkedHashSet<>();
        if (held != null) {
            for (final long chunk : held.chunks) {
                out.add(new ChunkPos(chunk));
            }
        }
        return out;
    }

    /** Every source of {@code level} keeping chunks loaded, with its owner. */
    public static Map<BlockPos, UUID> sources(final ServerLevel level) {
        final Map<BlockPos, UUID> out = new LinkedHashMap<>();
        ledger(level).sources.forEach((pos, source) -> out.put(pos, source.owner));
        return out;
    }

    /* When the world loads, a ticket whose source the ledger no longer knows is let go of. */
    private static void validate(final ServerLevel level, final TicketHelper tickets) {
        final Ledger ledger = ledger(level);
        for (final BlockPos source : Set.copyOf(tickets.getBlockTickets().keySet())) {
            if (!ledger.sources.containsKey(source)) {
                tickets.removeAllTickets(source);
            }
        }
    }

    private static Ledger ledger(final ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Ledger::new, Ledger::load), FILE);
    }

    /** One source's chunks and whose they are. */
    private record Source(UUID owner, boolean ticking, LongSet chunks) {
    }

    /** What every source of one dimension keeps loaded, as its saved data. */
    private static final class Ledger extends SavedData {

        private final Map<BlockPos, Source> sources = new LinkedHashMap<>();

        static Ledger load(final CompoundTag tag, final HolderLookup.Provider registries) {
            final Ledger ledger = new Ledger();
            for (final Tag one : tag.getList("sources", Tag.TAG_COMPOUND)) {
                final CompoundTag source = (CompoundTag) one;
                final LongSet chunks = new LongLinkedOpenHashSet(source.getLongArray("chunks"));
                NbtUtils.readBlockPos(source, "pos").ifPresent(pos -> ledger.sources.put(pos,
                        new Source(source.getUUID("owner"), source.getBoolean("ticking"), chunks)));
            }
            return ledger;
        }

        @Override
        public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
            final ListTag list = new ListTag();
            sources.forEach((pos, source) -> {
                final CompoundTag one = new CompoundTag();
                one.put("pos", NbtUtils.writeBlockPos(pos));
                one.putUUID("owner", source.owner());
                one.putBoolean("ticking", source.ticking());
                one.put("chunks", new LongArrayTag(source.chunks().toLongArray()));
                list.add(one);
            });
            tag.put("sources", list);
            return tag;
        }
    }
}
