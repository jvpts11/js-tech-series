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
 * What the desktop itself says, around the programs it runs: its errors and notices, the crash of a system that
 * ran out of memory, the menus of the wallpaper, of an icon and of a panel entry, and the power choices. File and
 * program names are data. Kept apart from the desktop so the language generator can read it on a server too.
 */
@TextHolder
final class DesktopTexts {

    // Errors and notices.
    static final TextKey ERROR = TextKey.of("jsc.desktop.error", "Error");
    static final TextKey DAT_LOCKED = TextKey.of("jsc.desktop.dat_locked",
            "This file is impossible to modify, create, delete or change manually, use the network interactor for"
                    + " it.");
    static final TextKey INSTALLER_LOCKED = TextKey.of("jsc.desktop.installer_locked",
            "This file is part of the installer and cannot be changed, copied or deleted. Run the setup program to"
                    + " install it.");
    static final TextKey LOW_MEMORY = TextKey.of("jsc.desktop.low_memory", "Low on memory");
    static final TextKey LOW_MEMORY_BODY = TextKey.of("jsc.desktop.low_memory_body",
            "This computer is running out of RAM for programs. %s needs %s MB and only %s MB are free.");
    static final TextKey LOW_VIDEO_MEMORY = TextKey.of("jsc.desktop.low_video_memory", "Not enough video memory");
    static final TextKey LOW_VIDEO_MEMORY_BODY = TextKey.of("jsc.desktop.low_video_memory_body",
            "%s needs %s of video memory, and %s are free. Turn off a monitor or close a graphics program.");
    static final TextKey CANNOT_OPEN = TextKey.of("jsc.desktop.cannot_open", "Cannot open");
    static final TextKey NO_PROGRAM_OPENS =
            TextKey.of("jsc.desktop.no_program_opens", "No program on this computer opens %s");

    // The crash of a system that shares its memory with every program, one line of the screen to a line here.
    static final TextKey CRASH_FATAL = TextKey.of("jsc.desktop.crash.fatal", "A fatal exception has occurred.");
    static final TextKey CRASH_CAUSE = TextKey.of("jsc.desktop.crash.cause",
            "This computer ran out of memory with too many\nprograms open, and the system became unstable.");
    static final TextKey CRASH_NO_RECOVERY =
            TextKey.of("jsc.desktop.crash.no_recovery", "The cooperative kernel cannot recover.");
    static final TextKey REBOOTING = TextKey.of("jsc.desktop.crash.rebooting", "Rebooting...");

    // The panel's own menu.
    static final TextKey CASCADE = TextKey.of("jsc.desktop.panel.cascade", "Cascade Windows");
    static final TextKey SHOW_DESKTOP = TextKey.of("jsc.desktop.panel.show_desktop", "Show the Desktop");
    static final TextKey TASK_MANAGER = TextKey.of("jsc.desktop.panel.task_manager", "Task Manager");

    // The menus of the wallpaper, of an icon and of a file.
    static final TextKey OPEN = TextKey.of("jsc.desktop.open", "Open");
    static final TextKey OPEN_WITH = TextKey.of("jsc.desktop.open_with", "Open with");
    static final TextKey CHOOSE_ANOTHER = TextKey.of("jsc.desktop.choose_another", "Choose another program...");
    static final TextKey NO_PROGRAM_OPENS_THIS =
            TextKey.of("jsc.desktop.no_program_opens_this", "No program opens this");
    static final TextKey PIN = TextKey.of("jsc.desktop.pin", "Pin to taskbar");
    static final TextKey UNPIN = TextKey.of("jsc.desktop.unpin", "Unpin from taskbar");
    static final TextKey UNINSTALL = TextKey.of("jsc.desktop.uninstall", "Uninstall");
    static final TextKey RENAME = TextKey.of("jsc.desktop.rename", "Rename");
    static final TextKey DELETE = TextKey.of("jsc.desktop.delete", "Delete");
    static final TextKey PROPERTIES = TextKey.of("jsc.desktop.properties", "Properties");
    static final TextKey NEW = TextKey.of("jsc.desktop.new", "New");
    static final TextKey FOLDER = TextKey.of("jsc.desktop.folder", "Folder");
    static final TextKey REFRESH = TextKey.of("jsc.desktop.refresh", "Refresh");
    static final TextKey DISPLAY_SETTINGS = TextKey.of("jsc.desktop.display_settings", "Display settings");
    static final TextKey PERSONALIZE = TextKey.of("jsc.desktop.personalize", "Personalize");
    static final TextKey EXTRACT_HERE = TextKey.of("jsc.desktop.extract_here", "Extract here");
    static final TextKey COMPRESS_TO = TextKey.of("jsc.desktop.compress_to", "Compress to %s");

