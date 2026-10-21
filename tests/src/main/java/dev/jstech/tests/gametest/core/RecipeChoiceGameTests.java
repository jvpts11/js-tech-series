/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.recipe.RecipeChoices;
import dev.jstech.core.registry.CoreAttachments;
import dev.jstech.tests.JsTests;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Choosing between recipes that overlap: two barriers fit two of the test mod's crafting recipes, and a barrier two
 * of its smelting recipes. At the crafting grid the player is told both fit, the result follows the one they choose,
 * and the same grid gives it again; a recipe they were not told of is refused. A furnace set to the recipe the game
 * would not have used smelts by it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RecipeChoiceGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos FURNACE = new BlockPos(1, 1, 1);
    private static final int GRID_WINDOW = 4201;
    private static final int FURNACE_WINDOW = 4202;
    private static final int FIRST_CELL = 1;
    private static final int SECOND_CELL = 2;
    private static final int RESULT = 0;

    private RecipeChoiceGameTests() {
    }

    @GameTest(template = ARENA)
    public static void grid_makesTheChosenRecipeAndKeepsIt(final GameTestHelper helper) {
        final FakePlayer player = player(helper, "grid");
        player.setData(CoreAttachments.RECIPE_CHOICES, Map.of());
        final CraftingMenu menu = new CraftingMenu(GRID_WINDOW, player.getInventory(), ContainerLevelAccess.create(
                helper.getLevel(), helper.absolutePos(BlockPos.ZERO)));
        player.containerMenu = menu;
        fill(menu, new ItemStack(Items.BARRIER));
        final List<ResourceLocation> offered = RecipeChoices.offered(player);
        helper.assertTrue(offered.size() == 2, "the player is told both recipes fit; got " + offered);
        final Item before = menu.getSlot(RESULT).getItem().getItem();
        final ResourceLocation other = offered.stream().filter(id -> !resultOf(helper, id).is(before)).findFirst()
                .orElseThrow();
        helper.assertFalse(RecipeChoices.choose(player, GRID_WINDOW, ResourceLocation.fromNamespaceAndPath(
                JsTests.MODID, "change/kept")), "a recipe the player was not told of is refused");
        helper.assertTrue(RecipeChoices.choose(player, GRID_WINDOW, other), "the other recipe is taken");
        final Item chosen = resultOf(helper, other).getItem();
        helper.assertTrue(menu.getSlot(RESULT).getItem().is(chosen), "the result follows the choice; got "
                + menu.getSlot(RESULT).getItem());
        fill(menu, ItemStack.EMPTY);
        helper.assertTrue(menu.getSlot(RESULT).getItem().isEmpty(), "an empty grid makes nothing");
        fill(menu, new ItemStack(Items.BARRIER));
        helper.assertTrue(menu.getSlot(RESULT).getItem().is(chosen), "and the same grid gives the chosen result "
                + "again; got " + menu.getSlot(RESULT).getItem());
        player.containerMenu = player.inventoryMenu;
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void furnace_smeltsByTheRecipeItWasSetTo(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(FURNACE, Blocks.FURNACE);
        final FurnaceBlockEntity furnace = (FurnaceBlockEntity) helper.getBlockEntity(FURNACE);
        final FakePlayer player = player(helper, "furnace");
        final FurnaceMenu menu = new FurnaceMenu(FURNACE_WINDOW, player.getInventory(), furnace,
                new SimpleContainerData(AbstractFurnaceMenu.DATA_COUNT));
        player.containerMenu = menu;
        furnace.setItem(0, new ItemStack(Items.BARRIER));
        RecipeChoices.lookAtFurnace(player);
        final List<ResourceLocation> offered = RecipeChoices.offered(player);
        helper.assertTrue(offered.size() == 2, "the player is told both smelting recipes fit; got " + offered);
        final ResourceLocation usual = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,
                new SingleRecipeInput(new ItemStack(Items.BARRIER)), level).map(RecipeHolder::id).orElseThrow();
        final ResourceLocation other = offered.stream().filter(id -> !id.equals(usual)).findFirst().orElseThrow();
        helper.assertTrue(RecipeChoices.choose(player, FURNACE_WINDOW, other), "the other recipe is taken");
        helper.assertTrue(other.equals(furnace.getData(CoreAttachments.FURNACE_CHOICES).get(
                ResourceLocation.withDefaultNamespace("barrier"))), "and the furnace keeps it for the barrier");
        player.containerMenu = player.inventoryMenu;
        final Item made = resultOf(helper, other).getItem();
        furnace.setItem(1, new ItemStack(Items.COAL));
        helper.succeedWhen(() -> helper.assertTrue(furnace.getItem(2).is(made),
                "the furnace smelts the barrier by the recipe it was set to; it holds " + furnace.getItem(2)));
    }

    private static FakePlayer player(final GameTestHelper helper, final String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(("jstests recipe "
                + name).getBytes(StandardCharsets.UTF_8)), "jstests_recipes_" + name));
    }

    private static void fill(final CraftingMenu menu, final ItemStack stack) {
        menu.getSlot(FIRST_CELL).set(stack.copy());
        menu.getSlot(SECOND_CELL).set(stack.copy());
    }

    private static ItemStack resultOf(final GameTestHelper helper, final ResourceLocation recipe) {
        return helper.getLevel().getRecipeManager().byKey(recipe).orElseThrow().value()
                .getResultItem(helper.getLevel().registryAccess());
    }
}
