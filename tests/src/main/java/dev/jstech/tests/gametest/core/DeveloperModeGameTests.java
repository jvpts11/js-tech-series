/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.developer.DeveloperMode;
import dev.jstech.core.developer.ThingTests;
import dev.jstech.tests.JsTests;
import dev.jstech.testkit.gametest.GameTestPlayers;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Developer mode on a server: the tools that read or change the world are an operator's only, while the world's own
 * key is on, and asking the server to run a thing's tests runs nothing for anyone else, nor for a thing no test names.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DeveloperModeGameTests {

    private static final String ARENA = "empty";
    /* A thing no GameTest is named after. */
    private static final ResourceLocation UNTESTED = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "nothing_names_this");
    /* The level a dedicated server gives its operators. */
    private static final int OPERATOR = 4;

    private DeveloperModeGameTests() {
    }

    @GameTest(template = ARENA)
    public static void worldTools_areAnOperatorsOnlyWhileTheWorldsKeyIsOn(final GameTestHelper helper) {
        final ServerPlayer player = GameTestPlayers.connected(helper, "studio_tester");
        try {
            helper.assertTrue(DeveloperMode.onServer(), "the world's key is on in a development run");
            helper.assertFalse(DeveloperMode.mayUseWorldTools(player),
                    "a player who is no operator may not use the tools that change the world");
            helper.assertTrue(ThingTests.run(player, UNTESTED) == 0, "what they ask the server to run runs nothing");
            // The GameTest server makes its operators at level 0, so the entry is written at a server's own level.
            player.server.getPlayerList().getOps().add(new ServerOpListEntry(player.getGameProfile(), OPERATOR,
                    false));
            helper.assertTrue(DeveloperMode.mayUseWorldTools(player), "an operator may, while the world's key is on");
            helper.assertTrue(ThingTests.run(player, UNTESTED) == 0, "a thing no GameTest names runs none");
        } finally {
            player.server.getPlayerList().getOps().remove(player.getGameProfile());
            GameTestPlayers.leave(player);
        }
        helper.succeed();
    }
}
