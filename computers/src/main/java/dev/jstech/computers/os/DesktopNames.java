/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What each desktop calls the programs it bundles, when it calls them something of its own: Dolphin and Konsole on
 * KDE Plasma, the File Manager on CDE. A name that is a product's (Dolphin, Kate, Nemo) reads the same in every
 * language; a plain one (Text Editor, System Settings) is translated like any other word a player reads.
 */
@TextHolder
final class DesktopNames {

    static final TextKey MEGASHELL = TextKey.of("jsc.desktop.frames_11.command_prompt", "Megashell");

    static final TextKey KDE_FILES = TextKey.of("jsc.desktop.kde_plasma.files", "Dolphin");
    static final TextKey KDE_EDITOR = TextKey.of("jsc.desktop.kde_plasma.editor", "Kate");
    static final TextKey KDE_TERMINAL = TextKey.of("jsc.desktop.kde_plasma.command_prompt", "Konsole");
    static final TextKey KDE_CALCULATOR = TextKey.of("jsc.desktop.kde_plasma.calculator", "KCalc");
    static final TextKey KDE_MONITOR = TextKey.of("jsc.desktop.kde_plasma.system_monitor", "System Monitor");
    static final TextKey KDE_SETTINGS = TextKey.of("jsc.desktop.kde_plasma.settings", "System Settings");
    static final TextKey KDE_THIS_PC = TextKey.of("jsc.desktop.kde_plasma.this_pc", "Info Center");

    static final TextKey GNOME_FILES = TextKey.of("jsc.desktop.gnome.files", "Files");
    static final TextKey GNOME_EDITOR = TextKey.of("jsc.desktop.gnome.editor", "Text Editor");
    static final TextKey GNOME_TERMINAL = TextKey.of("jsc.desktop.gnome.command_prompt", "Terminal");
    static final TextKey GNOME_CALCULATOR = TextKey.of("jsc.desktop.gnome.calculator", "Calculator");
    static final TextKey GNOME_MONITOR = TextKey.of("jsc.desktop.gnome.system_monitor", "System Monitor");
    static final TextKey GNOME_SETTINGS = TextKey.of("jsc.desktop.gnome.settings", "Settings");
    static final TextKey GNOME_THIS_PC = TextKey.of("jsc.desktop.gnome.this_pc", "About");

    static final TextKey CINNAMON_FILES = TextKey.of("jsc.desktop.cinnamon.files", "Nemo");
    static final TextKey CINNAMON_EDITOR = TextKey.of("jsc.desktop.cinnamon.editor", "xed");
    static final TextKey CINNAMON_TERMINAL = TextKey.of("jsc.desktop.cinnamon.command_prompt", "Terminal");
    static final TextKey CINNAMON_CALCULATOR = TextKey.of("jsc.desktop.cinnamon.calculator", "Calculator");
    static final TextKey CINNAMON_MONITOR = TextKey.of("jsc.desktop.cinnamon.system_monitor", "System Monitor");
    static final TextKey CINNAMON_SETTINGS = TextKey.of("jsc.desktop.cinnamon.settings", "System Settings");
    static final TextKey CINNAMON_THIS_PC = TextKey.of("jsc.desktop.cinnamon.this_pc", "System Info");

    static final TextKey CDE_FILES = TextKey.of("jsc.desktop.cde.files", "File Manager");
    static final TextKey CDE_EDITOR = TextKey.of("jsc.desktop.cde.editor", "Text Editor");
    static final TextKey CDE_TERMINAL = TextKey.of("jsc.desktop.cde.command_prompt", "Terminal");
    static final TextKey CDE_CALCULATOR = TextKey.of("jsc.desktop.cde.calculator", "Calculator");
    static final TextKey CDE_MONITOR = TextKey.of("jsc.desktop.cde.system_monitor", "Performance Meter");
    static final TextKey CDE_SETTINGS = TextKey.of("jsc.desktop.cde.settings", "Style Manager");

    private DesktopNames() {
    }
}
