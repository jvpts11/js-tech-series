/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.client.LargeScalePrinterScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.EditorApp;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.BookFile;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.ScanFile;
import dev.jstech.computers.printer.BookJob;
import dev.jstech.computers.printer.LargeScalePrinterBlockEntity;
import dev.jstech.computers.printer.LargeScalePrinters;
import dev.jstech.core.text.GameText;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The Large Scale Printer at work (computers/docs/design/peripherals.md): the machine built and formed, paper in its
 * feeder and three books queued from two computers, a signed book, an enchanted one and a book and quill; its window
 * while it prints the first, as the mocks show it; and the same queue once the server turns the printing of
 * enchanted books off, the enchanted one marked as refused there.
 */
public final class LargeScalePrinterClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final int WAIT = 400;
    /* A few books into the first job, so the window has copies made and a page under way. */
    private static final int PRINTING = 120;
    private static final BlockPos CORNER = new BlockPos(2, 2, 4);
    /*
     * A computer against the machine's Peripheral Port, on the east face of its middle layer, with its monitor and a
     * laser printer beside it on blocks of stone, and the player standing at the monitor.
     */
    private static final BlockPos COMPUTER = new BlockPos(6, 3, 6);
    private static final BlockPos MONITOR = COMPUTER.east();
    private static final BlockPos PRINTER = COMPUTER.south();
    private static final BlockPos PLAYER_AT_MONITOR = MONITOR.east(2);
    private static final String DOCUMENTS = "Users/Public/Documents";
    private static final String BOOK = DOCUMENTS + "/Mine Log.book";
    private static final String EDITOR = "jsc:editor";
    /* Before the machine's front and to its right, a few steps back from it. */
    private static final BlockPos IN_FRONT = CORNER.offset(6, 0, -4);

    private LargeScalePrinterClientTests() {
    }

    @ClientTest(timeoutTicks = 1600)
    public static void lsp_printsItsQueueAndMarksWhatTheServerRefuses(final ClientTestContext ctx) {
        final ChainMachine machine = ChainMachines.byId("large_scale_printer").orElseThrow();
        final BlockPos terminal = terminalOf(machine);
        ComputersServerConfig.FILE.set(ComputersServerConfig.PRINT_ENCHANTED_BOOKS, true);
        // The setting is put back however the test ends: it turns it off halfway, and a failure there would leave it.
        ctx.afterTest(() -> ComputersServerConfig.FILE.set(ComputersServerConfig.PRINT_ENCHANTED_BOOKS, true));
        ctx.thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent())
                .thenBuild(0, world -> build(world::setBlock, machine))
                .thenServer(SETTLE * 5, level -> ctx.assertTrue(ChainMachines.formAt(level, ctx.abs(terminal),
                        Direction.NORTH), "the Large Scale Printer forms"))
                .thenServer(SETTLE, level -> {
                    final LargeScalePrinterBlockEntity printer = printer(ctx, level, terminal);
                    printer.getInventory().insertItem(0, new ItemStack(Items.PAPER, 64), false);
                    printer.submit(BookJob.of(new BookFile("Mine Log", "Steve", pages("Day", 23)), "Mine Log.book",
                            64, "OFFICE-PC", "Dev"));
                    printer.submit(BookJob.enchanted(List.of(new ScanFile.Enchanted("", "minecraft:sharpness", 5)),
                            "Sharpness V", 8, "OFFICE-PC", "Dev"));
                    printer.submit(BookJob.of(new BookFile("", "", pages("Plan", 12)), "Base notes", 2, "HOME-PC",
                            "Dev"));
                })
                .thenTeleport(SETTLE, terminal.north(2), Direction.SOUTH)
                .thenRightClick(SETTLE, terminal)
                .thenAwaitScreen(LargeScalePrinterScreen.class, SCREEN_WAIT)
                .thenWaitUntilServer(level -> printer(ctx, level, terminal).done() > 0, WAIT,
                        "the printer to make a book of the first job", level -> "done="
                                + printer(ctx, level, terminal).done())
                .then(PRINTING, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(1, "lsp-printing")
                .then(0, () -> ComputersServerConfig.FILE.set(ComputersServerConfig.PRINT_ENCHANTED_BOOKS, false))
                .then(SETTLE * 10, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(1, "lsp-enchanted-off")
                .then(0, () -> ComputersServerConfig.FILE.set(ComputersServerConfig.PRINT_ENCHANTED_BOOKS, true))
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT)
                // The machine at work in the world, seen from before it and to the right, as its mock sees it.
                .thenTeleport(SETTLE, IN_FRONT, Direction.SOUTH)
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(CORNER.offset(2, 1, 1)))))
                .then(SETTLE * 5, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(1, "lsp-in-the-world");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void printWindow_offersTheLargeScalePrinterFirstForABook(final ClientTestContext ctx) {
        final ChainMachine machine = ChainMachines.byId("large_scale_printer").orElseThrow();
        final BlockPos terminal = terminalOf(machine);
        ctx.thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent())
                .thenBuild(0, world -> {
                    build(world::setBlock, machine);
                    for (final BlockPos ground : new BlockPos[] {COMPUTER.below(), MONITOR.below(), PRINTER.below(),
                            PLAYER_AT_MONITOR.below()}) {
                        world.setBlock(ground, Blocks.STONE);
                    }
                    world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_10"));
                    world.placeMonitor(MONITOR, Direction.EAST);
                    world.setBlock(PRINTER, ComputingModule.PRINTER.get());
                })
                .thenServer(SETTLE * 5, level -> {
                    ctx.assertTrue(ChainMachines.formAt(level, ctx.abs(terminal), Direction.NORTH),
                            "the Large Scale Printer forms");
                    printer(ctx, level, terminal).getInventory().insertItem(0, new ItemStack(Items.PAPER, 64), false);
                    ((PrinterBlockEntity) level.getBlockEntity(ctx.abs(PRINTER))).paper()
                            .setStackInSlot(0, new ItemStack(Items.PAPER, 16));
                    final IOsHost host = (IOsHost) level.getBlockEntity(ctx.abs(COMPUTER));
                    final ItemStack disk = host.systemDisk();
                    final FilesystemKind kind = FileAccess.filesystemKindOf(host);
                    DiskFilesystem.mkdirs(disk, DOCUMENTS, kind);
                    DiskFilesystem.write(disk, BOOK, FileType.BOOK, new BookFile("Mine Log", "Steve",
                            pages("Day", 23)).write(), Long.MAX_VALUE, kind);
                    host.setChanged();
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, SCREEN_WAIT * 3)
                .then(SETTLE * 4, () -> DesktopScreen.requestOpenFile(BOOK))
                .thenWaitUntil(() -> editor(ctx) != null, SCREEN_WAIT, "the book to open in the Editor")
                .then(SETTLE * 2, () -> ctx.key(GLFW.GLFW_KEY_P, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntil(() -> editor(ctx).printDialog().isOpen()
                                && editor(ctx).printDialog().printerNames().size() == 2, SCREEN_WAIT,
                        "the Print window to list the laser printer and the Large Scale Printer")
                .thenAssert(0, () -> editor(ctx).printDialog().printerNames().getFirst().startsWith(
                        GameText.resolve(LargeScalePrinters.NAME)), "for a book the Large Scale Printer comes first")
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE, "lsp-print-window")
                .then(SETTLE, () -> editor(ctx).printDialog().close());
    }

    /* Lays {@code machine}'s blocks from {@link #CORNER} facing north, its Machine Terminal at its place. */
    private static void build(final Placer world, final ChainMachine machine) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final String row = layers.get(y).get(z);
                for (int x = 0; x < row.length(); x++) {
                    final char letter = row.charAt(x);
                    final Block block = letter == '#' ? ChainMachineBlocks.TERMINAL.get()
                            : ChainMachineBlocks.byId(machine.legend().get(letter)).get();
                    final BlockState state = block.defaultBlockState();
                    world.place(CORNER.offset(x, y, z), state.hasProperty(HorizontalDirectionalBlock.FACING)
                            ? state.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH) : state);
                }
            }
        }
    }

    /* Where {@link #build} lays the Machine Terminal of {@code machine}. */
    private static BlockPos terminalOf(final ChainMachine machine) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final int x = layers.get(y).get(z).indexOf('#');
                if (x >= 0) {
                    return CORNER.offset(x, y, z);
                }
            }
        }
        throw new IllegalArgumentException(machine.id() + " has no terminal");
    }

    private static LargeScalePrinterBlockEntity printer(final ClientTestContext ctx, final ServerLevel level,
                                                        final BlockPos terminal) {
        return (LargeScalePrinterBlockEntity) level.getBlockEntity(ctx.abs(terminal));
    }

    @Nullable
    private static EditorApp editor(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final DesktopWindow window = desktop == null ? null : desktop.windowFor(EDITOR);
        return window != null && window.app() instanceof EditorApp app ? app : null;
    }

    private static List<String> pages(final String word, final int count) {
        final List<String> out = new ArrayList<>();
        for (int page = 1; page <= count; page++) {
            out.add(word + " " + page + ": iron and coal.");
        }
        return out;
    }

    /* Where the machine's blocks are set: the test's builder. */
    @FunctionalInterface
    private interface Placer {
        void place(BlockPos at, BlockState state);
    }
}
