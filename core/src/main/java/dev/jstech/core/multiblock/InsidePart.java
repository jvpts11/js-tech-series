/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A kind of block that may stand inside a structure and give it something, with the rules of its neighbours, as the
 * cells, moderators and coolers of a reactor: what each one gives when it works ("each cell gives 10 heat"), what it
 * gives more for each neighbour of a kind ("and 5 more beside each moderator"), and what it needs beside it to work at
 * all ("a cooler works only beside a working cell"). Pure.
 *
 * @param name    what the part is called; its count is how many work
 * @param which   the blocks that are this part
 * @param gives   what each working block gives, by key, summed over every one
 * @param bonuses what each working block gives more for each neighbour of a kind
 * @param needs   what a block needs beside it to work; a part with no needs always works
 */
public record InsidePart(String name, IBlockMatcher which, Map<String, Double> gives, List<Bonus> bonuses,
                         List<Need> needs) {

    public InsidePart {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(which, "which");
        gives = Map.copyOf(gives);
        bonuses = List.copyOf(bonuses);
        needs = List.copyOf(needs);
    }

    /** The part {@code name}: the blocks {@code which} names, giving nothing and needing nothing yet. */
    public static InsidePart of(final String name, final IBlockMatcher which) {
        return new InsidePart(name, which, Map.of(), List.of(), List.of());
    }

    /** The same, each working block giving {@code each} to {@code key}. */
    public InsidePart gives(final String key, final double each) {
        final Map<String, Double> more = new LinkedHashMap<>(this.gives);
        more.put(key, each);
        return new InsidePart(this.name, this.which, more, this.bonuses, this.needs);
    }

    /** The same, each working block giving {@code each} more to {@code key} for each neighbour {@code of}. */
    public InsidePart bonus(final String key, final Neighbour of, final double each) {
        final List<Bonus> more = new ArrayList<>(this.bonuses);
        more.add(new Bonus(key, of, each));
        return new InsidePart(this.name, this.which, this.gives, more, this.needs);
    }

    /** The same, a block working only with at least {@code least} neighbours {@code of}. */
    public InsidePart needs(final Neighbour of, final int least) {
        final List<Need> more = new ArrayList<>(this.needs);
        more.add(new Need(of, least));
        return new InsidePart(this.name, this.which, this.gives, this.bonuses, more);
    }

    /**
     * What a neighbour is, among the six places beside a block: a block of a kind, wherever it stands (inside, in the
     * shell, or outside), or a working block of a part.
     */
    public sealed interface Neighbour permits Neighbour.Block, Neighbour.Part {

        /** A neighbour that is one of the blocks {@code which} names. */
        static Neighbour block(final IBlockMatcher which) {
            return new Block(which);
        }

        /** A neighbour that is a working block of the part {@code name}. */
        static Neighbour part(final String name) {
            return new Part(name);
        }

        /** A block of a kind. */
        record Block(IBlockMatcher which) implements Neighbour {

            public Block {
                Objects.requireNonNull(which, "which");
            }
        }

        /** A working block of a part. */
        record Part(String name) implements Neighbour {

            public Part {
                Objects.requireNonNull(name, "name");
            }
        }
    }

    /**
     * What a working block gives more for each neighbour of a kind.
     *
     * @param key  what it adds to
     * @param of   the neighbours counted
     * @param each how much each neighbour adds
     */
    public record Bonus(String key, Neighbour of, double each) {

        public Bonus {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(of, "of");
        }
    }

    /**
     * What a block needs beside it to work.
     *
     * @param of    the neighbours counted
     * @param least how many it needs, one to six
     */
    public record Need(Neighbour of, int least) {

        public Need {
            Objects.requireNonNull(of, "of");
            if (least < 1 || least > 6) {
                throw new IllegalArgumentException("a block has six neighbours, so it needs one to six, not " + least);
            }
        }
    }
}
