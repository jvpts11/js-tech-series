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

    private SettingsTexts() {
    }
}
