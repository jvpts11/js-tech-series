/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.core.grid.LineGroup;
import dev.jstech.core.grid.LineGroups;
import dev.jstech.industrial.research.ResearchLink;
import dev.jstech.testkit.gametest.ScenarioBuilder;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Research Link Cable: a run of it joins the machines at its ends into one group on its own grid, and cut, each
 * side is a group of its own. The test mod's lodestones stand for research computers, as any block that joins a group.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ResearchLinkGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos FIRST = new BlockPos(1, 2, 2);
    private static final List<BlockPos> RUN = List.of(new BlockPos(2, 2, 2), new BlockPos(3, 2, 2),
            new BlockPos(4, 2, 2));
    private static final BlockPos SECOND = new BlockPos(5, 2, 2);
    private static final int SETTLE = 2;

    private ResearchLinkGameTests() {
    }

    @GameTest(template = ARENA)
    public static void cable_joinsTheMachinesOfARunIntoOneGroup(final GameTestHelper helper) {
        helper.setBlock(FIRST, Blocks.LODESTONE);
        helper.setBlock(SECOND, Blocks.LODESTONE);
        for (final BlockPos at : RUN) {
            helper.assertTrue(ScenarioBuilder.layCable(helper.getLevel(), helper.absolutePos(at),
                    ResearchLink.CABLE.get()), "the cable is laid at " + at);
        }
        helper.runAfterDelay(SETTLE, () -> {
            final Optional<LineGroup> group = LineGroups.of(helper.getLevel(), ResearchLink.GRID)
                    .flatMap(groups -> groups.groupOf(helper.absolutePos(FIRST)));
            helper.assertTrue(group.isPresent() && group.get().has(helper.absolutePos(SECOND)),
                    "both ends are one group on the research link's grid; got " + group);
            helper.setBlock(RUN.get(1), Blocks.AIR);
            helper.runAfterDelay(SETTLE, () -> {
                final Optional<LineGroup> cut = LineGroups.of(helper.getLevel(), ResearchLink.GRID)
                        .flatMap(groups -> groups.groupOf(helper.absolutePos(FIRST)));
                helper.assertTrue(cut.isEmpty() || !cut.get().has(helper.absolutePos(SECOND)),
                        "cut, they are groups apart; got " + cut);
                helper.succeed();
            });
        });
    }
}
