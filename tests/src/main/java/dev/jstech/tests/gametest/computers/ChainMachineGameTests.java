/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.machine.ChainFormedBlock;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.chain.machine.ChainMachineTerminalBlock;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestComputerEnergy;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The chain's machines (computers/docs/design/survival.md): each built of its blocks round the Machine Terminal, which
 * becomes the machine's own terminal once the shape stands whole; formed, its entries draw nothing and its plain
 * blocks merge into its model; it works its recipes, taking through its inlets and giving through its outlets, drawing
 * its energy through its power entry (the Coke Oven none); broken, its terminal gives back the Machine Terminal; a
 * Legacy Lithography Station waits for its computer, and the ArF one works two eras' recipes at each era's draw; and an
 * Electronics Assembly Line works its era's bench recipes.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ChainMachineGameTests {

    private static final String ARENA = "empty";
    private static final String ENERGY = TestComputerEnergy.BATCH;
    /* Where a machine's front west corner stands, its front to the north. */
    private static final BlockPos CORNER = new BlockPos(2, 1, 2);
    private static final int LONGEST = 4;

    private ChainMachineGameTests() {
    }

    @GameTest(template = ARENA)
    public static void every_machineFormsFromItsBlocks(final GameTestHelper helper) {
        for (final ChainMachine machine : ChainMachines.all()) {
            final BlockPos terminal = build(helper, machine);
            helper.assertTrue(ChainMachines.formAt(helper.getLevel(), helper.absolutePos(terminal), Direction.NORTH),
                    machine.id() + " forms round the Machine Terminal");
            helper.assertTrue(helper.getBlockState(terminal).getBlock() instanceof ChainMachineTerminalBlock own
                    && own.machine().equals(machine.id()), machine.id() + "'s terminal is its own");
            clear(helper);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void terminal_formsNothingAlone(final GameTestHelper helper) {
        helper.setBlock(CORNER, ChainMachineBlocks.TERMINAL.get().defaultBlockState());
        helper.assertTrue(!ChainMachines.formAt(helper.getLevel(), helper.absolutePos(CORNER), Direction.NORTH),
                "no machine stands round it");
        helper.assertBlockPresent(ChainMachineBlocks.TERMINAL.get(), CORNER);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void crusher_formsItsEntriesDrawingNothing(final GameTestHelper helper) {
        final ChainMachine crusher = machine("crusher");
        final BlockPos terminal = formed(helper, crusher);
        final BlockPos outlet = place(crusher, "item_outlet");
        final BlockPos roll = place(crusher, "crusher_roll");
        helper.succeedWhen(() -> {
            helper.assertTrue(machineAt(helper, terminal).formed(), "the crusher stands formed");
            helper.assertTrue(helper.getBlockState(outlet).getValue(ChainFormedBlock.FORMED),
                    "its item outlet draws nothing");
            helper.assertTrue(!helper.getBlockState(roll).is(ChainMachineBlocks.byId("crusher_roll").get()),
                    "its roll merged into its model");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void crusher_worksItsRecipes(final GameTestHelper helper) {
        final ChainMachine crusher = machine("crusher");
        final BlockPos terminal = formed(helper, crusher);
        helper.runAfterDelay(20, () -> machineAt(helper, terminal).getInventory()
                .insertItem(0, new ItemStack(ChainParts.FERRITE.get()), false));
        helper.succeedWhen(() -> helper.assertTrue(holds(machineAt(helper, terminal), ChainParts.INDUCTOR.get(), 2),
                "the ferrite crushed into two inductors"));
    }

    /* What moves things for it (a hopper, a bus, a Crafting Interface) reaches it through its inlet and its outlet. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void crusher_takesThroughItsInletAndGivesThroughItsOutlet(final GameTestHelper helper) {
        final ChainMachine crusher = machine("crusher");
        formed(helper, crusher);
        final BlockPos inlet = helper.absolutePos(place(crusher, "item_inlet"));
        final BlockPos outlet = helper.absolutePos(place(crusher, "item_outlet"));
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    final IItemHandler in = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, inlet,
                            Direction.UP);
                    helper.assertTrue(in != null, "its item inlet opens onto the machine");
                    helper.assertTrue(in.insertItem(0, new ItemStack(ChainParts.FERRITE.get()), false).isEmpty(),
                            "the inlet takes the ferrite in");
                })
                .thenWaitUntil(() -> {
                    final IItemHandler out = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, outlet,
                            Direction.NORTH);
                    helper.assertTrue(out != null, "its item outlet opens onto the machine");
                    int inductors = 0;
                    for (int slot = 0; slot < out.getSlots(); slot++) {
                        final ItemStack stack = out.extractItem(slot, Item.ABSOLUTE_MAX_STACK_SIZE, true);
                        inductors += stack.is(ChainParts.INDUCTOR.get()) ? stack.getCount() : 0;
                    }
                    helper.assertTrue(inductors == 2, "the outlet gives out the two inductors: " + inductors);
                })
                .thenSucceed();
    }

    /* The ArF station works the Transition's recipes and the Standard's, each era's at its own draw. */
    @GameTest(template = ARENA)
    public static void lithography_arfWorksTwoErasEachAtItsDraw(final GameTestHelper helper) {
        final List<ChainMachines.Worked> worked = ChainMachines.workedBy(machine("lithography_arf"));
        helper.assertTrue(worked.size() == 2, "two eras' recipes: " + worked.size());
        helper.assertTrue(worked.get(0).era() == HardwareEra.TRANSITION && worked.get(0).joulesATick() == 200,
                "the Transition's at 4 kW");
        helper.assertTrue(worked.get(1).era() == HardwareEra.STANDARD && worked.get(1).joulesATick() == 300,
                "the Standard's at 6 kW");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300, batch = ENERGY)
    public static void crusher_drawsItsEnergyThroughItsPowerEntry(final GameTestHelper helper) {
        final ChainMachine crusher = machine("crusher");
        final BlockPos terminal = formed(helper, crusher);
        final BlockPos entry = helper.absolutePos(place(crusher, "power_entry"));
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> machineAt(helper, terminal).getInventory()
                        .insertItem(0, new ItemStack(ChainParts.FERRITE.get()), false))
                .thenIdle(40)
                .thenExecute(() -> helper.assertTrue(!holds(machineAt(helper, terminal), ChainParts.INDUCTOR.get(), 1),
                        "no energy, no work"))
                .thenWaitUntil(() -> {
                    // A cable at the power entry gives it a tick's worth every tick.
                    TestComputerEnergy.feed(machineAt(helper, terminal).energyClassPort(PortKind.ENERGY_INPUT, entry,
                            Direction.SOUTH));
                    helper.assertTrue(holds(machineAt(helper, terminal), ChainParts.INDUCTOR.get(), 2),
                            "fed through its power entry, it works");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = ENERGY)
    public static void cokeOven_burnsWithNoEnergy(final GameTestHelper helper) {
        final ChainMachine oven = machine("coke_oven");
        final BlockPos terminal = formed(helper, oven);
        helper.runAfterDelay(20, () -> machineAt(helper, terminal).getInventory()
                .insertItem(0, new ItemStack(ChainParts.TRANSFORMER.get()), false));
        helper.succeedWhen(() -> helper.assertTrue(holds(machineAt(helper, terminal), ChainParts.FERRITE.get(), 1),
                "it burns part of its own coal: no energy asked"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void lithography_legacyWaitsForItsComputer(final GameTestHelper helper) {
        final ChainMachine station = machine("lithography_legacy");
        final BlockPos terminal = formed(helper, station);
        helper.runAfterDelay(20, () -> machineAt(helper, terminal).getInventory()
                .insertItem(0, new ItemStack(ChainParts.MEMORY_CHIPS.get(1).get()), false));
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(machineAt(helper, terminal).formed(), "the station stands formed");
            helper.assertTrue(!holds(machineAt(helper, terminal), ChainParts.CONTROLLER_CHIPS.get(1).get(), 1),
                    "with no computer linked to its peripheral port, it exposes nothing");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void assembly_worksItsErasBenchRecipes(final GameTestHelper helper) {
        final ChainMachine line = machine("assembly_vintage");
        final BlockPos terminal = formed(helper, line);
        helper.runAfterDelay(20, () -> {
            machineAt(helper, terminal).getInventory().insertItem(0,
                    new ItemStack(ChainParts.MEMORY_CHIPS.getFirst().get(), 2), false);
            machineAt(helper, terminal).getInventory().insertItem(1,
                    new ItemStack(ChainParts.ETCHED_BOARDS.getFirst().get()), false);
        });
        helper.succeedWhen(() -> helper.assertTrue(holds(machineAt(helper, terminal),
                ChainParts.CERAMIC_PACKAGE.get(), 1), "the Vintage line assembles the Vintage bench's part"));
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void terminal_breaksIntoTheMachineTerminal(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("grinder"));
        helper.runAfterDelay(20, () -> {
            helper.getLevel().destroyBlock(helper.absolutePos(terminal), true);
            helper.assertItemEntityPresent(ChainMachineBlocks.TERMINAL.get().asItem(), terminal, 2.0);
            helper.succeed();
        });
    }

    private static ChainMachine machine(final String id) {
        return ChainMachines.byId(id).orElseThrow();
    }

    /* Builds {@code machine} and makes its Machine Terminal its own; its terminal's place. */
    private static BlockPos formed(final GameTestHelper helper, final ChainMachine machine) {
        final BlockPos terminal = build(helper, machine);
        helper.assertTrue(ChainMachines.formAt(helper.getLevel(), helper.absolutePos(terminal), Direction.NORTH),
                machine.id() + " forms");
        return terminal;
    }

    /* Lays {@code machine}'s blocks at {@link #CORNER} facing north, the Machine Terminal at its place; that place. */
    private static BlockPos build(final GameTestHelper helper, final ChainMachine machine) {
        BlockPos terminal = CORNER;
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final String row = layers.get(y).get(z);
                for (int x = 0; x < row.length(); x++) {
                    final BlockPos at = CORNER.offset(x, y, z);
                    final char letter = row.charAt(x);
                    if (letter == '#') {
                        terminal = at;
                        helper.setBlock(at, ChainMachineBlocks.TERMINAL.get().defaultBlockState()
                                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    } else {
                        final Block block = ChainMachineBlocks.byId(machine.legend().get(letter)).get();
                        final BlockState state = block.defaultBlockState();
                        helper.setBlock(at, state.hasProperty(HorizontalDirectionalBlock.FACING)
                                ? state.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH) : state);
                    }
                }
            }
        }
        return terminal;
    }

    /* The place of the first block of {@code id} in {@code machine}'s shape, as {@link #build} lays it. */
    private static BlockPos place(final ChainMachine machine, final String id) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final String row = layers.get(y).get(z);
                for (int x = 0; x < row.length(); x++) {
                    if (id.equals(machine.legend().get(row.charAt(x)))) {
                        return CORNER.offset(x, y, z);
                    }
                }
            }
        }
        throw new IllegalArgumentException(machine.id() + " has no " + id);
    }

    private static void clear(final GameTestHelper helper) {
        for (int y = 0; y < LONGEST; y++) {
            for (int z = 0; z < LONGEST; z++) {
                for (int x = 0; x < LONGEST; x++) {
                    helper.setBlock(CORNER.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static ChainMachineBlockEntity machineAt(final GameTestHelper helper, final BlockPos terminal) {
        return (ChainMachineBlockEntity) helper.getBlockEntity(terminal);
    }

    /* Whether the machine's outputs hold at least {@code count} of {@code item}. */
    private static boolean holds(final ChainMachineBlockEntity machine, final Item item, final int count) {
        int found = 0;
        for (int slot = 0; slot < machine.getInventory().getSlots(); slot++) {
            final ItemStack stack = machine.getInventory().getStackInSlot(slot);
            if (stack.is(item) && machine.layout().isOutput(slot)) {
                found += stack.getCount();
            }
        }
        return found >= count;
    }
}
