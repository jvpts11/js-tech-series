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
 * What This PC says in words of its own: the machine's card, its sections, each drive's row and tooltip, and the
 * hardware list. The machine's, the disks', the media's, the systems' and the programs' names are data, and so are
 * counts, clocks and drive letters. Kept apart from the window so the language generator can read it on a server
 * too, where windows do not exist.
 */
@TextHolder
final class ThisPcTexts {

    static final TextKey TITLE = TextKey.of("jsc.this_pc.title", "This PC");

    // The machine's card.
    static final TextKey RENAME = TextKey.of("jsc.this_pc.rename", "Rename");
    static final TextKey ERA = TextKey.of("jsc.this_pc.era", "%s era");
    static final TextKey KIND_AND_ERA = TextKey.of("jsc.this_pc.kind_and_era", "%s · %s era");
    static final TextKey NO_SYSTEM = TextKey.of("jsc.this_pc.no_system", "No system installed");
    static final TextKey NOT_ON_NETWORK = TextKey.of("jsc.this_pc.not_on_network", "not on a network");
    static final TextKey ON_NETWORK = TextKey.of("jsc.this_pc.on_network", "network %s");

    // The sections.
    static final TextKey DEVICES_AND_DRIVES = TextKey.of("jsc.this_pc.devices_and_drives", "Devices and drives");
    static final TextKey NO_DRIVES =
            TextKey.of("jsc.this_pc.no_drives", "No disks installed and no drives linked");
    static final TextKey HARDWARE = TextKey.of("jsc.this_pc.hardware", "Hardware");
    static final TextKey INSTALLED_PROGRAMS = TextKey.of("jsc.this_pc.installed_programs", "Installed programs  %s");
    static final TextKey NO_PROGRAMS =
            TextKey.of("jsc.this_pc.no_programs", "None. Insert an installer, or run a package manager.");

    // A drive's row.
    static final TextKey OPEN = TextKey.of("jsc.this_pc.open", "Open");
    static final TextKey INSTALL = TextKey.of("jsc.this_pc.install", "Install");
    static final TextKey EJECT = TextKey.of("jsc.this_pc.eject", "Eject");
    static final TextKey SYSTEM = TextKey.of("jsc.this_pc.system", "System");
    static final TextKey SYSTEM_IS = TextKey.of("jsc.this_pc.system_is", "System · %s");
    static final TextKey FREE = TextKey.of("jsc.this_pc.free", "%s of %s it free");
    static final TextKey FLOPPY = TextKey.of("jsc.this_pc.floppy", "Floppy");
    static final TextKey CD = TextKey.of("jsc.this_pc.cd", "CD");
    static final TextKey DVD = TextKey.of("jsc.this_pc.dvd", "DVD");
    static final TextKey USB = TextKey.of("jsc.this_pc.usb", "USB");
    static final TextKey NO_DISC = TextKey.of("jsc.this_pc.no_disc", "no disc");
    static final TextKey DRIVE_AWAY = TextKey.of("jsc.this_pc.drive_away", "%s drive, %s blocks away");
    static final TextKey INSTALLS = TextKey.of("jsc.this_pc.installs", "Installs %s");
    static final TextKey INSTALLED = TextKey.of("jsc.this_pc.installed", "Installed: %s");
    static final TextKey BOOTABLE = TextKey.of("jsc.this_pc.bootable", "bootable");
    static final TextKey PACKAGE = TextKey.of("jsc.this_pc.package", "package %s");
    static final TextKey DATA_MEDIUM = TextKey.of("jsc.this_pc.data_medium", "Data medium · %s stored");

    // A disk's tooltip.
    static final TextKey SYSTEM_NAMED = TextKey.of("jsc.this_pc.system_named", "System: %s");
    static final TextKey SHARE_SYSTEM = TextKey.of("jsc.this_pc.share_system", "  system   %s it");
    static final TextKey SHARE_ITEMS = TextKey.of("jsc.this_pc.share_items", "  items    %s it");
    static final TextKey SHARE_FILES = TextKey.of("jsc.this_pc.share_files", "  files    %s it");
    static final TextKey SHARE_FREE = TextKey.of("jsc.this_pc.share_free", "  free     %s it");

    // The hardware.
    static final TextKey BOARD = TextKey.of("jsc.this_pc.board", "Board");
    static final TextKey PROCESSOR = TextKey.of("jsc.this_pc.processor", "Processor");
    static final TextKey ARCHITECTURE = TextKey.of("jsc.this_pc.architecture", "Architecture");
    static final TextKey MEMORY = TextKey.of("jsc.this_pc.memory", "Memory");
    static final TextKey GRAPHICS = TextKey.of("jsc.this_pc.graphics", "Graphics");
    static final TextKey POWER = TextKey.of("jsc.this_pc.power", "Power");
    static final TextKey PERIPHERALS = TextKey.of("jsc.this_pc.peripherals", "Peripherals");
    static final TextKey BUILD = TextKey.of("jsc.this_pc.build", "Build");
    static final TextKey NONE = TextKey.of("jsc.this_pc.none", "none");
    static final TextKey MEMORY_VALUE = TextKey.of("jsc.this_pc.memory_value", "%s it");
    static final TextKey GRAPHICS_VALUE = TextKey.of("jsc.this_pc.graphics_value", "%s × %s MB VRAM");
    static final TextKey NONE_LINKED = TextKey.of("jsc.this_pc.none_linked", "none linked");
    static final TextKey COMES_UP = TextKey.of("jsc.this_pc.comes_up", "OK, the machine comes up");
    static final TextKey NOT_VALID = TextKey.of("jsc.this_pc.not_valid", "not valid");

    private ThisPcTexts() {
    }
}
