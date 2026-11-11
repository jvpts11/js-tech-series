/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.printer;

import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.BookFile;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.ScanFile;
import dev.jstech.computers.os.fs.ScanSeal;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The Large Scale Printers a computer prints books on (computers/docs/design/peripherals.md): those whose peripheral
 * port is linked to it by cable, listed in its Print dialog among its printers. A book goes to one as its file says:
 * a {@code .book} as the book it holds, a sealed {@code .ench} as its enchanted book; and a system's book from its
 * help. The printers themselves say they are loaded and gone; only the server's thread touches them.
 */
@TextHolder
public final class LargeScalePrinters {

    /** What a Print dialog's row calls a Large Scale Printer, where a printer's row names its model. */
    public static final String MODEL = "large_scale_printer";
    public static final TextKey NAME = TextKey.of("jsc.large_scale_printer.name", "Large Scale Printer");
    public static final TextKey BOOKS = TextKey.of("jsc.large_scale_printer.prints", "books");
    public static final TextKey PORT = TextKey.of("jsc.large_scale_printer.port", "peripheral cable");
    public static final TextKey NOT_A_BOOK = TextKey.of("jsc.large_scale_printer.not_a_book",
            "The Large Scale Printer prints books: open a .book or a .ench file to print it.");
    public static final TextKey NOT_PRINTABLE = TextKey.of("jsc.large_scale_printer.not_printable",
            "Only the books of the systems and the languages are printed from the help.");
    private static final Set<LargeScalePrinterBlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final TextKey READY = TextKey.of("jsc.large_scale_printer.status.ready", "Ready");
    private static final TextKey PRINTING = TextKey.of("jsc.large_scale_printer.status.printing", "Printing");
    private static final TextKey PAUSED = TextKey.of("jsc.large_scale_printer.status.paused", "Paused");
    private static final TextKey NO_PAPER = TextKey.of("jsc.large_scale_printer.status.no_paper", "Out of paper");
    private static final TextKey APART = TextKey.of("jsc.large_scale_printer.status.apart", "Not formed");
    private static final TextKey WAITING = TextKey.of("jsc.large_scale_printer.status.waiting", "%s (%s waiting)");
    private static final TextKey SENT = TextKey.of("jsc.large_scale_printer.sent",
            "\"%s\" was sent to the Large Scale Printer.");
    private static final TextKey QUEUE_FULL = TextKey.of("jsc.large_scale_printer.queue_full",
            "The Large Scale Printer's queue is full.");
    private static final TextKey ENCHANTED_OFF = TextKey.of("jsc.large_scale_printer.refused_enchanted",
            "This server does not let enchanted books be printed.");
    private static final TextKey NOT_SEALED = TextKey.of("jsc.large_scale_printer.not_sealed",
            "%s is not an enchanted book a scanner of this world read.");
    private static final TextKey NO_PRINTER = TextKey.of("jsc.large_scale_printer.no_printer",
            "No Large Scale Printer is linked to this computer.");

    private LargeScalePrinters() {
    }

    /** A printer came into the world; it is listed while it stays. */
    public static void loaded(final LargeScalePrinterBlockEntity printer) {
        LOADED.add(printer);
    }

    /** A printer left the world. */
    public static void gone(final LargeScalePrinterBlockEntity printer) {
        LOADED.remove(printer);
    }

    /** The formed printers whose peripheral port is linked to {@code host}, by position. */
    public static List<LargeScalePrinterBlockEntity> of(final ServerLevel level, final IOsHost host) {
        if (!(host instanceof BlockEntity machine)) {
            return List.of();
        }
        final long at = machine.getBlockPos().asLong();
        final List<LargeScalePrinterBlockEntity> out = new ArrayList<>();
        for (final LargeScalePrinterBlockEntity printer : List.copyOf(LOADED)) {
            if (printer.isRemoved() || printer.getLevel() != level || !printer.formed()) {
                continue;
            }
            final BlockPos port = printer.port(level);
            final OptionalLong owner = port == null ? OptionalLong.empty()
                    : PeripheralLinks.discoverOwner(level, port.asLong());
            if (owner.isPresent() && owner.getAsLong() == at) {
                out.add(printer);
            }
        }
        out.sort(Comparator.comparingLong(printer -> printer.getBlockPos().asLong()));
        return out;
    }

    /** The printer of {@code host} at {@code pos}, or null. */
    @Nullable
    public static LargeScalePrinterBlockEntity at(final ServerLevel level, final IOsHost host, final long pos) {
        for (final LargeScalePrinterBlockEntity printer : of(level, host)) {
            if (printer.getBlockPos().asLong() == pos) {
                return printer;
            }
        }
        return null;
    }

    /** What a printer is doing, as a dialog's Status line says it. */
    public static Text status(final LargeScalePrinterBlockEntity printer) {
        final Text state;
        if (!printer.formed()) {
            state = APART.text();
        } else if (printer.paused()) {
            state = PAUSED.text();
        } else if (printer.status() == MachineStatus.WORKING) {
            state = PRINTING.text();
        } else if (!printer.jobs().isEmpty() && printer.paperCount() < LargeScalePrinterBlockEntity.SHEETS_A_BOOK) {
            state = NO_PAPER.text();
        } else {
            state = READY.text();
        }
        final int queued = printer.jobs().size();
        return queued == 0 ? state : WAITING.with(state, queued);
    }

    /**
     * Sends the file {@code name}, holding {@code content}, to {@code printer} as the book it is; what became of it.
     * A file of another kind, an enchanted book the world did not seal or one the server will not print, and a full
     * queue are each refused, saying why.
     */
    public static Answer print(@Nullable final LargeScalePrinterBlockEntity printer, final String name,
                               final String content, final int copies, final String from, final String user) {
        if (printer == null) {
            return Answer.no(NO_PRINTER.text());
        }
        final FileType type = FileType.ofPath(name);
        final BookJob job;
        if (type == FileType.BOOK) {
            job = BookJob.of(BookFile.read(content), stem(name), copies, from, user);
        } else if (type == FileType.ENCH) {
            final Optional<String> body = ScanSeal.key().map(key -> ScanFile.open(content, key));
            if (body.isEmpty()) {
                return Answer.no(NOT_SEALED.with(name));
            }
            job = BookJob.enchanted(ScanFile.enchantmentsOf(body.get()), stem(name), copies, from, user);
        } else {
            return Answer.no(NOT_A_BOOK.text());
        }
        return submit(printer, job);
    }

    /** Sends the system's book {@code item} to {@code printer}, as a computer's help prints one. */
    public static Answer printManual(@Nullable final LargeScalePrinterBlockEntity printer,
                                     final ResourceLocation item, final String title, final int copies,
                                     final String from, final String user) {
        if (printer == null) {
            return Answer.no(NO_PRINTER.text());
        }
        return submit(printer, BookJob.manual(item, title, copies, from, user));
    }

    private static Answer submit(final LargeScalePrinterBlockEntity printer, final BookJob job) {
        if (LargeScalePrinterBlockEntity.refused(job)) {
            return Answer.no(ENCHANTED_OFF.text());
        }
        return printer.submit(job).isPresent() ? new Answer(true, SENT.with(job.title()))
                : Answer.no(QUEUE_FULL.text());
    }

    /* A file's name without its extension, which a book printed from it is called by. */
    private static String stem(final String name) {
        final int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /**
     * What became of a book sent to print.
     *
     * @param ok      whether the printer took it
     * @param message what to tell whoever printed it
     */
    public record Answer(boolean ok, Text message) {

        static Answer no(final Text why) {
            return new Answer(false, why);
        }
    }
}
