/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.model.ModelData;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * What a renderer works out once from a model's bones (which bone is a seat, which an anchor, which a row of a rack),
 * kept by the model and worked out again only when the models are read again, so a frame reads it rather than
 * walking the bones' names. Used on the render thread only.
 *
 * @param <V> what is worked out
 */
final class ModelFacts<V> {

    private final Function<ModelData, V> work;
    private final Map<ModelData, V> known = new IdentityHashMap<>();
    private long readAt = Long.MIN_VALUE;

    ModelFacts(final Function<ModelData, V> work) {
        this.work = work;
    }

    /** What is worked out from the model {@code model}, as its pose or its file has it. */
    V of(final ModelData model) {
        final long read = KitModels.reloadedAt();
        if (read != this.readAt) {
            this.known.clear();
            this.readAt = read;
        }
        return this.known.computeIfAbsent(model, this.work);
    }
}
