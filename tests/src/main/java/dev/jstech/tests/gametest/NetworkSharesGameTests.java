/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.machine.MachinePrograms;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A folder one machine shares is reachable from every other machine on its network, by the sharing
 * machine's host name, from the prompt and from a program: listed, read, written where the share
 * allows it, copied, and gone again when the share is closed.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkSharesGameTests {

    private NetworkSharesGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    /** A Mainframe and a personal computer on one network: the computer shares, the Mainframe reaches. */
    private record Pair(MainframeBlockEntity mainframe, PersonalComputerBlockEntity computer) {
    }

    private static Pair wire(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE.get());
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity computer = world.placeRunningPersonalComputer(new BlockPos(5, 2, 2));
        return new Pair(mainframe, computer);
    }

    private static boolean lists(final ICliComputer.FsResult listing, final String name) {
        return listing.ok() && listing.entries() != null
                && listing.entries().stream().anyMatch(entry -> entry.name().equalsIgnoreCase(name));
    }

    @GameTest(template = ARENA)
    public static void shares_reachAnotherMachinesFolderByItsHostName(final GameTestHelper helper) {
        final Pair pair = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    pair.computer().console().setComputerName("lab");
                    final ServerCliComputer lab = new ServerCliComputer(pair.computer(), helper.getLevel());
                    helper.assertTrue(lab.makeDir("C:\\pub").ok(), "the folder is made: " + lab.makeDir("C:\\pub").message());
                    helper.assertTrue(lab.writeFile("C:\\pub\\note.txt", "hi").ok(),
                            "the file is on the computer: " + lab.writeFile("C:\\pub\\note.txt", "hi").message());
                    final ICliComputer.OpResult shared = lab.setConfig("share", "C:\\pub write");
                    helper.assertTrue(shared.ok(), "the folder is shared: " + shared.message());
                    helper.assertTrue(shared.message().contains("\\\\lab\\pub"),
                            "and named for the folder; got " + shared.message());

                    final ServerCliComputer main = new ServerCliComputer(pair.mainframe(), helper.getLevel());
                    helper.assertTrue(lists(main.listDisk("\\\\"), "lab"),
                            "the network lists the host that shares something; got " + main.listDisk("\\\\").message());
                    helper.assertTrue(lists(main.listDisk("\\\\lab"), "pub"),
                            "the host lists its share; got " + main.listDisk("\\\\lab").message());
                    helper.assertTrue(lists(main.listDisk("\\\\lab\\pub"), "note.txt"),
                            "the share lists its files; got " + main.listDisk("\\\\lab\\pub").message());
                    helper.assertTrue("hi".equals(main.readFile("\\\\lab\\pub\\note.txt").message()),
                            "and the file reads across; got " + main.readFile("\\\\lab\\pub\\note.txt").message());
                    helper.assertTrue("hi".equals(main.readFile("/net/lab/pub/note.txt").message()),
                            "in the Linux spelling too");

                    helper.assertTrue(main.writeFile("\\\\lab\\pub\\new.txt", "x").ok(),
                            "a writable share takes a write; got " + main.writeFile("\\\\lab\\pub\\new.txt", "x").message());
                    helper.assertTrue("x".equals(lab.readFile("C:\\pub\\new.txt").message()),
                            "which landed on the computer's own disk");
                    final ICliComputer.FsResult copied = main.copyPath("\\\\lab\\pub\\note.txt", "\\\\lab\\pub\\copy.txt");
                    helper.assertTrue(copied.ok(), "a copy goes across the network; got " + copied.message());
                    helper.assertTrue("hi".equals(lab.readFile("C:\\pub\\copy.txt").message()), "and lands whole");
                    helper.assertTrue(main.makeDir("\\\\lab\\pub\\more").ok(), "a folder can be made in a writable share");
                    helper.assertTrue(main.deleteFile("\\\\lab\\pub\\copy.txt").ok(), "and a file deleted from it");
                    helper.assertTrue(!lab.readFile("C:\\pub\\copy.txt").ok(), "for real");

                    helper.assertTrue(lab.setConfig("share", "C:\\pub read").ok(), "the share is made read-only");
                    helper.assertTrue(!main.writeFile("\\\\lab\\pub\\again.txt", "y").ok(), "and refuses a write");
                    helper.assertTrue(!main.deleteFile("\\\\lab\\pub\\note.txt").ok(), "and a delete");
                    helper.assertTrue(main.readFile("\\\\lab\\pub\\note.txt").ok(), "while still reading");
                    helper.assertTrue(lists(main.listDisk("\\\\lab"), "pub")
                                    && main.listDisk("\\\\lab").entries().stream().allMatch(ICliComputer.FsEntry::readOnly),
                            "a read-only share is listed as such");

                    helper.assertTrue(!main.readFile("\\\\nowhere\\pub\\note.txt").ok(), "an unknown host is refused");
                    helper.assertTrue(!main.readFile("\\\\lab\\docs\\note.txt").ok(), "so is an unknown share");

                    helper.assertTrue(lab.setConfig("unshare", "pub").ok(), "the share is closed");
                    helper.assertTrue(!main.readFile("\\\\lab\\pub\\note.txt").ok(), "and nothing reads through it");
                    helper.assertTrue(!lists(main.listDisk("\\\\"), "lab"), "and the host drops off the network list");
                })
                .thenSucceed();
    }

    private static final String READER = """
            using System.*;
            using System.IO.*;
            namespace Programs;
            class Reader {
                static void Main() {
                    Console.PrintLine("read " + File.Read("//lab/pub/note.txt"));
                    Console.PrintLine("there " + File.Exists("//lab/pub/missing.txt"));
                    Console.PrintLine("wrote " + File.Write("//lab/pub/from-main.txt", "hello"));
                }
            }
            """;

    @GameTest(template = ARENA)
    public static void programs_readAnotherMachinesShareThroughTheFileApi(final GameTestHelper helper) {
        final Pair pair = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    pair.computer().console().setComputerName("lab");
                    final ServerCliComputer lab = new ServerCliComputer(pair.computer(), helper.getLevel());
                    helper.assertTrue(lab.makeDir("C:\\pub").ok(), "the folder is made");
                    helper.assertTrue(lab.writeFile("C:\\pub\\note.txt", "hi").ok(), "the file is on the computer");
                    helper.assertTrue(lab.setConfig("share", "C:\\pub write").ok(), "the folder is shared");

                    final CannonCompiler.Result built =
                            CannonCompiler.compile(List.of(new SourceFile("Reader.can", READER)));
                    helper.assertTrue(built.ok(), "the program compiles: " + String.join("\n", built.lines()));
                    final MachinePrograms programs = pair.mainframe().cannon();
                    final MachinePrograms.Started started =
                            programs.start("reader.asm", built.assembly(), 1, pair.mainframe());
                    helper.assertTrue(started.ok(), "it starts on the Mainframe: " + started.message());
                    programs.tick(4096);
                    final List<String> said = programs.byId(started.id()).process().console();
                    helper.assertTrue(said.equals(List.of("read hi", "there false", "wrote true")),
                            "a program reaches the share like any file; got " + said);
                    helper.assertTrue("hello".equals(lab.readFile("C:\\pub\\from-main.txt").message()),
                            "and what it wrote is on the other machine");
                })
                .thenSucceed();
    }
}
