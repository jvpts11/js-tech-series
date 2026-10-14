/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.machine.IMachineRuntime;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.machine.ProgramView;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.ServerStacks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.computers.TestShell.text;

/**
 * GameTests for the Σ# programs a server in a rack runs, each server its own, as a computer runs them: one started at a
 * server's prompt takes that server's terminal and reads what is typed there while the server beside it runs nothing,
 * a script runs on by itself and is listed and stopped by its number, and what is running rides on the Server item, so
 * it is still running once the world comes back, and ends when the server is pulled out.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RackSigmaGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos RACK = new BlockPos(2, 2, 2);
    private static final int SETTLE = 4;
    /** The rows of the two servers, with the KVM switch between them. */
    private static final int FIRST = 0;
    private static final int SECOND = 2;
    private static final int WIDTH = 52;

    /** A program that asks for a name at its terminal and answers with it. */
    private static final String ASKS = """
            using System.IO.*;
            namespace Programs;
            class Asks {
                static void Main() {
                    Console.PrintLine("name?");
                    Console.PrintLine("hello " + Console.ReadLine());
                }
            }
            """;

    /** A script, which runs on by itself every tick until it is stopped, and counts the ticks it was given. */
    private static final String COUNTER = """
            using System.*;
            using System.IO.*;
            namespace Programs;
            class Counter : IScript {
                int seen;
                public void OnInit() { seen = 0; }
                public void OnTick() { seen = seen + 1; Console.PrintLine("tick " + seen); }
                public void OnDestroy() { }
            }
            """;

    private RackSigmaGameTests() {
    }

    /**
     * A program started with sigma run at a server's prompt runs on that server, holds its terminal, stops on its read
     * and finishes with the line typed; the server beside it in the rack runs none of it. It used to be refused, as if
     * a server could run no programs at all.
     */
    @GameTest(template = ARENA, timeoutTicks = 3600)
    public static void sigmaRun_aServerRunsTheProgramAndItReadsWhatIsTyped(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = twoServers(helper);
        final IOsHost first = rack.unitHost(FIRST);
        final IOsHost second = rack.unitHost(SECOND);
        final IMachineRuntime[] asked = new IMachineRuntime[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(helper, first);
                    install(helper, second);
                })
                .thenWaitUntil(() -> helper.assertTrue(up(first) && up(second), "both servers come up"))
                .thenExecute(() -> {
                    rack.setActiveChannel(SECOND);
                    tools(rack, SECOND);
                    final ServerCliComputer cli = new ServerCliComputer(rack, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, WIDTH);
                    helper.assertTrue(cli.writeFile("asks.sgs", ASKS).ok(), "the source is on the server's drive");
                    final String compiled = text(shell.run("sgsc asks.sgs", cli));
                    helper.assertTrue(compiled.contains("wrote"), "the program compiles; got " + compiled);
                    final String ran = text(shell.run("sigma run asks.asm", cli));
                    helper.assertFalse(ran.contains("sigma:") || ran.contains("not found"),
                            "the server runs the program; got " + ran);
                })
                .thenExecuteAfter(SETTLE * 4, () -> {
                    final MachinePrograms programs = second.programs();
                    helper.assertTrue(programs != null && programs.held() > 0, "its terminal holds the program");
                    final var one = programs.byId(programs.held());
                    helper.assertTrue(one != null && one.process().waitingForInput(),
                            "the program stops on its read; state " + (one == null ? "gone" : one.process().state()));
                    helper.assertTrue(one.process().console().contains("name?"), "what it printed first is there");
                    helper.assertTrue(first.programs() != null && first.programs().isEmpty(),
                            "the server beside it runs none of it");
                    asked[0] = one.process();
                    helper.assertTrue(programs.offerInput("Ada"), "a typed line goes to the program in front");
                })
                .thenExecuteAfter(SETTLE * 4, () -> {
                    helper.assertTrue(asked[0].console().contains("hello Ada"),
                            "the line typed is what the program read; console " + asked[0].console());
                    helper.assertTrue(asked[0].state() == ILanguageProcess.State.FINISHED,
                            "and it returned; state " + asked[0].state());
                })
                .thenSucceed();
    }

    /**
     * A script started at a server's prompt runs on by itself, a tick at a time, without holding the terminal; sigma ps
     * lists it under its number and sigma stop ends it.
     */
    @GameTest(template = ARENA, timeoutTicks = 3600)
    public static void sigmaRun_aScriptRunsOnAServerAndIsListedAndStopped(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = oneServer(helper);
        final IOsHost server = rack.unitHost(FIRST);
        final int[] id = new int[1];
        final int[] counted = new int[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> install(helper, server))
                .thenWaitUntil(() -> helper.assertTrue(up(server), "the server comes up"))
                .thenExecute(() -> startCounter(helper, rack))
                .thenExecuteAfter(SETTLE * 10, () -> {
                    final List<ProgramView> running = server.programs().view();
                    helper.assertTrue(running.size() == 1, "the script is on the server; it lists " + listed(running));
                    helper.assertTrue(server.programs().held() == 0, "and leaves the terminal free");
                    id[0] = running.getFirst().id();
                    counted[0] = ticks(server, id[0]);
                    helper.assertTrue(counted[0] > 0, "the server gives it its ticks; it counted " + counted[0]);
                    final String listedAtPrompt = text(prompt(rack, helper).run("sigma ps", cli(rack, helper)));
                    helper.assertTrue(listedAtPrompt.contains(String.valueOf(id[0])),
                            "sigma ps lists it by its number; got " + listedAtPrompt);
                })
                .thenExecuteAfter(SETTLE * 2, () -> {
                    helper.assertTrue(ticks(server, id[0]) > counted[0],
                            "it runs on, tick after tick; still at " + ticks(server, id[0]));
                    final String stopped = text(prompt(rack, helper).run("sigma stop " + id[0], cli(rack, helper)));
                    helper.assertTrue(stopped.contains("stopped " + id[0]), "sigma stop ends it; got " + stopped);
                    helper.assertTrue(server.programs().isEmpty(), "and the server runs nothing now");
                })
                .thenSucceed();
    }

    /**
     * What a server is running is saved on its Server item with the rest of its run, so the server read back from it is
     * still running it, under the same number and from where it was. A server pulled out of its rack loses its power,
     * and what it ran ends.
     */
    @GameTest(template = ARENA, timeoutTicks = 3600)
    public static void programs_rideOnTheServerItemAndEndWhenItIsPulled(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = oneServer(helper);
        final IOsHost server = rack.unitHost(FIRST);
        final int[] id = new int[1];
        final int[] counted = new int[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> install(helper, server))
                .thenWaitUntil(() -> helper.assertTrue(up(server), "the server comes up"))
                .thenExecute(() -> startCounter(helper, rack))
                .thenExecuteAfter(SETTLE * 10, () -> {
                    helper.assertTrue(server.programs().view().size() == 1, "the script runs on the server");
                    id[0] = server.programs().view().getFirst().id();
                    counted[0] = ticks(server, id[0]);
                    // Saving the rack writes each server's run onto its item, as a world being saved does.
                    rack.saveWithoutMetadata(helper.getLevel().registryAccess());
                    final ItemStack saved = rack.getServers().getStackInSlot(FIRST).copy();
                    // The same server mounted again is read back from what its item carries, as after a reload.
                    rack.getServers().setStackInSlot(FIRST, saved);
                    final List<ProgramView> back = server.programs().view();
                    helper.assertTrue(back.size() == 1 && back.getFirst().id() == id[0],
                            "the script is running again under its number " + id[0] + "; got " + listed(back));
                })
                .thenExecuteAfter(SETTLE * 4, () -> {
                    helper.assertTrue(ticks(server, id[0]) > counted[0], "it carries on from tick " + counted[0]
                            + " after coming back; it is at " + ticks(server, id[0]));
                    final ItemStack pulled = rack.getServers().extractItem(FIRST, 1, false);
                    final CompoundTag carried = pulled.get(ComputingComponents.SERVER_CONSOLE.get());
                    helper.assertTrue(carried != null && !carried.contains("Σ#"),
                            "a server pulled out takes no running program with it");
                })
                .thenSucceed();
    }

    /* Compiles the counter on the server the rack shows and starts it there, at its prompt. */
    private static void startCounter(final GameTestHelper helper, final ServerRackBlockEntity rack) {
        tools(rack, FIRST);
        final ServerCliComputer cli = cli(rack, helper);
        final CliShell shell = CliCommands.shellFor(cli, WIDTH);
        helper.assertTrue(cli.writeFile("counter.sgs", COUNTER).ok(), "the source is on the server's drive");
        final String compiled = text(shell.run("sgsc counter.sgs", cli));
        helper.assertTrue(compiled.contains("wrote"), "the script compiles; got " + compiled);
        final String ran = text(shell.run("sigma run counter.asm", cli));
        helper.assertTrue(ran.contains("started as"), "the server runs the script; got " + ran);
    }

    private static ServerCliComputer cli(final ServerRackBlockEntity rack, final GameTestHelper helper) {
        return new ServerCliComputer(rack, helper.getLevel());
    }

    private static CliShell prompt(final ServerRackBlockEntity rack, final GameTestHelper helper) {
        return CliCommands.shellFor(cli(rack, helper), WIDTH);
    }

    /* How many ticks the counter under that number has printed, by the last line it wrote; -1 when it is not there. */
    private static int ticks(final IOsHost server, final int id) {
        final MachinePrograms programs = server.programs();
        final var one = programs == null ? null : programs.byId(id);
        if (one == null || one.process().console().isEmpty()) {
            return -1;
        }
        final String last = one.process().console().getLast();
        return last.startsWith("tick ") ? Integer.parseInt(last.substring("tick ".length()).trim()) : -1;
    }

    /* A rack with a server in its top row, a drive in that server's bay. */
    private static ServerRackBlockEntity oneServer(final GameTestHelper helper) {
        helper.setBlock(RACK, ComputingModule.SERVER_RACK.get());
        if (!(helper.getBlockEntity(RACK) instanceof ServerRackBlockEntity rack)) {
            throw new IllegalStateException("no server rack at " + RACK);
        }
        rack.getServers().setStackInSlot(FIRST, ServerStacks.defaultServer());
        rack.insertDrive(FIRST, drive());
        return rack;
    }

    /* A rack with two servers, a KVM switch between them so a monitor can address each, and a drive in each bay. */
    private static ServerRackBlockEntity twoServers(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = oneServer(helper);
        rack.getServers().setStackInSlot(FIRST + 1, new ItemStack(ComputingModule.KVM_SWITCH.get()));
        rack.getServers().setStackInSlot(SECOND, ServerStacks.defaultServer());
        rack.insertDrive(SECOND, drive());
        return rack;
    }

    private static ItemStack drive() {
        return new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1));
    }

    private static void install(final GameTestHelper helper, final IOsHost server) {
        helper.assertTrue(server.installOs(ResourceLocation.fromNamespaceAndPath("jsc", "frames_10")),
                "Frames installs on the server's drive");
    }

    /* The Σ# compiler and runtime, installed on the server at that row. */
    private static void tools(final ServerRackBlockEntity rack, final int row) {
        rack.consoleOf(row).install("jsc:sgsc");
        rack.consoleOf(row).install("jsc:sigma");
    }

    /* The programs a server lists, each by its number, its name and how it stands. */
    private static String listed(final List<ProgramView> programs) {
        return programs.stream().map(one -> one.id() + " " + one.name() + " " + one.state()).toList().toString();
    }

    private static boolean up(final IOsHost server) {
        return server.isRunning() && !server.needsPost() && !server.booting() && !server.atBootMenu()
                && server.crashPage() == null;
    }
}
