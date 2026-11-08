/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.audio.SystemSound;
import dev.jstech.computers.operation.payload.MachineSoundPayload;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.power.UpsTexts;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.motion.FadeLayer;
import dev.jstech.core.gui.layout.DesktopZ;
import dev.jstech.core.motion.Motion;
import dev.jstech.core.motion.MotionKinds;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * What the desktop tells the player, in the two ways it can: a dialog over everything that has to be answered or put
 * away (an error, a question, the Open with chooser), and a balloon over the notification area that says one thing
 * and goes away on its own, taking nothing over. On Frames 10 the balloon is the toast in the corner, and every
 * notice is also kept for the Action Center.
 *
 * <p>The toast's colours are {@code jsc:desktop/toast}, Plasma's notice's {@code jsc:desktop/plasma_notice}.
 */
@PaletteHolder
final class DesktopNotices {

    private final DesktopState desktop;
    /** The dialog over the desktop, or null when none is up. */
    @Nullable
    private Popup popup;
    @Nullable
    private Balloon balloon;
    /** The balloon up now coming in, on a system whose balloons fade in. */
    private Motion balloonIn = Motion.FINISHED;
    /** A balloon put away, still drawn while it fades out, and its fading. */
    @Nullable
    private Balloon leaving;
    private Motion balloonOut = Motion.FINISHED;
    /** Every notice raised since the desktop opened, newest first, which Frames 10's Action Center lists. */
    private final List<Notice> history = new ArrayList<>();
    /** How many of those came since the Action Center was last opened. */
    private int unread;
    /** Frames 10's Quiet hours: on, a notice goes to the Action Center without popping up or sounding. */
    private boolean quiet;

    /** How long a balloon stays up before it fades away, in milliseconds. */
    private static final long BALLOON_MS = 9_000L;
    private static final int BALLOON_W = 152;
    /** Frames 10's notice, the toast in the corner, and how many notices the Action Center keeps. */
    private static final int TOAST_W = 168;
    private static final int HISTORY_MAX = 12;
    private static final Palette<Toast> TOAST = Palettes.declare(JsComputers.MODID, "desktop/toast",
            new Toast(0xF21F1F1F, 0xFF2B2B2B, 0xFFFFFFFF, 0xFFBBBBBB, 0xFF0078D7, 0xFFF0B23A));
    /** Plasma's notice: the card, its edge, the title, the words, the dim words, the bar's track and its fill. */
    private static final Palette<PlasmaCard> PLASMA = Palettes.declare(JsComputers.MODID, "desktop/plasma_notice",
            new PlasmaCard(0xFFFCFCFC, 0xFFBDC3C7, 0xFF232629, 0xFF31363B, 0xFF7F8C8D, 0xFFD6D9DC, 0xFFF67400));
    /** Plasma's bar under a UPS's notice, and the room it takes with its words. */
    private static final int BAR_H = 3;
    private static final int BAR_ROOM = 4 + BAR_H + 3 + 9;

