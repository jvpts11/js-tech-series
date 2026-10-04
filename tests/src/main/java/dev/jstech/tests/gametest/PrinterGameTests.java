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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.item.PrintedPaperItem;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.PixImage;
import dev.jstech.computers.printer.PrintedDocument;
import dev.jstech.computers.printer.PrinterModel;
import dev.jstech.computers.printer.Printers;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The printers: a document from a computer comes out of the printer linked to it as a Printed Paper, a sheet of the
 * tray a page and the page as long as the printer's sound; with no paper or no room in the output the queue waits,
 * paused it waits too; each copy comes out on its own; a full queue refuses more; the sound loops only while a page
 * comes out; and the shells print, MC-DOS's PRINT going resident once and the Unix family's lp and lpstat.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PrinterGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(4, 2, 2);
    private static final BlockPos PRINTER = COMPUTER.south();
    private static final int SETTLE = 10;
    private static final String NOTES = "notes.txt";

    private PrinterGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void document_comesOutAPageASheetAsAPrintedPaper(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computer(helper);
        final PrinterBlockEntity printer = printer(helper, ComputingModule.PRINTER.get());
        printer.paper().setStackInSlot(0, new ItemStack(Items.PAPER, 10));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(Printers.of(helper.getLevel(), pc).contains(printer),
                            "the printer beside the computer is one it prints on");
                    final Printers.Result result = Printers.print(helper.getLevel(), Printers.at(helper.getLevel(),
                            pc, -1L), twoPages(pc), 1, "dev");
                    helper.assertTrue(result.ok(), "the printer takes the document; said " + result.message());
                })
                .thenWaitUntil(() -> helper.assertTrue(!printer.output().getStackInSlot(0).isEmpty(),
                        "the document comes out"))
                .thenExecute(() -> {
                    final ItemStack sheet = printer.output().getStackInSlot(0);
                    helper.assertTrue(sheet.is(ComputingModule.PRINTED_PAPER.get()), "as a Printed Paper");
                    final PrintedDocument document = PrintedPaperItem.document(sheet);
                    helper.assertTrue(document.pages().size() == 2, "with its two pages");
                    helper.assertTrue(document.printerModel() == PrinterModel.PAKARD_LASERJOT_1102,
                            "looking like the laser printer's");
                    helper.assertTrue(printer.paperCount() == 8, "a sheet of paper for each page; left "
                            + printer.paperCount());
                    helper.assertTrue(printer.jobs().isEmpty() && !printer.printing(), "and the queue is done");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void queue_waitsForPaperAndPrintsWhenItComes(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computer(helper);
        final PrinterBlockEntity printer = printer(helper, ComputingModule.PRINTER.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> Printers.print(helper.getLevel(), printer, onePage(pc), 1, "dev"))
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(printer.waiting() == PrinterBlockEntity.Wait.NO_PAPER,
                            "with an empty tray the job waits; waiting " + printer.waiting());
                    helper.assertTrue(!printer.printing() && printer.loops().isEmpty(),
                            "nothing comes out and nothing is heard");
                    printer.paper().setStackInSlot(0, new ItemStack(Items.PAPER, 1));
                })
                .thenWaitUntil(() -> helper.assertTrue(!printer.output().getStackInSlot(0).isEmpty(),
                        "paper in the tray starts it"))
                .thenExecute(() -> helper.assertTrue(printer.paperCount() == 0, "and the sheet is used"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 500)
    public static void copies_eachComeOutOnTheirOwnSheet(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computer(helper);
        final PrinterBlockEntity printer = printer(helper, ComputingModule.PRINTER.get());
        printer.paper().setStackInSlot(0, new ItemStack(Items.PAPER, 5));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> Printers.print(helper.getLevel(), printer, onePage(pc), 2, "dev"))
                .thenWaitUntil(() -> helper.assertTrue(!printer.output().getStackInSlot(1).isEmpty(),
                        "the second copy comes out after the first"))
                .thenExecute(() -> {
                    helper.assertTrue(printer.paperCount() == 3, "a sheet for each copy; left " + printer.paperCount());
                    helper.assertTrue(printer.jobs().isEmpty(), "and the job is done");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void queue_waitsWhileTheOutputIsFullOrPaused(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computer(helper);
        final PrinterBlockEntity printer = printer(helper, ComputingModule.PRINTER.get());
        printer.paper().setStackInSlot(0, new ItemStack(Items.PAPER, 5));
        for (int i = 0; i < PrinterBlockEntity.OUTPUT_SLOTS; i++) {
            printer.output().setStackInSlot(i, new ItemStack(ComputingModule.PRINTED_PAPER.get()));
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> Printers.print(helper.getLevel(), printer, onePage(pc), 1, "dev"))
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(printer.waiting() == PrinterBlockEntity.Wait.OUTPUT_FULL,
                            "with the output full the job waits; waiting " + printer.waiting());
                    printer.output().setStackInSlot(0, ItemStack.EMPTY);
                    printer.togglePaused();
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(printer.waiting() == PrinterBlockEntity.Wait.PAUSED && !printer.printing(),
                            "paused, it waits with room in the output");
                    printer.cancelCurrent();
                    helper.assertTrue(printer.jobs().isEmpty() && printer.queueRows().isEmpty(),
                            "Cancel job drops it from the queue");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void queue_refusesPastItsLength(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computer(helper);
        final PrinterBlockEntity printer = printer(helper, ComputingModule.PRINTER.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    for (int i = 0; i < PrinterBlockEntity.QUEUE_MAX; i++) {
                        helper.assertTrue(Printers.print(helper.getLevel(), printer, onePage(pc), 1, "dev").ok(),
                                "the queue takes document " + (i + 1));
                    }
                    final Printers.Result refused = Printers.print(helper.getLevel(), printer, onePage(pc), 1, "dev");
                    helper.assertTrue(!refused.ok() && refused.message().english().contains("queue is full"),
                            "one more is refused, and why is said; said " + refused.message().english());
                    helper.assertTrue(!Printers.print(helper.getLevel(), null, onePage(pc), 1, "dev").ok(),
                            "with no printer nothing takes it");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void sound_loopsOnlyWhileAPageComesOut(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computer(helper);
        final PrinterBlockEntity printer = printer(helper, ComputingModule.VINTAGE_PRINTER.get());
        printer.paper().setStackInSlot(0, new ItemStack(Items.PAPER, 1));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(printer.loops().isEmpty(), "an idle printer is quiet");
                    Printers.print(helper.getLevel(), printer, onePage(pc), 1, "dev");
                })
                .thenExecuteAfter(5, () -> {
                    helper.assertTrue(printer.printing(), "the page is coming out");
                    helper.assertTrue(printer.loops().size() == 1 && printer.loops().getFirst().sound()
                                    == PrinterBlockEntity.sound(PrinterModel.EPSILON_FX_80),
                            "and the dot matrix's sound loops");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void picture_comesOutOneToASheet(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computer(helper);
        final PrinterBlockEntity printer = printer(helper, ComputingModule.ADVANCED_PRINTER.get());
        printer.paper().setStackInSlot(0, new ItemStack(Items.PAPER, 2));
        final PixImage picture = new PixImage(8, 8);
        picture.fillAll(1);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> Printers.print(helper.getLevel(), printer,
                        PrintedDocument.picture("house.pix", Printers.machineName(pc), "Paint", picture.encode(),
                                "house.pix"), 1, "dev"))
                .thenWaitUntil(() -> helper.assertTrue(!printer.output().getStackInSlot(0).isEmpty(),
                        "the picture comes out"))
                .thenExecute(() -> {
                    final PrintedDocument document = PrintedPaperItem.document(printer.output().getStackInSlot(0));
                    helper.assertTrue(document.isPicture() && "house.pix".equals(document.pictureName()),
                            "as a picture, with its file's name");
                    helper.assertTrue(printer.paperCount() == 1, "on one sheet");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void dosPrint_goesResidentOnceAndListsTheQueue(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computer(helper);
        final PrinterBlockEntity printer = printer(helper, ComputingModule.PRINTER.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    writeNotes(pc);
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    final String first = said(CliCommands.shellFor(cli, 80).run("print " + NOTES, cli));
                    helper.assertTrue(first.contains("Resident part of PRINT installed"),
                            "the first PRINT installs its resident part; said\n" + first);
                    helper.assertTrue(first.contains("NOTES.TXT is currently being printed"),
                            "and lists the file it sent; said\n" + first);
                    final String second = said(CliCommands.shellFor(cli, 80).run("print " + NOTES, cli));
                    helper.assertTrue(!second.contains("Resident part") && second.contains("is in queue"),
                            "the second is already resident and waits behind the first; said\n" + second);
                    helper.assertTrue(printer.jobs().size() == 2, "both are in the printer's queue");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void unixLp_answersARequestIdAndLpstatListsIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(
                COMPUTER, ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "ubuntu"));
        printer(helper, ComputingModule.LEGACY_PRINTER.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    writeNotes(pc);
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    final String sent = said(CliCommands.shellFor(cli, 80).run("lp -n 2 /" + NOTES, cli));
                    helper.assertTrue(sent.contains("request id is deskjot-1 (1 file)"),
                            "lp answers with the request id; said\n" + sent);
                    final String listed = said(CliCommands.shellFor(cli, 80).run("lpstat", cli));
                    helper.assertTrue(listed.contains("deskjot-1"), "lpstat lists it; said\n" + listed);
                    final String printers = said(CliCommands.shellFor(cli, 80).run("lpstat -p", cli));
                    helper.assertTrue(printers.contains("printer deskjot"), "and names the printer; said\n"
                            + printers);
                    final String wrong = said(CliCommands.shellFor(cli, 80).run("lp -d nowhere /" + NOTES, cli));
                    helper.assertTrue(wrong.contains("non-existent"), "a queue it has not is said so; said\n"
                            + wrong);
                })
                .thenSucceed();
    }

    /* A running Personal Computer with the default desktop system. */
    private static PersonalComputerBlockEntity computer(final GameTestHelper helper) {
        return TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(COMPUTER);
    }

    /* A printer of that block beside the computer, and its block entity. */
    private static PrinterBlockEntity printer(final GameTestHelper helper, final Block block) {
        helper.setBlock(PRINTER, block);
        return (PrinterBlockEntity) helper.getBlockEntity(PRINTER);
    }

    private static PrintedDocument onePage(final PersonalComputerBlockEntity pc) {
        return Printers.text("note.txt", Printers.machineName(pc), "Editor", "one page of print", false, 0, 0);
    }

    private static PrintedDocument twoPages(final PersonalComputerBlockEntity pc) {
        return Printers.text("log.txt", Printers.machineName(pc), "Editor", "line\n".repeat(40), false, 0, 0);
    }

    private static void writeNotes(final PersonalComputerBlockEntity pc) {
        DiskFilesystem.write(pc.systemDisk(), NOTES, FileType.TXT, "a note to print\nand a second line",
                Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
    }

    private static String said(final CliShell.Response response) {
        return response.lines().stream().map(CliLine::text).collect(Collectors.joining("\n"));
    }
}
