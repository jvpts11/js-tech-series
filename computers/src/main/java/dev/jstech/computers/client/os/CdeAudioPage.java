/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.audio.SoundOutput;
import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.computers.gui.layout.CdeStyleLayout;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * The Audio page of CDE's Style Manager, where a CDE desktop keeps what the others put on their panel: a scale for
 * the volume with its value over the slider, a Mute toggle, and the two outputs as toggles of their own, which may
 * both be on, the way a workstation's audio control let a monitor and speakers play together. One of them always
 * stays on: the last is not let go.
 *
 * <p>Nothing is sent while the page is worked: OK sets the machine to it and closes, Cancel leaves the machine as it
 * was.
 */
final class CdeAudioPage implements IDesktopApp {

    /** What the Style Manager is told when the page goes, so it opens a fresh one the next time. */
    private final Runnable gone;
    private int volume;
    private boolean muted;
    private boolean monitor;
    private boolean speakers;
    private boolean dragging;

    private OsSkin skin;
    private int left;
    private int top;

    /** The width of the scale's slider. */
    private static final int SLIDER_W = 16;

    CdeAudioPage(final int volume, final boolean muted, final SoundOutput output, final Runnable gone) {
        this.volume = volume;
        this.muted = muted;
        this.monitor = output != SoundOutput.SPEAKERS;
        this.speakers = output != SoundOutput.MONITOR;
        this.gone = gone;
    }

    /** The middle of a part of the page, in desktop pixels: {@code mute}, {@code monitor}, {@code speakers},
     *  {@code ok}, {@code cancel}, or {@code scale} at the volume {@code value}; null for anything else. */
    @Nullable
    int[] partCentre(final String part, final int value) {
        final Rect rect = switch (part) {
            case "mute" -> CdeStyleLayout.audioMute();
            case "monitor" -> CdeStyleLayout.audioOutput(0);
            case "speakers" -> CdeStyleLayout.audioOutput(1);
            case "ok" -> CdeStyleLayout.audioButton(0);
            case "cancel" -> CdeStyleLayout.audioButton(1);
            default -> null;
        };
        if ("scale".equals(part)) {
            final Rect scale = CdeStyleLayout.audioScale();
            return new int[] {this.left + sliderX(scale, value) + SLIDER_W / 2, this.top + scale.y() + scale.h() / 2};
        }
        return rect == null ? null : CdeStylePages.centre(rect, this.left, this.top);
    }

    /** The volume, the mute and the output the page stands at, as the player has worked it. */
    int volume() {
        return this.volume;
    }

    boolean muted() {
        return this.muted;
    }

    SoundOutput output() {
        return this.monitor && this.speakers ? SoundOutput.BOTH
                : this.monitor ? SoundOutput.MONITOR : SoundOutput.SPEAKERS;
    }

    @Override
    public String title() {
        return GameText.resolve(CdeStyleTitles.AUDIO_PAGE);
    }

    @Override
    public int defaultWidth() {
        return CdeStyleLayout.AUDIO_W + CdeStyleLayout.FRAME_W;
    }

    @Override
    public int defaultHeight() {
        return CdeStyleLayout.AUDIO_H + CdeStyleLayout.FRAME_H;
    }

    @Override
    public int minWidth() {
        return defaultWidth();
    }

    @Override
    public int minHeight() {
        return defaultHeight();
    }

