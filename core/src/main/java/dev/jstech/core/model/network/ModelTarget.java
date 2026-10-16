/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model.network;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a model the server animates is drawn for: the block entity at a place, an entity by its number, or an item by
 * the number its stack was given.
 *
 * @param kind   which of the three
 * @param pos    the block entity's place, for a block
 * @param entity the entity's number, for an entity
 * @param item   the stack's number, for an item
 */
public record ModelTarget(Kind kind, BlockPos pos, int entity, long item) {

    public static final StreamCodec<ByteBuf, ModelTarget> STREAM_CODEC = StreamCodec.of(ModelTarget::write,
            ModelTarget::read);

    public ModelTarget {
        Objects.requireNonNull(kind, "kind");
        pos = pos.immutable();
    }

    /** The block entity at {@code pos}. */
    public static ModelTarget block(final BlockPos pos) {
        return new ModelTarget(Kind.BLOCK, pos, 0, 0);
    }

    /** The entity numbered {@code id}. */
    public static ModelTarget entity(final int id) {
        return new ModelTarget(Kind.ENTITY, BlockPos.ZERO, id, 0);
    }

    /** The item stack numbered {@code id}. */
    public static ModelTarget item(final long id) {
        return new ModelTarget(Kind.ITEM, BlockPos.ZERO, 0, id);
    }

    private static void write(final ByteBuf buffer, final ModelTarget target) {
        buffer.writeByte(target.kind == Kind.BLOCK ? 0 : target.kind == Kind.ENTITY ? 1 : 2);
        switch (target.kind) {
            case BLOCK -> buffer.writeLong(target.pos.asLong());
            case ENTITY -> buffer.writeInt(target.entity);
            case ITEM -> buffer.writeLong(target.item);
        }
    }

    private static ModelTarget read(final ByteBuf buffer) {
        return switch (buffer.readByte()) {
            case 0 -> block(BlockPos.of(buffer.readLong()));
            case 1 -> entity(buffer.readInt());
            default -> item(buffer.readLong());
        };
    }

    /** What a model is drawn for. */
    public enum Kind {
        BLOCK,
        ENTITY,
        ITEM
    }
}
