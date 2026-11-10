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
 * What the debugger of Virtual Studio and Virtual Studio Code says: its menu items and buttons, its panes, and the
 * lines its status bar and its Diagnostic Tools show.
 */
@TextHolder
final class DebugTexts {

    static final TextKey START_DEBUGGING = TextKey.of("jsc.studio.debug.start", "Start Debugging");
    static final TextKey START_WITHOUT = TextKey.of("jsc.studio.debug.start_without", "Start Without Debugging");
    static final TextKey CONTINUE = TextKey.of("jsc.studio.debug.continue", "Continue");
    static final TextKey PAUSE = TextKey.of("jsc.studio.debug.pause", "Break All");
    static final TextKey STOP = TextKey.of("jsc.studio.debug.stop", "Stop Debugging");
    static final TextKey RESTART = TextKey.of("jsc.studio.debug.restart", "Restart");
    static final TextKey STEP_INTO = TextKey.of("jsc.studio.debug.step_into", "Step Into");
    static final TextKey STEP_OVER = TextKey.of("jsc.studio.debug.step_over", "Step Over");
    static final TextKey STEP_OUT = TextKey.of("jsc.studio.debug.step_out", "Step Out");
    static final TextKey TOGGLE_BREAKPOINT = TextKey.of("jsc.studio.debug.toggle_breakpoint", "Toggle Breakpoint");
    static final TextKey DELETE_BREAKPOINTS = TextKey.of("jsc.studio.debug.delete_breakpoints",
            "Delete All Breakpoints");
    static final TextKey ADD_WATCH = TextKey.of("jsc.studio.debug.add_watch", "Add Watch");
    /* The configuration of a build for the debugger, which defines DEBUG; the other is Release, to ship. */
    static final TextKey DEBUG = TextKey.of("jsc.studio.debug.config_debug", "Debug");
    /* The panes, captioned in capitals as both studios caption them. */
    static final TextKey LOCALS = TextKey.of("jsc.studio.debug.locals", "LOCALS");
    static final TextKey WATCH = TextKey.of("jsc.studio.debug.watch", "WATCH");
    static final TextKey CALL_STACK = TextKey.of("jsc.studio.debug.call_stack", "CALL STACK");
    static final TextKey BREAKPOINTS = TextKey.of("jsc.studio.debug.breakpoints", "BREAKPOINTS");
    static final TextKey OUTPUT = TextKey.of("jsc.studio.debug.output", "OUTPUT");
    static final TextKey VARIABLES = TextKey.of("jsc.studio.debug.variables", "VARIABLES");
    static final TextKey DEBUG_CONSOLE = TextKey.of("jsc.studio.debug.console", "DEBUG CONSOLE");
    static final TextKey RUN_AND_DEBUG = TextKey.of("jsc.studio.debug.run_and_debug", "RUN AND DEBUG");
    static final TextKey DIAGNOSTIC_TOOLS = TextKey.of("jsc.studio.debug.diagnostic_tools", "DIAGNOSTIC TOOLS");
    /* The scope the Variables section shows, and the button that starts debugging from the Run and Debug view. */
    static final TextKey LOCALS_SCOPE = TextKey.of("jsc.studio.debug.locals_scope", "Locals");
    static final TextKey START_RUN_AND_DEBUG = TextKey.of("jsc.studio.debug.start_run_and_debug", "Run and Debug");
    static final TextKey NAME = TextKey.of("jsc.studio.debug.name", "Name");
    static final TextKey VALUE = TextKey.of("jsc.studio.debug.value", "Value");
    static final TextKey TYPE = TextKey.of("jsc.studio.debug.type", "Type");
    static final TextKey LINE = TextKey.of("jsc.studio.debug.line", "Line");
    static final TextKey NOT_FOUND = TextKey.of("jsc.studio.debug.not_found", "not available here");
    /* The Diagnostic Tools: what was spent, on which calls, and what happened. */
    static final TextKey CREDITS_SPENT = TextKey.of("jsc.studio.debug.credits_spent", "Credits spent");
    static final TextKey THE_STEPS = TextKey.of("jsc.studio.debug.the_steps", "the steps");
    static final TextKey PROCESS_MEMORY = TextKey.of("jsc.studio.debug.process_memory", "Process memory");
    static final TextKey ELAPSED = TextKey.of("jsc.studio.debug.elapsed", "Elapsed");
    static final TextKey TICKS = TextKey.of("jsc.studio.debug.ticks", "%s ticks");
    static final TextKey EVENTS = TextKey.of("jsc.studio.debug.events", "Events");
    static final TextKey EVENT_OUTPUT = TextKey.of("jsc.studio.debug.event_output", "Output: %s");
    static final TextKey EVENT_BREAKPOINT = TextKey.of("jsc.studio.debug.event_breakpoint", "Breakpoint hit: line %s");
    static final TextKey EVENT_STEP = TextKey.of("jsc.studio.debug.event_step", "Step: line %s");
    static final TextKey EVENT_BREAK = TextKey.of("jsc.studio.debug.event_break", "Break: line %s");
    /* The status bar, orange while a program is being debugged. */
    static final TextKey STATUS_RUNNING = TextKey.of("jsc.studio.debug.status_running", "Running");
    static final TextKey STATUS_BREAKPOINT = TextKey.of("jsc.studio.debug.status_breakpoint",
            "Paused at a breakpoint");
    static final TextKey STATUS_PAUSED = TextKey.of("jsc.studio.debug.status_paused", "Paused");
    static final TextKey STATUS_DEBUGGING = TextKey.of("jsc.studio.debug.status_debugging", "Debugging: %s");
    static final TextKey STATUS_CREDITS = TextKey.of("jsc.studio.debug.status_credits", "%s credits spent");
    /* What the Debug Console and the Output pane say as the program goes. */
    static final TextKey HIT = TextKey.of("jsc.studio.debug.hit", "Breakpoint hit: %s line %s");
    static final TextKey STEPPED_INTO = TextKey.of("jsc.studio.debug.stepped_into", "Stepped into %s");
    static final TextKey EXITED = TextKey.of("jsc.studio.debug.exited", "The program '%s' has exited with code %s.");
    static final TextKey HALTED = TextKey.of("jsc.studio.debug.halted", "The program '%s' stopped: %s");
    static final TextKey NOT_STARTED = TextKey.of("jsc.studio.debug.not_started", "The program did not start: %s");
    static final TextKey WAITING = TextKey.of("jsc.studio.debug.waiting",
            "The program waits for a line: type it below and press Enter");

    private DebugTexts() {
    }
}