    // A program's menu on its panel entry.
    static final TextKey RESTORE_ALL = TextKey.of("jsc.desktop.task.restore_all", "Restore all");
    static final TextKey RESTORE = TextKey.of("jsc.desktop.task.restore", "Restore");
    static final TextKey BRING_TO_FRONT = TextKey.of("jsc.desktop.task.bring_to_front", "Bring to front");
    static final TextKey MINIMIZE_ALL = TextKey.of("jsc.desktop.task.minimize_all", "Minimize all");
    static final TextKey MINIMIZE = TextKey.of("jsc.desktop.task.minimize", "Minimize");
    static final TextKey MAXIMIZE = TextKey.of("jsc.desktop.task.maximize", "Maximize");
    static final TextKey MINIMIZE_OTHERS = TextKey.of("jsc.desktop.task.minimize_others", "Minimize others");
    static final TextKey CLOSE_ALL_WINDOWS = TextKey.of("jsc.desktop.task.close_all_windows", "Close all windows");
    static final TextKey CLOSE = TextKey.of("jsc.desktop.task.close", "Close");

    // The power choices, and the account a start menu names.
    static final TextKey POWER = TextKey.of("jsc.desktop.power", "Power");
    static final TextKey SHUT_DOWN = TextKey.of("jsc.desktop.power.shut_down", "Shut down");
    static final TextKey SHUT_DOWN_HINT = TextKey.of("jsc.desktop.power.shut_down_hint", "the machine powers off");
    static final TextKey RESTART = TextKey.of("jsc.desktop.power.restart", "Restart");
    static final TextKey RESTART_HINT =
            TextKey.of("jsc.desktop.power.restart_hint", "power-cycle, back at the POST");
    static final TextKey LOG_OFF = TextKey.of("jsc.desktop.power.log_off", "Log off");
    static final TextKey LOG_OFF_HINT =
            TextKey.of("jsc.desktop.power.log_off_hint", "leave the screen, keep it running");
    static final TextKey TURN_OFF_COMPUTER = TextKey.of("jsc.desktop.xp.turn_off_computer", "Turn Off Computer");
    static final TextKey XP_LOG_OFF = TextKey.of("jsc.desktop.xp.log_off", "Log Off");
    static final TextKey LOCAL_ACCOUNT = TextKey.of("jsc.desktop.local_account", "Local account");

