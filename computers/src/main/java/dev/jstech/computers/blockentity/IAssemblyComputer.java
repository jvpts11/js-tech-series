/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

/**
 * What an assembly menu reads off the computer it is open on, besides the hardware slots and the case: whether it
 * runs, whether its build is valid, its capacity and memory buffer, whether it starts by itself and whether it is on
 * a network. The block entities mirror these to the client while the menu is open.
 */
public interface IAssemblyComputer {

    /** Whether the menu should show the machine as running. */
    boolean assemblyRunning();

    /** Whether the menu should show a valid build. */
    boolean assemblyBuildValid();

    long assemblyCapacity();

    long assemblyRamBuffer();

    boolean assemblyAutoStart();

    boolean assemblyOnNetwork();
}
