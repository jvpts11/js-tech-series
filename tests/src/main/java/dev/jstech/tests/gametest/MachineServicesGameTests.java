/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachineServices;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.MemberKind;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.computers.vm.system.TypeSpec;
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
 * What a computer's programs reach through it: every call and value the system declares as the world's, answered by
 * services kept with the computer, the same for every call while the computer stays in its world, and made once the
 * computer is in one. What each of those calls does is the tests of its own service's to hold.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineServicesGameTests {

    private static final String ARENA = "empty";

    private MachineServicesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void bind_answersEveryCallAndValueTheSystemDeclaresAsTheWorlds(final GameTestHelper helper) {
        final MachineServices services = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2)).services();

        final List<String> unbound = new ArrayList<>();
        for (final TypeSpec type : SystemApi.types()) {
            for (final IMemberSpec member : type.members()) {
                if (member.kind() == MemberKind.WORLD && services.bind(member.id()) == null) {
                    unbound.add(member.id().describe());
                }
            }
        }
        helper.assertTrue(unbound.isEmpty(),
                "a real computer answers every call and value of the world; nothing answers " + unbound);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void shell_isTheSameForEveryCallWhileTheComputerStaysInItsWorld(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));

        final ServerCliComputer first = pc.services().shell();

        helper.assertTrue(first != null && first.machine() == pc, "a computer in a world has a shell of its own");
        helper.assertTrue(pc.services().shell() == first, "asking again hands back the same one");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void shell_isMadeOnceAComputerReadOutOfASaveIsInAWorld(final GameTestHelper helper) {
        final PersonalComputerBlockEntity placed = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        final PersonalComputerBlockEntity fresh =
                new PersonalComputerBlockEntity(placed.getBlockPos(), placed.getBlockState());

        helper.assertTrue(fresh.services().shell() == null, "a computer in no world has no shell");
        fresh.setLevel(helper.getLevel());
        final ServerCliComputer made = fresh.services().shell();

        helper.assertTrue(made != null && made.machine() == fresh, "placed in a world, it has one; got " + made);
        helper.succeed();
    }
}
