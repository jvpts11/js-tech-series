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

/** The words of PACE, Bellwether Labs' menus for UNIX System V: its frames, its items, its keys and its help. */
@TextHolder
final class PaceTexts {

    static final TextKey NAME = TextKey.of("jsc.pace.name", "PACE");
    static final TextKey MAKER = TextKey.of("jsc.pace.maker", "Bellwether Labs UNIX");

    // The frames and their items.
    static final TextKey OFFICE = TextKey.of("jsc.pace.office", "Office of %s");
    static final TextKey PROGRAMS = TextKey.of("jsc.pace.programs", "Programs");
    static final TextKey ADMINISTRATION = TextKey.of("jsc.pace.administration", "System Administration");
    static final TextKey UNIX_SYSTEM = TextKey.of("jsc.pace.unix_system", "UNIX System");
    static final TextKey EXIT = TextKey.of("jsc.pace.exit", "Exit PACE");
    static final TextKey FILECABINET = TextKey.of("jsc.pace.filecabinet", "Filecabinet");
    static final TextKey WASTEBASKET = TextKey.of("jsc.pace.wastebasket", "Wastebasket");
    static final TextKey OTHER_USERS = TextKey.of("jsc.pace.other_users", "Other Users");
    static final TextKey PREFERENCES = TextKey.of("jsc.pace.preferences", "Preferences");
    static final TextKey FOLDER_TITLE = TextKey.of("jsc.pace.folder_title", "%s  %s");
    static final TextKey MACHINE = TextKey.of("jsc.pace.admin.machine", "Machine Information");
    static final TextKey FILE_SYSTEMS = TextKey.of("jsc.pace.admin.file_systems", "File Systems");
    static final TextKey SOFTWARE = TextKey.of("jsc.pace.admin.software", "Software Installation");
    static final TextKey COMMAND_MENU = TextKey.of("jsc.pace.command_menu", "Command Menu");
    static final TextKey HELP_TITLE = TextKey.of("jsc.pace.help_title", "Help: %s");
    static final TextKey EMPTY = TextKey.of("jsc.pace.empty", "(empty)");
    static final TextKey NO_OTHER_USERS = TextKey.of("jsc.pace.no_other_users", "(no other users)");
    static final TextKey READING = TextKey.of("jsc.pace.reading", "Reading...");

    // The types of what a filecabinet holds.
    static final TextKey DIRECTORY = TextKey.of("jsc.pace.type.directory", "Directory");
    static final TextKey SIGMA_LISTING = TextKey.of("jsc.pace.type.sigma", "Sigma listing");
    static final TextKey SIGMA_SHARP_LISTING = TextKey.of("jsc.pace.type.sigma_sharp", "Sigma# listing");
    static final TextKey EXECUTABLE = TextKey.of("jsc.pace.type.executable", "Executable");
    static final TextKey STANDARD_FILE = TextKey.of("jsc.pace.type.standard", "Standard file");

    // The line above the keys.
    static final TextKey MOVE_AND_ENTER = TextKey.of("jsc.pace.say.move", "Move to an item with the arrow keys and"
            + " press ENTER.");
    static final TextKey SAYS_DIRECTORY = TextKey.of("jsc.pace.say.directory", "%s: a directory. ENTER opens it.");
    static final TextKey SAYS_SIGMA = TextKey.of("jsc.pace.say.sigma", "%s: a program in Sigma. ENTER opens it;"
            + " CMD-MENU runs it.");
    static final TextKey SAYS_SIGMA_SHARP = TextKey.of("jsc.pace.say.sigma_sharp", "%s: a program in Sigma#. ENTER"
            + " opens it.");
    static final TextKey SAYS_EXECUTABLE = TextKey.of("jsc.pace.say.executable", "%s: a program. ENTER runs it.");
    static final TextKey SAYS_FILE = TextKey.of("jsc.pace.say.file", "%s: a standard file. ENTER opens it.");
    static final TextKey SAYS_NOTHING_HERE = TextKey.of("jsc.pace.say.nothing", "Nothing to set on this machine.");
    static final TextKey SAYS_COMMAND = TextKey.of("jsc.pace.say.command", "Type a UNIX command and press ENTER.");
    static final TextKey RETURN = TextKey.of("jsc.pace.return", "Press ENTER to return to PACE.");

    // The function keys, eight to a row.
    static final TextKey KEY_HELP = TextKey.of("jsc.pace.key.help", "HELP");
    static final TextKey KEY_ENTER = TextKey.of("jsc.pace.key.enter", "ENTER");
    static final TextKey KEY_PREV = TextKey.of("jsc.pace.key.prev", "PREV-FRM");
    static final TextKey KEY_NEXT = TextKey.of("jsc.pace.key.next", "NEXT-FRM");
    static final TextKey KEY_CANCEL = TextKey.of("jsc.pace.key.cancel", "CANCEL");
    static final TextKey KEY_COMMANDS = TextKey.of("jsc.pace.key.commands", "CMD-MENU");

    // The command menu.
    static final TextKey RUN = TextKey.of("jsc.pace.command.run", "run");
    static final TextKey CANCEL = TextKey.of("jsc.pace.command.cancel", "cancel");
    static final TextKey HELP = TextKey.of("jsc.pace.command.help", "help");
    static final TextKey NEXT = TextKey.of("jsc.pace.command.next", "next-frm");
    static final TextKey PREV = TextKey.of("jsc.pace.command.prev", "prev-frm");
    static final TextKey REFRESH = TextKey.of("jsc.pace.command.refresh", "refresh");
    static final TextKey UNIX = TextKey.of("jsc.pace.command.unix", "unix-system");
    static final TextKey QUIT = TextKey.of("jsc.pace.command.exit", "exit");

    // Help.
    static final TextKey HELP_MAIN = TextKey.of("jsc.pace.help.main", "PACE opens a frame for each choice, over"
            + " the frame you chose it in.\nThe Office of the player holds the Filecabinet, your home.\nPrograms"
            + " lists what is installed; System Administration\nlooks at the machine; UNIX System gives you a shell,"
            + "\nand exit at it brings you back here.");
    static final TextKey HELP_FRAMES = TextKey.of("jsc.pace.help.frames", "The arrows move to an item and ENTER"
            + " opens it.\nF3 PREV-FRM and F4 NEXT-FRM move between the frames;\nF5 CANCEL closes the frame on top."
            + "\nF6 CMD-MENU opens the commands, or runs a Sigma listing.");

    private PaceTexts() {
    }
}
