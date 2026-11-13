/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.gui.tooltip.RichLine;
import dev.jstech.tests.JsTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A line of a tooltip built of pieces: a picture in it is drawn a pixel or more a side, as a tooltip's own are. */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RichLineGameTests {

    private static final String ARENA = "empty";
    private static final ResourceLocation SPRITE = ResourceLocation.withDefaultNamespace("icon/checkmark");

    private RichLineGameTests() {
    }

    @GameTest(template = ARENA)
    public static void image_isRefusedAtNoSizeOrLessAndTakenAtSome(final GameTestHelper helper) {
        for (final int[] size : new int[][] {{0, 8}, {8, 0}, {-4, 8}}) {
            try {
                RichLine.builder().image(SPRITE, size[0], size[1]);
                helper.fail("a picture " + size[0] + " by " + size[1] + " is refused");
                return;
            } catch (final IllegalArgumentException refused) {
                // As it should be.
            }
        }
        helper.assertTrue(RichLine.builder().image(SPRITE, 8, 8).build().pieces().size() == 1,
                "one of some size is a piece of the line");
        helper.succeed();
    }
}
