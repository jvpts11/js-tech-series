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
 * What the Settings window says: its pages, their headings and captions, and every button and value it shows.
 * The machine's, the systems', the disks', the folders' and the programs' names are data, and so are sizes and
 * paths. Kept apart from the window so the language generator can read it on a server too, where windows do not
 * exist.
 */
@TextHolder
final class SettingsTexts {

    // The pages.
    static final TextKey PERSONALIZE = TextKey.of("jsc.settings.personalize", "Personalize");
    static final TextKey SYSTEM = TextKey.of("jsc.settings.system", "System");
    static final TextKey NETWORK = TextKey.of("jsc.settings.network", "Network");
    static final TextKey STORAGE = TextKey.of("jsc.settings.storage", "Storage");
    static final TextKey DISPLAY = TextKey.of("jsc.settings.display", "Display");
    static final TextKey PROGRAMS = TextKey.of("jsc.settings.programs", "Programs");
    static final TextKey SOUND = TextKey.of("jsc.settings.sound", "Sound");
    static final TextKey USERS = TextKey.of("jsc.settings.users", "Users");
    static final TextKey LOADING = TextKey.of("jsc.settings.loading", "Loading...");
    // The home Frames 7 and 10 open on: 7's Control Panel by category, 10's grid of pages.
    static final TextKey HOME = TextKey.of("jsc.settings.home", "Home");
    static final TextKey ADJUST_SETTINGS = TextKey.of("jsc.settings.adjust_settings",
            "Adjust your computer's settings");
    static final TextKey SYSTEM_AND_SECURITY = TextKey.of("jsc.settings.system_and_security", "System and Security");
    static final TextKey SYSTEM_LINE = TextKey.of("jsc.settings.system_line", "Look after the disks");
    static final TextKey USER_ACCOUNTS = TextKey.of("jsc.settings.user_accounts", "User Accounts");
    static final TextKey USERS_LINE = TextKey.of("jsc.settings.users_line", "The accounts on this computer");
    static final TextKey NETWORK_AND_INTERNET = TextKey.of("jsc.settings.network_and_internet",
            "Network and Internet");
    static final TextKey NETWORK_LINE = TextKey.of("jsc.settings.network_line", "Share folders, allow remote programs");
    static final TextKey APPEARANCE_AND_PERSONALIZATION = TextKey.of("jsc.settings.appearance_and_personalization",
            "Appearance and Personalization");
    static final TextKey APPEARANCE_LINE = TextKey.of("jsc.settings.appearance_line", "Change the theme");
    static final TextKey HARDWARE_AND_SOUND = TextKey.of("jsc.settings.hardware_and_sound", "Hardware and Sound");
    static final TextKey HARDWARE_LINE = TextKey.of("jsc.settings.hardware_line", "Adjust the display");
    static final TextKey PROGRAMS_LINE = TextKey.of("jsc.settings.programs_line", "Uninstall a program");
    static final TextKey EASE_OF_ACCESS = TextKey.of("jsc.settings.ease_of_access", "Ease of Access");
    static final TextKey EASE_LINE = TextKey.of("jsc.settings.ease_line", "Optimize visual display");
    static final TextKey FRAMES_SETTINGS = TextKey.of("jsc.settings.frames_settings", "Frames Settings");
    static final TextKey SYSTEM_ABOUT = TextKey.of("jsc.settings.system_about",
            "Computer name, processor, memory, graphics");
    static final TextKey DISPLAY_ABOUT = TextKey.of("jsc.settings.display_about",
            "Brightness, scale, the linked monitor");
    static final TextKey SOUND_ABOUT = TextKey.of("jsc.settings.sound_about", "Volume and the system sounds");
    static final TextKey NETWORK_ABOUT = TextKey.of("jsc.settings.network_about",
            "Public share, shared folders, remote programs");
    static final TextKey PERSONALIZE_ABOUT = TextKey.of("jsc.settings.personalize_about",
            "Wallpaper, accent, theme, clock, dark mode");
    static final TextKey STORAGE_ABOUT = TextKey.of("jsc.settings.storage_about", "Disks and the system disk");
    static final TextKey PROGRAMS_ABOUT = TextKey.of("jsc.settings.programs_about", "Installed programs, uninstall");
    static final TextKey USERS_ABOUT = TextKey.of("jsc.settings.users_about", "The accounts on this computer");
    static final TextKey COMING_SOON = TextKey.of("jsc.settings.coming_soon", "Coming in a future update");

