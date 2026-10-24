/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.id.IStableName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * A place of a structure, with its rule: what fits there; the blocks there that are counted, under a name the
 * multiblock limits ("from one to four energy ports"); the number a block there gives the formed machine, as the
 * coils of a blast furnace give its highest temperature, gathered over every such place as the multiblock says (the
 * lowest, the highest, the sum, the mean); and the entries that may stand there in place of its own block. Pure.
 *
 * <p>An entry is one of the multiblock's own blocks through which something comes in or goes out: a power entry, a
 * coolant inlet. The multiblock says where entries may go by declaring them on those places, and the player puts each
 * where it suits; whichever entry stands at a place makes that place the port of its kind, and is counted under its
 * name, so the multiblock can say how many of each it takes.
 *
 * @param fits    what fits there, its own block and every entry declared on it
 * @param counted the blocks there that are counted, and under what name; none for a place that counts nothing
 * @param valued  the number a block there gives, and under what key; none for a place that gives nothing
 * @param entries the entries that may stand there, none for a place that takes no entry
 */
public record Place(IBlockMatcher fits, @Nullable Counted counted, @Nullable Valued valued, List<Entry> entries) {

    public Place {
        Objects.requireNonNull(fits, "fits");
        entries = List.copyOf(entries);
    }

    public Place(final IBlockMatcher fits, @Nullable final Counted counted, @Nullable final Valued valued) {
        this(fits, counted, valued, List.of());
    }

    /** A place where {@code fits} fits, counting and giving nothing. */
    public static Place of(final IBlockMatcher fits) {
        return new Place(fits, null, null);
    }

    /** The same, counting the blocks there that {@code which} names under {@code name}. */
    public Place counting(final String name, final IBlockMatcher which) {
        return new Place(this.fits, new Counted(name, which), this.valued, this.entries);
    }

    /**
     * The same, each block there giving the number {@code byBlock} names it by (a block id, or a tag as
     * {@code #namespace:path}) under {@code key}, gathered as {@code how} says.
     */
    public Place valued(final String key, final Map<String, Double> byBlock, final Gather how) {
        return new Place(this.fits, this.counted, new Valued(key, byBlock, how), this.entries);
    }

    /**
     * The same, taking the entry {@code which} in place of its own block: counted under {@code name}, and making the
     * place a port of {@code port} when it stands there (none for an entry that opens onto a line of a mod's own,
     * which reaches the machine by {@link MultiblockPorts#registerOther}).
     */
    public Place entry(final String name, final IBlockMatcher which, @Nullable final PortKind port) {
        final List<Entry> more = new ArrayList<>(this.entries);
        more.add(new Entry(name, which, port));
        return new Place(widened(this.fits, which), this.counted, this.valued, more);
    }

    /** The entry standing at {@code pos}, or none when the place holds its own block. */
    public @Nullable Entry entryAt(final IBlockProvider provider, final long pos, final String blockId) {
        for (final Entry entry : this.entries) {
            if (entry.which().matchesAt(provider, pos, blockId)) {
                return entry;
            }
        }
        return null;
    }

    /*
     * What fits a place once it takes an entry too: one block match naming both, which a data file can hold, when
     * both are block matches; otherwise either, made in code.
     */
    private static IBlockMatcher widened(final IBlockMatcher fits, final IBlockMatcher entry) {
        if (fits instanceof BlockMatch own && entry instanceof BlockMatch other) {
            final Set<String> blocks = new HashSet<>(own.blocks());
            blocks.addAll(other.blocks());
            final Set<String> tags = new HashSet<>(own.tags());
            tags.addAll(other.tags());
            return new BlockMatch(blocks.stream().sorted().toList(), tags.stream().sorted().toList());
        }
        return new IBlockMatcher() {
            @Override
            public boolean matches(final String blockId) {
                return fits.matches(blockId) || entry.matches(blockId);
            }

            @Override
            public boolean matchesAt(final IBlockProvider provider, final long encodedPos, final String blockId) {
                return fits.matchesAt(provider, encodedPos, blockId) || entry.matchesAt(provider, encodedPos, blockId);
            }
        };
    }

    /**
     * An entry a place takes.
     *
     * @param name  what its count is called, which the multiblock's limits name
     * @param which its block
     * @param port  what the place becomes a port of when it stands there; none for an entry onto a line of a mod's
     *              own
     */
    public record Entry(String name, IBlockMatcher which, @Nullable PortKind port) {

        public Entry {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(which, "which");
        }
    }

    /**
     * The blocks of a place that are counted.
     *
     * @param name  what the count is called, which the multiblock's limits name
     * @param which the blocks that are counted
     */
    public record Counted(String name, IBlockMatcher which) {

        public Counted {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(which, "which");
        }
    }

    /**
     * The number a block of a place gives.
     *
     * @param key     what the number is called
     * @param byBlock the number of each block, by its id or by a tag as {@code #namespace:path}
     * @param how     how the numbers of every such place are gathered into one
     */
    public record Valued(String key, Map<String, Double> byBlock, Gather how) {

        public Valued {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(how, "how");
            byBlock = Map.copyOf(byBlock);
            if (byBlock.isEmpty()) {
                throw new IllegalArgumentException("a valued place gives a number for at least one block");
            }
        }

        /** The number the block {@code blockId} at {@code pos} gives, or none for a block it names no number for. */
        public @Nullable Double of(final IBlockProvider provider, final long pos, final String blockId) {
            final Double own = this.byBlock.get(blockId);
            if (own != null) {
                return own;
            }
            for (final Map.Entry<String, Double> entry : this.byBlock.entrySet()) {
                if (entry.getKey().startsWith("#") && provider.hasTag(pos, entry.getKey().substring(1))) {
                    return entry.getValue();
                }
            }
            return null;
        }
    }

    /** How the numbers of a key are gathered into one. */
    public enum Gather implements IStableName {
        /** The lowest: a blast furnace is as hot as its coolest coil. */
        LEAST("least"),
        /** The highest. */
        MOST("most"),
        /** Their sum. */
        SUM("sum"),
        /** Their mean. */
        MEAN("mean");

        private final String serializedName;

        Gather(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }
}
