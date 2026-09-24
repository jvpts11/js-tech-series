/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.os.Branding;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.core.text.GameText;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The wall of text a machine of the first two ages printed while it tested itself.
 *
 * <p>Every line of it is read off the machine: the processor by its own model, the board it is built on, the
 * memory counted over the modules seated, the video card, and each drive with what is on it. A self-test that
 * named the kind of block instead would say the same thing on every computer, which is the one thing a machine
 * reading out its own parts must never do.
 *
 * <p>The two ages print the same facts in their own hand. The earliest board names the parts in a column of
 * short labels with the machine's name off to the right; the one after it writes them out and gives the
 * machine and its board a line of their own.
 */
public final class PostWall {

    /** Where the wall begins, and how much air is left at the right edge. */
    public static final int MARGIN = 10;

    /** The columns a drive row is printed in, from the wall's left edge: role, device, size, contents. */
    private static final int COL_DEVICE = 52;
    private static final int COL_SIZE = 176;
    private static final int COL_HOLDS = 222;

    /**
     * How many drives the self-test lists before it starts counting instead.
     *
     * <p>Enough for any computer a player builds, and few enough that the lines under the list, which are the
     * ones that say what is about to boot or why nothing can, are still on the glass.
     */
    public static final int MOST_DRIVES = 10;

    /** The amber those boards lifted a key out of a sentence with. */
    private static final int KEY = 0xFFFFE14D;

    private PostWall() {
    }

    /**
     * One printed line of a self-test.
     *
     * <p>A machine of these ages printed in one weight and lifted what it had just found out of it, so a line
     * is what leads into the finding, the finding itself, and whatever trails after it. A line the firmware
     * puts at both ends of the glass carries what belongs on the right as well.
     *
     * <p>A drive row is columns instead, because a variable-width font cannot be made to line up with spaces
     * and a list of drives that does not line up is a list nobody can read down.
     */
    private record Line(String head, String hot, String tail, String right, String[] cols) {

        static Line of(final String text) {
            return new Line(text, "", "", "", null);
        }

        static Line blank() {
            return new Line("", "", "", "", null);
        }

        static Line found(final String head, final String hot, final String tail) {
            return new Line(head, hot, tail, "", null);
        }

        static Line columns(final String role, final String device, final String size, final String holds) {
            return new Line("", "", "", "", new String[]{role, device, size, holds});
        }
    }

    /**
     * Draws the wall, revealing it line by line as the machine finds each part.
     *
     * @param state    what the machine answered about itself, or null while it is still being asked
     * @param booting  what it is about to boot, worded as the firmware says it, or empty when it can boot
     *                 nothing and the failure lines take that line's place instead
     * @param ticks    how far into the self-test the machine is, which is what reveals the lines
     * @param finished whether the self-test has run its course, which is when the last line can be said
     */
    public static void draw(final GuiGraphics g, final Font font, final FirmwareKind kind,
                            final HardwareEra era, @Nullable final FirmwareStatePayload state,
                            final String machineName, final String booting, final List<String> noBoot,
                            final int x, final int y, final int w, final int ticks, final boolean finished,
                            final int text, final int dim, final int accent) {
        final List<Line> lines = lines(kind, era, state, machineName, booting, noBoot, ticks, finished);
        final int left = x + MARGIN;
        final int right = x + w - MARGIN;
        int ty = y + 10;
        for (int i = 0; i < lines.size(); i++) {
            if (ticks < 4 + i * 3) {
                break; // lines appear one by one, like a machine actually finding its hardware
            }
            final Line line = lines.get(i);
            if (line.cols() != null) {
                drawColumns(g, font, line.cols(), left, right, ty, text, dim);
            } else {
                drawRuns(g, font, line, left, right, ty, text, dim, accent);
            }
            ty += TextWall.ROW;
        }
    }