    @Override
    public void applySkin(final OsSkin skin) {
        this.skin = skin;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h, final int mouseX, final int mouseY, final float partialTick) {
        this.left = x;
        this.top = y;
        final DesktopScreen desktop = DesktopScreen.current();
        if (this.skin == null || desktop == null) {
            return;
        }
        final CdePalette p = desktop.prefs().cdePalette();
        final int ground = p.window();
        final Rect volumeLabel = CdeStyleLayout.audioVolumeLabel();
        Draw.text(g, font, words(StyleManagerTexts.VOLUME), x + volumeLabel.x(), y + volumeLabel.y(), this.skin.text(),
                ground);
        final Rect scale = CdeStyleLayout.audioScale();
        MotifChrome.sunken(g, x + scale.x(), y + scale.y(), scale.w(), scale.h(), p.inset(), p);
        final int sx = x + sliderX(scale, this.volume);
        MotifChrome.raised(g, sx, y + scale.y() + 1, SLIDER_W, scale.h() - 2, p.window(), p);
        g.fill(sx + SLIDER_W / 2 - 1, y + scale.y() + 2, sx + SLIDER_W / 2, y + scale.y() + scale.h() - 2, p.shade());
        g.fill(sx + SLIDER_W / 2, y + scale.y() + 2, sx + SLIDER_W / 2 + 1, y + scale.y() + scale.h() - 2, p.light());
        final String value = String.valueOf(this.volume);
        Draw.text(g, font, value, sx + (SLIDER_W - font.width(value)) / 2, y + scale.y() - 10, this.skin.text(),
                ground);
        toggle(g, font, p, CdeStyleLayout.audioMute(), StyleManagerTexts.MUTE, this.muted);
        final Rect outputLabel = CdeStyleLayout.audioOutputLabel();
        Draw.text(g, font, words(StyleManagerTexts.OUTPUT), x + outputLabel.x(), y + outputLabel.y(),
                this.skin.text(), ground);
        toggle(g, font, p, CdeStyleLayout.audioOutput(0), StyleManagerTexts.MONITOR, this.monitor);
        toggle(g, font, p, CdeStyleLayout.audioOutput(1), StyleManagerTexts.SPEAKERS, this.speakers);
        final int rule = y + CdeStyleLayout.audioRule();
        g.fill(x + 4, rule, x + CdeStyleLayout.AUDIO_W - 4, rule + 1, p.shade());
        g.fill(x + 4, rule + 1, x + CdeStyleLayout.AUDIO_W - 4, rule + 2, p.light());
        for (int i = 0; i < 2; i++) {
            final Rect b = CdeStyleLayout.audioButton(i);
            this.skin.button(g, font, x + b.x(), y + b.y(), b.w(), b.h(), words(i == 0 ? CdeTexts.OK : CdeTexts.CANCEL),
                    b.holds(mouseX - x, mouseY - y), false, i == 0);
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (button != 0) {
            return;
        }
        final double px = mouseX - this.left;
        final double py = mouseY - this.top;
        final Rect scale = CdeStyleLayout.audioScale();
        if (px >= scale.x() && px < scale.x() + scale.w() && py >= scale.y() - 11 && py < scale.y() + scale.h()) {
            this.dragging = true;
            this.volume = volumeAt(scale, px);
            return;
        }
        if (CdeStyleLayout.audioMute().holds(px, py)) {
            this.muted = !this.muted;
            return;
        }
        // One output always stays on: letting go of the last would leave the sound nowhere to go.
        if (CdeStyleLayout.audioOutput(0).holds(px, py) && (!this.monitor || this.speakers)) {
            this.monitor = !this.monitor;
            return;
        }
        if (CdeStyleLayout.audioOutput(1).holds(px, py) && (!this.speakers || this.monitor)) {
            this.speakers = !this.speakers;
            return;
        }
        final int pressed = CdeStyleLayout.audioButtonAt(px, py);
        if (pressed == 0) {
            ok();
        } else if (pressed == 1) {
            DesktopScreen.closeDialog(this);
        }
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (this.dragging) {
            this.volume = volumeAt(CdeStyleLayout.audioScale(), mouseX - this.left);
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY,
                              final int button) {
        this.dragging = false;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> this.volume = Math.max(0, this.volume - 5);
            case GLFW.GLFW_KEY_RIGHT -> this.volume = Math.min(100, this.volume + 5);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> ok();
            case GLFW.GLFW_KEY_ESCAPE -> DesktopScreen.closeDialog(this);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Escape is this page's Cancel, so the desktop must not take it as the way out of the monitor. */
    @Override
    public boolean wantsEscape() {
        return true;
    }

    @Override
    public void onClosed() {
        this.gone.run();
    }

    private void ok() {
        final DesktopScreen desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.applySound(this.volume, this.muted, output());
        }
        DesktopScreen.closeDialog(this);
    }

    /* A toggle: its square, filled while it is on, and its words beside it. */
    private void toggle(final GuiGraphics g, final Font font, final CdePalette p, final Rect r, final TextKey label,
                        final boolean on) {
        final int bx = this.left + r.x();
        final int by = this.top + r.y() + (r.h() - CdeStyleLayout.TOGGLE) / 2;
        if (on) {
            MotifChrome.sunken(g, bx, by, CdeStyleLayout.TOGGLE, CdeStyleLayout.TOGGLE, p.active(), p);
        } else {
            MotifChrome.raised(g, bx, by, CdeStyleLayout.TOGGLE, CdeStyleLayout.TOGGLE, p.window(), p);
        }
        Draw.text(g, font, words(label), bx + CdeStyleLayout.TOGGLE + CdeStyleLayout.TOGGLE_GAP,
                this.top + r.y() + (r.h() - 8) / 2 + 1, this.skin.text(), p.window());
    }

    /* The slider's left edge, relative to the page, for a volume. */
    private static int sliderX(final Rect scale, final int volume) {
        return scale.x() + 1 + Math.round(volume * (scale.w() - 2 - SLIDER_W) / 100.0F);
    }

    /* The volume a point across the scale stands for, the slider's middle being where it points. */
    private static int volumeAt(final Rect scale, final double px) {
        final double at = (px - scale.x() - 1 - SLIDER_W / 2.0) / (scale.w() - 2 - SLIDER_W);
        return (int) Math.max(0, Math.min(100, Math.round(at * 100.0)));
    }

    private static String words(final TextKey key) {
        return GameText.resolve(key);
    }
}
