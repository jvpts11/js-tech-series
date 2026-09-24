/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a machine's firmware says on the glass: the boot menus, the self-test, the setup and the installer's pages.
 *
 * <p>Kept apart from the screens that draw it, since the language generator loads every class that declares sentences,
 * on a server as well, where screens do not exist.
 */
@TextHolder
public final class FirmwareScreenTexts {

    static final TextKey DISK = TextKey.of("jsc.firmware.screen.disk", "Disk %s");
    static final TextKey DISK_DEVICE = TextKey.of("jsc.firmware.screen.disk_device", "Disk %s %s");
    static final TextKey BOOT_MENU = TextKey.of("jsc.firmware.screen.boot_menu", "Boot Menu");
    static final TextKey BOOT_MENU_THIS_BOOT =
            TextKey.of("jsc.firmware.screen.boot_menu_this_boot", "Boot Menu  (this boot only)");
    static final TextKey THIS_BOOT_ONLY = TextKey.of("jsc.firmware.screen.this_boot_only", "this boot only");
    static final TextKey KEYS_TUBE = TextKey.of("jsc.firmware.screen.keys_tube", "Up/Down  select     Enter  boot");
    static final TextKey KEYS_BLUE = TextKey.of("jsc.firmware.screen.keys_blue", "Up/Down: Select     Enter: Boot");
    static final TextKey KEYS_DIALOG = TextKey.of("jsc.firmware.screen.keys_dialog", "Up/Down Select   Enter Boot");

    // The self-test.
    static final TextKey ENTERING_SETUP = TextKey.of("jsc.firmware.screen.entering_setup", "Entering SETUP ...");
    static final TextKey KEYS_POST = TextKey.of("jsc.firmware.screen.keys_post", "DEL  Setup      F12  Boot Menu");
    /* The hint of the boards that printed it, around the two keys lifted out of it. */
    static final TextKey HINT_PRESS = TextKey.of("jsc.firmware.screen.hint_press", "Press ");
    static final TextKey HINT_SETUP = TextKey.of("jsc.firmware.screen.hint_setup", " to enter SETUP, ");
    static final TextKey HINT_BOOT_MENU = TextKey.of("jsc.firmware.screen.hint_boot_menu", " for Boot Menu");
    static final TextKey READING_CONFIG =
            TextKey.of("jsc.firmware.screen.reading_config", "Reading system configuration ...");
    static final TextKey MAIN_PROCESSOR = TextKey.of("jsc.firmware.screen.main_processor", "Main Processor : ");
    static final TextKey PROCESSOR = TextKey.of("jsc.firmware.screen.processor", "Processor : ");
    static final TextKey NOT_DETECTED = TextKey.of("jsc.firmware.screen.not_detected", "not detected");
    static final TextKey BOARD = TextKey.of("jsc.firmware.screen.board", "Board     : %s");
    static final TextKey VIDEO_ADAPTER = TextKey.of("jsc.firmware.screen.video_adapter", "Video Adapter  : %s");
    static final TextKey VIDEO = TextKey.of("jsc.firmware.screen.video", "Video     : %s");
    static final TextKey NONE = TextKey.of("jsc.firmware.screen.none", "none");
    static final TextKey DETECTING_DRIVES_OLD =
            TextKey.of("jsc.firmware.screen.detecting_drives_old", "Detecting drives ...");
    static final TextKey DETECTING_DRIVES = TextKey.of("jsc.firmware.screen.detecting_drives", "Detecting drives...");
    static final TextKey BOOTING_FROM = TextKey.of("jsc.firmware.screen.booting_from", "Booting from %s ...");
    static final TextKey CPU_OLD = TextKey.of("jsc.firmware.screen.cpu_old", "  %s MHz  %s");
    static final TextKey CPU_ONE_CORE = TextKey.of("jsc.firmware.screen.cpu_one_core", "   %s core   %s MHz   %s");
    static final TextKey CPU_CORES = TextKey.of("jsc.firmware.screen.cpu_cores", "   %s cores   %s MHz   %s");
    static final TextKey MEMORY_OLD = TextKey.of("jsc.firmware.screen.memory_old", "Memory Testing : %sK OK");
    static final TextKey MEMORY = TextKey.of("jsc.firmware.screen.memory", "Memory    : %s KB OK");

