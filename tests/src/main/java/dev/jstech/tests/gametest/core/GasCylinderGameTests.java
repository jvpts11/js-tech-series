/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SharedCatalogue;
import dev.jstech.core.jstechseries.cylinder.GasCylinders;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.MaterialValues;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Gas Cylinder: made at the Crafting Table from seven steel plates and a bronze ingot; one to a stack, holding
 * 10,000 mB of one gas; its shoulder painted by the European code for a gas the code names, in the gas's own colour
 * for one it does not, and bare steel when empty.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class GasCylinderGameTests {

    private static final String ARENA = "empty";
    private static final long HOLDS = 10_000L;
    private static final int OPAQUE = 0xFF << 24;
    private static final int HEX = 16;

    private GasCylinderGameTests() {
    }

    @GameTest(template = ARENA)
    public static void cylinder_isMadeOfSevenSteelPlatesAndABronzeIngot(final GameTestHelper helper) {
        final ItemStack plate = new ItemStack(GasCylinders.STEEL_PLATE.get());
        final ItemStack valve = new ItemStack(GasCylinders.BRONZE_INGOT.get());
        final List<ItemStack> grid = new ArrayList<>(List.of(plate, valve, plate, plate, ItemStack.EMPTY, plate,
                plate, plate, plate));
        final ItemStack made = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                CraftingInput.of(3, 3, grid), helper.getLevel()).map(recipe -> recipe.value().assemble(
                CraftingInput.of(3, 3, grid), helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
        helper.assertTrue(made.is(GasCylinders.CYLINDER.get()), "the plates round a bronze valve make a cylinder: "
                + made);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cylinder_holdsTenThousandMillibucketsOfOneGasOneToAStack(final GameTestHelper helper) {
        final ItemStack cylinder = new ItemStack(GasCylinders.CYLINDER.get());
        helper.assertTrue(cylinder.getMaxStackSize() == 1, "one cylinder to a stack");
        final IMatterHandler tank = cylinder.getCapability(MatterCapabilities.GAS_ITEM);
        helper.assertTrue(tank != null && tank.tanks() == 1 && tank.capacity(0) == HOLDS,
                "it holds 10,000 mB: " + (tank == null ? "no tank" : tank.capacity(0)));
        final long taken = tank.insert(new MatterStack(Elements.of(Element.CHLORINE), HOLDS * 2), false);
        helper.assertTrue(taken == HOLDS, "no more than it holds: " + taken);
        helper.assertTrue(tank.insert(new MatterStack(Elements.of(Element.ARGON), 1), true) == 0,
                "and one gas at a time");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void shoulder_followsTheEuropeanCodeOrTheGasesOwnColour(final GameTestHelper helper) {
        final GasCylinders.Shoulders code = GasCylinders.SHOULDERS.get();
        helper.assertTrue(GasCylinders.shoulderOf(new ItemStack(GasCylinders.CYLINDER.get())) == code.empty(),
                "an empty cylinder shows its bare steel");
        helper.assertTrue(GasCylinders.shoulderOf(filled(Elements.of(Element.CHLORINE))) == code.chlorine(),
                "chlorine's is yellow");
        helper.assertTrue(GasCylinders.shoulderOf(filled(SharedCatalogue.CARBON_DIOXIDE)) == code.carbonDioxide(),
                "carbon dioxide's grey, its name read in two words");
        final int own = MaterialValues.value(SharedCatalogue.AMMONIA, MaterialProperties.COLOUR, true)
                .map(colour -> OPAQUE | Integer.parseInt(colour.substring(1), HEX)).orElse(0);
        helper.assertTrue(GasCylinders.shoulderOf(filled(SharedCatalogue.AMMONIA)) == own,
                "ammonia, which the code does not name, shows its own colour");
        helper.succeed();
    }

    private static ItemStack filled(final Material gas) {
        final ItemStack cylinder = new ItemStack(GasCylinders.CYLINDER.get());
        cylinder.getCapability(MatterCapabilities.GAS_ITEM).insert(new MatterStack(gas, HOLDS), false);
        return cylinder;
    }
}
