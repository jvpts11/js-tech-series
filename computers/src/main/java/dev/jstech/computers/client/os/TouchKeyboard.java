/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.TouchKeyboardLayout;
import dev.jstech.computers.gui.layout.TouchKeyboardLayout.Box;
import dev.jstech.computers.gui.layout.TouchKeyboardLayout.Key;
import dev.jstech.computers.gui.layout.TouchKeyboardLayout.Kind;
import dev.jstech.computers.gui.layout.TouchKeyboardLayout.Layer;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.gui.layout.DesktopZ;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The touch keyboard of Frames 10 and Frames 11, on the desktop of a touch monitor (computers/docs/design/
 * peripherals.md): it comes up while what is in front takes typing, Frames 10's docked along the foot of the desktop
 * and Frames 11's floating in the middle, and goes away with Hide or its close box until a field is touched again. A
 * touched key types into the desktop as the keyboard's would; the key under the finger is lit, and a ring shows a
 * moment where the touch landed. The other systems have none, and take what is typed through the typing line alone.
 */
@PaletteHolder
final class TouchKeyboard {

    private final DesktopState desktop;
    private Layer layer = Layer.LETTERS;
    /** Whether the next letter comes in capitals. */
    private boolean shift;
    /** Whether the next key goes with Control held. */
    private boolean ctrl;
    /** Put away until a field is touched again. */
    private boolean hidden;
    /** Where the last touch landed, desktop-local, and when, on the motion clock. */
    private double ringX;
    private double ringY;
    private double ringAt = Double.NEGATIVE_INFINITY;

    /** The keyboard of each system, as it draws it. */
    private static final Palette<Look> FRAMES_10 = Palettes.declare(JsComputers.MODID,
            "desktop/touch_keyboard_frames_10", new Look(0xff1f1f1f, 0xff1f1f1f, 0xff2b2b2b, 0xff2d2d2d, 0xff262626,
                    0xff0078d7, 0xffffffff, 0xff9a9a9a, 0xc8ffffff, 0x6effffff));
    private static final Palette<Look> FRAMES_11 = Palettes.declare(JsComputers.MODID,
            "desktop/touch_keyboard_frames_11", new Look(0xff202020, 0xff3a3a3a, 0xff202020, 0xff3a3a3a, 0xff2c2c2c,
                    0xff5b84f0, 0xffffffff, 0xffa0a0a0, 0xc8ffffff, 0x6effffff));
    /* How long the ring of a touch stays, in milliseconds. */
    private static final double RING_MS = 300.0;
    private static final int RING_INNER = 7;
    private static final int RING_OUTER = 9;
    /* Over the dialogs and whatever they lift, as the systems' own touch keyboards stood over everything. */
    private static final int Z = DesktopZ.POPUP + DesktopZ.DECORATION_LIFT;

