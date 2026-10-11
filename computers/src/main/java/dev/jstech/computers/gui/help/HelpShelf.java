/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import dev.jstech.computers.guide.ComputersGuide;
import dev.jstech.computers.os.InstallMode;
import dev.jstech.computers.os.Platform;
import dev.jstech.core.guide.ManualReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Which book a system's help opens first: the one written for it. MC-DOS and the Frames editions open the Frames
 * Resource Kit; the Linux distributions installed with a guide the Linux Desk Reference, and those installed by hand
 * Linux by Hand; FreeBSD and UNIX the Daemon Handbook; and MC-NET, a network's own machine, the Guide to Operations.
 * The other manuals follow in their own order, so nothing a help program showed before is gone.
 *
 * <p>Pure: a system in, a manual's id out.
 */
public final class HelpShelf {

    private HelpShelf() {
    }

    /** The id of the book written for a system of that family, installed that way. */
    public static String bookFor(final Platform platform, final InstallMode install) {
        return switch (platform) {
            case MC_DOS, FRAMES -> ComputersGuide.FRAMES_RESOURCE_KIT;
            case LINUX -> install == InstallMode.GUIDED ? ComputersGuide.LINUX_DESK_REFERENCE
                    : ComputersGuide.LINUX_BY_HAND;
            case UNIX, FREEBSD -> ComputersGuide.DAEMON_HANDBOOK;
            case MC_NET -> ComputersGuide.GUIDE_TO_OPERATIONS;
        };
    }

    /** The manuals with that book first when it is among them, the others in the order they came. */
    public static List<ManualReader> first(final List<ManualReader> manuals, final String book) {
        final List<ManualReader> out = new ArrayList<>();
        for (final ManualReader reader : manuals) {
            if (reader.manualId().equals(book)) {
                out.add(reader);
            }
        }
        for (final ManualReader reader : manuals) {
            if (!reader.manualId().equals(book)) {
                out.add(reader);
            }
        }
        return out;
    }
}
