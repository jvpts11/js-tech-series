/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.MediaDevices;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.blockentity.ScannerBlockEntity;
import dev.jstech.computers.os.fs.BookFile;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.ScanFile;
import dev.jstech.computers.os.fs.ScanSeal;
import dev.jstech.computers.printer.Printers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The scanners (computers/docs/design/peripherals.md): a written book on the glass becomes a .book in the scans folder
 * of the computer at the cable's other end, the book staying where it was; a map becomes a sealed .map, which a
 * printer copies back onto an empty map from its tray as the same map; an empty glass scans nothing and says so.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ScannerGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(4, 2, 2);
    private static final BlockPos SCANNER = COMPUTER.south();
    private static final BlockPos PRINTER = COMPUTER.north();
    private static final int SETTLE = 10;

    private ScannerGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void writtenBook_scansIntoABookFileAndStaysOnTheGlass(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(
                COMPUTER);
        helper.setBlock(SCANNER, MediaDevices.SCANNER.get());
        final ScannerBlockEntity scanner = (ScannerBlockEntity) helper.getBlockEntity(SCANNER);
        final ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Mine Log"),
                "Steve", 0, List.of(Filterable.passThrough(Component.literal("Day 1: iron."))), true));
        scanner.glass().setStackInSlot(0, book);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, scanner::scan)
                .thenExecute(() -> helper.assertTrue(scanner.scanning(), "it reads the book; problem "
                        + scanner.problem()))
                .thenWaitUntil(() -> helper.assertTrue(scanner.scanned() == 1, "the book is read"))
                .thenExecute(() -> {
                    helper.assertTrue(scanner.problem() == ScannerBlockEntity.Problem.NONE,
                            "nothing went wrong; " + scanner.problem());
                    final String path = FsPaths.join(scanner.folder(), scanner.lastFile());
                    helper.assertTrue(scanner.lastFile().endsWith(".book"), "as a .book: " + scanner.lastFile());
                    final BookFile read = BookFile.read(DiskFilesystem.read(pc.systemDisk(), path).orElse(""));
                    helper.assertTrue("Mine Log".equals(read.title()) && "Steve".equals(read.author())
                            && read.pages().equals(List.of("Day 1: iron.")), "holding the book: " + read);
                    helper.assertTrue(scanner.onGlass().is(Items.WRITTEN_BOOK), "the book stays on the glass");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void emptyGlass_scansNothingAndSaysSo(final GameTestHelper helper) {
        TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(COMPUTER);
        helper.setBlock(SCANNER, MediaDevices.ADVANCED_SCANNER.get());
        final ScannerBlockEntity scanner = (ScannerBlockEntity) helper.getBlockEntity(SCANNER);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    scanner.scan();
                    helper.assertTrue(!scanner.scanning()
                            && scanner.problem() == ScannerBlockEntity.Problem.NOTHING_ON_GLASS,
                            "with nothing on the glass it does not start; " + scanner.problem());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void map_scansSealedAndPrintsBackAsTheSameMap(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(
                COMPUTER);
        helper.setBlock(SCANNER, MediaDevices.ADVANCED_SCANNER.get());
        helper.setBlock(PRINTER, ComputingModule.PRINTER.get());
        final ScannerBlockEntity scanner = (ScannerBlockEntity) helper.getBlockEntity(SCANNER);
        final PrinterBlockEntity printer = (PrinterBlockEntity) helper.getBlockEntity(PRINTER);
        final BlockPos here = helper.absolutePos(COMPUTER);
        final ItemStack map = MapItem.create(helper.getLevel(), here.getX(), here.getZ(), (byte) 0, false, false);
        final MapId id = map.get(DataComponents.MAP_ID);
        scanner.glass().setStackInSlot(0, map);
        printer.paper().setStackInSlot(0, new ItemStack(Items.MAP, 2));
        final String[] content = new String[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, scanner::scan)
                .thenWaitUntil(() -> helper.assertTrue(scanner.scanned() == 1, "the map is read"))
                .thenExecute(() -> {
                    helper.assertTrue(scanner.lastFile().endsWith(".map"), "as a .map: " + scanner.lastFile());
                    content[0] = DiskFilesystem.read(pc.systemDisk(),
                            FsPaths.join(scanner.folder(), scanner.lastFile())).orElse("");
                    final byte[] key = ScanSeal.key().orElseThrow();
                    final String body = ScanFile.open(content[0], key);
                    helper.assertTrue(body != null && ScanFile.mapOf(body).equals(OptionalInt.of(id.id())),
                            "sealed by this world, naming the map");
                    final Printers.Result changed = Printers.printMap(helper.getLevel(), printer, "map", "dev",
                            "Paint", content[0].replace("#map", "#map 9"), 1, "dev");
                    helper.assertTrue(!changed.ok(), "a .map whose words were changed does not print");
                    helper.assertTrue(Printers.printMap(helper.getLevel(), printer, "map", "dev", "Paint",
                            content[0], 1, "dev").ok(), "the scanned one prints");
                })
                .thenWaitUntil(() -> helper.assertTrue(printer.output().getStackInSlot(0).is(Items.FILLED_MAP),
                        "a filled map comes out"))
                .thenExecute(() -> {
                    helper.assertTrue(id.equals(printer.output().getStackInSlot(0).get(DataComponents.MAP_ID)),
                            "the same map as the one scanned");
                    helper.assertTrue(printer.paper().getStackInSlot(0).getCount() == 1,
                            "and it took one empty map from the tray");
                })
                .thenSucceed();
    }
}
