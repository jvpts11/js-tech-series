/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.crafting.PatternWorkbench;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.engine.CraftRequest;
import dev.jstech.computers.operation.payload.CraftPlanRequestPayload;
import dev.jstech.computers.program.DesktopLayout;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What the server does with numbers and names a player's screen hands it: each is held to what the server can work
 * with, whatever a modified client sends, rather than wrapping round, growing without end or being taken as given.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ClientTrustGameTests {

    private static final String ARENA = "empty";

    private ClientTrustGameTests() {
    }

    @GameTest(template = ARENA)
    public static void craftRequest_holdsAHugeDemandToTheMostACraftAsks(final GameTestHelper helper) {
        final CraftRequest request = CraftRequest.of(StorageKey.of(Items.OAK_PLANKS), Long.MAX_VALUE, false, "", null);
        helper.assertTrue(request.demand() == CraftRequest.MOST_DEMAND,
                "a demand past what a plan's sums hold is cut to the most; got " + request.demand());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void craftPreview_holdsAHugeQuantityToTheMostACraftAsks(final GameTestHelper helper) {
        final CraftPlanRequestPayload payload = new CraftPlanRequestPayload(BlockPos.ZERO, BlockPos.ZERO,
                new ItemStack(Items.OAK_PLANKS), Long.MAX_VALUE);
        helper.assertTrue(payload.quantity() == CraftRequest.MOST_DEMAND, "the preview asks no more than a craft");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void processingPattern_holdsEachAmountToARunsWorth(final GameTestHelper helper) {
        final StorageKey iron = StorageKey.of(Items.IRON_INGOT);
        helper.assertTrue(new ProcessingPattern.ProcessingInput(iron, Long.MAX_VALUE).amount()
                == ProcessingPattern.MOST_AMOUNT, "an input past the most is held to it");
        helper.assertTrue(new ProcessingPattern.ProcessingInput(iron, -5L).amount() == 1L,
                "and one of nothing, or less, asks for one");
        helper.assertTrue(new ProcessingPattern.ProcessingOutput(iron, 0L, 100).amount() == 1L,
                "and so does an output");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void workbench_leavesCellsOfNothingEmpty(final GameTestHelper helper) {
        final PatternWorkbench workbench = new PatternWorkbench();
        final StorageKey iron = StorageKey.of(Items.IRON_INGOT);
        workbench.applyProcessingCells(
                List.of(new PatternWorkbench.DataCell(iron, -64L, false),
                        new PatternWorkbench.DataCell(iron, Long.MAX_VALUE, false)),
                List.of(new PatternWorkbench.DataCell(iron, 0L, false)));
        helper.assertTrue(workbench.procInput(0) == null, "a cell asking for less than nothing is an empty cell");
        helper.assertTrue(workbench.procOutput(0) == null, "and so is one asking for nothing");
        helper.assertTrue(workbench.procInput(1) != null
                        && workbench.procInput(1).amount() == ProcessingPattern.MOST_AMOUNT,
                "a cell asking for more than a run moves is held to the most");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void desktop_pinsNoMoreIconsThanTheListingCarries(final GameTestHelper helper) {
        final DesktopLayout layout = new DesktopLayout();
        for (int i = 0; i < DesktopLayout.MOST_ICON_CELLS + 50; i++) {
            layout.setIconCell("app:program" + i, DesktopLayout.packCell(i % 16, i / 16));
        }
        helper.assertTrue(layout.iconCells().size() == DesktopLayout.MOST_ICON_CELLS,
                "keys past the most are left to the auto-flow layout; got " + layout.iconCells().size());
        layout.setIconCell("app:program0", DesktopLayout.packCell(3, 3));
        helper.assertTrue(layout.iconCells().get("app:program0") == DesktopLayout.packCell(3, 3),
                "while an icon already pinned still moves");
        helper.succeed();
    }
}
