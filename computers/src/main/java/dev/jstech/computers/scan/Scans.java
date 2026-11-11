/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.scan;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.os.fs.BookFile;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.PixImage;
import dev.jstech.computers.os.fs.ScanFile;
import dev.jstech.computers.os.fs.ScanSeal;
import dev.jstech.computers.printer.PrintLayout;
import dev.jstech.computers.printer.PrintedDocument;
import dev.jstech.computers.registry.ComputingComponents;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

/**
 * What a scanner makes of what lies on its glass (computers/docs/design/peripherals.md): a written book or a book and
 * quill becomes a {@code .book}, an enchanted book a {@code .ench}, a map a {@code .map}, and a Printed Paper goes back
 * to the {@code .txt} or the {@code .pix} it was printed from. The item is only read; nothing of it is used up.
 */
public final class Scans {

    /** How many pixels a map is on a side, as the game draws one. */
    private static final int MAP_SIDE = 128;
    /* A map's colour byte: the colour's id above its two bits of shade. */
    private static final int SHADE_BITS = 2;
    private static final int SHADE_MASK = 3;
    private static final int FULL = 255;
    private static final String PAGE_BREAK = "\n\n";

    private Scans() {
    }

    /** Whether a scanner reads {@code stack}: the five kinds of item it turns into files. */
    public static boolean reads(final ItemStack stack) {
        return stack.is(Items.WRITTEN_BOOK) || stack.is(Items.WRITABLE_BOOK) || stack.is(Items.ENCHANTED_BOOK)
                || stack.is(Items.FILLED_MAP) || stack.is(ComputingModule.PRINTED_PAPER.get());
    }

    /** How many pages scanning {@code stack} reads: a book's pages, a printed document's sheets, one for the rest. */
    public static int pages(final ItemStack stack) {
        final WrittenBookContent written = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (stack.is(Items.WRITTEN_BOOK) && written != null) {
            return Math.max(1, Math.min(BookFile.MOST_PAGES, written.pages().size()));
        }
        final WritableBookContent writable = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
        if (stack.is(Items.WRITABLE_BOOK) && writable != null) {
            return Math.max(1, Math.min(BookFile.MOST_PAGES, writable.pages().size()));
        }
        final PrintedDocument document = stack.get(ComputingComponents.PRINTED_DOCUMENT.get());
        if (stack.is(ComputingModule.PRINTED_PAPER.get()) && document != null) {
            return document.sheets();
        }
        return 1;
    }

    /** The kind of file scanning {@code stack} makes, or null when a scanner does not read it. */
    @Nullable
    public static FileType makes(final ItemStack stack) {
        if (stack.is(Items.WRITTEN_BOOK) || stack.is(Items.WRITABLE_BOOK)) {
            return FileType.BOOK;
        }
        if (stack.is(Items.ENCHANTED_BOOK)) {
            return FileType.ENCH;
        }
        if (stack.is(Items.FILLED_MAP)) {
            return FileType.MAP;
        }
        if (stack.is(ComputingModule.PRINTED_PAPER.get())) {
            final PrintedDocument document = stack.get(ComputingComponents.PRINTED_DOCUMENT.get());
            return document != null && document.isPicture() ? FileType.PIX : FileType.TXT;
        }
        return null;
    }

    /**
     * The file scanning {@code stack} makes, or empty when the scanner does not read it, or when an enchanted book or a
     * map cannot be sealed because no world is running.
     */
    public static Optional<Scan> read(final Level level, final ItemStack stack) {
        final FileType type = makes(stack);
        if (type == null) {
            return Optional.empty();
        }
        return switch (type) {
            case BOOK -> Optional.of(book(stack));
            case ENCH -> enchanted(stack);
            case MAP -> map(level, stack);
            default -> Optional.of(printed(stack, type));
        };
    }

