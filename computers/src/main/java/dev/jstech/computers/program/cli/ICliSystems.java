/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.sigma.DesktopFacts;
import dev.jstech.computers.sigma.SystemFacts;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Map;

/**
 * What a machine does with an operating system or a desktop a player built at its prompt
 * (computers/docs/design/player-systems.md): it keeps them for the world, writes a system's installer onto a medium of
 * its era in one of its drives, and publishes both to the network's Mirror.
 */
@TextHolder
public interface ICliSystems {

    /** What a machine that keeps nothing says: one with no world behind it, as a test's. */
    TextKey KEEPS_NOTHING = TextKey.of("jsc.cli.systems.keeps_nothing", "this machine keeps no systems");

    /**
     * Keeps the system built from {@code folder}, with {@code files} its files by path inside it, and writes its
     * installer onto a blank medium that can be written and holds it, in one of the machine's drives.
     */
    default Kept keepSystem(final SystemFacts facts, final String folder, final Map<String, String> files) {
        return Kept.refused(KEEPS_NOTHING.text());
    }

    /** Keeps the desktop built from {@code folder}, with {@code files} its programs and pictures. */
    default Kept keepDesktop(final DesktopFacts facts, final String folder, final Map<String, String> files) {
        return Kept.refused(KEEPS_NOTHING.text());
    }

    /**
     * Publishes to the network's Mirror what was built from {@code folder} on this machine, the system and its
     * desktop, as the world's player who built them: answers the packages published, or why none was.
     */
    default Published publishSystems(final String folder) {
        return Published.refused(KEEPS_NOTHING.text());
    }

    /**
     * What keeping something built came to.
     *
     * @param kept    whether the machine kept it
     * @param failure why it did not, or nothing
     * @param medium  the medium its installer went onto and the drive it is in, or nothing when none could take it
     * @param sizeMb  what it takes, in megabytes
     */
    record Kept(boolean kept, Text failure, Text medium, int sizeMb) {

        /** Kept, with its installer on {@code medium}, or on nothing when that is empty. */
        public static Kept kept(final Text medium, final int sizeMb) {
            return new Kept(true, Text.EMPTY, medium, sizeMb);
        }

        /** Not kept, for that reason. */
        public static Kept refused(final Text failure) {
            return new Kept(false, failure, Text.EMPTY, 0);
        }
    }

    /**
     * What publishing came to.
     *
     * @param packages the packages published, each "name version"
     * @param failure  why nothing was, or why the rest was not, or nothing
     * @param mirror   the Mainframe that serves the Mirror, by name
     * @param author   who the packages are published as: the author the system and the desktop declare
     */
    record Published(List<String> packages, Text failure, String mirror, String author) {

        public Published {
            packages = List.copyOf(packages);
        }

        /** Nothing published, for that reason. */
        public static Published refused(final Text failure) {
            return new Published(List.of(), failure, "", "");
        }
    }
}
