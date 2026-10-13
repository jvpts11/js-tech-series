/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

/**
 * A service a Mainframe runs, found by the program that installs it: installed or not, and serving only while it is
 * installed, not stopped, and the Mainframe has power.
 *
 * <p>Whatever installs, removes or asks about a service goes through this, so a service is one object on the
 * Mainframe rather than a name every install path has to know how to turn into the right flag.
 */
public interface IMainframeService {

    boolean installed();

    /** Whether it serves right now: installed, not stopped, and the Mainframe running. */
    boolean active();

    /** Installs it; false if it was installed already. */
    boolean install();

    /** Takes it off, stopping it on the way; false if it was not installed. */
    boolean uninstall();
}
