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

    /** Every answer in one list, each marked given or still wanted, for the installer that works that way. */
    HUB,

    /** Which disk the system goes on, with what each disk already holds. */
    DISK,

    /** The disk and the name together, the way the installers of the first age asked them. */
    SETTINGS,

    /** What the computer is called, which becomes the name the prompt and the network use. */
    NAME,

    /** Which desktop comes with the system, when a Mirror answers and can serve one. */
    DESKTOP,

    /** The work: the steps of the copy, how far along it is and how long is left. */
    COPY,

    /** The system is on the disk and the machine is waiting to be restarted into it. */
    DONE
}
