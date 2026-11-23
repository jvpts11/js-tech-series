/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.block.part.ReceivingBusPart;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.machine.WaferWork;
import dev.jstech.computers.crafting.CraftPlanner;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.computers.crafting.NetworkProcessingOperation;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.PatternWorkbench;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.ChainMachineSite;
import dev.jstech.tests.testkit.computers.CraftingRig;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A machine-pattern input that lasts several runs, as an ion implanter's dopant dust lasts eight layers: it is kept in
 * the machine, topped up only when the machine holds none, and planned once for every few runs it lasts.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class KeptInputGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    /* The implanter stands clear of the standard crafting network, its front west corner here, its front north. */
    private static final BlockPos IMPLANTER_CORNER = new BlockPos(2, 1, 5);
    private static final int LASTS = 8;

    private KeptInputGameTests() {
    }

    /** A kept input keeps its runs through a pattern file; a file written before inputs could last reads as one run. */
    @GameTest(template = ARENA)
    public static void pattern_aKeptInputKeepsItsRunsInAFile(final GameTestHelper helper) {
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
        final ProcessingPattern.ProcessingInput kept =
                new ProcessingPattern.ProcessingInput(StorageKey.of(new ItemStack(Items.REDSTONE)), 1, false, LASTS);
        final Tag written = ProcessingPattern.ProcessingInput.CODEC.encodeStart(ops, kept).getOrThrow();
        helper.assertTrue(ProcessingPattern.ProcessingInput.CODEC.parse(ops, written).getOrThrow().equals(kept),
                "the file gives the input back lasting eight runs");
        final CompoundTag old = ((CompoundTag) written).copy();
        old.remove("lasts");
        helper.assertTrue(ProcessingPattern.ProcessingInput.CODEC.parse(ops, old).getOrThrow().lasts() == 1,
                "an older file's input lasts one run, as every input did");
        helper.assertTrue(kept.lasting(0).lasts() == 1 && kept.lasting(1 << 20).lasts()
                        == ProcessingPattern.ProcessingInput.MOST_LASTS, "the runs are held between 1 and the most");
        helper.assertTrue(kept.need(9) == 2 && kept.lasting(1).need(9) == 9,
                "nine runs take two of an input lasting eight, nine of one lasting one");
        helper.succeed();
    }

    /** Three steps that each keep the same input take one of it between them, as the machine keeps it between jobs. */
    @GameTest(template = ARENA)
    public static void planner_stepsOfOneKeptInputTakeOneBetweenThem(final GameTestHelper helper) {
        final List<ProcessingPattern> kept = chain(LASTS);
        final Map<StorageKey, Long> stock = Map.of(key(Items.COBBLESTONE), 64L, key(Items.REDSTONE), 64L);
        final CraftPlanner.Plan one = CraftPlanner.plan(key(Items.STONE_BRICKS), 1, List.of(), kept, stock);
        helper.assertTrue(one.feasible() && one.rawConsumption().get(key(Items.REDSTONE)) == 1L,
                "one brick takes one redstone over its three steps; took " + one.rawConsumption());
        final CraftPlanner.Plan many = CraftPlanner.plan(key(Items.STONE_BRICKS), 16, List.of(), kept, stock);
        helper.assertTrue(many.rawConsumption().get(key(Items.REDSTONE)) == 6L,
                "sixteen bricks, forty-eight runs, take six; took " + many.rawConsumption());
        final CraftPlanner.Plan each = CraftPlanner.plan(key(Items.STONE_BRICKS), 1, List.of(), chain(1), stock);
        helper.assertTrue(each.rawConsumption().get(key(Items.REDSTONE)) == 3L,
                "fed every run, it takes one a step; took " + each.rawConsumption());
        final CraftPlanner.Plan scarce = CraftPlanner.plan(key(Items.STONE_BRICKS), 1, List.of(), kept,
                Map.of(key(Items.COBBLESTONE), 1L, key(Items.REDSTONE), 1L));
        helper.assertTrue(scarce.feasible(), "a single redstone is enough for one brick; missing " + scarce.missing());
        helper.succeed();
    }

    /**
     * The interface reads the machine rather than counting what it fed: a dust put in by hand serves the first layer,
     * and once the implanter has used it one more is put in to wait, never one a run.
     */
    @GameTest(template = ARENA, timeoutTicks = 900)
    public static void engine_aKeptInputIsToppedUpOnlyWhenTheMachineHasNone(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final BlockPos terminal = ChainMachineSite.formed(helper, ChainMachineSite.machine("ion_implanter"),
                IMPLANTER_CORNER);
        final BlockPos inlet = IMPLANTER_CORNER;
        final BlockPos outlet = IMPLANTER_CORNER.south();
        final CraftingFloor.Site feeding = new CraftingFloor.Site(inlet.north(), Direction.SOUTH);
        final CraftingFloor.Site receiving = new CraftingFloor.Site(outlet.west(), Direction.EAST);
        CraftingRig.lay(world, new BlockPos(5, 2, 3), new BlockPos(5, 2, 4), new BlockPos(4, 2, 4),
                new BlockPos(3, 2, 4), new BlockPos(2, 2, 4), feeding.cable(), feeding.cable().west(),
                receiving.cable().north(), receiving.cable());
        final CraftingInterfacePart part = CraftingRig.addPart(world, feeding,
                new CraftingInterfacePart(HardwareEra.STANDARD));
        CraftingRig.addPart(world, receiving, new ReceivingBusPart());
        final Item dust = MaterialUses.item(Elements.of(Element.PHOSPHORUS), Forms.DUST).get();
        final ProcessingPattern implant = new ProcessingPattern(
                List.of(new ProcessingPattern.ProcessingInput(StorageKey.of(wafer(WaferWork.Step.IMPLANT)), 1),
                        new ProcessingPattern.ProcessingInput(key(dust), 1, false, LASTS)),
                List.of(new ProcessingPattern.ProcessingOutput(StorageKey.of(wafer(WaferWork.Step.DEPOSITION)), 1,
                        ProcessingPattern.FULL_CHANCE)), 400);
        final AtomicReference<NetworkProcessingOperation> op = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.rack().getServerStorage(0).insert(StorageKey.of(wafer(WaferWork.Step.IMPLANT)), 2);
                    net.rack().getServerStorage(0).insert(key(dust), 16);
                    final IItemHandler in = ChainMachineSite.machineAt(helper, terminal).getInventory();
                    in.insertItem(1, new ItemStack(dust), false);
                    helper.assertTrue(part.place(NetworkRecipe.ofProcessing(implant)), "the interface takes it");
                    net.cc().forgetFloor();
                })
                .thenExecuteAfter(SETTLE, () -> op.set(net.mainframe().submitNetworkProcessing(implant, 2, "kept")))
                .thenWaitUntil(() -> helper.assertTrue(op.get().isDone(), "the two layers are done"))
                .thenExecute(() -> {
                    helper.assertTrue(op.get().produced() == 2, "both wafers came back: " + op.get().produced());
                    final long left = net.storage(helper.getLevel()).count(key(dust));
                    helper.assertTrue(left == 15, "one dust left the network, to wait for the ninth layer, not one a"
                            + " layer; 16 became " + left);
                    final IItemHandler in = ChainMachineSite.machineAt(helper, terminal).getInventory();
                    int held = 0;
                    for (int slot = 0; slot < in.getSlots(); slot++) {
                        held += in.getStackInSlot(slot).is(dust) ? in.getStackInSlot(slot).getCount() : 0;
                    }
                    helper.assertTrue(held == 1, "the implanter keeps that one; it holds " + held);
                })
                .thenSucceed();
    }

    /** The Studio's machine draft makes an input it is told lasts several runs a kept one, and keeps that saved. */
    @GameTest(template = ARENA)
    public static void studio_anInputSetToLastSeveralRunsIsKept(final GameTestHelper helper) {
        final PatternWorkbench studio = new PatternWorkbench();
        studio.setProcCell(false, 0, new PatternWorkbench.DataCell(key(Items.REDSTONE), 1, false));
        studio.setProcCell(true, 0, new PatternWorkbench.DataCell(key(Items.STONE), 1, false));
        studio.setInputLasts(0, LASTS);
        helper.assertTrue(studio.processingPattern().inputs().getFirst().lasts() == LASTS, "the pattern keeps it");
        final CompoundTag saved = new CompoundTag();
        studio.save(saved, helper.getLevel().registryAccess());
        final PatternWorkbench reloaded = new PatternWorkbench();
        reloaded.load(saved, helper.getLevel().registryAccess());
        helper.assertTrue(reloaded.inputLasts(0) == LASTS, "the draft keeps it through a save");
        studio.setProcCell(false, 0, null);
        helper.assertTrue(studio.inputLasts(0) == 1, "a cleared cell lasts one run again");
        studio.setInputLasts(0, LASTS);
        helper.assertTrue(studio.inputLasts(0) == 1, "an empty cell takes no runs");
        helper.succeed();
    }

    /* Cobblestone to stone to smooth stone to bricks, each step taking a redstone that lasts {@code lasts} runs. */
    private static List<ProcessingPattern> chain(final int lasts) {
        final Item[] steps = {Items.COBBLESTONE, Items.STONE, Items.SMOOTH_STONE, Items.STONE_BRICKS};
        final List<ProcessingPattern> out = new ArrayList<>();
        for (int i = 0; i < steps.length - 1; i++) {
            out.add(new ProcessingPattern(
                    List.of(new ProcessingPattern.ProcessingInput(key(steps[i]), 1),
                            new ProcessingPattern.ProcessingInput(key(Items.REDSTONE), 1, false, lasts)),
                    List.of(new ProcessingPattern.ProcessingOutput(key(steps[i + 1]), 1,
                            ProcessingPattern.FULL_CHANCE)), 200));
        }
        return out;
    }

    private static StorageKey key(final Item item) {
        return StorageKey.of(new ItemStack(item));
    }

    /* A Vintage memory wafer on its first layer, going to {@code step}. */
    private static ItemStack wafer(final WaferWork.Step step) {
        final ItemStack wafer = new ItemStack(ChainChips.waferInProcess(ChainParts.ERAS.getFirst()).get());
        wafer.set(ComputingComponents.WAFER_WORK.get(), new WaferWork(
                BuiltInRegistries.ITEM.getKey(ChainParts.MEMORY_CHIPS.getFirst().get()), 1,
                2, step));
        return wafer;
    }
}
