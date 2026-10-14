/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The datapack examples of {@code computers/docs/MODPACKS.md}, kept as the test mod's own data so the page never shows
 * one that does not work: a recipe whose result is an installer, named by its data components, and a loot modifier
 * that puts one Vintage thing in every dungeon chest.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ModpackDataGameTests {

    private static final String ARENA = "empty";
    private static final ResourceLocation PLAIN_RECIPE = ResourceLocation.fromNamespaceAndPath("jstests",
            "vintage_personal_computer_from_data");
    private static final ResourceLocation INSTALLER_RECIPE = ResourceLocation.fromNamespaceAndPath("jstests",
            "frames_95_installer_from_data");
    private static final TagKey<Item> VINTAGE = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "era/vintage"));

    private ModpackDataGameTests() {
    }

    @GameTest(template = ARENA)
    public static void datapackRecipe_makesAPlainItemByItsId(final GameTestHelper helper) {
        final Optional<RecipeHolder<?>> recipe = helper.getLevel().getRecipeManager().byKey(PLAIN_RECIPE);
        helper.assertTrue(recipe.isPresent(), "the datapack's plain recipe loads");
        final ItemStack made = recipe.get().value().getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(made.is(ComputingModule.VINTAGE_PERSONAL_COMPUTER.get().asItem()),
                "it makes a Vintage Personal Computer; got " + made);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void datapackRecipe_namesAnInstallerByItsComponents(final GameTestHelper helper) {
        final Optional<RecipeHolder<?>> recipe = helper.getLevel().getRecipeManager().byKey(INSTALLER_RECIPE);
        helper.assertTrue(recipe.isPresent(), "the datapack's installer recipe loads");
        final ItemStack made = recipe.get().value().getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(ItemStack.isSameItemSameComponents(made, InstallerStacks.forSystem(
                        ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_95"))),
                "it makes the same installer the creative tab holds; got " + made);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void lootModifier_putsAVintageThingInADungeonChest(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final LootTable dungeon = level.getServer().reloadableRegistries()
                .getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
        final LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .create(LootContextParamSets.CHEST);
        for (int chest = 0; chest < 5; chest++) {
            final List<ItemStack> loot = dungeon.getRandomItems(params);
            helper.assertTrue(loot.stream().filter(stack -> stack.is(VINTAGE)).count() == 1,
                    "every dungeon chest holds one Vintage thing beside its own loot; got " + loot);
        }
        helper.succeed();
    }
}
