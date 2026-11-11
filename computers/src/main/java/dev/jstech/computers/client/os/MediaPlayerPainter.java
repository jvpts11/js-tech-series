/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.audio.MediaLibrary;
import dev.jstech.computers.operation.payload.SoundfoundryStatePayload;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Draws the Media Player in each desktop's look, after the player that desktop shipped (the mocks of the Media
 * Player's ten looks), and marks on the player every place a click does something. What it shows comes from the
 * player; what is clicked goes back to it as a {@link MediaPlayerApp.Kind}.
 */
@PaletteHolder
final class MediaPlayerPainter {

    private final MediaPlayerApp app;

    /** The choices of the speed dialog, which the player reads back from a click. */
    static final int SLOWER = 0;
    static final int FASTER = 1;
    static final int DONE = 2;
    /**
     * The players' own colours, {@code jsc:app/media_player}: the picture's black, XP's rail and its pick, XP's lower
     * bar and its bars, the round blue button, the dark lower bar of 7 and 10 and its words, the track, CDE's wave
     * ground with its played and its unplayed half, Celluloid's green, the QuinceTime bar, the refusal's red; then the
     * white of words and signs on the dark, the ink of signs on a button, the 95 and CDE buttons' face, shadow and
     * dark edge, XP's unplayed track, a song's name in the dark, and Celluloid's pick.
     */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/media_player",
            new Colours(0xff000000, 0xff0d2b6b, 0xff2156b8, 0xff10306f, 0xff3fd07e, 0xff3b82f0, 0xff1e2430,
                    0xffe8ecf2, 0xff8a8f99, 0xff1b2741, 0xff83e0cf, 0xff4d90a8, 0xff6db33f, 0xffe4e4e4,
                    0xffe53935, 0xffffffff, 0xff000000, 0xffc0c0c0, 0xff808080, 0xff404040, 0xff8fa8d8,
                    0xff9aa0a6, 0x406db33f));
    private static final float SMALL = 0.75f;
    private static final int LINE = 11;
    private static final int PAD = 4;
    private static final int BAR = 24;
    private static final int BUTTON = 12;
    private static final int BARS = 24;

    MediaPlayerPainter(final MediaPlayerApp app) {
        this.app = app;
    }

    /** Draws the player in its look over the window's content, {@code w} by {@code h} from ({@code x}, {@code y}). */
    void paint(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + h, ctx.skin().windowBg());
        switch (app.look()) {
            case FRAMES_95 -> frames95(g, ctx, x, y, w, h);
            case FRAMES_XP -> framesXp(g, ctx, x, y, w, h);
            case FRAMES_7 -> frames7(g, ctx, x, y, w, h);
            case FRAMES_10 -> frames10(g, ctx, x, y, w, h);
            case FRAMES_11 -> frames11(g, ctx, x, y, w, h);
            case KDE -> dragon(g, ctx, x, y, w, h);
            case GNOME -> videos(g, ctx, x, y, w, h);
            case CINNAMON -> celluloid(g, ctx, x, y, w, h);
            case CDE -> audio(g, ctx, x, y, w, h);
            case QUINCE -> quinceTime(g, ctx, x, y, w, h);
        }
    }

    /** Draws the dialog that is up: the refusal of a recording with no sound card, or the speed of the pictures. */
    void paintModal(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w, final int h) {
        final int dw = Math.min(w - 16, 200);
        final int dh = 62;
        final int dx = x + (w - dw) / 2;
        final int dy = y + (h - dh) / 2;
        ctx.skin().panel(g, dx, dy, dw, dh);
        g.fill(dx, dy, dx + dw, dy + LINE, ctx.skin().accent());
        final Font font = ctx.font();
        final int ok = font.width(GameText.resolve(MediaPlayerTexts.OK)) + 16;
        if (!app.refused().isEmpty()) {
            Texts.scaled(g, font, app.name(), dx + PAD, dy + 2, SMALL, PALETTE.get().white());
            round(g, dx + 14, dy + 26, 7, PALETTE.get().red());
            Draw.text(g, font, "x", dx + 12, dy + 21, PALETTE.get().white());
            wrapped(g, font, GameText.resolve(MediaPlayerTexts.NO_CARD.with(app.refused())), dx + 26, dy + 15,
                    dw - 30, ctx.skin().text());
        } else {
            Texts.scaled(g, font, GameText.resolve(MediaPlayerTexts.SPEED_TITLE), dx + PAD, dy + 2, SMALL,
                    PALETTE.get().white());
            Draw.text(g, font, GameText.resolve(MediaPlayerTexts.SPEED_LINE.with(app.fps())), dx + PAD + 22,
                    dy + 22, ctx.skin().text());
            box(g, ctx, dx + PAD, dy + 19, BUTTON + 2, BUTTON + 2, MediaPlayerApp.Kind.SPEED, SLOWER);
            Draw.text(g, font, "-", dx + PAD + 5, dy + 21, ctx.skin().text());
            box(g, ctx, dx + dw - PAD - BUTTON - 2, dy + 19, BUTTON + 2, BUTTON + 2, MediaPlayerApp.Kind.SPEED, FASTER);
            Draw.text(g, font, "+", dx + dw - PAD - BUTTON + 2, dy + 21, ctx.skin().text());
        }
        final int bx = dx + (dw - ok) / 2;
        box(g, ctx, bx, dy + dh - BUTTON - 6, ok, BUTTON + 2, MediaPlayerApp.Kind.SPEED, DONE);
        Draw.text(g, font, GameText.resolve(MediaPlayerTexts.OK), bx + 8, dy + dh - BUTTON - 3, ctx.skin().text());
    }

    // The looks

    /* The Media Player of 95: a menu bar, the picture, a track bar with its seconds, and its row of buttons. */
    private void frames95(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w,
                          final int h) {
        menuBar(g, ctx, x, y, w);
        final int vy = y + LINE + 2;
        final int vh = h - LINE - 2 - 46;
        sunken(g, x + PAD, vy, w - PAD * 2, vh);
        video(g, ctx, x + PAD + 1, vy + 1, w - PAD * 2 - 2, vh - 2, false);
        final int ty = vy + vh + 6;
        final int tx = x + PAD + 4;
        final int tw = w - PAD * 2 - 8;
        g.fill(tx, ty + 2, tx + tw, ty + 4, ctx.skin().fieldBg());
        g.fill(tx, ty + 2, tx + tw, ty + 3, PALETTE.get().track());
        final int knob = tx + (int) (app.progress() * (tw - 6));
        bevel(g, knob, ty - 2, 6, 10, false);
        app.hit(MediaPlayerApp.Kind.SEEK, 0, tx, ty - 2, tw, 10);
        final double seconds = app.length() / 1000.0;
        for (int i = 0; i <= 4; i++) {
            final String label = String.format(Locale.ROOT, "%.1f", seconds * i / 4.0);
            final int lx = tx + tw * i / 4 - Math.round(ctx.font().width(label) * SMALL / 2f);
            g.fill(tx + tw * i / 4, ty + 6, tx + tw * i / 4 + 1, ty + 9, ctx.skin().text());
            Texts.scaled(g, ctx.font(), label, lx, ty + 10, SMALL, ctx.skin().text());
        }
        final int by = y + h - BUTTON - 5;
        final MediaPlayerApp.Kind[] kinds = {MediaPlayerApp.Kind.PLAY_PAUSE, MediaPlayerApp.Kind.STOP,
            MediaPlayerApp.Kind.OPEN, null, MediaPlayerApp.Kind.PREVIOUS, MediaPlayerApp.Kind.BACK,
            MediaPlayerApp.Kind.AHEAD, MediaPlayerApp.Kind.NEXT};
        int bx = x + PAD;
        for (final MediaPlayerApp.Kind kind : kinds) {
            if (kind == null) {
                bx += 4;
                continue;
            }
            bevel(g, bx, by, BUTTON + 2, BUTTON + 2, false);
            glyph(g, kind, bx + 3, by + 3, 8, PALETTE.get().ink());
            app.hit(kind, 0, bx, by, BUTTON + 2, BUTTON + 2);
            bx += BUTTON + 3;
        }
        final String place = app.mode() == MediaPlayerApp.Mode.SEQUENCE
                ? GameText.resolve(MediaPlayerTexts.PICTURE_OF.with(app.frame() + 1, app.sequence().frames().size()))
                : GameText.resolve(MediaPlayerTexts.SECONDS_OF.with(String.format(Locale.ROOT, "%.1f",
                app.elapsed() / 1000.0), String.format(Locale.ROOT, "%.1f", seconds)));
        right(g, ctx, place, x + w - PAD, by + 3, ctx.skin().text());
    }

    /* Frames XP's: a menu bar, the rail of views, the bars or the picture, the list, and the blue lower bar. */
    private void framesXp(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w,
                          final int h) {
        final Colours c = PALETTE.get();
        menuBar(g, ctx, x, y, w);
        final int top = y + LINE + 1;
        final int bottom = y + h - 30;
        final int railW = 54;
        g.fill(x, top, x + railW, bottom, c.rail());
        final TextKey[] views = {MediaPlayerTexts.NOW_PLAYING, MediaPlayerTexts.LIBRARY,
            MediaPlayerTexts.IMAGE_SEQUENCES};
        for (int i = 0; i < views.length; i++) {
            final int ry = top + 3 + i * LINE;
            if (app.view() == i) {
                g.fill(x, ry - 1, x + railW, ry + LINE - 2, c.railPick());
            }
            Texts.scaled(g, ctx.font(), clip(ctx.font(), GameText.resolve(views[i]), railW - 6), x + 3, ry + 1,
                    SMALL, PALETTE.get().white());
            app.hit(MediaPlayerApp.Kind.VIEW, i, x, ry - 1, railW, LINE);
        }
        final int listW = Math.max(80, w / 3);
        final int vx = x + railW + 3;
        final int vw = w - railW - listW - 9;
        g.fill(vx, top + 3, vx + vw, bottom - 3, c.video());
        if (app.mode() == MediaPlayerApp.Mode.SEQUENCE) {
            video(g, ctx, vx, top + 14, vw, bottom - top - 18, false);
        } else {
            bars(g, vx + 4, bottom - 6, vw - 8, bottom - top - 30, c.bars());
        }
        Texts.scaled(g, ctx.font(), clip(ctx.font(), app.playingName(), vw - 6), vx + 3, top + 5, SMALL,
                PALETTE.get().white());
        final int lx = x + w - listW - 3;
        ctx.skin().panel(g, lx, top + 3, listW, bottom - top - 6);
        final boolean pictures = app.view() == 2;
        Texts.scaled(g, ctx.font(), GameText.resolve(pictures ? MediaPlayerTexts.IMAGE_SEQUENCES
                : MediaPlayerTexts.SONGS_HERE), lx + 3, top + 6, SMALL, ctx.skin().text());
        list(g, ctx, lx + 1, top + 16, listW - 2, bottom - top - 32, pictures);
        if (!pictures) {
            right(g, ctx, GameText.resolve(MediaPlayerTexts.TOTAL_TIME.with(MediaPlayerApp.clock(totalSongs()))),
                    lx + listW - 3, bottom - 13, ctx.skin().dim());
        }
        g.fill(x, bottom, x + w, y + h, c.xpBar());
        seekLine(g, x + 6, bottom + 4, w - 12, c.blue(), c.xpTrack());
        final int cy = bottom + 17;
        round(g, x + 16, cy, 7, c.blue());
        glyph(g, MediaPlayerApp.Kind.PLAY_PAUSE, x + 13, cy - 3, 6, PALETTE.get().white());
        app.hit(MediaPlayerApp.Kind.PLAY_PAUSE, 0, x + 9, cy - 7, 14, 14);
        controls(g, x + 28, cy - 4, PALETTE.get().white(), MediaPlayerApp.Kind.STOP, MediaPlayerApp.Kind.PREVIOUS,
                MediaPlayerApp.Kind.NEXT);
        volume(g, x + 76, cy - 4, 44, PALETTE.get().white());
        right(g, ctx, GameText.resolve(MediaPlayerTexts.PLAYING.with(app.playingName())) + "  "
                + MediaPlayerApp.clock(app.elapsed()), x + w - 6, cy - 3, PALETTE.get().white());
    }

    /* Frames 7's: the path, the tree of the library, its table, and the dark lower bar. */
    private void frames7(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w,
                         final int h) {
        final Colours c = PALETTE.get();
        final boolean pictures = app.view() == 2;
        final String where = GameText.resolve(MediaPlayerTexts.LIBRARY) + "  >  " + GameText.resolve(pictures
                ? MediaPlayerTexts.IMAGE_SEQUENCES : app.view() == 0 ? MediaPlayerTexts.PLAYLISTS
                : MediaPlayerTexts.MUSIC) + (app.view() == 1 ? "  >  " + GameText.resolve(MediaPlayerTexts.ALL_MUSIC)
                : "");
        g.fill(x, y, x + w, y + LINE + 2, ctx.skin().panelBg());
        Texts.scaled(g, ctx.font(), where, x + PAD, y + 3, SMALL, ctx.skin().text());
        final int top = y + LINE + 3;
        final int bottom = y + h - 30;
        final int treeW = 66;
        g.fill(x, top, x + treeW, bottom, ctx.skin().panelBg());
        Texts.scaled(g, ctx.font(), GameText.resolve(MediaPlayerTexts.LIBRARY), x + PAD, top + 3, SMALL,
                ctx.skin().text());
        final TextKey[] nodes = {MediaPlayerTexts.PLAYLISTS, MediaPlayerTexts.MUSIC, MediaPlayerTexts.IMAGE_SEQUENCES};
        for (int i = 0; i < nodes.length; i++) {
            final int ny = top + 3 + (i + 1) * LINE;
            if (app.view() == i) {
                g.fill(x + 2, ny - 1, x + treeW - 2, ny + LINE - 2, app.skin().listSelect());
            }
            Texts.scaled(g, ctx.font(), clip(ctx.font(), GameText.resolve(nodes[i]), treeW - 14), x + PAD + 6,
                    ny + 1, SMALL, ctx.skin().text());
            app.hit(MediaPlayerApp.Kind.VIEW, i, x, ny - 1, treeW, LINE);
        }
        final int tx = x + treeW + 2;
        final int tw = w - treeW - 2;
        Texts.scaled(g, ctx.font(), "#", tx + PAD, top + 3, SMALL, ctx.skin().dim());
        Texts.scaled(g, ctx.font(), GameText.resolve(MediaPlayerTexts.TITLE_COLUMN), tx + 16, top + 3, SMALL,
                ctx.skin().dim());
        Texts.scaled(g, ctx.font(), GameText.resolve(MediaPlayerTexts.LENGTH_COLUMN), tx + tw * 11 / 20, top + 3,
                SMALL, ctx.skin().dim());
        Texts.scaled(g, ctx.font(), GameText.resolve(MediaPlayerTexts.FILE_COLUMN), tx + tw * 7 / 10, top + 3,
                SMALL, ctx.skin().dim());
        g.fill(tx, top + LINE + 1, tx + tw, top + LINE + 2, ctx.skin().edge());
        table(g, ctx, tx, top + LINE + 3, tw, bottom - top - LINE - 4);
        g.fill(x, bottom, x + w, y + h, c.darkBar());
        g.fill(x + PAD, bottom + 2, x + PAD + (int) ((w - PAD * 2) * app.progress()), bottom + 3, c.blue());
        Texts.scaled(g, ctx.font(), clip(ctx.font(), app.playingName(), 80), x + PAD, bottom + 7, SMALL, c.light());
        Texts.scaled(g, ctx.font(), MediaPlayerApp.clock(app.elapsed()), x + PAD, bottom + 17, SMALL, c.light());
        app.hit(MediaPlayerApp.Kind.SEEK, 0, x + PAD, bottom, w - PAD * 2, 5);
        final int mid = x + w / 2;
        controls(g, mid - 62, bottom + 11, c.light(), MediaPlayerApp.Kind.SHUFFLE, MediaPlayerApp.Kind.REPEAT,
                MediaPlayerApp.Kind.STOP, MediaPlayerApp.Kind.PREVIOUS);
        round(g, mid + 6, bottom + 15, 8, c.blue());
        glyph(g, MediaPlayerApp.Kind.PLAY_PAUSE, mid + 3, bottom + 12, 6, PALETTE.get().white());
        app.hit(MediaPlayerApp.Kind.PLAY_PAUSE, 0, mid - 2, bottom + 7, 16, 16);
        controls(g, mid + 18, bottom + 11, c.light(), MediaPlayerApp.Kind.NEXT);
        volume(g, x + w - 56, bottom + 11, 46, c.light());
    }

    /* Frames 10's: the picture, what plays over it, and the lower bar with its round button. */
    private void frames10(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w,
                          final int h) {
        final Colours c = PALETTE.get();
        g.fill(x, y, x + w, y + h, c.video());
        if (app.filled()) {
            video(g, ctx, x, y, w, h, false);
            app.hit(MediaPlayerApp.Kind.FILL, 0, x, y, w, h);
            return;
        }
        final int bottom = y + h - 28;
        video(g, ctx, x, y + 20, w, bottom - y - 22, false);
        Texts.scaled(g, ctx.font(), app.playingName(), x + PAD, y + 2, SMALL, c.light());
        Texts.scaled(g, ctx.font(), app.mode() == MediaPlayerApp.Mode.SEQUENCE ? GameText.resolve(
                MediaPlayerTexts.PICTURE_RATE.with(app.frame() + 1, app.sequence().frames().size(), app.fps()))
                : MediaPlayerApp.clock(app.length()), x + PAD, y + 11, SMALL, c.light());
        g.fill(x, bottom, x + w, y + h, c.darkBar());
        seekLine(g, x + PAD, bottom + 3, w - PAD * 2, c.blue(), c.track());
        Texts.scaled(g, ctx.font(), MediaPlayerApp.clock(app.elapsed()), x + PAD, bottom + 13, SMALL, c.light());
        final int mid = x + w / 2;
        controls(g, mid - 62, bottom + 11, c.light(), MediaPlayerApp.Kind.SHUFFLE, MediaPlayerApp.Kind.REPEAT,
                MediaPlayerApp.Kind.STOP, MediaPlayerApp.Kind.PREVIOUS);
        ring(g, mid + 6, bottom + 15, 8, c.blue());
        glyph(g, MediaPlayerApp.Kind.PLAY_PAUSE, mid + 3, bottom + 12, 6, c.light());
        app.hit(MediaPlayerApp.Kind.PLAY_PAUSE, 0, mid - 2, bottom + 7, 16, 16);
        controls(g, mid + 18, bottom + 11, c.light(), MediaPlayerApp.Kind.NEXT);
        controls(g, x + w - 20, bottom + 11, c.light(), MediaPlayerApp.Kind.FILL);
    }

    /* Frames 11's: the views at the left, the library or the picture, and the lower bar with its seek line. */
    private void frames11(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w,
                          final int h) {
        final Colours c = PALETTE.get();
        final int bottom = y + h - 34;
        final int navW = 66;
        g.fill(x, y, x + navW, bottom, ctx.skin().panelBg());
        final TextKey[] views = {MediaPlayerTexts.HOME, MediaPlayerTexts.MUSIC_LIBRARY, MediaPlayerTexts.VIDEO_LIBRARY,
            MediaPlayerTexts.PLAY_QUEUE};
        for (int i = 0; i < views.length; i++) {
            final int ny = y + 6 + i * 14;
            if (app.view() == i) {
                g.fill(x + 2, ny - 2, x + navW - 2, ny + 10, app.skin().listSelect());
                g.fill(x + 2, ny, x + 4, ny + 8, ctx.skin().accent());
            }
            Texts.scaled(g, ctx.font(), clip(ctx.font(), GameText.resolve(views[i]), navW - 12), x + 8, ny + 1,
                    SMALL, ctx.skin().text());
            app.hit(MediaPlayerApp.Kind.VIEW, i, x, ny - 2, navW, 14);
        }
        final int mx = x + navW + PAD;
        final int mw = w - navW - PAD * 2;
        if (app.mode() == MediaPlayerApp.Mode.SEQUENCE && app.view() == 2) {
            g.fill(mx, y + PAD, mx + mw, bottom - PAD, c.video());
            video(g, ctx, mx, y + PAD, mw, bottom - y - PAD * 2, false);
        } else {
            final TextKey heading = app.view() == 2 ? MediaPlayerTexts.VIDEO_LIBRARY : app.view() == 3
                    ? MediaPlayerTexts.PLAY_QUEUE : MediaPlayerTexts.MUSIC_LIBRARY;
            Draw.text(g, ctx.font(), GameText.resolve(heading), mx, y + 5, ctx.skin().text());
            cards(g, ctx, mx, y + 20, mw, bottom - y - 24);
        }
        g.fill(x, bottom, x + w, y + h, ctx.skin().panelBg());
        g.fill(x, bottom, x + w, bottom + 1, ctx.skin().edge());
        g.fill(x + PAD, bottom + 6, x + PAD + 14, bottom + 20, c.blue());
        glyph(g, MediaPlayerApp.Kind.PLAY_PAUSE, x + PAD + 4, bottom + 10, 6, PALETTE.get().white());
        Texts.scaled(g, ctx.font(), clip(ctx.font(), app.playingName(), 60), x + PAD + 18, bottom + 7, SMALL,
                ctx.skin().text());
        Texts.scaled(g, ctx.font(), GameText.resolve(app.mode() == MediaPlayerApp.Mode.SEQUENCE
                ? MediaPlayerTexts.VIDEOS : MediaPlayerTexts.MUSIC), x + PAD + 18, bottom + 15, SMALL,
                ctx.skin().dim());
        final int mid = x + w / 2 + 20;
        controls(g, mid - 40, bottom + 5, ctx.skin().text(), MediaPlayerApp.Kind.SHUFFLE,
                MediaPlayerApp.Kind.PREVIOUS);
        round(g, mid + 2, bottom + 9, 7, c.blue());
        glyph(g, MediaPlayerApp.Kind.PLAY_PAUSE, mid - 1, bottom + 6, 6, PALETTE.get().white());
        app.hit(MediaPlayerApp.Kind.PLAY_PAUSE, 0, mid - 6, bottom + 2, 16, 14);
        controls(g, mid + 14, bottom + 5, ctx.skin().text(), MediaPlayerApp.Kind.NEXT, MediaPlayerApp.Kind.REPEAT);
        final int sx = x + navW + 10;
        final int sw = w - navW - 70;
        Texts.scaled(g, ctx.font(), MediaPlayerApp.clock(app.elapsed()), sx - 2, bottom + 22, SMALL,
                ctx.skin().text());
        seekLine(g, sx + 22, bottom + 25, sw - 44, c.blue(), c.track());
        Texts.scaled(g, ctx.font(), MediaPlayerApp.clock(app.length()), sx + sw - 18, bottom + 22, SMALL,
                ctx.skin().text());
        volume(g, x + w - 44, bottom + 8, 36, ctx.skin().text());
    }

    /* KDE's Dragon Player: its toolbar of Play File, Pause and the volume, and the picture under it. */
    private void dragon(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w,
                        final int h) {
        final Colours c = PALETTE.get();
        final int bar = 14;
        g.fill(x, y, x + w, y + bar, ctx.skin().panelBg());
        int bx = x + PAD;
        bx = toolButton(g, ctx, bx, y + 3, "+ " + GameText.resolve(MediaPlayerTexts.PLAY_FILE),
                MediaPlayerApp.Kind.OPEN);
        toolButton(g, ctx, bx + 6, y + 3, (app.running() ? "|| " : "> ") + GameText.resolve(app.running()
                ? MediaPlayerTexts.PAUSE : MediaPlayerTexts.PLAY), MediaPlayerApp.Kind.PLAY_PAUSE);
        right(g, ctx, MediaPlayerApp.clock(app.elapsed()), x + w - PAD, y + 4, ctx.skin().text());
        volume(g, x + w - 96, y + 3, 64, ctx.skin().text());
        g.fill(x, y + bar, x + w, y + h, c.video());
        video(g, ctx, x, y + bar, w, h - bar, false);
        app.hit(MediaPlayerApp.Kind.SEEK, 0, x, y + h - 4, w, 4);
        g.fill(x, y + h - 2, x + (int) (w * app.progress()), y + h, ctx.skin().accent());
    }

    /* GNOME's Videos: the grid of the sequences, or the one playing with its bar and a way back to the grid. */
    private void videos(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w,
                        final int h) {
        final Colours c = PALETTE.get();
        final int bar = 16;
        g.fill(x, y, x + w, y + bar, ctx.skin().panelBg());
        g.fill(x, y + bar - 1, x + w, y + bar, ctx.skin().edge());
        final String title = GameText.resolve(MediaPlayerTexts.VIDEOS);
        final int tw = ctx.font().width(title) + 16;
        g.fill(x + (w - tw) / 2, y + 3, x + (w + tw) / 2, y + bar - 3, ctx.skin().fieldBg());
        Draw.text(g, ctx.font(), title, x + (w - tw) / 2 + 8, y + 4, ctx.skin().text());
        if (app.view() == 1) {
            Draw.text(g, ctx.font(), "<", x + PAD + 2, y + 4, ctx.skin().text());
            app.hit(MediaPlayerApp.Kind.VIEW, 0, x, y, 16, bar);
            final int bottom = y + h - 18;
            g.fill(x, y + bar, x + w, bottom, c.video());
            video(g, ctx, x, y + bar, w, bottom - y - bar, false);
            g.fill(x, bottom, x + w, y + h, ctx.skin().panelBg());
            controls(g, x + PAD, bottom + 4, ctx.skin().text(), MediaPlayerApp.Kind.PLAY_PAUSE);
            seekLine(g, x + 22, bottom + 8, w - 64, ctx.skin().accent(), c.track());
            right(g, ctx, MediaPlayerApp.clock(app.elapsed()), x + w - PAD, bottom + 5, ctx.skin().text());
            return;
        }
        final List<MediaLibrary.Sequence> all = app.sequences();
        if (all.isEmpty()) {
            wrapped(g, ctx.font(), GameText.resolve(MediaPlayerTexts.NOTHING), x + PAD * 2, y + bar + 8,
                    w - PAD * 4, ctx.skin().dim());
            return;
        }
        final int columns = 3;
        final int tileW = (w - PAD * (columns + 1)) / columns;
        final int tileH = tileW * 5 / 8;
        for (int i = 0; i < all.size(); i++) {
            final int tx = x + PAD + (i % columns) * (tileW + PAD);
            final int ty = y + bar + PAD + (i / columns) * (tileH + 26);
            if (ty + tileH > y + h) {
                break;
            }
            g.fill(tx, ty, tx + tileW, ty + tileH, c.video());
            picture(g, app.thumbnail(all.get(i)), tx + 2, ty + 2, tileW - 4, tileH - 4);
            Texts.scaled(g, ctx.font(), clip(ctx.font(), FsPaths.fileName(all.get(i).folder()), tileW), tx,
                    ty + tileH + 3, SMALL, ctx.skin().text());
            Texts.scaled(g, ctx.font(), app.sequenceLine(all.get(i)), tx, ty + tileH + 12, SMALL, ctx.skin().dim());
            app.hit(MediaPlayerApp.Kind.SEQUENCE, i, tx, ty, tileW, tileH + 20);
        }
    }

    /* Cinnamon's Celluloid: its header with + and its menu, the picture, the playlist, and its green bar. */
    private void celluloid(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w,
                           final int h) {
        final Colours c = PALETTE.get();
        final int bar = 12;
        g.fill(x, y, x + w, y + bar, ctx.skin().panelBg());
        Texts.scaled(g, ctx.font(), clip(ctx.font(), app.playingName(), w - 40), x + PAD, y + 3, SMALL,
                ctx.skin().text());
        Draw.text(g, ctx.font(), "+", x + w - 22, y + 2, ctx.skin().text());
        app.hit(MediaPlayerApp.Kind.OPEN, 0, x + w - 24, y, 10, bar);
        Draw.text(g, ctx.font(), "=", x + w - 11, y + 2, ctx.skin().text());
        app.hit(MediaPlayerApp.Kind.MENU, -1, x + w - 13, y, 12, bar);
        final int bottom = y + h - 16;
        final int listW = Math.max(70, w / 4);
        g.fill(x, y + bar, x + w - listW, bottom, c.video());
        video(g, ctx, x, y + bar, w - listW, bottom - y - bar, true);
        final int lx = x + w - listW;
        g.fill(lx, y + bar, x + w, bottom, ctx.skin().fieldBg());
        Texts.scaled(g, ctx.font(), GameText.resolve(MediaPlayerTexts.PLAYLIST), lx + PAD, y + bar + 4, SMALL,
                ctx.skin().text());
        final List<SoundfoundryStatePayload.Song> playlist = app.playlist();
        for (int i = 0; i < playlist.size() && y + bar + 14 + (i + 1) * LINE < bottom; i++) {
            final int ry = y + bar + 14 + i * LINE;
            final boolean now = i == app.current() && app.mode() == MediaPlayerApp.Mode.SONG;
            if (now) {
                g.fill(lx, ry - 1, x + w, ry + LINE - 2, c.mintPick());
            }
            Texts.scaled(g, ctx.font(), clip(ctx.font(), FsPaths.fileName(playlist.get(i).path()), listW - 8),
                    lx + PAD, ry + 1, SMALL, now ? c.mint() : ctx.skin().text());
            app.hit(MediaPlayerApp.Kind.PLAYLIST, i, lx, ry - 1, listW, LINE);
        }
        g.fill(x, bottom, x + w, y + h, ctx.skin().panelBg());
        controls(g, x + PAD, bottom + 3, ctx.skin().text(), MediaPlayerApp.Kind.PLAY_PAUSE,
                MediaPlayerApp.Kind.PREVIOUS, MediaPlayerApp.Kind.NEXT);
        Texts.scaled(g, ctx.font(), MediaPlayerApp.clock(app.elapsed()), x + 48, bottom + 5, SMALL,
                ctx.skin().text());
        seekLine(g, x + 70, bottom + 7, w - 140, c.mint(), c.track());
        Texts.scaled(g, ctx.font(), MediaPlayerApp.clock(app.length()), x + w - 66, bottom + 5, SMALL,
                ctx.skin().text());
        volume(g, x + w - 40, bottom + 3, 22, ctx.skin().text());
        controls(g, x + w - 14, bottom + 3, ctx.skin().text(), MediaPlayerApp.Kind.FILL);
    }

    /* CDE's Audio: a menu bar, the wave of the recording (the picture in its place for a sequence), and its buttons. */
    private void audio(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w,
                       final int h) {
        final Colours c = PALETTE.get();
        menuBar(g, ctx, x, y, w);
        final int wy = y + LINE + 4;
        final int wh = h - LINE - 52;
        sunken(g, x + PAD, wy, w - PAD * 2, wh);
        g.fill(x + PAD + 1, wy + 1, x + w - PAD - 1, wy + wh - 1, c.wave());
        if (app.mode() == MediaPlayerApp.Mode.SEQUENCE) {
            video(g, ctx, x + PAD + 1, wy + 1, w - PAD * 2 - 2, wh - 2, false);
        } else {
            wave(g, x + PAD + 3, wy + wh / 2, w - PAD * 2 - 6, wh / 2 - 3, c.waveOn(), c.waveOff());
        }
        final int sy = wy + wh + 5;
        sunken(g, x + PAD, sy, w - PAD * 2, 6);
        final int knob = x + PAD + (int) (app.progress() * (w - PAD * 2 - 14));
        bevel(g, knob, sy - 2, 14, 10, false);
        app.hit(MediaPlayerApp.Kind.SEEK, 0, x + PAD, sy - 2, w - PAD * 2, 10);
        Texts.scaled(g, ctx.font(), MediaPlayerApp.clock(app.elapsed()), x + PAD, sy + 11, SMALL, ctx.skin().text());
        right(g, ctx, MediaPlayerApp.clock(app.length()), x + w - PAD, sy + 11, ctx.skin().text());
        final int by = y + h - BUTTON - 6;
        int bx = x + PAD;
        for (final MediaPlayerApp.Kind kind : List.of(MediaPlayerApp.Kind.PLAY_PAUSE, MediaPlayerApp.Kind.STOP)) {
            final String label = GameText.resolve(kind == MediaPlayerApp.Kind.STOP ? MediaPlayerTexts.STOP
                    : app.running() ? MediaPlayerTexts.PAUSE : MediaPlayerTexts.PLAY);
            final int bw = ctx.font().width(label) + 18;
            bevel(g, bx, by, bw, BUTTON + 3, false);
            glyph(g, kind, bx + 4, by + 4, 7, ctx.skin().text());
            Draw.text(g, ctx.font(), label, bx + 14, by + 3, ctx.skin().text());
            app.hit(kind, 0, bx, by, bw, BUTTON + 3);
            bx += bw + 6;
        }
        final String volumeWord = GameText.resolve(MediaPlayerTexts.VOLUME);
        final int vx = x + w - PAD - 60;
        right(g, ctx, volumeWord, vx - 4, by + 4, ctx.skin().text());
        sunken(g, vx, by + 5, 60, 4);
        bevel(g, vx + (int) (app.volume() * 52), by + 1, 8, 12, false);
        app.hit(MediaPlayerApp.Kind.VOLUME, 0, vx, by, 60, 14);
    }

    /* Quince's QuinceTime Player: the picture in its frame, and the bar under it with its round button. */
    private void quinceTime(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w,
                            final int h) {
        final Colours c = PALETTE.get();
        g.fill(x, y, x + w, y + h, c.quinceBar());
        final int bottom = y + h - 14;
        g.fill(x + 3, y + 3, x + w - 3, bottom - 2, c.video());
        video(g, ctx, x + 3, y + 3, w - 6, bottom - y - 5, true);
        ring(g, x + 9, bottom + 7, 5, ctx.skin().text());
        glyph(g, MediaPlayerApp.Kind.PLAY_PAUSE, x + 7, bottom + 5, 4, ctx.skin().text());
        app.hit(MediaPlayerApp.Kind.PLAY_PAUSE, 0, x + 3, bottom + 1, 12, 12);
        seekLine(g, x + 20, bottom + 6, w - 54, c.blue(), c.track());
        right(g, ctx, MediaPlayerApp.clock(app.elapsed()), x + w - 5, bottom + 4, ctx.skin().text());
    }

    // The parts

    /* The picture playing fitted in its place, or, for a song, its name in the middle of the dark. */
    private void video(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w, final int h,
                       final boolean nameOfSong) {
        if (app.mode() == MediaPlayerApp.Mode.SEQUENCE) {
            picture(g, app.picture(), x, y, w, h);
            return;
        }
        final String name = app.playingName();
        if (!name.isEmpty() || nameOfSong) {
            final int nw = Math.round(ctx.font().width(name) * SMALL);
            Texts.scaled(g, ctx.font(), name, x + (w - nw) / 2, y + h / 2 - 3, SMALL, PALETTE.get().songName());
        }
    }

    /* A picture fitted in its place: by whole pixels for one smaller than it, as Paint's are, else brought down. */
    private static void picture(final GuiGraphics g, final MediaFrames.Frame frame, final int x, final int y,
                                final int w, final int h) {
        if (frame == null || frame.texture() == null || frame.width() <= 0 || frame.height() <= 0) {
            return;
        }
        final double fit = Math.min(w / (double) frame.width(), h / (double) frame.height());
        final double scale = fit >= 1.0 ? Math.floor(fit) : fit;
        final int dw = Math.max(1, (int) (frame.width() * scale));
        final int dh = Math.max(1, (int) (frame.height() * scale));
        g.blit(frame.texture(), x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, 0f, 0f, frame.width(), frame.height(),
                frame.width(), frame.height());
    }

    /* The songs or the sequences of a list, one a line, each with its length, the one playing picked. */
    private void list(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w, final int h,
                      final boolean pictures) {
        final int rows = h / LINE;
        if (pictures) {
            final List<MediaLibrary.Sequence> all = app.sequences();
            for (int i = 0; i < Math.min(rows, all.size()); i++) {
                final boolean now = app.mode() == MediaPlayerApp.Mode.SEQUENCE
                        && all.get(i).folder().equals(app.sequence().folder());
                row(g, ctx, x, y + i * LINE, w, FsPaths.fileName(all.get(i).folder()),
                        MediaPlayerApp.clock(all.get(i).frames().size() * 1000L / app.fps()), now);
                app.hit(MediaPlayerApp.Kind.SEQUENCE, i, x, y + i * LINE, w, LINE);
            }
            return;
        }
        final List<MediaLibrary.Song> songs = app.songs();
        for (int i = 0; i < Math.min(rows, songs.size()); i++) {
            final boolean now = app.mode() == MediaPlayerApp.Mode.SONG
                    && FsPaths.fileName(songs.get(i).path()).equals(app.playingName());
            row(g, ctx, x, y + i * LINE, w, FsPaths.fileName(songs.get(i).path()),
                    MediaPlayerApp.clock(songs.get(i).millis()), now);
            app.hit(MediaPlayerApp.Kind.SONG, i, x, y + i * LINE, w, LINE);
        }
    }

    /* Frames 7's table: the view's songs or sequences with their number, length and file. */
    private void table(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w, final int h) {
        final int rows = h / LINE;
        if (app.view() == 2) {
            final List<MediaLibrary.Sequence> all = app.sequences();
            for (int i = 0; i < Math.min(rows, all.size()); i++) {
                final MediaLibrary.Sequence one = all.get(i);
                tableRow(g, ctx, x, y + i * LINE, w, i, FsPaths.fileName(one.folder()),
                        MediaPlayerApp.clock(one.frames().size() * 1000L / app.fps()), one.folder(),
                        app.mode() == MediaPlayerApp.Mode.SEQUENCE && one.folder().equals(app.sequence().folder()));
                app.hit(MediaPlayerApp.Kind.SEQUENCE, i, x, y + i * LINE, w, LINE);
            }
            return;
        }
        if (app.view() == 0) {
            final List<SoundfoundryStatePayload.Song> playlist = app.playlist();
            for (int i = 0; i < Math.min(rows, playlist.size()); i++) {
                tableRow(g, ctx, x, y + i * LINE, w, i, stem(playlist.get(i).path()),
                        MediaPlayerApp.clock(playlist.get(i).millis()), playlist.get(i).path(), i == app.current());
                app.hit(MediaPlayerApp.Kind.PLAYLIST, i, x, y + i * LINE, w, LINE);
            }
            return;
        }
        final List<MediaLibrary.Song> songs = app.songs();
        for (int i = 0; i < Math.min(rows, songs.size()); i++) {
            final MediaLibrary.Song song = songs.get(i);
            tableRow(g, ctx, x, y + i * LINE, w, i, song.title(), MediaPlayerApp.clock(song.millis()), song.path(),
                    app.mode() == MediaPlayerApp.Mode.SONG && FsPaths.fileName(song.path()).equals(app.playingName()));
            app.hit(MediaPlayerApp.Kind.SONG, i, x, y + i * LINE, w, LINE);
        }
    }

    /* Frames 11's rows: each song or sequence on a card of its own, with its kind and its length. */
    private void cards(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w, final int h) {
        final int pitch = LINE + 4;
        final int rows = h / pitch;
        if (app.view() == 2) {
            final List<MediaLibrary.Sequence> all = app.sequences();
            for (int i = 0; i < Math.min(rows, all.size()); i++) {
                card(g, ctx, x, y + i * pitch, w, FsPaths.fileName(all.get(i).folder()),
                        GameText.resolve(MediaPlayerTexts.VIDEOS), app.sequenceLine(all.get(i)), false);
                app.hit(MediaPlayerApp.Kind.SEQUENCE, i, x, y + i * pitch, w, LINE + 2);
            }
            return;
        }
        if (app.view() == 3) {
            final List<SoundfoundryStatePayload.Song> playlist = app.playlist();
            for (int i = 0; i < Math.min(rows, playlist.size()); i++) {
                card(g, ctx, x, y + i * pitch, w, stem(playlist.get(i).path()),
                        GameText.resolve(MediaPlayerTexts.MUSIC), MediaPlayerApp.clock(playlist.get(i).millis()),
                        i == app.current());
                app.hit(MediaPlayerApp.Kind.PLAYLIST, i, x, y + i * pitch, w, LINE + 2);
            }
            return;
        }
        final List<MediaLibrary.Song> songs = app.songs();
        for (int i = 0; i < Math.min(rows, songs.size()); i++) {
            card(g, ctx, x, y + i * pitch, w, stem(songs.get(i).path()),
                    GameText.resolve(MediaPlayerTexts.MUSIC), MediaPlayerApp.clock(songs.get(i).millis()),
                    app.mode() == MediaPlayerApp.Mode.SONG
                            && FsPaths.fileName(songs.get(i).path()).equals(app.playingName()));
            app.hit(MediaPlayerApp.Kind.SONG, i, x, y + i * pitch, w, LINE + 2);
        }
    }

    private void row(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w,
                            final String name, final String length, final boolean now) {
        if (now) {
            g.fill(x, y, x + w, y + LINE - 1, app.skin().listSelect());
        }
        final int lw = Math.round(ctx.font().width(length) * SMALL);
        Texts.scaled(g, ctx.font(), clip(ctx.font(), name, w - lw - 8), x + 2, y + 2, SMALL, ctx.skin().text());
        Texts.scaled(g, ctx.font(), length, x + w - lw - 2, y + 2, SMALL, ctx.skin().text());
    }

    private void tableRow(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w,
                                 final int index, final String title, final String length, final String file,
                                 final boolean now) {
        if (now) {
            g.fill(x + 1, y, x + w - 1, y + LINE - 1, app.skin().listSelect());
        }
        Texts.scaled(g, ctx.font(), Integer.toString(index + 1), x + PAD, y + 2, SMALL, ctx.skin().text());
        Texts.scaled(g, ctx.font(), clip(ctx.font(), title, w * 11 / 20 - 20), x + 16, y + 2, SMALL,
                ctx.skin().text());
        Texts.scaled(g, ctx.font(), length, x + w * 11 / 20, y + 2, SMALL, ctx.skin().text());
        Texts.scaled(g, ctx.font(), clip(ctx.font(), file, w * 3 / 10 - 4), x + w * 7 / 10, y + 2, SMALL,
                ctx.skin().dim());
    }

    private static void card(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w,
                             final String title, final String kind, final String length, final boolean now) {
        g.fill(x, y, x + w, y + LINE + 2, ctx.skin().fieldBg());
        g.fill(x, y + LINE + 2, x + w, y + LINE + 3, ctx.skin().edge());
        final int colour = now ? ctx.skin().accent() : ctx.skin().text();
        Texts.scaled(g, ctx.font(), clip(ctx.font(), title, w / 2 - 8), x + PAD, y + 3, SMALL, colour);
        Texts.scaled(g, ctx.font(), kind, x + w * 11 / 20, y + 3, SMALL, ctx.skin().dim());
        final int lw = Math.round(ctx.font().width(length) * SMALL);
        Texts.scaled(g, ctx.font(), length, x + w - PAD - lw, y + 3, SMALL, ctx.skin().text());
    }

    /* The menus along a player's own bar, each opening its menu. */
    private void menuBar(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w) {
        g.fill(x, y, x + w, y + LINE, ctx.skin().panelBg());
        int mx = x + PAD;
        final List<TextKey> menus = app.look().menus();
        for (int i = 0; i < menus.size(); i++) {
            final String label = GameText.resolve(menus.get(i));
            final int mw = Math.round(ctx.font().width(label) * SMALL);
            Texts.scaled(g, ctx.font(), label, mx, y + 3, SMALL, ctx.skin().text());
            app.hit(MediaPlayerApp.Kind.MENU, i, mx - 2, y, mw + 4, LINE);
            mx += mw + 8;
        }
    }

    /* A flat button of a toolbar, its words beside its sign; answers where the next one starts. */
    private int toolButton(final GuiGraphics g, final UiContext ctx, final int x, final int y, final String label,
                           final MediaPlayerApp.Kind kind) {
        final int w = Math.round(ctx.font().width(label) * SMALL) + 6;
        if (ctx.over(x, y - 1, w, 10)) {
            g.fill(x, y - 1, x + w, y + 9, app.skin().listSelect());
        }
        Texts.scaled(g, ctx.font(), label, x + 3, y + 1, SMALL, ctx.skin().text());
        app.hit(kind, 0, x, y - 1, w, 10);
        return x + w;
    }

    /* Small transport buttons in a row, each its sign alone, from {@code x}. */
    private void controls(final GuiGraphics g, final int x, final int y, final int colour,
                          final MediaPlayerApp.Kind... kinds) {
        int bx = x;
        for (final MediaPlayerApp.Kind kind : kinds) {
            glyph(g, kind, bx, y, 7, colour);
            app.hit(kind, 0, bx - 2, y - 2, 11, 11);
            bx += 14;
        }
    }

    /* A speaker and a volume slider. */
    private void volume(final GuiGraphics g, final int x, final int y, final int w, final int colour) {
        g.fill(x, y + 2, x + 2, y + 5, colour);
        g.fill(x + 2, y + 1, x + 3, y + 6, colour);
        g.fill(x + 3, y, x + 4, y + 7, colour);
        final int sx = x + 8;
        final int sw = w - 8;
        g.fill(sx, y + 3, sx + sw, y + 4, PALETTE.get().track());
        g.fill(sx, y + 3, sx + (int) (sw * app.volume()), y + 4, colour);
        g.fill(sx + (int) (sw * app.volume()) - 1, y + 1, sx + (int) (sw * app.volume()) + 2, y + 6, colour);
        app.hit(MediaPlayerApp.Kind.VOLUME, 0, sx, y - 1, sw, 9);
    }

    /* A thin seek line with its played part and its knob. */
    private void seekLine(final GuiGraphics g, final int x, final int y, final int w, final int played,
                          final int rest) {
        final int at = x + (int) (w * app.progress());
        g.fill(x, y, x + w, y + 2, rest);
        g.fill(x, y, at, y + 2, played);
        g.fill(at - 2, y - 2, at + 2, y + 4, played);
        app.hit(MediaPlayerApp.Kind.SEEK, 0, x, y - 3, w, 8);
    }

    /* XP's bars, moving while a song plays and still otherwise. */
    private void bars(final GuiGraphics g, final int x, final int base, final int w, final int most,
                      final int colour) {
        final long beat = app.running() ? Util.getMillis() / 120L : 0L;
        final int step = Math.max(2, w / BARS);
        for (int i = 0; i < BARS && i * step < w; i++) {
            final long seed = (beat * 31L + i * 7919L) * 2654435761L;
            final int height = (int) (most * (0.25 + ((seed >>> 16) & 0xFF) / 255.0 * 0.75));
            g.fill(x + i * step, base - height, x + i * step + step - 2, base, colour);
        }
    }

    /* CDE's wave: the recording's shape, the part played lit, and a line where it is now. */
    private void wave(final GuiGraphics g, final int x, final int mid, final int w, final int most, final int on,
                      final int off) {
        final int played = (int) (w * app.progress());
        final long seed = app.playingName().hashCode();
        for (int i = 0; i < w; i++) {
            final double envelope = 0.45 + 0.4 * Math.sin(i * Math.PI * 3.0 / Math.max(1, w));
            final long noise = (seed + i * 2654435761L) * 6364136223846793005L;
            final int height = Math.max(1, (int) (most * envelope * (0.3 + ((noise >>> 40) & 0xFF) / 255.0 * 0.7)));
            g.fill(x + i, mid - height, x + i + 1, mid + height, i < played ? on : off);
        }
        g.fill(x + played, mid - most - 2, x + played + 1, mid + most + 2, PALETTE.get().white());
    }

    /* A button's box that answers a click with {@code kind} and {@code index}. */
    private void box(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w, final int h,
                     final MediaPlayerApp.Kind kind, final int index) {
        ctx.skin().panel(g, x, y, w, h);
        app.hit(kind, index, x, y, w, h);
    }

    /* The sign of a transport button, {@code size} pixels across. */
    private void glyph(final GuiGraphics g, final MediaPlayerApp.Kind kind, final int x, final int y, final int size,
                       final int colour) {
        switch (kind) {
            case PLAY_PAUSE -> {
                if (app.running()) {
                    g.fill(x, y, x + size / 3, y + size, colour);
                    g.fill(x + size - size / 3, y, x + size, y + size, colour);
                } else {
                    triangle(g, x, y, size, colour, true);
                }
            }
            case STOP -> g.fill(x, y, x + size, y + size, colour);
            case PREVIOUS -> {
                g.fill(x, y, x + 1, y + size, colour);
                triangle(g, x + 1, y, size, colour, false);
            }
            case NEXT -> {
                triangle(g, x, y, size - 1, colour, true);
                g.fill(x + size - 1, y, x + size, y + size, colour);
            }
            case BACK -> {
                triangle(g, x, y, size / 2 + 1, colour, false);
                triangle(g, x + size / 2, y, size / 2 + 1, colour, false);
            }
            case AHEAD -> {
                triangle(g, x, y, size / 2 + 1, colour, true);
                triangle(g, x + size / 2, y, size / 2 + 1, colour, true);
            }
            case OPEN -> {
                triangle(g, x, y, size, colour, size, true);
                g.fill(x, y + size - 1, x + size, y + size, colour);
            }
            case SHUFFLE -> {
                g.fill(x, y + 1, x + size, y + 2, colour);
                g.fill(x, y + size - 2, x + size, y + size - 1, colour);
                g.fill(x + size / 2, y + 1, x + size / 2 + 1, y + size - 1, colour);
            }
            case REPEAT -> {
                g.fill(x, y + 1, x + size, y + 2, colour);
                g.fill(x, y + size - 2, x + size, y + size - 1, colour);
                g.fill(x, y + 1, x + 1, y + size - 1, colour);
                g.fill(x + size - 1, y + 1, x + size, y + size - 1, colour);
            }
            case FILL -> {
                g.fill(x, y, x + 3, y + 1, colour);
                g.fill(x, y, x + 1, y + 3, colour);
                g.fill(x + size - 3, y + size - 1, x + size, y + size, colour);
                g.fill(x + size - 1, y + size - 3, x + size, y + size, colour);
            }
            default -> {
            }
        }
    }

    /* A triangle {@code size} high pointing right or left. */
    private static void triangle(final GuiGraphics g, final int x, final int y, final int size, final int colour,
                                 final boolean right) {
        for (int i = 0; i < size; i++) {
            final int half = Math.min(i, size - 1 - i);
            final int span = half + 1;
            if (right) {
                g.fill(x, y + i, x + span, y + i + 1, colour);
            } else {
                g.fill(x + size / 2 + 1 - span, y + i, x + size / 2 + 1, y + i + 1, colour);
            }
        }
    }

    /* An eject sign: a triangle pointing up over its bar. */
    private static void triangle(final GuiGraphics g, final int x, final int y, final int size, final int colour,
                                 final int wide, final boolean up) {
        final int rows = size / 2;
        for (int i = 0; i < rows; i++) {
            final int span = up ? i * 2 + 1 : (rows - i) * 2 - 1;
            final int left = x + (wide - span) / 2;
            g.fill(left, y + i, left + span, y + i + 1, colour);
        }
    }

    /* A filled disc of radius {@code r} centred on ({@code cx}, {@code cy}). */
    private static void round(final GuiGraphics g, final int cx, final int cy, final int r, final int colour) {
        for (int dy = -r; dy <= r; dy++) {
            final int span = (int) Math.sqrt((double) r * r - (double) dy * dy);
            g.fill(cx - span, cy + dy, cx + span + 1, cy + dy + 1, colour);
        }
    }

    /* A ring of radius {@code r}, a pixel thick. */
    private static void ring(final GuiGraphics g, final int cx, final int cy, final int r, final int colour) {
        for (int dy = -r; dy <= r; dy++) {
            final int outer = (int) Math.sqrt((double) r * r - (double) dy * dy);
            final int inner = Math.abs(dy) >= r - 1 ? -1 : (int) Math.sqrt((double) (r - 1) * (r - 1)
                    - (double) dy * dy);
            if (inner < 0) {
                g.fill(cx - outer, cy + dy, cx + outer + 1, cy + dy + 1, colour);
            } else {
                g.fill(cx - outer, cy + dy, cx - inner, cy + dy + 1, colour);
                g.fill(cx + inner + 1, cy + dy, cx + outer + 1, cy + dy + 1, colour);
            }
        }
    }

    /* A raised box with light top-left edges and dark bottom-right ones, as the 95 and CDE players draw them. */
    private static void bevel(final GuiGraphics g, final int x, final int y, final int w, final int h,
                              final boolean pressed) {
        g.fill(x, y, x + w, y + h, PALETTE.get().face());
        g.fill(x, y, x + w, y + 1, pressed ? PALETTE.get().shadow() : PALETTE.get().white());
        g.fill(x, y, x + 1, y + h, pressed ? PALETTE.get().shadow() : PALETTE.get().white());
        g.fill(x, y + h - 1, x + w, y + h, pressed ? PALETTE.get().white() : PALETTE.get().dark());
        g.fill(x + w - 1, y, x + w, y + h, pressed ? PALETTE.get().white() : PALETTE.get().dark());
    }

    /* A sunken well, the way the picture and the track sit in the older players. */
    private static void sunken(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + 1, PALETTE.get().shadow());
        g.fill(x, y, x + 1, y + h, PALETTE.get().shadow());
        g.fill(x, y + h - 1, x + w, y + h, PALETTE.get().white());
        g.fill(x + w - 1, y, x + w, y + h, PALETTE.get().white());
    }

    private static void right(final GuiGraphics g, final UiContext ctx, final String text, final int right,
                              final int y, final int colour) {
        Texts.scaled(g, ctx.font(), text, right - Math.round(ctx.font().width(text) * SMALL), y, SMALL, colour);
    }

    private static void wrapped(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                                final int w, final int colour) {
        final List<FormattedCharSequence> lines = font.split(Component.literal(text), (int) (w / SMALL));
        for (int i = 0; i < lines.size(); i++) {
            g.pose().pushPose();
            g.pose().translate(x, y + i * 8, 0);
            g.pose().scale(SMALL, SMALL, 1f);
            Draw.text(g, font, lines.get(i), 0, 0, colour);
            g.pose().popPose();
        }
    }

    private static String clip(final Font font, final String text, final int width) {
        return Texts.clip(font, text, (int) (width / SMALL));
    }

    private long totalSongs() {
        long total = 0L;
        for (final MediaLibrary.Song song : app.songs()) {
            total += song.millis();
        }
        return total;
    }

    /* A file's name without its kind, as a library lists a song: theme for theme.ogg. */
    private static String stem(final String path) {
        final String name = FsPaths.fileName(path);
        final int dot = name.lastIndexOf('.');
        return dot <= 0 ? name : name.substring(0, dot);
    }

    /** The player's colours, as the palette above names them. */
    private record Colours(int video, int rail, int railPick, int xpBar, int bars, int blue, int darkBar, int light,
                           int track, int wave, int waveOn, int waveOff, int mint, int quinceBar, int red, int white,
                           int ink, int face, int shadow, int dark, int xpTrack, int songName, int mintPick) {
    }
}
