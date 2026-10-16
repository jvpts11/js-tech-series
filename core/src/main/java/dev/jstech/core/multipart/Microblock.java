/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

/**
 * A microblock standing in a block space: a piece of a block of any mod, of a shape and a thickness, in one of the
 * space's places; a post in the centre runs along an axis.
 *
 * @param piece    its shape, thickness and material
 * @param slot     the place it stands in ({@link SpaceSlots})
 * @param axis     for a post in the centre, the axis it runs along; else 0
 */
public record Microblock(MicroblockPiece piece, int slot, int axis) {

    public static final Codec<Microblock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MicroblockPiece.CODEC.fieldOf("piece").forGetter(Microblock::piece),
            Codec.intRange(0, SpaceSlots.COUNT - 1).fieldOf("slot").forGetter(Microblock::slot),
            Codec.intRange(SpaceSlots.X, SpaceSlots.Z).optionalFieldOf("axis", SpaceSlots.Y).forGetter(Microblock::axis)
    ).apply(instance, Microblock::new));

    public Microblock {
        Objects.requireNonNull(piece, "piece");
        if (!piece.shape().fits(slot)) {
            throw new IllegalArgumentException("a " + piece.shape().serializedName() + " does not stand in place "
                    + slot);
        }
    }

    /** The room it takes, in pixels. */
    public List<PartBox> boxes() {
        return this.piece.shape().boxes(this.slot, this.piece.size(), this.axis);
    }

    /** It as the fit sees it. */
    public SpaceFit.Piece fitPiece() {
        final boolean onFace = this.piece.shape().onFace();
        return new SpaceFit.Piece(this.slot, boxes(), onFace, onFace);
    }

    /**
     * A microblock's shape, thickness and material: what its item carries, and what the saw cuts.
     *
     * @param shape    its shape
     * @param size     its thickness, in eighths of a block
     * @param material the block it is cut from
     */
    public record MicroblockPiece(MicroShape shape, int size, Block material) {

        public static final Codec<MicroblockPiece> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StableCodecs.byName(MicroShape.class).fieldOf("shape").forGetter(MicroblockPiece::shape),
                Codec.intRange(MicroShape.THINNEST, MicroShape.THICKEST).fieldOf("size")
                        .forGetter(MicroblockPiece::size),
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("material").forGetter(MicroblockPiece::material)
        ).apply(instance, MicroblockPiece::new));

        public MicroblockPiece {
            Objects.requireNonNull(shape, "shape");
            Objects.requireNonNull(material, "material");
            if (!shape.takes(size)) {
                throw new IllegalArgumentException("no " + shape.serializedName() + " is " + size + " eighths thick");
            }
        }

        /** It as the saw sees it, its material apart. */
        public Cuts.Piece cut() {
            return new Cuts.Piece(this.shape, this.size);
        }
    }
}
