/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import dev.jstech.core.guide.ManualReader;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * What a help program at a terminal reads from: the manuals of the player's game, and the commands of the machine it
 * runs on, whose pages come from the machine when they are asked for.
 */
public interface IHelpSource {

    /** Every manual, read as text, the series' own first. */
    List<ManualReader> manuals();

    /** The machine's commands, in the order its help lists them; empty until the machine has answered. */
    List<HelpCommand> commands();

    /** A command's manual page, or null while the machine has not sent it, which asking here sets going. */
    @Nullable
    List<String> commandPage(String name);

    /** One line per recipe of that type making that item, or of every item when none is named. */
    List<String> recipeLines(String type, String output);
}
