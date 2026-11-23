/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
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
import dev.jstech.tests.TestVehicles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
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
    private static final long STOCKED = 15_000L;
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

    /**
     * Used on a block that gives gas it fills with the block's gas, and only with the gas it already holds; used while
     * sneaking on a block that takes gas it empties into it.
     */
    @GameTest(template = ARENA)
    public static void use_fillsFromABlockAndSneakingEmptiesIntoOne(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos chlorineAt = new BlockPos(1, 2, 1);
        final BlockPos argonAt = new BlockPos(3, 2, 1);
        helper.setBlock(chlorineAt, TestVehicles.PUMP.get());
        helper.setBlock(argonAt, TestVehicles.PUMP.get());
        final IMatterHandler chlorine = gasAt(helper, chlorineAt);
        final IMatterHandler argon = gasAt(helper, argonAt);
        chlorine.insert(new MatterStack(Elements.of(Element.CHLORINE), STOCKED), false);
        argon.insert(new MatterStack(Elements.of(Element.ARGON), STOCKED), false);
        final ServerPlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(
                "gas_cylinder".getBytes(StandardCharsets.UTF_8)), "gas_cylinder"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GasCylinders.CYLINDER.get()));
        use(helper, player, chlorineAt);
        final IMatterHandler cylinder = player.getMainHandItem().getCapability(MatterCapabilities.GAS_ITEM);
        helper.assertTrue(heldOf(cylinder) == HOLDS && amountIn(chlorine) == STOCKED - HOLDS,
                "used on the chlorine it fills with chlorine: " + heldOf(cylinder));
        player.setShiftKeyDown(true);
        use(helper, player, chlorineAt);
        helper.assertTrue(heldOf(cylinder) == 0 && amountIn(chlorine) == STOCKED,
                "used sneaking it empties back: " + heldOf(cylinder));
        player.setShiftKeyDown(false);
        use(helper, player, chlorineAt);
        helper.assertTrue(cylinder.extract(Elements.of(Element.CHLORINE), HOLDS / 2, false) == HOLDS / 2,
                "filled again, half of it let out");
        use(helper, player, argonAt);
        helper.assertTrue(amountIn(argon) == STOCKED && cylinder.stack(0).map(MatterStack::material)
                .orElse(null) == Elements.of(Element.CHLORINE), "with chlorine in it, it takes no argon");
        helper.succeed();
    }

    private static void use(final GameTestHelper helper, final ServerPlayer player, final BlockPos relative) {
        final BlockPos at = helper.absolutePos(relative);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at).add(0, 0.5, 0), Direction.UP, at, false));
    }

    private static IMatterHandler gasAt(final GameTestHelper helper, final BlockPos relative) {
        return helper.getLevel().getCapability(MatterCapabilities.GAS, helper.absolutePos(relative), Direction.UP);
    }

    private static long amountIn(final IMatterHandler handler) {
        return handler.stack(0).map(MatterStack::amount).orElse(0L);
    }

    private static long heldOf(final IMatterHandler cylinder) {
        return amountIn(cylinder);
    }

    private static ItemStack filled(final Material gas) {
        final ItemStack cylinder = new ItemStack(GasCylinders.CYLINDER.get());
        cylinder.getCapability(MatterCapabilities.GAS_ITEM).insert(new MatterStack(gas, HOLDS), false);
        return cylinder;
    }
}
