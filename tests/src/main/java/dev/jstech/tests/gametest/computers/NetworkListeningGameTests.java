/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvents;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.SigmaCommands;
import dev.jstech.computers.vm.program.ProgramEntry;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The two more sources of events: a program that waits on its network hears each entry written to the network's event
 * log ({@code Network.OnEvent}) and each Operation of it that settles ({@code Operations.OnSettled}), on whichever
 * machine of the network it runs.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkListeningGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 8;
    private static final String WATCHER = String.join("\n",
            "using System.*; using System.Network.*; using System.Operations.*; namespace Tests;",
            "class Watcher : IScript {",
            "    public void OnInit() {",
            "        Network.OnEvent(e => Console.PrintLine(\"event \" + e.Level + \" \" + e.Text));",
            "        Operations.OnSettled(o => Console.PrintLine(\"settled \" + o.Status + \" \" + o.Moved));",
            "    }",
            "    public void OnTick() { }",
            "    public void OnDestroy() { }",
            "}");

    private NetworkListeningGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void aProgram_hearsTheNetworksLogAndItsOperationsSettle(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity computer = world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
        computer.console().install(SigmaCommands.COMPILER);
        computer.console().install(SigmaCommands.RUNTIME);
        final List<Integer> started = new ArrayList<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(computer.networkUuid() != null
                            && computer.networkUuid().equals(mainframe.networkUuid()),
                            "the computer is on the network");
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    cli.writeFile("Watcher.sgs", WATCHER);
                    CliCommands.shellFor(cli, 80).run("sgsc Watcher.sgs", cli);
                    helper.assertTrue(cli.startSigma("Watcher.asm", 0).ok(), "the watcher starts");
                    computer.programs().view().stream().filter(one -> one.file().endsWith("Watcher.asm"))
                            .forEach(one -> started.add(one.id()));
                })
                .thenExecuteAfter(SETTLE, () -> {
                    NetworkEvents.record(helper.getLevel(), mainframe.networkUuid(), EventLevel.WARN,
                            computer.nodeUuid(), "UPS-1", Text.literal("on battery"));
                    mainframe.recordOperation(OperationRecord.TYPE_CRAFT, new ItemStack(Items.OAK_PLANKS), 64, 64,
                            OperationRecord.STATUS_COMPLETED, List.of());
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final ProgramEntry<?> watcher = computer.programs().byId(started.getFirst());
                    helper.assertTrue(watcher != null, "the watcher still runs");
                    final List<String> said = computer.programs().byId(started.getFirst()).process().console();
                    helper.assertTrue(said.contains("event warn on battery"), "it heard the log; got " + said);
                    helper.assertTrue(said.contains("settled done 64"), "and the Operation settle; got " + said);
                })
                .thenSucceed();
    }
}