    // What a machine says when it starts, or will not.
    static final TextKey THE_MEDIUM = TextKey.of("jsc.firmware.screen.the_medium", "the medium");
    /* A device and what it holds: "Disk 0: Frames XP". */
    static final TextKey ENTRY = TextKey.of("jsc.firmware.screen.entry", "%s: %s");
    static final TextKey DISK_NAMED = TextKey.of("jsc.firmware.screen.disk_named", "Disk %s · %s");
    static final TextKey REPAIR = TextKey.of("jsc.firmware.screen.repair",
            "Put in an installation medium and install over it to repair.");
    static final TextKey NON_SYSTEM_DISK =
            TextKey.of("jsc.firmware.screen.non_system_disk", "Non-system disk or disk error");
    static final TextKey REPLACE_DISK =
            TextKey.of("jsc.firmware.screen.replace_disk", "Replace and press any key when ready");
    static final TextKey DISK_BOOT_FAILURE = TextKey.of("jsc.firmware.screen.disk_boot_failure",
            "DISK BOOT FAILURE, INSERT SYSTEM DISK AND PRESS ENTER");
    static final TextKey NO_BOOTABLE_FOUND =
            TextKey.of("jsc.firmware.screen.no_bootable_found", "No bootable device found");
    static final TextKey PRESS_ANY_KEY_SETUP =
            TextKey.of("jsc.firmware.screen.press_any_key_setup", "Press any key to enter Setup");
    static final TextKey NO_BOOTABLE = TextKey.of("jsc.firmware.screen.no_bootable", "No bootable device");
    static final TextKey NOTHING_ATTACHED = TextKey.of("jsc.firmware.screen.nothing_attached",
            "No disk and no drive is attached to this computer.");
    static final TextKey INSERT_MEDIA = TextKey.of("jsc.firmware.screen.insert_media",
            "Insert installation media and press Enter, or DEL for Setup");
    static final TextKey ENTERING_SETUP_NOW = TextKey.of("jsc.firmware.screen.entering_setup_now", "Entering Setup ...");
    /* The two keys along a modern machine's foot, each followed by what it does. */
    static final TextKey KEY_SETUP = TextKey.of("jsc.firmware.screen.key_setup", " Setup   ");
    static final TextKey KEY_BOOT_MENU = TextKey.of("jsc.firmware.screen.key_boot_menu", " Boot Menu");
    /* A machine summed up on one line: processor, cores, memory, architecture. */
    static final TextKey SUMMARY = TextKey.of("jsc.firmware.screen.summary", "%s · %s · %s · %s");
    static final TextKey CORE_COUNT_ONE = TextKey.of("jsc.firmware.screen.core_count_one", "%s core");
    static final TextKey CORE_COUNT_MANY = TextKey.of("jsc.firmware.screen.core_count_many", "%s cores");

