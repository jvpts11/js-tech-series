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
 * What the Transition faces of KDE and GNOME say: KDE 4's Kickoff and its desktop, and GNOME 2's menus along the top.
 * Programs' names are data. Kept apart from the panels so the language generator can read it on a server too.
 */
@TextHolder
final class TransitionTexts {

    // KDE 4's Kickoff: its tabs, its header and the rows of the tabs that are not programs.
    static final TextKey FAVORITES = TextKey.of("jsc.desktop.kde4.favorites", "Favorites");
    static final TextKey APPLICATIONS = TextKey.of("jsc.desktop.kde4.applications", "Applications");
    static final TextKey COMPUTER = TextKey.of("jsc.desktop.kde4.computer", "Computer");
    static final TextKey RECENTLY_USED = TextKey.of("jsc.desktop.kde4.recently_used", "Recently Used");
    static final TextKey LEAVE = TextKey.of("jsc.desktop.kde4.leave", "Leave");
    static final TextKey SEARCH = TextKey.of("jsc.desktop.kde4.search", "Search:");
    static final TextKey NOTHING_RECENT = TextKey.of("jsc.desktop.kde4.nothing_recent", "Nothing used yet");
    static final TextKey SHUT_DOWN = TextKey.of("jsc.desktop.kde4.shut_down", "Shut Down");
    static final TextKey SHUT_DOWN_LINE = TextKey.of("jsc.desktop.kde4.shut_down_line", "Turn off the computer");
    static final TextKey RESTART = TextKey.of("jsc.desktop.kde4.restart", "Restart");
    static final TextKey RESTART_LINE = TextKey.of("jsc.desktop.kde4.restart_line", "Restart the computer");
    static final TextKey SYSTEM_SETTINGS_LINE = TextKey.of("jsc.desktop.kde4.system_settings_line",
            "Configure the computer");
    static final TextKey HOME = TextKey.of("jsc.desktop.kde4.home", "Home");
    static final TextKey HOME_LINE = TextKey.of("jsc.desktop.kde4.home_line", "The files on the system disk");
    static final TextKey NETWORK_LINE = TextKey.of("jsc.desktop.kde4.network_line", "The machines on the network");
    static final TextKey TRASH_LINE = TextKey.of("jsc.desktop.kde4.trash_line", "What was deleted");
    // KDE 4's desktop: the Folder View's title and what the cashew in the corner offers.
    static final TextKey DESKTOP_FOLDER = TextKey.of("jsc.desktop.kde4.desktop_folder", "Desktop Folder");
    static final TextKey DESKTOP_SETTINGS = TextKey.of("jsc.desktop.kde4.desktop_settings", "Desktop Settings");

    // GNOME 2's three menus, the places, and what the System menu holds.
    static final TextKey PLACES = TextKey.of("jsc.desktop.gnome2.places", "Places");
    static final TextKey SYSTEM = TextKey.of("jsc.desktop.gnome2.system", "System");
    static final TextKey HOME_FOLDER = TextKey.of("jsc.desktop.gnome2.home_folder", "Home Folder");
    static final TextKey DESKTOP = TextKey.of("jsc.desktop.gnome2.desktop", "Desktop");
    static final TextKey PREFERENCES = TextKey.of("jsc.desktop.gnome2.preferences", "Preferences");
    static final TextKey ADMINISTRATION = TextKey.of("jsc.desktop.gnome2.administration", "Administration");
    static final TextKey SHUT_DOWN_ELLIPSIS = TextKey.of("jsc.desktop.gnome2.shut_down", "Shut Down...");
    // GNOME 2's Applications menu, by what a program is for.
    static final TextKey ACCESSORIES = TextKey.of("jsc.desktop.gnome2.accessories", "Accessories");
    static final TextKey GAMES = TextKey.of("jsc.desktop.gnome2.games", "Games");
    static final TextKey GRAPHICS = TextKey.of("jsc.desktop.gnome2.graphics", "Graphics");
    static final TextKey INTERNET = TextKey.of("jsc.desktop.gnome2.internet", "Internet");
    static final TextKey PROGRAMMING = TextKey.of("jsc.desktop.gnome2.programming", "Programming");
    static final TextKey SOUND_AND_VIDEO = TextKey.of("jsc.desktop.gnome2.sound_and_video", "Sound & Video");
    static final TextKey SYSTEM_TOOLS = TextKey.of("jsc.desktop.gnome2.system_tools", "System Tools");

    private TransitionTexts() {
    }
}
