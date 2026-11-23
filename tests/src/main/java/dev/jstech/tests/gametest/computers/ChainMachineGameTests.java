/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import static dev.jstech.tests.testkit.computers.ChainMachineSite.CORNER;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.build;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.clear;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.formed;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.holds;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machine;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machineAt;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.place;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.stands;

import dev.jstech.computers.audio.ChainSounds;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.machine.ChainFormedBlock;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.chain.machine.ChainMachineTerminalBlock;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestComputerEnergy;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The chain's machines (computers/docs/design/survival.md): each built of its blocks round its own terminal, which
 * forms it once the shape stands whole and alone forms nothing; formed, its entries draw nothing and its plain
 * blocks merge into its model; it works its recipes, taking through its inlets and giving through its outlets, drawing
 * its energy through its power entry (the Coke Oven none); broken, its terminal gives itself back; a
 * Legacy Lithography Station waits for its computer, and the ArF one works two eras' recipes at each era's draw; and an
 * Electronics Assembly Line works its era's bench recipes.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ChainMachineGameTests {

    private static final String ARENA = "empty";
    private static final String ENERGY = TestComputerEnergy.BATCH;

    private ChainMachineGameTests() {
    }

    @GameTest(template = ARENA)
    public static void every_machineFormsFromItsBlocks(final GameTestHelper helper) {
        for (final ChainMachine machine : ChainMachines.all()) {
            final BlockPos terminal = build(helper, machine);
            helper.assertTrue(stands(helper, machine, terminal), machine.id() + " stands round its own terminal");
            helper.assertTrue(helper.getBlockState(terminal).getBlock() instanceof ChainMachineTerminalBlock own
                    && own.machine().equals(machine.id()), machine.id() + "'s terminal is its own");
            clear(helper);
        }
        helper.succeed();
    }

    /* Every machine of the chain but the Prebuilder, whose sounds follow its own steps, runs a loop of its own. */
    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void every_machineRunsALoopOfItsOwnWhileItWorks(final GameTestHelper helper) {
        for (final ChainMachine machine : ChainMachines.all()) {
            if (machine.id().equals("prebuilder")) {
                continue;
            }
            final Optional<SoundKey> work = ChainSounds.work(machine.id());
            helper.assertTrue(work.isPresent(), machine.id() + " has a loop to run while it works");
            final String file = "/assets/jsc/sounds/" + work.get().id().getPath() + ".ogg";
            helper.assertTrue(ChainSounds.class.getResource(file) != null, machine.id() + "'s loop is a file: " + file);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void crusher_runsItsLoopOnlyWhileItWorks(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("crusher"));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).formed(), "the crusher formed"))
                .thenExecute(() -> {
                    helper.assertTrue(machineAt(helper, terminal).loops().isEmpty(), "an idle crusher is quiet");
                    machineAt(helper, terminal).getInventory().insertItem(0, new ItemStack(ChainParts.FERRITE.get()),
                            false);
                })
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).loops().stream().anyMatch(loop
                        -> loop.sound().equals(ChainSounds.work("crusher").orElseThrow())), "working, the crusher "
                        + "runs its loop"))
                .thenWaitUntil(() -> helper.assertTrue(holds(machineAt(helper, terminal), ChainParts.INDUCTOR.get(), 2)
                        && machineAt(helper, terminal).loops().isEmpty(), "done, it falls quiet again"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void terminal_formsNothingAlone(final GameTestHelper helper) {
        final ChainMachine grinder = machine("grinder");
        helper.setBlock(CORNER, ChainMachines.terminalOf(grinder).get().defaultBlockState());
        helper.assertTrue(!stands(helper, grinder, CORNER), "no machine stands round it");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(!machineAt(helper, CORNER).formed(), "alone, the Grinder Terminal forms nothing");
            helper.succeed();
        });
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
        final BlockPos terminal = formed(helper, machine("crusher"));
        helper.runAfterDelay(20, () -> machineAt(helper, terminal).getInventory()
                .insertItem(0, new ItemStack(ChainParts.FERRITE.get()), false));
        helper.succeedWhen(() -> helper.assertTrue(holds(machineAt(helper, terminal), ChainParts.INDUCTOR.get(), 2),
                "the ferrite crushed into two inductors"));
    }

    /*
     * Raw zinc gives its crushed ore and three byproducts, indium, germanium and sulfur, each by its chance: the work
     * starts only when all of it fits at once, and the Crusher has an output for each. With two it never began.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void crusher_crushesZincWithRoomForItsThreeByproducts(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("crusher"));
        helper.runAfterDelay(20, () -> machineAt(helper, terminal).getInventory().insertItem(0, new ItemStack(
                MaterialUses.item(Elements.of(Element.ZINC), Forms.RAW).get()), false));
        helper.succeedWhen(() -> helper.assertTrue(holds(machineAt(helper, terminal), MaterialUses.item(
                Elements.of(Element.ZINC), Forms.CRUSHED).get(), 2), "the raw zinc crushed into two crushed zinc"));
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

    /** The Air Separation Unit takes nothing in: it works on the air round it, which no inlet gives. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void airSeparation_separatesTheAirRoundIt(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("air_separation_unit"));
        helper.succeedWhen(() -> {
            long argon = 0L;
            for (final MatterTank tank : machineAt(helper, terminal).matter().outputs(MatterState.GAS)) {
                argon += tank.held().filter(held -> held.material().equals(Elements.of(Element.ARGON)))
                        .map(MatterStack::amount).orElse(0L);
            }
            helper.assertTrue(argon >= 9, "a step of the air round it gives nine millibuckets of argon; " + argon);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = ENERGY)
    public static void cokeOven_burnsWithNoEnergy(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("coke_oven"));
        helper.runAfterDelay(20, () -> machineAt(helper, terminal).getInventory()
                .insertItem(0, new ItemStack(ChainParts.TRANSFORMER.get()), false));
        helper.succeedWhen(() -> helper.assertTrue(holds(machineAt(helper, terminal), ChainParts.FERRITE.get(), 1),
                "it burns part of its own coal: no energy asked"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void lithography_legacyWaitsForItsComputer(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("lithography_legacy"));
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
        final BlockPos terminal = formed(helper, machine("assembly_vintage"));
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
    public static void terminal_breaksIntoItsOwnTerminal(final GameTestHelper helper) {
        final ChainMachine grinder = machine("grinder");
        final BlockPos terminal = formed(helper, grinder);
        helper.runAfterDelay(20, () -> {
            helper.getLevel().destroyBlock(helper.absolutePos(terminal), true);
            helper.assertItemEntityPresent(ChainMachines.terminalOf(grinder).get().asItem(), terminal, 2.0);
            helper.succeed();
        });
    }
}
