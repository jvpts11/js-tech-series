/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The memory and its swap at the prompt (computers/docs/design/hardware.md): {@code free}, {@code swapon} and
 * {@code top} on Linux, {@code swapinfo} and {@code sysctl} on FreeBSD, each printing what its real one does from the
 * machine's own memory: what is installed, what is promised past it and so in swap, and how much of each program is
 * there. The numbers are data, written alike in every language; the words around them are each player's.
 */
public final class SwapCommands {

    private SwapCommands() {
    }

    /** The commands, for the families they belong to. */
    static List<ICliCommand> all() {
        return List.of(new Free(), new Swapon(), new Top(), new Swapinfo(), new Sysctl());
    }

    /* A size in megabytes as the tools print it read by a person: 512M, 1.2G, 16G. */
    static String human(final long mb) {
        if (mb < 1_024L) {
            return mb + "M";
        }
        final double gb = mb / 1_024.0;
        return gb >= 10.0 || gb == Math.rint(gb) ? Math.round(gb) + "G" : String.format(Locale.ROOT, "%.1fG", gb);
    }

    @TextHolder
    static final class Free implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.tool.free.summary",
                "how much memory and swap there is, used and free");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.tool.free.about", "Prints the memory this computer"
                + " has and the swap behind it: what is used, what is free and what a new program may still be"
                + " given. With -h the sizes read in megabytes and gigabytes; without, in kibibytes.");
        private static final TextKey HEADER = TextKey.of("jsc.cli.tool.free.header",
                "               total        used        free      shared  buff/cache   available");
        private static final TextKey MEM = TextKey.of("jsc.cli.tool.free.mem", "Mem:%s");
        private static final TextKey SWAP = TextKey.of("jsc.cli.tool.free.swap", "Swap:%s");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.LINUX);
        }

        @Override public String name() {
            return "free";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            final boolean human = ctx.hasArgs() && ctx.arg(0).startsWith("-h");
            final ICliComputer.MemoryUse memory = ctx.computer().memory();
            final ICliComputer.SwapUse swap = ctx.computer().swap();
            final long used = Math.min(memory.totalMb(), memory.usedMb());
            final long free = memory.totalMb() - used;
            ctx.out().line(HEADER);
            ctx.out().line(MEM.with(String.format(Locale.ROOT, "%14s%12s%12s%12s%12s%12s",
                    size(memory.totalMb(), human), size(used, human), size(free, human), size(0, human),
                    size(0, human), size(free + swap.sizeMb() - swap.usedMb(), human))));
            ctx.out().line(SWAP.with(String.format(Locale.ROOT, "%13s%12s%12s", size(swap.sizeMb(), human),
                    size(swap.usedMb(), human), size(swap.sizeMb() - swap.usedMb(), human))));
        }

        /* A size as free writes it: kibibytes, or with -h a person's Mi and Gi. */
        private static String size(final long mb, final boolean human) {
            if (!human) {
                return Long.toString(mb * 1_024L);
            }
            return mb < 1_024L ? mb + "Mi" : human(mb).replace("G", "Gi");
        }
    }

    @TextHolder
    static final class Swapon implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.tool.swapon.summary",
                "which partitions the swap is on, and how much of each is used");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.tool.swapon.about", "With --show, lists the swap"
                + " partitions this computer uses, each with its size, how much of it is in use and its priority. The"
                + " installer made them; a partition is made swap with a disk tool.");
        private static final TextKey HEADER = TextKey.of("jsc.cli.tool.swapon.header",
                "NAME      TYPE      SIZE USED PRIO");
        private static final TextKey ROW = TextKey.of("jsc.cli.tool.swapon.row", "%s partition %s");
        private static final TextKey USAGE = TextKey.of("jsc.cli.tool.swapon.usage", "usage: swapon --show");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.LINUX);
        }

        @Override public String name() {
            return "swapon";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs() || !("--show".equals(ctx.arg(0)) || "-s".equals(ctx.arg(0)))) {
                ctx.out().line(USAGE);
                return;
            }
            final ICliComputer.SwapUse swap = ctx.computer().swap();
            if (swap.areas().isEmpty()) {
                return;
            }
            ctx.out().line(HEADER);
            int priority = -2;
            for (final ICliComputer.SwapArea area : swap.areas()) {
                ctx.out().line(ROW.with(String.format(Locale.ROOT, "%-9s", area.device()), String.format(Locale.ROOT,
                        " %4s %4s %4d", human(area.sizeMb()), human(area.usedMb()), priority--)));
            }
        }
    }

    @TextHolder
    static final class Top implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.tool.top.summary",
                "what is running, with its memory and its swap");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.tool.top.about", "Prints what is running once:"
                + " the memory and the swap, then every program with the share of the processor it takes, what it"
                + " holds and how much of it is in swap. With -o SWAP the programs most in swap come first.");
        private static final TextKey MEM_LINE = TextKey.of("jsc.cli.tool.top.mem",
                "MiB Mem : %s total, %s free, %s used, %s buff/cache");
        private static final TextKey SWAP_LINE = TextKey.of("jsc.cli.tool.top.swap",
                "MiB Swap: %s total, %s free, %s used. %s avail Mem");
        private static final TextKey HEADER = TextKey.of("jsc.cli.tool.top.header",
                "    PID USER      %CPU  %MEM     RES   SWAP COMMAND");
        private static final TextKey ROW = TextKey.of("jsc.cli.tool.top.row", "%s player    %s");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.LINUX);
        }

        @Override public String name() {
            return "top";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer.MemoryUse memory = ctx.computer().memory();
            final ICliComputer.SwapUse swap = ctx.computer().swap();
            final long used = Math.min(memory.totalMb(), memory.usedMb());
            final long free = memory.totalMb() - used;
            ctx.out().line(MEM_LINE.with(mib(memory.totalMb()), mib(free), mib(used), mib(0)));
            ctx.out().line(SWAP_LINE.with(mib(swap.sizeMb()), mib(swap.sizeMb() - swap.usedMb()), mib(swap.usedMb()),
                    mib(free)));
            ctx.out().line("");
            ctx.out().line(HEADER);
            final List<ICliComputer.SwapProgram> programs = new ArrayList<>(swap.programs());
            final boolean bySwap = ctx.args().size() >= 2 && "-o".equals(ctx.arg(0))
                    && "SWAP".equalsIgnoreCase(ctx.arg(1));
            programs.sort(bySwap ? Comparator.comparingInt(ICliComputer.SwapProgram::inSwapMb).reversed()
                    : Comparator.comparingLong(ICliComputer.SwapProgram::heldBytes).reversed());
            final long total = Math.max(1L, memory.totalMb());
            for (final ICliComputer.SwapProgram program : programs) {
                final long heldMb = program.heldBytes() / RamLedger.BYTES_PER_MB;
                final String figures = String.format(Locale.ROOT, "%5.1f %5.1f %7s %6s %s",
                        share(program, memory.usedMb()), heldMb * 100.0 / total, top(heldMb), top(program.inSwapMb()),
                        program.name().toLowerCase(Locale.ROOT).replace(' ', '-'));
                ctx.out().line(ROW.with(String.format(Locale.ROOT, "%7d", program.pid()), figures));
            }
        }

        /* A program's share of the processor: of half the machine's time, what its share of what is promised takes. */
        private static double share(final ICliComputer.SwapProgram program, final int promisedMb) {
            return promisedMb <= 0 ? 0.0 : program.mb() * 50.0 / promisedMb;
        }

        private static String mib(final long mb) {
            return String.format(Locale.ROOT, "%.1f", (double) mb);
        }

        /* A size as top writes it: 512.0m, 1.5g, 0.0k for nothing. */
        private static String top(final long mb) {
            if (mb <= 0L) {
                return "0.0k";
            }
            return mb < 1_024L ? String.format(Locale.ROOT, "%.1fm", (double) mb)
                    : String.format(Locale.ROOT, "%.1fg", mb / 1_024.0);
        }
    }

    @TextHolder
    static final class Swapinfo implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.tool.swapinfo.summary",
                "the swap partitions, their size and how full they are");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.tool.swapinfo.about", "Lists the swap partitions"
                + " this computer uses, each with its size, what is used and free and how full it is, and their"
                + " total. When the memory and the swap are both spent, the kernel ends the program holding the"
                + " most.");
        private static final TextKey HEADER = TextKey.of("jsc.cli.tool.swapinfo.header",
                "Device              Size     Used    Avail Capacity");
        private static final TextKey TOTAL = TextKey.of("jsc.cli.tool.swapinfo.total", "Total            %s");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.FREEBSD);
        }

        @Override public String name() {
            return "swapinfo";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer.SwapUse swap = ctx.computer().swap();
            ctx.out().line(HEADER);
            for (final ICliComputer.SwapArea area : swap.areas()) {
                ctx.out().line(String.format(Locale.ROOT, "%-17s", area.device()) + figures(area.sizeMb(),
                        area.usedMb()));
            }
            if (swap.areas().size() > 1) {
                ctx.out().line(TOTAL.with(figures(swap.sizeMb(), swap.usedMb())));
            }
        }

        private static String figures(final long sizeMb, final long usedMb) {
            final long avail = Math.max(0L, sizeMb - usedMb);
            return String.format(Locale.ROOT, " %6s %8s %8s %7d%%", human(sizeMb), human(usedMb),
                    avail == 0L ? "0B" : human(avail), sizeMb == 0L ? 0L : usedMb * 100L / sizeMb);
        }
    }

    @TextHolder
    static final class Sysctl implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.tool.sysctl.summary",
                "reads what the kernel knows of the machine");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.tool.sysctl.about", "Prints the value of a"
                + " kernel variable: hw.physmem, the memory in bytes, or vm.swap_total, the swap in bytes.");
        private static final TextKey UNKNOWN = TextKey.of("jsc.cli.tool.sysctl.unknown", "sysctl: unknown oid '%s'");
        private static final TextKey USAGE = TextKey.of("jsc.cli.tool.sysctl.usage", "usage: sysctl name");

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.FREEBSD);
        }

        @Override public String name() {
            return "sysctl";
        }

        @Override public CommandGroup group() {
            return CommandGroup.MACHINE;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().line(USAGE);
                return;
            }
            final String name = ctx.arg(0);
            final long value = switch (name) {
                case "hw.physmem" -> ctx.computer().memory().totalMb() * RamLedger.BYTES_PER_MB;
                case "vm.swap_total" -> ctx.computer().swap().sizeMb() * RamLedger.BYTES_PER_MB;
                default -> -1L;
            };
            if (value < 0L) {
                ctx.out().error(UNKNOWN.with(name));
                return;
            }
            ctx.out().line(name + ": " + value);
        }
    }
}
