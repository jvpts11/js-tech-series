/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.gui.tooltip.ItemTooltips;
import dev.jstech.core.gui.tooltip.TooltipPicture;
import dev.jstech.core.gui.tooltip.TooltipPreview;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestItems;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What the test mod says in tooltips, as another mod says it through the Core: a line on its full tool, so that the
 * order of the mods shows; and, on an item marked for it with the test mod's number, a 3D preview of a furnace of
 * bricks three blocks a side or a picture, a painting of the game's.
 */
@TextHolder
public final class TestTooltips {

    public static final TextKey MADE = TextKey.of("jstests.tooltip.made", "Made for the tests");
    public static final TextKey BUILDS = TextKey.of("jstests.tooltip.builds", "Builds a 3 x 3 x 3 furnace:");
    public static final TextKey TAKEN = TextKey.of("jstests.tooltip.taken", "A painting of the game's");
    /** The test mod's number on an item that shows the preview, and on one that shows the picture. */
    public static final int PREVIEW = 1;
    public static final int PICTURE = 2;
    private static final int PREVIEW_SIZE = 56;
    private static final int PICTURE_SIZE = 48;
    private static final ResourceLocation PAINTING = ResourceLocation.withDefaultNamespace(
            "textures/painting/kebab.png");

    private TestTooltips() {
    }

    /** Hands the Core what the test mod says, once, from its client. */
    public static void declare() {
        ItemTooltips.add(JsTests.MODID, (stack, tooltip) -> {
            if (stack.is(TestItems.FULL.get())) {
                tooltip.line(MADE.text());
            }
            final Integer marked = stack.get(TestItems.MARK_COMPONENT.get());
            if (marked != null && marked == PREVIEW) {
                tooltip.line(BUILDS.text()).piece(new TooltipPreview(furnace(), PREVIEW_SIZE));
            } else if (marked != null && marked == PICTURE) {
                tooltip.piece(new TooltipPicture(PAINTING, PICTURE_SIZE, PICTURE_SIZE)).line(TAKEN.text());
            }
        });
    }

    /** A stack of {@code stack}'s item marked to show {@code what}. */
    public static ItemStack marked(final ItemStack stack, final int what) {
        stack.set(TestItems.MARK_COMPONENT.get(), what);
        return stack;
    }

    /* Bricks three a side with a lit furnace in the middle of the front, the east, the side seen on the left. */
    private static Map<BlockPos, BlockState> furnace() {
        final Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    blocks.put(new BlockPos(x, y, z), Blocks.BRICKS.defaultBlockState());
                }
            }
        }
        blocks.put(new BlockPos(2, 1, 1), Blocks.BLAST_FURNACE.defaultBlockState()
                .setValue(AbstractFurnaceBlock.FACING, Direction.EAST).setValue(AbstractFurnaceBlock.LIT, true));
        return blocks;
    }
}
