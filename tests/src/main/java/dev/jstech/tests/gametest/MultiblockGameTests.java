/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.IMatchResult;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestFurnaceBlockEntity;
import dev.jstech.tests.TestMultiblocks;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The Core's multiblocks: a pattern with ports declared in code, written to its file and read back; one read from a
 * datapack's file alone, with a tag slot; one read from a structure block's file; and a formed structure whose ports
 * pass items and energy to its controller while its plain parts offer nothing.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MultiblockGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(2, 2, 2);

    private MultiblockGameTests() {
    }

    @GameTest(template = ARENA)
    public static void pattern_readsBackFromTheFileItIsWrittenTo(final GameTestHelper helper) {
        final JsonElement file = MultiblockPatterns.CODEC.encodeStart(JsonOps.INSTANCE, TestMultiblocks.FURNACE)
                .getOrThrow();
        final MultiblockPattern back = MultiblockPatterns.CODEC.parse(JsonOps.INSTANCE, file).getOrThrow();
        helper.assertTrue(back.sizeX() == 3 && back.sizeZ() == 3 && back.rows(0)[0].equals("PIP"),
                "the layers come back as written; got " + back.rows(0)[0]);
        helper.assertTrue(back.ports().equals(TestMultiblocks.FURNACE.ports()),
                "the ports come back by their letters; got " + back.ports());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void pattern_isReadFromADatapacksFileAlone(final GameTestHelper helper) {
        final Optional<MultiblockPattern> tower = MultiblockPatterns.get(
                ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_tower"));
        helper.assertTrue(tower.isPresent(), "the tower is read from its file, with no declaration in code");
        helper.assertTrue(tower.get().ports().get('S') == PortKind.FLUID_INPUT, "its ports come with it");
        // The tower stands on logs of any kind, which its tag slot fits, and stone bricks beside the controller.
        final BlockPos at = CONTROLLER.above();
        helper.setBlock(at.west(), Blocks.STONE_BRICKS);
        helper.setBlock(at.east(), Blocks.STONE_BRICKS);
        helper.setBlock(at, Blocks.FURNACE);
        helper.setBlock(at.above().west(), Blocks.OAK_LOG);
        helper.setBlock(at.above(), Blocks.BIRCH_LOG);
        helper.setBlock(at.above().east(), Blocks.SPRUCE_LOG);
        final IMatchResult match = MultiblockPatterns.match(tower.get(), helper.getLevel(), helper.absolutePos(at));
        helper.assertTrue(match instanceof IMatchResult.Success, "logs of three kinds fit the tag; got " + match);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void structure_readsAsAPattern(final GameTestHelper helper) {
        // A structure block's file of a 3x1x1 row: casing, controller, casing, and air left free above it.
        final CompoundTag structure = new CompoundTag();
        structure.put("size", ints(3, 2, 1));
        final ListTag palette = new ListTag();
        palette.add(named("jstests:test_furnace_part"));
        palette.add(named("jstests:test_furnace_controller"));
        palette.add(named("minecraft:air"));
        structure.put("palette", palette);
        final ListTag blocks = new ListTag();
        blocks.add(block(0, 0, 0, 0));
        blocks.add(block(1, 0, 0, 1));
        blocks.add(block(2, 0, 0, 0));
        blocks.add(block(1, 1, 0, 2));
        structure.put("blocks", blocks);
        final MultiblockPattern pattern = MultiblockPatterns.fromStructure("row", structure,
                "jstests:test_furnace_controller", Map.of("jstests:test_furnace_part", PortKind.ITEM_INPUT));
        helper.assertTrue(pattern.controllerX() == 1 && pattern.controllerY() == 0,
                "the named block is the controller");
        helper.assertTrue(pattern.charAt(1, 1, 0) == MultiblockPattern.IGNORE_CHAR, "air is left free");
        helper.assertTrue(pattern.ports().get(pattern.charAt(0, 0, 0)) == PortKind.ITEM_INPUT,
                "the part is a port of the kind named for its block");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void structure_rejectsABadPaletteIndexOrPosition(final GameTestHelper helper) {
        for (final int[] bad : new int[][] {{0, 0, 0, 9}, {5, 0, 0, 0}}) {
            final CompoundTag structure = new CompoundTag();
            structure.put("size", ints(3, 2, 1));
            final ListTag palette = new ListTag();
            palette.add(named("jstests:test_furnace_part"));
            structure.put("palette", palette);
            final ListTag blocks = new ListTag();
            blocks.add(block(bad[0], bad[1], bad[2], bad[3]));
            structure.put("blocks", blocks);
            boolean refused = false;
            try {
                MultiblockPatterns.fromStructure("bad", structure, "jstests:test_furnace_controller", Map.of());
            } catch (final IllegalArgumentException expected) {
                refused = expected.getMessage().contains("bad");
            }
            helper.assertTrue(refused, "a block outside the palette or the size is refused naming the structure");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void declare_rejectsAnIdTheGameCannotRead(final GameTestHelper helper) {
        final MultiblockPattern pattern = MultiblockPattern.builder("bad_tag").layer("#A")
                .where('A', BlockMatch.tag("Not A Tag")).build();
        boolean refused = false;
        try {
            MultiblockPatterns.declare(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "bad_tag"), pattern);
        } catch (final IllegalArgumentException expected) {
            refused = true;
        }
        helper.assertTrue(refused, "a tag that is not an id is refused when the pattern is declared");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ports_passToTheControllerThroughThePartsTheyMark(final GameTestHelper helper) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    helper.setBlock(CONTROLLER.offset(dx, 0, dz), TestMultiblocks.PART.get());
                }
            }
        }
        helper.setBlock(CONTROLLER, TestMultiblocks.CONTROLLER.get());
        final TestFurnaceBlockEntity furnace = helper.getBlockEntity(CONTROLLER);
        final IMatchResult match = furnace.form();
        helper.assertTrue(match instanceof IMatchResult.Success success && success.ports().size() == 3,
                "the casing forms with three ports; got " + match);
        final ServerLevel level = helper.getLevel();
        final IItemHandler input = level.getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(CONTROLLER.north()), null);
        helper.assertTrue(input != null, "the north part takes items in");
        input.insertItem(0, new ItemStack(Items.RAW_IRON, 4), false);
        helper.assertTrue(furnace.slots().getStackInSlot(TestFurnaceBlockEntity.INPUT).getCount() == 4,
                "what goes into the port lands in the controller's input");
        helper.assertTrue(level.getCapability(Capabilities.ItemHandler.BLOCK,
                        helper.absolutePos(CONTROLLER.east()), null) == null,
                "a plain part of the casing offers nothing");
        final IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                helper.absolutePos(CONTROLLER.west()), null);
        helper.assertTrue(energy != null && energy.receiveEnergy(100, false) == 100
                        && furnace.energy().getEnergyStored() == 100,
                "the west part takes energy into the controller");
        furnace.slots().setStackInSlot(TestFurnaceBlockEntity.OUTPUT, new ItemStack(Items.IRON_INGOT, 2));
        final IItemHandler output = level.getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(CONTROLLER.south()), null);
        helper.assertTrue(output != null && output.extractItem(0, 1, false).is(Items.IRON_INGOT),
                "the south part gives out what the controller made");
        helper.succeed();
    }

    private static ListTag ints(final int... values) {
        final ListTag list = new ListTag();
        for (final int value : values) {
            list.add(IntTag.valueOf(value));
        }
        return list;
    }

    private static CompoundTag named(final String id) {
        final CompoundTag entry = new CompoundTag();
        entry.putString("Name", id);
        return entry;
    }

    private static CompoundTag block(final int x, final int y, final int z, final int state) {
        final CompoundTag block = new CompoundTag();
        block.put("pos", ints(x, y, z));
        block.putInt("state", state);
        return block;
    }
}
