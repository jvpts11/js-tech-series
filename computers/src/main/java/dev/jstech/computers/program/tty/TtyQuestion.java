/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.tty;

import dev.jstech.computers.program.cli.CliLine;

/**
 * What a tool has stopped to ask: the words it asks in, and whether the answer is to be kept off the glass.
 *
 * <p>The words stand where the prompt would, since that is where a real tool puts them. An answer kept off
 * the glass is a password: typed, taken, and never shown, not even as dots.
 */
public record TtyQuestion(CliLine text, boolean masked) {
}
