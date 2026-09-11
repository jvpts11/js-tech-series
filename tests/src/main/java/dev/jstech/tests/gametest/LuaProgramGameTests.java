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
import dev.jstech.computers.cannon.lua.lib.LuaTerminal;
import dev.jstech.computers.cannon.machine.LuaLanguage;
import dev.jstech.computers.cannon.machine.MachinePrograms;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.core.JsCore;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.core.language.IProgrammingLanguage;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Lua programs on a real machine: run from the prompt as they are, compiled by {@code cannonc},
 * refused with the place of their mistake, and carried through a save in the middle of a coroutine.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LuaProgramGameTests {

    private LuaProgramGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 2;

    private static final String ASK = """
            io.write("name? ")
            local name = io.read()
            print("hi " .. name)
            """;

    private static final String GENERATOR = """
            local gen = coroutine.wrap(function()
              for i = 1, 6 do coroutine.yield(i) end
            end)
            local total = 0
            for v in gen do
              total = total + v
              print("step " .. v)
            end
            print("total " .. total)
            """;

    private static final String FILES = """
            print(shell.getRunningProgram())
            print(shell.dir())
            local h = fs.open("progs/out.txt", "w")
            h.write("from lua")
            h.close()
            print(fs.exists("progs/out.txt"), fs.isDir("progs"), fs.getSize("progs/out.txt"))
            print(table.concat(fs.list("progs"), ","))
            print(fs.open("progs/save.lua", "r").readLine())
            print(os.getComputerID() >= 0, os.getComputerLabel() ~= nil)
            """;

    private static final String DRAW = """
            term.setCursorPos(1, 2)
            term.write("kept")
            local _, v = os.pullEvent("go")
            print("got " .. v)
            """;

    private static CraftingComputerBlockEntity computer(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, ComputingModule.CRAFTING_COMPUTER.get());
        if (!(helper.getBlockEntity(at) instanceof CraftingComputerBlockEntity computer)) {
            helper.fail("no computer at " + at);
            return null;
        }
        final ItemStackHandler hw = computer.getHardware();
        hw.setStackInSlot(CraftingComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(ComputingModule.MOTHERBOARD_ATX_P.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.CPU_SLOT, new ItemStack(ComputingModule.CPU_ASCENT_965.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.RAM_SLOTS_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        computer.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp"));
        for (final String id : new String[] {"cannonc", "cannonrt", "lrt"}) {
            computer.console().install(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, id).toString());
        }
        return computer;
    }

    private static String text(final CliShell.Response response) {
        final StringBuilder out = new StringBuilder();
        for (final CliLine line : response.lines()) {
            out.append(line.text()).append('\n');
        }
        return out.toString();
    }

    private static void write(final CraftingComputerBlockEntity computer, final String path, final FileType type,
                              final String text) {
        DiskFilesystem.write(computer.systemDisk(), path, type, text, Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
    }

    /** The machines know Lua as a language: a {@code .lua} is its source and something it runs. */
    @GameTest(template = ARENA)
    public static void lua_isALanguageTheMachinesKnow(final GameTestHelper helper) {
        final IProgrammingLanguage found = JsCore.languages().byExtension("lua");
        helper.assertTrue(found == LuaLanguage.INSTANCE, "a .lua belongs to Lua; got " + found);
        helper.assertTrue(JsCore.languages().runnerOf("lua") == LuaLanguage.INSTANCE, "and Lua runs it");
        helper.assertTrue(FileType.fromExtension("lua").orElse(null) == FileType.LUA, "a .lua is a file a disk holds");
        final List<IProgrammingLanguage.Token> tokens = LuaLanguage.INSTANCE.tokenize("local x = 1 -- note");
        helper.assertTrue(tokens.getFirst().kind() == IProgrammingLanguage.Kind.KEYWORD, "local is a keyword");
        helper.assertTrue(tokens.getLast().kind() == IProgrammingLanguage.Kind.COMMENT, "the comment is coloured too");
        helper.succeed();
    }

    /**
     * {@code lrt run} starts a Lua file as it is: it prompts, stops on the read, and finishes with
     * what was typed. What the machine keeps is the listing it compiled, under the file's own name.
     */
    @GameTest(template = ARENA)
    public static void lua_runsFromThePromptAndReadsALine(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        computer.togglePower();
        final ILanguageProcess[] asked = new ILanguageProcess[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    write(computer, "progs/ask.lua", FileType.LUA, ASK);
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String ran = text(shell.run("lrt run progs/ask.lua", cli));
                    helper.assertFalse(ran.contains("cannon:") || ran.contains("nothing installed"),
                            "the Lua file starts at the terminal; got " + ran);
                })
                .thenExecuteAfter(SETTLE * 4, () -> {
                    final MachinePrograms.Live one = computer.cannon().byId(computer.cannon().held());
                    helper.assertTrue(one != null, "the terminal is holding the program");
                    helper.assertTrue("ask.lua".equals(one.file()), "listed by its file; got " + one.file());
                    helper.assertTrue(one.binary().startsWith(".asm "), "what the machine keeps is the listing");
                    helper.assertTrue(one.process().waitingForInput(), "it stops on its read");
                    helper.assertTrue(one.process().console().contains("name? "),
                            "the prompt is shown before the wait; console " + one.process().console());
                    asked[0] = one.process();
                    helper.assertTrue(computer.cannon().offerInput("Ada"), "a typed line goes to the program in front");
                })
                .thenExecuteAfter(SETTLE * 4, () -> {
                    helper.assertTrue(asked[0].console().contains("hi Ada"),
                            "the line typed is what the program read; console " + asked[0].console());
                    helper.assertTrue(asked[0].state() == ILanguageProcess.State.FINISHED,
                            "and it returned; state " + asked[0].state());
                })
                .thenSucceed();
    }

    /** A Lua file that does not read is refused at the prompt with its own name and the place of the mistake. */
    @GameTest(template = ARENA)
    public static void lua_aFileThatDoesNotReadIsRefusedWithWhereItWentWrong(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    write(computer, "progs/bad.lua", FileType.LUA, "local x = = 1\n");
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String ran = text(shell.run("lrt run progs/bad.lua", cli));
                    helper.assertTrue(ran.contains("bad.lua(1,11)") && ran.contains("unexpected symbol near '='"),
                            "the mistake is named where it is; got " + ran);
                    helper.assertTrue(computer.cannon().all().isEmpty(), "and nothing was started");
                })
                .thenSucceed();
    }

    /**
     * Lua and Cannon keep to their own runtimes: the Cannon verbs send a Lua file to lrt, lrt sends a
     * Cannon one back, and each lists only its own programs.
     */
    @GameTest(template = ARENA)
    public static void runtimes_eachTakeOnlyTheirOwnLanguage(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    write(computer, "progs/sum.lua", FileType.LUA, "local s = 0 for i = 1, 10 do s = s + i end print(s)");
                    write(computer, "progs/hi.can", FileType.CAN, "using System.IO.*; namespace P; "
                            + "class Hi { static void Main() { Console.PrintLine(\"hi\"); } }");
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String cannon = text(shell.run("cannon run progs/sum.lua", cli));
                    helper.assertTrue(cannon.contains("is a Lua program; run it with lrt run progs/sum.lua"),
                            "cannon sends a Lua file to lrt; got " + cannon);
                    final String compiled = text(shell.run("cannonc progs/sum.lua", cli));
                    helper.assertTrue(compiled.contains("is a Lua program") && !compiled.contains("wrote"),
                            "cannonc does not compile Lua; got " + compiled);
                    final String lrt = text(shell.run("lrt run progs/hi.can", cli));
                    helper.assertTrue(lrt.contains("is a Cannon program; run it with cannon run progs/hi.can"),
                            "lrt sends a Cannon file back; got " + lrt);
                    helper.assertTrue(computer.cannon().all().isEmpty(), "and nothing was started");
                    final MachinePrograms.Started started = computer.cannon().start("sum.lua",
                            cli.readFile("progs/sum.lua").message(), 1, computer);
                    helper.assertTrue(started.ok(), "the Lua program starts: " + started.message());
                    final String listed = text(shell.run("lrt ps", cli));
                    helper.assertTrue(listed.contains("lrt") && !listed.contains("cannonrt"),
                            "lrt lists it, as its own; got " + listed);
                    final String others = text(shell.run("cannon ps", cli));
                    helper.assertTrue(others.contains("no Cannon programs"), "cannon does not; got " + others);
                })
                .thenSucceed();
    }

    /** Without the Lua runtime installed there is no lrt to type, as with any package's verb. */
    @GameTest(template = ARENA)
    public static void lrt_isAVerbOnlyOnceItsPackageIsInstalled(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    computer.console().uninstall(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "lrt").toString());
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String typed = text(shell.run("lrt ps", cli));
                    helper.assertTrue(!typed.contains("no Lua programs"), "lrt is not there; got " + typed);
                })
                .thenSucceed();
    }

    /**
     * A Cannon program that includes a Lua file beside it runs from the prompt: the file is read and
     * compiled with it, and the program calls into it.
     */
    @GameTest(template = ARENA)
    public static void include_runsACannonProgramThatCallsALuaFileBesideIt(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    write(computer, "progs/reactor.lua", FileType.LUA, "function Heat(a, b) return a * b end");
                    write(computer, "progs/plant.can", FileType.CAN, "include \"reactor.lua\";\nusing System.IO.*;\n"
                            + "namespace P;\nclass Plant { static void Main() { "
                            + "Console.PrintLine(\"heat \" + (int) reactor.Heat(6, 7)); } }");
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String compiled = text(shell.run("cannonc progs/plant.can", cli));
                    helper.assertTrue(compiled.contains("wrote progs/plant.asm"), "it compiles with the file; got " + compiled);
                    final MachinePrograms.Started started = computer.cannon().start("plant.can",
                            cli.readFile("progs/plant.can").message(), 1, computer, List.of(), 0,
                            MachinePrograms.DEFAULT_PRIORITY, cli.besideReader("progs/plant.can"));
                    helper.assertTrue(started.ok(), "the source starts with what it includes: " + started.message());
                    final ILanguageProcess process = computer.cannon().byId(started.id()).process();
                    computer.cannon().tick(8192);
                    helper.assertTrue(process.console().equals(List.of("heat 42")), "and calls it; got " + process.console());
                })
                .thenSucceed();
    }

    /** A Cannon source file runs from the prompt as it is too, compiled on the way in. */
    @GameTest(template = ARENA)
    public static void cannon_runsACannonSourceFileAsItIs(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = computer.cannon().start("hello.can", """
                            using System.IO.*;
                            namespace Programs;
                            class Hello { static void Main() { Console.PrintLine("from source"); } }
                            """, 1, computer);
                    helper.assertTrue(started.ok(), "the source starts: " + started.message());
                    final ILanguageProcess process = computer.cannon().byId(started.id()).process();
                    computer.cannon().tick(4096);
                    helper.assertTrue(process.console().equals(List.of("from source")),
                            "and runs; got " + process.console());
                })
                .thenSucceed();
    }

    /**
     * A Lua program's fs is the machine's disk, by ComputerCraft's paths from the root, and its shell
     * knows the file it was started from and the folder that is in.
     */
    @GameTest(template = ARENA)
    public static void lua_fsIsTheMachinesDiskAndTheShellKnowsWhereItIs(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        computer.togglePower();
        final ILanguageProcess[] ran = new ILanguageProcess[1];
        final ServerCliComputer[] cli = new ServerCliComputer[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    write(computer, "progs/save.lua", FileType.LUA, FILES);
                    cli[0] = new ServerCliComputer(computer, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli[0], 52);
                    final String typed = text(shell.run("lrt run progs/save.lua", cli[0]));
                    helper.assertFalse(typed.contains("lrt:"), "it starts; got " + typed);
                    final MachinePrograms.Live one = computer.cannon().byId(computer.cannon().held());
                    helper.assertTrue(one != null, "the terminal is holding the program");
                    ran[0] = one.process();
                })
                .thenExecuteAfter(SETTLE * 4, () -> {
                    final List<String> said = ran[0].console();
                    helper.assertTrue(ran[0].state() == ILanguageProcess.State.FINISHED,
                            "it finished; state " + ran[0].state() + ", console " + said);
                    helper.assertTrue(said.size() == 6, "six lines; got " + said);
                    helper.assertTrue("progs/save.lua".equals(said.get(0)), "its own path; got " + said.get(0));
                    helper.assertTrue("progs".equals(said.get(1)), "the folder it is in; got " + said.get(1));
                    helper.assertTrue("true\ttrue\t8".equals(said.get(2)), "what it wrote is there; got " + said.get(2));
                    helper.assertTrue(said.get(3).contains("out.txt") && said.get(3).contains("save.lua"),
                            "the folder lists both; got " + said.get(3));
                    helper.assertTrue("print(shell.getRunningProgram())".equals(said.get(4)),
                            "it reads its own file; got " + said.get(4));
                    helper.assertTrue("true\ttrue".equals(said.get(5)), "it knows the machine; got " + said.get(5));
                    helper.assertTrue("from lua".equals(cli[0].readFile("progs/out.txt").message()),
                            "the shell reads what the program wrote");
                })
                .thenSucceed();
    }

    /** A Lua program's sleep waits on the world's clock, spending nothing until its timer comes. */
    @GameTest(template = ARENA)
    public static void lua_sleepWaitsOnTheWorldsClock(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        computer.togglePower();
        final ILanguageProcess[] nap = new ILanguageProcess[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = computer.cannon().start("nap.lua",
                            "sleep(1) print('woke')", 1, computer);
                    helper.assertTrue(started.ok(), "it starts: " + started.message());
                    nap[0] = computer.cannon().byId(started.id()).process();
                })
                .thenExecuteAfter(SETTLE * 2, () -> {
                    helper.assertTrue(nap[0].state() == ILanguageProcess.State.PARKED,
                            "it waits on its timer; state " + nap[0].state());
                    helper.assertTrue(nap[0].console().isEmpty(), "and has not woken yet");
                })
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(nap[0].console().equals(List.of("woke")), "it woke; got " + nap[0].console());
                    helper.assertTrue(nap[0].state() == ILanguageProcess.State.FINISHED, "and returned");
                })
                .thenSucceed();
    }

    /**
     * A Lua program's screen and the event it waits on come back through a save: what it drew is still
     * there, and the event it wanted, queued afterwards, lets it finish.
     */
    @GameTest(template = ARENA)
    public static void lua_theScreenAndTheWaitCarryThroughASave(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms before = computer.cannon();
                    final MachinePrograms.Started started = before.start("draw.lua", DRAW, 1, computer);
                    helper.assertTrue(started.ok(), "it starts: " + started.message());
                    before.tick(4096);
                    helper.assertTrue(before.terminalOf(started.id()) != null, "it has a screen");
                    final CompoundTag tag = new CompoundTag();
                    before.save(tag);

                    final MachinePrograms after = new MachinePrograms();
                    after.load(tag, computer);
                    final MachinePrograms.Live back = after.all().getFirst();
                    final LuaTerminal screen = after.terminalOf(back.id());
                    helper.assertTrue(screen != null && screen.row(1).startsWith("kept"),
                            "what it drew comes back; got " + (screen == null ? "no screen" : screen.row(1)));
                    helper.assertTrue(after.queueEvent(back.id(), new ArrayList<>(List.of("go", 5L))),
                            "the event reaches it");
                    after.tick(4096);
                    helper.assertTrue(back.process().console().equals(List.of("got 5")),
                            "and it carries on with it; got " + back.process().console());
                })
                .thenSucceed();
    }

    /**
     * A Lua program saved in the middle of a coroutine comes back where it was and finishes as if the
     * world had never been put away.
     */
    @GameTest(template = ARENA)
    public static void lua_aProgramCarriesOnThroughASaveInTheMiddleOfACoroutine(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms before = computer.cannon();
                    final MachinePrograms.Started started = before.start("gen.lua", GENERATOR, 1, computer);
                    helper.assertTrue(started.ok(), "it starts: " + started.message());
                    before.tick(60);
                    final List<String> early = before.byId(started.id()).process().console();
                    helper.assertTrue(!early.contains("total 21"), "it is still in the middle; got " + early);
                    final CompoundTag tag = new CompoundTag();
                    before.save(tag);

                    final MachinePrograms after = new MachinePrograms();
                    after.load(tag, computer);
                    helper.assertTrue(after.all().size() == 1, "it comes back; got " + after.all().size());
                    final ILanguageProcess process = after.all().getFirst().process();
                    for (int i = 0; i < 20; i++) {
                        after.tick(4096);
                    }
                    helper.assertTrue(process.console().getLast().equals("total 21"),
                            "and finishes as if never put away; got " + process.console());
                    helper.assertTrue(process.console().stream().filter(line -> line.startsWith("step ")).count() == 6,
                            "every step once; got " + process.console());
                })
                .thenSucceed();
    }
}
