/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.JsCore;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesAlloys;
import dev.jstech.core.jstechseries.SeriesBronze;
import dev.jstech.core.item.ItemGasTank;
import dev.jstech.core.jstechseries.SeriesChemicals;
import dev.jstech.core.jstechseries.SharedCatalogue;
import dev.jstech.core.jstechseries.cylinder.GasCylinders;
import dev.jstech.core.jstechseries.cylinder.GasMarking;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.MaterialValues;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestVehicles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Gas Cylinder: made at the Crafting Table from seven steel plates and a bronze ingot, or seven iron ingots in a
 * world where no mod makes steel plates; one to a stack, holding
 * 10,000 mB of one gas; its shoulder painted by the European code for a gas the code names, in the gas's own colour
 * for one it does not, and bare steel when empty.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class GasCylinderGameTests {

    private static final String ARENA = "empty";
    private static final long HOLDS = 10_000L;
    private static final long STOCKED = 15_000L;
    /* A height of open sky, over every test's arena. */
    private static final double OPEN_SKY = 300.0;
    private static final int OPAQUE = 0xFF << 24;
    private static final int HEX = 16;

    private GasCylinderGameTests() {
    }

    /**
     * J's Computers makes steel plates, so in this world the cylinder is of steel; its iron recipe, for a world where
     * no mod makes steel plates, does not stand.
     */
    @GameTest(template = ARENA)
    public static void cylinder_isMadeOfSteelPlatesWhereAModMakesThemAndNotOfIron(final GameTestHelper helper) {
        final ItemStack valve = new ItemStack(SeriesBronze.BRONZE_INGOT.get());
        final ItemStack plate = new ItemStack(MaterialUses.item(SeriesAlloys.STEEL, Forms.PLATE).get());
        helper.assertTrue(crafted(helper, plate, valve).is(GasCylinders.CYLINDER.get()),
                "the steel plates round a bronze valve make a cylinder");
        helper.assertTrue(crafted(helper, new ItemStack(Items.IRON_INGOT), valve).isEmpty(),
                "with steel plates in the world, iron ingots make none");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                JsCore.MODID, "gas_cylinder_from_iron")).isEmpty(), "the iron recipe does not stand");
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

    /**
     * An empty cylinder marked for argon fills from argon alone, keeps its mark once emptied, and is painted for argon
     * all along; a gas had no name in a pattern until a machine had made it into a cylinder.
     */
    @GameTest(template = ARENA)
    public static void marked_fillsOnlyWithItsGasAndKeepsItsMarkEmpty(final GameTestHelper helper) {
        final BlockPos chlorineAt = new BlockPos(1, 2, 1);
        final BlockPos argonAt = new BlockPos(3, 2, 1);
        helper.setBlock(chlorineAt, TestVehicles.PUMP.get());
        helper.setBlock(argonAt, TestVehicles.PUMP.get());
        final IMatterHandler chlorine = gasAt(helper, chlorineAt);
        final IMatterHandler argon = gasAt(helper, argonAt);
        chlorine.insert(new MatterStack(Elements.of(Element.CHLORINE), STOCKED), false);
        argon.insert(new MatterStack(Elements.of(Element.ARGON), STOCKED), false);
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(
                "gas_marked".getBytes(StandardCharsets.UTF_8)), "gas_marked"));
        final ItemStack marked = GasCylinders.marked(Elements.of(Element.ARGON));
        helper.assertTrue(GasCylinders.shoulderOf(marked) == GasCylinders.shoulder(Elements.of(Element.ARGON))
                .orElseThrow(), "marked for argon, its shoulder is argon's");
        player.setItemInHand(InteractionHand.MAIN_HAND, marked);
        use(helper, player, chlorineAt);
        final IMatterHandler cylinder = player.getMainHandItem().getCapability(MatterCapabilities.GAS_ITEM);
        helper.assertTrue(heldOf(cylinder) == 0 && amountIn(chlorine) == STOCKED,
                "marked for argon, it takes no chlorine: " + heldOf(cylinder));
        helper.assertTrue(cylinder.insert(new MatterStack(Elements.of(Element.CHLORINE), HOLDS), true) == 0,
                "and refuses chlorine poured in, wherever from");
        use(helper, player, argonAt);
        helper.assertTrue(heldOf(cylinder) == HOLDS, "it fills with argon: " + heldOf(cylinder));
        player.setShiftKeyDown(true);
        use(helper, player, argonAt);
        helper.assertTrue(heldOf(cylinder) == 0 && ItemGasTank.markOf(player.getMainHandItem())
                .equals(Optional.of(Elements.of(Element.ARGON))), "emptied, it keeps its mark");
        helper.succeed();
    }

    /** Used in the air while sneaking, an empty marked cylinder loses its mark, and is bare steel again. */
    @GameTest(template = ARENA)
    public static void marked_sneakingInTheAirClearsTheMark(final GameTestHelper helper) {
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(
                "gas_clear".getBytes(StandardCharsets.UTF_8)), "gas_clear"));
        // High over the test world, where nothing stands over its head for the look to meet.
        final Vec3 at = helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        player.moveTo(at.x, OPEN_SKY, at.z, 0.0F, -90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, GasCylinders.marked(Elements.of(Element.CHLORINE)));
        player.setShiftKeyDown(true);
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(ItemGasTank.markOf(player.getMainHandItem()).isEmpty(),
                "sneaking and used looking up into the air, its mark comes off");
        helper.assertTrue(GasCylinders.shoulderOf(player.getMainHandItem())
                == GasCylinders.SHOULDERS.get().empty(), "and its shoulder is bare steel again");
        helper.succeed();
    }

    /** The list a cylinder is marked from holds every gas of the Core, each a gas at room temperature. */
    @GameTest(template = ARENA)
    public static void marking_listsEveryGasOfTheCore(final GameTestHelper helper) {
        final List<Material> gases = GasMarking.gases();
        helper.assertTrue(gases.stream().allMatch(gas -> gas.roomState() == MatterState.GAS),
                "only gases: " + gases);
        helper.assertTrue(gases.containsAll(List.of(Elements.of(Element.HYDROGEN), Elements.of(Element.CHLORINE),
                Elements.of(Element.ARGON), SeriesChemicals.HYDROGEN_CHLORIDE)), "the chain's gases among them");
        helper.succeed();
    }

    /* What the Crafting Table makes of {@code side} round the cylinder's sides and bottom, {@code valve} on top. */
    private static ItemStack crafted(final GameTestHelper helper, final ItemStack side, final ItemStack valve) {
        final CraftingInput input = CraftingInput.of(3, 3, new ArrayList<>(List.of(side, valve, side, side,
                ItemStack.EMPTY, side, side, side, side)));
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
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
