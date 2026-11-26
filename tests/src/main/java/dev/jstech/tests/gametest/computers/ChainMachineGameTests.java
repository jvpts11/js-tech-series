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

import com.mojang.authlib.GameProfile;
import dev.jstech.computers.audio.ChainSounds;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.machine.ChainFormedBlock;
import dev.jstech.computers.chain.machine.ChainInletNotes;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.chain.machine.ChainMachineTerminalBlock;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesChemicals;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.machine.MatterTanks;
import dev.jstech.core.machine.ProcessingMachineBlockEntity.Layout;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestComputerEnergy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
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

    /**
     * A water bucket used on the cell's Liquid Inlet empties into the cell and comes back empty; used on its Gas Outlet
     * it stays full. Either way the entry takes the click, so the bucket is never used on the world beside it: on the
     * Liquid Inlet it poured its water out by the machine.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void liquidInlet_aBucketEmptiesIntoItAndNeverPoursOut(final GameTestHelper helper) {
        final ChainMachine cell = machine("electrolytic_cell");
        final BlockPos terminal = formed(helper, cell);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).formed(), "the cell formed"))
                .thenExecute(() -> {
                    final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                            new GameProfile(UUID.randomUUID(), "bucket"));
                    final BlockPos inlet = helper.absolutePos(place(cell, "liquid_inlet"));
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
                    helper.assertTrue(useHeld(player, inlet).consumesAction(), "the inlet takes the bucket's click");
                    helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "the bucket comes back empty; holds "
                            + player.getMainHandItem());
                    final IFluidHandler tanks = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                            inlet, Direction.UP);
                    helper.assertTrue(tanks != null && held(tanks, Fluids.WATER) == FluidType.BUCKET_VOLUME,
                            "the cell holds the bucket's water");
                    final BlockPos outlet = helper.absolutePos(place(cell, "gas_outlet"));
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
                    helper.assertTrue(useHeld(player, outlet).consumesAction(), "the gas outlet takes the click too");
                    helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "and the bucket stays full");
                })
                .thenSucceed();
    }

    /**
     * An empty item inlet of the cell's screen says what it takes: the items of the cell's own recipes, and how the cell
     * makes its brine. A player read the inlet under the word BRINE as where the brine goes.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void cell_saysWhatItsItemInletsTake(final GameTestHelper helper) {
        final ChainMachine cell = machine("electrolytic_cell");
        final BlockPos terminal = formed(helper, cell);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).formed(), "the cell formed"))
                .thenExecute(() -> {
                    final List<ItemStack> takes = machineAt(helper, terminal).recipeItems();
                    helper.assertTrue(!takes.isEmpty(), "the cell's recipes take items");
                    helper.assertTrue(takes.stream().map(ItemStack::getItem).distinct().count() == takes.size(),
                            "each item once; got " + takes);
                    helper.assertTrue(machineAt(helper, terminal).inletNote().isPresent(),
                            "the cell says how it makes its brine");
                    helper.assertTrue(ChainInletNotes.of("rolling_mill").isEmpty(),
                            "a machine with nothing to add says nothing more");
                })
                .thenSucceed();
    }

    /* Uses what {@code player} holds on the top of the block at {@code pos}, as a right click does. */
    private static InteractionResult useHeld(final ServerPlayer player, final BlockPos pos) {
        return player.gameMode.useItemOn(player, player.serverLevel(), player.getMainHandItem(),
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos,
                        false));
    }

    /* How much of {@code fluid} the tanks of {@code handler} hold together. */
    private static int held(final IFluidHandler handler, final Fluid fluid) {
        int total = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            if (handler.getFluidInTank(tank).is(fluid)) {
                total += handler.getFluidInTank(tank).getAmount();
            }
        }
        return total;
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

    /*
     * Every recipe a machine works fits in it: its items and the tools it keeps there each in an input slot of their
     * own, what it makes in the outputs, its liquids and other matter in the tanks; a tool with no slot left (the Board
     * Etcher's phototool) would leave the recipe never made.
     */
    @GameTest(template = ARENA)
    public static void every_machineHoldsEveryRecipeItWorks(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        for (final ChainMachine machine : ChainMachines.all()) {
            final Layout layout = machine.layout();
            final int matterIn = layout.matter().stream().mapToInt(MatterTanks.MatterSpec::inputs).sum();
            final int matterOut = layout.matter().stream().mapToInt(MatterTanks.MatterSpec::outputs).sum();
            for (final ChainMachines.Worked worked : ChainMachines.workedBy(machine)) {
                for (final RecipeHolder<ProcessingRecipe> holder : helper.getLevel().getRecipeManager()
                        .getAllRecipesFor(worked.kind().type())) {
                    final ProcessingRecipe recipe = holder.value();
                    if (recipe.inputs().size() + recipe.catalysts().size() > layout.inputs()
                            || recipe.outputs().size() > layout.outputs()
                            || recipe.fluidInputs().size() > layout.inputTanks()
                            || recipe.fluidOutputs().size() > layout.outputTanks()
                            || recipe.matterInputs().size() > matterIn || recipe.matterOutputs().size() > matterOut) {
                        wrong.add(holder.id() + " in " + machine.id());
                    }
                }
            }
        }
        helper.assertTrue(wrong.isEmpty(), "every recipe fits in its machine: " + wrong);
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

    /* The Board Etcher holds the laminate and the phototool side by side, etches the board and keeps the phototool. */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void boardEtcher_etchesTheLaminateThroughItsPhototool(final GameTestHelper helper) {
        final ChainMachine etcher = machine("board_etcher");
        final BlockPos terminal = formed(helper, etcher);
        final Item phototool = ChainParts.PHOTOTOOLS.getFirst().get();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(machineAt(helper, terminal).formed(), "the etcher formed"))
                .thenExecute(() -> {
                    final IFluidHandler inlet = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                            helper.absolutePos(place(etcher, "liquid_inlet")), Direction.UP);
                    helper.assertTrue(inlet != null, "the liquid inlet opens onto the etcher");
                    inlet.fill(new FluidStack(MaterialLiquids.fluid(SeriesChemicals.FERRIC_CHLORIDE).get(),
                            FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
                    final IItemHandler slots = machineAt(helper, terminal).getInventory();
                    slots.insertItem(0, new ItemStack(ChainParts.FR2_LAMINATE.get()), false);
                    helper.assertTrue(slots.insertItem(1, new ItemStack(phototool), false).isEmpty(),
                            "the phototool has an inlet of its own");
                })
                .thenWaitUntil(() -> helper.assertTrue(holds(machineAt(helper, terminal),
                        ChainParts.ETCHED_BOARDS.getFirst().get(), 1), "the board is etched"))
                .thenExecute(() -> helper.assertTrue(machineAt(helper, terminal).getInventory().getStackInSlot(1)
                        .is(phototool), "the phototool stays, never used up"))
                .thenSucceed();
    }

    /*
     * A machine that makes only from exactly what a recipe takes still takes a lot lying over two slots, the rest of
     * an input that did not fit one stack: two coils' wire in two slots makes both coils.
     */
    @GameTest(template = ARENA, timeoutTicks = 1200)
    public static void coilWinder_takesALotLyingOverTwoSlots(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("coil_winder"));
        final Item wire = MaterialUses.item(Elements.of(Element.COPPER), Forms.WIRE).get();
        final Item pvc = MaterialUses.item(SeriesCompounds.PVC, Forms.SHEET).get();
        helper.runAfterDelay(20, () -> {
            final IItemHandler slots = machineAt(helper, terminal).getInventory();
            slots.insertItem(0, new ItemStack(wire, 4), false);
            slots.insertItem(1, new ItemStack(pvc, 2), false);
            slots.insertItem(2, new ItemStack(wire, 4), false);
        });
        helper.succeedWhen(() -> {
            final IItemHandler slots = machineAt(helper, terminal).getInventory();
            final List<String> held = new ArrayList<>();
            for (int slot = 0; slot < slots.getSlots(); slot++) {
                held.add(slots.getStackInSlot(slot).toString());
            }
            helper.assertTrue(holds(machineAt(helper, terminal), ChainParts.COPPER_COIL.get(), 2),
                    "two coils from the wire lying over two slots; it holds " + held);
        });
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
