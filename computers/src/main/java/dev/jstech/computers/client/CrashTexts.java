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
 * What the Frames family's crash pages say, in the player's language as the real ones were in theirs. A kernel's
 * panic is not here: no kernel ever printed one in anything but its own words, so those lines come from the machine
 * as they are.
 */
@TextHolder
final class CrashTexts {

    // Frames 95: the fatal exception, its points and the restart.
    static final TextKey F95_FATAL = TextKey.of("jsc.crash.f95.fatal",
            "A fatal exception %s has occurred at %s. The current application will be terminated.");
    static final TextKey F95_MEMORY = TextKey.of("jsc.crash.f95.memory",
            "This computer ran out of memory with too many programs open, and the system became unstable.");
    static final TextKey F95_MEMORY_NEXT = TextKey.of("jsc.crash.f95.memory_next",
            "The cooperative kernel cannot recover. Frames will restart.");
    static final TextKey F95_HARDWARE = TextKey.of("jsc.crash.f95.hardware",
            "The hardware of this computer stopped answering: a part was taken out while it was running.");
    static final TextKey F95_HARDWARE_NEXT = TextKey.of("jsc.crash.f95.hardware_next",
            "Frames cannot continue. The computer will restart if it still can.");
    static final TextKey F95_PROCESS = TextKey.of("jsc.crash.f95.process",
            "A process the system cannot run without was ended.");
    static final TextKey F95_PROCESS_NEXT = TextKey.of("jsc.crash.f95.process_next",
            "Frames cannot continue. Frames will restart.");
    static final TextKey F95_RESTARTING = TextKey.of("jsc.crash.f95.restarting", "Restarting...");

    // Frames XP and 7: the Stop error screen.
    static final TextKey NT_DETECTED = TextKey.of("jsc.crash.nt.detected",
            "A problem has been detected and Frames has been shut down to prevent damage to your computer.");
    static final TextKey NT_FIRST_TIME = TextKey.of("jsc.crash.nt.first_time",
            "If this is the first time you've seen this Stop error screen, restart your computer. If this screen"
                    + " appears again, follow these steps:");
    static final TextKey NT_ADVICE_MEMORY = TextKey.of("jsc.crash.nt.advice_memory",
            "Close some programs before you open more. Every open window holds part of this computer's memory, and"
                    + " more memory can be fitted on its motherboard.");
    static final TextKey NT_ADVICE_HARDWARE = TextKey.of("jsc.crash.nt.advice_hardware",
            "Check that every part of this computer is seated. A part was taken out of it while it was running, and"
                    + " Frames cannot run without it.");
    static final TextKey NT_ADVICE_PROCESS = TextKey.of("jsc.crash.nt.advice_process",
            "A process Frames cannot run without was ended. Do not end the system's own processes.");
    static final TextKey NT_TECHNICAL = TextKey.of("jsc.crash.nt.technical", "Technical information:");
    static final TextKey NT_STOP = TextKey.of("jsc.crash.nt.stop", "*** STOP: %s");
    static final TextKey XP_DUMP_BEGIN = TextKey.of("jsc.crash.xp.dump_begin", "Beginning dump of physical memory");
    static final TextKey XP_DUMP_DONE = TextKey.of("jsc.crash.xp.dump_done", "Physical memory dump complete.");
    static final TextKey XP_CONTACT = TextKey.of("jsc.crash.xp.contact",
            "Contact your system administrator or technical support group for further assistance.");
    static final TextKey SEVEN_COLLECTING = TextKey.of("jsc.crash.seven.collecting",
            "Collecting data for crash dump ...");
    static final TextKey SEVEN_INITIALIZING = TextKey.of("jsc.crash.seven.initializing",
            "Initializing disk for crash dump ...");
    static final TextKey SEVEN_BEGIN = TextKey.of("jsc.crash.seven.begin", "Beginning dump of physical memory.");
    static final TextKey SEVEN_DUMPING = TextKey.of("jsc.crash.seven.dumping", "Dumping physical memory to disk:  %s");

    // Frames 10 and 11: the sad face's page.
    static final TextKey TEN_SENTENCE = TextKey.of("jsc.crash.ten.sentence",
            "Your PC ran into a problem and needs to restart. We're just collecting some error info, and then we'll"
                    + " restart for you.");
    static final TextKey ELEVEN_SENTENCE = TextKey.of("jsc.crash.eleven.sentence",
            "Your device ran into a problem and needs to restart. We're just collecting some error info, and then"
                    + " we'll restart for you.");
    static final TextKey MODERN_COMPLETE = TextKey.of("jsc.crash.modern.complete", "%s complete");
    static final TextKey MODERN_MORE = TextKey.of("jsc.crash.modern.more",
            "For more information about this issue and possible fixes, visit %s");
    static final TextKey MODERN_SUPPORT = TextKey.of("jsc.crash.modern.support",
            "If you call a support person, give them this info:");
    static final TextKey MODERN_STOP_CODE = TextKey.of("jsc.crash.modern.stop_code", "Stop code: %s");
    static final TextKey MODERN_FAILED = TextKey.of("jsc.crash.modern.failed", "What failed: %s");

    private CrashTexts() {
    }
}
