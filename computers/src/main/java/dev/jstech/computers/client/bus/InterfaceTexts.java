/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.bus;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a Crafting Interface's window says, kept apart from the window so the language generator can read it on a server
 * too, where windows do not exist.
 */
@TextHolder
final class InterfaceTexts {

    static final TextKey TITLE = TextKey.of("jsc.interface.screen.title", "CRAFTING INTERFACE");
    static final TextKey PATTERNS = TextKey.of("jsc.interface.screen.patterns", "PATTERNS");
    static final TextKey COUNT = TextKey.of("jsc.interface.screen.count", "%s of %s");
    static final TextKey PATTERN_NOTE = TextKey.of("jsc.interface.screen.pattern_note",
            "Patterns are placed and moved in the Crafting Manager. Click one to see its inputs.");
    static final TextKey INPUTS_OF = TextKey.of("jsc.interface.screen.inputs_of", "INPUTS OF %s");
    static final TextKey INPUT = TextKey.of("jsc.interface.screen.input", "%s x%s");
    static final TextKey WHY_CHOSEN = TextKey.of("jsc.interface.screen.why_chosen", "chosen by hand");
    static final TextKey WHY_FILTER = TextKey.of("jsc.interface.screen.why_filter", "its filter takes it");
    static final TextKey WHY_ANY = TextKey.of("jsc.interface.screen.why_any", "it takes anything");
    static final TextKey WHY_NONE = TextKey.of("jsc.interface.screen.why_none",
            "no router on its cable takes it: this pattern cannot run");
    static final TextKey NO_ROUTER = TextKey.of("jsc.interface.screen.no_router", "none");
    static final TextKey MODE = TextKey.of("jsc.interface.screen.mode", "MODE");
    static final TextKey EXCLUSIVE = TextKey.of("jsc.interface.screen.exclusive", "EXCLUSIVE");
    static final TextKey NOT_EXCLUSIVE = TextKey.of("jsc.interface.screen.not_exclusive", "NOT EXCLUSIVE");
    static final TextKey EXCLUSIVE_NOTE = TextKey.of("jsc.interface.screen.exclusive_note",
            "One recipe at a time on this machine, as its routers need; jobs of the same pattern run together.");
    static final TextKey SHARED_NOTE = TextKey.of("jsc.interface.screen.shared_note",
            "Several jobs at once, different recipes too, as far as the machine takes them.");
    static final TextKey FEEDS = TextKey.of("jsc.interface.screen.feeds", "FEEDS");
    static final TextKey RECEIVING = TextKey.of("jsc.interface.screen.receiving", "RECEIVING");
    static final TextKey STATE = TextKey.of("jsc.interface.screen.state", "STATE");
    static final TextKey RUNNING = TextKey.of("jsc.interface.screen.running", "RUNNING");
    static final TextKey PAUSED = TextKey.of("jsc.interface.screen.paused", "PAUSED");
    static final TextKey MAX_JOBS = TextKey.of("jsc.interface.screen.max_jobs", "MAX JOBS");
    static final TextKey AUTO = TextKey.of("jsc.interface.screen.auto", "Auto");
    static final TextKey JOBS_NOTE = TextKey.of("jsc.interface.screen.jobs_note", "at once on this interface");
    static final TextKey NOW = TextKey.of("jsc.interface.screen.now", "NOW");
    static final TextKey NAME = TextKey.of("jsc.interface.screen.name", "NAME");
    static final TextKey NAME_FIELD = TextKey.of("jsc.interface.screen.name_field", "Interface name");
    static final TextKey LINKED = TextKey.of("jsc.interface.screen.linked", "Driven by a Crafting Computer");
    static final TextKey OFFLINE = TextKey.of("jsc.interface.screen.offline", "Driven by no Crafting Computer");
    static final TextKey PICK_HINT = TextKey.of("jsc.interface.screen.pick_hint",
            "Click for the next router on its cable, right-click for the one its filter picks.");
    static final TextKey NO_ACTIVITY = TextKey.of("jsc.interface.screen.no_activity", "No jobs yet.");
    static final TextKey ACTIVITY_NOTE = TextKey.of("jsc.interface.screen.activity_note",
            "Every job is an Operation: the terminal and the Network Manager list the same log.");
    static final TextKey JOB = TextKey.of("jsc.interface.screen.job", "%s x%s");
    static final TextKey JOB_DONE = TextKey.of("jsc.interface.screen.job_done", "%s of %s made");
    static final TextKey JOB_FAILED = TextKey.of("jsc.interface.screen.job_failed",
            "the machine gave none of the expected outputs");
    static final TextKey UNEXPECTED = TextKey.of("jsc.interface.screen.unexpected",
            "unexpected outputs: %s %s, sent to the network");
    static final TextKey DRAINING = TextKey.of("jsc.interface.screen.draining", "draining: the recipe changes");
    static final TextKey STATUS_COMPLETED = TextKey.of("jsc.interface.screen.status_completed", "COMPLETED");
    static final TextKey STATUS_PARTIAL = TextKey.of("jsc.interface.screen.status_partial", "COMPLETED_PARTIAL");
    static final TextKey STATUS_FAILED = TextKey.of("jsc.interface.screen.status_failed", "FAILED");
    static final TextKey STATUS_WAITING = TextKey.of("jsc.interface.screen.status_waiting", "WAITING");
    static final TextKey ADDRESS = TextKey.of("jsc.interface.screen.address", "ADDRESS");
    static final TextKey IQL = TextKey.of("jsc.interface.screen.iql", "IQL");
    static final TextKey SIGMA = TextKey.of("jsc.interface.screen.sigma", "SIGMA");
    static final TextKey SET_BY_PROGRAMS = TextKey.of("jsc.interface.screen.set_by_programs", "SET BY PROGRAMS");
    static final TextKey SET_BY_LINE = TextKey.of("jsc.interface.screen.set_by_line", "%s set by %s");
    static final TextKey SOFTWARE_NOTE = TextKey.of("jsc.interface.screen.software_note",
            "Everything here can also be set in CONFIGURE, and what a program set is marked there.");
    static final TextKey UNNAMED = TextKey.of("jsc.interface.screen.unnamed",
            "Give it a name in CONFIGURE so software can find it.");

    private InterfaceTexts() {
    }
}
