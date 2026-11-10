/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

/**
 * One page of an installer: a question it asks, or a beat it shows.
 *
 * <p>Every system's installer is made of these, in its own order and its own words. The oldest one puts the
 * disk and the name on a single settings page; the newest asks them one at a time; one of them gathers every
 * answer in a list and lets the player pick which to give first.
 */
public enum InstallerPage {

    /** What is about to happen, and what the installer found before it starts. */
    WELCOME,

    /** What the maker wants read before anything is installed, as an assistant's second page shows it. */
    READ_ME,

    /** The licence the system is installed under, agreed to before the disk is chosen. */
    LICENSE,

    /**
     * The next disk of a system that comes on several, asked for when the copy has read everything on the one in the
     * drive. The installer puts that one out of the slot and waits for the next one to go in.
     */
    INSERT,

    /** Every answer in one list, each marked given or still wanted, for the installer that works that way. */
    HUB,

    /** Which disk the system goes on, with what each disk already holds. */
    DISK,

    /** The divider between the system already on the disk and the one going in beside it. */
    ALONGSIDE,

    /** The disk and the name together, the way the installers of the first age asked them. */
    SETTINGS,

    /** What the computer is called, which becomes the name the prompt and the network use. */
    NAME,

    /** The optional parts of the system, over the ones that always go. */
    COMPONENTS,

    /** Where the packages and the ports come from over the network, or nowhere at all. */
    MIRROR,

    /** Which of the system's own services start at boot. */
    SERVICES,

    /** Which desktop comes with the system, when a Mirror answers and can serve one. */
    DESKTOP,

    /** The work: the steps of the copy, how far along it is and how long is left. */
    COPY,

    /** The system is on the disk and the machine is waiting to be restarted into it. */
    DONE
}
