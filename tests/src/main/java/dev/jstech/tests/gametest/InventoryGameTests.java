/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.inventory.FaceConfig;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.inventory.ItemFilters;
import dev.jstech.core.inventory.StackSubject;
import dev.jstech.core.inventory.Transfers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Inventories in the Core: filters on the game's stacks (exact, fuzzy, by tag) and how they are saved, what each face
 * of a block lets through, turning with the block, and moving items, fluid and energy from one store to another,
 * into an item's own inventory among them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class InventoryGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos FROM = new BlockPos(1, 2, 1);
    private static final BlockPos TO = new BlockPos(3, 2, 1);

    private InventoryGameTests() {
    }

    @GameTest(template = ARENA)
    public static void stackSubject_equalStacksMakeEqualFilters(final GameTestHelper helper) {
        final ItemFilter first = ItemFilter.only(ItemFilters.exactly(new ItemStack(Items.IRON_SWORD), 0));
        final ItemFilter second = ItemFilter.only(ItemFilters.exactly(new ItemStack(Items.IRON_SWORD), 0));
        final ItemFilter other = ItemFilter.only(ItemFilters.exactly(new ItemStack(Items.GOLDEN_SWORD), 0));
        helper.assertTrue(first.equals(second) && first.hashCode() == second.hashCode(),
                "two filters built from equal stacks are equal");
        helper.assertTrue(!first.equals(other), "a filter of another item is not equal");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void filters_tellStacksApartAndComeBackFromTheSave(final GameTestHelper helper) {
        final ItemStack sword = new ItemStack(Items.IRON_SWORD);
        final ItemStack worn = new ItemStack(Items.IRON_SWORD);
        worn.set(DataComponents.DAMAGE, 40);

        helper.assertTrue(!ItemFilters.allows(ItemFilter.only(ItemFilters.exactly(sword, 0)), worn),
                "an exact rule tells a worn sword from a new one");
        helper.assertTrue(ItemFilters.allows(ItemFilter.only(new ItemFilter.Fuzzy("minecraft:iron_sword", 0)), worn),
                "a fuzzy one does not");
        helper.assertTrue(ItemFilters.allows(ItemFilter.only(new ItemFilter.Tag("minecraft:logs", 0)),
                new ItemStack(Items.OAK_LOG)), "the logs tag names oak");

        final ItemFilter filter = ItemFilter.allBut(ItemFilters.exactly(worn, 2),
                new ItemFilter.Fuzzy("minecraft:stone", 0), new ItemFilter.Tag("minecraft:logs", 16));
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        final Tag saved = ItemFilters.CODEC.encodeStart(ops, filter).getOrThrow();
        final ItemFilter loaded = ItemFilters.CODEC.parse(ops, saved).getOrThrow();
        helper.assertTrue(loaded.mode() == ItemFilter.Mode.ALL_BUT && loaded.rules().size() == 3,
                "the mode and the three rules came back: " + loaded);
        helper.assertTrue(loaded.amountFor(StackSubject.of(worn)) == 2
                        && !ItemFilters.allows(loaded, worn) && ItemFilters.allows(loaded, sword),
                "the exact rule came back with its components and its amount");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void faceConfig_letsEachFaceThroughAndTurnsWithTheBlock(final GameTestHelper helper) {
        final FaceConfig faces = new FaceConfig(FaceMode.BOTH, () -> { });
        faces.set(RelativeFace.BACK, FaceMode.NONE);
        faces.set(RelativeFace.LEFT, FaceMode.IN);
        final BlockState east = Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                Direction.EAST);
        final ItemStackHandler inner = new ItemStackHandler(2);
        inner.setStackInSlot(0, new ItemStack(Items.COAL, 5));

        helper.assertTrue(faces.handler(east, Direction.WEST, inner) == null,
                "the back of a block facing east is its west face, closed");
        final IItemHandler left = faces.handler(east, Direction.NORTH, inner);
        helper.assertTrue(left != null && left.insertItem(1, new ItemStack(Items.STONE), false).isEmpty(),
                "its left, the north face, takes in");
        helper.assertTrue(left.extractItem(0, 1, false).isEmpty(), "but gives nothing out");
        helper.assertTrue(faces.handler(east, Direction.UP, inner).extractItem(0, 1, false).getCount() == 1,
                "a face never set lets both through");

        final CompoundTag tag = new CompoundTag();
        faces.save(tag, helper.getLevel().registryAccess());
        final FaceConfig loaded = new FaceConfig(FaceMode.BOTH, () -> { });
        loaded.load(tag, helper.getLevel().registryAccess());
        helper.assertTrue(loaded.mode(RelativeFace.BACK) == FaceMode.NONE
                && loaded.mode(RelativeFace.LEFT) == FaceMode.IN && loaded.mode(RelativeFace.TOP) == FaceMode.BOTH,
                "each face came back from the save");
        helper.assertTrue(loaded.cycle(RelativeFace.LEFT) == FaceMode.OUT, "in goes on to out");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void moveItems_movesOnlyWhatPassesUpToTheMost(final GameTestHelper helper) {
        final IItemHandler from = chest(helper, FROM);
        final IItemHandler to = chest(helper, TO);
        from.insertItem(0, new ItemStack(Items.GOLD_INGOT, 5), false);
        from.insertItem(1, new ItemStack(Items.IRON_INGOT, 10), false);

        final int moved = Transfers.moveItems(from, to,
                ItemFilter.only(new ItemFilter.Fuzzy("minecraft:iron_ingot", 0)), 6);

        helper.assertTrue(moved == 6, "six iron moved: " + moved);
        helper.assertTrue(from.getStackInSlot(1).getCount() == 4 && from.getStackInSlot(0).getCount() == 5,
                "four iron and all the gold stayed");
        helper.assertTrue(to.getStackInSlot(0).is(Items.IRON_INGOT) && to.getStackInSlot(0).getCount() == 6,
                "the six are in the second chest");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void moveItems_fillsAnItemsOwnInventory(final GameTestHelper helper) {
        final IItemHandler from = chest(helper, FROM);
        from.insertItem(0, new ItemStack(Items.REDSTONE, 64), false);
        from.insertItem(1, new ItemStack(Items.REDSTONE, 64), false);
        final ItemStack tool = new ItemStack(TestItems.FULL.get());
        final IItemHandler held = tool.getCapability(Capabilities.ItemHandler.ITEM);

        final int moved = Transfers.moveItems(from, held, ItemFilter.EVERYTHING, 1_000);

        helper.assertTrue(moved == 128, "both stacks went into the tool's four slots: " + moved);
        helper.assertTrue(held.getStackInSlot(0).getCount() == 64 && held.getStackInSlot(1).getCount() == 64,
                "the tool holds them in its own inventory");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void moveFluidAndEnergy_moveWhatTheOtherSideTakes(final GameTestHelper helper) {
        final FluidTank source = new FluidTank(8_000);
        source.fill(new FluidStack(Fluids.LAVA, 5_000), IFluidHandler.FluidAction.EXECUTE);
        final FluidTank target = new FluidTank(3_000);
        final EnergyStorage cell = new EnergyStorage(10_000, 10_000, 10_000, 10_000);
        final EnergyStorage machine = new EnergyStorage(2_000, 500, 0, 0);

        helper.assertTrue(Transfers.moveFluid(source, target, 4_000) == 3_000, "the target takes three buckets");
        helper.assertTrue(source.getFluidAmount() == 2_000 && target.getFluid().is(Fluids.LAVA),
                "two stay behind and the lava arrived");
        helper.assertTrue(Transfers.moveEnergy(cell, machine, 1_000) == 500, "the machine takes 500 a time");
        helper.assertTrue(cell.getEnergyStored() == 9_500 && machine.getEnergyStored() == 500,
                "what left the cell is in the machine");
        helper.succeed();
    }

    private static IItemHandler chest(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, Blocks.CHEST);
        final IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(at), null);
        if (handler == null) {
            helper.fail("no chest at " + at);
        }
        return handler;
    }
}
