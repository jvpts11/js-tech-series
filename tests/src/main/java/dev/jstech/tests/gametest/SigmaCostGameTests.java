/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.vm.system.CallCost;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What the machine charges a program, against what the editors tell the player it will charge.
 *
 * <p>Every call a program makes of the machine is charged by the runtime straight from its declaration, which the
 * tests of each of the machine's services cover. What is left here is the promise a real computer keeps by never
 * being asked at all: being told about something costs nothing.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SigmaCostGameTests {

    private SigmaCostGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 2;

    /** A computer good enough to answer, with a disk in it and a system on it. */
    private static CraftingComputerBlockEntity computer(final GameTestHelper helper, final BlockPos at) {
        return TestWorldBuilder.forGameTest(helper).placeCraftingComputer(at,
                ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp"));
    }

    /** Being told about something costs nothing, which is the whole reason to prefer it to asking. */
    @GameTest(template = ARENA)
    public static void watching_costsNothingToArm(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, 2, 2);
        final CraftingComputerBlockEntity computer = computer(helper, at);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    for (final String member : List.of("Watch", "WatchBelow", "WatchAbove")) {
                        final IMemberSpec watch = SystemApi.members("Network", member).getFirst();
                        helper.assertTrue(CallCost.FREE.equals(watch.cost()), member + " should cost nothing to arm");
                        /*
                         * The machine is never asked: a watch is answered inside the runtime, which is
                         * what makes it free. If that ever changes, the machine starts answering for it and
                         * this stops being true.
                         */
                        helper.assertTrue(computer.services().bind(watch.id()) == null,
                                "a watch is the runtime's to answer, not the machine's");
                    }
                })
                .thenSucceed();
    }
}
