/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A machine's programs say goodbye out of one budget per tick between them, not one budget each. A machine switched
 * off with programs whose farewells never end used to run every one of those farewells in full in a single tick.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FarewellBudgetGameTests {

    private static final String ARENA = "empty";
    /** Every farewell on one machine in one tick, between them. */
    private static final int FAREWELL_PER_TICK = 4096;

    /** A script whose farewell never ends. */
    private static final String STUBBORN = """
            using System.*;
            namespace Programs;
            class Stubborn : IScript {
                int n;
                public void OnInit() { n = 0; }
                public void OnTick() { }
                public void OnDestroy() { while (true) { n = n + 1; } }
            }
            """;

    private FarewellBudgetGameTests() {
    }

    @GameTest(template = ARENA)
    public static void stopAll_sharesOneFarewellBudgetBetweenThePrograms(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computer(helper);
        final List<ILanguageProcess> processes = start(helper, pc, 4);
        final long[] before = spent(processes);

        pc.programs().stopAll();

        final long[] after = spent(processes);
        long farewells = 0;
        for (int i = 0; i < processes.size(); i++) {
            helper.assertTrue(after[i] > before[i], "program " + i + " still got a farewell");
            farewells += after[i] - before[i];
        }
        helper.assertTrue(pc.programs().isEmpty(), "every program is stopped");
        helper.assertTrue(farewells <= FAREWELL_PER_TICK, "the farewells ran " + farewells
                + " instructions between them, more than one tick's " + FAREWELL_PER_TICK);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void stop_givesNoFarewellOnceTheTicksBudgetIsSpent(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computer(helper);
        final List<ILanguageProcess> processes = start(helper, pc, 3);
        final List<Integer> ids = new ArrayList<>();
        pc.programs().view().forEach(one -> ids.add(one.id()));
        final long[] before = spent(processes);

        pc.programs().stop(ids.get(0));
        pc.programs().stop(ids.get(1));
        final long[] sameTick = spent(processes);
        helper.assertTrue(sameTick[0] > before[0], "the first program stopped got its farewell");
        helper.assertTrue(sameTick[1] == before[1],
                "the second, stopped in the same tick after the budget was spent, got none");

        pc.programs().tick(1);
        pc.programs().stop(ids.get(2));
        helper.assertTrue(spent(processes)[2] > sameTick[2], "a program stopped in the next tick gets one again");
        helper.succeed();
    }

    private static PersonalComputerBlockEntity computer(final GameTestHelper helper) {
        return TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(new BlockPos(2, 2, 2));
    }

    /** Starts that many stubborn scripts and lets each run its {@code OnInit}. */
    private static List<ILanguageProcess> start(final GameTestHelper helper, final PersonalComputerBlockEntity pc,
                                                final int count) {
        final List<ILanguageProcess> processes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final MachinePrograms.Started started = pc.programs().start("stubborn.sgs", STUBBORN, 1, pc);
            helper.assertTrue(started.ok(), "program " + i + " starts: " + started.message());
            processes.add(pc.programs().byId(started.id()).process());
        }
        pc.programs().tick(100_000);
        return processes;
    }

    private static long[] spent(final List<ILanguageProcess> processes) {
        final long[] spent = new long[processes.size()];
        for (int i = 0; i < processes.size(); i++) {
            spent[i] = processes.get(i).spent();
        }
        return spent;
    }
}
