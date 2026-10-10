/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Printing from a prompt, each family its own way: MC-DOS's {@code PRINT}, which puts a resident part in memory the
 * first time it runs and prints in the background, listing what is printing and what waits; and the Unix family's
 * {@code lp}, which answers with a request id, and {@code lpstat}, which lists the requests and the printers.
 */
final class PrintCommands {

    /** The words for files on a system that keeps files, in each family. */
    private static final CommandScope DOS_FILES =
            CommandScope.on(CommandScope.DOS_SYSTEMS).needing(CommandScope.Need.FILES);
    private static final CommandScope POSIX_FILES =
            CommandScope.on(CommandScope.UNIX_SYSTEMS).needing(CommandScope.Need.FILES);

    private PrintCommands() {
    }

    /** The DOS family's PRINT. */
    static List<ICliCommand> dos() {
        return List.of(new Print());
    }

    /** The Unix family's lp and lpstat. */
    static List<ICliCommand> posix() {
        return List.of(new Lp(), new Lpstat());
    }

    /**
     * MC-DOS's PRINT: prints files in the background, installing its resident part the first time, and lists the
     * queue of the machine's printer, with no file named or after the ones it sent.
     */
    @TextHolder
    static final class Print implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.dos.print.summary",
                "print text files in the background");
        private static final TextKey USAGE = TextKey.of("jsc.cli.dos.print.usage", "[file...]");
        private static final TextKey RESIDENT = TextKey.of("jsc.cli.dos.print.resident",
                "Resident part of PRINT installed");
        private static final TextKey PRINTING = TextKey.of("jsc.cli.dos.print.printing",
                "   %s is currently being printed");
        private static final TextKey QUEUED = TextKey.of("jsc.cli.dos.print.queued", "   %s is in queue");
        private static final TextKey EMPTY = TextKey.of("jsc.cli.dos.print.empty", "PRINT queue is empty");
        private static final TextKey OFF_LINE = TextKey.of("jsc.cli.dos.print.off_line",
                "Errors on list device indicate that it may be off-line. Please check it.");

        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "print"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            final List<ICliComputer.PrinterInfo> printers = ctx.computer().printers();
            if (printers.isEmpty()) {
                ctx.out().error(OFF_LINE.text());
                return;
            }
            if (ctx.hasArgs() && ctx.computer().installPrint()) {
                ctx.out().line(RESIDENT.text());
            }
            for (final String file : ctx.args()) {
                final ICliComputer.PrintAnswer answer = ctx.computer().printFile(file, "", 1);
                if (!answer.ok()) {
                    ctx.out().error(answer.message());
                }
            }
            final List<ICliComputer.PrintJobInfo> jobs = ctx.computer().printers().getFirst().jobs();
            if (jobs.isEmpty()) {
                ctx.out().line(EMPTY.text());
                return;
            }
            for (int i = 0; i < jobs.size(); i++) {
                final String title = jobs.get(i).title().toUpperCase(Locale.ROOT);
                ctx.out().line(i == 0 ? PRINTING.with(title) : QUEUED.with(title));
            }
        }
    }

    /** The Unix family's lp: sends files to a printer, the default one or the one {@code -d} names. */
    @TextHolder
    static final class Lp implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.posix.lp.summary", "send files to a printer");
        private static final TextKey USAGE = TextKey.of("jsc.cli.posix.lp.usage",
                "[-d destination] [-n copies] file...");
        private static final TextKey REQUEST = TextKey.of("jsc.cli.posix.lp.request", "request id is %s (1 file)");
        private static final TextKey NO_DEFAULT = TextKey.of("jsc.cli.posix.lp.no_default",
                "lp: no default destination");
        private static final TextKey NO_SUCH = TextKey.of("jsc.cli.posix.lp.no_such",
                "lp: destination \"%s\" non-existent");
        private static final TextKey NO_FILES = TextKey.of("jsc.cli.posix.lp.no_files",
                "lp: no files to print");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "lp"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            String destination = "";
            int copies = 1;
            final List<String> files = new ArrayList<>();
            final List<String> args = ctx.args();
            for (int i = 0; i < args.size(); i++) {
                final String arg = args.get(i);
                if (arg.equals("-d") && i + 1 < args.size()) {
                    destination = args.get(++i);
                } else if (arg.startsWith("-d") && arg.length() > 2) {
                    destination = arg.substring(2);
                } else if (arg.equals("-n") && i + 1 < args.size()) {
                    copies = copies(args.get(++i));
                } else if (arg.startsWith("-n") && arg.length() > 2) {
                    copies = copies(arg.substring(2));
                } else {
                    files.add(arg);
                }
            }
            final List<ICliComputer.PrinterInfo> printers = ctx.computer().printers();
            if (printers.isEmpty()) {
                ctx.out().error(NO_DEFAULT.text());
                return;
            }
            if (!destination.isEmpty() && !hasQueue(printers, destination)) {
                ctx.out().error(NO_SUCH.with(destination));
                return;
            }
            if (files.isEmpty()) {
                ctx.out().error(NO_FILES.text());
                return;
            }
            for (final String file : files) {
                final ICliComputer.PrintAnswer answer = ctx.computer().printFile(file, destination, copies);
                if (answer.ok()) {
                    ctx.out().line(REQUEST.with(answer.requestId()));
                } else {
                    ctx.out().error(CliTexts.SAID_BY.with(name(), answer.message()));
                }
            }
        }

        private static boolean hasQueue(final List<ICliComputer.PrinterInfo> printers, final String name) {
            for (final ICliComputer.PrinterInfo printer : printers) {
                if (printer.queue().equalsIgnoreCase(name)) {
                    return true;
                }
            }
            return false;
        }

        private static int copies(final String value) {
            try {
                return Math.max(1, Math.min(99, Integer.parseInt(value)));
            } catch (final NumberFormatException e) {
                return 1;
            }
        }
    }

    /** The Unix family's lpstat: the requests waiting, and with {@code -p} the printers and what they do. */
    @TextHolder
    static final class Lpstat implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.posix.lpstat.summary",
                "show the print requests and the printers");
        private static final TextKey USAGE = TextKey.of("jsc.cli.posix.lpstat.usage", "[-p]");
        private static final TextKey NOW_PRINTING = TextKey.of("jsc.cli.posix.lpstat.now_printing",
                "printer %s now printing %s.");
        private static final TextKey IDLE = TextKey.of("jsc.cli.posix.lpstat.idle", "printer %s is idle.");
        private static final TextKey NO_PRINTERS = TextKey.of("jsc.cli.posix.lpstat.no_printers",
                "lpstat: no system default destination");
        /* A request: its id, owner and size in columns, then the day and hour it was sent, and the printer it is on. */
        private static final TextKey ROW = TextKey.of("jsc.cli.posix.lpstat.row", "%s   Day %s %s");
        private static final TextKey ROW_ON = TextKey.of("jsc.cli.posix.lpstat.row_on", "%s   Day %s %s  on %s");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "lpstat"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            final List<ICliComputer.PrinterInfo> printers = ctx.computer().printers();
            if (printers.isEmpty()) {
                ctx.out().error(NO_PRINTERS.text());
                return;
            }
            final boolean listPrinters = ctx.args().contains("-p");
            for (final ICliComputer.PrinterInfo printer : printers) {
                if (listPrinters) {
                    ctx.out().line(printer.printing() && !printer.jobs().isEmpty()
                            ? NOW_PRINTING.with(printer.queue(), printer.jobs().getFirst().id())
                            : IDLE.with(printer.queue()));
                    continue;
                }
                for (int i = 0; i < printer.jobs().size(); i++) {
                    final ICliComputer.PrintJobInfo job = printer.jobs().get(i);
                    final String columns = String.format(Locale.ROOT, "%-12s %-8s %6d", job.id(), job.user(),
                            job.bytes());
                    final long day = Stamps.day(job.submitted());
                    final String clock = Stamps.clock(job.submitted());
                    ctx.out().line(i == 0 && printer.printing() ? ROW_ON.with(columns, day, clock, printer.queue())
                            : ROW.with(columns, day, clock));
                }
            }
        }
    }
}
