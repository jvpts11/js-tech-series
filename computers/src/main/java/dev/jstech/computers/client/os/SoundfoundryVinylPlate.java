/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.SoundfoundryLayout;
import dev.jstech.computers.gui.layout.SoundfoundryLayout.Rect;
import dev.jstech.computers.operation.payload.VinylActionPayload;
import dev.jstech.computers.operation.payload.VinylStatePayload;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * The Vinyl RW Drive's plate of the classic Soundfoundry (computers/docs/design/peripherals.md), under the player where
 * the playlist goes: the drive and what lies on it, the recording or the cut and how far, where the recording goes and
 * that a song from a disc can be cut, the levels while the song plays, and REC, STOP, CUT, EJECT and LIST, which shows
 * the recordings in the system's Music/Vinyl folder to choose the one to cut.
 */
final class SoundfoundryVinylPlate {

    private final BlockPos host;
    private boolean listing;
    private int chosen = -1;

    private static final int TICKS_A_SECOND = 20;
    private static final int SECONDS_A_MINUTE = 60;
    private static final int METER_CELL = 6;
    private static final int METER_ROWS = 2;
    /* Where the meter turns from green to yellow, and from yellow to red, as shares of its cells. */
    private static final double METER_GREEN = 2.0 / 3.0;
    private static final double METER_YELLOW = 26.0 / 30.0;
    private static final String ELLIPSIS = "...";

    SoundfoundryVinylPlate(final BlockPos host) {
        this.host = host;
    }