    private static Scan book(final ItemStack stack) {
        final WrittenBookContent written = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (stack.is(Items.WRITTEN_BOOK) && written != null) {
            final List<String> pages = new ArrayList<>();
            for (final Filterable<Component> page : written.pages()) {
                pages.add(page.raw().getString());
            }
            final String title = written.title().raw();
            final BookFile book = new BookFile(title, written.author(), pages);
            return new Scan(FileType.BOOK, title, book.write(), book.pages().size());
        }
        final WritableBookContent writable = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
        final List<String> pages = new ArrayList<>();
        if (writable != null) {
            for (final Filterable<String> page : writable.pages()) {
                pages.add(page.raw());
            }
        }
        final BookFile book = new BookFile("", "", pages);
        return new Scan(FileType.BOOK, stack.getHoverName().getString(), book.write(), book.pages().size());
    }

    private static Optional<Scan> enchanted(final ItemStack stack) {
        final ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        final List<ScanFile.Enchanted> enchantments = new ArrayList<>();
        for (final Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
            final Holder<Enchantment> enchantment = entry.getKey();
            enchantment.unwrapKey().ifPresent(key -> enchantments.add(new ScanFile.Enchanted(
                    Enchantment.getFullname(enchantment, entry.getIntValue()).getString(), key.location().toString(),
                    entry.getIntValue())));
        }
        final String name = enchantments.isEmpty() ? stack.getHoverName().getString() : enchantments.getFirst().words();
        return ScanSeal.key().map(key -> new Scan(FileType.ENCH, name,
                ScanFile.seal(ScanFile.enchantments(enchantments), key), 1));
    }

    private static Optional<Scan> map(final Level level, final ItemStack stack) {
        final MapId id = stack.get(DataComponents.MAP_ID);
        if (id == null) {
            return Optional.empty();
        }
        final PixImage picture = new PixImage(MAP_SIDE, MAP_SIDE);
        final MapItemSavedData data = MapItem.getSavedData(id, level);
        if (data != null) {
            for (int y = 0; y < MAP_SIDE; y++) {
                for (int x = 0; x < MAP_SIDE; x++) {
                    picture.set(x, y, colourOf(data.colors[x + y * MAP_SIDE] & 0xFF));
                }
            }
        }
        final String name = stack.getHoverName().getString() + " " + id.id();
        return ScanSeal.key().map(key -> new Scan(FileType.MAP, name,
                ScanFile.seal(ScanFile.map(id.id(), picture.encode()), key), 1));
    }

    private static Scan printed(final ItemStack stack, final FileType type) {
        final PrintedDocument document = stack.getOrDefault(ComputingComponents.PRINTED_DOCUMENT.get(),
                PrintedDocument.EMPTY);
        if (type == FileType.PIX) {
            final String name = document.pictureName().isEmpty() ? document.title() : document.pictureName();
            return new Scan(FileType.PIX, name, document.picture(), 1);
        }
        // The words of the sheet, not the marks that lay a line on a grey band.
        return new Scan(FileType.TXT, document.title(), PrintLayout.plain(String.join(PAGE_BREAK, document.pages())),
                document.sheets());
    }

    /* A map's colour byte as the palette's nearest colour; the colour of nothing stays clear. */
    private static int colourOf(final int packed) {
        final MapColor colour = MapColor.byId(packed >> SHADE_BITS);
        if (colour == MapColor.NONE) {
            return 0;
        }
        final int shade = MapColor.Brightness.byId(packed & SHADE_MASK).modifier;
        final int red = (colour.col >> 16 & FULL) * shade / FULL;
        final int green = (colour.col >> 8 & FULL) * shade / FULL;
        final int blue = (colour.col & FULL) * shade / FULL;
        return PixImage.nearest(red << 16 | green << 8 | blue);
    }

    /**
     * A scan's file, before it has a place.
     *
     * @param type    its kind
     * @param name    the name it is saved under, without its extension
     * @param content what it holds
     * @param pages   how many pages were read to make it
     */
    public record Scan(FileType type, String name, String content, int pages) {
    }
}
