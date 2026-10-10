/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.printer;

import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The printers a computer prints on: those linked to it and not disabled on it, in the order of their positions, so
 * the first is always the same one and a request id names the same printer from one command to the next. What a
 * printer's status and port read as in the systems' dialogs and in {@code lpstat} is worked out here once.
 */
@TextHolder
public final class Printers {

    private static final TextKey READY = TextKey.of("jsc.printer.state.ready", "Ready");
    private static final TextKey PRINTING = TextKey.of("jsc.printer.state.printing", "Printing");
    private static final TextKey PAUSED = TextKey.of("jsc.printer.state.paused", "Paused");
    private static final TextKey NO_PAPER = TextKey.of("jsc.printer.state.no_paper", "Out of paper");
    private static final TextKey OUTPUT_FULL = TextKey.of("jsc.printer.state.output_full",
            "Output tray full");
    private static final TextKey WAITING = TextKey.of("jsc.printer.state.waiting", "%s (%s waiting)");
    private static final TextKey SENT = TextKey.of("jsc.printer.sent", "\"%s\" was sent to %s.");
    /** What a computer with no printer answers, in its Print window and to whatever tries to print on it. */
    public static final TextKey NO_PRINTER = TextKey.of("jsc.printer.no_printer",
            "No printer is connected to this computer.");
    private static final TextKey QUEUE_FULL = TextKey.of("jsc.printer.queue_full",
            "%s cannot take more documents: its queue is full.");
    public static final TextKey NOTHING = TextKey.of("jsc.printer.nothing_to_print", "There is nothing to print.");
    private static final String PARALLEL = "LPT1";
    private static final String USB = "USB";

    private Printers() {
    }

    /**
     * The pages of a text as a document, cut to the range asked for.
     *
     * @param fromPage the first page, from 1; 0 or less from the first
     * @param toPage   the last page, from 1; 0 or less to the end
     */
    public static PrintedDocument text(final String title, final String machine, final String program,
                                       final String text, final boolean landscape, final int fromPage,
                                       final int toPage) {
        final List<String> pages = PrintLayout.pages(title, text, landscape);
        final int first = Math.max(1, fromPage) - 1;
        final int last = toPage <= 0 ? pages.size() : Math.min(pages.size(), toPage);
        final List<String> kept = first < last ? pages.subList(first, last) : pages.subList(0, 1);
        return PrintedDocument.text(title, machine, program, kept);
    }

    /**
     * Puts {@code document} in the queue of {@code printer}, or says why it could not: no printer, a full queue,
     * nothing to print. The document's sheet takes the printer's look when it comes out.
     *
     * @param user who sent it, as {@code lpstat} names a request's owner
     */
    public static Result print(final ServerLevel level, @Nullable final PrinterBlockEntity printer,
                               final PrintedDocument document, final int copies, final String user) {
        if (printer == null) {
            return new Result(null, null, NO_PRINTER.text());
        }
        if (!document.isPicture() && document.bytes() == 0 && document.title().isEmpty()) {
            return new Result(printer, null, NOTHING.text());
        }
        final PrinterBlockEntity.PrintJob job = printer.submit(document, copies, user, level.getGameTime());
        if (job == null) {
            return new Result(printer, null, QUEUE_FULL.with(printer.model().displayName()));
        }
        return new Result(printer, job, SENT.with(document.title(), printer.model().displayName()));
    }

    /** The printers {@code host} prints on, by position. */
    public static List<PrinterBlockEntity> of(final ServerLevel level, final IOsHost host) {
        final List<Long> linked = new ArrayList<>(host.enabledEndpoints());
        Collections.sort(linked);
        final List<PrinterBlockEntity> out = new ArrayList<>();
        for (final long pos : linked) {
            if (Loaded.blockEntity(level, BlockPos.of(pos)) instanceof PrinterBlockEntity printer) {
                out.add(printer);
            }
        }
        return out;
    }

    /** The printer of {@code host} at {@code pos}, the first one when {@code pos} is -1, or null. */
    @Nullable
    public static PrinterBlockEntity at(final ServerLevel level, final IOsHost host, final long pos) {
        for (final PrinterBlockEntity printer : of(level, host)) {
            if (pos == -1L || printer.getBlockPos().asLong() == pos) {
                return printer;
            }
        }
        return null;
    }

    /** The printer of {@code host} whose queue goes by {@code name} in {@code lp -d}, or null. */
    @Nullable
    public static PrinterBlockEntity named(final ServerLevel level, final IOsHost host, final String name) {
        for (final PrinterBlockEntity printer : of(level, host)) {
            if (printer.model().queueName().equalsIgnoreCase(name)) {
                return printer;
            }
        }
        return null;
    }

    /** The name a machine's documents carry as where they came from: its host name, as its shell gives it. */
    public static String machineName(final IOsHost host) {
        return host instanceof IComputerTerminalHost terminal ? terminal.hostname() : host.customName();
    }

    /** A job's request id, as {@code lp} answers it and {@code lpstat} lists it: the queue's name and its number. */
    public static String requestId(final PrinterBlockEntity printer, final PrinterBlockEntity.PrintJob job) {
        return printer.model().queueName() + "-" + job.id();
    }

    /** What a printer is doing, as a dialog's Status line says it. */
    public static Text status(final PrinterBlockEntity printer) {
        final Text state = switch (printer.waiting()) {
            case PAUSED -> PAUSED.text();
            case NO_PAPER -> NO_PAPER.text();
            case OUTPUT_FULL -> OUTPUT_FULL.text();
            case NONE -> printer.printing() ? PRINTING.text() : READY.text();
        };
        final int queued = printer.jobs().size();
        return queued == 0 ? state : WAITING.with(state, queued);
    }

    /** The port a printer is on, as a dialog's Location line says it: the parallel port of old, or USB. */
    public static String port(final PrinterBlockEntity printer) {
        return printer.model().era() == HardwareEra.VINTAGE ? PARALLEL : USB;
    }

    /**
     * What became of a document sent to print.
     *
     * @param printer the printer it was sent to, or null when there was none
     * @param job     the job it became, or null when the printer did not take it
     * @param message what to tell whoever printed it
     */
    public record Result(@Nullable PrinterBlockEntity printer, @Nullable PrinterBlockEntity.PrintJob job,
                         Text message) {

        /** Whether a printer took it. */
        public boolean ok() {
            return job != null;
        }
    }
}
