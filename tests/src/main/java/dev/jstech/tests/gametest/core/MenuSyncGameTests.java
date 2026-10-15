/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestScreens;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A menu's values bound by name, as a screen's file reads them on the server's copy: found by their names, worked out
 * where they are asked, and sent without a packet of the menu's own as the menu opens and as they change.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MenuSyncGameTests {

    private static final String ARENA = "empty";

    private MenuSyncGameTests() {
    }

    @GameTest(template = ARENA)
    public static void bound_findsEveryValueByItsName(final GameTestHelper helper) {
        final TestScreens.TestDataMenu menu = new TestScreens.TestDataMenu(1, player(helper).getInventory());

        helper.assertTrue(menu.boundNames().equals(List.of("big", "word", "progress", "ready")),
                "the values are bound in the order they were declared: " + menu.boundNames());
        helper.assertTrue(Long.valueOf(TestScreens.BIG).equals(menu.bound("big").orElseThrow().get()),
                "a number past a short's reach is worked out whole on the server");
        helper.assertTrue(TestScreens.WORD.equals(menu.bound("word").orElseThrow().get()), "a word is too");
        helper.assertTrue(Boolean.TRUE.equals(menu.bound("ready").orElseThrow().get()), "a flag reads as yes or no");
        helper.assertTrue(menu.bound("nothing").isEmpty(), "a name nobody bound finds nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void broadcast_sendsTheValuesWithoutAPacketOfTheMenusOwn(final GameTestHelper helper) {
        final ServerPlayer player = player(helper);
        final TestScreens.TestDataMenu menu = new TestScreens.TestDataMenu(1, player.getInventory());

        // A fake player takes what is sent and drops it: sending must not fail, as it opens and as it ticks.
        menu.sendAllDataToRemote();
        menu.broadcastChanges();
        menu.broadcastFullState();

        helper.assertTrue(menu.big() == TestScreens.BIG && TestScreens.WORD.equals(menu.word()),
                "the server's copy answers its own values");
        helper.succeed();
    }

    private static ServerPlayer player(final GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "jstests-menu"));
    }
}
