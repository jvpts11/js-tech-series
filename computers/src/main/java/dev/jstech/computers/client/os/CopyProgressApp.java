/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.CancelCopyPayload;
import dev.jstech.computers.operation.payload.CopyProgressPayload;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.motion.MotionKinds;
import dev.jstech.core.motion.MotionScope;
import dev.jstech.core.motion.Rhythm;
import dev.jstech.core.text.GameText;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * A copy window, in the shape each system gave it: Frames 95's and XP's "Copying..." with the paper flying from
 * folder to folder over a bar of blocks and the seconds remaining, "Deleting..." with the paper flying into the
 * Recycle Bin; Frames 11's window with its speed graph and its details; KDE 2 and 3's KIO progress dialog; GNOME 1's
 * small gmc dialog; Cinnamon's File Operations. It shows the machine's whole run of copies and goes when the run ends,
 * unless KIO was told to keep it open; Cancel calls off whatever of the run is left.
 */
final class CopyProgressApp implements IDesktopApp {

    private final BlockPos host;
    private final Style style;
    private OsSkin skin = OsSkin.fallback();
    /* Where Cancel and KIO's box were drawn last, so a click lands on what was drawn. */
    private int[] cancelAt = NOWHERE;
    private int[] keepAt = NOWHERE;
    private boolean keepOpen;
    /* The run as it was last drawn, so a window kept open still has something to say once the run is over. */
    @Nullable
    private CopyRun last;

    private static final int[] NOWHERE = {0, 0, 0, 0};
    private static final int LINE = 10;
    private static final int PAD = 4;
    private static final int BUTTON_W = 46;
    private static final int BUTTON_H = 12;
    /* The flying paper of Frames 95 and XP: sixteen pictures of 136 by 30 side by side. */
    private static final int AVI_W = 136;
    private static final int AVI_H = 30;
    private static final int AVI_FRAMES = 16;
    private static final int GRAPH_H = 36;
    private static final int AREA_ALPHA = 0x60;

    CopyProgressApp(final BlockPos host, final Style style) {
        this.host = host;
        this.style = style;
    }

    /** Whether the player asked KIO to keep this window open once the run is over. */
    boolean keptOpen() {
        return keepOpen;
    }

    @Override
    public String title() {
        final CopyRun run = run();
        return switch (style) {
            case FRAMES_95, FRAMES_XP -> GameText.resolve(run != null && run.deleting() ? CopyTexts.DELETING
                    : run != null && run.current().kind() == CopyProgressPayload.MOVE ? CopyTexts.MOVING
                    : CopyTexts.COPYING);
            case FRAMES_11 -> GameText.resolve(CopyTexts.PERCENT_COMPLETE.with(percent(run)));
            case KDE2 -> GameText.resolve(CopyTexts.PROGRESS_DIALOG);
            case GNOME1 -> GameText.resolve(CopyTexts.COPYING_FILES);
            case CINNAMON -> GameText.resolve(CopyTexts.FILE_OPERATIONS);
        };
    }

    @Override
    public int defaultWidth() {
        return switch (style) {
            case FRAMES_95, FRAMES_XP -> 192;
            case FRAMES_11 -> 238;
            case KDE2 -> 228;
            case GNOME1 -> 198;
            case CINNAMON -> 228;
        };
    }

    @Override
    public int defaultHeight() {
        return switch (style) {
            case FRAMES_95, FRAMES_XP -> 124;
            case FRAMES_11 -> 136;
            case KDE2 -> 124;
            case GNOME1 -> 80;
            case CINNAMON -> 62;
        };
    }

