/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a system's welcome says in each of its shapes: the greeting, the facts of the machine, the lines on its
 * cards and the way out. The machine's, the system's, the network's and the parts' names are data, and so are
 * sizes and disk numbers. Kept apart from the window so the language generator can read it on a server too, where
 * windows do not exist.
 */
@TextHolder
final class WelcomeTexts {

    // The window's name, by the shape it takes.
    static final TextKey TITLE = TextKey.of("jsc.welcome.title", "Welcome");
    static final TextKey TITLE_GET_STARTED = TextKey.of("jsc.welcome.title_get_started", "Get started");

    static final TextKey GETTING_READY = TextKey.of("jsc.welcome.getting_ready", "Getting ready...");
    static final TextKey WELCOME_TO = TextKey.of("jsc.welcome.welcome_to", "Welcome to %s");
    static final TextKey DID_YOU_KNOW = TextKey.of("jsc.welcome.did_you_know", "Did you know...");
    static final TextKey NEXT_TIP = TextKey.of("jsc.welcome.next_tip", "Next Tip");
    static final TextKey GET_GOING = TextKey.of("jsc.welcome.get_going", "Get going");
    static final TextKey IS_READY = TextKey.of("jsc.welcome.is_ready", "%s is ready");
    static final TextKey WHAT_IT_HAS =
            TextKey.of("jsc.welcome.what_it_has", "What this PC has, and where to go next.");
    static final TextKey FOLDERS_ON_DISK = TextKey.of("jsc.welcome.folders_on_disk", "The folders on Disk %s.");

    // The machine's facts.
    static final TextKey COMPUTER_NAME = TextKey.of("jsc.welcome.computer_name", "Computer name");
    static final TextKey PROCESSOR = TextKey.of("jsc.welcome.processor", "Processor");
    static final TextKey MEMORY = TextKey.of("jsc.welcome.memory", "Memory");
    static final TextKey MEGABYTES = TextKey.of("jsc.welcome.megabytes", "%s MB");
    static final TextKey ON_DISK = TextKey.of("jsc.welcome.on_disk", "Disk %s, %s");
    static final TextKey ALSO_INSTALLED = TextKey.of("jsc.welcome.also_installed", "Also installed");
    static final TextKey OTHER_SYSTEM = TextKey.of("jsc.welcome.other_system", "%s on Disk %s (F12 at power-on)");
    static final TextKey NETWORK = TextKey.of("jsc.welcome.network", "Network");

    // The cards' lines.
    static final TextKey HARDWARE = TextKey.of("jsc.welcome.hardware", "%s, %s MB. %s on Disk %s.");
    static final TextKey MIRROR_ANSWERS =
            TextKey.of("jsc.welcome.mirror_answers", "pckmgr install adds programs from the Mirror on %s.");
    static final TextKey NO_MIRROR = TextKey.of("jsc.welcome.no_mirror",
            "pckmgr installs programs once a Mainframe on this network runs the Mirror.");
    static final TextKey NOT_CABLED = TextKey.of("jsc.welcome.not_cabled", "Not cabled to a network.");
    static final TextKey CABLED_WITH_MIRROR =
            TextKey.of("jsc.welcome.cabled_with_mirror", "Cabled to %s, and its Mirror answers.");
    static final TextKey CABLED = TextKey.of("jsc.welcome.cabled", "Cabled to %s.");

    // The way out.
    static final TextKey SHOW_AT_STARTUP = TextKey.of("jsc.welcome.show_at_startup", "Show this at startup");
    static final TextKey CLOSE = TextKey.of("jsc.welcome.close", "Close");

    private WelcomeTexts() {
    }
}