    /** Draws the plate with its top left corner at {@code (x, y)}. */
    void render(final GuiGraphics g, final Font font, final int x, final int y, final boolean focused,
                final int pressed) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        SoundfoundrySkin.plate(g, x, y, SoundfoundryLayout.WIDTH, SoundfoundryLayout.PLAYLIST_H);
        SoundfoundrySkin.bar(g, font, x, y, words(SoundfoundryAppTexts.VINYL), focused, pressed);
        final Rect screen = SoundfoundryLayout.VINYL_SCREEN;
        SoundfoundrySkin.lcd(g, x + screen.x(), y + screen.y(), screen.w(), screen.h());
        final VinylStatePayload state = SoundfoundryVinyl.state(host);
        if (listing) {
            renderList(g, font, x, y, state);
        } else {
            renderDrive(g, font, x, y, state);
        }
        final boolean drive = state != null && state.drive() != 0L;
        final boolean working = drive && state.work() != 0;
        button(g, font, x, y, SoundfoundryLayout.REC, SoundfoundryAppTexts.REC, drive
                && state.work() == 1, !drive || state.platter() != VinylStatePayload.DISC || working);
        button(g, font, x, y, SoundfoundryLayout.VINYL_STOP, SoundfoundryAppTexts.VINYL_STOP, false, !working);
        button(g, font, x, y, SoundfoundryLayout.CUT, SoundfoundryAppTexts.CUT, drive && state.work() == 2,
                !drive || state.platter() != VinylStatePayload.BLANK || working || cutChoice(state) == null);
        button(g, font, x, y, SoundfoundryLayout.VINYL_EJECT, SoundfoundryAppTexts.VINYL_EJECT, false,
                !drive || state.platter() == VinylStatePayload.EMPTY || working);
        button(g, font, x, y, SoundfoundryLayout.LIST_BUTTON, SoundfoundryAppTexts.LIST, listing, false);
        final Rect time = SoundfoundryLayout.VINYL_TIME;
        SoundfoundrySkin.lcd(g, x + time.x(), y + time.y(), time.w(), time.h());
        if (working) {
            final String clock = clock(state.elapsed()) + "/" + clock(state.length());
            SoundfoundrySkin.lit(g, font, clock, x + time.right() - 4 - font.width(clock), y + time.y() + 2,
                    c.amber());
        }
    }

    /** Answers a click on the plate, {@code (lx, ly)} from its top left corner. */
    void clicked(final double lx, final double ly) {
        final VinylStatePayload state = SoundfoundryVinyl.state(host);
        if (SoundfoundryLayout.LIST_BUTTON.contains(lx, ly)) {
            listing = !listing;
            return;
        }
        if (state == null || state.drive() == 0L) {
            return;
        }
        if (SoundfoundryLayout.REC.contains(lx, ly)) {
            SoundfoundryVinyl.ask(host, VinylActionPayload.RECORD, "");
        } else if (SoundfoundryLayout.VINYL_STOP.contains(lx, ly)) {
            SoundfoundryVinyl.ask(host, VinylActionPayload.STOP, "");
        } else if (SoundfoundryLayout.CUT.contains(lx, ly)) {
            final VinylStatePayload.Recording cut = cutChoice(state);
            if (cut != null) {
                SoundfoundryVinyl.ask(host, VinylActionPayload.CUT, cut.path());
            }
        } else if (SoundfoundryLayout.VINYL_EJECT.contains(lx, ly)) {
            SoundfoundryVinyl.ask(host, VinylActionPayload.EJECT, "");
        } else if (listing) {
            final int row = SoundfoundryLayout.rowAt(lx, ly);
            if (row >= 0 && row < state.recordings().size()) {
                chosen = row;
            }
        }
    }

    /* The drive's lines: what lies on it, the work and how far, where the recording goes, and the cut's hint. */
    private void renderDrive(final GuiGraphics g, final Font font, final int x, final int y,
                             @Nullable final VinylStatePayload state) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        if (state == null || state.drive() == 0L) {
            line(g, font, x, y, SoundfoundryLayout.VINYL_DRIVE_Y, SoundfoundryAppTexts.VINYL_NO_DRIVE.text(),
                    c.amberDim());
            return;
        }
        final Text drive = switch (state.platter()) {
            case VinylStatePayload.DISC -> SoundfoundryAppTexts.VINYL_DISC.with(state.disc());
            case VinylStatePayload.BLANK -> SoundfoundryAppTexts.VINYL_BLANK.text();
            default -> SoundfoundryAppTexts.VINYL_EMPTY.text();
        };
        line(g, font, x, y, SoundfoundryLayout.VINYL_DRIVE_Y, drive, c.amber());
        final Text work = switch (state.work()) {
            case 1 -> SoundfoundryAppTexts.VINYL_RECORDING.with(clock(state.elapsed()), clock(state.length()));
            case 2 -> SoundfoundryAppTexts.VINYL_CUTTING.with(clock(state.elapsed()), clock(state.length()));
            default -> state.problem().isEmpty() ? SoundfoundryAppTexts.VINYL_READY.text() : state.problem();
        };
        line(g, font, x, y, SoundfoundryLayout.VINYL_WORK_Y, work, state.work() == 0 && !state.problem().isEmpty()
                ? c.amberDim() : c.amber());
        final Rect bar = SoundfoundryLayout.VINYL_PROGRESS;
        SoundfoundrySkin.progress(g, x + bar.x(), y + bar.y(), bar.w(), bar.h(), state.length() <= 0 ? 0.0
                : state.elapsed() / (double) state.length(), false);
        meter(g, x, y, state.work() == 1);
        if (state.work() == 1) {
            line(g, font, x, y, SoundfoundryLayout.VINYL_SAVING_Y, fitted(font, SoundfoundryAppTexts.VINYL_SAVING,
                    state.saving()), c.amberDim());
            line(g, font, x, y, SoundfoundryLayout.VINYL_FROM_Y, SoundfoundryAppTexts.VINYL_FROM_DISC.text(),
                    c.amberDim());
        } else if (!state.saving().isEmpty()) {
            line(g, font, x, y, SoundfoundryLayout.VINYL_SAVING_Y, fitted(font, SoundfoundryAppTexts.VINYL_KEPT,
                    state.saving()), c.amberDim());
        }
        final Text bringing = SoundfoundryVinyl.bringing();
        if (bringing != null) {
            line(g, font, x, y, SoundfoundryLayout.VINYL_FROM_Y + 12, bringing, c.amberDim());
        }
        final VinylStatePayload.Recording cut = cutChoice(state);
        line(g, font, x, y, SoundfoundryLayout.VINYL_CUT_Y, state.platter() == VinylStatePayload.BLANK && cut != null
                ? SoundfoundryAppTexts.VINYL_CUT_CHOSEN.with(cut.title())
                : SoundfoundryAppTexts.VINYL_PUT_BLANK.text(), c.amberDim());
    }

    /* The recordings of the Music/Vinyl folder, the one to cut lit, those from no disc dim. */
    private void renderList(final GuiGraphics g, final Font font, final int x, final int y,
                            @Nullable final VinylStatePayload state) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        final List<VinylStatePayload.Recording> all = state == null ? List.of() : state.recordings();
        if (all.isEmpty()) {
            line(g, font, x, y, SoundfoundryLayout.ROW_TOP, SoundfoundryAppTexts.VINYL_NONE.text(), c.amberDim());
            return;
        }
        final VinylStatePayload.Recording cut = cutChoice(state);
        for (int i = 0; i < all.size() && i < SoundfoundryLayout.ROWS; i++) {
            final VinylStatePayload.Recording one = all.get(i);
            final int rowY = y + SoundfoundryLayout.ROW_TOP + i * SoundfoundryLayout.ROW_H;
            final int ink = one == cut ? c.ink() : one.fromDisc() ? c.amber() : c.amberDim();
            final String title = (i + 1) + ". " + one.title().toUpperCase(Locale.ROOT);
            SoundfoundrySkin.lit(g, font, title, x + SoundfoundryLayout.ROW_X, rowY + 1, ink);
            final String length = one.fromDisc() ? clock((int) (one.millis() / 50L))
                    : words(SoundfoundryAppTexts.VINYL_NO_DISC_ROW);
            SoundfoundrySkin.lit(g, font, length, x + SoundfoundryLayout.LENGTH_RIGHT - font.width(length), rowY + 1,
                    ink);
        }
    }

    /* The recording CUT puts onto the blank: the one chosen in the list when it came from a disc, else the first. */
    @Nullable
    private VinylStatePayload.Recording cutChoice(@Nullable final VinylStatePayload state) {
        if (state == null) {
            return null;
        }
        final List<VinylStatePayload.Recording> all = state.recordings();
        if (chosen >= 0 && chosen < all.size() && all.get(chosen).fromDisc()) {
            return all.get(chosen);
        }
        return all.stream().filter(VinylStatePayload.Recording::fromDisc).findFirst().orElse(null);
    }

    /*
     * The level meter: two rows of cells lit to the song's loudness while it records, still otherwise; green for the
     * most of it, yellow near the top and red at it, as a recording meter shows.
     */
    private static void meter(final GuiGraphics g, final int x, final int y, final boolean live) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        final Rect meter = SoundfoundryLayout.VINYL_METER;
        final int cells = meter.w() / METER_CELL;
        final long now = System.currentTimeMillis();
        for (int row = 0; row < METER_ROWS; row++) {
            final int lit = live ? (int) (cells * (0.55 + 0.25 * Math.sin(now / 180.0 + row))) : 0;
            for (int cell = 0; cell < cells; cell++) {
                final int colour = cell >= lit ? c.segmentOff() : cell < cells * METER_GREEN ? c.meterLow()
                        : cell < cells * METER_YELLOW ? c.meterMid() : c.record();
                SoundfoundrySkin.fill(g, x + meter.x() + cell * METER_CELL, y + meter.y() + row * 4, METER_CELL - 1, 3,
                        colour);
            }
        }
    }

    /*
     * {@code key} with the path {@code path}, its first folders left off for an ellipsis until the line fits the
     * screen: the end of a path, where the file is, is what is worth reading.
     */
    private static Text fitted(final Font font, final TextKey key, final String path) {
        final Rect screen = SoundfoundryLayout.VINYL_SCREEN;
        final int room = screen.w() - 2 * (SoundfoundryLayout.VINYL_TEXT_X - screen.x());
        String shown = path;
        int cut = 0;
        while (font.width(GameText.resolve(key.with(shown)).toUpperCase(Locale.ROOT)) > room) {
            cut = path.indexOf('/', cut + 1);
            if (cut < 0) {
                break;
            }
            shown = ELLIPSIS + path.substring(cut);
        }
        return key.with(shown);
    }

    private static void line(final GuiGraphics g, final Font font, final int x, final int y, final int lineY,
                             final Text text, final int colour) {
        SoundfoundrySkin.lit(g, font, GameText.resolve(text).toUpperCase(Locale.ROOT),
                x + SoundfoundryLayout.VINYL_TEXT_X, y + lineY, colour);
    }

    private static void button(final GuiGraphics g, final Font font, final int x, final int y, final Rect at,
                               final TextKey label, final boolean lit, final boolean dim) {
        SoundfoundrySkin.labelButton(g, font, x + at.x(), y + at.y(), at.w(), at.h(), words(label), lit, dim);
    }

    /* A count of ticks as a song's clock, minutes and seconds, each two figures. */
    private static String clock(final int ticks) {
        final int seconds = Math.max(0, ticks) / TICKS_A_SECOND;
        return String.format(Locale.ROOT, "%02d:%02d", seconds / SECONDS_A_MINUTE, seconds % SECONDS_A_MINUTE);
    }

    private static String words(final TextKey key) {
        return GameText.resolve(key);
    }
}