    @Override
    public void applySkin(final OsSkin applied) {
        this.skin = applied;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h, final int mouseX, final int mouseY, final float partialTick) {
        g.fill(x, y, x + w, y + h, skin.windowBg());
        final CopyRun run = run();
        if (run == null) {
            return;
        }
        switch (style) {
            case FRAMES_95, FRAMES_XP -> frames(g, font, run, x, y, w, h, mouseX, mouseY);
            case FRAMES_11 -> eleven(g, font, run, x, y, w, h);
            case KDE2 -> kio(g, font, run, x, y, w, h, mouseX, mouseY);
            case GNOME1 -> gmc(g, font, run, x, y, w, h, mouseX, mouseY);
            case CINNAMON -> nemo(g, font, run, x, y, w);
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (button != 0) {
            return;
        }
        if (inside(cancelAt, mouseX, mouseY)) {
            cancel(host);
        } else if (inside(keepAt, mouseX, mouseY)) {
            keepOpen = !keepOpen;
        }
    }

    /** Calls off whatever of the machine's run of copies is left. */
    static void cancel(final BlockPos host) {
        for (final CopyProgressPayload copy : DesktopCopies.run(host)) {
            if (!copy.done()) {
                PacketDistributor.sendToServer(new CancelCopyPayload(host, copy.job()));
            }
        }
    }

    /** The copy window shape of a desktop of that look, or null for one with no copy window (CDE's). */
    @Nullable
    static Style styleOf(final PanelStyle panel, final boolean period) {
        return switch (panel) {
            case FRAMES_95 -> Style.FRAMES_95;
            case FRAMES_XP -> Style.FRAMES_XP;
            case FRAMES_11 -> Style.FRAMES_11;
            case KDE -> period ? Style.KDE2 : null;
            case GNOME -> period ? Style.GNOME1 : null;
            case CINNAMON -> Style.CINNAMON;
            case CDE -> null;
        };
    }

    /** The percentage of a run done, whole, for the words that say it. */
    static int percent(@Nullable final CopyRun run) {
        return run == null ? 100 : (int) Math.floor(run.fraction() * 100.0);
    }

    /** A megabyte figure as the windows write it, with one decimal. */
    static String mb(final double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /* The run now, or the last one drawn when it is over and the window was kept open. */
    @Nullable
    private CopyRun run() {
        final CopyRun now = CopyRun.of(DesktopCopies.run(host), DesktopCopies.now());
        if (now != null) {
            last = now;
        }
        return now != null ? now : last;
    }

    /*
     * Frames 95 and XP: the flying paper at the top, the file, where from and to, the bar of blocks, the seconds left
     * and Cancel.
     */
    private void frames(final GuiGraphics g, final Font font, final CopyRun run, final int x, final int y,
                        final int w, final int h, final int mouseX, final int mouseY) {
        final String set = (style == Style.FRAMES_95 ? "frames_95_" : "frames_xp_") + (run.deleting() ? "delete"
                : "copy");
        final int frame = Math.min(AVI_FRAMES - 1, Rhythm.frame(MotionScope.spec(MotionKinds.COPY),
                MotionClock.loopMs()));
        g.blit(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "textures/gui/copy/" + set + ".png"),
                x + (w - AVI_W) / 2, y + PAD, frame * AVI_W, 0, AVI_W, AVI_H, AVI_FRAMES * AVI_W, AVI_H);
        int ly = y + PAD + AVI_H + 3;
        text(g, font, run.current().name(), x + PAD, ly, w - 2 * PAD, skin.text());
        ly += LINE;
        final String where = run.deleting() ? GameText.resolve(CopyTexts.FROM.with(run.current().from()))
                : GameText.resolve(CopyTexts.FROM_TO.with(run.current().from(), run.current().to()));
        text(g, font, where, x + PAD, ly, w - 2 * PAD, skin.text());
        ly += LINE + 1;
        blocks(g, x + PAD, ly, w - 2 * PAD, 9, run.fraction());
        ly += 12;
        text(g, font, GameText.resolve(CopyTexts.SECONDS_REMAINING.with(Math.max(1, run.secondsLeft()))),
                x + PAD, ly, w - 2 * PAD, skin.text());
        cancelButton(g, font, x + w - PAD - BUTTON_W, y + h - PAD - BUTTON_H, mouseX, mouseY);
    }

    /* A bar of separate blocks, as the classic progress bars filled. */
    private void blocks(final GuiGraphics g, final int x, final int y, final int w, final int h,
                        final double fraction) {
        skin.field(g, x, y, w, h, false);
        final int inner = w - 4;
        final int block = Math.max(3, h - 3);
        final int count = inner / (block + 1);
        final int lit = (int) Math.floor(count * fraction);
        for (int i = 0; i < lit; i++) {
            final int bx = x + 2 + i * (block + 1);
            g.fill(bx, y + 2, bx + block, y + h - 2, skin.progressFill());
        }
    }

    /* Frames 11: what is copied, the percentage, the speed graph, and the details under it. */
    private void eleven(final GuiGraphics g, final Font font, final CopyRun run, final int x, final int y,
                        final int w, final int h) {
        int ly = y + PAD;
        text(g, font, GameText.resolve(CopyTexts.COPYING_ITEMS.with(run.items(), run.current().from(),
                run.current().to())), x + PAD, ly, w - 2 * PAD, skin.text());
        ly += LINE + 2;
        text(g, font, GameText.resolve(CopyTexts.PERCENT_COMPLETE.with(percent(run))), x + PAD, ly,
                w - 2 * PAD - 14, skin.text());
        final int cancelX = x + w - PAD - 9;
        Draw.text(g, font, "x", cancelX + 2, ly, skin.text());
        cancelAt = new int[] {cancelX, ly - 1, 9, 9};
        ly += LINE + 2;
        graph(g, run, x + PAD, ly, w - 2 * PAD, GRAPH_H);
        ly += GRAPH_H + 4;
        final int value = x + PAD + 68;
        Draw.text(g, font, GameText.resolve(CopyTexts.NAME), x + PAD, ly, skin.dim());
        text(g, font, run.current().name(), value, ly, w - (value - x) - PAD, skin.text());
        ly += LINE;
        Draw.text(g, font, GameText.resolve(CopyTexts.TIME_REMAINING), x + PAD, ly, skin.dim());
        text(g, font, GameText.resolve(CopyTexts.ABOUT_SECONDS.with(Math.max(1, run.secondsLeft()))), value, ly,
                w - (value - x) - PAD, skin.text());
        ly += LINE;
        Draw.text(g, font, GameText.resolve(CopyTexts.ITEMS_REMAINING), x + PAD, ly, skin.dim());
        text(g, font, GameText.resolve(CopyTexts.ITEMS_LEFT.with(run.itemsLeft(), mb(run.mbTotal() - run.mbDone()))),
                value, ly, w - (value - x) - PAD, skin.text());
        Draw.text(g, font, GameText.resolve(CopyTexts.FEWER_DETAILS), x + PAD, y + h - PAD - 8, skin.accent());
    }

    /*
     * The speed graph: the area of the speed so far growing from the left with the current speed's line over it.
     * The speed a disk keeps wavers a little round its rate, and is the same each time for the same copy.
     */
    private void graph(final GuiGraphics g, final CopyRun run, final int x, final int y, final int w, final int h) {
        skin.panel(g, x, y, w, h);
        for (int gx = x + 10; gx < x + w; gx += 10) {
            g.fill(gx, y + 1, gx + 1, y + h - 1, skin.listHover());
        }
        final int reached = (int) Math.floor((w - 2) * run.fraction());
        final long seed = run.current().job();
        // The area under the speed is the bar's colour at three eighths of its strength.
        final int area = skin.progressFill() & 0xFFFFFF | AREA_ALPHA << 24;
        int lastTop = y + h - 1;
        for (int i = 0; i < reached; i++) {
            final double along = i / (double) Math.max(1, w - 2);
            final double speed = 0.62 + 0.08 * Math.sin(along * 23.0 + seed)
                    + 0.05 * Math.sin(along * 57.0 + seed * 3) - (along < 0.06 ? 0.4 * (1.0 - along / 0.06) : 0.0);
            final int top = y + h - 1 - (int) Math.round(Math.max(0.05, speed) * (h - 2));
            g.fill(x + 1 + i, top, x + 2 + i, y + h - 1, area);
            lastTop = top;
        }
        // The current speed, as a line across the whole graph at the height the speed has now.
        g.fill(x + 1, lastTop, x + w - 1, lastTop + 1, skin.progressFill());
    }

    /* KDE 2 and 3, KIO's progress dialog. */
    private void kio(final GuiGraphics g, final Font font, final CopyRun run, final int x, final int y, final int w,
                     final int h, final int mouseX, final int mouseY) {
        int ly = y + PAD;
        Draw.text(g, font, GameText.resolve(CopyTexts.KIO_COPYING), x + PAD, ly, skin.text());
        ly += LINE + 2;
        final int value = x + PAD + 56;
        Draw.text(g, font, GameText.resolve(CopyTexts.SOURCE), x + PAD, ly, skin.text());
        text(g, font, GameText.resolve(CopyTexts.FILE_URL.with(run.current().from() + "/" + run.current().name())),
                value, ly, w - (value - x) - PAD, skin.text());
        ly += LINE;
        Draw.text(g, font, GameText.resolve(CopyTexts.DESTINATION), x + PAD, ly, skin.text());
        text(g, font, GameText.resolve(CopyTexts.FILE_URL.with(run.current().to() + "/" + run.current().name())),
                value, ly, w - (value - x) - PAD, skin.text());
        ly += LINE + 2;
        final int done = run.items() - run.itemsLeft() + (run.itemsLeft() > 0 ? 1 : 0);
        Draw.text(g, font, GameText.resolve(CopyTexts.FILES_OF.with(Math.min(run.items(), done), run.items())),
                x + PAD, ly, skin.text());
        final String mbOf = GameText.resolve(CopyTexts.MB_OF.with(mb(run.mbDone()), mb(run.mbTotal())));
        Draw.text(g, font, mbOf, x + w - PAD - font.width(mbOf), ly, skin.text());
        ly += LINE + 1;
        skin.field(g, x + PAD, ly, w - 2 * PAD, 10, false);
        final int fill = (int) Math.floor((w - 2 * PAD - 2) * run.fraction());
        g.fill(x + PAD + 1, ly + 1, x + PAD + 1 + fill, ly + 9, skin.progressFill());
        final String pct = percent(run) + "%";
        Draw.textCentered(g, font, pct, x + w / 2, ly + 1, skin.text());
        ly += 13;
        final String remaining = String.format(Locale.ROOT, "00:%02d:%02d", run.secondsLeft() / 60,
                run.secondsLeft() % 60);
        Draw.text(g, font, GameText.resolve(CopyTexts.RATE_REMAINING.with(mb(run.rate()), remaining)), x + PAD, ly,
                skin.text());
        ly += LINE + 1;
        skin.field(g, x + PAD, ly, 8, 8, false);
        if (keepOpen) {
            g.fill(x + PAD + 2, ly + 2, x + PAD + 6, ly + 6, skin.text());
        }
        keepAt = new int[] {x + PAD, ly, w - 2 * PAD, 9};
        text(g, font, GameText.resolve(CopyTexts.KEEP_OPEN), x + PAD + 11, ly, w - 2 * PAD - 11, skin.text());
        final int by = y + h - PAD - BUTTON_H;
        final int openFileW = 50;
        final int openDestW = 72;
        skin.button(g, font, x + w - PAD - BUTTON_W - 4 - openDestW - 4 - openFileW, by, openFileW, BUTTON_H,
                GameText.resolve(CopyTexts.OPEN_FILE), false, false, false);
        skin.button(g, font, x + w - PAD - BUTTON_W - 4 - openDestW, by, openDestW, BUTTON_H,
                GameText.resolve(CopyTexts.OPEN_DESTINATION), false, false, false);
        cancelButton(g, font, x + w - PAD - BUTTON_W, by, mouseX, mouseY);
    }

    /* GNOME 1, gmc's small dialog: where from, where to, the bar and Cancel. */
    private void gmc(final GuiGraphics g, final Font font, final CopyRun run, final int x, final int y, final int w,
                     final int h, final int mouseX, final int mouseY) {
        int ly = y + PAD;
        final int value = x + PAD + 62;
        Draw.text(g, font, GameText.resolve(CopyTexts.COPYING_FROM), x + PAD, ly, skin.text());
        text(g, font, "/" + run.current().from() + "/" + run.current().name(), value, ly, w - (value - x) - PAD,
                skin.text());
        ly += LINE;
        Draw.text(g, font, GameText.resolve(CopyTexts.TO), x + PAD, ly, skin.text());
        text(g, font, "/" + run.current().to(), value, ly, w - (value - x) - PAD, skin.text());
        ly += LINE + 3;
        skin.field(g, x + PAD, ly, w - 2 * PAD, 8, false);
        g.fill(x + PAD + 1, ly + 1, x + PAD + 1 + (int) Math.floor((w - 2 * PAD - 2) * run.fraction()), ly + 7,
                skin.progressFill());
        cancelButton(g, font, x + w - PAD - BUTTON_W, y + h - PAD - BUTTON_H, mouseX, mouseY);
    }

    /* Cinnamon, Nemo's File Operations: the file's mark, what is copied, the bar, the figures, and the cancel mark. */
    private void nemo(final GuiGraphics g, final Font font, final CopyRun run, final int x, final int y,
                      final int w) {
        final int ly = y + PAD + 2;
        g.fill(x + PAD, ly + 2, x + PAD + 14, ly + 16, skin.progressFill());
        final int tx = x + PAD + 20;
        final int tw = w - (tx - x) - PAD - 12;
        final String what = run.deleting() ? GameText.resolve(CopyTexts.DELETING_QUOTED.with(run.current().name()))
                : GameText.resolve(CopyTexts.COPYING_QUOTED.with(run.current().name(), run.current().to()));
        text(g, font, what, tx, ly, tw, skin.text());
        g.fill(tx, ly + LINE + 1, tx + tw, ly + LINE + 4, skin.listHover());
        g.fill(tx, ly + LINE + 1, tx + (int) Math.floor(tw * run.fraction()), ly + LINE + 4, skin.progressFill());
        text(g, font, GameText.resolve(CopyTexts.PROGRESS_LINE.with(mb(run.mbDone()), mb(run.mbTotal()),
                Math.max(1, run.secondsLeft()), mb(run.rate()))), tx, ly + LINE + 7, tw, skin.dim());
        final int cx = x + w - PAD - 8;
        Draw.text(g, font, "x", cx + 2, ly + 4, skin.text());
        cancelAt = new int[] {cx, ly + 3, 9, 9};
    }

    private void cancelButton(final GuiGraphics g, final Font font, final int x, final int y, final int mouseX,
                              final int mouseY) {
        cancelAt = new int[] {x, y, BUTTON_W, BUTTON_H};
        skin.button(g, font, x, y, BUTTON_W, BUTTON_H, GameText.resolve(CopyTexts.CANCEL),
                inside(cancelAt, mouseX, mouseY), false, false);
    }

    /* A line of text cut to the room it has. */
    private static void text(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                             final int room, final int colour) {
        Draw.text(g, font, font.width(text) <= room ? text : font.plainSubstrByWidth(text, Math.max(0, room)), x, y,
                colour);
    }

    private static boolean inside(final int[] r, final double mx, final double my) {
        return r[2] > 0 && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    /** The copy window shapes, by the systems that had a window for a copy. */
    enum Style {
        FRAMES_95, FRAMES_XP, FRAMES_11, KDE2, GNOME1, CINNAMON
    }
}
