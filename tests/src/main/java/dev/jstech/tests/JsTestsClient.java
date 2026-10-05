/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.client.model.CoreModels;
import dev.jstech.core.connect.IJoinRule;
import dev.jstech.tests.clienttest.TestClientParts;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * The test mod on a player's game: the connected panel drawn from five of the game's concrete colours, one a tile,
 * so a screenshot shows which tile each quarter of a face took.
 */
@Mod(value = JsTests.MODID, dist = Dist.CLIENT)
public final class JsTestsClient {

    /** Alone, across, upright, inner corner, whole: red, yellow, blue, lime and white. */
    public static final List<ResourceLocation> PANEL_TILES = List.of(
            ResourceLocation.withDefaultNamespace("block/red_concrete"),
            ResourceLocation.withDefaultNamespace("block/yellow_concrete"),
            ResourceLocation.withDefaultNamespace("block/blue_concrete"),
            ResourceLocation.withDefaultNamespace("block/lime_concrete"),
            ResourceLocation.withDefaultNamespace("block/white_concrete"));

    public JsTestsClient() {
        CoreModels.connected(TestBlocks.CONNECTED_PANEL, IJoinRule.sameBlock(), PANEL_TILES);
        // The dial's renderer and the picture program's window, as another mod registers them.
        TestClientParts.register();
    }
}
