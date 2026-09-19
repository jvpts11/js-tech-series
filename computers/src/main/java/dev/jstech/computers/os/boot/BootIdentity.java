/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

/**
 * Who is coming up, as far as a desktop's own loading screen needs to know it: which desktop it is, which
 * system it stands on, and what the machine is called on a network, which is what its prompt will say.
 *
 * @param desktopId  the desktop the machine brings up after its system, by the last part of its id, or empty
 *                   when it has none and comes up at a prompt
 * @param systemName the system by the name a person reads
 * @param hostName   the name the machine answers to at a prompt
 */
public record BootIdentity(String desktopId, String systemName, String hostName) {

    /** A machine nothing is known of, and one on its way down, which brings no desktop up. */
    public static final BootIdentity NONE = new BootIdentity("", "", "");

    public BootIdentity {
        desktopId = desktopId == null ? "" : desktopId;
        systemName = systemName == null ? "" : systemName;
        hostName = hostName == null ? "" : hostName;
    }

    /** The same machine on its way down: it names its system and its host, and no desktop is coming. */
    public BootIdentity goingDown() {
        return new BootIdentity("", this.systemName, this.hostName);
    }
}