    // Personalize.
    static final TextKey WALLPAPER = TextKey.of("jsc.settings.wallpaper", "Wallpaper");
    static final TextKey ACCENT = TextKey.of("jsc.settings.accent", "Accent");
    static final TextKey THEME = TextKey.of("jsc.settings.theme", "Theme");
    static final TextKey CLOCK = TextKey.of("jsc.settings.clock", "Clock");
    static final TextKey HOUR_24 = TextKey.of("jsc.settings.hour_24", "24-hour");
    static final TextKey HOUR_12 = TextKey.of("jsc.settings.hour_12", "12-hour");
    static final TextKey TASKBAR = TextKey.of("jsc.settings.taskbar", "Taskbar");
    static final TextKey CENTER = TextKey.of("jsc.settings.center", "Center");
    static final TextKey LEFT = TextKey.of("jsc.settings.left", "Left");
    static final TextKey APPEARANCE = TextKey.of("jsc.settings.appearance", "Appearance");
    static final TextKey LIGHT = TextKey.of("jsc.settings.light", "Light");
    static final TextKey DARK = TextKey.of("jsc.settings.dark", "Dark");

    // System.
    static final TextKey COMPUTER_NAME = TextKey.of("jsc.settings.computer_name", "Computer name");
    static final TextKey UNNAMED = TextKey.of("jsc.settings.unnamed", "(unnamed)");
    static final TextKey ABOUT = TextKey.of("jsc.settings.about", "About");
    static final TextKey PROCESSOR = TextKey.of("jsc.settings.processor", "Processor");
    static final TextKey PROCESSOR_VALUE = TextKey.of("jsc.settings.processor_value", "%s - %s MHz");
    static final TextKey ARCHITECTURE = TextKey.of("jsc.settings.architecture", "Architecture");
    static final TextKey MEMORY = TextKey.of("jsc.settings.memory", "Memory");
    static final TextKey MEGABYTES = TextKey.of("jsc.settings.megabytes", "%s MB");
    static final TextKey GRAPHICS = TextKey.of("jsc.settings.graphics", "Graphics");
    static final TextKey VRAM = TextKey.of("jsc.settings.vram", "%s MB VRAM");
    static final TextKey PLATFORM = TextKey.of("jsc.settings.platform", "Platform");
    static final TextKey RESTART_TO_FIRMWARE = TextKey.of("jsc.settings.restart_to_firmware", "Restart to firmware");
    // Frames 7's rating of the machine, its base score and the five it is the lowest of.
    static final TextKey RATING = TextKey.of("jsc.settings.rating", "Rating");
    static final TextKey PROCESSOR_SCORE = TextKey.of("jsc.settings.processor_score", "Processor %s");
    static final TextKey MEMORY_SCORE = TextKey.of("jsc.settings.memory_score", "Memory %s");
    static final TextKey DISK_SCORE = TextKey.of("jsc.settings.disk_score", "Disk %s");
    static final TextKey GRAPHICS_SCORE = TextKey.of("jsc.settings.graphics_score", "Graphics %s");
    static final TextKey GAMING_SCORE = TextKey.of("jsc.settings.gaming_score", "Gaming %s");

