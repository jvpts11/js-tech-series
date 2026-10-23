/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.machine.Byproduct;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.machine.ProcessingRecipeBuilder;
import dev.jstech.core.machine.RecipeConditions;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.TestKitMachine.KitMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The whole recipe format, read from files: a recipe that takes an item and a gas, needs a catalyst it does not use
 * up, makes items, a gas and a byproduct, and runs only above a pressure; one that waits without its catalyst; one
 * held back by a temperature its machine is not at; and a recipe of every part written and read back.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RecipeFormatGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MACHINE = new BlockPos(4, 2, 4);
    private static final long HYDROGEN = 500L;

    private RecipeFormatGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void recipe_takesGasLeavesTheCatalystAndMakesAByproductAndGas(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = machine(helper);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        machine.getInventory().setStackInSlot(1, new ItemStack(Items.BLAZE_ROD));
        hydrogen(machine);
        helper.succeedWhen(() -> {
            helper.assertTrue(machine.getInventory().getStackInSlot(0).isEmpty(), "the ingot is taken: "
                    + machine.status());
            helper.assertTrue(machine.getInventory().getStackInSlot(1).is(Items.BLAZE_ROD),
                    "the catalyst is not used up");
            helper.assertTrue(made(machine, Items.IRON_NUGGET) == 9, "nine nuggets are made");
            helper.assertTrue(made(machine, Items.GOLD_NUGGET) == 1, "and the byproduct that always comes");
            helper.assertTrue(gas(machine, true) == HYDROGEN - 100L, "a hundred of hydrogen is taken");
            helper.assertTrue(gas(machine, false) == 50L, "fifty of oxygen made");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void recipe_waitsWithoutItsCatalyst(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = machine(helper);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        hydrogen(machine);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(machine.status() == MachineStatus.WAITING && made(machine, Items.IRON_NUGGET) == 0,
                    "without its catalyst it does not start: " + machine.status());
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void recipe_heldBackByATemperatureItsMachineIsNotAt(final GameTestHelper helper) {
        final KitMachineBlockEntity machine = machine(helper);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.COPPER_INGOT));
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(made(machine, Items.COPPER_BLOCK) == 0 && machine.status() == MachineStatus.WAITING,
                    "at the overworld's air, a recipe for a hundred kelvin waits: " + machine.status());
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void recipe_ofEveryPartIsWrittenAndReadBack(final GameTestHelper helper) {
        final ProcessingRecipe built = ProcessingRecipeBuilder.of(TestKitMachine.REFINING)
                .input(Items.IRON_INGOT, 2).catalyst(Ingredient.of(Items.BLAZE_ROD), 1)
                .matterInput(Elements.of(Element.HYDROGEN), 100L).output(Items.IRON_NUGGET, 9)
                .matterOutput(Elements.of(Element.OXYGEN), 50L).byproduct(new ItemStack(Items.GOLD_NUGGET), 0.25F)
                .countedByproduct(new ItemStack(Items.FLINT), 1.5F)
                .conditions(new RecipeConditions(300.0, 900.0, 50.0, Double.MAX_VALUE)).ticks(30).build();
        final Codec<ProcessingRecipe> codec = TestKitMachine.REFINING.serializer().codec().codec();
        final JsonElement file = codec.encodeStart(JsonOps.INSTANCE, built).getOrThrow();
        final ProcessingRecipe back = codec.parse(JsonOps.INSTANCE, file).getOrThrow();
        helper.assertTrue(back.catalysts().size() == 1 && back.matterInputs().equals(built.matterInputs())
                        && back.matterOutputs().equals(built.matterOutputs())
                        && back.byproducts().getFirst().chance() == 0.25F
                        && back.byproducts().getFirst().draw() == Byproduct.Draw.CHANCE
                        && back.byproducts().get(1).draw() == Byproduct.Draw.SHARE
                        && back.byproducts().get(1).amount() == 1.5F
                        && back.conditions().equals(built.conditions()),
                "every part comes back as written: " + file);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void byproduct_comesByAChanceOrAShareAndNeverBoth(final GameTestHelper helper) {
        final JsonElement both = JsonParser.parseString("{\"item\": {\"id\": \"minecraft:flint\"}, \"chance\": 0.5,"
                + " \"share\": 0.5}");
        final JsonElement neither = JsonParser.parseString("{\"item\": {\"id\": \"minecraft:flint\"}}");
        helper.assertTrue(Byproduct.CODEC.parse(JsonOps.INSTANCE, both).isError()
                && Byproduct.CODEC.parse(JsonOps.INSTANCE, neither).isError(),
                "a byproduct with both a chance and a share, or neither, is refused");
        helper.assertTrue(Byproduct.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"item\": {\"id\": \"minecraft:flint\"}, \"share\": 2.5}")).getOrThrow().draw()
                == Byproduct.Draw.SHARE, "a share above one is more than one each time");
        helper.succeed();
    }

    private static KitMachineBlockEntity machine(final GameTestHelper helper) {
        helper.setBlock(MACHINE, TestKitMachine.BLOCK.get());
        final KitMachineBlockEntity machine = (KitMachineBlockEntity) helper.getLevel().getBlockEntity(
                helper.absolutePos(MACHINE));
        machine.power().portStore().setStored(TestKitMachine.CAPACITY);
        return machine;
    }

    private static void hydrogen(final KitMachineBlockEntity machine) {
        machine.matter().inputs(MatterState.GAS).getFirst().insert(new MatterStack(Elements.of(Element.HYDROGEN),
                HYDROGEN), false);
    }

    /* How many of {@code item} its outputs hold. */
    private static int made(final KitMachineBlockEntity machine, final Item item) {
        int count = 0;
        for (int slot = TestKitMachine.OUT; slot < TestKitMachine.OUT + 2; slot++) {
            final ItemStack held = machine.getInventory().getStackInSlot(slot);
            if (held.is(item)) {
                count += held.getCount();
            }
        }
        return count;
    }

    /* What its gas input, or its gas output, holds. */
    private static long gas(final KitMachineBlockEntity machine, final boolean input) {
        return (input ? machine.matter().inputs(MatterState.GAS) : machine.matter().outputs(MatterState.GAS))
                .getFirst().held().map(MatterStack::amount).orElse(0L);
    }
}
