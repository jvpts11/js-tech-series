/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.os.KernelNames;
import dev.jstech.computers.os.Platform;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * screenfetch: the system's logo with a readout of the machine beside it.
 *
 * <p>A package a player installs, on the distributions, on FreeBSD and on Frames, where it runs at the Command
 * Prompt. The logos and their colours are in {@link ScreenfetchLogos}.
 *
 * <p>It never wraps. A row longer than the terminal is wide is cut at the edge, the way neofetch switches line wrap
 * off while it prints: a logo broken over two rows, with the readout pushed into the middle of it, is no logo.
 */
@TextHolder
final class ScreenfetchCommand implements ICliCommand {

    private static final ResourceLocation PACKAGE = ResourceLocation.fromNamespaceAndPath("jsc", "screenfetch");

    /* How far the readout starts past the widest row of the logo: one colour, as it always was, or neofetch's. */
    private static final int GAP_ONE_COLOUR = 2;
    private static final int GAP = 3;

    private static final TextKey SUMMARY = TextKey.of("jsc.cli.screenfetch.summary",
            "show the system logo and information");
    private static final TextKey NO_SYSTEM = TextKey.of("jsc.cli.screenfetch.no_system",
            "no operating system installed");
    /* A label of the readout as it is printed, with what closes it before the value. */
    private static final TextKey LABEL = TextKey.of("jsc.cli.screenfetch.label", "%s:");
    private static final TextKey OS = TextKey.of("jsc.cli.screenfetch.label.os", "OS");
    private static final TextKey KERNEL = TextKey.of("jsc.cli.screenfetch.label.kernel", "Kernel");
    private static final TextKey UPTIME = TextKey.of("jsc.cli.screenfetch.label.uptime", "Uptime");
    private static final TextKey PACKAGES = TextKey.of("jsc.cli.screenfetch.label.packages", "Packages");
    private static final TextKey SHELL = TextKey.of("jsc.cli.screenfetch.label.shell", "Shell");
    private static final TextKey DESKTOP = TextKey.of("jsc.cli.screenfetch.label.desktop", "DE");
    private static final TextKey CPU = TextKey.of("jsc.cli.screenfetch.label.cpu", "CPU");
    private static final TextKey RAM = TextKey.of("jsc.cli.screenfetch.label.ram", "RAM");
    private static final TextKey DISK = TextKey.of("jsc.cli.screenfetch.label.disk", "Disk");

    @Override
    public CommandScope scope() {
        return CommandScope.on(Platform.LINUX, Platform.FREEBSD, Platform.FRAMES).fromPackage(PACKAGE.toString());
    }

    @Override
    public String name() {
        return "screenfetch";
    }

    @Override
    public CommandGroup group() {
        return CommandGroup.MACHINE;
    }

    @Override
    public List<String> aliases() {
        return List.of("neofetch");
    }

    @Override
    public Text summary() {
        return SUMMARY.text();
    }

    @Override
    public boolean available(final ICliComputer computer) {
        /*
         * A package the Mirror serves, not a built-in: 'command not found' until it is installed, the classic
         * first thing to install on a fresh system.
         */
        return computer.hasProgram(PACKAGE);
    }

    @Override
    public void run(final CliContext ctx) {
        final ICliComputer.SystemInfo info = ctx.computer().systemInfo();
        if (info == null) {
            ctx.out().error(CliTexts.SAID_BY.with(name(), NO_SYSTEM));
            return;
        }
        ctx.computer().report(JscEvents.SCREENFETCH, info.distroId());
        final ScreenfetchLogos.Logo logo = ScreenfetchLogos.of(info.distroId());
        final List<Field> readout = readout(ctx, info);
        final String user = "player@" + info.hostname();
        final int start = logo.width() + (logo.oneColour() ? GAP_ONE_COLOUR : GAP);
        final int rows = Math.max(logo.rows().size(), readout.size() + 2);
        for (int i = 0; i < rows; i++) {
            final List<CliSpan> left = i < logo.rows().size() ? logo.rows().get(i) : List.of();
            final CliLine line = logo.oneColour()
                    ? oneColour(left, start, i, user, readout, logo.title())
                    : drawn(left, start, i, info.hostname(), user, readout, logo);
            ctx.out().line(clipped(line, ctx.out().width()));
        }
    }

