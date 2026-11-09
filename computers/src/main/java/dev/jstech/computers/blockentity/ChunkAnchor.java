/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.hardware.AnchorCardSpec;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.core.region.ChunkLoaders;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

/**
 * What a Network Anchor Card does for the machine it sits in: while the machine runs, the chunks round it stay loaded
 * through J's Core's chunk loading, under the name of whoever works the machine and within their quota; when it stops,
 * loses its card or has nobody working it, they are let go (computers/docs/design/network.md).
 *
 * <p>It asks once a second and holds no record of its own: what is kept is read back from J's Core every time, so a
 * machine that comes back after a restart, switched off meanwhile, lets go of what it kept before.
 */
@TextHolder
public final class ChunkAnchor {

    private int checkIn;
    /* Whether it may still hold chunks: what it last kept, or a first look after the machine loaded. */
    private boolean mayHold = true;
    // Whether the last refusal was written down, so it is told once and not every second.
    private boolean told;

    /** Why the chunks are kept, as an operator who finds the world held open reads it beside the owner's name. */
    public static final TextKey REASON = TextKey.of("jsc.chunk_anchor.reason",
            "A computer's Network Anchor Card keeps the chunks round it loaded");
    private static final int CHECK_EVERY = 20;

    /**
     * How many chunks the anchor cards of {@code build} keep on each side of the machine's own: the farthest of them,
     * or -1 when it has none.
     */
    public static int reachOf(@Nullable final ComputerBuild build) {
        int reach = -1;
        if (build != null) {
            for (final IExpansionCardSpec card : build.cardsOfKind(ExpansionCardKind.ANCHOR)) {
                if (card instanceof AnchorCardSpec anchor) {
                    reach = Math.max(reach, anchor.reach());
                }
            }
        }
        return reach;
    }

    /** Lets go of every chunk the machine at {@code source} kept, as a machine taken down does. */
    public static void letGo(final ServerLevel level, final BlockPos source) {
        ChunkLoaders.releaseAll(level, source);
    }

    /**
     * Once a second, keeps the square {@code reach} chunks on each side of {@code source}'s chunk loaded for
     * {@code owner}, nearest first, or lets go of it all when {@code reach} is negative or nobody works the machine.
     *
     * @return the chunks it keeps now and how many it wants, while the owner's quota or the server's budget refuses
     *         it some and that has not been told yet ({@link #told()}); null otherwise
     */
    @Nullable
    public Refusal tick(final ServerLevel level, final BlockPos source, @Nullable final UUID owner, final int reach) {
        if (--checkIn > 0) {
            return null;
        }
        checkIn = CHECK_EVERY;
        if (reach < 0 || owner == null) {
            if (mayHold) {
                ChunkLoaders.releaseAll(level, source);
                mayHold = false;
            }
            told = false;
            return null;
        }
        mayHold = true;
        final List<ChunkPos> wanted = square(new ChunkPos(source), reach);
        final Set<ChunkPos> held = ChunkLoaders.loadedFrom(level, source);
        final Set<ChunkPos> covered = new LinkedHashSet<>(wanted);
        for (final ChunkPos chunk : held) {
            if (!covered.contains(chunk)) {
                ChunkLoaders.release(level, source, chunk);
            }
        }
        boolean full = false;
        for (final ChunkPos chunk : wanted) {
            if (held.contains(chunk)) {
                continue;
            }
            final ChunkLoaders.LoadOutcome outcome = ChunkLoaders.load(level, source, owner, REASON, chunk, true);
            if (outcome == ChunkLoaders.LoadOutcome.LIMIT_REACHED
                    || outcome == ChunkLoaders.LoadOutcome.SERVER_FULL) {
                full = true;
                break;
            }
        }
        if (!full) {
            told = false;
            return null;
        }
        return told ? null : new Refusal(ChunkLoaders.loadedFrom(level, source).size(), wanted.size());
    }

    /**
     * The refusal {@link #tick} returned is written down: it is not returned again until the quota has had room once
     * more. A machine off any network, whose log there is none to write in, does not call this, and is told again.
     */
    public void told() {
        told = true;
    }

    /* The chunks {@code reach} on each side of {@code centre}, the centre first and then ring by ring outwards. */
    private static List<ChunkPos> square(final ChunkPos centre, final int reach) {
        final List<ChunkPos> out = new ArrayList<>();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                out.add(new ChunkPos(centre.x + dx, centre.z + dz));
            }
        }
        out.sort(Comparator.comparingInt(chunk -> Math.max(Math.abs(chunk.x - centre.x),
                Math.abs(chunk.z - centre.z))));
        return out;
    }

    /**
     * A refusal: the owner keeps as many chunks loaded as they may, or the server as many as it allows.
     *
     * @param kept   the chunks the card keeps now
     * @param wanted the chunks it would keep
     */
    public record Refusal(int kept, int wanted) {
    }
}
