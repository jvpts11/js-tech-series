/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.VinylActionPayload;
import dev.jstech.computers.operation.payload.VinylStatePayload;
import dev.jstech.core.client.gui.component.Texts;
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
 * The Vinyl page of the modern Soundfoundry (computers/docs/design/peripherals.md): the Vinyl RW Drive on the
 * computer's cable and what lies on it, the recordings in the system's Music/Vinyl folder, each with where it came
 * from, and the drive's work: Record the disc, Cut onto the vinyl, Stop, Eject, and how far a recording or a cut is.
 */
final class SoundfoundryVinylPage {

    private final BlockPos host;
    private int chosen = -1;

    private static final int PAD = 12;
    private static final int TITLE_Y = 12;
    private static final int DRIVE_Y = 42;
    private static final int NOTE_Y = 54;
    private static final int HEAD_Y = 74;
    private static final int ROWS_Y = 88;
    private static final int ROW = 11;
    private static final int MOST_ROWS = 8;
    private static final int BUTTON_W = 88;
    private static final int SMALL_BUTTON_W = 44;
    private static final int BUTTON_H = 14;
    private static final int TICKS_A_SECOND = 20;
    private static final int SECONDS_A_MINUTE = 60;
    private static final String ARTIST_SPLIT = " - ";

    SoundfoundryVinylPage(final BlockPos host) {
        this.host = host;
    }

