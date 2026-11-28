/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import java.util.List;

/**
 * Patterns read whole or not at all. A codec keeps what it could read of a value when a part of it fails: a list
 * keeps its other elements, and an optional field reads as absent. That is right for rows that stand apart and wrong
 * for a recipe: a pattern read back without an ingredient the game no longer has makes its result out of less, and
 * the planner takes it as the cheaper way. So a pattern with a part that cannot be read is not read at all, and
 * whoever reads a list of patterns leaves it out whole.
 */
final class WholeReads {

    private WholeReads() {
    }

    /** {@code codec}, read whole: a value with a part that cannot be read is not read, nothing kept of it. */
    static <T> Codec<T> of(final Codec<T> codec) {
        return Codec.of(codec, new Decoder<>() {
            @Override
            public <O> DataResult<Pair<T, O>> decode(final DynamicOps<O> ops, final O input) {
                final DataResult<Pair<T, O>> read = codec.decode(ops, input);
                return read.error().<DataResult<Pair<T, O>>>map(error -> DataResult.error(error.messageSupplier()))
                        .orElse(read);
            }
        });
    }

    /** A list of {@code element}, read whole: one element that cannot be read, and none of the list is. */
    static <E> Codec<List<E>> listOf(final Codec<E> element) {
        return of(element.listOf());
    }
}
