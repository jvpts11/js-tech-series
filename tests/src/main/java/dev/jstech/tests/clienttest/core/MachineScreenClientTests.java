/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.machine.MachineScreen;
import dev.jstech.core.energy.EnergyProtection;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.team.Access;
import dev.jstech.core.team.Ownership;
import dev.jstech.tests.TestFuses;
import dev.jstech.tests.TestItems;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.TestPress;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The machine screen on a player's game, to set beside the approved design: the test kit machine, owned by the player
 * and open to their team, working cobblestone into stone with a speed upgrade in a bay, hydrogen in its gas tank, a
 * fuse in its holder and a charged battery in its bay; each of its five tabs in turn; and its switch thrown off and on
 * again from the screen.
 */
public final class MachineScreenClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final BlockPos MACHINE = new BlockPos(0, 2, 1);
    private static final int WAIT = 200;
    private static final long STORED = 6_000L;
    private static final long HYDROGEN = 6_000L;
    private static final int CHARGE = 7_200;

    private MachineScreenClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void screen_showsEveryTabOfAWorkingMachine(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> build(ctx, level))
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(MACHINE))))
                .then(2, () -> ctx.rightClick(MACHINE, Direction.NORTH))
                .thenWaitUntil(() -> ctx.mc().screen instanceof MachineScreen && machine(ctx).running(), WAIT,
                        "the machine's screen to open on the machine at work")
                .thenScreenshot(10, "machine-main")
                .then(0, () -> tab(ctx, MachineMenu.SIDES))
                .thenScreenshot(5, "machine-sides")
                .then(0, () -> tab(ctx, MachineMenu.REDSTONE))
                .thenScreenshot(5, "machine-redstone")
                .then(0, () -> tab(ctx, MachineMenu.UPGRADES))
                .thenScreenshot(5, "machine-upgrades")
                .then(0, () -> tab(ctx, MachineMenu.PROTECTION))
                .thenScreenshot(5, "machine-protection")
                .then(0, () -> tab(ctx, MachineMenu.MAIN))
                .then(2, () -> face(ctx, MachineScreenLayout.SWITCH_X + 7, MachineScreenLayout.SWITCH_Y + 11))
                .thenWaitUntilServer(level -> !serverMachine(ctx, level).control().on(), WAIT,
                        "the switch to turn the machine off", level -> "still on")
                .thenScreenshot(5, "machine-switched-off")
                .then(0, () -> face(ctx, MachineScreenLayout.SWITCH_X + 7, MachineScreenLayout.SWITCH_Y + 11))
                .thenWaitUntilServer(level -> serverMachine(ctx, level).control().on(), WAIT,
                        "the switch to turn it on again", level -> "still off")
                .then(0, () -> ctx.mc().setScreen(null))
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(MACHINE), Blocks.AIR.defaultBlockState()));
    }

    /* The machine, owned by the player and open to their team, given everything its tabs show. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        final BlockPos at = ctx.abs(MACHINE);
        level.setBlockAndUpdate(at, TestKitMachine.BLOCK.get().defaultBlockState());
        if (!(level.getBlockEntity(at) instanceof TestKitMachine.KitMachineBlockEntity machine)) {
            throw new IllegalStateException("no kit machine where it was placed");
        }
        machine.own(Ownership.of(ctx.player().getUUID()).withAccess(Access.TEAM));
        Objects.requireNonNull(machine.power().portStore(), "the kit machine has a port").setStored(STORED);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 64));
        machine.getInventory().setStackInSlot(TestKitMachine.OUT, new ItemStack(Items.STONE, 7));
        machine.getInventory().setStackInSlot(TestKitMachine.LAYOUT.firstUpgrade(),
                new ItemStack(TestPress.UPGRADE.get()));
        machine.matter().inputs(MatterState.GAS).getFirst().insert(new MatterStack(Elements.of(Element.HYDROGEN),
                HYDROGEN), false);
        final EnergyProtection protection = Objects.requireNonNull(machine.power().protection(), "a protection");
        protection.slots().setStackInSlot(EnergyProtection.FUSE, new ItemStack(TestFuses.ONE_CLASS.get()));
        final ItemStack battery = new ItemStack(TestItems.FULL.get());
        final IEnergyStorage cell = battery.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cell != null) {
            for (int given = 0; given < CHARGE; given += TestItems.ENERGY_IN) {
                cell.receiveEnergy(TestItems.ENERGY_IN, false);
            }
        }
        protection.slots().setStackInSlot(EnergyProtection.BATTERY, battery);
    }

    private static MachineBlockEntity machine(final ClientTestContext ctx) {
        return ((MachineScreen) Objects.requireNonNull(ctx.mc().screen, "no screen")).getMenu().machine();
    }

    /* The machine as the server holds it, read on the server's own thread, where the level hands it out. */
    private static MachineBlockEntity serverMachine(final ClientTestContext ctx, final ServerLevel level) {
        return (MachineBlockEntity) Objects.requireNonNull(level.getBlockEntity(ctx.abs(MACHINE)),
                "no machine on the server");
    }

    /* Clicks tab {@code index} of the screen, drawn in the middle of the window. */
    private static void tab(final ClientTestContext ctx, final int index) {
        ctx.click(left(ctx) + MachineScreenLayout.tabX(index) + MachineScreenLayout.TAB_WIDTH / 2.0,
                top(ctx) + MachineScreenLayout.TAB_HEIGHT / 2.0);
    }

    /* Clicks the point ({@code x}, {@code y}) of the steel face. */
    private static void face(final ClientTestContext ctx, final int x, final int y) {
        ctx.click(left(ctx) + MachineScreenLayout.faceX(x) + 0.5, top(ctx) + MachineScreenLayout.faceY(y) + 0.5);
    }

    private static int left(final ClientTestContext ctx) {
        return (ctx.mc().getWindow().getGuiScaledWidth() - MachineScreenLayout.WIDTH) / 2;
    }

    private static int top(final ClientTestContext ctx) {
        return (ctx.mc().getWindow().getGuiScaledHeight() - MachineScreenLayout.HEIGHT) / 2;
    }
}