    /** The readout's labels and values, in the order screenfetch has always printed them. */
    private static List<Field> readout(final CliContext ctx, final ICliComputer.SystemInfo info) {
        final List<Field> out = new ArrayList<>();
        out.add(new Field(OS, info.os() + " "
                + KernelNames.architecture(ctx.computer().platform(), ctx.computer().processorBits())));
        out.add(new Field(KERNEL, info.kernel()));
        out.add(new Field(UPTIME, uptime(info.uptimeTicks())));
        out.add(new Field(PACKAGES, info.packages() + " (" + ctx.computer().packageManager().command() + ")"));
        out.add(new Field(SHELL, info.shell()));
        out.add(new Field(DESKTOP, info.desktop()));
        out.add(new Field(CPU, info.cpu()));
        out.add(new Field(RAM, info.ramMb() + " MB"));
        out.add(new Field(DISK, info.diskUsedMb() + " MB / " + info.diskTotalMb() + " MB"));
        return out;
    }

    /*
     * A row as the one-colour logos have always printed it: all of it in the logo's colour. The art, the user and
     * the rule under it are one run; a field is the art, its label in the reader's language, then its value.
     */
    private static CliLine oneColour(final List<CliSpan> left, final int start, final int row, final String user,
                                     final List<Field> readout, final CliStyle colour) {
        final String art = left.isEmpty() ? "" : left.getFirst().english();
        final String padded = art + " ".repeat(Math.max(0, start - art.length()));
        if (row == 0) {
            return new CliLine((padded + user).stripTrailing(), colour);
        }
        if (row == 1) {
            return new CliLine((padded + "-".repeat(user.length())).stripTrailing(), colour);
        }
        if (row - 2 >= readout.size()) {
            return new CliLine(art.stripTrailing(), colour);
        }
        final Field field = readout.get(row - 2);
        return CliLine.build().add(padded, colour).add(LABEL.with(field.label()), colour)
                .add((" " + field.value()).stripTrailing(), colour).done();
    }

    /* A row in neofetch's colours: the logo's own, the user and host in its title colour, the labels in its own. */
    private static CliLine drawn(final List<CliSpan> left, final int start, final int row, final String host,
                                 final String user, final List<Field> readout, final ScreenfetchLogos.Logo logo) {
        final CliLine.Builder line = CliLine.build();
        for (final CliSpan run : left) {
            line.add(run.text(), run.style());
        }
        final String gap = " ".repeat(Math.max(0, start - ScreenfetchLogos.Logo.columns(left)));
        if (row == 0) {
            line.plain(gap).add("player", logo.title()).plain("@").add(host, logo.title());
        } else if (row == 1) {
            line.plain(gap).plain("-".repeat(user.length()));
        } else if (row - 2 < readout.size()) {
            final Field field = readout.get(row - 2);
            line.plain(gap).add(LABEL.with(field.label()), logo.label()).plain(" " + field.value());
        }
        return line.done();
    }

    /*
     * The row cut at the terminal's edge instead of wrapped under itself, and nothing trailing after the art. What
     * screenfetch prints is art and figures, the same in every language, so it is measured as it is written; the
     * labels are the one part in the reader's language, measured in English and kept whole wherever they fit.
     */
    private static CliLine clipped(final CliLine line, final int width) {
        final List<CliSpan> kept = new ArrayList<>();
        int used = 0;
        for (final CliSpan span : line.spans()) {
            final int room = width - used;
            if (room <= 0) {
                break;
            }
            final String words = span.english();
            if (words.length() <= room) {
                kept.add(span);
                used += words.length();
            } else {
                kept.add(new CliSpan(words.substring(0, room), span.style()));
                used += room;
            }
        }
        while (!kept.isEmpty() && kept.getLast().english().isBlank()) {
            kept.removeLast();
        }
        return kept.isEmpty() ? new CliLine("", line.style()) : new CliLine(kept);
    }

    /** The world's uptime as {@code Xd Xh Xm} (in-game time, and the machine has been part of it). */
    private static String uptime(final long ticks) {
        final long minutes = ticks / (20L * 60L);
        final long days = minutes / (60 * 24);
        final long hours = (minutes / 60) % 24;
        final long mins = minutes % 60;
        if (days > 0) {
            return days + "d " + hours + "h " + mins + "m";
        }
        return hours > 0 ? hours + "h " + mins + "m" : mins + "m";
    }

    /** One line of the readout: what it is called and what the machine says of it. */
    private record Field(TextKey label, String value) {
    }
}
