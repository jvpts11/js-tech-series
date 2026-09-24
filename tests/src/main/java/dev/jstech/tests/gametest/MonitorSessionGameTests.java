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
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.menu.IMonitorMenu;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Whether a player is already holding a monitor's session, which is what decides if one is opened for them.
 *
 * <p>Opening a menu tears the client's screen down and builds it again out of whatever the machine last sent.
 * That is right when the player is moving from one session to another and wrong when they are already in the
 * one being opened: an installer sends a page for every answer, and reopening the session for each of them
 * threw away the page that had just arrived and put back the one before it. What a player saw was a Next that
 * did nothing, and a machine that only moved on when they left the monitor and came back.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MonitorSessionGameTests {

    private MonitorSessionGameTests() {
    }

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos ELSEWHERE = new BlockPos(9, 2, 2);

    /** A machine with a monitor on it, and somebody to hold its sessions. */
    private static ServerPlayer holder(final GameTestHelper helper) {
        final TestWorldBuilder world =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO));
        world.placeRunningPersonalComputer(COMPUTER);
        world.placeMonitor(MONITOR, Direction.EAST);
        // A player per test: these run side by side, and each wants a holder of its own.
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "session-holder"));
    }

    /**
     * Held in the player's own menu rather than opened through the server.
     *
     * <p>A player with no connection behind it cannot be sent a screen, so opening one really would do nothing
     * and a test built on that would pass whatever the answer was. What is under test is the question asked
     * before a session is opened, so the session is simply put in the player's hands and the question asked.
     */
    private static void holding(final ServerPlayer player, final BlockPos host,
                                final MonitorSessionMenu.Phase phase) {
        player.containerMenu = new MonitorSessionMenu(1, player.getInventory(), host, host,
                HardwareEra.STANDARD, phase);
    }

    /**
     * The tab a terminal reopens on is settled on the server before the window opens, so a tab that is not there
     * any more is left for Network on both sides at once. The client used to keep it while the server moved on.
     */
    @GameTest(template = ARENA)
    public static void openingTab_leavesATabThatIsNotThereAnyMore(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(4, 2, 4));
        helper.assertTrue(ComputerTerminalMenu.openingTab(computer, helper.getLevel(), ComputerTerminalMenu.TAB_CRAFT)
                        == ComputerTerminalMenu.TAB_NETWORK,
                "with no Crafting Computer on its network the Craft tab is not there, so it opens on Network");
        helper.assertTrue(ComputerTerminalMenu.openingTab(computer, helper.getLevel(), ComputerTerminalMenu.TAB_LOCAL)
                        == ComputerTerminalMenu.TAB_LOCAL,
                "while a tab that is there is kept");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void isShowing_theSessionTheyAreHolding_isYes(final GameTestHelper helper) {
        final ServerPlayer player = holder(helper);
        final BlockPos host = helper.absolutePos(COMPUTER);
        holding(player, host, MonitorSessionMenu.Phase.INSTALLER);
        helper.assertTrue(MonitorSessionMenu.isShowing(player, host, MonitorSessionMenu.Phase.INSTALLER),
                "the installer they are holding is the installer they are holding");
        helper.succeed();
    }

    /**
     * The one that matters: another phase of the same machine is not the session they are holding, so it is
     * opened. This is the half that moves a player from the setup into the installer when they press Install.
     */
    @GameTest(template = ARENA)
    public static void isShowing_anotherPhaseOfTheSameMachine_isNo(final GameTestHelper helper) {
        final ServerPlayer player = holder(helper);
        final BlockPos host = helper.absolutePos(COMPUTER);
        holding(player, host, MonitorSessionMenu.Phase.FIRMWARE);
        helper.assertFalse(MonitorSessionMenu.isShowing(player, host, MonitorSessionMenu.Phase.INSTALLER),
                "the setup is not the installer");
        helper.assertFalse(MonitorSessionMenu.isShowing(player, host, MonitorSessionMenu.Phase.INSTALL_PROGRESS),
                "nor the copy");
        helper.succeed();
    }

    /** And the same phase of another machine is another machine, however alike the two screens look. */
    @GameTest(template = ARENA)
    public static void isShowing_theSamePhaseOfAnotherMachine_isNo(final GameTestHelper helper) {
        final ServerPlayer player = holder(helper);
        holding(player, helper.absolutePos(ELSEWHERE), MonitorSessionMenu.Phase.INSTALLER);
        helper.assertFalse(MonitorSessionMenu.isShowing(player, helper.absolutePos(COMPUTER),
                        MonitorSessionMenu.Phase.INSTALLER),
                "an installer on another machine is not this machine's");
        helper.succeed();
    }

    /**
     * A monitor is one screen with one keyboard: a second player who uses it while the first is at it is told who
     * has it, rather than being handed a session over the one the first is typing into.
     */
    @GameTest(template = ARENA)
    public static void userOf_somebodyElseAtTheMonitor_isThem(final GameTestHelper helper) {
        final ServerPlayer first = holder(helper);
        final ServerPlayer second = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "second-player"));
        final BlockPos monitor = helper.absolutePos(MONITOR);
        first.containerMenu = new MonitorSessionMenu(1, first.getInventory(), monitor,
                helper.absolutePos(COMPUTER), HardwareEra.STANDARD, MonitorSessionMenu.Phase.POST);
        second.containerMenu = second.inventoryMenu;
        helper.assertTrue(IMonitorMenu.userOf(List.of(first, second), monitor, second) == first,
                "the second player is told the first has the monitor");
        helper.assertTrue(IMonitorMenu.userOf(List.of(first, second), monitor, first) == null,
                "while the first, using it again, is in nobody's way but their own");
        helper.succeed();
    }

    /** A player at another monitor, even one on the same machine, leaves this one free. */
    @GameTest(template = ARENA)
    public static void userOf_somebodyAtAnotherMonitor_isNobody(final GameTestHelper helper) {
        final ServerPlayer first = holder(helper);
        final ServerPlayer second = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "second-player"));
        first.containerMenu = new MonitorSessionMenu(1, first.getInventory(), helper.absolutePos(ELSEWHERE),
                helper.absolutePos(COMPUTER), HardwareEra.STANDARD, MonitorSessionMenu.Phase.POST);
        helper.assertTrue(IMonitorMenu.userOf(List.of(first, second), helper.absolutePos(MONITOR), second) == null,
                "another monitor's session does not hold this one");
        helper.succeed();
    }

    /** Holding no session at all is holding no session, which is where every one of them starts. */
    @GameTest(template = ARENA)
    public static void isShowing_holdingNoSession_isNo(final GameTestHelper helper) {
        final ServerPlayer player = holder(helper);
        player.containerMenu = player.inventoryMenu;
        for (final MonitorSessionMenu.Phase phase : MonitorSessionMenu.Phase.values()) {
            helper.assertFalse(MonitorSessionMenu.isShowing(player, helper.absolutePos(COMPUTER), phase),
                    phase.name() + " is not open on somebody holding nothing");
        }
        helper.succeed();
    }
}
