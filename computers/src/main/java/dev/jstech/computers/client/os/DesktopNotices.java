/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.audio.SystemSound;
import dev.jstech.computers.operation.payload.MachineSoundPayload;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.gui.layout.DesktopZ;
import dev.jstech.core.text.GameText;
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
 * and goes away on its own, taking nothing over.
 */
final class DesktopNotices {

    private final DesktopState desktop;
    /** The dialog over the desktop, or null when none is up. */
    @Nullable
    private Popup popup;
    @Nullable
    private Balloon balloon;

    /** How long a balloon stays up before it fades away, in milliseconds. */
    private static final long BALLOON_MS = 9_000L;
    private static final int BALLOON_W = 152;

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

    /**
     * The same, for a notice that is also an invitation: clicking it opens the program it is about. Which is how a
     * machine of one edition said hello on its first start: one sentence from the corner, and the offer left open for
     * as long as the sentence was up.
     *
     * @param opens the window key of the program a click on it opens, or empty
     */
    void showBalloon(final String title, final String body, final String opens) {
        this.balloon = new Balloon(title, body, System.currentTimeMillis() + BALLOON_MS, opens);
        PacketDistributor.sendToServer(new MachineSoundPayload(desktop.hostPos(), SystemSound.NOTIFY));
    }

    /** Whether a balloon is up. */
    boolean balloonUp() {
        return balloon != null;
    }

    /** Puts the balloon away. */
    void dismissBalloon() {
        this.balloon = null;
    }

    /** The classic notification balloon: pale yellow, a blue "i", a title, a line or two, and a close box. */
    void renderBalloon(final GuiGraphics g, final int tbY, final int sw) {
        if (balloon != null && System.currentTimeMillis() > balloon.until()) {
            balloon = null;
        }
        final int[] r = balloonRect(tbY, sw);
        if (r == null || balloon == null) {
            return;
        }
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
        Draw.text(g, font, Component.literal(balloon.title()).withStyle(ChatFormatting.BOLD),
                x + 18, y + 5, c.balloonTitle());
        int ly = y + 16;
        final List<FormattedCharSequence> lines = font.split(Component.literal(balloon.body()), w - 12);
        for (final FormattedCharSequence line : lines) {
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
     * A click on the desktop, asked of the balloon first. The close box only puts it away; anywhere else on a balloon
     * that carries an offer takes it up, which is what made those balloons worth clicking rather than dismissing.
     *
     * @return null when the click missed the balloon; else the key of the program to open, or empty for none
     */
    @Nullable
    String clickBalloon(final double mx, final double my, final int tbY, final int sw) {
        final int[] r = balloonRect(tbY, sw);
        if (r == null || balloon == null) {
            return null;
        }
        if (mx < r[0] || mx > r[0] + r[2] || my < r[1] || my > r[1] + r[3]) {
            return null;
        }
        final String opens = balloon.opens();
        final boolean onClose = mx >= r[0] + r[2] - 14;
        balloon = null;
        return onClose ? "" : opens;
    }

    /** The balloon's box in desktop-local coordinates, or null when none is up. Draw and hit-test share it. */
    @Nullable
    private int[] balloonRect(final int tbY, final int sw) {
        if (balloon == null) {
            return null;
        }
        final int lines = desktop.textFont().split(Component.literal(balloon.body()), BALLOON_W - 12).size();
        final int h = 15 + lines * 9 + 5;
        final int x = Math.max(4, sw - BALLOON_W - 6);
        return new int[] {x, tbY - h - 7, BALLOON_W, h};
    }

    /**
     * A notice from the system itself: it rises over the notification area and goes away on its own.
     *
     * @param opens the program a click on it opens, or empty when clicking it only puts it away
     */
    private record Balloon(String title, String body, long until, String opens) {
    }
}
