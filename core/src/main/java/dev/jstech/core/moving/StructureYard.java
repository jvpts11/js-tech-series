/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.JsCore;
import dev.jstech.core.region.Box;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.DimensionState;
import dev.jstech.core.uuid.StructureUuid;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The yard of a dimension, where moving structures keep their real blocks: which plot is whose, kept with the world;
 * the chunks of a structure's plot kept loaded and ticking while the structure is in the world, so its machines run
 * and its crops grow wherever it goes; and its plot cleared when it goes for good. The chunks of the yard are made
 * empty, with no land, no structure and no creature of the world ({@code ChunkStatusTasksMixin}), and nothing is
 * born in them ({@code NaturalSpawnerMixin}).
 */
public final class StructureYard {

    private static final DimensionState<YardBook> BOOK = CoreState.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "structure_yard"), YardBook.CODEC, YardBook.EMPTY)
            .dimension();
    /* The ticket that keeps a plot's chunks loaded; it is not saved, a structure takes it again as it loads. */
    private static final TicketType<ChunkPos> TICKET = TicketType.create("jscore_moving_structure",
            Comparator.comparingLong(ChunkPos::toLong));
    /* A ticket of this reach makes its chunk tick as the land round a player does: blocks, entities and all. */
    private static final int REACH = 2;
    /* Changing a block of a plot this way tells no neighbour, drops nothing and keeps the light right. */
    private static final int QUIETLY = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    /* How often the plots left to clear are looked at, in ticks. */
    private static final int CLEAR_EVERY = 10;
    /* How long the land a structure arrives at in another dimension stays loaded for it, in ticks, as a portal's. */
    private static final int LANDING_TICKS = 300;
    /*
     * The ticket that keeps the land a structure arrives at loaded a while, so the structure ticks there and holds its
     * plot; it goes by itself, and the structure with it when no player is near, as anything sent through a portal.
     */
    private static final TicketType<ChunkPos> LANDING = TicketType.create("jscore_moving_structure_landing",
            Comparator.comparingLong(ChunkPos::toLong), LANDING_TICKS);

    private StructureYard() {
    }

    /**
     * Registers the book of plots, and hands the game's bus the clearing of the plots left to clear, once, from the
     * Core's constructor.
     */
    public static void listen() {
        CoreStates.register(BOOK);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % CLEAR_EVERY == 0) {
                clearLeft(level);
            }
        });
    }

    /**
     * Gives the plot {@code plot} of {@code level}'s yard back once the blocks it holds in {@code bounds} are
     * cleared: now, when its chunks are loaded, or when they have loaded; nothing is read where they are not.
     */
    public static void clearAndGiveBack(final ServerLevel level, final int plot, final Box bounds) {
        if (loaded(level, bounds)) {
            clear(level, bounds);
            giveBack(level, plot);
            return;
        }
        BOOK.update(level, book -> book.leftToClear(plot, bounds));
        hold(level, bounds);
    }

    /**
     * Gives the next free plot of {@code level}'s yard to {@code structure}.
     *
     * @return the plot
     * @throws IllegalStateException when the yard has no plot left
     */
    public static int take(final ServerLevel level, final StructureUuid structure) {
        final YardBook book = BOOK.get(level);
        final int plot = book.nextPlot();
        BOOK.set(level, book.taken(structure));
        return plot;
    }

    /** Gives plot {@code plot} of {@code level}'s yard back, for the next structure. */
    public static void giveBack(final ServerLevel level, final int plot) {
        BOOK.update(level, book -> book.givenBack(plot));
    }

    /** The structure plot {@code plot} of {@code level}'s yard is for, if it is taken. */
    public static Optional<StructureUuid> owner(final ServerLevel level, final int plot) {
        return BOOK.get(level).owner(plot);
    }

    /** How many plots of {@code level}'s yard are taken. */
    public static int taken(final ServerLevel level) {
        return BOOK.get(level).owners().size();
    }

    /** The plot of {@code level}'s yard the next structure made there gets. */
    public static int nextPlot(final ServerLevel level) {
        return BOOK.get(level).nextPlot();
    }

    /** Keeps every chunk of the yard holding {@code bounds} loaded and ticking. */
    public static void hold(final ServerLevel level, final Box bounds) {
        forEachChunk(bounds, chunk -> level.getChunkSource().addRegionTicket(TICKET, chunk, REACH, chunk, true));
    }

    /** Lets go of the chunks of the yard holding {@code bounds}. */
    public static void release(final ServerLevel level, final Box bounds) {
        forEachChunk(bounds, chunk -> level.getChunkSource().removeRegionTicket(TICKET, chunk, REACH, chunk, true));
    }

    /** Keeps the world's chunk {@code chunk} loaded and ticking, for a structure that keeps its place loaded. */
    public static void holdChunk(final ServerLevel level, final ChunkPos chunk) {
        level.getChunkSource().addRegionTicket(TICKET, chunk, REACH, chunk, true);
    }

    /** Lets go of the world's chunk {@code chunk}. */
    public static void releaseChunk(final ServerLevel level, final ChunkPos chunk) {
        level.getChunkSource().removeRegionTicket(TICKET, chunk, REACH, chunk, true);
    }

    /**
     * Keeps the land at {@code chunk} loaded a while for a structure that has just arrived there, as a portal keeps
     * the land it sends to: the ticket goes by itself after a few seconds.
     */
    public static void holdLanding(final ServerLevel level, final ChunkPos chunk) {
        level.getChunkSource().addRegionTicket(LANDING, chunk, REACH, chunk);
    }

    /**
     * Whether every chunk of the yard holding {@code bounds} is loaded, and every chunk round them, so its blocks can
     * be read and laid: a block laid, a redstone wire say, reads the blocks beside it, which may be across a chunk's
     * edge. The plot's tickets load the chunks round it too.
     */
    public static boolean loaded(final ServerLevel level, final Box bounds) {
        for (int x = (bounds.minX() >> 4) - 1; x <= (bounds.maxX() >> 4) + 1; x++) {
            for (int z = (bounds.minZ() >> 4) - 1; z <= (bounds.maxZ() >> 4) + 1; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Takes every block of {@code bounds} out of the yard, dropping nothing and telling no neighbour. */
    public static void clear(final ServerLevel level, final Box bounds) {
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                    at.set(x, y, z);
                    if (!level.getBlockState(at).isAir()) {
                        // Taken from its chunk, not through the level, which would tell the comparators round it.
                        level.getChunkAt(at).removeBlockEntity(at);
                        level.setBlock(at, Blocks.AIR.defaultBlockState(), QUIETLY);
                    }
                }
            }
        }
    }

    /** Changes a block of the yard quietly: no neighbour told, nothing dropped, the light kept right. */
    public static void setQuietly(final ServerLevel level, final BlockPos at, final BlockState state) {
        level.setBlock(at, state, QUIETLY);
    }

    private static void forEachChunk(final Box bounds, final Consumer<ChunkPos> action) {
        for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) {
            for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
                action.accept(new ChunkPos(x, z));
            }
        }
    }

    /*
     * Clears the plots left to clear whose chunks have loaded, and gives them back; keeps the others' chunks asked
     * for, as the ticket is not saved and a plot left to clear when the world was saved is asked for again here.
     */
    private static void clearLeft(final ServerLevel level) {
        final YardBook book = BOOK.get(level);
        if (book.clearing().isEmpty()) {
            return;
        }
        for (final Map.Entry<Integer, Box> each : book.clearing().entrySet()) {
            final Box bounds = each.getValue();
            hold(level, bounds);
            if (loaded(level, bounds)) {
                clear(level, bounds);
                release(level, bounds);
                BOOK.update(level, now -> now.cleared(each.getKey()));
            }
        }
    }
}
