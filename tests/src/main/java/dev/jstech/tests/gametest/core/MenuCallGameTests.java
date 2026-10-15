/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.call.MenuCalls;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestToolkit;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A call a screen makes to the server, as the server takes it: handled for a player with a menu of its kind open, and
 * refused for one with another menu, whatever the call carries.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MenuCallGameTests {

    private static final String ARENA = "empty";

    private MenuCallGameTests() {
    }

    @GameTest(template = ARENA)
    public static void receive_handlesTheCallFromAMenuOfItsKind(final GameTestHelper helper) {
        final ServerPlayer player = player(helper);
        player.containerMenu = ChestMenu.threeRows(1, player.getInventory());

        helper.assertTrue(TestToolkit.SET.receive(player, 5) && TestToolkit.CALLED.get() == 5,
                "the call from a chest's screen sets 5");
        helper.assertTrue(MenuCalls.byId(TestToolkit.SET.id()).orElseThrow() == TestToolkit.SET,
                "and is found by its id, as the payload finds it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void receive_refusesTheCallFromAnotherMenu(final GameTestHelper helper) {
        final ServerPlayer player = player(helper);
        player.containerMenu = player.inventoryMenu;
        TestToolkit.CALLED.set(0);

        helper.assertTrue(!TestToolkit.SET.receive(player, 9) && TestToolkit.CALLED.get() == 0,
                "a player looking at their own inventory cannot make a chest's call");
        helper.succeed();
    }

    /*
     * A player the server never sees join: a mock player joins the player list, and other mods installed beside the
     * series (Mekanism) then send it payloads it cannot take.
     */
    private static ServerPlayer player(final GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "jstests-caller"));
    }
}
