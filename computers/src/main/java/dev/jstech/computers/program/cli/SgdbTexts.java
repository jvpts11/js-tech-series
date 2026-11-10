/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * What sgdb says, in the words gdb says it in, where gdb has words for it.
 */
@TextHolder
final class SgdbTexts {

    static final TextKey SUMMARY = TextKey.of("jsc.cli.sgdb.summary", "debug a Σ# or Σ program at the terminal");
    static final TextKey USAGE = TextKey.of("jsc.cli.sgdb.usage", "<file.asm> [arguments ...]");
    /* The debugger's version, which is the version of the language its compiler package knows. */
    static final TextKey BANNER = TextKey.of("jsc.cli.sgdb.banner",
            "sgdb %s, the Σ debugger. \"help\" lists its commands.");
    static final TextKey NO_LINES = TextKey.of("jsc.cli.sgdb.no_lines",
            "%s has no lines for the debugger: build it with sgsc --debug");
    static final TextKey UNREADABLE = TextKey.of("jsc.cli.sgdb.unreadable", "%s is not a listing a machine runs");
    /* The number of the breakpoint, the file and the line it landed on. */
    static final TextKey BREAKPOINT_SET = TextKey.of("jsc.cli.sgdb.breakpoint_set", "Breakpoint %s at %s, line %s.");
    static final TextKey NO_LINE = TextKey.of("jsc.cli.sgdb.no_line", "No line %s in file \"%s\".");
    static final TextKey NO_SUCH_BREAKPOINT = TextKey.of("jsc.cli.sgdb.no_such_breakpoint",
            "No breakpoint number %s.");
    static final TextKey NO_BREAKPOINTS = TextKey.of("jsc.cli.sgdb.no_breakpoints", "No breakpoints.");
    static final TextKey BREAKPOINTS_HEAD = TextKey.of("jsc.cli.sgdb.breakpoints_head", "Num  Where");
    static final TextKey NOT_RUNNING = TextKey.of("jsc.cli.sgdb.not_running", "The program is not being run.");
    static final TextKey RUNNING = TextKey.of("jsc.cli.sgdb.running", "The program is running.");
    static final TextKey AGAIN = TextKey.of("jsc.cli.sgdb.again", "Starting the program again from the beginning.");
    /* The number of the breakpoint, then the call it stopped in, what it was handed, its file and line. */
    static final TextKey HIT = TextKey.of("jsc.cli.sgdb.hit", "Breakpoint %s, %s (%s) at %s:%s");
    static final TextKey FRAME = TextKey.of("jsc.cli.sgdb.frame", "%s (%s) at %s:%s");
    static final TextKey STOPPED = TextKey.of("jsc.cli.sgdb.stopped", "Program stopped.");
    static final TextKey RUN_TILL = TextKey.of("jsc.cli.sgdb.run_till", "Run till exit from %s (%s) at %s:%s");
    static final TextKey EXITED = TextKey.of("jsc.cli.sgdb.exited", "[%s exited with code %s]");
    static final TextKey HALTED = TextKey.of("jsc.cli.sgdb.halted", "[%s halted: %s]");
    static final TextKey KILLED = TextKey.of("jsc.cli.sgdb.killed", "[%s killed]");
    static final TextKey NO_SYMBOL = TextKey.of("jsc.cli.sgdb.no_symbol", "No symbol \"%s\" in current context.");
    static final TextKey NO_LOCALS = TextKey.of("jsc.cli.sgdb.no_locals", "No locals.");
    static final TextKey NO_ARGUMENTS = TextKey.of("jsc.cli.sgdb.no_arguments", "No arguments.");
    static final TextKey NO_STACK = TextKey.of("jsc.cli.sgdb.no_stack", "No stack.");
    static final TextKey CREDITS = TextKey.of("jsc.cli.sgdb.credits",
            "%s credits spent: each step pays for what it runs");
    static final TextKey UNDEFINED = TextKey.of("jsc.cli.sgdb.undefined",
            "Undefined command: \"%s\". Try \"help\".");
    static final TextKey HELP_HEAD = TextKey.of("jsc.cli.sgdb.help_head", "List of commands:");
    static final TextKey HELP_BREAK = TextKey.of("jsc.cli.sgdb.help.break",
            "break [file:]line   stop the program at a line");
    static final TextKey HELP_DELETE = TextKey.of("jsc.cli.sgdb.help.delete",
            "delete [number]     take a breakpoint away, or all of them");
    static final TextKey HELP_BREAKPOINTS = TextKey.of("jsc.cli.sgdb.help.info_breakpoints",
            "info breakpoints    list the breakpoints");
    static final TextKey HELP_RUN = TextKey.of("jsc.cli.sgdb.help.run",
            "run                 start the program, or start it again");
    static final TextKey HELP_CONTINUE = TextKey.of("jsc.cli.sgdb.help.continue",
            "continue            go on to the next breakpoint");
    static final TextKey HELP_STEP = TextKey.of("jsc.cli.sgdb.help.step",
            "step                go to the next line, into a call");
    static final TextKey HELP_NEXT = TextKey.of("jsc.cli.sgdb.help.next",
            "next                go to the next line, over a call");
    static final TextKey HELP_FINISH = TextKey.of("jsc.cli.sgdb.help.finish",
            "finish              go on until this call returns");
    static final TextKey HELP_PRINT = TextKey.of("jsc.cli.sgdb.help.print",
            "print name          show what a variable holds");
    static final TextKey HELP_LOCALS = TextKey.of("jsc.cli.sgdb.help.info_locals",
            "info locals         show the variables of this call");
    static final TextKey HELP_ARGS = TextKey.of("jsc.cli.sgdb.help.info_args",
            "info args           show what this call was handed");
    static final TextKey HELP_BACKTRACE = TextKey.of("jsc.cli.sgdb.help.backtrace",
            "backtrace           show the calls the program is in");
    static final TextKey HELP_CREDITS = TextKey.of("jsc.cli.sgdb.help.info_credits",
            "info credits        show what the program has spent");
    static final TextKey HELP_KILL = TextKey.of("jsc.cli.sgdb.help.kill", "kill                end the program");
    static final TextKey HELP_QUIT = TextKey.of("jsc.cli.sgdb.help.quit",
            "quit                end the program and leave sgdb");
    /** One line for each command, in the order help lists them. */
    static final List<TextKey> HELP = List.of(HELP_BREAK, HELP_DELETE, HELP_BREAKPOINTS, HELP_RUN, HELP_CONTINUE,
            HELP_STEP, HELP_NEXT, HELP_FINISH, HELP_PRINT, HELP_LOCALS, HELP_ARGS, HELP_BACKTRACE, HELP_CREDITS,
            HELP_KILL, HELP_QUIT);

    private SgdbTexts() {
    }
}
