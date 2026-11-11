/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.printer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.os.fs.BookFile;
import dev.jstech.computers.os.fs.ScanFile;
import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.text.TextBounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * A book waiting in a Large Scale Printer (computers/docs/design/peripherals.md): what it prints, how many copies, and
 * where it came from. A signed {@code .book} prints as a written book, a copy of the original; an unsigned one as a book
 * and quill; a {@code .ench} as an enchanted book with every enchantment at the level it was scanned with; a system's
 * book from its help as that book.
 *
 * @param id           its number at this printer
 * @param kind         what kind of book it is
 * @param title        its title, as the queue shows it
 * @param author       who signed it, for a written book
 * @param pages        its pages, for a written book or a book and quill
 * @param enchantments its enchantments, for an enchanted book
 * @param manual       the book's item id, for a system's book
 * @param copies       how many copies
 * @param from         the name of the machine that sent it
 * @param user         who sent it
 */
public record BookJob(int id, Kind kind, String title, String author, List<String> pages,
                      List<ScanFile.Enchanted> enchantments, String manual, int copies, String from, String user) {

    /** The most copies of one book a job asks for. */
    public static final int MOST_COPIES = 64;
    private static final Codec<ScanFile.Enchanted> ENCHANTED = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(ScanFile.Enchanted::id),
            Codec.INT.fieldOf("level").forGetter(ScanFile.Enchanted::level)
    ).apply(i, (id, level) -> new ScanFile.Enchanted("", id, level)));
    public static final Codec<BookJob> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("id").forGetter(BookJob::id),
            StableCodecs.byName(Kind.class).fieldOf("kind").forGetter(BookJob::kind),
            Codec.STRING.optionalFieldOf("title", "").forGetter(BookJob::title),
            Codec.STRING.optionalFieldOf("author", "").forGetter(BookJob::author),
            Codec.STRING.listOf().optionalFieldOf("pages", List.of()).forGetter(BookJob::pages),
            ENCHANTED.listOf().optionalFieldOf("enchantments", List.of()).forGetter(BookJob::enchantments),
            Codec.STRING.optionalFieldOf("manual", "").forGetter(BookJob::manual),
            Codec.INT.optionalFieldOf("copies", 1).forGetter(BookJob::copies),
            Codec.STRING.optionalFieldOf("from", "").forGetter(BookJob::from),
            Codec.STRING.optionalFieldOf("user", "").forGetter(BookJob::user)
    ).apply(i, BookJob::new));

    public BookJob {
        title = TextBounds.clip(title, BookFile.MOST_TITLE);
        author = TextBounds.clip(author, BookFile.MOST_AUTHOR);
        // Bounded as a book bounds its pages, and copied, so the job keeps none of the list it was handed.
        pages = List.copyOf(new BookFile("", "", pages).pages());
        enchantments = List.copyOf(enchantments);
        copies = Math.max(1, Math.min(MOST_COPIES, copies));
        from = TextBounds.clip(from, PrintedDocument.MAX_NAME);
        user = TextBounds.clip(user, PrintedDocument.MAX_NAME);
    }

    /** A job for the book a {@code .book} file holds: a written book when it was signed, a book and quill when not. */
    public static BookJob of(final BookFile book, final String name, final int copies, final String from,
                             final String user) {
        return new BookJob(0, book.signed() ? Kind.WRITTEN : Kind.QUILL, book.signed() ? book.title() : name,
                book.author(), book.pages(), List.of(), "", copies, from, user);
    }

    /** A job for an enchanted book with {@code enchantments}, as a sealed {@code .ench} file names them. */
    public static BookJob enchanted(final List<ScanFile.Enchanted> enchantments, final String name, final int copies,
                                    final String from, final String user) {
        return new BookJob(0, Kind.ENCHANTED, name, "", List.of(), enchantments, "", copies, from, user);
    }

    /** A job for a system's book, by its item's id, as a computer's help prints one. */
    public static BookJob manual(final ResourceLocation item, final String title, final int copies, final String from,
                                 final String user) {
        return new BookJob(0, Kind.MANUAL, title, "", List.of(), List.of(), item.toString(), copies, from, user);
    }

    /** The same job, numbered {@code number} at its printer. */
    public BookJob numbered(final int number) {
        return new BookJob(number, kind, title, author, pages, enchantments, manual, copies, from, user);
    }

    /** One copy of the book, as it comes out; empty for a system's book the game no longer has. */
    public ItemStack book(final HolderLookup.Provider registries) {
        return switch (kind) {
            case WRITTEN -> written();
            case QUILL -> quill();
            case ENCHANTED -> enchantedBook(registries);
            case MANUAL -> manualBook();
        };
    }

    private ItemStack written() {
        final List<Filterable<Component>> lines = new ArrayList<>();
        for (final String page : pages) {
            lines.add(Filterable.passThrough(Component.literal(page)));
        }
        final ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        // A printed book is a copy of the original, as a copied book is.
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough(title), author, 1,
                lines, true));
        return book;
    }

    private ItemStack quill() {
        final List<Filterable<String>> lines = new ArrayList<>();
        for (final String page : pages) {
            lines.add(Filterable.passThrough(page));
        }
        final ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
        book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(lines));
        return book;
    }

    private ItemStack enchantedBook(final HolderLookup.Provider registries) {
        final ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        final HolderLookup.RegistryLookup<Enchantment> all = registries.lookupOrThrow(Registries.ENCHANTMENT);
        for (final ScanFile.Enchanted enchanted : enchantments) {
            final ResourceLocation id = ResourceLocation.tryParse(enchanted.id());
            final Optional<Holder.Reference<Enchantment>> found = id == null ? Optional.empty()
                    : all.get(ResourceKey.create(Registries.ENCHANTMENT, id));
            found.ifPresent(holder -> stored.set(holder, enchanted.level()));
        }
        final ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return book;
    }

    private ItemStack manualBook() {
        final ResourceLocation id = ResourceLocation.tryParse(manual);
        final Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** What kind of book a job prints. */
    public enum Kind implements IStableId, IStableName {
        WRITTEN(0, "written"),
        QUILL(1, "quill"),
        ENCHANTED(2, "enchanted"),
        MANUAL(3, "manual");

        private final int id;
        private final String serializedName;

        Kind(final int id, final String serializedName) {
            this.id = id;
            this.serializedName = serializedName;
        }

        @Override
        public int id() {
            return id;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }
}
