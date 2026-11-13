/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import com.mojang.serialization.Codec;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.DimensionState;
import dev.jstech.core.state.PlayerState;
import dev.jstech.core.state.ServerState;
import dev.jstech.core.state.TeamState;
import dev.jstech.tests.JsTests;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

/**
 * A state of every scope for the tests to keep values in, each synced, and two that are not, kept for the tests of a
 * file a newer mod saved and of one read only in part. Registered on both sides, as every state is.
 */
public final class TestStates {

    public static final ServerState<Integer> COUNTER = CoreState.builder(id("counter"), Codec.INT, 0)
            .synced(ByteBufCodecs.VAR_INT)
            .server();
    public static final DimensionState<String> WEATHER = CoreState.builder(id("weather"), Codec.STRING, "")
            .synced(ByteBufCodecs.STRING_UTF8)
            .dimension();
    public static final PlayerState<Integer> SCORE = CoreState.builder(id("score"), Codec.INT, 0)
            .synced(ByteBufCodecs.VAR_INT)
            .player();
    public static final TeamState<Integer> FUNDS = CoreState.builder(id("funds"), Codec.INT, 0)
            .synced(ByteBufCodecs.VAR_INT)
            .team();
    public static final ServerState<Integer> NEWER = CoreState.builder(id("newer"), Codec.INT, 0).server();
    public static final PlayerState<Integer> PARTLY = CoreState.builder(id("partly"), Codec.INT, 0).player();

    private TestStates() {
    }

    public static void register() {
        CoreStates.register(COUNTER);
        CoreStates.register(WEATHER);
        CoreStates.register(SCORE);
        CoreStates.register(FUNDS);
        CoreStates.register(NEWER);
        CoreStates.register(PARTLY);
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }
}
