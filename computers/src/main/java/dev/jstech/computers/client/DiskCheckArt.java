/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.DiskCheckLayout;
import dev.jstech.computers.os.boot.BootSplash;
import dev.jstech.computers.os.boot.DiskCheck;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The check a system makes of its disks as it comes up after a sudden shutdown, drawn as each system drew it: Frames
 * 95's ScanDisk on its blue text screen, chkdsk on the text screen of Frames XP and 7, the line under the mark of
 * Frames 10 and 11, and fsck on the console of Linux, FreeBSD and UNIX. The machine keeps the time; this only draws how
 * far the check has got.
 */
@PaletteHolder
final class DiskCheckArt {

    private static final int W = DiskCheckLayout.WIDTH;
    private static final int H = DiskCheckLayout.HEIGHT;
    /** ScanDisk's title in its grey band, the program's own name for itself. */
    private static final String SCANDISK = " Midsoft ScanDisk ";
    /** The drive every Frames system lives on. */
    private static final String DRIVE = "C";
    /** How long chkdsk offers to be skipped for, which it says on the screen. */
    private static final String SKIP_SECONDS = "10";
    private static final TextKey[] AREAS = {DiskCheckTexts.MEDIA_DESCRIPTOR, DiskCheckTexts.ALLOCATION_TABLES,
            DiskCheckTexts.DIRECTORY_STRUCTURE, DiskCheckTexts.FILE_SYSTEM, DiskCheckTexts.FREE_SPACE,
            DiskCheckTexts.SURFACE_SCAN};
    private static final TextKey[] STAGES = {DiskCheckTexts.VERIFYING_FILES, DiskCheckTexts.FILES_DONE,
            DiskCheckTexts.VERIFYING_INDEXES, DiskCheckTexts.INDEXES_DONE, DiskCheckTexts.VERIFYING_SECURITY,
            DiskCheckTexts.SECURITY_DONE, DiskCheckTexts.VERIFYING_JOURNAL, DiskCheckTexts.JOURNAL_DONE};
    /** The share of the check after which chkdsk says what it found. */
    private static final double SUMMARY_FROM = 0.9;
    private static final long KB_A_MB = 1_024L;

