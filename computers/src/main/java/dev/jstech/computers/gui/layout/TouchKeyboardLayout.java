/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Where the keys of a system's touch keyboard stand, on the desktop of a touch monitor (computers/docs/design/
 * peripherals.md): a bar with its name and its close box, and four rows of keys under it, each row as wide as the
 * widest lets it, its keys of their own widths. Frames 10 docks it along the foot of the desktop, over the taskbar's
 * edge; Frames 11 floats it, narrower, in the middle.
 *
 * <p>Pure: the keys are kinds and characters, and what each kind is called is the drawing's to say.
 */
public final class TouchKeyboardLayout {

    /** The bar along its top, with its name and its close box. */
    public static final int BAR = 12;
    /** The room between the bar and the first row. */
    public static final int UNDER_BAR = 5;
    /** The room kept round the rows. */
    public static final int MARGIN = 4;
    /** The room between two keys. */
    public static final int GAP = 2;
    /** How tall the docked keyboard of Frames 10 is. */
    public static final int DOCKED_HEIGHT = 121;
    /** How wide and tall the floating keyboard of Frames 11 is, and how far it stands above the taskbar. */
    public static final int FLOATING_WIDTH = 341;
    public static final int FLOATING_HEIGHT = 133;
    public static final int FLOATING_LIFT = 6;
    /** The close box at the right of the bar. */
    public static final int CLOSE = 8;

    /* The keys' characters, row by row, as a QWERTY keyboard has them; characters, not words. */
    private static final List<List<Spec>> LETTERS = List.of(
            row(chars('q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p'), new Spec(Kind.BACKSPACE, ' ', 1.6)),
            row(chars('a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', '\''), new Spec(Kind.ENTER, ' ', 1.9)),
            row(List.of(new Spec(Kind.SHIFT, ' ', 1.4)), chars('z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '?'),
                    new Spec(Kind.SHIFT, ' ', 1.4)),
            bottom());
    private static final List<List<Spec>> SYMBOLS = List.of(
            row(chars('1', '2', '3', '4', '5', '6', '7', '8', '9', '0'), new Spec(Kind.BACKSPACE, ' ', 1.6)),
            row(chars('@', '#', '$', '%', '&', '*', '-', '+', '(', ')'), new Spec(Kind.ENTER, ' ', 1.9)),
            row(List.of(new Spec(Kind.SHIFT, ' ', 1.4)), chars('!', '"', '/', ':', ';', '=', '_', '~', '[', ']'),
                    new Spec(Kind.SHIFT, ' ', 1.4)),
            bottom());

    private TouchKeyboardLayout() {
    }

    /** The keyboard of Frames 10, docked along the foot of a desktop that wide, its work area ending at that y. */
    public static Box docked(final int desktopWidth, final int workAreaBottom) {
        return new Box(0, workAreaBottom - DOCKED_HEIGHT, desktopWidth, DOCKED_HEIGHT);
    }

    /** The keyboard of Frames 11, floating in the middle of a desktop that wide, above its work area's foot. */
    public static Box floating(final int desktopWidth, final int workAreaBottom) {
        final int width = Math.min(FLOATING_WIDTH, desktopWidth);
        return new Box((desktopWidth - width) / 2, workAreaBottom - FLOATING_HEIGHT - FLOATING_LIFT, width,
                FLOATING_HEIGHT);
    }

    /** The keys of one of its two layers, in a keyboard standing on {@code box}. */
    public static List<Key> keys(final Layer layer, final Box box) {
        final List<List<Spec>> rows = layer == Layer.LETTERS ? LETTERS : SYMBOLS;
        final int room = box.w() - 2 * MARGIN;
        double unit = Double.MAX_VALUE;
        for (final List<Spec> row : rows) {
            unit = Math.min(unit, (room - GAP * (row.size() - 1)) / units(row));
        }
        final int top = box.y() + BAR + UNDER_BAR;
        final int pitch = (box.h() - BAR - UNDER_BAR - MARGIN) / rows.size();
        final List<Key> keys = new ArrayList<>();
        for (int r = 0; r < rows.size(); r++) {
            final List<Spec> row = rows.get(r);
            final double used = units(row) * unit + GAP * (row.size() - 1);
            double left = box.x() + MARGIN + (room - used) / 2;
            for (final Spec spec : row) {
                final double right = left + spec.wide() * unit;
                final int x = (int) Math.round(left);
                keys.add(new Key(spec.kind(), spec.character(), x, top + r * pitch, (int) Math.round(right) - x,
                        pitch - GAP));
                left = right + GAP;
            }
        }
        return keys;
    }

    /** The key at a point, if any. */
    public static Optional<Key> at(final List<Key> keys, final double x, final double y) {
        for (final Key key : keys) {
            if (key.contains(x, y)) {
                return Optional.of(key);
            }
        }
        return Optional.empty();
    }

    /** The close box at the right of the bar of a keyboard standing on {@code box}. */
    public static Box closeBox(final Box box) {
        return new Box(box.x() + box.w() - CLOSE - 2, box.y() + 1, CLOSE, CLOSE);
    }

    private static List<Spec> row(final List<Spec> keys, final Spec last) {
        final List<Spec> row = new ArrayList<>(keys);
        row.add(last);
        return List.copyOf(row);
    }

    private static List<Spec> row(final List<Spec> first, final List<Spec> keys, final Spec last) {
        final List<Spec> row = new ArrayList<>(first);
        row.addAll(keys);
        row.add(last);
        return List.copyOf(row);
    }

    private static List<Spec> bottom() {
        return List.of(new Spec(Kind.LAYER, ' ', 1.3), new Spec(Kind.CTRL, ' ', 1.1), new Spec(Kind.SMILE, ' ', 1.0),
                new Spec(Kind.SPACE, ' ', 5.6), new Spec(Kind.LEFT, ' ', 1.0), new Spec(Kind.RIGHT, ' ', 1.0),
                new Spec(Kind.HIDE, ' ', 1.3));
    }

    private static List<Spec> chars(final char... keys) {
        final List<Spec> specs = new ArrayList<>();
        for (final char key : keys) {
            specs.add(new Spec(Kind.CHAR, key, 1.0));
        }
        return specs;
    }

    private static double units(final List<Spec> row) {
        double total = 0;
        for (final Spec spec : row) {
            total += spec.wide();
        }
        return total;
    }

    /** The two layers: the letters, and the digits and signs. */
    public enum Layer {
        LETTERS,
        SYMBOLS
    }

    /** What a key does. */
    public enum Kind {
        /** Types its character, a letter in capitals while Shift is down. */
        CHAR,
        BACKSPACE,
        ENTER,
        /** The next letter in capitals. */
        SHIFT,
        /** Turns to the other layer. */
        LAYER,
        /** The next key with Control held. */
        CTRL,
        /** Types a smile. */
        SMILE,
        SPACE,
        LEFT,
        RIGHT,
        /** Puts the keyboard away until the next field is touched. */
        HIDE
    }

    /**
     * A rectangle of the desktop.
     *
     * @param x left
     * @param y top
     * @param w width
     * @param h height
     */
    public record Box(int x, int y, int w, int h) {

        /** Whether a point is on it. */
        public boolean contains(final double px, final double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    /**
     * One key where it stands.
     *
     * @param kind      what it does
     * @param character what it types, for a {@link Kind#CHAR} key
     * @param x         left
     * @param y         top
     * @param w         width
     * @param h         height
     */
    public record Key(Kind kind, char character, int x, int y, int w, int h) {

        /** Whether a point is on it. */
        public boolean contains(final double px, final double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private record Spec(Kind kind, char character, double wide) {
    }
}
