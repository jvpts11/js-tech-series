/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.multipart.Microblock;
import dev.jstech.core.multipart.MicroblockRecipes;
import dev.jstech.core.multipart.MicroblockShowcase;
import dev.jstech.tests.JsTests;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * What the recipe viewers show of the microblocks' rules is what the crafting grid makes: each rule is one entry, its
 * frames go through every block microblocks are cut from, and every frame of a few blocks, crafted in the grid, makes
 * exactly what it shows.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MicroblockShowcaseGameTests {

    private static final String ARENA = "empty";
    /* A few blocks whose every frame is crafted: stone, a plank and glass. */
    private static final Set<Block> CRAFTED = Set.of(Blocks.STONE, Blocks.OAK_PLANKS, Blocks.GLASS);
    private static final int CUTS_A_BLOCK = 6;
    private static final int JOINS_A_BLOCK = 4;

    private MicroblockShowcaseGameTests() {
    }

    @GameTest(template = ARENA)
    public static void frames_goThroughEveryBlockOnceForEachWay(final GameTestHelper helper) {
        final int blocks = MicroblockShowcase.materials().size();
        helper.assertTrue(blocks > CRAFTED.size(), "many blocks are cut from, got " + blocks);
        helper.assertTrue(MicroblockShowcase.frames(MicroblockShowcase.Rule.CUT).size() == blocks * CUTS_A_BLOCK,
                "six cuts of every block");
        helper.assertTrue(MicroblockShowcase.frames(MicroblockShowcase.Rule.JOIN).size() == blocks * JOINS_A_BLOCK,
                "four joins of every block");
        helper.assertTrue(MicroblockShowcase.frames(MicroblockShowcase.Rule.FACADE).size() == blocks,
                "a facade of every block");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void frames_makeInTheGridWhatTheyShow(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        int crafted = 0;
        for (final MicroblockShowcase.Rule rule : MicroblockShowcase.Rule.values()) {
            for (final MicroblockShowcase.Frame frame : MicroblockShowcase.frames(rule)) {
                if (!shows(frame)) {
                    continue;
                }
                final CraftingInput input = CraftingInput.of(3, 3, frame.grid());
                final ItemStack made = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                        .map(holder -> holder.value().assemble(input, level.registryAccess()))
                        .orElse(ItemStack.EMPTY);
                helper.assertTrue(same(made, frame.output()), rule.id() + ": the grid " + frame.grid()
                        + " makes " + made + ", the viewer shows " + frame.output());
                crafted++;
            }
        }
        helper.assertTrue(crafted >= (CUTS_A_BLOCK + JOINS_A_BLOCK + 1) * CRAFTED.size(), "frames crafted: " + crafted);
        helper.succeed();
    }

    /* Whether {@code frame} is one of a block whose frames are crafted. */
    private static boolean shows(final MicroblockShowcase.Frame frame) {
        for (final ItemStack held : frame.grid()) {
            final Block block = blockOf(held);
            if (block != null && CRAFTED.contains(block)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable Block blockOf(final ItemStack stack) {
        final Microblock.MicroblockPiece piece = MicroblockRecipes.pieceOf(stack);
        if (piece != null) {
            return piece.material();
        }
        return stack.getItem() instanceof BlockItem item ? item.getBlock() : null;
    }

    private static boolean same(final ItemStack made, final ItemStack shown) {
        return ItemStack.isSameItemSameComponents(made, shown) && made.getCount() == shown.getCount();
    }
}
