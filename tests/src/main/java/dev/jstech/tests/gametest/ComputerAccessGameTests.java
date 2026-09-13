/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.authlib.GameProfile;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.ScreenSessions;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A payload that acts on a computer is taken only from a player the server put in front of that computer:
 * through its desktop, through a plain screen the server opened on its monitor, or from a desktop that has
 * just closed. A position the client names is never enough on its own.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ComputerAccessGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos AT_MONITOR = new BlockPos(8, 2, 2);
    private static final BlockPos FAR_AWAY = new BlockPos(6, 2, 20);

    /** Longer than a closed desktop's last word is still taken. */
    private static final int AFTER_CLOSING = 45;

    private ComputerAccessGameTests() {
    }

    private record Desk(BlockPos computer, BlockPos monitor, ServerPlayer player) {

        /** Another machine's position, as a modified client could put in any payload. */
        BlockPos elsewhere() {
            return computer.north(3);
        }
    }

    @GameTest(template = ARENA)
    public static void machine_admitsOnlyADesktopOfThatMachineWithinReach(final GameTestHelper helper) {
        final Desk desk = desk(helper);
        final ComputerAccess.IGate<BlockPos> gate = ComputerAccess.machine(pos -> pos);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(linked(helper, desk), "the monitor links to the computer"))
                .thenExecute(() -> {
                    final ServerPlayer player = desk.player();
                    helper.assertFalse(gate.admits(player, desk.computer()), "no screen open: refused");
                    player.containerMenu = desktopOf(desk);
                    helper.assertTrue(gate.admits(player, desk.computer()), "the desktop of that machine: admitted");
                    helper.assertFalse(gate.admits(player, desk.elsewhere()), "a payload naming another machine: refused");
                    standAt(helper, player, FAR_AWAY);
                    helper.assertFalse(gate.admits(player, desk.computer()), "walked away from the monitor: refused");
                    player.containerMenu = player.inventoryMenu;
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void screen_admitsTheOpenedScreenOnlyAtTheMonitorShowingItsMachine(final GameTestHelper helper) {
        final Desk desk = desk(helper);
        final ComputerAccess.IGate<BlockPos> gate = ComputerAccess.screen(pos -> pos);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(linked(helper, desk), "the monitor links to the computer"))
                .thenExecute(() -> {
                    final ServerPlayer player = desk.player();
                    helper.assertFalse(gate.admits(player, desk.computer()), "the server opened nothing: refused");
                    ScreenSessions.opened(player, desk.monitor(), desk.computer());
                    helper.assertTrue(gate.admits(player, desk.computer()), "the screen the server opened: admitted");
                    helper.assertFalse(gate.admits(player, desk.elsewhere()), "a payload naming another machine: refused");
                    standAt(helper, player, FAR_AWAY);
                    helper.assertFalse(gate.admits(player, desk.computer()), "walked away from the monitor: refused");
                    standAt(helper, player, AT_MONITOR);
                    helper.assertTrue(gate.admits(player, desk.computer()), "back at the monitor: admitted again");
                    NeoForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, desktopOf(desk)));
                    helper.assertFalse(gate.admits(player, desk.computer()), "a menu opened since: refused");
                    ScreenSessions.opened(player, desk.monitor(), desk.elsewhere());
                    helper.assertFalse(gate.admits(player, desk.elsewhere()),
                            "a monitor that does not show that machine: refused");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void closingDesktop_admitsItsLastLayoutForAMomentOnly(final GameTestHelper helper) {
        final Desk desk = desk(helper);
        final ComputerAccess.IGate<BlockPos> machine = ComputerAccess.machine(pos -> pos);
        final ComputerAccess.IGate<BlockPos> gate = ComputerAccess.machineOrClosingDesktop(pos -> pos);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(linked(helper, desk), "the monitor links to the computer"))
                .thenExecute(() -> {
                    final ServerPlayer player = desk.player();
                    player.containerMenu = desktopOf(desk);
                    player.closeContainer();
                    helper.assertFalse(machine.admits(player, desk.computer()), "the desktop is closed");
                    helper.assertTrue(gate.admits(player, desk.computer()), "its last layout, right after closing: admitted");
                    helper.assertFalse(gate.admits(player, desk.elsewhere()), "for another machine: refused");
                })
                .thenExecuteAfter(AFTER_CLOSING, () -> helper.assertFalse(gate.admits(desk.player(), desk.computer()),
                        "long after closing: refused"))
                .thenSucceed();
    }

    /** A running desk computer, its monitor, and a player of its own standing at the monitor. */
    private static Desk desk(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.placeRunningPersonalComputer(COMPUTER);
        world.placeMonitor(MONITOR, Direction.EAST);
        // A player per test: the gates remember players by id, and the tests run side by side.
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "access-gate"));
        standAt(helper, player, AT_MONITOR);
        return new Desk(helper.absolutePos(COMPUTER), helper.absolutePos(MONITOR), player);
    }

    private static DesktopMenu desktopOf(final Desk desk) {
        return new DesktopMenu(1, desk.player().getInventory(), desk.monitor(), desk.computer(),
                ResourceLocation.fromNamespaceAndPath("jsc", "frames_xp"), "Desk", 8192, 0);
    }

    private static boolean linked(final GameTestHelper helper, final Desk desk) {
        return helper.getLevel().getBlockEntity(desk.monitor()) instanceof MonitorBlockEntity monitor
                && monitor.shows(desk.computer());
    }

    private static void standAt(final GameTestHelper helper, final ServerPlayer player, final BlockPos relative) {
        final Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(relative));
        player.setPos(at.x, at.y, at.z);
    }
}
