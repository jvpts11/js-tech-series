/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.item.CraftingCardItem;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.CraftingFixtures.placeComputerWithCard;
import static dev.jstech.tests.testkit.CraftingFixtures.planksPattern;
import static dev.jstech.tests.testkit.CraftingFixtures.sticksPattern;

/**
 * GameTests for the bench recipes a Crafting Card keeps in its ROM: the cap per era, the round trip
 * through NBT, and how patterns compare.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RecipeRomGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private RecipeRomGameTests() {
    }

    /** The bench recipes live in the cards' ROM: a card keeps as many as its era allows, and a second adds room. */
    @GameTest(template = ARENA)
    public static void recipeRom_capsAtTheCardsRom(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = placeComputerWithCard(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final int size = computer.romSize();
                    helper.assertTrue(size == CraftingCardItem.romSize(
                                    new ItemStack(ComputingModule.CRAFTING_CARD_T2.get())),
                            "one card's ROM is the computer's whole ROM; got " + size);
                    for (int i = 1; i <= size; i++) {
                        helper.assertTrue(computer.loadPattern(planksPattern(i)), "pattern " + i + " fits");
                    }
                    helper.assertFalse(computer.loadPattern(sticksPattern()), "one more is refused");
                    helper.assertTrue(computer.romUsed() == size, "the ROM sits exactly at its size");
                    computer.getHardware().setStackInSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START + 2,
                            new ItemStack(ComputingModule.CRAFTING_CARD_T2.get()));
                    helper.assertTrue(computer.romSize() == 2 * size, "a second card doubles the ROM");
                    helper.assertTrue(computer.loadPattern(sticksPattern()), "the refused pattern now fits");
                    helper.assertFalse(computer.loadPattern(planksPattern(1)), "a pattern already kept is refused");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void recipeRom_persistsThroughNbtRoundTrip(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = placeComputerWithCard(helper, new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    computer.loadPattern(planksPattern(1));
                    computer.loadPattern(sticksPattern());
                    final CompoundTag saved = computer.saveWithFullMetadata(helper.getLevel().registryAccess());

                    final CraftingComputerBlockEntity reloaded = new CraftingComputerBlockEntity(
                            computer.getBlockPos(), computer.getBlockState());
                    reloaded.loadWithComponents(saved, helper.getLevel().registryAccess());
                    helper.assertTrue(reloaded.romUsed() == 2, "the ROM survives the NBT round-trip");
                    helper.assertTrue(reloaded.romContains(sticksPattern()),
                            "the reloaded ROM still holds the same recipes");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void craftingPattern_comparesByValueNotIdentity(final GameTestHelper helper) {
        final CraftingPattern a = planksPattern(4);
        final CraftingPattern b = planksPattern(4);
        helper.assertTrue(a.equals(b), "two patterns holding the same recipe must be equal");
        helper.assertTrue(a.hashCode() == b.hashCode(), "equal patterns must share a hash code");
        helper.assertFalse(a.equals(planksPattern(8)), "a different result count is a different pattern");
        helper.assertFalse(a.equals(sticksPattern()), "a different recipe is not equal");
        helper.succeed();
    }
}