    DesktopNotices(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** The dialog up over the desktop, or null when none is. */
    @Nullable
    Popup popup() {
        return popup;
    }

    /** Puts a dialog over the desktop, in place of any that was up. */
    void show(final Popup dialog) {
        this.popup = dialog;
    }

    /** Puts the dialog away. */
    void dismissPopup() {
        this.popup = null;
    }

    /** Whether a dialog is up; while one is, it takes every click, key and wheel turn. */
    boolean popupUp() {
        return popup != null;
    }

    /**
     * A click at a desktop-local point while a dialog is up: only its own buttons answer it, and a click beside it
     * rings the bell. Returns whether a dialog was up to take it.
     */
    boolean clickPopup(final double x, final double y, final int button) {
        if (popup == null) {
            return false;
        }
        if (!popup.contains(x, y)) {
            PacketDistributor.sendToServer(new MachineSoundPayload(desktop.hostPos(), SystemSound.BEEP));
        }
        popup.mouseClicked(x, y, button);
        if (!popup.isOpen()) {
            popup = null;
        }
        return true;
    }

    /** A button let go at a desktop-local point while a dialog is up; returns whether one was up to take it. */
    boolean releasePopup(final double x, final double y, final int button) {
        if (popup == null) {
            return false;
        }
        popup.mouseReleased(x, y, button);
        return true;
    }

    /** A key while a dialog is up: Enter or Escape answers it, and nothing leaks behind it. */
    boolean keyPopup(final int key, final int scanCode, final int modifiers) {
        if (popup == null) {
            return false;
        }
        popup.keyPressed(key, scanCode, modifiers);
        if (!popup.isOpen()) {
            popup = null;
        }
        return true;
    }

    /** Opens a modal error dialog with that title and message over the desktop, and the machine sounds it. */
    void showError(final String title, final String message) {
        this.popup = new DesktopPopup(title, message, desktop.textFont());
        PacketDistributor.sendToServer(new MachineSoundPayload(desktop.hostPos(), SystemSound.ERROR));
    }

    /**
     * The error for a {@code .dat} touched by hand. A {@code .dat} is a read-only projection of the computer's stored
     * items, so the only sanctioned way to move those items is the Network Interactor.
     */
    void datLocked() {
        showError(GameText.resolve(DesktopTexts.ERROR), GameText.resolve(DesktopTexts.DAT_LOCKED));
    }

    /** Asks a question over the whole desktop, running {@code yes} only when the answer is Yes. */
    void ask(final String title, final String message, @Nullable final Runnable yes) {
        this.popup = new QuestionPopup(title, message, desktop.textFont(), yes);
    }

    /** The question or note up over the desktop, or null while none is. */
    @Nullable
    QuestionPopup question() {
        return popup instanceof QuestionPopup q && q.isOpen() ? q : null;
    }

    /** The Open with chooser up over the desktop, or null while none is. */
    @Nullable
    OpenWithPopup chooser() {
        return popup instanceof OpenWithPopup chooser && chooser.isOpen() ? chooser : null;
    }

    /**
     * Draws the dialog at the height every dialog rides at, over a desktop {@code sw} by {@code sh}; one that closed
     * by its own choice rather than by a click the desktop saw, as Open with can, is put away first.
     */
    void renderPopup(final GuiGraphics g, final OsSkin skin, final int mouseX, final int mouseY, final int sw,
                     final int sh) {
        if (popup != null && !popup.isOpen()) {
            popup = null;
        }
        if (popup != null) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.POPUP);
            popup.renderIn(g, new UiContext(skin, desktop.textFont(), mouseX, mouseY, 0f), 0, 0, sw, sh);
            g.pose().popPose();
        }
    }

    /**
     * Raises a balloon. Unlike an error, it takes nothing over: the machine is telling the player something, not
     * asking them to answer, so the desktop stays usable underneath it.
     */
    void showBalloon(final String title, final String body) {
        showBalloon(title, body, "");
    }

    /** The heading of the balloon up now, or empty when there is none or it has gone. */
    String balloonTitle() {
        return balloon == null || System.currentTimeMillis() > balloon.until() ? "" : balloon.title();
    }

    /** The words of the balloon up now, or empty when there is none or it has gone. */
    String balloonBody() {
        return balloon == null || System.currentTimeMillis() > balloon.until() ? "" : balloon.body();
    }

    /**
     * The same, for a notice that is also an invitation: clicking it opens the program it is about. Which is how a
     * machine of one edition said hello on its first start: one sentence from the corner, and the offer left open for
     * as long as the sentence was up.
     *
     * @param opens the window key of the program a click on it opens, or empty
     */
    void showBalloon(final String title, final String body, final String opens) {
        showBalloon(title, body, opens, null);
    }

    /** The same, from the machine's UPS: {@code battery} is shown under the words, as the desktop shows it. */
    void showBalloon(final String title, final String body, final String opens, @Nullable final Battery battery) {
        history.add(0, new Notice(title, body, opens, desktop.prefs().clockText()));
        if (history.size() > HISTORY_MAX) {
            history.remove(history.size() - 1);
        }
        unread++;
        if (quiet) {
            return;
        }
        this.balloon = new Balloon(title, body, System.currentTimeMillis() + BALLOON_MS, opens, battery);
        this.balloonIn = desktop.motion().start(MotionKinds.NOTICE_SHOW);
        this.leaving = null;
        PacketDistributor.sendToServer(new MachineSoundPayload(desktop.hostPos(), SystemSound.NOTIFY));
    }

    /** The notices raised since the desktop opened, newest first. */
    List<Notice> history() {
        return Collections.unmodifiableList(history);
    }

    /** How many notices came since the Action Center was last opened. */
    int unread() {
        return unread;
    }

    /** The Action Center was opened: what it lists has been seen. */
    void markRead() {
        unread = 0;
    }

    /** Clears the Action Center's list, and the one up in the corner with it. */
    void clearAll() {
        history.clear();
        unread = 0;
        letGo();
    }

    /** Takes one notice off the Action Center's list. */
    void forget(final Notice notice) {
        history.remove(notice);
    }

    /** Whether Quiet hours is on. */
    boolean quiet() {
        return quiet;
    }

    /** Turns Quiet hours on or off. */
    void toggleQuiet() {
        quiet = !quiet;
    }

    /** Whether a balloon is up. */
    boolean balloonUp() {
        return balloon != null;
    }

    /** Whether a balloon is on the glass: one up, or one still fading out. */
    boolean balloonDrawn() {
        return balloon != null || leaving != null;
    }

    /** Puts the balloon away. */
    void dismissBalloon() {
        letGo();
    }

    /**
     * The classic notification balloon: pale yellow, a blue "i", a title, a line or two, and a close box; faded in
     * and out on a system whose balloons fade.
     */
    void renderBalloon(final GuiGraphics g, final int tbY, final int sw) {
        if (balloon != null && System.currentTimeMillis() > balloon.until()) {
            letGo();
        }
        final double now = DesktopMotion.now();
        if (balloon != null) {
            final Balloon up = balloon;
            FadeLayer.draw(g, (float) balloonIn.opacity(now), () -> paintBalloon(g, up, tbY, sw));
            return;
        }
        if (leaving != null) {
            if (balloonOut.done(now)) {
                leaving = null;
                return;
            }
            final Balloon going = leaving;
            FadeLayer.draw(g, (float) balloonOut.opacity(now), () -> paintBalloon(g, going, tbY, sw));
        }
    }

    /* The balloon put away: gone at once, or drawn fading out where the system fades its balloons away. */
    private void letGo() {
        if (balloon != null) {
            final Motion going = desktop.motion().start(MotionKinds.NOTICE_HIDE);
            if (!going.done(DesktopMotion.now())) {
                leaving = balloon;
                balloonOut = going;
            }
        }
        balloon = null;
    }

    private void paintBalloon(final GuiGraphics g, final Balloon shown, final int tbY, final int sw) {
        if (desktop.panelStyle() == PanelStyle.FRAMES_10) {
            paintToast(g, shown, tbY, sw);
            return;
        }
        if (plasma()) {
            paintPlasma(g, shown, tbY, sw);
            return;
        }
        final int[] r = balloonRect(shown, tbY, sw);
        final Font font = desktop.textFont();
        final int x = r[0];
        final int y = r[1];
        final int w = r[2];
        final int h = r[3];
        final DesktopShellPalette.Colours c = DesktopShellPalette.get();
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, c.balloonBorder());
        g.fill(x, y, x + w, y + h, c.balloonFill());
        /*
         * The tail, pointing down at the notification area it came from: a bordered wedge, drawn as an outline
         * first and the pale fill inset into it, so it carries the same 1px edge as the box.
         */
        final int tail = x + w - 42;
        for (int i = 0; i < 7; i++) {
            g.fill(tail + i - 1, y + h + i, tail + 14 - i, y + h + i + 1, c.balloonBorder());
        }
        for (int i = 0; i < 6; i++) {
            g.fill(tail + i, y + h + i, tail + 12 - i, y + h + i + 1, c.balloonFill());
        }
        g.fill(tail, y + h - 1, tail + 12, y + h, c.balloonFill()); // the tail opens into the balloon
        // The round blue "i" and the title beside it.
        g.fill(x + 6, y + 5, x + 14, y + 13, c.balloonIcon());
        g.fill(x + 7, y + 4, x + 13, y + 14, c.balloonIcon());
        g.fill(x + 9, y + 6, x + 11, y + 7, c.balloonIconMark());
        g.fill(x + 9, y + 8, x + 11, y + 12, c.balloonIconMark());
        Draw.text(g, font, Component.literal(shown.title()).withStyle(ChatFormatting.BOLD),
                x + 18, y + 5, c.balloonTitle());
        int ly = y + 16;
        for (final FormattedCharSequence line : words(shown, w - 12)) {
            Draw.text(g, font, line, x + 6, ly, c.balloonBody());
            ly += 9;
        }
        // The close box, the one part of a balloon anyone ever clicked.
        final int bx = x + w - 12;
        g.fill(bx, y + 4, bx + 8, y + 12, c.balloonCloseFill());
        desktop.drawOutline(g, bx, y + 4, 8, 8, c.balloonCloseEdge());
        Draw.text(g, font, "x", bx + 2, y + 5, c.balloonBody());
    }

    /**
     * Frames 10's notice: a dark card in the corner over the taskbar, the program's mark, its name over the words, and
     * a cross that appears under the cursor; no tail, since it points at nothing but the corner it came from. The UPS's
     * mark is its battery.
     */
    private void paintToast(final GuiGraphics g, final Balloon shown, final int tbY, final int sw) {
        final int[] r = balloonRect(shown, tbY, sw);
        final Font font = desktop.textFont();
        final Toast c = TOAST.get();
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], c.card());
        Draw.outline(g, r[0], r[1], r[2], r[3], c.edge());
        if (shown.battery() != null) {
            BatteryGlyph.draw(g, r[0] + 4, r[1] + 7, 14, 8, shown.battery().percent(), c.title(), c.battery());
        } else {
            g.fill(r[0] + 6, r[1] + 6, r[0] + 16, r[1] + 16, c.mark());
        }
        Draw.text(g, font, font.plainSubstrByWidth(shown.title(), r[2] - 34), r[0] + 22, r[1] + 6, c.title());
        int ly = r[1] + 18;
        for (final FormattedCharSequence line : words(shown, r[2] - 28)) {
            Draw.text(g, font, line, r[0] + 22, ly, c.body());
            ly += 9;
        }
        Draw.text(g, font, "x", r[0] + r[2] - 9, r[1] + 4, c.body());
    }

    /**
     * Plasma's notice: a light card over the panel's corner, the program's mark and name over the words, and its cross.
     * The UPS's notice carries the battery as Plasma's power manager drew it: a bar, how full under its start and how
     * long it lasts under its end.
     */
    private void paintPlasma(final GuiGraphics g, final Balloon shown, final int tbY, final int sw) {
        final int[] r = balloonRect(shown, tbY, sw);
        final Font font = desktop.textFont();
        final PlasmaCard c = PLASMA.get();
        final int x = r[0];
        final int y = r[1];
        final int w = r[2];
        g.fill(x, y, x + w, y + r[3], c.card());
        Draw.outline(g, x, y, w, r[3], c.edge());
        final Battery battery = shown.battery();
        if (battery != null) {
            BatteryGlyph.draw(g, x + 5, y + 6, 10, 6, battery.percent(), c.title(), c.barFill());
        }
        Draw.text(g, font, font.plainSubstrByWidth(shown.title(), w - 34), x + 18, y + 5, c.title());
        Draw.text(g, font, "x", x + w - 9, y + 4, c.dim());
        int ly = y + 17;
        for (final FormattedCharSequence line : words(shown, w - 12)) {
            Draw.text(g, font, line, x + 6, ly, c.body());
            ly += 9;
        }
        if (battery == null) {
            return;
        }
        final int barX = x + 6;
        final int barW = w - 12;
        final int barY = ly + 3;
        g.fill(barX, barY, barX + barW, barY + BAR_H, c.barTrack());
        g.fill(barX, barY, barX + barW * battery.percent() / 100, barY + BAR_H, c.barFill());
        final int textY = barY + BAR_H + 3;
        Draw.text(g, font, GameText.resolve(UpsTexts.PERCENT.with(battery.percent())), barX, textY, c.body());
        if (battery.minutes() > 0L) {
            final String left = GameText.resolve(UpsTexts.MINUTES_LEFT.with(battery.minutes()));
            Draw.text(g, font, left, barX + barW - font.width(left), textY, c.dim());
        }
    }

    /* Whether this desktop raises Plasma's notices: KDE's own desktop of today, not its period one. */
    private boolean plasma() {
        return desktop.panelStyle() == PanelStyle.KDE && !desktop.periodPanel();
    }

    /*
     * The words of a notice, wrapped to {@code width}: what it says, and under it, where the desktop shows it as words,
     * the UPS's battery; Frames adds when the system shuts itself down. Plasma shows the battery as its bar instead.
     */
    private List<FormattedCharSequence> words(final Balloon shown, final int width) {
        final Font font = desktop.textFont();
        final List<FormattedCharSequence> lines = new ArrayList<>(font.split(Component.literal(shown.body()), width));
        final Battery battery = shown.battery();
        if (battery == null || plasma()) {
            return lines;
        }
        final String charge = GameText.resolve(battery.minutes() > 0L
                ? UpsTexts.PERCENT_MINUTES_LEFT.with(battery.percent(), battery.minutes())
                : UpsTexts.PERCENT.with(battery.percent()));
        lines.addAll(font.split(Component.literal(charge), width));
        if (!desktop.panelStyle().unixLike()) {
            lines.addAll(font.split(Component.literal(GameText.resolve(UpsTexts.SHUTS_DOWN)), width));
        }
        return lines;
    }

    /**
     * A click on the desktop, asked of the balloon first. The close box only puts it away; anywhere else on a balloon
     * that carries an offer takes it up, which is what made those balloons worth clicking rather than dismissing.
     *
     * @return null when the click missed the balloon; else the key of the program to open, or empty for none
     */
    @Nullable
    String clickBalloon(final double mx, final double my, final int tbY, final int sw) {
        if (balloon == null) {
            return null;
        }
        final int[] r = balloonRect(balloon, tbY, sw);
        if (mx < r[0] || mx > r[0] + r[2] || my < r[1] || my > r[1] + r[3]) {
            return null;
        }
        final String opens = balloon.opens();
        final boolean onClose = mx >= r[0] + r[2] - 14;
        letGo();
        return onClose ? "" : opens;
    }

    /** The balloon's box in desktop-local coordinates, or null when none is up. Draw and hit-test share it. */
    @Nullable
    private int[] balloonRect(final Balloon shown, final int tbY, final int sw) {
        if (desktop.panelStyle() == PanelStyle.FRAMES_10) {
            final int lines = words(shown, TOAST_W - 28).size();
            final int h = 18 + lines * 9 + 5;
            return new int[] {Math.max(4, sw - TOAST_W - 4), tbY - h - 4, TOAST_W, h};
        }
        if (plasma()) {
            final int lines = words(shown, TOAST_W - 12).size();
            final int h = 17 + lines * 9 + (shown.battery() == null ? 0 : BAR_ROOM) + 5;
            return new int[] {Math.max(4, sw - TOAST_W - 4), tbY - h - 4, TOAST_W, h};
        }
        final int lines = words(shown, BALLOON_W - 12).size();
        final int h = 15 + lines * 9 + 5;
        final int x = Math.max(4, sw - BALLOON_W - 6);
        return new int[] {x, tbY - h - 7, BALLOON_W, h};
    }

    /**
     * A notice from the system itself: it rises over the notification area and goes away on its own.
     *
     * @param opens the program a click on it opens, or empty when clicking it only puts it away
     */
    private record Balloon(String title, String body, long until, String opens, @Nullable Battery battery) {
    }

    /**
     * The battery of the machine's UPS, under a notice the UPS raised.
     *
     * @param percent how full it is
     * @param minutes how long it lasts at the load, in whole minutes
     */
    record Battery(int percent, long minutes) {
    }

    /**
     * A notice as the Action Center keeps it: what it said, the program a click on it opens, and the time on the
     * clock when it came.
     */
    record Notice(String title, String body, String opens, String time) {
    }

    /**
     * Frames 10's toast: its card and edge, the program's mark, the name and the words under it, and the charge of the
     * battery a UPS's notice carries as its mark.
     */
    private record Toast(int card, int edge, int title, int body, int mark, int battery) {
    }

    /** Plasma's notice: its card and edge, the name, the words, the dim words, and the bar's track and fill. */
    private record PlasmaCard(int card, int edge, int title, int body, int dim, int barTrack, int barFill) {
    }
}
