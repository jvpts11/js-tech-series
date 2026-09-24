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
 * What a guided installer says on its pages: the welcome, the check, the disks, the name, the desktop, the copy and
 * the end, kept apart from the screen that draws them so the language generator can read them on a server too.
 */
@TextHolder
final class InstallerScreenTexts {

    // The welcome.
    static final TextKey PREPARES = TextKey.of("jsc.installer.screen.prepares",
            "Setup prepares %s to run on this computer.");
    static final TextKey TO_SET_UP = TextKey.of("jsc.installer.screen.to_set_up", "- To set up %s now, press ENTER.");
    static final TextKey TO_QUIT = TextKey.of("jsc.installer.screen.to_quit",
            "- To quit Setup without installing, press F3.");
    static final TextKey NO_ROOM_ANYWHERE =
            TextKey.of("jsc.installer.screen.no_room_anywhere", "No disk in this machine has room for it.");
    static final TextKey FOUND_DISK = TextKey.of("jsc.installer.screen.found_disk", "Setup found a disk for %s:");
    /* A disk by its slot and what its maker called it, with two spaces between. */
    static final TextKey DISK_WIDE = TextKey.of("jsc.installer.screen.disk_wide", "Disk %s  %s");
    static final TextKey HOLDS_FREE = TextKey.of("jsc.installer.screen.holds_free", "%s, %s free");

    // The check.
    static final TextKey READING = TextKey.of("jsc.installer.screen.reading", "reading ...");
    static final TextKey NO_DISK_WITH_ROOM = TextKey.of("jsc.installer.screen.no_disk_with_room", "no disk with room");
    static final TextKey GENERATION = TextKey.of("jsc.installer.screen.generation", "Generation");
    static final TextKey PROCESSOR = TextKey.of("jsc.installer.screen.processor", "Processor");
    static final TextKey MEMORY = TextKey.of("jsc.installer.screen.memory", "Memory");
    static final TextKey DISK = TextKey.of("jsc.installer.screen.disk", "Disk %s");
    static final TextKey FREE = TextKey.of("jsc.installer.screen.free", "%s free");
    static final TextKey INSTALLATION_MEDIUM =
            TextKey.of("jsc.installer.screen.installation_medium", "Installation medium");
    static final TextKey OK = TextKey.of("jsc.installer.screen.ok", "OK");
    static final TextKey NEEDS_ON_DISK = TextKey.of("jsc.installer.screen.needs_on_disk", "%s needs %s on a disk.");

    // The disks.
    static final TextKey COLUMN_DISK = TextKey.of("jsc.installer.screen.column_disk", "Disk");
    static final TextKey COLUMN_SIZE = TextKey.of("jsc.installer.screen.column_size", "Size");
    static final TextKey COLUMN_FREE = TextKey.of("jsc.installer.screen.column_free", "Free");
    static final TextKey COLUMN_HOLDS = TextKey.of("jsc.installer.screen.column_holds", "Holds");
    static final TextKey NOTHING = TextKey.of("jsc.installer.screen.nothing", "Nothing");
    static final TextKey DISK_NARROW = TextKey.of("jsc.installer.screen.disk_narrow", "Disk %s %s");
    static final TextKey NEEDS = TextKey.of("jsc.installer.screen.needs", "%s needs %s.");
    static final TextKey NO_ROOM_HERE = TextKey.of("jsc.installer.screen.no_room_here",
            "No room here. Erase this disk, or choose another.");
    static final TextKey ERASED_FIRST = TextKey.of("jsc.installer.screen.erased_first",
            "%s on this disk is erased first.");
    static final TextKey COMPUTER_NAME = TextKey.of("jsc.installer.screen.computer_name", "Computer name:");
    static final TextKey NO_SYSTEM = TextKey.of("jsc.installer.screen.no_system", "no system");

    // The name.
    static final TextKey NAME_HELP_FIRST =
            TextKey.of("jsc.installer.screen.name_help_first", "This name identifies the computer at the");
    static final TextKey NAME_HELP_SECOND =
            TextKey.of("jsc.installer.screen.name_help_second", "prompt and on the network.");

