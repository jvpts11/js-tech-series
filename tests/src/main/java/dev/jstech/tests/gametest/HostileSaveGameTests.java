/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.vm.program.UiWidgets;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A world is saved whenever the server decides, never at a moment a program chose. These catch a machine at the
 * worst moments there are (a program waiting for a line, a thread waiting for a lock, a parent waiting for a child
 * here or on another machine, a click not yet handled, work asked of the network, a pile of unread messages), write
 * it to its block entity's own tag and read it back in place, and check the program carries on as if nothing happened.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class HostileSaveGameTests {

    private HostileSaveGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    /** Credits that run any of these programs as far as it can go in one tick. */
    private static final int PLENTY = 100_000;

    private static final String READER = """
            using System.*;
            using System.IO.*;
            namespace Programs;
            class Reader {
                static void Main() {
                    Console.PrintLine("name?");
                    string name = Console.ReadLine();
                    Console.PrintLine("got " + name);
                }
            }
            """;

    private static final String GATEKEEPER = """
            using System.*;
            using System.IO.*;
            using System.Collections.*;
            using System.Threading.*;
            namespace Programs;
            class Gatekeeper : IScript {
                object gate = new List<int>();
                int ticks;
                public void OnInit() {
                    Thread.Start(() => { lock (gate) { Thread.Sleep(5); Console.PrintLine("holder done"); } });
                }
                public void OnTick() {
                    ticks = ticks + 1;
                    if (ticks == 2) { lock (gate) { Console.PrintLine("waiter in"); } }
                }
                public void OnDestroy() { }
            }
            """;

    private static final String PARENT = """
            using System.*;
            using System.IO.*;
            using System.Collections.*;
            using System.Execution.*;
            namespace Programs;
            class Parent {
                static void Main() {
                    List<string> args = new List<string>();
                    args.Add("a");
                    Process p = Program.Start("C:\\\\child.asm", args);
                    Console.PrintLine("started " + p.Name);
                    p.Wait();
                    foreach (string line in p.Output()) { Console.PrintLine("child: " + line); }
                    Console.PrintLine("code " + p.ExitCode);
                }
            }
            """;

    private static final String SLOW_CHILD = """
            using System.*;
            using System.IO.*;
            using System.Execution.*;
            using System.Threading.*;
            namespace Programs;
            class Child {
                static void Main() {
                    Thread.Sleep(5);
                    Console.PrintLine("hello " + Program.Args.Get(0));
                    Program.Exit(4);
                }
            }
            """;

    private static final String SLOW_TOOL = """
            using System.*;
            using System.IO.*;
            using System.Execution.*;
            using System.Threading.*;
            namespace Programs;
            class Tool {
                static void Main() {
                    Thread.Sleep(10);
                    Console.PrintLine("tool " + Program.Args.Get(0));
                    Program.Exit(6);
                }
            }
            """;

    private static final String PATIENT = """
            using System.*;
            using System.IO.*;
            using System.Collections.*;
            using System.Network.*;
            using System.Execution.*;
            namespace Programs;
            class Patient {
                static void Main() {
                    List<string> args = new List<string>();
                    args.Add("y");
                    Process p = Network.Computer("desk").Start("C:\\\\tool.asm", args);
                    p.Wait();
                    Console.PrintLine("code " + p.ExitCode);
                }
            }
            """;

    private static final String PANEL = """
            using System.*;
            using System.UI.*;
            namespace Plant;
            class Panel : IScript {
                Window window;
                Label heat;
                Button scram;
                public void OnInit() {
                    heat = new Label("812 K");
                    scram = new Button("SCRAM");
                    scram.OnClick += Scram;
                    Row top = new Row();
                    top.Add(heat);
                    top.Add(scram);
                    window = new Window("Reactor", 240, 150);
                    window.Content = top;
                    window.Show();
                }
                void Scram() { heat.Text = "cold"; }
                public void OnTick() { }
                public void OnDestroy() { window.Close(); }
            }
            """;

    private static final String RESTOCK = """
            using System.*;
            using System.IO.*;
            using System.Operations.*;
            namespace Programs;
            class Restock : IScript {
                public void OnInit() {
                    AskResult asked = Operations.Pull("minecraft:oak_log", 64);
                    Console.PrintLine(asked.Ok ? "asked" : asked.Message);
                }
                public void OnTick() { }
                public void OnDestroy() { }
            }
            """;

    private static final String LISTENER = """
            using System.*;
            using System.IO.*;
            using System.Execution.*;
            namespace Programs;
            class Listener : IScript {
                int count;
                public void OnInit() {
                    Program.OnMessage(m => {
                        count = count + 1;
                        if (count % 100 == 0) { Console.PrintLine("got " + count + " " + m.Text); }
                    });
                }
                public void OnTick() { }
                public void OnDestroy() { }
            }
            """;

    private static final int MESSAGES = 500;

    @GameTest(template = ARENA)
    public static void save_whileAProgramWaitsForALine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = pc.cannon().start("reader.can", READER, 1, pc);
                    helper.assertTrue(started.ok(), "the reader starts: " + started.message());
                    pc.cannon().hold(started.id());
                    pc.cannon().tick(PLENTY);
                    helper.assertTrue(process(pc, started.id()).waitingForInput(), "it waits for a line before the save");

                    reload(helper, pc);
                    final ILanguageProcess after = process(pc, started.id());
                    helper.assertTrue(after.waitingForInput(), "and still waits for one after it; state " + after.state());
                    helper.assertTrue(pc.cannon().held() == started.id(), "the terminal still holds it");
                    helper.assertTrue(pc.cannon().offerInput("Ada"), "the line typed after the save reaches it");
                    pc.cannon().tick(PLENTY);
                    helper.assertTrue(after.console().equals(List.of("name?", "got Ada")),
                            "it reads the line and goes on; got " + after.console() + " (" + after.message() + ")");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void save_whileAThreadWaitsForALock(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        final ILanguageProcess[] after = new ILanguageProcess[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = pc.cannon().start("gate.can", GATEKEEPER, 1, pc);
                    helper.assertTrue(started.ok(), "the program starts: " + started.message());
                    /*
                     * All within one game tick, so the holder's sleep never runs out: the thread started at init takes
                     * the lock and sleeps, and the script's second tick then waits for that lock.
                     */
                    for (int i = 0; i < 4; i++) {
                        pc.cannon().tick(PLENTY);
                    }
                    final ILanguageProcess before = process(pc, started.id());
                    helper.assertTrue(before.console().isEmpty() && before.state() == ILanguageProcess.State.PARKED,
                            "one thread sleeps holding the lock and the other waits for it; state " + before.state()
                                    + ", said " + before.console());
                    reload(helper, pc);
                    after[0] = process(pc, started.id());
                })
                .thenExecuteAfter(20, () -> helper.assertTrue(
                        after[0].console().equals(List.of("holder done", "waiter in")),
                        "the holder wakes, lets go, and the waiter gets the lock; got " + after[0].console()
                                + " (" + after[0].message() + ")"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void save_whileAParentWaitsForAChildOnTheSameMachine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        final ILanguageProcess[] parent = new ILanguageProcess[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(new ServerCliComputer(pc, helper.getLevel())
                            .writeFile("C:\\child.asm", listing(SLOW_CHILD)).ok(), "the child is on the disk");
                    final MachinePrograms.Started started = pc.cannon().start("parent.can", PARENT, 1, pc);
                    helper.assertTrue(started.ok(), "the parent starts: " + started.message());
                    pc.cannon().tick(PLENTY);
                    pc.cannon().tick(PLENTY);
                    helper.assertTrue(pc.cannon().all().size() == 2
                                    && process(pc, started.id()).console().equals(List.of("started child.asm")),
                            "the parent started the child and waits for it; programs " + pc.cannon().all().size()
                                    + ", said " + process(pc, started.id()).console());
                    reload(helper, pc);
                    parent[0] = process(pc, started.id());
                })
                .thenExecuteAfter(30, () -> helper.assertTrue(
                        parent[0].console().equals(List.of("started child.asm", "child: hello a", "code 4")),
                        "after the save the parent still gets what its child left; got " + parent[0].console()
                                + " (" + parent[0].message() + ")"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void save_whileAParentWaitsForAChildOnAnotherMachine(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE.get());
        world.placeSeededRack(new BlockPos(2, 2, 1));
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity lab = world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
        world.setBlock(new BlockPos(4, 2, 3), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity desk = world.placeRunningPersonalComputer(new BlockPos(5, 2, 3));
        lab.console().setComputerName("lab");
        desk.console().setComputerName("desk");
        final int[] id = new int[1];
        final ILanguageProcess[] patient = new ILanguageProcess[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    helper.assertTrue(new ServerCliComputer(desk, helper.getLevel())
                            .writeFile("C:\\tool.asm", listing(SLOW_TOOL)).ok(), "the tool is on desk");
                    final MachinePrograms.Started started = lab.cannon().start("patient.asm", listing(PATIENT), 1, lab);
                    helper.assertTrue(started.ok(), "the patient starts on lab: " + started.message());
                    lab.cannon().hold(started.id());
                    id[0] = started.id();
                })
                .thenExecuteAfter(4, () -> {
                    helper.assertTrue(desk.cannon().all().stream().anyMatch(one -> "tool.asm".equals(one.file())),
                            "lab started the tool on desk, which is still sleeping");
                    helper.assertTrue(process(lab, id[0]).console().isEmpty(), "and lab waits for it");
                    reload(helper, desk);
                    reload(helper, lab);
                    patient[0] = process(lab, id[0]);
                })
                .thenExecuteAfter(60, () -> helper.assertTrue(patient[0].console().equals(List.of("code 6")),
                        "both machines saved mid-wait, and the parent still reads the exit code; got "
                                + patient[0].console() + " (" + patient[0].message() + ")"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void save_withAClickNotYetHandled(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = pc.cannon().start("panel.can", PANEL, 1, pc);
                    helper.assertTrue(started.ok(), "the panel starts: " + started.message());
                    pc.cannon().tick(PLENTY);
                    final Values.Obj window = pc.cannon().windowsOf(started.id()).getFirst();
                    final Values.Obj button = widgetOf(window, UiWidgets.BUTTON);
                    helper.assertTrue(pc.cannon().deliverUiEvent(started.id(), 1L, (Long) button.get(UiWidgets.ID),
                            "click", List.of()), "the click is taken");

                    reload(helper, pc);
                    pc.cannon().tick(PLENTY);
                    final List<Values.Obj> windows = pc.cannon().windowsOf(started.id());
                    helper.assertTrue(windows.size() == 1, "the window comes back; got " + windows.size());
                    final Values.Obj label = widgetOf(windows.getFirst(), UiWidgets.LABEL);
                    helper.assertTrue(label != null && "cold".equals(label.get(UiWidgets.TEXT)),
                            "the click taken before the save is handled after it; the label says "
                                    + (label == null ? "nothing" : label.get(UiWidgets.TEXT)));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void save_rightAfterAProgramAskedTheNetworkForWork(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork wired = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        final CraftingComputerBlockEntity computer = wired.cc();
        wired.rack().getServerStorage(0).insert(Items.OAK_LOG, 640);
        final ILanguageProcess[] after = new ILanguageProcess[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = computer.cannon().start("restock.can", RESTOCK, 1, computer);
                    helper.assertTrue(started.ok(), "the program starts: " + started.message());
                    computer.cannon().tick(PLENTY);
                    helper.assertTrue(process(computer, started.id()).console().equals(List.of("asked")),
                            "the network takes the ask; got " + process(computer, started.id()).console());
                    reload(helper, computer);
                    after[0] = process(computer, started.id());
                })
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(after[0].console().equals(List.of("asked")),
                            "the program does not ask again after the save; got " + after[0].console());
                    helper.assertTrue(rowsNaming(wired.mainframe(), "Cannon: Programs.Restock") == 1,
                            "the network did the work once; rows naming the script: "
                                    + rowsNaming(wired.mainframe(), "Cannon: Programs.Restock"));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void save_withAPileOfUnreadMessages(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = pc.cannon().start("listener.can", LISTENER, 1, pc);
                    helper.assertTrue(started.ok(), "the listener starts: " + started.message());
                    pc.cannon().tick(PLENTY);
                    final long now = helper.getLevel().getGameTime();
                    for (int i = 0; i < MESSAGES; i++) {
                        helper.assertTrue(pc.cannon().send(0, started.id(), "m" + i, now),
                                "message " + i + " is taken");
                    }

                    reload(helper, pc);
                    final ILanguageProcess after = process(pc, started.id());
                    for (int i = 0; i < 20 && after.console().size() < MESSAGES / 100; i++) {
                        pc.cannon().tick(PLENTY);
                    }
                    helper.assertTrue(after.console().equals(List.of("got 100 m99", "got 200 m199", "got 300 m299",
                                    "got 400 m399", "got 500 m499")),
                            "every message waiting at the save is handled after it, in order; got " + after.console()
                                    + " (" + after.message() + ")");
                })
                .thenSucceed();
    }

    /** Writes the machine to its block entity's tag and reads it back in place, the way a chunk comes back. */
    private static void reload(final GameTestHelper helper, final BlockEntity machine) {
        final var registries = helper.getLevel().registryAccess();
        final CompoundTag saved = machine.saveWithFullMetadata(registries);
        machine.loadWithComponents(saved, registries);
    }

    private static ILanguageProcess process(final PersonalComputerBlockEntity machine, final int id) {
        final MachinePrograms.Live one = machine.cannon().byId(id);
        if (one == null) {
            throw new IllegalStateException("no program " + id + " on the machine");
        }
        return one.process();
    }

    private static ILanguageProcess process(final CraftingComputerBlockEntity machine, final int id) {
        final MachinePrograms.Live one = machine.cannon().byId(id);
        if (one == null) {
            throw new IllegalStateException("no program " + id + " on the machine");
        }
        return one.process();
    }

    private static Values.Obj widgetOf(final Values.Obj window, final String kind) {
        for (final Values.Obj widget : UiWidgets.inside(window)) {
            if (kind.equals(widget.type())) {
                return widget;
            }
        }
        return null;
    }

    private static int rowsNaming(final MainframeBlockEntity mainframe, final String who) {
        int rows = 0;
        for (final OperationRecord record : mainframe.recentOperations()) {
            if (record.moves().stream().anyMatch(move -> move.to().contains(who) || move.from().contains(who))) {
                rows++;
            }
        }
        return rows;
    }

    private static String listing(final String source) {
        final CannonCompiler.Result built = CannonCompiler.compile(List.of(new SourceFile("Program.can", source)));
        if (!built.ok()) {
            throw new IllegalStateException(String.join("\n", built.lines()));
        }
        return built.assembly();
    }
}
