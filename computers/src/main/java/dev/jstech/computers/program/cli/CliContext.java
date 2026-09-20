/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.List;

/**
 * Everything a command receives when it runs: the arguments after the command word, the computer to act on, the output buffer to write into, and the shell itself (so {@code help} can list its peers). Pure data (no Minecraft types) so a command is exercised in a unit test with a fake computer.
 */
public record CliContext(List<String> args, ICliComputer computer, CliOutput out, CliShell shell,
                         List<String> input) {

    /** A command run on its own, with nothing feeding it, which is most of them. */
    public CliContext(final List<String> args, final ICliComputer computer, final CliOutput out,
                      final CliShell shell) {
        this(args, computer, out, shell, List.of());
    }

    /**
     * What is feeding this command: the lines the command before it in the pipeline printed, or a file's
     * lines when one was named, and nothing at all for a command run on its own.
     *
     * <p>A command that has no use for it ignores it, exactly as at a real shell, where most commands are
     * handed a keyboard they never read.
     */
    public boolean hasInput() {
        return !this.input.isEmpty();
    }

    /** The argument at {@code index}, or {@code ""} when there are fewer arguments than that. */
    public String arg(final int index) {
        return index >= 0 && index < args.size() ? args.get(index) : "";
    }

    public boolean hasArgs() {
        return !args.isEmpty();
    }

    public int argCount() {
        return args.size();
    }

    /**
     * Parses {@code args[index]} as a positive long quantity.
     *
     * @return the parsed amount, or {@code -1} when it is missing or not a positive whole number
     */
    public long longArg(final int index) {
        final String raw = arg(index).replace(",", "").replace("_", "");
        if (raw.isEmpty()) {
            return -1L;
        }
        try {
            final long value = Long.parseLong(raw);
            return value > 0L ? value : -1L;
        } catch (final NumberFormatException notANumber) {
            return -1L;
        }
    }

    /** The arguments from {@code index} onward joined with spaces, for free-text trailing operands. */
    public String rest(final int index) {
        if (index >= args.size()) {
            return "";
        }
        return String.join(" ", args.subList(index, args.size()));
    }
}