    // The setup: its pages, its lists and its buttons, in the three looks.
    static final TextKey PAGE_BOOT_NAME = TextKey.of("jsc.firmware.screen.page_boot", "Boot");
    static final TextKey PAGE_BOOT_ORDER = TextKey.of("jsc.firmware.screen.page_boot_order", "Boot Order");
    static final TextKey PAGE_HARDWARE_NAME = TextKey.of("jsc.firmware.screen.page_hardware", "Hardware");
    static final TextKey PAGE_STORAGE_NAME = TextKey.of("jsc.firmware.screen.page_storage", "Storage");
    static final TextKey BOOT_MANAGER = TextKey.of("jsc.firmware.screen.boot_manager", "Boot Manager");
    static final TextKey EXIT = TextKey.of("jsc.firmware.screen.exit", "Exit");
    static final TextKey NO_INSTALLER_ON_MEDIUM =
            TextKey.of("jsc.firmware.screen.no_installer_on_medium", "There is no installer on that medium.");
    static final TextKey NEEDS_HARDWARE = TextKey.of("jsc.firmware.screen.needs_hardware", "%s needs %s hardware.");
    static final TextKey NO_INSTALLABLE =
            TextKey.of("jsc.firmware.screen.no_installable", "No installable system in a linked drive.");
    static final TextKey AUTOMATIC = TextKey.of("jsc.firmware.screen.automatic", "automatic");
    /* Something and, beside it, what it is or holds: "Disk 0  (Frames XP)". */
    static final TextKey BESIDE = TextKey.of("jsc.firmware.screen.beside", "%s  (%s)");
    /* Where a device is and what stands in the way of booting it. */
    static final TextKey WHERE_BUT = TextKey.of("jsc.firmware.screen.where_but", "%s - %s");
    static final TextKey BOOT_DEVICE_PRIORITY_LIST =
            TextKey.of("jsc.firmware.screen.boot_device_priority_list", "Boot Device Priority:");
    static final TextKey BOOT_MENU_LIST = TextKey.of("jsc.firmware.screen.boot_menu_list", "Boot Menu:");
    static final TextKey NO_BOOTABLE_FOUND_SENTENCE =
            TextKey.of("jsc.firmware.screen.no_bootable_found_sentence", "No bootable device found.");
    static final TextKey SET_FIRST = TextKey.of("jsc.firmware.screen.set_first", "SET FIRST");
    static final TextKey BOOT = TextKey.of("jsc.firmware.screen.boot", "BOOT");
    static final TextKey INSTALL_OS = TextKey.of("jsc.firmware.screen.install_os", "INSTALL OS");
    static final TextKey INSTALL_TO_DISK = TextKey.of("jsc.firmware.screen.install_to_disk", "INSTALL TO DISK");
    static final TextKey SETUP_UTILITY = TextKey.of("jsc.firmware.screen.setup_utility", "%s BIOS Setup Utility");
    static final TextKey SYSTEM_INFORMATION =
            TextKey.of("jsc.firmware.screen.system_information", "System Information");
    static final TextKey HARDWARE_HELP = TextKey.of("jsc.firmware.screen.hardware_help",
            "The hardware this firmware detected at power-on.");
    static final TextKey STORAGE_CONTROLLER =
            TextKey.of("jsc.firmware.screen.storage_controller", "Storage Controller");
    static final TextKey STORAGE_HELP = TextKey.of("jsc.firmware.screen.storage_help",
            "Up/Down chooses an array mode, Enter applies it.");
    static final TextKey BOOT_DEVICE_PRIORITY =
            TextKey.of("jsc.firmware.screen.boot_device_priority", "Boot Device Priority");
    static final TextKey DETECTING_DEVICES =
            TextKey.of("jsc.firmware.screen.detecting_devices", "Detecting devices ...");
    static final TextKey SCANNING_DEVICES = TextKey.of("jsc.firmware.screen.scanning_devices", "Scanning devices ...");
    static final TextKey FIRST = TextKey.of("jsc.firmware.screen.first", "1st");
    static final TextKey FIRST_TAG = TextKey.of("jsc.firmware.screen.first_tag", "[first]");
    static final TextKey DRIVE = TextKey.of("jsc.firmware.screen.drive", "Drive");
    static final TextKey SET_AS_FIRST = TextKey.of("jsc.firmware.screen.set_as_first", "Set as First Boot Device");
    static final TextKey BOOT_SELECTED = TextKey.of("jsc.firmware.screen.boot_selected", "Boot Selected Device");
    static final TextKey INSTALL_SYSTEM =
            TextKey.of("jsc.firmware.screen.install_system", "Install Operating System");
    static final TextKey ORDER_HELP = TextKey.of("jsc.firmware.screen.order_help",
            "Select the disk that boots first. The choice is saved, so two installed systems dual-boot.");
    static final TextKey BOOT_HELP = TextKey.of("jsc.firmware.screen.boot_help",
            "Select a device and press Enter to boot it. Installer media in a linked drive install onto disk %s.");
    static final TextKey ITEM_HELP = TextKey.of("jsc.firmware.screen.item_help", "Item Help");
    static final TextKey MORE = TextKey.of("jsc.firmware.screen.more", "+%s more");

    // The hardware page.
    static final TextKey DETECTING = TextKey.of("jsc.firmware.screen.detecting", "detecting ...");
    static final TextKey ARCH_BITS = TextKey.of("jsc.firmware.screen.arch_bits", "%s  (%s-bit)");
    static final TextKey CORES_AT = TextKey.of("jsc.firmware.screen.cores_at", "%s @ %s MHz");
    static final TextKey RAM_SLOTS = TextKey.of("jsc.firmware.screen.ram_slots", "%s MB  (%s of %s slots)");
    static final TextKey MONITORS_PORTS = TextKey.of("jsc.firmware.screen.monitors_ports", "%s of %s ports linked");
    static final TextKey LABEL_PROCESSOR = TextKey.of("jsc.firmware.screen.label_processor", "Processor");
    static final TextKey LABEL_ARCHITECTURE = TextKey.of("jsc.firmware.screen.label_architecture", "Architecture");
    static final TextKey LABEL_CORES = TextKey.of("jsc.firmware.screen.label_cores", "Cores");
    static final TextKey LABEL_MEMORY = TextKey.of("jsc.firmware.screen.label_memory", "Memory");
    static final TextKey LABEL_GRAPHICS = TextKey.of("jsc.firmware.screen.label_graphics", "Graphics");
    static final TextKey LABEL_VIDEO = TextKey.of("jsc.firmware.screen.label_video", "Video");
    static final TextKey LABEL_BOARD = TextKey.of("jsc.firmware.screen.label_board", "Board");
    static final TextKey LABEL_MONITORS = TextKey.of("jsc.firmware.screen.label_monitors", "Monitors");
    static final TextKey LABEL_HARDWARE_ERA = TextKey.of("jsc.firmware.screen.label_hardware_era", "Hardware Era");
    static final TextKey LABEL_BOOT_DISK = TextKey.of("jsc.firmware.screen.label_boot_disk", "Boot Disk");
    static final TextKey LABEL_INSTALL_TARGET =
            TextKey.of("jsc.firmware.screen.label_install_target", "Install Target");
    static final TextKey NO_DISK = TextKey.of("jsc.firmware.screen.no_disk", "no disk");

