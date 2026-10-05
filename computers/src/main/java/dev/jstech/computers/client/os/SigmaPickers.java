/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.UiWindowPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Which items an item picker shows for what is typed in its search line: those whose id or name holds it, in the
 * order the program added them. The search is the player's own and never reaches the program; only the item picked
 * does.
 */
final class SigmaPickers {

    private SigmaPickers() {
    }

    /** The items shown, as their places in the picker's list counted from zero. */
    static List<Integer> shown(final UiWindowPayload.Widget picker, final String filter, final SigmaPainter painter) {
        final String wanted = filter.toLowerCase(Locale.ROOT).trim();
        final List<Integer> shown = new ArrayList<>(picker.rows().size());
        for (int i = 0; i < picker.rows().size(); i++) {
            final String id = picker.rows().get(i);
            if (wanted.isEmpty() || id.toLowerCase(Locale.ROOT).contains(wanted)
                    || painter.stackOf(id).getHoverName().getString().toLowerCase(Locale.ROOT).contains(wanted)) {
                shown.add(i);
            }
        }
        return shown;
    }
}
