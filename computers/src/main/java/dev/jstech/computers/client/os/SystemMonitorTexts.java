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
 * What the System Monitor says: its headings, its specifications and what each disk and each holder of memory is.
 * Names, sizes and clocks are data. Kept apart from the window so the language generator can read it on a server
 * too, where windows do not exist.
 */
@TextHolder
final class SystemMonitorTexts {

    static final TextKey TITLE = TextKey.of("jsc.system_monitor.title", "System Monitor");
    static final TextKey READING = TextKey.of("jsc.system_monitor.reading", "Reading machine...");
    static final TextKey COMPUTER = TextKey.of("jsc.system_monitor.computer", "Computer");
    static final TextKey PROCESSOR = TextKey.of("jsc.system_monitor.processor", "Processor");
    static final TextKey MEMORY = TextKey.of("jsc.system_monitor.memory", "Memory");
    static final TextKey RAM = TextKey.of("jsc.system_monitor.ram", "RAM");
    static final TextKey HELD_OF = TextKey.of("jsc.system_monitor.held_of", "%s of %s MB");
    static final TextKey GRAPHICS = TextKey.of("jsc.system_monitor.graphics", "Graphics");
    static final TextKey VRAM = TextKey.of("jsc.system_monitor.vram", "VRAM");
    static final TextKey NO_GPU = TextKey.of("jsc.system_monitor.no_gpu", "no GPU");
    static final TextKey MEGABYTES = TextKey.of("jsc.system_monitor.megabytes", "%s MB");
    static final TextKey MEMORY_HEADER = TextKey.of("jsc.system_monitor.memory_header", "MEMORY");
    static final TextKey FREE = TextKey.of("jsc.system_monitor.free", "%s MB free");
    static final TextKey STORAGE_HEADER = TextKey.of("jsc.system_monitor.storage_header", "STORAGE");
    static final TextKey PROGRAMS_INSTALLED =
            TextKey.of("jsc.system_monitor.programs_installed", "%s programs installed");
    static final TextKey SYSTEM_DISK = TextKey.of("jsc.system_monitor.system_disk", "%s (system)");

    private SystemMonitorTexts() {
    }
}
