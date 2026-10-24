/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.machine.DataMachine;
import dev.jstech.core.machine.DataMachineBlockEntity;
import dev.jstech.core.machine.DataMachines;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.multiblock.PatternById;
import dev.jstech.core.multiblock.ShapeMatch;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMultiblocks;
import dev.jstech.tests.TestStructures;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Multiblock machines declared as data: the baker's file gives it its shape, its slots, its energy, its recipes and
 * its three at once, and it bakes clay through ports of its own; a file that names a pattern by its id forms on that
 * pattern; a terminal that names a file there is not never forms and says so; and a machine's file reads back as it
 * was written.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DataMachineGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos LOW = new BlockPos(1, 1, 1);
    private static final BlockPos HIGH = new BlockPos(3, 3, 3);
    private static final BlockPos TERMINAL = new BlockPos(2, 2, 1);
    private static final BlockPos WEST = new BlockPos(1, 2, 2);
    private static final BlockPos EAST = new BlockPos(3, 2, 2);
    private static final BlockPos SOUTH = new BlockPos(2, 2, 3);

    private DataMachineGameTests() {
    }

    @GameTest(template = ARENA)
    public static void baker_formsAsItsFileSaysAndBakesThroughItsPorts(final GameTestHelper helper) {
        box(helper, TestStructures.BAKER.get());
        helper.setBlock(WEST, TestStructures.ITEM_INPUT.get());
        helper.setBlock(EAST, TestStructures.ITEM_OUTPUT.get());
        helper.setBlock(SOUTH, TestStructures.ENERGY_INPUT.get());
        helper.startSequence()
                .thenWaitUntil(() -> {
                    final DataMachineBlockEntity baker = machine(helper);
                    helper.assertTrue(baker.machine().isPresent(), "the baker reads its file");
                    helper.assertTrue(baker.structure().formed(), "and forms as the file's box says");
                    helper.assertTrue(baker.recipesAtOnce() == 3, "three at once, as the file says; got "
                            + baker.recipesAtOnce());
                })
                .thenExecute(() -> {
                    final IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,
                            helper.absolutePos(SOUTH), Direction.SOUTH);
                    helper.assertTrue(energy != null && energy.receiveEnergy(5_000, false) > 0,
                            "it takes energy as its file allows");
                    final IItemHandler in = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                            helper.absolutePos(WEST), Direction.WEST);
                    helper.assertTrue(in != null && in.insertItem(0, new ItemStack(Items.CLAY_BALL, 6), false)
                            .isEmpty(), "clay goes in through the input port");
                })
                .thenWaitUntil(() -> {
                    final IItemHandler out = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                            helper.absolutePos(EAST), Direction.EAST);
                    helper.assertTrue(out != null && out.extractItem(0, 64, true).is(Items.BRICK),
                            "the bricks of the file's kind of recipe come out");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void dataFurnace_formsOnThePatternItsFileNamesByItsId(final GameTestHelper helper) {
        final Block part = TestMultiblocks.PART.get();
        final BlockPos at = new BlockPos(2, 2, 2);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x != 0 || z != 0) {
                    helper.setBlock(at.offset(x, 0, z), part);
                }
            }
        }
        helper.setBlock(at, TestStructures.DATA_FURNACE.get());
        helper.succeedWhen(() -> {
            final DataMachineBlockEntity furnace = (DataMachineBlockEntity) helper.getBlockEntity(at);
            helper.assertTrue(furnace.machine().orElseThrow().shape() instanceof PatternById,
                    "its file names the test furnace's pattern by its id");
            helper.assertTrue(furnace.structure().formed(), "and it forms on that pattern");
        });
    }

    @GameTest(template = ARENA)
    public static void terminal_ofAFileThereIsNotNeverFormsAndSaysSo(final GameTestHelper helper) {
        helper.setBlock(TERMINAL, TestStructures.NO_MACHINE.get());
        helper.runAfterDelay(10, () -> {
            final DataMachineBlockEntity none = machine(helper);
            helper.assertTrue(none.machine().isEmpty(), "no file is found for it");
            helper.assertTrue(none.status() == MachineStatus.NOT_FORMED, "it stands not formed; got "
                    + none.status());
            final ShapeMatch.Failure failure = none.structure().failure().orElseThrow();
            helper.assertTrue(failure.wanted().equals(PatternById.MISSING)
                    && failure.found().equals("jstests:nowhere"), "and names the file it lacks; got " + failure);
            final List<Text> lines = new ArrayList<>();
            none.describe(lines, false);
            helper.assertTrue(lines.stream().anyMatch(line -> line.english()
                    .equals("Its shape, the pattern jstests:nowhere, is in no file")),
                    "whoever looks at it is told which file it lacks; got " + lines);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void machineFile_readsBackAsItWasWritten(final GameTestHelper helper) {
        final DataMachine baker = DataMachines.get(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "baker"))
                .orElseThrow();
        final JsonElement file = DataMachines.CODEC.encodeStart(JsonOps.INSTANCE, baker).getOrThrow();
        final DataMachine back = DataMachines.CODEC.parse(JsonOps.INSTANCE, file).getOrThrow();
        helper.assertTrue(back.equals(baker), "the machine comes back whole; got " + back);
        final JsonElement twoShapes = file.deepCopy();
        twoShapes.getAsJsonObject().getAsJsonObject("shape").addProperty("pattern", "jstests:test_furnace");
        helper.assertTrue(DataMachines.CODEC.parse(JsonOps.INSTANCE, twoShapes).error().isPresent(),
                "a file naming two shapes is refused");
        helper.succeed();
    }

    /* A box of iron edges and glass faces, the terminal in the middle of its north face. */
    private static void box(final GameTestHelper helper, final Block terminal) {
        for (int x = LOW.getX(); x <= HIGH.getX(); x++) {
            for (int y = LOW.getY(); y <= HIGH.getY(); y++) {
                for (int z = LOW.getZ(); z <= HIGH.getZ(); z++) {
                    final int bounds = (x == LOW.getX() || x == HIGH.getX() ? 1 : 0)
                            + (y == LOW.getY() || y == HIGH.getY() ? 1 : 0)
                            + (z == LOW.getZ() || z == HIGH.getZ() ? 1 : 0);
                    final BlockPos at = new BlockPos(x, y, z);
                    if (bounds >= 2) {
                        helper.setBlock(at, Blocks.IRON_BLOCK);
                    } else if (bounds == 1) {
                        helper.setBlock(at, Blocks.GLASS);
                    }
                }
            }
        }
        helper.setBlock(TERMINAL, terminal);
    }

    private static DataMachineBlockEntity machine(final GameTestHelper helper) {
        if (helper.getBlockEntity(TERMINAL) instanceof DataMachineBlockEntity machine) {
            return machine;
        }
        throw new IllegalStateException("no data machine where the test put it");
    }
}