    // The start menus.
    static final TextKey START_SHUT_DOWN = TextKey.of("jsc.desktop.start.shut_down", "Shut Down");
    static final TextKey ALL_PROGRAMS = TextKey.of("jsc.desktop.start.all_programs", "All Programs");
    static final TextKey SEARCH_HINT = TextKey.of("jsc.desktop.start.search_hint", "Type here to search");
    static final TextKey PINNED = TextKey.of("jsc.desktop.start.pinned", "Pinned");
    static final TextKey NO_RESULTS = TextKey.of("jsc.desktop.start.no_results", "No results");
    static final TextKey BEST_MATCH = TextKey.of("jsc.desktop.start.best_match", "Best match");
    static final TextKey TYPE_TO_SEARCH = TextKey.of("jsc.desktop.start.type_to_search", "Type to search");
    static final TextKey TYPE_TO_SEARCH_MORE = TextKey.of("jsc.desktop.start.type_to_search_more", "Type to search...");
    static final TextKey SEARCH = TextKey.of("jsc.desktop.start.search", "Search");
    static final TextKey APPLICATIONS = TextKey.of("jsc.desktop.start.applications", "Applications");
    static final TextKey SLEEP = TextKey.of("jsc.desktop.start.sleep", "Sleep");
    static final TextKey FAVORITES = TextKey.of("jsc.desktop.start.favorites", "Favorites");
    static final TextKey ALL_APPS = TextKey.of("jsc.desktop.start.all_apps", "All Apps");
    static final TextKey SYSTEM = TextKey.of("jsc.desktop.start.system", "System");
    static final TextKey UTILITIES = TextKey.of("jsc.desktop.start.utilities", "Utilities");
    static final TextKey ALL = TextKey.of("jsc.desktop.start.all", "All");
    static final TextKey ACCESSORIES = TextKey.of("jsc.desktop.start.accessories", "Accessories");
    // Frames 7's Start menu.
    static final TextKey SEARCH_PROGRAMS_AND_FILES = TextKey.of("jsc.desktop.start.search_programs_and_files",
            "Search programs and files");
    // Frames 10's Start menu: its list, its tiles and what the right button offers on them.
    static final TextKey MOST_USED = TextKey.of("jsc.desktop.start.most_used", "Most used");
    static final TextKey LIFE_AT_A_GLANCE = TextKey.of("jsc.desktop.start.life_at_a_glance", "Life at a glance");
    static final TextKey PIN_TO_START = TextKey.of("jsc.desktop.start.pin_to_start", "Pin to Start");
    static final TextKey UNPIN_FROM_START = TextKey.of("jsc.desktop.start.unpin_from_start", "Unpin from Start");
    static final TextKey TILE_SMALL = TextKey.of("jsc.desktop.start.tile_small", "Resize: Small");
    static final TextKey TILE_MEDIUM = TextKey.of("jsc.desktop.start.tile_medium", "Resize: Medium");
    static final TextKey TILE_WIDE = TextKey.of("jsc.desktop.start.tile_wide", "Resize: Wide");
    static final TextKey TILE_ONLINE = TextKey.of("jsc.desktop.start.tile_online", "Connected to a network");
    static final TextKey TILE_OFFLINE = TextKey.of("jsc.desktop.start.tile_offline", "No network");
    static final TextKey TILE_CPU = TextKey.of("jsc.desktop.start.tile_cpu", "CPU %s%%");
    static final TextKey TILE_MEMORY = TextKey.of("jsc.desktop.start.tile_memory", "Memory %s");
    // Frames 10's Action Center and its quick actions.
    static final TextKey NOTIFICATIONS = TextKey.of("jsc.desktop.action_center.notifications", "Notifications");
    static final TextKey CLEAR_ALL = TextKey.of("jsc.desktop.action_center.clear_all", "Clear all");
    static final TextKey NO_NOTIFICATIONS = TextKey.of("jsc.desktop.action_center.no_notifications",
            "No new notifications");
    static final TextKey ALL_SETTINGS = TextKey.of("jsc.desktop.action_center.all_settings", "All settings");
    static final TextKey QUICK_NETWORK = TextKey.of("jsc.desktop.action_center.network", "Network");
    static final TextKey QUIET_HOURS = TextKey.of("jsc.desktop.action_center.quiet_hours", "Quiet hours");
    static final TextKey QUICK_SOUND = TextKey.of("jsc.desktop.action_center.sound", "Sound");
    // Frames 10's Task View.
    static final TextKey NO_WINDOWS = TextKey.of("jsc.desktop.task_view.no_windows", "No windows are open");
    static final TextKey OFFICE = TextKey.of("jsc.desktop.start.office", "Office");
    static final TextKey PREFERENCES = TextKey.of("jsc.desktop.start.preferences", "Preferences");

    // A question over the whole desktop, and a note closed with OK.
    static final TextKey QUESTION_OK = TextKey.of("jsc.desktop.question.ok", "OK");
    static final TextKey QUESTION_YES = TextKey.of("jsc.desktop.question.yes", "Yes");
    static final TextKey QUESTION_NO = TextKey.of("jsc.desktop.question.no", "No");

    /*
     * What a new file or folder is called until the player names it, in the player's language, the way every
     * desktop names one. Once made, the name is the file's own and stays as it was made.
     */
    static final TextKey NEW_FILE = TextKey.of("jsc.desktop.new_file", "New File");
    static final TextKey NEW_FOLDER = TextKey.of("jsc.desktop.new_folder", "New Folder");
    static final TextKey NEW_FOLDER_NUMBERED = TextKey.of("jsc.desktop.new_folder_numbered", "New Folder (%s)");

    /** The plain editor, as a menu that offers programs to open a file with names it. */
    static final TextKey EDITOR = TextKey.of("jsc.desktop.editor", "Editor");

    private DesktopTexts() {
    }
}
