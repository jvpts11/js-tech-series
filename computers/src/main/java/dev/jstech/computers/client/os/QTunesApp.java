/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.client.audio.SoundfoundryStates;
import dev.jstech.computers.operation.payload.SoundfoundryActionPayload;
import dev.jstech.computers.operation.payload.SoundfoundryStatePayload;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * qTunes, Quince's music player (computers/docs/design/quince.md): the machine's songs in its library, with the song
 * playing over them and the buttons that go back, play and go on. What plays is the machine's own player, the one every
 * music program on it shares, so qTunes and any other player at the same machine show the same song.
 */
@TextHolder
final class QTunesApp implements IDesktopApp {

    private final BlockPos host;
    /** The row picked in the library. */
    private int picked = -1;
    private int scroll;
    private long nextLookAt;
    private long lastClickAt;
    /* Where the controls and the library were last drawn. */
    private int controlsX;
    private int controlsY;
    private int listY;
    private int listX;
    private int sourcesW;

    private static final TextKey LIBRARY = TextKey.of("jsc.quince.qtunes.library", "Library");
    private static final TextKey PARTY_SHUFFLE = TextKey.of("jsc.quince.qtunes.party_shuffle", "Party Shuffle");
    private static final TextKey RECENTLY_ADDED = TextKey.of("jsc.quince.qtunes.recently_added", "Recently Added");
    private static final List<TextKey> SOURCES = List.of(LIBRARY, PARTY_SHUFFLE, RECENTLY_ADDED);
    private static final TextKey NAME = TextKey.of("jsc.quince.qtunes.name", "Name");
    private static final TextKey TIME = TextKey.of("jsc.quince.qtunes.time", "Time");
    private static final TextKey ARTIST = TextKey.of("jsc.quince.qtunes.artist", "Artist");
    private static final TextKey ALBUM = TextKey.of("jsc.quince.qtunes.album", "Album");
    private static final TextKey NOTHING = TextKey.of("jsc.quince.qtunes.nothing", "No songs on this computer yet.");
    private static final long LOOK_EVERY = 1000L;
    private static final long DOUBLE_CLICK = 300L;
    private static final int ROW = 11;
    private static final int TOP_H = 30;
    private static final int BUTTON = 13;

    QTunesApp(final BlockPos host) {
        this.host = host;
    }

    @Override
    public String title() {
        return QuinceAboutApp.programName("qtunes");
    }

    @Override
    public int defaultWidth() {
        return 280;
    }