    // Network.
    static final TextKey PUBLIC_SHARE = TextKey.of("jsc.settings.public_share", "System disk public share");
    static final TextKey SHARE_AMOUNT = TextKey.of("jsc.settings.share_amount", "%s%% (%s/1000)");
    static final TextKey LINK = TextKey.of("jsc.settings.link", "Link: on the data network");
    static final TextKey SHARED_FOLDERS = TextKey.of("jsc.settings.shared_folders", "Shared folders");
    static final TextKey READ = TextKey.of("jsc.settings.read", "read");
    static final TextKey WRITE = TextKey.of("jsc.settings.write", "write");
    static final TextKey REMOVE = TextKey.of("jsc.settings.remove", "Remove");
    static final TextKey MORE_SHARES =
            TextKey.of("jsc.settings.more_shares", "and %s more: config share at the prompt lists them all");
    static final TextKey SHARE_HINT = TextKey.of("jsc.settings.share_hint", "folder to share, as C:\\pub");
    static final TextKey SHARE_READ_ONLY = TextKey.of("jsc.settings.share_read_only", "Share read-only");
    static final TextKey SHARE_FOR_WRITING = TextKey.of("jsc.settings.share_for_writing", "Share for writing");
    static final TextKey REACHED_AS = TextKey.of("jsc.settings.reached_as", "Others reach it as \\\\%s\\<share>,");
    static final TextKey CC_REACHES_AS = TextKey.of("jsc.settings.cc_reaches_as", "CC computers as /jsc/%s/<share>.");
    static final TextKey REMOTE_PROGRAMS = TextKey.of("jsc.settings.remote_programs", "Remote programs");
    static final TextKey ALLOWED = TextKey.of("jsc.settings.allowed", "Allowed");
    static final TextKey REFUSED = TextKey.of("jsc.settings.refused", "Refused");

    // Storage, Display and Programs.
    static final TextKey SYSTEM_DISK = TextKey.of("jsc.settings.system_disk", "%s  [sys]");
    static final TextKey NO_DISKS = TextKey.of("jsc.settings.no_disks", "No disks installed");
    static final TextKey BRIGHTNESS = TextKey.of("jsc.settings.brightness", "Brightness");
    static final TextKey SCALE = TextKey.of("jsc.settings.scale", "Scale");
    static final TextKey PERCENT = TextKey.of("jsc.settings.percent", "%s%%");
    static final TextKey MONITOR = TextKey.of("jsc.settings.monitor", "Monitor: linked display");
    static final TextKey NO_PROGRAMS = TextKey.of("jsc.settings.no_programs", "No programs installed");
    static final TextKey UNINSTALL = TextKey.of("jsc.settings.uninstall", "Uninstall");

    // Sound.
    static final TextKey VOLUME = TextKey.of("jsc.settings.volume", "Volume");
    static final TextKey MUTE = TextKey.of("jsc.settings.mute", "Mute");
    static final TextKey ON = TextKey.of("jsc.settings.on", "On");
    static final TextKey OFF = TextKey.of("jsc.settings.off", "Off");
    static final TextKey OUTPUT = TextKey.of("jsc.settings.output", "Output");
    static final TextKey OUTPUT_MONITOR = TextKey.of("jsc.settings.output_monitor", "Monitor");
    static final TextKey OUTPUT_SPEAKERS = TextKey.of("jsc.settings.output_speakers", "Speakers");
    static final TextKey OUTPUT_BOTH = TextKey.of("jsc.settings.output_both", "Both");
    static final TextKey SOUND_HARDWARE = TextKey.of("jsc.settings.sound_hardware", "Sound hardware");
    static final TextKey NO_SOUND_HARDWARE = TextKey.of("jsc.settings.no_sound_hardware", "No sound hardware");
    static final TextKey SPEAKERS = TextKey.of("jsc.settings.speakers", "Speakers");
    static final TextKey NO_SPEAKERS = TextKey.of("jsc.settings.no_speakers", "No speakers linked");
    /** A speaker nobody has named yet, the way its own screen calls it. */
    static final TextKey UNNAMED_SPEAKER = TextKey.of("jsc.settings.unnamed_speaker", "Speaker");
    static final TextKey SIDE_LEFT = TextKey.of("jsc.settings.side_left", "Left");
    static final TextKey SIDE_RIGHT = TextKey.of("jsc.settings.side_right", "Right");
    static final TextKey SIDE_BOTH = TextKey.of("jsc.settings.side_both", "Both sides");
    static final TextKey MORE_SPEAKERS = TextKey.of("jsc.settings.more_speakers", "and %s more");
    static final TextKey TEST = TextKey.of("jsc.settings.test", "Test");

    private SettingsTexts() {
    }
}
