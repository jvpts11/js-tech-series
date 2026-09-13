/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.machine.MachinePrograms;
import dev.jstech.computers.cannon.machine.ServerTickDeadline;
import dev.jstech.computers.cannon.run.ConsoleBuffer;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A program on a real machine is called once when it starts and once every tick after that if it stays
 * up, and it only ever gets the instructions the machine's processor is worth. These check that: what a
 * program says over several ticks, how the tick is shared when more than one is running, and that both
 * come back unchanged from a save.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CannonProcessGameTests {

    private CannonProcessGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 2;

    /** A script that says which tick it is on, so its console counts the ticks it was given. */
    private static final String COUNTER = """
            using System.*;
            using System.IO.*;
            namespace Programs;
            class Counter : IScript {
                int seen;

                public void OnInit() { seen = 0; Console.PrintLine("up"); }
                public void OnTick() { seen = seen + 1; Console.PrintLine("tick " + seen); }
                public void OnDestroy() { Console.PrintLine("down"); }
            }
            """;

    /** A program that runs at a terminal: it starts at Main, prints, and is done. */
    private static final String HELLO = """
            using System.IO.*;
            namespace Programs;
            class Hello {
                static void Main() {
                    for (int i = 0; i < 3; i++) { Console.PrintLine("hi " + i); }
                }
            }
            """;

    /**
     * What a script starts with when it does not say so itself: the whole library brought in and a
     * namespace, on one line so the source keeps its line numbers.
     */
    private static final String PRELUDE = "using System.*; using System.IO.*; using System.Collections.*; "
            + "using System.Utils.*; using System.Machine.*; using System.Network.*; using System.Operations.*; "
            + "using System.Execution.*; namespace Programs; ";

    private static String listing(final String source) {
        final String whole = source.contains("namespace ") ? source : PRELUDE + source;
        final CannonCompiler.Result built =
                CannonCompiler.compile(List.of(new SourceFile("Script.can", whole)));
        if (!built.ok()) {
            throw new IllegalStateException(String.join("\n", built.lines()));
        }
        return built.assembly();
    }

    /** A computer with enough hardware to run and a system on it. */
    private static CraftingComputerBlockEntity computer(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, ComputingModule.CRAFTING_COMPUTER.get());
        if (!(helper.getBlockEntity(at) instanceof CraftingComputerBlockEntity computer)) {
            helper.fail("no computer at " + at);
            return null;
        }
        final ItemStackHandler hw = computer.getHardware();
        hw.setStackInSlot(CraftingComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_ATX_P.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.CPU_SLOT,
                new ItemStack(ComputingModule.CPU_ASCENT_965.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.PSU_SLOT,
                new ItemStack(ComputingModule.PSU_650G.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        computer.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp"));
        return computer;
    }

    private static ILanguageProcess only(final MachinePrograms programs) {
        return programs.all().getFirst().process();
    }

    /** A Mainframe up and running Frames 95, since it ticks itself rather than the way other computers do. */
    private static MainframeBlockEntity mainframe(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(at) instanceof MainframeBlockEntity mainframe)) {
            helper.fail("no Mainframe at " + at);
            return null;
        }
        final ItemStackHandler inv = mainframe.getInventory();
        inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT, new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
        inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        mainframe.togglePower();
        if (!mainframe.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_95"))) {
            helper.fail("could not install Frames 95 on the Mainframe");
            return null;
        }
        return mainframe;
    }

    /**
     * The world's own ticks reach a script on a Mainframe.
     *
     * <p>The other tests tick the programs by hand; this one leaves it to the machine, on the one
     * computer that runs its own tick instead of the shared one, which is where a script could sit
     * started and never be called.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void programs_areTickedByAMainframeOnItsOwn(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, new BlockPos(2, 2, 2));
        if (mainframe == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    final MachinePrograms.Started started =
                            mainframe.cannon().start("counter.asm", listing(COUNTER), 1, mainframe);
                    helper.assertTrue(started.ok(), "it starts on the Mainframe: " + started.message());
                })
                .thenExecuteAfter(6, () -> {
                    final List<String> said = only(mainframe.cannon()).console();
                    helper.assertTrue(said.size() >= 4 && said.get(0).equals("up") && said.get(1).equals("tick 1"),
                            "the world ticks the script without anybody asking; got " + said);
                })
                .thenExecuteAfter(1, () -> {
                    mainframe.togglePower();
                })
                .thenExecuteAfter(4, () -> {
                    helper.assertTrue(mainframe.cannon().isEmpty(),
                            "switching the cabinet off stops what it was running");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void programs_callAScriptOnceWhenItStartsAndOnceATickAfter(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms programs = computer.cannon();
                    final MachinePrograms.Started started =
                            programs.start("counter.asm", listing(COUNTER), 1, computer);
                    helper.assertTrue(started.ok(), "it starts: " + started.message());
                    for (int i = 0; i < 4; i++) {
                        programs.tick(512);
                    }
                    final List<String> said = only(programs).console();
                    helper.assertTrue(said.equals(List.of("up", "tick 1", "tick 2", "tick 3")),
                            "four ticks say what they did; got " + said);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void programs_shareOneTickBetweenEveryProgramRunning(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms programs = computer.cannon();
                    final String listing = listing(COUNTER);
                    programs.start("one.asm", listing, 1, computer);
                    programs.start("two.asm", listing, 1, computer);
                    programs.tick(9);
                    final int first = programs.all().getFirst().process().spent();
                    final int second = programs.all().get(1).process().spent();
                    helper.assertTrue(first + second == 9,
                            "the whole tick is spent; got " + first + " and " + second);
                    helper.assertTrue(Math.abs(first - second) <= 1,
                            "and evenly between them; got " + first + " and " + second);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void programs_carryEveryProgramThroughASaveAndOn(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms before = computer.cannon();
                    before.start("counter.asm", listing(COUNTER), 2, computer);
                    before.tick(512);
                    before.tick(512);
                    final CompoundTag tag = new CompoundTag();
                    before.save(tag);

                    final MachinePrograms after = new MachinePrograms();
                    after.load(tag, computer);
                    helper.assertTrue(after.all().size() == 1,
                            "one program comes back; got " + after.all().size());
                    helper.assertTrue(after.all().getFirst().heapMb() == 2,
                            "with the memory it was given; got " + after.all().getFirst().heapMb());
                    after.tick(512);
                    final List<String> said = only(after).console();
                    helper.assertTrue(said.equals(List.of("up", "tick 1", "tick 2")),
                            "and carries on counting; got " + said);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void programs_letAProgramSayGoodbyeWhenItIsStopped(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms programs = computer.cannon();
                    final int id = programs.start("counter.asm", listing(COUNTER), 1, computer).id();
                    programs.tick(512);
                    final ILanguageProcess running = only(programs);
                    helper.assertTrue(programs.stop(id), "it stops");
                    helper.assertTrue(programs.isEmpty(), "and is gone from the list");
                    helper.assertTrue(running.console().contains("down"),
                            "having said goodbye; got " + running.console());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void programs_runATerminalProgramOnceAndAreDoneWithIt(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms programs = computer.cannon();
                    final MachinePrograms.Started started =
                            programs.start("hello.asm", listing(HELLO), 1, computer);
                    helper.assertTrue(started.ok(), "it starts: " + started.message());
                    final ILanguageProcess running = only(programs);
                    programs.tick(512);
                    helper.assertTrue(running.console().equals(List.of("hi 0", "hi 1", "hi 2")),
                            "it says its piece; got " + running.console());
                    programs.tick(512);
                    helper.assertTrue(programs.isEmpty(),
                            "and is gone, not asked again; " + programs.all().size() + " left");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void programs_keepAFinishedTerminalProgramWhileTheTerminalHasIt(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms programs = computer.cannon();
                    final int id = programs.start("hello.asm", listing(HELLO), 1, computer).id();
                    programs.hold(id);
                    programs.tick(512);
                    programs.tick(512);
                    helper.assertTrue(programs.all().size() == 1, "it waits to be read");
                    helper.assertTrue(only(programs).state() == ILanguageProcess.State.FINISHED,
                            "having finished");
                    programs.release();
                    helper.assertTrue(programs.isEmpty(), "and goes once the terminal lets it");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void programs_handTheTerminalEachLineOnceAndOnlyOnce(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms programs = computer.cannon();
                    final int id = programs.start("hello.asm", listing(HELLO), 1, computer).id();
                    programs.hold(id);
                    helper.assertTrue(programs.unseen().isEmpty(), "nothing has been printed yet");
                    programs.tick(512);
                    final List<String> first = programs.unseen();
                    helper.assertTrue(first.equals(List.of("hi 0", "hi 1", "hi 2")),
                            "it hands over what was printed; got " + first);
                    helper.assertTrue(programs.unseen().isEmpty(),
                            "and does not hand the same lines twice");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void programs_giveTheTerminalTheLastOfALoudProgramWhenItScrolled(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms programs = computer.cannon();
                    final int id = programs.start("loud.asm", listing("""
                            class Loud {
                                static void Main() {
                                    for (int i = 0; i < 260; i++) { Console.PrintLine("line " + i); }
                                }
                            }
                            """), 1, computer).id();
                    programs.hold(id);
                    programs.tick(100000);
                    final List<String> seen = programs.unseen();
                    /*
                     * What fell off the end while nobody looked is gone, as it is on any terminal; what
                     * is left is the newest, in order, ending with the last thing the program said.
                     */
                    helper.assertTrue(seen.size() == ConsoleBuffer.MOST_LINES,
                            "it hands over everything still kept; got " + seen.size());
                    helper.assertTrue("line 259".equals(seen.getLast()),
                            "ending with the last; got " + seen.getLast());
                    helper.assertTrue("line 60".equals(seen.getFirst()),
                            "starting where it scrolled; got " + seen.getFirst());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void programs_refuseAListingTheyCannotRead(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms programs = computer.cannon();
                    helper.assertFalse(
                            programs.start("broken.asm", "this is not an assembly", 1, computer).ok(),
                            "it does not start");
                    helper.assertTrue(programs.isEmpty(), "and nothing is left running");
                    // And a file no registered language claims is refused by name, not by guessing.
                    helper.assertFalse(programs.start("thing.zz", "whatever", 1, computer).ok(),
                            "nothing runs a .zz");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void programs_creditsFollowTheClockWithNoCeiling(final GameTestHelper helper) {
        helper.assertTrue(MachinePrograms.creditsFor(0) == 0, "no processor is worth nothing");
        helper.assertTrue(MachinePrograms.creditsFor(100) == MachinePrograms.LEAST_PER_TICK,
                "the slowest machine still moves; got " + MachinePrograms.creditsFor(100));
        helper.assertTrue(MachinePrograms.creditsFor(700) == 87,
                "an early one follows its clock; got " + MachinePrograms.creditsFor(700));
        helper.assertTrue(MachinePrograms.creditsFor(4000) == 500,
                "a middling one too; got " + MachinePrograms.creditsFor(4000));
        helper.assertTrue(MachinePrograms.creditsFor(19_200) == 2400,
                "and the fastest is not capped; got " + MachinePrograms.creditsFor(19_200));
        helper.assertTrue(MachinePrograms.creditsFor(1_000_000) == 125_000,
                "however fast it gets; got " + MachinePrograms.creditsFor(1_000_000));
        helper.succeed();
    }

    /**
     * The server's clock bounds the tick, and a machine that went without runs next tick.
     *
     * <p>Run on deadlines of the test's own rather than the balance, so nothing else ticking on the
     * server at the same time is starved by it or starves it.
     */
    @GameTest(template = ARENA)
    public static void programs_aMachineTheServerHadNoTimeForRunsFirstNextTick(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos one = helper.absolutePos(new BlockPos(1, 2, 1));
        final BlockPos two = helper.absolutePos(new BlockPos(3, 2, 1));
        final long machineNanos = 1_000_000L;
        final ServerTickDeadline starved = new ServerTickDeadline(() -> 0L, () -> machineNanos);
        final ServerTickDeadline roomy = new ServerTickDeadline(() -> 8_000_000L, () -> machineNanos);
        helper.startSequence()
                .thenExecute(() -> {
                    helper.assertTrue(starved.claim(level, one) == ServerTickDeadline.NONE,
                            "a server with no time for programs gives the first machine none");
                    helper.assertTrue(starved.claim(level, two) == ServerTickDeadline.NONE,
                            "nor the second");
                    final long before = System.nanoTime();
                    final long given = roomy.claim(level, one);
                    helper.assertTrue(given != ServerTickDeadline.NONE && given > before,
                            "a server with time gives a deadline ahead");
                    helper.assertTrue(given - before <= 2 * machineNanos,
                            "bounded by the machine's own, not the server's; got " + (given - before));
                })
                .thenExecuteAfter(1, () -> {
                    helper.assertTrue(starved.claim(level, one) != ServerTickDeadline.NONE,
                            "a machine that went without runs next tick whatever the server has");
                    helper.assertTrue(starved.claim(level, two) != ServerTickDeadline.NONE,
                            "and so does the other");
                })
                .thenExecuteAfter(1, () -> {
                    helper.assertTrue(starved.claim(level, one) == ServerTickDeadline.NONE,
                            "and waits again the tick after");
                })
                .thenSucceed();
    }

    /** A script whose thread counts under a lock, giving way each round, so a save has to keep both. */
    private static final String COUNTING_THREAD = """
            using System.*;
            using System.IO.*;
            using System.Collections.*;
            using System.Threading.*;
            namespace Programs;
            class Counting : IScript {
                object gate = new List<int>();
                int seen;

                public void OnInit() {
                    Thread.Start(() => {
                        while (true) {
                            lock (gate) { seen = seen + 1; Console.PrintLine("t" + seen); }
                            Thread.Yield();
                        }
                    });
                }
                public void OnTick() { }
                public void OnDestroy() { }
            }
            """;

    /** Whether the lines count up from t1 with nothing missing and nothing said twice. */
    private static boolean countsUp(final List<String> said) {
        for (int i = 0; i < said.size(); i++) {
            if (!said.get(i).equals("t" + (i + 1))) {
                return false;
            }
        }
        return !said.isEmpty();
    }

    @GameTest(template = ARENA)
    public static void programs_carryAThreadAndItsLockThroughASave(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms before = computer.cannon();
                    final MachinePrograms.Started started =
                            before.start("counting.asm", listing(COUNTING_THREAD), 1, computer);
                    helper.assertTrue(started.ok(), "it starts: " + started.message());
                    before.tick(512);
                    final List<String> said = only(before).console();
                    helper.assertTrue(countsUp(said), "the thread counts as far as the tick lets it; got " + said);

                    final CompoundTag tag = new CompoundTag();
                    before.save(tag);
                    final MachinePrograms after = new MachinePrograms();
                    after.load(tag, computer);
                    helper.assertTrue(after.all().size() == 1, "the program comes back");
                    after.tick(512);
                    final List<String> more = only(after).console();
                    helper.assertTrue(more.size() > said.size() && countsUp(more)
                                    && more.subList(0, said.size()).equals(said),
                            "and the thread carries on counting after the save, lock and all; got " + more);
                })
                .thenSucceed();
    }

    /** A program that starts another from the disk, waits for it and reads what it left. */
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

    private static final String CHILD = """
            using System.*;
            using System.IO.*;
            using System.Execution.*;
            namespace Programs;
            class Child {
                static void Main() {
                    Console.PrintLine("hello " + Program.Args.Get(0));
                    Program.Exit(4);
                }
            }
            """;

    @GameTest(template = ARENA)
    public static void programs_startAnotherFromTheDiskAndReadWhatItLeft(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final dev.jstech.computers.program.ServerCliComputer shell =
                            new dev.jstech.computers.program.ServerCliComputer(computer, helper.getLevel());
                    helper.assertTrue(shell.writeFile("C:\\child.asm", listing(CHILD)).ok(),
                            "the child is on the disk");
                    final MachinePrograms programs = computer.cannon();
                    final MachinePrograms.Started started =
                            programs.start("parent.asm", listing(PARENT), 1, computer);
                    helper.assertTrue(started.ok(), "the parent starts: " + started.message());
                    final ILanguageProcess parent = programs.byId(started.id()).process();
                    int ticks = 0;
                    while (parent.console().size() < 3 && ticks++ < 12) {
                        programs.tick(2048);
                    }
                    helper.assertTrue(parent.console().equals(List.of("started child.asm", "child: hello a", "code 4")),
                            "the parent started the child, waited, and read it; got " + parent.console()
                                    + " (" + parent.message() + ")");
                    helper.assertTrue(programs.all().size() == 2,
                            "the finished child stays for the parent to read; got " + programs.all().size());
                    programs.tick(2048);
                    programs.tick(2048);
                    helper.assertTrue(programs.isEmpty(),
                            "and both are gone once the parent is; " + programs.all().size() + " left");
                })
                .thenSucceed();
    }

    private static final String LISTENER = """
            using System.*;
            using System.IO.*;
            using System.Execution.*;
            namespace Programs;
            class Listener : IScript {
                public void OnInit() {
                    Program.OnMessage(m => Console.PrintLine(m.From + ":" + m.Text));
                }
                public void OnTick() { }
                public void OnDestroy() { }
            }
            """;

    private static final String SENDER = """
            using System.*;
            using System.IO.*;
            using System.Utils.*;
            using System.Execution.*;
            namespace Programs;
            class Sender {
                static void Main() {
                    bool ok = Process.Send(Convert.ToInt(Program.Args.Get(0)), "ping");
                    Console.PrintLine("sent " + ok);
                }
            }
            """;

    @GameTest(template = ARENA)
    public static void programs_passALineFromOneToAnother(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms programs = computer.cannon();
                    final int listener = programs.start("listener.asm", listing(LISTENER), 1, computer).id();
                    programs.tick(2048);
                    final MachinePrograms.Started sender = programs.start("sender.asm", listing(SENDER), 1,
                            computer, List.of(String.valueOf(listener)), 0, MachinePrograms.DEFAULT_PRIORITY);
                    helper.assertTrue(sender.ok(), "the sender starts: " + sender.message());
                    for (int i = 0; i < 3; i++) {
                        programs.tick(2048);
                    }
                    final List<String> heard = programs.byId(listener).process().console();
                    helper.assertTrue(heard.equals(List.of(sender.id() + ":ping")),
                            "the listener heard the line and who sent it; got " + heard);
                    final MachinePrograms.Live sent = programs.byId(sender.id());
                    helper.assertTrue(sent == null || sent.process().console().equals(List.of("sent true")),
                            "the sender was told it was taken; got "
                                    + (sent == null ? "gone" : sent.process().console()));
                })
                .thenSucceed();
    }
}
