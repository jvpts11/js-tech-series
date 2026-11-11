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
    static final TextKey COMPUTER_NAME = TextKey.of("jsc.installer.screen.computer_name", "Computer name:");
    static final TextKey NO_SYSTEM = TextKey.of("jsc.installer.screen.no_system", "no system");

    // The partitions and the free space, as Frames XP's text phase lists them.
    static final TextKey XP_INTRO_KEYS = TextKey.of("jsc.installer.screen.xp_intro_keys",
            "Use the UP and DOWN keys to select an item in the list.");
    static final TextKey XP_INTRO_SET_UP = TextKey.of("jsc.installer.screen.xp_intro_set_up",
            "- To set up %s on the selected item, press ENTER.");
    static final TextKey XP_INTRO_DELETE = TextKey.of("jsc.installer.screen.xp_intro_delete",
            "- To delete the selected partition, press D.");
    static final TextKey XP_DISK_LINE = TextKey.of("jsc.installer.screen.xp_disk_line",
            "%s MB Disk %s at Id 0 on bus 0 on atapi [%s]");
    static final TextKey XP_PARTITION = TextKey.of("jsc.installer.screen.xp_partition", "Partition%s [%s]");
    static final TextKey XP_PARTITION_FREE = TextKey.of("jsc.installer.screen.xp_partition_free", "(%s MB free)");
    static final TextKey XP_UNPARTITIONED = TextKey.of("jsc.installer.screen.xp_unpartitioned",
            "Unpartitioned space");
    static final TextKey XP_UNKNOWN = TextKey.of("jsc.installer.screen.xp_unknown", "Unknown");
    static final TextKey XP_LEAVES_OTHERS = TextKey.of("jsc.installer.screen.xp_leaves_others",
            "Installing %s on %s leaves the other partitions as they are, with their files and their items.");
    static final TextKey XP_THE_UNPARTITIONED = TextKey.of("jsc.installer.screen.xp_the_unpartitioned",
            "the unpartitioned space");
    static final TextKey XP_ON_DISK = TextKey.of("jsc.installer.screen.xp_on_disk",
            "on %s MB Disk %s at Id 0 on bus 0 on atapi [%s].");
    static final TextKey XP_FORMAT_HELP = TextKey.of("jsc.installer.screen.xp_format_help",
            "Use the UP and DOWN keys to select the file system you want, and then press ENTER. To choose a "
                    + "different partition for %s, press ESC.");
    static final TextKey XP_FORMAT_NTFS = TextKey.of("jsc.installer.screen.xp_format_ntfs",
            "Format the partition using the NTFS file system");
    static final TextKey XP_FORMAT_FAT = TextKey.of("jsc.installer.screen.xp_format_fat",
            "Format the partition using the FAT file system");
    static final TextKey XP_QUICK = TextKey.of("jsc.installer.screen.xp_quick", "%s (Quick)");
    static final TextKey XP_FORMAT_KEEP = TextKey.of("jsc.installer.screen.xp_format_keep",
            "Leave the current file system intact (no changes)");
    static final TextKey XP_ONLY_FORMATTED = TextKey.of("jsc.installer.screen.xp_only_formatted",
            "Only %s is formatted.");
    static final TextKey XP_FAT32_NOTE = TextKey.of("jsc.installer.screen.xp_fat32_note",
            "FAT here is FAT32: no file over 4 GB on it.");

    // The partitions and the free space, as the table of Frames 7 to 11 lists them.
    static final TextKey TABLE_PARTITION = TextKey.of("jsc.installer.screen.table_partition",
            "Disk %s, Partition %s");
    static final TextKey TABLE_PARTITION_NAMED = TextKey.of("jsc.installer.screen.table_partition_named",
            "Disk %s, Partition %s (%s)");
    static final TextKey TABLE_FREE = TextKey.of("jsc.installer.screen.table_free", "Disk %s, Unallocated space");
    static final TextKey HOLDS_BOOT_ENTRIES = TextKey.of("jsc.installer.screen.holds_boot_entries",
            "The boot entries");
    static final TextKey HOLDS_FILES = TextKey.of("jsc.installer.screen.holds_files", "Files");
    static final TextKey HOLDS_SWAP = TextKey.of("jsc.installer.screen.holds_swap", "Swap");
    static final TextKey LINK_NEW = TextKey.of("jsc.installer.screen.link_new", "New");
    static final TextKey LINK_DELETE = TextKey.of("jsc.installer.screen.link_delete", "Delete");
    static final TextKey LINK_FORMAT = TextKey.of("jsc.installer.screen.link_format", "Format");
    static final TextKey LINK_EXTEND = TextKey.of("jsc.installer.screen.link_extend", "Extend");
    static final TextKey NEXT_IN_FREE = TextKey.of("jsc.installer.screen.next_in_free",
            "%s needs %s. Next makes an %s partition of this free space and installs there; every other partition "
                    + "stays.");
    static final TextKey NEXT_ON_PARTITION = TextKey.of("jsc.installer.screen.next_on_partition",
            "%s needs %s. Next installs on this partition beside its files; every other partition stays.");
    static final TextKey NEXT_FORMATTED = TextKey.of("jsc.installer.screen.next_formatted",
            "%s needs %s. Next formats this partition as %s and installs there; every other partition stays.");

    // The question before a partition is deleted or formatted.
    static final TextKey DELETE_ASK = TextKey.of("jsc.installer.screen.delete_ask", "Delete Partition %s of Disk %s?");
    static final TextKey FORMAT_ASK = TextKey.of("jsc.installer.screen.format_ask", "Format Partition %s of Disk %s?");
    static final TextKey EVERY_FILE_ON_IT = TextKey.of("jsc.installer.screen.every_file_on_it",
            "Every file and item on it is lost.");
    static final TextKey DELETE_KEYS = TextKey.of("jsc.installer.screen.delete_keys",
            "Y = delete       N = cancel");
    static final TextKey FORMAT_KEYS = TextKey.of("jsc.installer.screen.format_keys",
            "Y = format       N = cancel");

    // Ubuntu's guided storage.
    static final TextKey UBUNTU_CHOOSE = TextKey.of("jsc.installer.screen.ubuntu_choose", "Choose how to install %s.");
    static final TextKey UBUNTU_ALONGSIDE = TextKey.of("jsc.installer.screen.ubuntu_alongside",
            "Install alongside %s");
    static final TextKey UBUNTU_SHRINKS = TextKey.of("jsc.installer.screen.ubuntu_shrinks",
            "Shrinks %s (%s) and uses the room it frees:");
    static final TextKey UBUNTU_OTHER = TextKey.of("jsc.installer.screen.ubuntu_other", "of %s (holds %s)");
    static final TextKey UBUNTU_NEW = TextKey.of("jsc.installer.screen.ubuntu_new", "%s: %s %s for /, swap %s");
    static final TextKey UBUNTU_NEW_NO_SWAP = TextKey.of("jsc.installer.screen.ubuntu_new_no_swap",
            "%s: %s %s for /");
    static final TextKey UBUNTU_ENTIRE = TextKey.of("jsc.installer.screen.ubuntu_entire",
            "Use an entire disk: %s, erasing %s");
    static final TextKey UBUNTU_ENTIRE_EMPTY = TextKey.of("jsc.installer.screen.ubuntu_entire_empty",
            "Use an entire disk: %s");
    static final TextKey UBUNTU_CUSTOM = TextKey.of("jsc.installer.screen.ubuntu_custom", "Custom storage layout");
    static final TextKey GRUB_LISTS = TextKey.of("jsc.installer.screen.grub_lists",
            "GRUB lists %s and %s at power-on.");
    static final TextKey MBR_PARTITIONS = TextKey.of("jsc.installer.screen.mbr_partitions",
            "The disk is MBR: this makes %s of its four partitions.");
    static final TextKey SIZE_KEYS = TextKey.of("jsc.installer.screen.size_keys",
            "LEFT and RIGHT change the size.");

    // What an installer that lists whole disks does with the one chosen.
    static final TextKey PLAN_WHOLE = TextKey.of("jsc.installer.screen.plan_whole",
            "Disk %s is erased: %s %s for the system.");
    static final TextKey PLAN_FREE = TextKey.of("jsc.installer.screen.plan_free",
            "The system goes in the free space of Disk %s: %s %s.");
    static final TextKey PLAN_PARTITION = TextKey.of("jsc.installer.screen.plan_partition",
            "The system goes on partition %s of Disk %s, %s %s.");
    static final TextKey PLAN_BESIDE = TextKey.of("jsc.installer.screen.plan_beside",
            "%s is shrunk to %s and the system goes in the room it frees: %s %s.");
    static final TextKey PLAN_SWAP = TextKey.of("jsc.installer.screen.plan_swap", "Swap: %s.");
    static final TextKey PART_LINE = TextKey.of("jsc.installer.screen.part_line", "#%s  %s  %s  %s");
    static final TextKey FREE_LINE = TextKey.of("jsc.installer.screen.free_line", "FREE SPACE  %s");

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
    /** bsdinstall's own, bare word for the same choice: its dialogs never spell out what leaving it does. */
    static final TextKey BSD_DESKTOP_NONE = TextKey.of("jsc.installer.screen.bsd_desktop_none", "None");

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
    /* The same question on an installer that calls its disks by device name (ada0) rather than by number. */
    static final TextKey ERASE_ASK_NAMED = TextKey.of("jsc.installer.screen.erase_ask_named", "Erase %s?");
    static final TextKey AND_EVERY_FILE = TextKey.of("jsc.installer.screen.and_every_file", "%s and every file on");
    static final TextKey EVERY_FILE = TextKey.of("jsc.installer.screen.every_file", "Every file on");
    static final TextKey WILL_BE_DELETED = TextKey.of("jsc.installer.screen.will_be_deleted", "%s will be deleted.");
    static final TextKey CANNOT_UNDO = TextKey.of("jsc.installer.screen.cannot_undo", "This cannot be undone.");
    static final TextKey ERASE_KEYS = TextKey.of("jsc.installer.screen.erase_keys", "Y = erase        N = cancel");

    // The frames around an installer's pages and the buttons they carry.
    static final TextKey FRAME_HELP = TextKey.of("jsc.installer.frame.help", "[ Help ]");
    static final TextKey FRAME_REBOOT_NOW = TextKey.of("jsc.installer.frame.reboot_now", "[ Reboot Now ]");
    /* The bare word, for the two consoles that print their own brackets or reverse video around a button. */
    static final TextKey FRAME_REBOOT = TextKey.of("jsc.installer.frame.reboot", "Reboot");
    /* Written-out buttons of a terminal installer, padded to one width so they line up. */
    static final TextKey FRAME_DONE = TextKey.of("jsc.installer.frame.done", "[ Done       ]");
    static final TextKey FRAME_BACK = TextKey.of("jsc.installer.frame.back", "[ Back       ]");
    static final TextKey FRAME_WIZARD = TextKey.of("jsc.installer.frame.wizard", "%s Wizard");
    static final TextKey FRAME_CANCEL = TextKey.of("jsc.installer.frame.cancel", "Cancel");
    static final TextKey FRAME_RESTART = TextKey.of("jsc.installer.frame.restart", "Restart");
    static final TextKey FRAME_NEXT_ARROW = TextKey.of("jsc.installer.frame.next_arrow", "Next >");
    static final TextKey FRAME_BACK_ARROW = TextKey.of("jsc.installer.frame.back_arrow", "< Back");
    static final TextKey FRAME_NEXT = TextKey.of("jsc.installer.frame.next", "Next");
    static final TextKey FRAME_BACK_PLAIN = TextKey.of("jsc.installer.frame.back_plain", "Back");
    static final TextKey FRAME_ERASE_DISK = TextKey.of("jsc.installer.frame.erase_disk", "Erase disk");
    /* Three lines of a countdown: "Setup will complete in / approximately: / 39 seconds". */
    static final TextKey FRAME_COMPLETE_IN = TextKey.of("jsc.installer.frame.complete_in", "Setup will complete in");
    static final TextKey FRAME_APPROXIMATELY = TextKey.of("jsc.installer.frame.approximately", "approximately:");
    static final TextKey FRAME_SECONDS = TextKey.of("jsc.installer.frame.seconds", "%s seconds");
    /* The glass and the white setup windows of the later Frames editions. */
    static final TextKey FRAME_INSTALL_NOW = TextKey.of("jsc.installer.frame.install_now", "Install now");
    static final TextKey FRAME_SET_UP = TextKey.of("jsc.installer.frame.set_up", "Set Up %s");
    static final TextKey FRAME_PHASE_COLLECTING = TextKey.of("jsc.installer.frame.phase_collecting",
            "1  Collecting information");
    static final TextKey FRAME_PHASE_INSTALLING = TextKey.of("jsc.installer.frame.phase_installing",
            "2  Installing %s");
    static final TextKey FRAME_RESTARTS_SEVERAL = TextKey.of("jsc.installer.frame.restarts_several",
            "Your computer will restart several times. This might take a while.");
    static final TextKey FRAME_STATUS = TextKey.of("jsc.installer.frame.status", "Status");
    static final TextKey FRAME_COPYRIGHT = TextKey.of("jsc.installer.frame.copyright", "Copyright (c) %s %s");

    // The firmware's own copy of a system that has no installer of its own.
    static final TextKey COPY_PREPARING = TextKey.of("jsc.installer.copy.preparing", "preparing the disk");
    static final TextKey COPY_COPYING = TextKey.of("jsc.installer.copy.copying", "copying the system");
    static final TextKey COPY_FOLDERS = TextKey.of("jsc.installer.copy.folders", "creating folders");
    static final TextKey COPY_BOOT_ENTRY = TextKey.of("jsc.installer.copy.boot_entry", "registering the boot entry");
    static final TextKey COPY_STEP = TextKey.of("jsc.installer.copy.step", "%s ...");
    static final TextKey COPY_DONE = TextKey.of("jsc.installer.copy.done", "done");
    static final TextKey COPY_INSTALLERS_SYSTEM =
            TextKey.of("jsc.installer.copy.installers_system", "the installer's system");
    static final TextKey COPY_DEFAULT_DISK = TextKey.of("jsc.installer.copy.default_disk", "the default disk");
    static final TextKey COPY_INSTALLING = TextKey.of("jsc.installer.copy.installing", "INSTALLING %s");
    static final TextKey COPY_COMPLETE = TextKey.of("jsc.installer.copy.complete", "INSTALLATION COMPLETE");
    static final TextKey COPY_FAILED = TextKey.of("jsc.installer.copy.failed", "INSTALLATION FAILED");
    static final TextKey COPY_KEEP_MEDIUM = TextKey.of("jsc.installer.copy.keep_medium", "Do not remove the medium.");
    static final TextKey COPY_INSTALLED_ON = TextKey.of("jsc.installer.copy.installed_on", "%s installed on %s.");
    /* Two lines that read as one sentence. */
    static final TextKey COPY_TAKE_OUT_FIRST =
            TextKey.of("jsc.installer.copy.take_out_first", "Take the installation medium out before rebooting,");
    static final TextKey COPY_TAKE_OUT_SECOND =
            TextKey.of("jsc.installer.copy.take_out_second", "or the machine boots the installer again.");
    static final TextKey COPY_REBOOT = TextKey.of("jsc.installer.copy.reboot", "REBOOT");
    static final TextKey COPY_BACK_TO_SETUP = TextKey.of("jsc.installer.copy.back_to_setup", "BACK TO SETUP");
    static final TextKey COPY_NOTHING_WRITTEN =
            TextKey.of("jsc.installer.copy.nothing_written", "Nothing was written to %s.");
    static final TextKey COPY_CLOSE = TextKey.of("jsc.installer.copy.close", "CLOSE");

    // bsdinstall's own pages.
    static final TextKey BSD_INSTALL_BUTTON = TextKey.of("jsc.installer.screen.bsd_install_button", "Install");
    static final TextKey BSD_OK_BUTTON = TextKey.of("jsc.installer.screen.bsd_ok_button", "OK");
    static final TextKey BSD_WELCOME_BODY_1 = TextKey.of("jsc.installer.screen.bsd_welcome_body_1",
            "Welcome to %s! This will install");
    static final TextKey BSD_WELCOME_BODY_2 = TextKey.of("jsc.installer.screen.bsd_welcome_body_2",
            "%s on this computer.");
    static final TextKey BSD_NAME_BODY_1 = TextKey.of("jsc.installer.screen.bsd_name_body_1",
            "Please choose a hostname for this");
    static final TextKey BSD_NAME_BODY_2 = TextKey.of("jsc.installer.screen.bsd_name_body_2", "computer.");
    static final TextKey BSD_COMPONENTS_INTRO_1 = TextKey.of("jsc.installer.screen.bsd_components_intro_1",
            "Choose optional system components to");
    static final TextKey BSD_COMPONENTS_INTRO_2 =
            TextKey.of("jsc.installer.screen.bsd_components_intro_2", "install:");
    static final TextKey BSD_COMPONENT_BASE = TextKey.of("jsc.installer.screen.bsd_component_base",
            "base      the base system (always)");
    static final TextKey BSD_COMPONENT_KERNEL = TextKey.of("jsc.installer.screen.bsd_component_kernel",
            "kernel    the kernel (always)");
    static final TextKey BSD_COMPONENT_PORTS = TextKey.of("jsc.installer.screen.bsd_component_ports",
            "ports     the Ports Collection");
    static final TextKey BSD_MIRROR_INTRO = TextKey.of("jsc.installer.screen.bsd_mirror_intro",
            "Where should pkg and ports fetch from?");
    static final TextKey BSD_MIRROR_OPTION = TextKey.of("jsc.installer.screen.bsd_mirror_option",
            "mirror://mainframe   the network's Mirror");
    static final TextKey BSD_MIRROR_NONE =
            TextKey.of("jsc.installer.screen.bsd_mirror_none", "none                 install from CD only");
    static final TextKey BSD_SERVICES_INTRO_1 = TextKey.of("jsc.installer.screen.bsd_services_intro_1",
            "Choose the services you would like to be");
    static final TextKey BSD_SERVICES_INTRO_2 =
            TextKey.of("jsc.installer.screen.bsd_services_intro_2", "started at boot:");
    static final TextKey BSD_SERVICE_CRON = TextKey.of("jsc.installer.screen.bsd_service_cron",
            "cron   run commands on a schedule");
    static final TextKey BSD_SERVICE_SSHD = TextKey.of("jsc.installer.screen.bsd_service_sshd",
            "sshd   let other computers reach this one");
    static final TextKey BSD_DESKTOP_INTRO_1 = TextKey.of("jsc.installer.screen.bsd_desktop_intro_1",
            "The Mirror answered. Install a desktop");
    static final TextKey BSD_DESKTOP_INTRO_2 = TextKey.of("jsc.installer.screen.bsd_desktop_intro_2", "now?");
    static final TextKey BSD_DONE_LINE_1 = TextKey.of("jsc.installer.screen.bsd_done_line_1",
            "%s is installed on %s.");
    static final TextKey BSD_DONE_LINE_2 = TextKey.of("jsc.installer.screen.bsd_done_line_2",
            "Remove the CD and reboot.");
    static final TextKey BSD_DISK_INTRO = TextKey.of("jsc.installer.screen.bsd_disk_intro",
            "How would you like to install %s?");
    static final TextKey BSD_DISK_AUTO = TextKey.of("jsc.installer.screen.bsd_disk_auto",
            "Auto (UFS)    use the whole of %s (%s, empty)");
    /**
     * The same row when no empty disk has room: real bsdinstall's Auto (UFS) still names a disk big enough and
     * takes the whole of it, erasing the system that disk already holds rather than leaving the player with
     * nothing to choose.
     */
    static final TextKey BSD_DISK_AUTO_ERASE = TextKey.of("jsc.installer.screen.bsd_disk_auto_erase",
            "Auto (UFS)    use the whole of %s (%s, %s)");
    /* The same again for a disk big enough that holds files but no system. */
    static final TextKey BSD_DISK_AUTO_ERASE_FILES = TextKey.of("jsc.installer.screen.bsd_disk_auto_erase_files",
            "Auto (UFS)    use the whole of %s (%s, its files erased)");
    static final TextKey BSD_DISK_BESIDE = TextKey.of("jsc.installer.screen.bsd_disk_beside",
            "Beside        share %s with %s (%s free)");
    static final TextKey BSD_DISK_AUTO_ZFS = TextKey.of("jsc.installer.screen.bsd_disk_auto_zfs",
            "Auto (ZFS)    Guided Root-on-ZFS");
    static final TextKey BSD_DISK_TABLE_ROW = TextKey.of("jsc.installer.screen.bsd_disk_table_row", "%s   %s   %s");
    static final TextKey POOL_CONFIGURE = TextKey.of("jsc.installer.screen.pool_configure", "Configure Options:");
    static final TextKey POOL_INSTALL = TextKey.of("jsc.installer.screen.pool_install", "Install");
    static final TextKey POOL_PROCEED = TextKey.of("jsc.installer.screen.pool_proceed", "Proceed with Installation");
    static final TextKey POOL_TYPE = TextKey.of("jsc.installer.screen.pool_type", "Pool Type/Disks:");
    static final TextKey POOL_STRIPE = TextKey.of("jsc.installer.screen.pool_stripe", "stripe: 1 disk");
    static final TextKey POOL_MIRROR = TextKey.of("jsc.installer.screen.pool_mirror", "mirror: 2 disks");
    static final TextKey POOL_RESCAN = TextKey.of("jsc.installer.screen.pool_rescan", "Rescan Devices");
    static final TextKey POOL_DISK_INFO = TextKey.of("jsc.installer.screen.pool_disk_info", "Disk Info");
    static final TextKey POOL_NAME = TextKey.of("jsc.installer.screen.pool_name", "Pool Name");
    static final TextKey POOL_FORCE_4K = TextKey.of("jsc.installer.screen.pool_force_4k", "Force 4K Sectors?");
    static final TextKey POOL_ENCRYPT = TextKey.of("jsc.installer.screen.pool_encrypt", "Encrypt Disks?");
    static final TextKey POOL_SCHEME = TextKey.of("jsc.installer.screen.pool_scheme", "Partition Scheme");
    static final TextKey POOL_GPT = TextKey.of("jsc.installer.screen.pool_gpt", "GPT (UEFI)");
    static final TextKey POOL_MBR = TextKey.of("jsc.installer.screen.pool_mbr", "MBR (BIOS)");
    static final TextKey POOL_SWAP = TextKey.of("jsc.installer.screen.pool_swap", "Swap Size");
    static final TextKey POOL_MIRROR_SWAP = TextKey.of("jsc.installer.screen.pool_mirror_swap", "Mirror Swap?");
    static final TextKey POOL_ENCRYPT_SWAP = TextKey.of("jsc.installer.screen.pool_encrypt_swap", "Encrypt Swap?");
    static final TextKey POOL_YES = TextKey.of("jsc.installer.screen.pool_yes", "YES");
    static final TextKey POOL_NO = TextKey.of("jsc.installer.screen.pool_no", "NO");
    static final TextKey BSD_DISK_EMPTY = TextKey.of("jsc.installer.screen.bsd_disk_empty", "empty");
    static final TextKey BSD_EXTRACT_DONE = TextKey.of("jsc.installer.screen.bsd_extract_done", "[   Done    ]");
    static final TextKey BSD_EXTRACT_IN_PROGRESS =
            TextKey.of("jsc.installer.screen.bsd_extract_in_progress", "[ In Progress ]");
    static final TextKey BSD_EXTRACT_WAITING =
            TextKey.of("jsc.installer.screen.bsd_extract_waiting", "[  Waiting  ]");
    static final TextKey BSD_EXTRACTING = TextKey.of("jsc.installer.screen.bsd_extracting",
            "Extracting distribution files...");
    static final TextKey BSD_OVERALL_PROGRESS =
            TextKey.of("jsc.installer.screen.bsd_overall_progress", "Overall Progress");

    // UNIX System V's own page.
    static final TextKey SYSV_CHOOSE_DISK = TextKey.of("jsc.installer.screen.sysv_choose_disk",
            "Choose the disk to install the %s on:");
    static final TextKey SYSV_HOLDS_QUESTION = TextKey.of("jsc.installer.screen.sysv_holds_question",
            "%s holds %s. What should happen to it?");
    static final TextKey SYSV_ERASE_OPTION = TextKey.of("jsc.installer.screen.sysv_erase_option",
            "1  Erase %s and install the %s");
    static final TextKey SYSV_BESIDE_OPTION = TextKey.of("jsc.installer.screen.sysv_beside_option",
            "2  Install the %s beside %s");
    static final TextKey SYSV_INSTALLING_BESIDE = TextKey.of("jsc.installer.screen.sysv_installing_beside",
            "Installing the %s on %s (beside %s)");
    static final TextKey SYSV_INSTALLING = TextKey.of("jsc.installer.screen.sysv_installing",
            "Installing the %s on %s");
    static final TextKey SYSV_DONE_LINE_1 =
            TextKey.of("jsc.installer.screen.sysv_done_line_1", "The %s is installed on %s.");
    static final TextKey SYSV_DONE_NAME = TextKey.of("jsc.installer.screen.sysv_done_name",
            "Computer name     %s");
    static final TextKey SYSV_DONE_SPACE = TextKey.of("jsc.installer.screen.sysv_done_space",
            "Space used        %s of %s");
    static final TextKey SYSV_DONE_BOOT_ORDER = TextKey.of("jsc.installer.screen.sysv_done_boot_order",
            "Boot order        %s first, then %s");
    static final TextKey SYSV_STATUS_COPYING = TextKey.of("jsc.installer.screen.sysv_status_copying", "copying");
    static final TextKey SYSV_STATUS_WAITING = TextKey.of("jsc.installer.screen.sysv_status_waiting", "waiting");
    static final TextKey SYSV_MONITOR_NOTE = TextKey.of("jsc.installer.screen.sysv_monitor_note",
            "The copy carries on if the monitor is closed.");

    private InstallerScreenTexts() {
    }
}
