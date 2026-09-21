/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.authlib.GameProfile;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A machine knows who is looking at it from the desktops and prompts players open and close, instead of going
 * through every player of the level on every tick to find them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineViewersGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(2, 2, 2);
    private static final BlockPos OTHER = new BlockPos(5, 2, 2);

    private MachineViewersGameTests() {
    }

    @GameTest(template = ARENA)
    public static void viewers_followTheScreensPlayersOpenAndClose(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity computer = world.placeRunningPersonalComputer(COMPUTER);
        final PersonalComputerBlockEntity other = world.placeRunningPersonalComputer(OTHER);
        final ServerLevel level = helper.getLevel();
        /*
         * A server player of the test's own, never put on the server's player list: a listed player is announced to
         * every mod when it leaves, and keeps chunks loaded and packets flowing for the rest of the run.
         */
        final ServerPlayer player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "viewer"), ClientInformation.createDefault());

        helper.assertTrue(computer.consoleViewers(level).isEmpty(), "nobody looks at a machine nobody opened");

        player.containerMenu = desktop(player, computer);
        helper.assertTrue(computer.consoleViewers(level).contains(player), "opening its desktop makes a viewer");

        player.containerMenu = desktop(player, other);
        helper.assertFalse(computer.consoleViewers(level).contains(player),
                "a player now at another machine's desktop is no longer this one's viewer");
        helper.assertTrue(other.consoleViewers(level).contains(player), "they are the other machine's");

        player.doCloseContainer();
        helper.assertTrue(other.consoleViewers(level).isEmpty(), "closing the desktop ends it");
        helper.succeed();
    }

    private static DesktopMenu desktop(final ServerPlayer player, final PersonalComputerBlockEntity host) {
        return new DesktopMenu(1, player.getInventory(), host.getBlockPos(), host.getBlockPos(),
                ResourceLocation.fromNamespaceAndPath("jsc", "frames_xp"), "Desk", 8192, 0);
    }
}
