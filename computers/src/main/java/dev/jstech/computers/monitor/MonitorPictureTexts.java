/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.monitor;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * The words a monitor's face shows in the world where the machine itself has none to give: the heading of a
 * self-test, a firmware with nothing to boot, a copy under way.
 */
@TextHolder
final class MonitorPictureTexts {

    static final TextKey SELF_TEST = TextKey.of("jsc.monitor.picture.self_test", "Power-on self-test");
    static final TextKey MEMORY = TextKey.of("jsc.monitor.picture.memory", "Memory test");
    static final TextKey MEGABYTES = TextKey.of("jsc.monitor.picture.megabytes", "%s MB OK");
    static final TextKey TESTING = TextKey.of("jsc.monitor.picture.testing", "Testing the hardware...");
    static final TextKey NO_SYSTEM = TextKey.of("jsc.monitor.picture.no_system", "No system to start");
    static final TextKey FIRMWARE = TextKey.of("jsc.monitor.picture.firmware", "Firmware setup");
    static final TextKey INSTALLER = TextKey.of("jsc.monitor.picture.installer", "Setup");
    static final TextKey INSTALLING = TextKey.of("jsc.monitor.picture.installing", "Installing %s");
    static final TextKey DONE = TextKey.of("jsc.monitor.picture.done", "%s%% complete");
    /** A step of a system coming up, with the mark the system puts at the head of its line. */
    static final TextKey MARKED = TextKey.of("jsc.monitor.picture.marked", "%s %s");

    private MonitorPictureTexts() {
    }
}
