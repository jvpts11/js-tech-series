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
final class ScreenfetchCommand implements ICliCommand {

    private static final ResourceLocation PACKAGE = ResourceLocation.fromNamespaceAndPath("jsc", "screenfetch");

    /* How far the readout starts past the widest row of the logo: one colour, as it always was, or neofetch's. */
    private static final int GAP_ONE_COLOUR = 2;
    private static final int GAP = 3;

    @Override
    public CommandScope scope() {
        return CommandScope.on(Platform.LINUX, Platform.FREEBSD, Platform.FRAMES).fromPackage(PACKAGE.toString());
    }

    @Override
    public String name() {
        return "screenfetch";
    }

    @Override
    public List<String> aliases() {
        return List.of("neofetch");
    }

    @Override
    public String summary() {
        return "show the system logo and information";
    }

    @Override
    public String usage() {
        return "";
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
            ctx.out().error("screenfetch: no operating system installed");
            return;
        }
        ctx.computer().report(JscEvents.SCREENFETCH, info.distroId());
        final ScreenfetchLogos.Logo logo = ScreenfetchLogos.of(info.distroId());
        final List<String[]> readout = readout(ctx, info);
        final String user = "player@" + info.hostname();
        final int start = logo.width() + (logo.oneColour() ? GAP_ONE_COLOUR : GAP);
        final int rows = Math.max(logo.rows().size(), readout.size() + 2);
        for (int i = 0; i < rows; i++) {
            final List<CliSpan> left = i < logo.rows().size() ? logo.rows().get(i) : List.of();
            final CliLine line = logo.oneColour()
                    ? oneColour(left, start, rightText(i, user, readout), logo.title())
                    : drawn(left, start, i, info.hostname(), user, readout, logo);
            ctx.out().line(clipped(line, ctx.out().width()));
        }
    }

    /** The readout's labels and values, in the order screenfetch has always printed them. */
    private static List<String[]> readout(final CliContext ctx, final ICliComputer.SystemInfo info) {
        final List<String[]> out = new ArrayList<>();
        out.add(new String[] {"OS", info.os() + " "
                + KernelNames.architecture(ctx.computer().platform(), ctx.computer().processorBits())});
        out.add(new String[] {"Kernel", info.kernel()});
        out.add(new String[] {"Uptime", uptime(info.uptimeTicks())});
        out.add(new String[] {"Packages", info.packages() + " (" + ctx.computer().packageManager().command() + ")"});
        out.add(new String[] {"Shell", info.shell()});
        out.add(new String[] {"DE", info.desktop()});
        out.add(new String[] {"CPU", info.cpu()});
        out.add(new String[] {"RAM", info.ramMb() + " MB"});
        out.add(new String[] {"Disk", info.diskUsedMb() + " MB / " + info.diskTotalMb() + " MB"});
        return out;
    }

    /** What the readout says on a row, as plain text: the user, the rule under it, then one field a row. */
    private static String rightText(final int row, final String user, final List<String[]> readout) {
        if (row == 0) {
            return user;
        }
        if (row == 1) {
            return "-".repeat(user.length());
        }
        final int field = row - 2;
        return field < readout.size() ? readout.get(field)[0] + ": " + readout.get(field)[1] : "";
    }

    /* A row as the one-colour logos have always printed it: all of it in the logo's colour. */
    private static CliLine oneColour(final List<CliSpan> left, final int start, final String right,
                                     final CliStyle colour) {
        final String art = left.isEmpty() ? "" : left.getFirst().text();
        final String padded = art + " ".repeat(Math.max(0, start - art.length()));
        return new CliLine((padded + right).stripTrailing(), colour);
    }

    /* A row in neofetch's colours: the logo's own, the user and host in its title colour, the labels in its own. */
    private static CliLine drawn(final List<CliSpan> left, final int start, final int row, final String host,
                                 final String user, final List<String[]> readout, final ScreenfetchLogos.Logo logo) {
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
            final String[] field = readout.get(row - 2);
            line.plain(gap).add(field[0] + ":", logo.label()).plain(" " + field[1]);
        }
        return line.done();
    }

    /* The row cut at the terminal's edge instead of wrapped under itself, and nothing trailing after the art. */
    private static CliLine clipped(final CliLine line, final int width) {
        final List<CliSpan> kept = new ArrayList<>();
        int used = 0;
        for (final CliSpan span : line.spans()) {
            final int room = width - used;
            if (room <= 0) {
                break;
            }
            final String text = span.text().length() > room ? span.text().substring(0, room) : span.text();
            kept.add(new CliSpan(text, span.style()));
            used += text.length();
        }
        while (!kept.isEmpty() && kept.getLast().text().isBlank()) {
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
}
