/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.gui.style.StyleSettings;
import dev.jstech.core.integration.jade.JadeStyles;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

/**
 * Jade's tooltip in the player's style, with Jade installed ({@code -PwithJade}): picking the series' style for
 * everything paints Jade's tooltip on the style's own ground, and picking none gives Jade its own theme back.
 */
public final class JadeStyleClientTests {

    private static final String JADE = "jade";
    private static final String SERIES = "jscore:jstech";
    private static final int WAIT = 100;
    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final BlockPos LOOKED_AT = new BlockPos(0, 2, -2);

    private JadeStyleClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void jade_wearsTheStyleThePlayerPicked(final ClientTestContext ctx) {
        if (!ModList.get().isLoaded(JADE)) {
            ctx.then(0, () -> { });
            return;
        }
        final String before = StyleSettings.everything();
        ctx.afterTest(() -> {
            choose(before);
            ctx.server().submit(() -> ctx.serverLevel().setBlockAndUpdate(ctx.abs(LOOKED_AT),
                    Blocks.AIR.defaultBlockState()));
        });
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(LOOKED_AT), Blocks.FURNACE.defaultBlockState()))
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(LOOKED_AT))))
                .then(0, () -> choose(SERIES))
                .thenWaitUntil(JadeStyles::wearing, WAIT, "Jade to wear the player's style")
                .thenAssert(0, () -> JadeStyles.ground() == Styles.get(ResourceLocation.parse(SERIES)).orElseThrow()
                        .colour(StyleRole.TOOLTIP_GROUND), "its tooltip on the ground of the series' style")
                .thenScreenshot(10, "jade-in-the-series-style")
                .then(0, () -> choose(""))
                .thenWaitUntil(() -> !JadeStyles.wearing(), WAIT, "and Jade's own theme back once none is picked");
    }

    private static void choose(final String style) {
        final ConfigDraft draft = new ConfigDraft(StyleSettings.FILE);
        draft.choose(StyleSettings.EVERYTHING, style);
        draft.apply();
    }
}