    // The desktop.
    static final TextKey MIRROR_FIRST = TextKey.of("jsc.installer.screen.mirror_first",
            "The Mirror on %s answers. Choose a desktop to install");
    static final TextKey MIRROR_SECOND = TextKey.of("jsc.installer.screen.mirror_second",
            "with %s, or none to boot to the terminal.");
    static final TextKey NO_MIRROR = TextKey.of("jsc.installer.screen.no_mirror",
            "No Mirror answers, so it comes up at its terminal.");
    static final TextKey TERMINAL_ONLY_CHOICE =
            TextKey.of("jsc.installer.screen.terminal_only_choice", "None, the terminal only");

    // The list of questions.
    static final TextKey HUB_FOOT = TextKey.of("jsc.installer.screen.hub_foot",
            "A letter begins the installation once nothing is still wanted.");
    static final TextKey NO_DISK_SELECTED = TextKey.of("jsc.installer.screen.no_disk_selected", "(no disk selected)");
    static final TextKey DISK_ANSWER = TextKey.of("jsc.installer.screen.disk_answer", "(Disk %s, %s)");
    static final TextKey NOT_SET = TextKey.of("jsc.installer.screen.not_set", "(not set)");
    static final TextKey ANSWER = TextKey.of("jsc.installer.screen.answer", "(%s)");
    static final TextKey TERMINAL_ONLY_ANSWER =
            TextKey.of("jsc.installer.screen.terminal_only_answer", "(the terminal only)");

    // The copy.
    static final TextKey COPYING = TextKey.of("jsc.installer.screen.copying", "Setup is copying %s to Disk %s, %s.");
    static final TextKey COMPLETE = TextKey.of("jsc.installer.screen.complete", "%s%% complete");
    static final TextKey SECONDS_LEFT = TextKey.of("jsc.installer.screen.seconds_left", "About %s seconds left");
    static final TextKey READING_MEDIUM = TextKey.of("jsc.installer.screen.reading_medium", "Reading %s. Leave it in.");
    static final TextKey DONE = TextKey.of("jsc.installer.screen.done", "done");
    static final TextKey SECONDS_LEFT_MEDIUM = TextKey.of("jsc.installer.screen.seconds_left_medium",
            "About %s seconds left. Leave the medium in.");
    static final TextKey RESTARTS_WHEN_FINISHED = TextKey.of("jsc.installer.screen.restarts_when_finished",
            "Your PC restarts when setup is finished.");
    static final TextKey SECONDS_LEFT_SENTENCE =
            TextKey.of("jsc.installer.screen.seconds_left_sentence", "About %s seconds left.");
    static final TextKey INSTALLING_ON = TextKey.of("jsc.installer.screen.installing_on", "Installing on Disk %s, %s");
    static final TextKey THE_DRIVE = TextKey.of("jsc.installer.screen.the_drive", "the %s");
    static final TextKey THE_INSTALLATION_MEDIUM =
            TextKey.of("jsc.installer.screen.the_installation_medium", "the installation medium");

    // The end, and the question before a disk is erased.
    static final TextKey INSTALLED = TextKey.of("jsc.installer.screen.installed", "%s is installed.");
    static final TextKey TAKE_OUT_FIRST = TextKey.of("jsc.installer.screen.take_out_first",
            "Take the installation medium out of the drive,");
    static final TextKey TAKE_OUT_SECOND =
            TextKey.of("jsc.installer.screen.take_out_second", "or the machine starts Setup again.");
    static final TextKey RESTART_TO_START = TextKey.of("jsc.installer.screen.restart_to_start", "Restart to start %s.");
    static final TextKey ERASE_ASK = TextKey.of("jsc.installer.screen.erase_ask", "Erase Disk %s?");
    static final TextKey AND_EVERY_FILE = TextKey.of("jsc.installer.screen.and_every_file", "%s and every file on");
    static final TextKey EVERY_FILE = TextKey.of("jsc.installer.screen.every_file", "Every file on");
    static final TextKey WILL_BE_DELETED = TextKey.of("jsc.installer.screen.will_be_deleted", "%s will be deleted.");
    static final TextKey CANNOT_UNDO = TextKey.of("jsc.installer.screen.cannot_undo", "This cannot be undone.");
    static final TextKey ERASE_KEYS = TextKey.of("jsc.installer.screen.erase_keys", "Y = erase        N = cancel");

    private InstallerScreenTexts() {
    }
}
