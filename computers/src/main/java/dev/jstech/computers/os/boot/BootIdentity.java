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
 * system it stands on, what the machine is called on a network, which is what its prompt will say, and the
 * look that desktop keeps, so its loading screen is already wearing it.
 *
 * @param desktopId  the desktop the machine brings up after its system, by the last part of its id, or empty
 *                   when it has none and comes up at a prompt
 * @param systemName the system by the name a person reads
 * @param hostName   the name the machine answers to at a prompt
 * @param look       the look the desktop keeps, as it keeps it, which only that desktop reads; empty for none
 */
public record BootIdentity(String desktopId, String systemName, String hostName, String look) {

    /** A machine nothing is known of. */
    public static final BootIdentity NONE = new BootIdentity("", "", "", "");

    public BootIdentity {
        desktopId = desktopId == null ? "" : desktopId;
        systemName = systemName == null ? "" : systemName;
        hostName = hostName == null ? "" : hostName;
        look = look == null ? "" : look;
    }

    /** The same machine on its way down: it names its system and its host, and no desktop is coming. */
    public BootIdentity goingDown() {
        return new BootIdentity("", this.systemName, this.hostName, "");
    }
}
