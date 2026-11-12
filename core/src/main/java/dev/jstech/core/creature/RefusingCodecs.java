/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.creature;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;

/**
 * Codecs of records that refuse a value they are built with: a record's refusal is an exception, which reading a file
 * does not turn into an error by itself, so it ended the whole reload where the one file should be left out.
 */
final class RefusingCodecs {

    private RefusingCodecs() {
    }

    /** {@code codec}, where a value its record refuses is an error in the file it was read from. */
    static <T> Codec<T> refusing(final Codec<T> codec) {
        return Codec.of(codec, new Decoder<>() {
            @Override
            public <I> DataResult<Pair<T, I>> decode(final DynamicOps<I> ops, final I input) {
                try {
                    return codec.decode(ops, input);
                } catch (final IllegalArgumentException refused) {
                    return DataResult.error(refused::getMessage);
                }
            }
        });
    }
}
