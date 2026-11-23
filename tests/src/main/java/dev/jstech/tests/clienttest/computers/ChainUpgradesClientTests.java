/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.core.client.machine.MachineScreen;
import dev.jstech.core.jstechseries.upgrade.SharedUpgrades;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The Upgrades tab of a chain machine of four upgrade slots, the Advanced Lithography Station: its four bays, each a
 * slot a player puts an upgrade into, closer than three are so the fourth keeps clear of the bank, and the bank's
 * eleven rows showing what four motors, a drive, two gas recovery units and two precision toolings do together.
 */
public final class ChainUpgradesClientTests {

    private static final BlockPos CORNER = new BlockPos(2, 1, 2);
    /* The ground the station stands on, cleared before and after. */
    private static final int ROOM = 8;
    private static final int WAIT = 300;
    private static final int SETTLE = 10;

    private ChainUpgradesClientTests() {
    }

    @ClientTest(timeoutTicks = 2_400)
    public static void upgradesTab_showsTheFourBaysOfAnAdvancedMachine(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        final ChainMachine station = ChainMachines.byId("lithography_advanced").orElseThrow();
        final BlockPos terminal = place(station);
        ctx.thenServer(0, level -> clear(ctx, level))
                .thenBuild(2, world -> lay(world, station))
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(terminal))
                        instanceof ChainMachineBlockEntity built && built.formed(), WAIT,
                        "the station to form", level -> "it stands unformed")
                .thenServer(0, level -> upgrade((ChainMachineBlockEntity) level.getBlockEntity(ctx.abs(terminal))))
                .thenTeleport(0, new BlockPos(terminal.getX(), CORNER.getY(), CORNER.getZ() - 2), Direction.SOUTH)
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(terminal))))
                .then(2, () -> ctx.rightClick(terminal, Direction.NORTH))
                .thenWaitUntil(() -> ctx.mc().screen instanceof MachineScreen, WAIT, "the station's screen to open")
                .then(0, () -> ctx.click(left(ctx) + MachineScreenLayout.tabX(MachineMenu.UPGRADES)
                        + MachineScreenLayout.TAB_WIDTH / 2.0, top(ctx) + MachineScreenLayout.TAB_HEIGHT / 2.0))
                .then(SETTLE, () -> {
                    final MachineMenu menu = ((MachineScreen) ctx.mc().screen).getMenu();
                    final int first = ((ChainMachineBlockEntity) menu.machine()).layout().firstUpgrade();
                    final List<Slot> bays = menu.slots.stream().filter(slot -> slot instanceof SlotItemHandler held
                            && held.getSlotIndex() >= first && held.getItemHandler() == menu.machine().getInventory())
                            .toList();
                    ctx.assertTrue(bays.size() == 4, "the tab shows four bays: " + bays.size());
                    ctx.assertTrue(bays.get(3).x + 17 < MachineScreenLayout.faceX(MachineScreenLayout.BANK_X - 3),
                            "the fourth keeps clear of the bank: " + bays.get(3).x);
                })
                .thenScreenshot(0, "chain-upgrades-four-bays")
                .then(0, () -> ctx.mc().setScreen(null))
                .thenServer(0, level -> clear(ctx, level));
    }

    /* Four motors, a drive, two gas recovery units and two precision toolings, put in as a hopper puts them. */
    private static void upgrade(final ChainMachineBlockEntity station) {
        final int first = station.layout().firstUpgrade();
        final List<ItemStack> each = List.of(new ItemStack(SharedUpgrades.BRUSHED_MOTOR.get(), 4),
                new ItemStack(SharedUpgrades.VARIABLE_FREQUENCY_DRIVE.get()),
                new ItemStack(SharedUpgrades.GAS_RECOVERY_UNIT.get(), 2),
                new ItemStack(SharedUpgrades.PRECISION_TOOLING.get(), 2));
        for (int i = 0; i < each.size(); i++) {
            station.getInventory().insertItem(first + i, each.get(i), false);
        }
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (final BlockPos at : BlockPos.betweenClosed(CORNER, CORNER.offset(ROOM, ROOM, ROOM))) {
            level.setBlock(ctx.abs(at), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /* Lays {@code machine}'s blocks from {@link #CORNER} facing north, its own terminal at its place. */
    private static void lay(final TestWorldBuilder world, final ChainMachine machine) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final String row = layers.get(y).get(z);
                for (int x = 0; x < row.length(); x++) {
                    final char letter = row.charAt(x);
                    final Block block = letter == '#' ? ChainMachines.terminalOf(machine).get()
                            : ChainMachineBlocks.byId(machine.legend().get(letter)).get();
                    final BlockState state = block.defaultBlockState();
                    world.setBlock(CORNER.offset(x, y, z), state.hasProperty(HorizontalDirectionalBlock.FACING)
                            ? state.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH) : state);
                }
            }
        }
    }

    /* Where {@link #lay} sets {@code machine}'s terminal. */
    private static BlockPos place(final ChainMachine machine) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final int x = layers.get(y).get(z).indexOf('#');
                if (x >= 0) {
                    return CORNER.offset(x, y, z);
                }
            }
        }
        throw new IllegalArgumentException(machine.id() + " has no terminal");
    }

    private static int left(final ClientTestContext ctx) {
        return (ctx.mc().getWindow().getGuiScaledWidth() - MachineScreenLayout.WIDTH) / 2;
    }

    private static int top(final ClientTestContext ctx) {
        return (ctx.mc().getWindow().getGuiScaledHeight() - MachineScreenLayout.HEIGHT) / 2;
    }
}
