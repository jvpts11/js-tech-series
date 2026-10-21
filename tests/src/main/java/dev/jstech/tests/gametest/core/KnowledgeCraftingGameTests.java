/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.progression.KnowledgeGate;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestProgression;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The crafting recipes that ask for knowledge ({@code data/jstests/recipe/knowledge}): at a crafting table they make
 * nothing for a player whose team does not know Test Fusion and make their result once it does, shaped and shapeless
 * alike, and where no player is at the grid, as in a crafter block, they never match.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class KnowledgeCraftingGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos TABLE = new BlockPos(1, 1, 1);
    /** The crafter's own id, so no other test's player shares its team. */
    private static final UUID CRAFTER = UUID.nameUUIDFromBytes("jstests:knowledge_crafter".getBytes());
    /** The grid's places, after the result's. */
    private static final int FIRST = 1;

    private KnowledgeCraftingGameTests() {
    }

    @GameTest(template = ARENA)
    public static void shapeless_craftsOnlyOnceTheTeamKnows(final GameTestHelper helper) {
        crafts(helper, List.of(Items.DIRT, Items.GRAVEL, Items.DIRT), Items.EMERALD);
    }

    @GameTest(template = ARENA)
    public static void shaped_craftsOnlyOnceTheTeamKnows(final GameTestHelper helper) {
        crafts(helper, List.of(Items.GRAVEL, Items.DIRT), Items.LAPIS_LAZULI);
    }

    @GameTest(template = ARENA)
    public static void knowledgeRecipe_neverMatchesWithNoPlayerAtTheGrid(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MinecraftServer server = level.getServer();
        final String team = CoreTeams.teamOf(server, CRAFTER);
        final CraftingRecipe recipe = recipe(helper, "knowledge/shapeless").value();
        final CraftingInput input = CraftingInput.of(3, 1, List.of(new ItemStack(Items.DIRT),
                new ItemStack(Items.GRAVEL), new ItemStack(Items.DIRT)));
        TeamProgress.grant(server, team, TestProgression.FUSION, 1.0);
        try {
            helper.assertFalse(recipe.matches(input, level), "no player at the grid crafts nothing locked");
            final FakePlayer crafter = crafter(level);
            final boolean[] matched = {false};
            KnowledgeGate.craftingAs(crafter, () -> matched[0] = recipe.matches(input, level));
            helper.assertTrue(matched[0], "the player at the grid, whose team knows it, crafts it");
        } finally {
            TeamProgress.forget(server, team, TestProgression.FUSION.id());
        }
        helper.succeed();
    }

    /* Lays {@code grid} on a crafting table's grid, before and after the crafter's team learns Test Fusion. */
    private static void crafts(final GameTestHelper helper, final List<Item> grid, final Item made) {
        final ServerLevel level = helper.getLevel();
        final MinecraftServer server = level.getServer();
        final BlockPos table = helper.absolutePos(TABLE);
        level.setBlockAndUpdate(table, Blocks.CRAFTING_TABLE.defaultBlockState());
        final FakePlayer crafter = crafter(level);
        final String team = CoreTeams.teamOf(server, CRAFTER);
        TeamProgress.forget(server, team, TestProgression.FUSION.id());
        final CraftingMenu menu = new CraftingMenu(1, crafter.getInventory(), ContainerLevelAccess.create(level,
                table));
        lay(menu, grid);
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(), "nothing is made before the team knows; got "
                + menu.getSlot(0).getItem());
        TeamProgress.grant(server, team, TestProgression.FUSION, 1.0);
        try {
            lay(menu, grid);
            helper.assertTrue(menu.getSlot(0).getItem().is(made), "the result once the team knows; got "
                    + menu.getSlot(0).getItem());
        } finally {
            TeamProgress.forget(server, team, TestProgression.FUSION.id());
        }
        helper.succeed();
    }

    private static void lay(final CraftingMenu menu, final List<Item> grid) {
        for (int place = 0; place < grid.size(); place++) {
            menu.getSlot(FIRST + place).set(new ItemStack(grid.get(place)));
        }
    }

    private static FakePlayer crafter(final ServerLevel level) {
        return FakePlayerFactory.get(level, new GameProfile(CRAFTER, "jstests_crafter"));
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<CraftingRecipe> recipe(final GameTestHelper helper, final String path) {
        return (RecipeHolder<CraftingRecipe>) helper.getLevel().getServer().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path)).orElseThrow();
    }
}