    // The storage page.
    static final TextKey NO_CONTROLLER =
            TextKey.of("jsc.firmware.screen.no_controller", "No storage controller fitted.");
    static final TextKey MOUNT_CONTROLLER = TextKey.of("jsc.firmware.screen.mount_controller",
            "Mount a RAID Controller in this machine's gadget bay.");
    static final TextKey UNCONFIGURED = TextKey.of("jsc.firmware.screen.unconfigured", "unconfigured");
    static final TextKey DEGRADED = TextKey.of("jsc.firmware.screen.degraded", "degraded");
    static final TextKey HEALTHY = TextKey.of("jsc.firmware.screen.healthy", "healthy");
    static final TextKey LABEL_CONTROLLER = TextKey.of("jsc.firmware.screen.label_controller", "Controller");
    static final TextKey RAID_CONTROLLER = TextKey.of("jsc.firmware.screen.raid_controller", "RAID Controller");
    static final TextKey LABEL_MEMBER_DRIVES = TextKey.of("jsc.firmware.screen.label_member_drives", "Member drives");
    static final TextKey COUNT_OF = TextKey.of("jsc.firmware.screen.count_of", "%s of %s");
    static final TextKey LABEL_ARRAY_STATE = TextKey.of("jsc.firmware.screen.label_array_state", "Array state");
    static final TextKey ARRAY_MODE = TextKey.of("jsc.firmware.screen.array_mode", "ARRAY MODE");
    static final TextKey NONE_INDEPENDENT = TextKey.of("jsc.firmware.screen.none_independent", "NONE (independent)");
    static final TextKey RAID0_STRENGTH = TextKey.of("jsc.firmware.screen.raid0_strength", "+25%% throughput");
    static final TextKey RAID1_STRENGTH = TextKey.of("jsc.firmware.screen.raid1_strength", "survives to 1 drive");
    static final TextKey RAID5_STRENGTH = TextKey.of("jsc.firmware.screen.raid5_strength", "survives 1 loss");
    static final TextKey NEEDS_DRIVES = TextKey.of("jsc.firmware.screen.needs_drives", "needs %s drives");
    static final TextKey CAPACITY_ITEMS = TextKey.of("jsc.firmware.screen.capacity_items", "%s items");
    static final TextKey APPLIES_MODE = TextKey.of("jsc.firmware.screen.applies_mode",
            "Enter applies the mode. Changing it erases the array.");

    // The key hints along the foot.
    static final TextKey FORMAT_ARMED = TextKey.of("jsc.firmware.screen.format_armed",
            "F again: FORMAT DISK (erases everything)   ESC: Exit");
    static final TextKey SETUP_KEYS = TextKey.of("jsc.firmware.screen.setup_keys",
            "Enter: Boot   Tab: Page   F: Format   ESC: Exit");

    // The boot manager's help under its list; its first two lines read as one sentence.
    static final TextKey LOADER_HELP_FIRST = TextKey.of("jsc.firmware.screen.loader_help_first",
            "   Use the Up and Down keys to select which entry is");
    static final TextKey LOADER_HELP_SECOND = TextKey.of("jsc.firmware.screen.loader_help_second",
            "   highlighted. Press Enter to boot the selected entry.");
    static final TextKey LOADER_HELD = TextKey.of("jsc.firmware.screen.loader_held",
            "The highlighted entry will be executed when you press Enter.");
    static final TextKey LOADER_COUNTDOWN = TextKey.of("jsc.firmware.screen.loader_countdown",
            "The highlighted entry will be executed automatically in %ss.");

    private FirmwareScreenTexts() {
    }

    /** A sentence as the player reads it. */
    static String of(final TextKey key) {
        return GameText.resolve(key.text());
    }

    /** A text as the player reads it. */
    static String of(final Text text) {
        return GameText.resolve(text);
    }
}
