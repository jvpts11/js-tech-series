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
        for (final String id : new String[] {"cannonc", "cannonrt"}) {
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
     * {@code cannon run} starts a Lua file as it is: it prompts, stops on the read, and finishes with
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
                    final String ran = text(shell.run("cannon run progs/ask.lua", cli));
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
                    final String ran = text(shell.run("cannon run progs/bad.lua", cli));
                    helper.assertTrue(ran.contains("bad.lua(1,11)") && ran.contains("unexpected symbol near '='"),
                            "the mistake is named where it is; got " + ran);
                    helper.assertTrue(computer.cannon().all().isEmpty(), "and nothing was started");
                })
                .thenSucceed();
    }

    /** {@code cannonc} compiles a Lua file to a listing like any other, and that listing runs. */
    @GameTest(template = ARENA)
    public static void cannonc_compilesALuaFileToAListingThatRuns(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2));
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    write(computer, "progs/sum.lua", FileType.LUA, "local s = 0 for i = 1, 10 do s = s + i end print(s)");
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String compiled = text(shell.run("cannonc progs/sum.lua", cli));
                    helper.assertTrue(compiled.contains("wrote progs/sum.asm"), "it writes the listing; got " + compiled);
                    final MachinePrograms.Started started = computer.cannon().start("sum.asm",
                            cli.readFile("progs/sum.asm").message(), 1, computer);
                    helper.assertTrue(started.ok(), "the listing starts: " + started.message());
                    final ILanguageProcess process = computer.cannon().byId(started.id()).process();
                    computer.cannon().tick(4096);
                    helper.assertTrue(process.console().equals(List.of("55")), "and runs; got " + process.console());
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
