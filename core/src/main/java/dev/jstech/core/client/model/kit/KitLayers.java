/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * The layers laid over the Core's models as they are drawn ({@link IKitLayer}): those of one model, and those of every
 * model. Added from a mod's client setup; they stay until they are taken off.
 */
public final class KitLayers {

    private static final Map<ResourceLocation, List<IKitLayer>> BY_MODEL = new HashMap<>();
    private static final List<IKitLayer> EVERYWHERE = new ArrayList<>();

    private KitLayers() {
    }

    /** Lays {@code layer} over the model {@code model}, wherever it is drawn. */
    public static void add(final ResourceLocation model, final IKitLayer layer) {
        BY_MODEL.computeIfAbsent(Objects.requireNonNull(model, "model"), id -> new ArrayList<>())
                .add(Objects.requireNonNull(layer, "layer"));
    }

    /** Lays {@code layer} over every model of the Core's. */
    public static void everywhere(final IKitLayer layer) {
        EVERYWHERE.add(Objects.requireNonNull(layer, "layer"));
    }

    /**
     * Takes {@code layer} off again, from every model or from the models it was laid over. A model with a layer is
     * drawn alone, never together with others like it, so a layer laid for a while is best taken off after it.
     */
    public static void remove(final IKitLayer layer) {
        EVERYWHERE.remove(layer);
        BY_MODEL.values().forEach(layers -> layers.remove(layer));
        BY_MODEL.values().removeIf(List::isEmpty);
    }

    /** The layers over {@code model}: every model's first, then its own. */
    public static List<IKitLayer> of(final ResourceLocation model) {
        final List<IKitLayer> own = BY_MODEL.get(model);
        if (own == null) {
            return EVERYWHERE;
        }
        if (EVERYWHERE.isEmpty()) {
            return own;
        }
        final List<IKitLayer> both = new ArrayList<>(EVERYWHERE);
        both.addAll(own);
        return both;
    }
}