    /**
     * The key hints along the bottom, in the words and the manner of that machine's own firmware.
     *
     * <p>The earliest boards blinked theirs; the ones after them printed a sentence and left it there, with
     * the two keys lifted out of it. Blinking both was one habit borrowed across a decade it did not belong to.
     */
    public static void hint(final GuiGraphics g, final Font font, final FirmwareKind kind, final int x,
                            final int y, final int h, final int ticks, final boolean leaving, final int dim,
                            final int accent) {
        final int ty = y + h - 14;
        if (leaving) {
            TextWall.draw(g, font, "Entering SETUP ...", x + MARGIN, ty, accent);
            return;
        }
        if (kind == FirmwareKind.CLI_BIOS) {
            if ((ticks / 10) % 2 == 0) {
                TextWall.draw(g, font, "DEL  Setup      F12  Boot Menu", x + MARGIN, ty, dim);
            }
            return;
        }
        int tx = x + MARGIN;
        tx = run(g, font, "Press ", tx, ty, dim);
        tx = run(g, font, "DEL", tx, ty, KEY);
        tx = run(g, font, " to enter SETUP, ", tx, ty, dim);
        tx = run(g, font, "F12", tx, ty, KEY);
        run(g, font, " for Boot Menu", tx, ty, dim);
    }

    /** Draws one run of a line and answers where the next one starts. */
    public static int run(final GuiGraphics g, final Font font, final String part, final int x, final int y,
                          final int color) {
        TextWall.draw(g, font, part, x, y, color);
        return x + TextWall.width(font, part);
    }

    /**
     * A drive row, each column cut to its own width.
     *
     * <p>A drive and a system are both named by whoever made them, at whatever length they chose, and a name
     * drawn at full length runs straight through the figures beside it and off the right edge of the glass.
     */
    private static void drawColumns(final GuiGraphics g, final Font font, final String[] cols, final int left,
                                    final int right, final int ty, final int text, final int dim) {
        TextWall.draw(g, font, cols[0], left + 6, ty, text);
        TextWall.draw(g, font, TextWall.clip(font, cols[1], COL_SIZE - COL_DEVICE - 6),
                left + COL_DEVICE, ty, text);
        TextWall.draw(g, font, cols[2], left + COL_SIZE, ty, dim);
        TextWall.draw(g, font, TextWall.clip(font, cols[3], right - (left + COL_HOLDS)),
                left + COL_HOLDS, ty, text);
    }

    /** A plain line: what leads in, what was found, what trails after, and whatever sits at the far end. */
    private static void drawRuns(final GuiGraphics g, final Font font, final Line line, final int left,
                                 final int right, final int ty, final int text, final int dim,
                                 final int accent) {
        int tx = left;
        if (!line.head().isEmpty()) {
            tx = run(g, font, line.head(), tx, ty, text);
        }
        if (!line.hot().isEmpty()) {
            tx = run(g, font, line.hot(), tx, ty, accent);
        }
        if (!line.tail().isEmpty()) {
            run(g, font, line.tail(), tx, ty, text);
        }
        if (!line.right().isEmpty()) {
            TextWall.right(g, font, line.right(), right, ty, dim);
        }
    }

