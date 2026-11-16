/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.block.WorkbenchBlock;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineTerminalBlock;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Each kind of recipe a chain machine works is shown in the recipe viewers by the machine itself, its terminal's item
 * drawn as the formed machine, or by the Electronics Workbench whose recipes an assembly line shares: never by one of
 * the machine's parts, which work nothing on their own.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ChainMachineKindsGameTests {

    private static final String ARENA = "empty";

    private ChainMachineKindsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void kinds_areShownByTheMachineItselfNeverByAPart(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        int kinds = 0;
        for (final ChainMachine machine : ChainMachines.all()) {
            final Item terminal = ChainMachines.terminalOf(machine).get().asItem();
            if (terminal == Items.AIR) {
                wrong.add(machine.id() + " has no item to show it");
                continue;
            }
            for (final ChainMachines.Worked worked : ChainMachines.workedBy(machine)) {
                kinds++;
                final List<ItemLike> shown = worked.kind().machines();
                if (shown.stream().noneMatch(station -> station.asItem() == terminal)) {
                    wrong.add(worked.kind().id() + " is not shown by " + machine.id());
                }
                shown.stream().filter(station -> !machineOrBench(station)).forEach(station ->
                        wrong.add(worked.kind().id() + " is shown by the part " + station.asItem()));
                if (!machine.id().startsWith(ChainMachines.ASSEMBLY + "_") && shown.getFirst().asItem() != terminal) {
                    wrong.add(worked.kind().id() + " is shown first by " + shown.getFirst().asItem());
                }
            }
        }
        helper.assertTrue(wrong.isEmpty(), wrong.size() + " wrong: " + wrong);
        helper.assertTrue(kinds > 0, "the chain machines work kinds of recipe");
        helper.succeed();
    }

    /* Whether {@code station} is a chain machine's own terminal or an Electronics Workbench. */
    private static boolean machineOrBench(final ItemLike station) {
        return station.asItem() instanceof BlockItem placed && (placed.getBlock() instanceof ChainMachineTerminalBlock
                || placed.getBlock() instanceof WorkbenchBlock);
    }
}