    /**
     * The screens' colours, {@code jsc:boot/disk_check}: ScanDisk's blue, its grey band and the band's black words, the
     * white of the words, the yellow of the area being checked and the grey of those still to come, the bar's black
     * track and its cyan; chkdsk's blue on XP and black on 7; a console's ground and its text.
     */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "boot/disk_check",
            new Colours(0xFF0000AA, 0xFFAAAAAA, 0xFF000000, 0xFFFFFFFF, 0xFFFFFF55, 0xFFAAAAAA, 0xFF000000,
                    0xFF00AAAA, 0xFF0000AA, 0xFF000000, 0xFF000000, 0xFFC8C8C8));

    private DiskCheckArt() {
    }

    /** Draws {@code check}, {@code ticks} into it, over the glass at {@code (x, y)}. */
    static void draw(final GuiGraphics g, final Font font, final DiskCheck check, final int x, final int y,
                     final int ticks, final double loop) {
        final Colours c = PALETTE.get();
        final double done = check.through(ticks);
        final String percent = (int) (done * 100) + "%";
        switch (check.look()) {
            case FRAMES_10, FRAMES_11 -> BootSplashArt.checking(g, font, check.look() == DiskCheck.Look.FRAMES_11,
                    x, y, W, H, loop, GameText.resolve(DiskCheckTexts.SCANNING.with(DRIVE + ":", percent)));
            case SCANDISK -> {
                g.pose().pushPose();
                g.pose().translate(x, y, 0.0F);
                scanDisk(g, font, c, done);
                g.pose().popPose();
            }
            case CHKDSK_XP, CHKDSK_7 -> {
                g.pose().pushPose();
                g.pose().translate(x, y, 0.0F);
                chkdsk(g, font, c, check, done, check.look() == DiskCheck.Look.CHKDSK_XP ? c.xpGround()
                        : c.sevenGround());
                g.pose().popPose();
            }
            case LINUX, FREEBSD, OPENBSD, OPENBSD_SD, UNIX -> {
                g.pose().pushPose();
                g.pose().translate(x, y, 0.0F);
                console(g, font, c, check.consoleLines(ticks));
                g.pose().popPose();
            }
            case NONE -> { }
        }
    }

    /** The colour the check's screen is mostly made of, which is left over the glass as the system takes it. */
    static int ground(final DiskCheck check) {
        final Colours c = PALETTE.get();
        return switch (check.look()) {
            case SCANDISK -> c.scanDiskGround();
            case CHKDSK_XP -> c.xpGround();
            case CHKDSK_7 -> c.sevenGround();
            case FRAMES_10, FRAMES_11 -> BootSplashArt.ground(check.look() == DiskCheck.Look.FRAMES_11
                    ? BootSplash.FRAMES_11 : BootSplash.FRAMES_10);
            default -> c.consoleGround();
        };
    }

    /* ScanDisk: the grey band, why it runs, its areas with the one it is on, how far it has got, and its buttons. */
    private static void scanDisk(final GuiGraphics g, final Font font, final Colours c, final double done) {
        g.fill(0, 0, W, H, c.scanDiskGround());
        g.fill(0, 0, W, DiskCheckLayout.BAND_H, c.band());
        TextWall.centered(g, font, SCANDISK, W / 2, DiskCheckLayout.BAND_TEXT_Y, c.bandInk());
        final int width = W - 2 * DiskCheckLayout.SCANDISK_MARGIN;
        int y = DiskCheckLayout.SCANDISK_TEXT_Y;
        for (final String paragraph : List.of(GameText.resolve(DiskCheckTexts.NOT_SHUT_DOWN), "",
                GameText.resolve(DiskCheckTexts.CHECKING_AREAS.with(DRIVE)))) {
            for (final String row : wrap(font, paragraph, width)) {
                TextWall.draw(g, font, row, DiskCheckLayout.SCANDISK_MARGIN, y, c.ink());
                y += DiskCheckLayout.ROW;
            }
        }
        final double through = done * AREAS.length;
        final int on = Math.min(AREAS.length - 1, (int) through);
        for (int i = 0; i < AREAS.length; i++) {
            final int ay = DiskCheckLayout.AREAS_Y + i * DiskCheckLayout.ROW;
            if (i < on) {
                TextWall.draw(g, font, "v", DiskCheckLayout.AREAS_X, ay, c.ink());
            } else if (i == on) {
                TextWall.draw(g, font, ">", DiskCheckLayout.AREAS_X, ay, c.now());
            }
            TextWall.draw(g, font, GameText.resolve(AREAS[i]), DiskCheckLayout.AREA_NAME_X, ay,
                    i <= on ? c.ink() : c.pending());
        }
        final double within = Math.min(1.0, through - on);
        TextWall.draw(g, font, GameText.resolve(DiskCheckTexts.AREA_COMPLETE.with(GameText.resolve(AREAS[on]),
                (int) (within * 100) + "%")), DiskCheckLayout.AREAS_X, DiskCheckLayout.PROGRESS_Y, c.ink());
        g.fill(DiskCheckLayout.BAR_X, DiskCheckLayout.BAR_Y, DiskCheckLayout.BAR_X + DiskCheckLayout.BAR_W,
                DiskCheckLayout.BAR_Y + DiskCheckLayout.BAR_H, c.barTrack());
        g.fill(DiskCheckLayout.BAR_X + 1, DiskCheckLayout.BAR_Y + 1,
                DiskCheckLayout.BAR_X + 1 + (int) ((DiskCheckLayout.BAR_W - 2) * within),
                DiskCheckLayout.BAR_Y + DiskCheckLayout.BAR_H - 1, c.barFill());
        final String gap = "        ";
        TextWall.centered(g, font, button(DiskCheckTexts.PAUSE) + gap + button(DiskCheckTexts.MORE_INFO) + gap
                + button(DiskCheckTexts.EXIT), W / 2, DiskCheckLayout.BUTTONS_Y, c.ink());
    }

    /* chkdsk: what it checks and why, its stages as it gets through them, and what it found at the end. */
    private static void chkdsk(final GuiGraphics g, final Font font, final Colours c, final DiskCheck check,
                               final double done, final int ground) {
        g.fill(0, 0, W, H, ground);
        final List<String> paragraphs = new ArrayList<>(List.of(
                GameText.resolve(DiskCheckTexts.CHECKING_ON.with(DRIVE + ":")),
                GameText.resolve(DiskCheckTexts.TYPE_IS.with(check.type().displayName())), "",
                GameText.resolve(DiskCheckTexts.NEEDS_CHECKING),
                GameText.resolve(DiskCheckTexts.TO_SKIP.with(SKIP_SECONDS)), ""));
        final int stages = Math.max(1, Math.min(STAGES.length, (int) Math.ceil(done * STAGES.length)));
        for (int i = 0; i < stages; i++) {
            paragraphs.add(GameText.resolve(STAGES[i]));
        }
        if (done >= SUMMARY_FROM) {
            paragraphs.add("");
            paragraphs.add(GameText.resolve(DiskCheckTexts.NO_PROBLEMS));
            paragraphs.add("");
            paragraphs.add("  " + GameText.resolve(DiskCheckTexts.TOTAL_SPACE.with(check.capacityMb() * KB_A_MB)));
            paragraphs.add("  " + GameText.resolve(DiskCheckTexts.IN_FILES.with(check.usedMb() * KB_A_MB,
                    check.files())));
            paragraphs.add("  " + GameText.resolve(DiskCheckTexts.AVAILABLE.with(check.freeMb() * KB_A_MB)));
        }
        int y = DiskCheckLayout.PAGE_MARGIN;
        for (final String paragraph : paragraphs) {
            for (final String row : wrap(font, paragraph, W - 2 * DiskCheckLayout.PAGE_MARGIN)) {
                TextWall.draw(g, font, row, DiskCheckLayout.PAGE_MARGIN, y, c.ink());
                y += DiskCheckLayout.ROW;
            }
        }
    }

    /* fsck on a console: its lines, broken where the glass ends, the newest kept when there are more than fit. */
    private static void console(final GuiGraphics g, final Font font, final Colours c, final List<String> lines) {
        g.fill(0, 0, W, H, c.consoleGround());
        final List<String> rows = new ArrayList<>();
        for (final String line : lines) {
            rows.addAll(wrapHard(font, line, W - 2 * DiskCheckLayout.CONSOLE_MARGIN));
        }
        int y = DiskCheckLayout.CONSOLE_MARGIN;
        for (int i = Math.max(0, rows.size() - DiskCheckLayout.consoleRows()); i < rows.size(); i++) {
            TextWall.draw(g, font, rows.get(i), DiskCheckLayout.CONSOLE_MARGIN, y, c.consoleInk());
            y += DiskCheckLayout.ROW;
        }
    }

    private static String button(final TextKey label) {
        return "< " + GameText.resolve(label) + " >";
    }

    /* The words of {@code text} in rows no wider than {@code width} on the wall. */
    private static List<String> wrap(final Font font, final String text, final int width) {
        final List<String> rows = new ArrayList<>();
        String row = "";
        for (final String word : text.split(" ")) {
            final String trial = row.isEmpty() ? word : row + " " + word;
            if (row.isEmpty() || TextWall.width(font, trial) <= width) {
                row = trial;
            } else {
                rows.add(row);
                row = word;
            }
        }
        rows.add(row);
        return rows;
    }

    /* A console line broken where the glass ends, word or no word, the way a text console wraps. */
    private static List<String> wrapHard(final Font font, final String text, final int width) {
        final List<String> rows = new ArrayList<>();
        String rest = text;
        while (TextWall.width(font, rest) > width && rest.length() > 1) {
            int cut = rest.length() - 1;
            while (cut > 1 && TextWall.width(font, rest.substring(0, cut)) > width) {
                cut--;
            }
            rows.add(rest.substring(0, cut));
            rest = rest.substring(cut);
        }
        rows.add(rest);
        return rows;
    }

    /** The screens' colours, as the palette above names them. */
    private record Colours(int scanDiskGround, int band, int bandInk, int ink, int now, int pending, int barTrack,
                           int barFill, int xpGround, int sevenGround, int consoleGround, int consoleInk) {
    }
}
