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
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IWorldCall;
import dev.jstech.computers.vm.program.IWorldFunction;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.computers.vm.system.MemberKind;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a program asks of the other programs on its computer and of the other computers of its network, as a real
 * computer answers it: every call and value the system declares as the world's is answered, a program the machine
 * does not have reads as gone, a computer the network does not have stops the program, and a computer in no world
 * cannot be reached. The programs actually started, waited on, read and stopped are the Σ# process tests' to run.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ProgramCallsGameTests {

    private ProgramCallsGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final int GHOST = 999;

    /** Where a call says what it moved and who asked; the asking program here has no number. */
    private static final IWorldCall UNNUMBERED = bytes -> {
    };

    /** Makes one of the calls, or reads one of the values, the way a program's line does. */
    private static Object ask(final MachineServices host, final MemberId id, final Object target,
                              final Object... arguments) {
        final IWorldFunction bound = host.bind(id);
        if (bound == null) {
            throw new IllegalStateException(id.describe() + " is not answered by the machine");
        }
        return bound.call(UNNUMBERED, target, arguments, 1);
    }

    private static MemberId member(final String owner, final String name, final String... parameters) {
        return new MemberId(owner, name, List.of(parameters));
    }

    /** A handle to a program the machine has never had. */
    private static Values.Obj ghost() {
        final Values.Obj handle = new Values.Obj("Process");
        handle.set("Id", GHOST);
        handle.set("Name", "ghost.asm");
        handle.set("Host", "");
        return handle;
    }

    @GameTest(template = ARENA)
    public static void bind_answersEveryWorldCallAndValueOnProgramsAndOtherComputers(final GameTestHelper helper) {
        final MachineServices host = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2)).services();

        for (final String owner : List.of("Program", "Process", "RemoteComputer")) {
            for (final IMemberSpec member : SystemApi.type(owner).members()) {
                if (member.kind() == MemberKind.WORLD) {
                    helper.assertTrue(host.bind(member.id()) != null,
                            member.id().describe() + " is answered by the machine");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void programs_readAProgramTheMachineDoesNotHaveAsGone(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachineServices host = pc.services();

                    helper.assertTrue(Boolean.FALSE.equals(ask(host, member("Process", "Running"), ghost())),
                            "it is not running");
                    helper.assertTrue(Integer.valueOf(0).equals(ask(host, member("Process", "ExitCode"), ghost())),
                            "and has no exit code");
                    helper.assertTrue(ask(host, member("Process", "Output"), ghost()) instanceof Values.ListValue said
                            && said.items().isEmpty(), "nor anything it printed");
                    helper.assertTrue(Boolean.FALSE.equals(ask(host, member("Process", "Kill"), ghost())),
                            "and there is nothing to stop");
                    helper.assertTrue(Boolean.FALSE.equals(ask(host, member("Process", "Send", "int", "string"), null,
                            GHOST, "hello")), "a line sent to it is not taken");
                    helper.assertTrue(!host.programRunning(GHOST, ""), "a wait on it would not wait");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void shell_runsALineAtTheComputersOwnPromptAndHandsBackWhatItPrinted(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final Object said =
                            ask(pc.services(), member("Program", "Shell", "string"), null, "echo hello");
                    helper.assertTrue(said instanceof Values.ListValue lines && lines.items().contains("hello"),
                            "the line ran at the computer's own prompt and what it printed came back");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void remote_stopsAProgramThatReachesForAComputerTheNetworkDoesNotHave(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final Values.Obj nowhere = new Values.Obj("RemoteComputer");
                    nowhere.set("Host", "nowhere");
                    try {
                        ask(pc.services(), member("RemoteComputer", "Processes"), nowhere);
                        helper.fail("reaching for a computer that is not there stops the program");
                    } catch (final Halt halt) {
                        helper.assertTrue(halt.reason() == Halt.Reason.NO_OBJECT
                                        && "nowhere: no such computer on this network".equals(halt.getMessage()),
                                "it names the computer that is not there; got " + halt.getMessage());
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void programs_cannotBeReachedFromAComputerInNoWorld(final GameTestHelper helper) {
        final PersonalComputerBlockEntity placed = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        final PersonalComputerBlockEntity loose =
                new PersonalComputerBlockEntity(placed.getBlockPos(), placed.getBlockState());
        final MachineServices host = loose.services();

        try {
            ask(host, member("Program", "Start", "string"), null, "tool.asm");
            helper.fail("a computer in no world cannot start a program");
        } catch (final Halt halt) {
            helper.assertTrue("this machine cannot reach Program".equals(halt.getMessage()),
                    "it says programs cannot be reached; got " + halt.getMessage());
        }
        helper.assertTrue(!host.programRunning(GHOST, ""), "and knows of no program running");
        helper.succeed();
    }
}