    @Override
    public int defaultHeight() {
        return 170;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        look();
        final QuinceLook.Colours c = QuinceLook.PULP.colours();
        final SoundfoundryStates.Known known = SoundfoundryStates.of(this.host);
        final List<SoundfoundryStatePayload.Song> songs = known == null ? List.of() : known.songs();
        final SoundfoundryStatePayload state = known == null ? null : known.state();
        // The controls and the song playing, in its lit box.
        this.controlsX = x + 8;
        this.controlsY = y + 8;
        final boolean playing = state != null && state.status() == SoundfoundryStatePayload.PLAYING;
        for (int k = 0; k < 3; k++) {
            final int cx = this.controlsX + k * (BUTTON + 4);
            QuinceArt.disc(g, cx + BUTTON / 2, this.controlsY + BUTTON / 2, BUTTON / 2, c.frame(), 0);
            QuinceArt.disc(g, cx + BUTTON / 2, this.controlsY + BUTTON / 2, BUTTON / 2 - 1, c.button(), 0);
            final String mark = k == 0 ? "|<" : k == 1 ? (playing ? "||" : ">") : ">|";
            Draw.textCentered(g, font, mark, cx + BUTTON / 2, this.controlsY + 3, c.ink());
        }
        final int lcdX = x + 70;
        final int lcdW = width - 140;
        QuinceArt.roundRect(g, lcdX, y + 3, lcdX + lcdW, y + TOP_H - 4, 4, c.frame());
        QuinceArt.roundRect(g, lcdX + 1, y + 4, lcdX + lcdW - 1, y + TOP_H - 5, 3, c.defaultTop());
        final SoundfoundryStatePayload.Song now = current(songs, state);
        if (now != null) {
            Draw.textCentered(g, font, clip(font, now.title(), lcdW - 8), lcdX + lcdW / 2, y + 6,
                    c.ink());
            Draw.textCentered(g, font, clip(font, now.artist(), lcdW - 8), lcdX + lcdW / 2, y + 15,
                    c.ink());
        }
        // The sources down the left, the library picked.
        this.sourcesW = 70;
        final int top = y + TOP_H;
        g.fill(x, top, x + this.sourcesW, y + height, c.sidebar());
        g.fill(x + this.sourcesW, top, x + this.sourcesW + 1, y + height, c.shade());
        for (int i = 0; i < SOURCES.size(); i++) {
            final int sy = top + 3 + i * ROW;
            if (i == 0 || i == 1 && state != null && state.shuffle()) {
                g.fill(x + 1, sy - 1, x + this.sourcesW - 1, sy + ROW - 1, c.select());
            }
            Draw.text(g, font, GameText.resolve(SOURCES.get(i)), x + 4, sy, c.ink());
        }
        // The library: the songs, the one playing marked.
        this.listX = x + this.sourcesW + 1;
        final int lw = width - this.sourcesW - 1;
        g.fill(this.listX, top, this.listX + lw, top + ROW, c.face());
        final int timeX = this.listX + lw * 45 / 100;
        final int artistX = timeX + 30;
        final int albumX = this.listX + lw * 75 / 100;
        Draw.text(g, font, GameText.resolve(NAME), this.listX + 3, top + 2, c.ink());
        Draw.text(g, font, GameText.resolve(TIME), timeX, top + 2, c.ink());
        Draw.text(g, font, GameText.resolve(ARTIST), artistX, top + 2, c.ink());
        Draw.text(g, font, GameText.resolve(ALBUM), albumX, top + 2, c.ink());
        this.listY = top + ROW;
        if (songs.isEmpty()) {
            Draw.text(g, font, GameText.resolve(NOTHING), this.listX + 4, this.listY + 4, c.dim());
            return;
        }
        final int rows = Math.max(1, (y + height - this.listY) / ROW);
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, songs.size() - rows)));
        for (int i = 0; i < rows && this.scroll + i < songs.size(); i++) {
            final int index = this.scroll + i;
            final SoundfoundryStatePayload.Song song = songs.get(index);
            final int ry = this.listY + i * ROW;
            final boolean lit = index == this.picked;
            g.fill(this.listX, ry, this.listX + lw, ry + ROW, lit ? c.select() : i % 2 == 0 ? c.light() : c.track());
            final int ink = lit ? c.selectInk() : song.present() ? c.ink() : c.dim();
            Draw.text(g, font, clip(font, song.title(), timeX - this.listX - 6), this.listX + 3,
                    ry + 2, ink);
            Draw.text(g, font, minutes(song.millis()), timeX, ry + 2, ink);
            Draw.text(g, font, clip(font, song.artist(), albumX - artistX - 4), artistX, ry + 2,
                    ink);
            Draw.text(g, font, clip(font, song.album(), this.listX + lw - albumX - 2), albumX,
                    ry + 2, ink);
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (mouseY >= this.controlsY && mouseY < this.controlsY + BUTTON) {
            // On a button only: the room left of the first and the gaps between them press nothing.
            final double off = mouseX - this.controlsX;
            final int k = off < 0 || off % (BUTTON + 4) >= BUTTON ? -1 : (int) (off / (BUTTON + 4));
            if (k == 0) {
                send(SoundfoundryActionPayload.PREVIOUS, 0);
            } else if (k == 1) {
                final SoundfoundryStates.Known known = SoundfoundryStates.of(this.host);
                final boolean playing = known != null
                        && known.state().status() == SoundfoundryStatePayload.PLAYING;
                send(playing ? SoundfoundryActionPayload.PAUSE : SoundfoundryActionPayload.PLAY,
                        playing || this.picked < 0 ? -1 : this.picked);
            } else if (k == 2) {
                send(SoundfoundryActionPayload.NEXT, 0);
            }
            return;
        }
        if (mouseX < this.listX && mouseY >= this.listY - ROW + 3 + ROW && mouseY < this.listY - ROW + 3 + 2 * ROW) {
            // Party Shuffle plays the library in no order, as the real one mixed it.
            send(SoundfoundryActionPayload.SHUFFLE, 0);
            return;
        }
        if (mouseX >= this.listX && mouseY >= this.listY) {
            final int index = this.scroll + (int) ((mouseY - this.listY) / ROW);
            final long now = Util.getMillis();
            if (index == this.picked && now - this.lastClickAt < DOUBLE_CLICK) {
                send(SoundfoundryActionPayload.PLAY, index);
            }
            this.picked = index;
            this.lastClickAt = now;
        }
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        this.scroll = Math.max(0, this.scroll - (int) Math.signum(delta));
        return true;
    }

    /** Asks the machine how its player stands, once a second while the window is up. */
    private void look() {
        final long now = Util.getMillis();
        if (now >= this.nextLookAt) {
            this.nextLookAt = now + LOOK_EVERY;
            send(SoundfoundryActionPayload.LOOK, SoundfoundryStates.revisionOf(this.host));
        }
    }

    private void send(final int action, final int index) {
        PacketDistributor.sendToServer(new SoundfoundryActionPayload(this.host, action, index, 0L, List.of(),
                List.of()));
    }

    @Nullable
    private static SoundfoundryStatePayload.Song current(final List<SoundfoundryStatePayload.Song> songs,
                                                         @Nullable final SoundfoundryStatePayload state) {
        if (state == null || state.status() == SoundfoundryStatePayload.STOPPED || state.current() < 0
                || state.current() >= songs.size()) {
            return null;
        }
        return songs.get(state.current());
    }

    /** The text cut to the room it has, with dots where it was cut. */
    private static String clip(final Font font, final String text, final int room) {
        if (font.width(text) <= room) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, room - font.width("..."))) + "...";
    }

    private static String minutes(final long millis) {
        final long seconds = millis / 1000L;
        return seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
    }
}
