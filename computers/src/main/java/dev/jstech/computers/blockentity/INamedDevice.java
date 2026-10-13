/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

/**
 * A device a player can give a name, which the machine it is plugged into then lists it by.
 *
 * <p>The device list asks through here, so a new kind of nameable device only has to implement this rather than
 * be added to the list's code.
 */
public interface INamedDevice {

    /** The name a player gave the device; empty while it has none. */
    String name();
}
