/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.MediaDevices;
import dev.jstech.computers.block.ScannerBlock;
import dev.jstech.computers.blockentity.ScannerBlockEntity;
import dev.jstech.computers.client.ScannerScreen;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.lwjgl.glfw.GLFW;

/**
 * The scanners of every era at work (computers/docs/design/peripherals.md): each beside a running computer, an item on
 * its glass being read page by page, and its screen in the look of the mod's devices of its era, as the mocks show it.
 * The window is open before the scan starts, so it sees each page begin and its light bar runs down the page.
 */
public final class ScannerClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final BlockPos COMPUTER = new BlockPos(4, 2, 2);
    private static final BlockPos SCANNER = COMPUTER.south();
    /* The player in front of the scanner, which faces them. */
    private static final BlockPos PLAYER = SCANNER.south(2);

    private ScannerClientTests() {
    }

    /* 8 s a page: halfway down the first of 23. */
    @ClientTest(timeoutTicks = 900)
    public static void vintage_readsAWrittenBook(final ClientTestContext ctx) {
        scanning(ctx, MediaDevices.VINTAGE_SCANNER, level -> writtenBook("Mine Log", 23), 90, "scanner-vintage");
    }

    /* 4 s a page: most of the way down a map. */
    @ClientTest(timeoutTicks = 900)
    public static void legacy_readsAMap(final ClientTestContext ctx) {
        scanning(ctx, MediaDevices.LEGACY_SCANNER, level -> MapItem.create(level, 0, 0, (byte) 0, false, false), 56,
                "scanner-legacy");
    }

    /* 2 s a page: a third of the way down an enchanted book's one page. */
    @ClientTest(timeoutTicks = 900)
    public static void transition_readsAnEnchantedBook(final ClientTestContext ctx) {
        scanning(ctx, MediaDevices.TRANSITION_SCANNER, level -> EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.EFFICIENCY), 4)), 14, "scanner-transition");
    }

    /* 1 s a page: well into the second page. */
    @ClientTest(timeoutTicks = 900)
    public static void standard_readsAWrittenBook(final ClientTestContext ctx) {
        scanning(ctx, MediaDevices.SCANNER, level -> writtenBook("Field Notes", 12), 28, "scanner-standard");
    }

    /* Half a second a page: into the ninth of 12. */
    @ClientTest(timeoutTicks = 900)
    public static void advanced_readsABookAndQuill(final ClientTestContext ctx) {
        final List<Filterable<String>> pages = new ArrayList<>();
        for (int page = 1; page <= 12; page++) {
            pages.add(Filterable.passThrough("Page " + page + " of the plan."));
        }
        final ItemStack quill = new ItemStack(Items.WRITABLE_BOOK);
        quill.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(pages));
        scanning(ctx, MediaDevices.ADVANCED_SCANNER, level -> quill, 85, "scanner-advanced");
    }

    /*
     * A scanner of one era facing the player beside a running computer, {@code item} on its glass; its window opened,
     * the scan started, and the window shot {@code reading} ticks into the scan.
     */
    private static void scanning(final ClientTestContext ctx, final BlockEntry<ScannerBlock> block,
                                 final Function<ServerLevel, ItemStack> item, final int reading, final String label) {
        ctx.thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent())
                .thenBuild(0, world -> {
                    world.placeRunningPersonalComputer(COMPUTER);
                    world.setBlock(SCANNER, block.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    world.blockEntity(SCANNER, ScannerBlockEntity.class).glass().setStackInSlot(0,
                            item.apply(world.level()).copy());
                })
                .thenTeleport(SETTLE * 5, PLAYER, Direction.NORTH)
                .thenRightClick(SETTLE, SCANNER)
                .thenAwaitScreen(ScannerScreen.class, SCREEN_WAIT)
                .thenServer(SETTLE, level -> scanner(ctx, level).scan())
                .thenWaitUntilServer(level -> scanner(ctx, level).scanning(), SCREEN_WAIT,
                        "the scanner to read what is on its glass", level -> "problem="
                                + scanner(ctx, level).problem())
                .then(reading, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(1, label)
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    private static ScannerBlockEntity scanner(final ClientTestContext ctx, final ServerLevel level) {
        return (ScannerBlockEntity) level.getBlockEntity(ctx.abs(SCANNER));
    }

    /* A signed book of {@code pages} pages. */
    private static ItemStack writtenBook(final String title, final int pages) {
        final List<Filterable<Component>> content = new ArrayList<>();
        for (int page = 1; page <= pages; page++) {
            content.add(Filterable.passThrough(Component.literal("Day " + page + ": iron and coal.")));
        }
        final ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(title), "Dev",
                0, content, true));
        return book;
    }
}