    TouchKeyboard(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** Whether the keyboard is up: on a touch monitor, on a system that has one, with something taking typing. */
    boolean shown() {
        return desktop.surface().touched() && look() != null && !hidden && desktop.takesTyping();
    }

    /** Where it stands on the desktop, in desktop-local units. */
    Box box() {
        final DesktopViewport view = desktop.view();
        return desktop.panelStyle() == PanelStyle.FRAMES_10
                ? TouchKeyboardLayout.docked(view.width(), view.workAreaBottom())
                : TouchKeyboardLayout.floating(view.width(), view.workAreaBottom());
    }

    /** Brings a keyboard put away back for the next field touched. */
    void reveal() {
        hidden = false;
    }

    /**
     * A touch on the keyboard at a desktop-local point: the key's own work (Shift, the layer, Control, Hide) done here,
     * and what it types handed back for the desktop to type; empty for a touch that types nothing.
     */
    Optional<Typed> touch(final double x, final double y) {
        ringX = x;
        ringY = y;
        ringAt = DesktopMotion.now();
        final Box box = box();
        if (TouchKeyboardLayout.closeBox(box).contains(x, y)) {
            hidden = true;
            return Optional.empty();
        }
        final Optional<Key> touched = TouchKeyboardLayout.at(TouchKeyboardLayout.keys(layer, box), x, y);
        if (touched.isEmpty()) {
            return Optional.empty();
        }
        final Key key = touched.get();
        final int held = ctrl ? GLFW.GLFW_MOD_CONTROL : 0;
        final Typed typed = switch (key.kind()) {
            case CHAR -> charOf(key.character(), held);
            case SPACE -> held == 0 ? Typed.of(" ") : Typed.key(GLFW.GLFW_KEY_SPACE, held);
            case SMILE -> Typed.of(":)");
            case BACKSPACE -> Typed.key(GLFW.GLFW_KEY_BACKSPACE, held);
            case ENTER -> Typed.key(GLFW.GLFW_KEY_ENTER, held);
            case LEFT -> Typed.key(GLFW.GLFW_KEY_LEFT, held);
            case RIGHT -> Typed.key(GLFW.GLFW_KEY_RIGHT, held);
            case SHIFT -> {
                shift = !shift;
                yield null;
            }
            case LAYER -> {
                layer = layer == Layer.LETTERS ? Layer.SYMBOLS : Layer.LETTERS;
                yield null;
            }
            case CTRL -> {
                ctrl = !ctrl;
                yield null;
            }
            case HIDE -> {
                hidden = true;
                yield null;
            }
        };
        if (typed != null) {
            shift = false;
            ctrl = false;
        }
        return Optional.ofNullable(typed);
    }

    /** Draws the keyboard, with the key under the finger at ({@code lmx}, {@code lmy}) lit. */
    void paint(final GuiGraphics g, final int lmx, final int lmy) {
        final Look look = look();
        if (look == null) {
            return;
        }
        final Font font = desktop.textFont();
        final Box box = box();
        g.pose().pushPose();
        g.pose().translate(0, 0, Z);
        g.fill(box.x(), box.y(), box.x() + box.w(), box.y() + box.h(), look.ground());
        Draw.outline(g, box.x(), box.y(), box.w(), box.h(), look.edge());
        g.fill(box.x() + 1, box.y() + 1, box.x() + box.w() - 1, box.y() + TouchKeyboardLayout.BAR, look.bar());
        Draw.text(g, font, GameText.resolve(TouchKeyboardTexts.TITLE), box.x() + 4, box.y() + 1, look.dim());
        final Box close = TouchKeyboardLayout.closeBox(box);
        Draw.textCentered(g, font, GameText.resolve(TouchKeyboardTexts.CLOSE), close.x() + close.w() / 2, close.y(),
                look.ink());
        for (final Key key : TouchKeyboardLayout.keys(layer, box)) {
            final boolean lit = key.contains(lmx, lmy) || key.kind() == Kind.SHIFT && shift
                    || key.kind() == Kind.CTRL && ctrl;
            final int face = lit ? look.lit() : key.kind() == Kind.CHAR ? look.key() : look.wideKey();
            g.fill(key.x(), key.y(), key.x() + key.w(), key.y() + key.h(), face);
            final String label = labelOf(key);
            if (!label.isEmpty()) {
                Draw.textCentered(g, font, label, key.x() + key.w() / 2, key.y() + (key.h() - font.lineHeight) / 2 + 1,
                        look.ink());
            }
        }
        ring(g, look);
        g.pose().popPose();
    }

    /* The keyboard of this desktop's system, or none for a system that has none. */
    @Nullable
    private Look look() {
        return switch (desktop.panelStyle()) {
            case FRAMES_10 -> FRAMES_10.get();
            case FRAMES_11 -> FRAMES_11.get();
            default -> null;
        };
    }

    /* A character key: the letter in capitals under Shift; under Control, the key itself with Control held. */
    private Typed charOf(final char character, final int held) {
        if (held != 0 && Character.isLetter(character)) {
            return Typed.key(GLFW.GLFW_KEY_A + Character.toLowerCase(character) - 'a', held);
        }
        return Typed.of(String.valueOf(shift ? Character.toUpperCase(character) : character));
    }

    private String labelOf(final Key key) {
        final TextKey word = switch (key.kind()) {
            case CHAR, SPACE -> null;
            case BACKSPACE -> TouchKeyboardTexts.BACKSPACE;
            case ENTER -> TouchKeyboardTexts.ENTER;
            case SHIFT -> TouchKeyboardTexts.SHIFT;
            case LAYER -> layer == Layer.LETTERS ? TouchKeyboardTexts.SYMBOLS : TouchKeyboardTexts.LETTERS;
            case CTRL -> TouchKeyboardTexts.CTRL;
            case SMILE -> TouchKeyboardTexts.SMILE;
            case LEFT -> TouchKeyboardTexts.LEFT;
            case RIGHT -> TouchKeyboardTexts.RIGHT;
            case HIDE -> TouchKeyboardTexts.HIDE;
        };
        if (word != null) {
            return GameText.resolve(word);
        }
        if (key.kind() == Kind.SPACE) {
            return "";
        }
        return String.valueOf(shift ? Character.toUpperCase(key.character()) : key.character());
    }

    /* The ring where the last touch landed, two circles of light, while it lasts. */
    private void ring(final GuiGraphics g, final Look look) {
        if (DesktopMotion.now() - ringAt > RING_MS) {
            return;
        }
        circle(g, (int) ringX, (int) ringY, RING_INNER, look.ring());
        circle(g, (int) ringX, (int) ringY, RING_OUTER, look.ringOuter());
    }

    /* A circle one pixel wide, plotted the midpoint way, eight points at a step. */
    private static void circle(final GuiGraphics g, final int cx, final int cy, final int r, final int colour) {
        int x = r;
        int y = 0;
        int err = 1 - r;
        while (x >= y) {
            final int[][] points = {{x, y}, {y, x}, {-y, x}, {-x, y}, {-x, -y}, {-y, -x}, {y, -x}, {x, -y}};
            for (final int[] p : points) {
                g.fill(cx + p[0], cy + p[1], cx + p[0] + 1, cy + p[1] + 1, colour);
            }
            y++;
            if (err < 0) {
                err += 2 * y + 1;
            } else {
                x--;
                err += 2 * (y - x) + 1;
            }
        }
    }

    /**
     * What a touched key types: text, or a key pressed with the keys held with it.
     *
     * @param text      the characters typed, or empty for a key
     * @param key       the key pressed, for no text
     * @param modifiers the keys held with it
     */
    record Typed(String text, int key, int modifiers) {

        static Typed of(final String text) {
            return new Typed(text, 0, 0);
        }

        static Typed key(final int key, final int modifiers) {
            return new Typed("", key, modifiers);
        }
    }

    /**
     * One system's touch keyboard, as it draws it.
     *
     * @param ground    behind the keys
     * @param edge      its edge
     * @param bar       the bar along its top
     * @param key       a letter's key
     * @param wideKey   a key that does something else
     * @param lit       the key under the finger
     * @param ink       the letters
     * @param dim       its name on the bar
     * @param ring      the inner ring of a touch
     * @param ringOuter the outer ring
     */
    private record Look(int ground, int edge, int bar, int key, int wideKey, int lit, int ink, int dim, int ring,
                        int ringOuter) {
    }
}
