/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import java.util.List;

/**
 * The profiles a machine of this architecture can be set to, and how somebody's typing is matched to one.
 *
 * <p>This is kept apart from the package manager's voice because the installer's saved progress reads it
 * too, and that has no business depending on how a merge is drawn.
 */
public final class GentooProfiles {

    /** The profiles, in the order the chooser numbers them. */
    private static final List<String> NAMES = List.of("default/linux/vel64/23.0",
            "default/linux/vel64/23.0/systemd", "default/linux/vel64/23.0/desktop",
            "default/linux/vel64/23.0/desktop/gnome", "default/linux/vel64/23.0/desktop/plasma",
            "default/linux/vel64/23.0/no-multilib", "default/linux/vel64/23.0/hardened");

    private GentooProfiles() {
    }

    /** The profile names, in the order the chooser numbers them. */
    public static List<String> names() {
        return NAMES;
    }

    /**
     * The number of the profile somebody named, by that number or by its whole name, or zero when what they
     * typed is neither.
     */
    public static int profileOf(final String typed) {
        final int byName = NAMES.indexOf(typed) + 1;
        if (byName > 0) {
            return byName;
        }
        try {
            final int number = Integer.parseInt(typed);
            return number >= 1 && number <= NAMES.size() ? number : 0;
        } catch (final NumberFormatException notANumber) {
            return 0;
        }
    }
}
