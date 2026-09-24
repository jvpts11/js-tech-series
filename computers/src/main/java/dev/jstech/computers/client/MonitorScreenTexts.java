/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a monitor shows between a machine's own screens: the KVM switch's channels and the splash screens of a
 * system and its desktop coming up. Kept apart from the screens so the language generator can read it on a server
 * too, where screens do not exist.
 */
@TextHolder
final class MonitorScreenTexts {

    // The KVM switch.
    static final TextKey KVM_TITLE = TextKey.of("jsc.kvm.title", "KVM SWITCH");
    static final TextKey KVM_MACHINES = TextKey.of("jsc.kvm.machines", "%s machines");
    /* The function key that picks a channel. */
    static final TextKey KVM_KEY = TextKey.of("jsc.kvm.key", "F%s");
    static final TextKey KVM_ONLINE = TextKey.of("jsc.kvm.online", "ONLINE");
    static final TextKey KVM_OFF = TextKey.of("jsc.kvm.off", "OFF");

    // The splash screens.
    static final TextKey SAFE_TO_TURN_OFF =
            TextKey.of("jsc.splash.safe_to_turn_off", "It's now safe to turn off your computer.");
    static final TextKey WELCOME = TextKey.of("jsc.splash.welcome", "welcome");
    static final TextKey STARTING_PANEL = TextKey.of("jsc.splash.starting_panel", "Starting the panel...");
    static final TextKey STARTING_DESKTOP = TextKey.of("jsc.splash.starting_desktop", "Starting the desktop...");
    static final TextKey STARTING_FILES = TextKey.of("jsc.splash.starting_files", "Starting Files...");
    static final TextKey RESTORING_SESSION = TextKey.of("jsc.splash.restoring_session", "Restoring the session...");
    static final TextKey READY = TextKey.of("jsc.splash.ready", "Ready");
    static final TextKey STARTING_YOUR_DESKTOP =
            TextKey.of("jsc.splash.starting_your_desktop", "starting your desktop");
    /* The system a desktop runs on, under its name. */
    static final TextKey ON_SYSTEM = TextKey.of("jsc.splash.on_system", "on %s");

    private MonitorScreenTexts() {
    }
}
