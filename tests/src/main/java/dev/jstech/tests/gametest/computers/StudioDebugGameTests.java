/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import com.mojang.authlib.GameProfile;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.operation.payload.program.DebugPayloads;
import dev.jstech.computers.operation.payload.program.DebugRequestPayload;
import dev.jstech.computers.operation.payload.program.DebugStatePayload;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.SigmaCommands;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The debugger of the studios, on the server: what Virtual Studio and Virtual Studio Code ask of a machine starts a
 * program under the debugger with their breakpoints in place, and what comes back says where it stopped, what its
 * variables and the names watched hold, and how it ended.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StudioDebugGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);
    private static final int SETTLE = 8;
    private static final String PLANT = String.join("\n",
            "using System.IO.*; namespace Plant;",
            "class Plant {",
            "    static int Twice(int n) {",
            "        int doubled = n * 2;",
            "        return doubled;",
            "    }",
            "    static void Main() {",
            "        long logs = 41;",
            "        Console.PrintLine(\"Logs: \" + logs);",
            "        if (logs < 64) {",
            "            int more = Twice(21);",
            "            Console.PrintLine(\"\" + more);",
            "        }",
            "    }",
            "}");

    private StudioDebugGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void aStudio_startsStopsWatchesStepsAndEndsTheProgram(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper);
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(),
                "debugger"));
        final BlockPos host = computer.getBlockPos();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    build(helper, computer);
                    DebugPayloads.handle(new DebugRequestPayload(host, DebugRequestPayload.START, "/Plant.asm",
                            List.of("/Plant.sgs:10")), player, helper.getLevel());
                    DebugPayloads.handle(new DebugRequestPayload(host, DebugRequestPayload.WATCHES, "",
                            List.of("logs", "nothing")), player, helper.getLevel());
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final DebugStatePayload stopped = DebugPayloads.stateOf(player, helper.getLevel());
                    helper.assertTrue(stopped.status() == DebugStatePayload.PAUSED
                            && "breakpoint".equals(stopped.reason()), "it stopped at the breakpoint; got "
                            + stopped.status() + " " + stopped.reason());
                    final DebugStatePayload.Frame top = stopped.stack().getFirst();
                    helper.assertTrue(top.method().equals("Plant.Main") && top.line() == 10 && top.column() == 9,
                            "in Main at the line and column of the if; got " + top);
                    helper.assertTrue(stopped.locals().contains(new DebugStatePayload.Variable("logs", "long", "41")),
                            "its locals; got " + stopped.locals());
                    helper.assertTrue(stopped.watches().getFirst().value().equals("41")
                            && stopped.watches().get(1).type().isEmpty(), "a name watched, and one not there; got "
                            + stopped.watches());
                    helper.assertTrue(stopped.spent() > 0 && stopped.spent() >= stopped.steps(),
                            "and what it has spent; got " + stopped.spent());
                    DebugPayloads.handle(DebugRequestPayload.of(host, DebugRequestPayload.STEP_OVER), player,
                            helper.getLevel());
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final DebugStatePayload stepped = DebugPayloads.stateOf(player, helper.getLevel());
                    helper.assertTrue(stepped.status() == DebugStatePayload.PAUSED
                            && stepped.stack().getFirst().line() == 11, "a step goes to the next line; got "
                            + stepped.stack());
                    DebugPayloads.handle(DebugRequestPayload.of(host, DebugRequestPayload.CONTINUE), player,
                            helper.getLevel());
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(DebugPayloads.stateOf(player, helper.getLevel()).status()
                            == DebugStatePayload.NONE, "the program ran to its end and the session is over");
                    helper.assertTrue(computer.programs().view().stream()
                            .noneMatch(one -> one.file().endsWith("Plant.asm")), "and the program is gone");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void stop_endsTheProgramAtOnce(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = machine(helper);
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(),
                "stopper"));
        final BlockPos host = computer.getBlockPos();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    build(helper, computer);
                    DebugPayloads.handle(new DebugRequestPayload(host, DebugRequestPayload.START, "/Plant.asm",
                            List.of("/Plant.sgs:4")), player, helper.getLevel());
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(DebugPayloads.stateOf(player, helper.getLevel()).status()
                            == DebugStatePayload.PAUSED, "stopped inside Twice");
                    DebugPayloads.handle(DebugRequestPayload.of(host, DebugRequestPayload.STOP), player,
                            helper.getLevel());
                    helper.assertTrue(DebugPayloads.stateOf(player, helper.getLevel()).status()
                            == DebugStatePayload.NONE, "Stop Debugging ends the session");
                    helper.assertTrue(computer.programs().view().stream()
                            .noneMatch(one -> one.file().endsWith("Plant.asm")), "and the program, at once");
                })
                .thenSucceed();
    }

    private static PersonalComputerBlockEntity machine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(WHERE, ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                        "ubuntu"));
        computer.console().install(SigmaCommands.COMPILER);
        computer.console().install(SigmaCommands.RUNTIME);
        return computer;
    }

    /* Builds the program for the debugger at the root of the disk, as a studio's Debug build writes it. */
    private static void build(final GameTestHelper helper, final PersonalComputerBlockEntity computer) {
        final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
        cli.writeFile("/Plant.sgs", PLANT);
        CliCommands.shellFor(cli, 80).run("sgsc --debug /Plant.sgs -o /Plant.asm", cli);
        helper.assertTrue(cli.readFile("/Plant.asm").ok(), "the listing is built");
    }
}