    void render(final GuiGraphics g, final Font font, final int px, final int py, final int pw,
                final SoundfoundryStandardSkin.Colours c) {
        SoundfoundryStandardSkin.big(g, font, words(SoundfoundryStandardTexts.VINYL), px + PAD, py + TITLE_Y, 2,
                c.ink(), c.base());
        final VinylStatePayload state = SoundfoundryVinyl.state(host);
        if (state == null || state.drive() == 0L) {
            SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.VINYL_NO_DRIVE), px + PAD,
                    py + DRIVE_Y, c.inkDim(), c.base());
            return;
        }
        final Text drive = switch (state.platter()) {
            case VinylStatePayload.DISC -> SoundfoundryStandardTexts.VINYL_DISC.with(state.disc());
            case VinylStatePayload.BLANK -> SoundfoundryStandardTexts.VINYL_BLANK.text();
            default -> SoundfoundryStandardTexts.VINYL_EMPTY.text();
        };
        SoundfoundryStandardSkin.text(g, font, GameText.resolve(drive), px + PAD, py + DRIVE_Y, c.ink(), c.base());
        SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.VINYL_ONLY_DISC), px + PAD,
                py + NOTE_Y, c.inkDim(), c.base());
        final int fromX = px + pw * 58 / 100;
        final int artistX = px + pw * 38 / 100;
        final int right = px + pw - PAD;
        for (final Column column : List.of(new Column("#", px + PAD), new Column(
                words(SoundfoundryStandardTexts.VINYL_COL_TITLE), px + PAD + 14), new Column(
                words(SoundfoundryStandardTexts.VINYL_COL_ARTIST), artistX), new Column(
                words(SoundfoundryStandardTexts.VINYL_COL_FROM), fromX))) {
            Texts.small(g, font, column.label(), column.x(), py + HEAD_Y, c.inkDim());
        }
        final String time = words(SoundfoundryStandardTexts.VINYL_COL_TIME);
        Texts.small(g, font, time, right - Texts.smallWidth(font, time), py + HEAD_Y, c.inkDim());
        SoundfoundryStandardSkin.fill(g, px + PAD, py + HEAD_Y + 9, pw - 2 * PAD, 1, c.line());
        final List<VinylStatePayload.Recording> all = state.recordings();
        final VinylStatePayload.Recording cut = cutChoice(state);
        if (all.isEmpty()) {
            SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.VINYL_NONE), px + PAD,
                    py + ROWS_Y, c.inkDim(), c.base());
        }
        for (int i = 0; i < all.size() && i < MOST_ROWS; i++) {
            final VinylStatePayload.Recording one = all.get(i);
            final int ry = py + ROWS_Y + i * ROW;
            if (one == cut) {
                SoundfoundryStandardSkin.fill(g, px + PAD - 2, ry - 2, pw - 2 * PAD + 4, ROW, c.picked());
            }
            final int ink = !one.fromDisc() ? c.inkOff() : one == cut ? c.orange() : c.ink();
            final int split = one.title().indexOf(ARTIST_SPLIT);
            final String title = split > 0 ? one.title().substring(split + ARTIST_SPLIT.length()) : one.title();
            final String artist = split > 0 ? one.title().substring(0, split) : "";
            SoundfoundryStandardSkin.text(g, font, Integer.toString(i + 1), px + PAD, ry, ink, c.base());
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, title, artistX - px - PAD - 18), px + PAD + 14,
                    ry, ink, c.base());
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, artist, fromX - artistX - 4), artistX, ry,
                    one == cut ? c.orange() : c.inkDim(), c.base());
            final String from = one.fromDisc() ? GameText.resolve(SoundfoundryStandardTexts.VINYL_FROM_DISC
                    .with(one.day())) : words(SoundfoundryStandardTexts.VINYL_FROM_COMPUTER);
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, from, right - fromX - 30), fromX, ry,
                    one.fromDisc() ? c.inkDim() : c.inkOff(), c.base());
            SoundfoundryStandardSkin.right(g, font, clock((int) (one.millis() / 50L)), right, ry,
                    one == cut ? c.orange() : c.inkDim(), c.base());
        }
        final int by = buttonsY(py, all.size());
        final TextKey action = action(state);
        if (action != null) {
            button(g, font, px + PAD, by, BUTTON_W, action, true, c);
        }
        if (state.platter() != VinylStatePayload.EMPTY && state.work() == 0) {
            button(g, font, px + PAD + BUTTON_W + 6, by, SMALL_BUTTON_W, SoundfoundryStandardTexts.VINYL_EJECT,
                    false, c);
        }
        final int ly = by + BUTTON_H + 8;
        if (state.work() != 0) {
            final TextKey line = state.work() == 1 ? SoundfoundryStandardTexts.VINYL_RECORDING
                    : SoundfoundryStandardTexts.VINYL_CUTTING;
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, GameText.resolve(line.with(state.song(),
                    clock(state.elapsed()), clock(state.length()))), pw - 2 * PAD), px + PAD, ly, c.ink(), c.base());
            final int barW = pw - 2 * PAD;
            SoundfoundryStandardSkin.fill(g, px + PAD, ly + 12, barW, 3, c.track());
            SoundfoundryStandardSkin.fill(g, px + PAD, ly + 12, state.length() <= 0 ? 0
                    : barW * state.elapsed() / state.length(), 3, c.orange());
            if (state.work() == 2) {
                SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.VINYL_GIVES_BACK), px + PAD,
                        ly + 20, c.inkDim(), c.base());
            }
        } else if (!state.problem().isEmpty()) {
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, GameText.resolve(state.problem()),
                    pw - 2 * PAD), px + PAD, ly, c.inkDim(), c.base());
        } else if (!state.saving().isEmpty()) {
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, GameText.resolve(SoundfoundryStandardTexts
                    .VINYL_KEPT.with(state.saving())), pw - 2 * PAD), px + PAD, ly, c.inkDim(), c.base());
        }
        final Text bringing = SoundfoundryVinyl.bringing();
        if (bringing != null) {
            SoundfoundryStandardSkin.text(g, font, GameText.resolve(bringing), px + PAD, ly + 30, c.inkDim(),
                    c.base());
        }
    }

    /** Answers a click on the page, {@code (lx, ly)} from the page's corner. */
    void clicked(final double lx, final double ly, final int pw) {
        final VinylStatePayload state = SoundfoundryVinyl.state(host);
        if (state == null || state.drive() == 0L) {
            return;
        }
        final int row = (int) Math.floor((ly - ROWS_Y + 2) / ROW);
        if (ly >= ROWS_Y - 2 && row >= 0 && row < Math.min(MOST_ROWS, state.recordings().size())) {
            chosen = row;
            return;
        }
        final int by = buttonsY(0, state.recordings().size());
        if (ly < by || ly >= by + BUTTON_H) {
            return;
        }
        if (lx >= PAD && lx < PAD + BUTTON_W) {
            final TextKey action = action(state);
            if (action == SoundfoundryStandardTexts.VINYL_RECORD) {
                SoundfoundryVinyl.ask(host, VinylActionPayload.RECORD, "");
            } else if (action == SoundfoundryStandardTexts.VINYL_STOP) {
                SoundfoundryVinyl.ask(host, VinylActionPayload.STOP, "");
            } else if (action == SoundfoundryStandardTexts.VINYL_CUT) {
                final VinylStatePayload.Recording cut = cutChoice(state);
                if (cut != null) {
                    SoundfoundryVinyl.ask(host, VinylActionPayload.CUT, cut.path());
                }
            }
        } else if (lx >= PAD + BUTTON_W + 6 && lx < PAD + BUTTON_W + 6 + SMALL_BUTTON_W) {
            SoundfoundryVinyl.ask(host, VinylActionPayload.EJECT, "");
        }
    }

    /* The drive's one action now: Stop while it works, Record with a disc, Cut with a blank and a song to cut. */
    @Nullable
    private TextKey action(final VinylStatePayload state) {
        if (state.work() != 0) {
            return SoundfoundryStandardTexts.VINYL_STOP;
        }
        if (state.platter() == VinylStatePayload.DISC) {
            return SoundfoundryStandardTexts.VINYL_RECORD;
        }
        return state.platter() == VinylStatePayload.BLANK && cutChoice(state) != null
                ? SoundfoundryStandardTexts.VINYL_CUT : null;
    }

    /* The recording to cut: the one picked when it came from a disc, else the first that did. */
    @Nullable
    private VinylStatePayload.Recording cutChoice(final VinylStatePayload state) {
        final List<VinylStatePayload.Recording> all = state.recordings();
        if (chosen >= 0 && chosen < all.size() && all.get(chosen).fromDisc()) {
            return all.get(chosen);
        }
        return all.stream().filter(VinylStatePayload.Recording::fromDisc).findFirst().orElse(null);
    }

    private static int buttonsY(final int py, final int rows) {
        return py + ROWS_Y + Math.max(1, Math.min(MOST_ROWS, rows)) * ROW + 8;
    }

    private static void button(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                               final TextKey label, final boolean primary, final SoundfoundryStandardSkin.Colours c) {
        SoundfoundryStandardSkin.fill(g, x, y, w, BUTTON_H, primary ? c.orange() : c.buttonOff());
        SoundfoundryStandardSkin.centred(g, font, words(label), x + w / 2, y + 3, primary ? c.base() : c.ink(),
                primary ? c.orange() : c.buttonOff());
    }

    /* A count of ticks as a song's length, minutes and two figures of seconds. */
    private static String clock(final int ticks) {
        final int seconds = Math.max(0, ticks) / TICKS_A_SECOND;
        return String.format(Locale.ROOT, "%d:%02d", seconds / SECONDS_A_MINUTE, seconds % SECONDS_A_MINUTE);
    }

    private static String words(final TextKey key) {
        return GameText.resolve(key);
    }

    /* A heading of the table and where it stands. */
    private record Column(String label, int x) {
    }
}
