/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * A block's state as a model's variants read it, each property by its name with its value as the game writes it,
 * worked out once for each state; and the way the state faces.
 */
final class KitStates {

    /* A block's states are made once, at the start, so each is its own key; the render thread is the only one. */
    private static final Map<BlockState, Map<String, String>> WRITTEN = new IdentityHashMap<>();

    private KitStates() {
    }

    /** The properties of {@code state}, by name. */
    static Map<String, String> of(final BlockState state) {
        return WRITTEN.computeIfAbsent(state, KitStates::write);
    }

    /** The way {@code state} faces, by its facing property; north for a block that has none. */
    static Direction facing(final BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    private static Map<String, String> write(final BlockState state) {
        final Map<String, String> out = new HashMap<>();
        for (final Property<?> property : state.getProperties()) {
            out.put(property.getName(), written(state, property));
        }
        return Map.copyOf(out);
    }

    private static <V extends Comparable<V>> String written(final BlockState state, final Property<V> property) {
        return property.getName(state.getValue(property));
    }
}
