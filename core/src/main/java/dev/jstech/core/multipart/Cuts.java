/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * What the saw makes of a block or a microblock in the crafting grid, as Forge Multipart's does: with the saw above
 * it, two halves (a block two slabs, a slab two panels, a panel two covers); with the saw beside it, cut across (a
 * piece on a face two posts, a post two corners); with the saw at a corner of it, a piece on a face hollowed. Two
 * pieces of one shape and material join into one as thick as both, the thicknesses between the halves among them;
 * two that make eight eighths make the block again. A cover joined with a cable makes a facade. Pure.
 */
public final class Cuts {

    private Cuts() {
    }

    /** What cutting {@code piece} with the saw placed {@code way} from it makes, or nothing when it does not cut so. */
    public static Optional<Made> cut(final Piece piece, final Way way) {
        return switch (way) {
            case HALVES -> halves(piece);
            case ACROSS -> piece.shape() == MicroShape.FACE ? Optional.of(new Made(new Piece(MicroShape.POST,
                    piece.size()), 2)) : piece.shape() == MicroShape.POST ? Optional.of(new Made(new Piece(
                    MicroShape.CORNER, piece.size()), 2)) : Optional.empty();
            case HOLLOWED -> piece.shape() == MicroShape.FACE ? Optional.of(new Made(new Piece(MicroShape.HOLLOW,
                    piece.size()), 1)) : Optional.empty();
        };
    }

    /** One piece as thick as {@code a} and {@code b} together, of their shape, or the whole block; nothing else. */
    public static Optional<Piece> join(final Piece a, final Piece b) {
        if (a.whole() || b.whole() || a.shape() != b.shape() || a.shape() == MicroShape.FACADE) {
            return Optional.empty();
        }
        final int size = a.size() + b.size();
        if (size == Piece.WHOLE_SIZE && a.shape() == MicroShape.FACE) {
            return Optional.of(Piece.WHOLE);
        }
        return a.shape().takes(size) ? Optional.of(new Piece(a.shape(), size)) : Optional.empty();
    }

    /** The facade a cover makes joined with a cable, or nothing for any other piece. */
    public static Optional<Piece> facade(final Piece cover) {
        return cover.shape() == MicroShape.FACE && cover.size() == MicroShape.COVER
                ? Optional.of(new Piece(MicroShape.FACADE, MicroShape.COVER)) : Optional.empty();
    }

    private static Optional<Made> halves(final Piece piece) {
        if (piece.whole()) {
            return Optional.of(new Made(new Piece(MicroShape.FACE, MicroShape.SLAB), 2));
        }
        if ((piece.shape() == MicroShape.FACE || piece.shape() == MicroShape.HOLLOW) && piece.size() % 2 == 0) {
            return Optional.of(new Made(new Piece(piece.shape(), piece.size() / 2), 2));
        }
        return Optional.empty();
    }

    /** Where the saw lies from the piece in the grid. */
    public enum Way {
        /** Just above it: two halves. */
        HALVES,
        /** Just beside it: cut across. */
        ACROSS,
        /** At one of its corners: hollowed. */
        HOLLOWED
    }

    /**
     * A block, whole, or a microblock of a shape and a thickness, its material apart.
     *
     * @param shape its shape, or none for the whole block
     * @param size  its thickness in eighths; eight for the whole block
     */
    public record Piece(@Nullable MicroShape shape, int size) {

        /** A whole block's thickness. */
        public static final int WHOLE_SIZE = 8;
        /** The whole block. */
        public static final Piece WHOLE = new Piece(null, WHOLE_SIZE);

        public Piece {
            if (shape == null ? size != WHOLE_SIZE : !shape.takes(size)) {
                throw new IllegalArgumentException("no piece of " + shape + " is " + size + " eighths thick");
            }
        }

        /** Whether it is the whole block. */
        public boolean whole() {
            return this.shape == null;
        }
    }

    /**
     * What a cut makes: a piece, so many of it.
     *
     * @param piece what it makes
     * @param count how many
     */
    public record Made(Piece piece, int count) {

        public Made {
            Objects.requireNonNull(piece, "piece");
        }
    }
}
