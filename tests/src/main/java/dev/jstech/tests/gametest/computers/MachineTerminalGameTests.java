/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import static dev.jstech.tests.testkit.computers.ChainMachineSite.build;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machine;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machineAt;

import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Every machine of the chain has a terminal of its own (computers/docs/design/survival.md, "The machines"): the Plasma
 * Etcher's is the Plasma Etcher Terminal, in the chain's tab with the machine's blocks, made at a crafting table of the
 * Machine Terminal, which is a part now and no block, and one block of that machine its own; placed among the
 * machine's blocks it forms that machine, and broken it gives itself back.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineTerminalGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 20;

    private MachineTerminalGameTests() {
    }

    @GameTest(template = ARENA)
    public static void terminal_ofEachMachineIsItsOwnPlaceableItemInTheChainTab(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        for (final ChainMachine machine : ChainMachines.all()) {
            final Item terminal = ChainMachines.terminalOf(machine).get().asItem();
            final Era era = machine.era() == null ? HardwareEra.VINTAGE : machine.era();
            if (!ComputingContent.shelf(ComputingContent.Shelf.CHAIN, era).items().contains(terminal)) {
                wrong.add(machine.id());
            }
        }
        helper.assertTrue(wrong.isEmpty(), "every machine's terminal is in the chain's tab: " + wrong);
        helper.assertTrue(!(ChainParts.MACHINE_TERMINAL.get() instanceof BlockItem), "the Machine Terminal is a part,"
                + " no block");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void terminal_isMadeOfTheMachineTerminalAndOneBlockOfItsMachine(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        final Set<String> keys = new HashSet<>();
        for (final ChainMachine machine : ChainMachines.all()) {
            final Item terminal = ChainMachines.terminalOf(machine).get().asItem();
            final Item own = ChainMachineBlocks.byId(ChainMachines.keyBlock(machine)).get().asItem();
            final List<RecipeHolder<?>> made = new ArrayList<>();
            for (final RecipeHolder<?> recipe : helper.getLevel().getRecipeManager().getAllRecipesFor(
                    RecipeType.CRAFTING)) {
                if (recipe.value().getResultItem(helper.getLevel().registryAccess()).is(terminal)) {
                    made.add(recipe);
                }
            }
            final boolean right = made.size() == 1 && made.getFirst().value().getIngredients().size() == 2
                    && takes(made.getFirst().value().getIngredients(), ChainParts.MACHINE_TERMINAL.get())
                    && takes(made.getFirst().value().getIngredients(), own);
            if (!right) {
                wrong.add(machine.id() + " (" + made.size() + " recipes)");
            }
            if (!keys.add(ChainMachines.keyBlock(machine))) {
                wrong.add(machine.id() + ": its block " + ChainMachines.keyBlock(machine) + " is another's too");
            }
        }
        helper.assertTrue(wrong.isEmpty(), "each terminal of the Machine Terminal and its machine's own block: "
                + wrong);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void terminal_formsItsMachineAndBreaksBackIntoItself(final GameTestHelper helper) {
        final ChainMachine etcher = machine("plasma_etcher");
        final BlockPos terminal = build(helper, etcher);
        helper.succeedWhen(() -> {
            helper.assertTrue(machineAt(helper, terminal).formed(), "the etcher forms round its own terminal");
            helper.getLevel().destroyBlock(helper.absolutePos(terminal), true);
            final Item own = ChainMachines.terminalOf(etcher).get().asItem();
            final List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(helper.absolutePos(terminal)).inflate(2));
            helper.assertTrue(dropped.stream().anyMatch(item -> item.getItem().is(own)), "broken, it gives back the"
                    + " Plasma Etcher Terminal: " + dropped.stream().map(item -> BuiltInRegistries.ITEM.getKey(
                    item.getItem().getItem())).toList());
        });
    }

    private static boolean takes(final List<Ingredient> ingredients, final Item item) {
        return ingredients.stream().anyMatch(ingredient -> ingredient.test(item.getDefaultInstance()));
    }
}
