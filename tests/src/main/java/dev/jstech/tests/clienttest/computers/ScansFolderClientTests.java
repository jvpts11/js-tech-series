/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.FilesApp;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.scan.Scans;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * The files a scanner makes, in the File Explorer of Frames 10 (computers/docs/design/peripherals.md): the Scans folder
 * of the computer's documents with a book, a signed book, a map, an enchanted book and a page of text, each with the
 * icon and kind of its own, the signed book chosen, as the mock shows it.
 */
public final class ScansFolderClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = COMPUTER.east();
    private static final BlockPos PLAYER = MONITOR.east(2);
    private static final String SCANS = "Users/Public/Documents/Scans";

    private ScansFolderClientTests() {
    }

    @ClientTest(timeoutTicks = 1600)
    public static void explorer_listsWhatTheScannerMade(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_10"));
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenServer(SETTLE * 5, level -> {
                    final IOsHost host = (IOsHost) level.getBlockEntity(ctx.abs(COMPUTER));
                    final ItemStack disk = host.systemDisk();
                    final FilesystemKind kind = FileAccess.filesystemKindOf(host);
                    DiskFilesystem.mkdirs(disk, SCANS, kind);
                    scanned(level, disk, kind, "Base notes", quill(12));
                    scanned(level, disk, kind, "Mine Log", signed("Mine Log", 23));
                    scanned(level, disk, kind, "Map 4", MapItem.create(level, 0, 0, (byte) 0, false, false));
                    scanned(level, disk, kind, "Sharpness V", EnchantedBookItem.createForEnchantment(
                            new EnchantmentInstance(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                                    .getOrThrow(Enchantments.SHARPNESS), 5)));
                    DiskFilesystem.write(disk, SCANS + "/Craft list.txt", FileType.TXT,
                            "Craft list\n64 Iron Ingot\n16 Copper Wire\n4 Circuit Board\n", Long.MAX_VALUE, kind);
                    host.setChanged();
                })
                .thenTeleport(SETTLE, PLAYER, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, SCREEN_WAIT * 3)
                .then(SETTLE * 4, () -> DesktopScreen.requestOpenFiles(SCANS))
                .thenWaitUntil(() -> files(ctx) != null && files(ctx).names().size() == 5, SCREEN_WAIT,
                        "the explorer to list the five files the scanner made")
                .then(SETTLE, () -> ctx.clickDesktop(files(ctx).rowPoint("Mine Log.book")))
                .then(2, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE, "scans-folder");
    }

    /* Writes the file a scanner makes of {@code item} into the Scans folder, under {@code name}. */
    private static void scanned(final ServerLevel level, final ItemStack disk, final FilesystemKind kind,
                                final String name, final ItemStack item) {
        final Scans.Scan scan = Scans.read(level, item).orElseThrow();
        DiskFilesystem.write(disk, SCANS + "/" + name + "." + scan.type().extension(), scan.type(), scan.content(),
                Long.MAX_VALUE, kind);
    }

    /* A signed book of {@code pages} pages. */
    private static ItemStack signed(final String title, final int pages) {
        final List<Filterable<Component>> content = new ArrayList<>();
        for (int page = 1; page <= pages; page++) {
            content.add(Filterable.passThrough(Component.literal("Day " + page + ": iron and coal.")));
        }
        final ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(title), "Steve",
                0, content, true));
        return book;
    }

    /* A book and quill of {@code pages} pages. */
    private static ItemStack quill(final int pages) {
        final List<Filterable<String>> content = new ArrayList<>();
        for (int page = 1; page <= pages; page++) {
            content.add(Filterable.passThrough("Page " + page + " of the plan."));
        }
        final ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
        book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(content));
        return book;
    }

    private static FilesApp files(final ClientTestContext ctx) {
        return DesktopSteps.app(ctx, "jsc:files", FilesApp.class);
    }
}
