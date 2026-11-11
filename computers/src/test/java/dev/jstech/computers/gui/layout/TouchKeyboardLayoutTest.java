/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.gui.layout.TouchKeyboardLayout.Box;
import dev.jstech.computers.gui.layout.TouchKeyboardLayout.Key;
import dev.jstech.computers.gui.layout.TouchKeyboardLayout.Kind;
import dev.jstech.computers.gui.layout.TouchKeyboardLayout.Layer;
import java.util.List;
import org.junit.jupiter.api.Test;

class TouchKeyboardLayoutTest {

    /* A desktop the size of a monitor's glass, its taskbar a band at its foot. */
    private static final int DESK_W = MonitorGlass.WIDTH;
    private static final int WORK_BOTTOM = MonitorGlass.HEIGHT - 24;

    @Test
    void keys_neitherOverlapNorLeaveTheKeyboard() {
        for (final Box box : boxes()) {
            for (final Layer layer : Layer.values()) {
                final List<Key> keys = TouchKeyboardLayout.keys(layer, box);
                for (int i = 0; i < keys.size(); i++) {
                    final Key key = keys.get(i);
                    assertTrue(key.x() >= box.x() && key.y() >= box.y() + TouchKeyboardLayout.BAR
                                    && key.x() + key.w() <= box.x() + box.w() && key.y() + key.h() <= box.y() + box.h(),
                            layer + " key " + key + " leaves " + box);
                    for (int j = i + 1; j < keys.size(); j++) {
                        assertFalse(overlap(key, keys.get(j)), key + " overlaps " + keys.get(j));
                    }
                }
            }
        }
    }

    @Test
    void keys_areBigEnoughToTouch() {
        for (final Box box : boxes()) {
            for (final Layer layer : Layer.values()) {
                for (final Key key : TouchKeyboardLayout.keys(layer, box)) {
                    assertTrue(key.w() >= 12 && key.h() >= 12, layer + " key " + key + " is too small in " + box);
                }
            }
        }
    }

    @Test
    void keys_haveFourRowsWithTheLettersOfTheMock() {
        final List<Key> keys = TouchKeyboardLayout.keys(Layer.LETTERS, TouchKeyboardLayout.docked(DESK_W, WORK_BOTTOM));
        assertEquals(11 + 11 + 12 + 7, keys.size());
        assertEquals('q', keys.getFirst().character());
        assertEquals(Kind.BACKSPACE, keys.get(10).kind());
        assertEquals(Kind.HIDE, keys.getLast().kind());
        assertEquals(26, keys.stream().filter(key -> Character.isLetter(key.character())
                && key.kind() == Kind.CHAR).count());
    }

    @Test
    void boxes_standOnTheDesktopAboveTheTaskbar() {
        for (final Box box : boxes()) {
            assertTrue(box.x() >= 0 && box.y() >= 0 && box.x() + box.w() <= DESK_W
                    && box.y() + box.h() <= WORK_BOTTOM, box + " leaves the desktop's work area");
        }
    }

    @Test
    void at_findsTheKeyUnderAPointAndNoneBetweenKeys() {
        final List<Key> keys = TouchKeyboardLayout.keys(Layer.LETTERS, TouchKeyboardLayout.floating(DESK_W,
                WORK_BOTTOM));
        final Key q = keys.getFirst();
        assertEquals(q, TouchKeyboardLayout.at(keys, q.x() + 1, q.y() + 1).orElseThrow());
        assertTrue(TouchKeyboardLayout.at(keys, q.x() + q.w(), q.y() + 1).isEmpty(), "the gap after a key is no key");
    }

    @Test
    void closeBox_sitsOnTheBarInsideTheKeyboard() {
        for (final Box box : boxes()) {
            final Box close = TouchKeyboardLayout.closeBox(box);
            assertTrue(close.x() >= box.x() && close.x() + close.w() <= box.x() + box.w()
                    && close.y() >= box.y() && close.y() + close.h() <= box.y() + TouchKeyboardLayout.BAR);
        }
    }

    private static List<Box> boxes() {
        return List.of(TouchKeyboardLayout.docked(DESK_W, WORK_BOTTOM), TouchKeyboardLayout.floating(DESK_W,
                WORK_BOTTOM));
    }

    private static boolean overlap(final Key a, final Key b) {
        return a.x() < b.x() + b.w() && b.x() < a.x() + a.w() && a.y() < b.y() + b.h() && b.y() < a.y() + a.h();
    }
}