    /** What this machine's firmware prints while it tests itself. */
    private static List<Line> lines(final FirmwareKind kind, final HardwareEra era,
                                    @Nullable final FirmwareStatePayload state, final String machineName,
                                    final String booting, final List<String> noBoot, final int ticks,
                                    final boolean finished) {
        final boolean legacy = kind == FirmwareKind.BLUE_BIOS;
        final String title = title(state, machineName);
        final List<Line> out = new ArrayList<>();
        out.add(new Line("", Branding.biosBanner(era), "", legacy ? "" : title, null));
        out.add(Line.of(Branding.firmwareCopyright(era)));
        out.add(Line.blank());
        if (state == null) {
            out.add(Line.of("Reading system configuration ..."));
            return out;
        }
        final FirmwareStatePayload.Machine machine = state.machine();
        if (legacy) {
            out.add(Line.found("", title, machine.boardName().isEmpty() ? "" : "   " + machine.boardName()));
            out.add(Line.blank());
        }
        /*
         * The processor by its own model, then what it is: a self-test reads out the machine it found, and the
         * model is the part of it a player recognises.
         */
        out.add(Line.found(legacy ? "Main Processor : " : "Processor : ",
                machine.cpuName().isEmpty() ? "not detected" : machine.cpuName(),
                machine.hasCpu() ? cpuDetail(machine, legacy) : ""));
        if (!legacy) {
            out.add(Line.of("Board     : "
                    + (machine.boardName().isEmpty() ? "not detected" : machine.boardName())));
        }
        out.add(Line.of(memoryLine(machine, legacy, ticks)));
        out.add(Line.of((legacy ? "Video Adapter  : " : "Video     : ")
                + (machine.gpuName().isEmpty() ? "none" : machine.gpuName())));
        out.add(Line.blank());
        out.add(Line.of(legacy ? "Detecting drives ..." : "Detecting drives..."));
        drives(state, out);
        out.add(Line.blank());
        if (finished) {
            /*
             * What it is about to boot, by name, or the era's own way of saying there is nothing: a machine of
             * this age told you which drive it was reaching for, and which one it had given up on.
             */
            if (!booting.isEmpty()) {
                out.add(Line.found("", "Booting from " + booting + " ...", ""));
            } else {
                for (final String line : noBoot) {
                    out.add(Line.found("", line, ""));
                }
            }
        }
        return out;
    }

    /** Every drive the firmware found, up to the room the glass has for them. */
    private static void drives(final FirmwareStatePayload state, final List<Line> out) {
        int slot = 0;
        int listed = 0;
        for (final FirmwareStatePayload.Entry entry : state.entries()) {
            /*
             * A machine can hold more drives than a screen this size has rows for, and the lines that matter
             * most are the last ones: what it is about to boot, or why it cannot. So the list says how many it
             * is not showing rather than pushing those off the bottom.
             */
            if (listed >= MOST_DRIVES) {
                out.add(Line.of("  +" + (state.entries().size() - listed) + " more"));
                return;
            }
            final String named = GameText.resolve(entry.device());
            final String role = entry.kind() == FirmwareStatePayload.KIND_DISK
                    ? "Disk " + slot++ : named;
            final String device = entry.kind() == FirmwareStatePayload.KIND_DISK ? named : "";
            out.add(Line.columns(role, device, entry.size(), entry.label()));
            listed++;
        }
    }

    /** What the machine is called: the name its owner gave it, else the kind of machine it is. */
    private static String title(@Nullable final FirmwareStatePayload state, final String fallback) {
        final String named = state == null ? "" : state.machine().name();
        return named.isEmpty() ? fallback : named;
    }

    /** What the firmware says about the processor beside its model: cores, clock and architecture. */
    private static String cpuDetail(final FirmwareStatePayload.Machine machine, final boolean legacy) {
        if (legacy) {
            return "  " + machine.cpuMhz() + " MHz  " + machine.cpuArch();
        }
        return "   " + machine.cores() + (machine.cores() == 1 ? " core   " : " cores   ")
                + machine.cpuMhz() + " MHz   " + machine.cpuArch();
    }

    /** The memory line: what the count has reached, and which modules it is counting over. */
    private static String memoryLine(final FirmwareStatePayload.Machine machine, final boolean legacy,
                                     final int ticks) {
        // The memory test counts up while the POST runs, settling on the installed total.
        final long total = (long) machine.ramMb() * 1024L;
        final long counted = Math.min(total, total * Math.max(0, ticks - 12) / 28L);
        final String amount = String.format(Locale.ROOT, "%,d", counted);
        final String modules = machine.memoryModules();
        if (legacy) {
            return "Memory Testing : " + amount + "K OK" + (modules.isEmpty() ? "" : "  " + modules);
        }
        return "Memory    : " + amount + " KB OK" + (modules.isEmpty() ? "" : "      " + modules);
    }
}
