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
 * What the desktops' disk tools say (computers/docs/design/storage-devices.md): Disk Management on Frames, the KDE
 * Partition Manager and GNOME Disks, their menus, their columns, their buttons and their dialogs, in each player's
 * language. Why a change is refused is said in the words every disk tool shares.
 */
@TextHolder
final class DiskToolTexts {

    // Shared.
    static final TextKey OK = TextKey.of("jsc.disk_tool.ok", "OK");
    static final TextKey CANCEL = TextKey.of("jsc.disk_tool.cancel", "Cancel");
    static final TextKey READING = TextKey.of("jsc.disk_tool.reading", "Reading the disks...");

    // Disk Management.
    static final TextKey DM_MENU = TextKey.of("jsc.disk_tool.dm.menu", "File   Action   View   Help");
    static final TextKey DM_MENU_XP = TextKey.of("jsc.disk_tool.dm.menu_xp", "File   Action   View   Window   Help");
    static final TextKey DM_VOLUME = TextKey.of("jsc.disk_tool.dm.volume", "Volume");
    /* The label Disk Management offers a volume it makes or formats, as Frames names one in its own language. */
    static final TextKey DM_NEW_VOLUME_LABEL = TextKey.of("jsc.disk_tool.dm.new_volume_label", "New Volume");
    static final TextKey DM_LAYOUT = TextKey.of("jsc.disk_tool.dm.layout", "Layout");
    static final TextKey DM_TYPE = TextKey.of("jsc.disk_tool.dm.type", "Type");
    static final TextKey DM_FILE_SYSTEM = TextKey.of("jsc.disk_tool.dm.file_system", "File System");
    static final TextKey DM_STATUS = TextKey.of("jsc.disk_tool.dm.status", "Status");
    static final TextKey DM_CAPACITY = TextKey.of("jsc.disk_tool.dm.capacity", "Capacity");
    static final TextKey DM_FREE_SPACE = TextKey.of("jsc.disk_tool.dm.free_space", "Free Space");
    static final TextKey DM_PERCENT_FREE = TextKey.of("jsc.disk_tool.dm.percent_free", "% Free");
    static final TextKey DM_SIMPLE = TextKey.of("jsc.disk_tool.dm.simple", "Simple");
    static final TextKey DM_PARTITION = TextKey.of("jsc.disk_tool.dm.partition", "Partition");
    static final TextKey DM_BASIC = TextKey.of("jsc.disk_tool.dm.basic", "Basic");
    static final TextKey DM_ONLINE = TextKey.of("jsc.disk_tool.dm.online", "Online");
    static final TextKey DM_DISK = TextKey.of("jsc.disk_tool.dm.disk", "Disk %s");
    static final TextKey DM_NO_LETTER = TextKey.of("jsc.disk_tool.dm.no_letter", "(Disk %s partition %s)");
    static final TextKey DM_HEALTHY = TextKey.of("jsc.disk_tool.dm.healthy", "Healthy");
    static final TextKey DM_HEALTHY_SYSTEM = TextKey.of("jsc.disk_tool.dm.healthy_system", "Healthy (System)");
    static final TextKey DM_HEALTHY_BOOT = TextKey.of("jsc.disk_tool.dm.healthy_boot",
            "Healthy (Boot, Page File, Primary Partition)");
    static final TextKey DM_HEALTHY_EFI = TextKey.of("jsc.disk_tool.dm.healthy_efi",
            "Healthy (EFI System Partition)");
    static final TextKey DM_HEALTHY_PRIMARY = TextKey.of("jsc.disk_tool.dm.healthy_primary",
            "Healthy (Primary Partition)");
    static final TextKey DM_HEALTHY_UNKNOWN = TextKey.of("jsc.disk_tool.dm.healthy_unknown",
            "Healthy (Unknown Partition)");
    static final TextKey DM_UNALLOCATED = TextKey.of("jsc.disk_tool.dm.unallocated", "Unallocated");
    static final TextKey DM_PRIMARY = TextKey.of("jsc.disk_tool.dm.primary", "Primary partition");
    static final TextKey DM_OPEN = TextKey.of("jsc.disk_tool.dm.open", "Open");
    static final TextKey DM_EXPLORE = TextKey.of("jsc.disk_tool.dm.explore", "Explore");
    static final TextKey DM_MARK_ACTIVE = TextKey.of("jsc.disk_tool.dm.mark_active", "Mark Partition as Active");
    static final TextKey DM_CHANGE_LETTER = TextKey.of("jsc.disk_tool.dm.change_letter",
            "Change Drive Letter and Paths...");
    static final TextKey DM_FORMAT = TextKey.of("jsc.disk_tool.dm.format", "Format...");
    static final TextKey DM_EXTEND = TextKey.of("jsc.disk_tool.dm.extend", "Extend Volume...");
    static final TextKey DM_SHRINK = TextKey.of("jsc.disk_tool.dm.shrink", "Shrink Volume...");
    static final TextKey DM_ADD_MIRROR = TextKey.of("jsc.disk_tool.dm.add_mirror", "Add Mirror...");
    static final TextKey DM_DELETE_VOLUME = TextKey.of("jsc.disk_tool.dm.delete_volume", "Delete Volume...");
    static final TextKey DM_DELETE_PARTITION = TextKey.of("jsc.disk_tool.dm.delete_partition", "Delete Partition...");
    static final TextKey DM_NEW_VOLUME = TextKey.of("jsc.disk_tool.dm.new_volume", "New Simple Volume...");
    static final TextKey DM_NEW_PARTITION = TextKey.of("jsc.disk_tool.dm.new_partition", "New Partition...");
    static final TextKey DM_TO_GPT = TextKey.of("jsc.disk_tool.dm.to_gpt", "Convert to GPT Disk");
    static final TextKey DM_TO_MBR = TextKey.of("jsc.disk_tool.dm.to_mbr", "Convert to MBR Disk");
    static final TextKey DM_PROPERTIES = TextKey.of("jsc.disk_tool.dm.properties", "Properties");
    static final TextKey DM_HELP = TextKey.of("jsc.disk_tool.dm.help", "Help");
    static final TextKey DM_FORMAT_TITLE = TextKey.of("jsc.disk_tool.dm.format_title", "Format");
    static final TextKey DM_VOLUME_LABEL = TextKey.of("jsc.disk_tool.dm.volume_label", "Volume label:");
    static final TextKey DM_FILE_SYSTEM_FIELD = TextKey.of("jsc.disk_tool.dm.file_system_field", "File system:");
    static final TextKey DM_QUICK = TextKey.of("jsc.disk_tool.dm.quick", "Perform a quick format");
    static final TextKey DM_FORMAT_WARNING_TITLE = TextKey.of("jsc.disk_tool.dm.format_warning_title",
            "Format Partition");
    static final TextKey DM_FORMAT_WARNING = TextKey.of("jsc.disk_tool.dm.format_warning",
            "Formatting this volume will erase all data on it. Back up any data you want to keep before"
                    + " formatting. Do you want to continue?");
    static final TextKey DM_SHRINK_TITLE = TextKey.of("jsc.disk_tool.dm.shrink_title", "Shrink %s");
    static final TextKey DM_SHRINK_BEFORE = TextKey.of("jsc.disk_tool.dm.shrink_before",
            "Total size before shrink in MB:");
    static final TextKey DM_SHRINK_AVAILABLE = TextKey.of("jsc.disk_tool.dm.shrink_available",
            "Size of available shrink space in MB:");
    static final TextKey DM_SHRINK_AMOUNT = TextKey.of("jsc.disk_tool.dm.shrink_amount",
            "Enter the amount of space to shrink in MB:");
    static final TextKey DM_SHRINK_NOTE = TextKey.of("jsc.disk_tool.dm.shrink_note",
            "A volume never shrinks below what it holds: %s holds %s MB.");
    static final TextKey DM_SHRINK_BUTTON = TextKey.of("jsc.disk_tool.dm.shrink_button", "Shrink");
    static final TextKey DM_EXTEND_TITLE = TextKey.of("jsc.disk_tool.dm.extend_title", "Extend Volume");
    static final TextKey DM_EXTEND_TOTAL = TextKey.of("jsc.disk_tool.dm.extend_total",
            "Total volume size in megabytes (MB):");
    static final TextKey DM_EXTEND_MAX = TextKey.of("jsc.disk_tool.dm.extend_max", "Maximum available space in MB:");
    static final TextKey DM_EXTEND_AMOUNT = TextKey.of("jsc.disk_tool.dm.extend_amount",
            "Select the amount of space in MB:");
    static final TextKey DM_EXTEND_BUTTON = TextKey.of("jsc.disk_tool.dm.extend_button", "Extend");
    static final TextKey DM_NEW_TITLE = TextKey.of("jsc.disk_tool.dm.new_title", "New Simple Volume");
    static final TextKey DM_NEW_TITLE_XP = TextKey.of("jsc.disk_tool.dm.new_title_xp", "New Partition");
    static final TextKey DM_NEW_SIZE = TextKey.of("jsc.disk_tool.dm.new_size", "Simple volume size in MB:");
    static final TextKey DM_NEW_MAX = TextKey.of("jsc.disk_tool.dm.new_max", "Maximum disk space in MB:");
    static final TextKey DM_FINISH = TextKey.of("jsc.disk_tool.dm.finish", "Finish");
    static final TextKey DM_DELETE_TITLE = TextKey.of("jsc.disk_tool.dm.delete_title", "Delete Simple Volume");
    static final TextKey DM_DELETE_TITLE_XP = TextKey.of("jsc.disk_tool.dm.delete_title_xp", "Delete Partition");
    static final TextKey DM_DELETE_WARNING = TextKey.of("jsc.disk_tool.dm.delete_warning",
            "Deleting this volume will erase all data on it. Back up any data you want to keep before deleting."
                    + " Do you want to continue?");
    static final TextKey DM_YES = TextKey.of("jsc.disk_tool.dm.yes", "Yes");
    static final TextKey DM_NO = TextKey.of("jsc.disk_tool.dm.no", "No");

    // The KDE Partition Manager.
    static final TextKey KPM_MENU = TextKey.of("jsc.disk_tool.kpm.menu",
            "File  Edit  View  Device  Partition  Tools  Settings  Help");
    static final TextKey KPM_APPLY = TextKey.of("jsc.disk_tool.kpm.apply", "Apply");
    static final TextKey KPM_UNDO = TextKey.of("jsc.disk_tool.kpm.undo", "Undo");
    static final TextKey KPM_CLEAR = TextKey.of("jsc.disk_tool.kpm.clear", "Clear");
    static final TextKey KPM_NEW = TextKey.of("jsc.disk_tool.kpm.new", "New");
    static final TextKey KPM_RESIZE = TextKey.of("jsc.disk_tool.kpm.resize", "Resize/Move");
    static final TextKey KPM_DELETE = TextKey.of("jsc.disk_tool.kpm.delete", "Delete");
    static final TextKey KPM_PROPERTIES = TextKey.of("jsc.disk_tool.kpm.properties", "Properties");
    static final TextKey KPM_NEW_TABLE = TextKey.of("jsc.disk_tool.kpm.new_table", "New Partition Table");
    static final TextKey KPM_DEVICES = TextKey.of("jsc.disk_tool.kpm.devices", "Devices");
    static final TextKey KPM_PARTITION = TextKey.of("jsc.disk_tool.kpm.partition", "Partition");
    static final TextKey KPM_TYPE = TextKey.of("jsc.disk_tool.kpm.type", "Type");
    static final TextKey KPM_MOUNT_POINT = TextKey.of("jsc.disk_tool.kpm.mount_point", "Mount Point");
    static final TextKey KPM_LABEL = TextKey.of("jsc.disk_tool.kpm.label", "Label");
    static final TextKey KPM_SIZE = TextKey.of("jsc.disk_tool.kpm.size", "Size");
    static final TextKey KPM_USED = TextKey.of("jsc.disk_tool.kpm.used", "Used");
    static final TextKey KPM_AVAILABLE = TextKey.of("jsc.disk_tool.kpm.available", "Available");
    static final TextKey KPM_UNALLOCATED = TextKey.of("jsc.disk_tool.kpm.unallocated", "unallocated");
    static final TextKey KPM_PENDING = TextKey.of("jsc.disk_tool.kpm.pending", "Pending Operations");
    static final TextKey KPM_NONE = TextKey.of("jsc.disk_tool.kpm.none", "None");
    static final TextKey KPM_COUNT = TextKey.of("jsc.disk_tool.kpm.count", "%s pending operations");
    static final TextKey KPM_OP_CREATE = TextKey.of("jsc.disk_tool.kpm.op_create",
            "Create a new partition (%s, %s) on '%s'");
    static final TextKey KPM_OP_DELETE = TextKey.of("jsc.disk_tool.kpm.op_delete", "Delete partition '%s'");
    static final TextKey KPM_OP_RESIZE = TextKey.of("jsc.disk_tool.kpm.op_resize", "Resize partition '%s' to %s");
    static final TextKey KPM_OP_FORMAT = TextKey.of("jsc.disk_tool.kpm.op_format",
            "Create file system %s on partition '%s'");
    static final TextKey KPM_OP_LABEL = TextKey.of("jsc.disk_tool.kpm.op_label", "Set the label of '%s' to %s");
    static final TextKey KPM_OP_TABLE = TextKey.of("jsc.disk_tool.kpm.op_table",
            "Create a new partition table (type: %s) on '%s'");
    static final TextKey KPM_RESIZE_TITLE = TextKey.of("jsc.disk_tool.kpm.resize_title",
            "Resize/Move partition: '%s'");
    static final TextKey KPM_FREE_BEFORE = TextKey.of("jsc.disk_tool.kpm.free_before", "Free space before:");
    static final TextKey KPM_SIZE_FIELD = TextKey.of("jsc.disk_tool.kpm.size_field", "Size:");
    static final TextKey KPM_FREE_AFTER = TextKey.of("jsc.disk_tool.kpm.free_after", "Free space after:");
    static final TextKey KPM_SMALLEST = TextKey.of("jsc.disk_tool.kpm.smallest", "smallest while mounted: %s");
    static final TextKey KPM_NEW_TITLE = TextKey.of("jsc.disk_tool.kpm.new_title", "Create a new partition");
    static final TextKey KPM_FILE_SYSTEM = TextKey.of("jsc.disk_tool.kpm.file_system", "File system:");
    static final TextKey KPM_LABEL_FIELD = TextKey.of("jsc.disk_tool.kpm.label_field", "Label:");
    static final TextKey KPM_PROPERTIES_TITLE = TextKey.of("jsc.disk_tool.kpm.properties_title",
            "Partition properties: '%s'");
    static final TextKey KPM_RECREATE = TextKey.of("jsc.disk_tool.kpm.recreate", "Recreate existing file system");
    static final TextKey KPM_TABLE_TITLE = TextKey.of("jsc.disk_tool.kpm.table_title",
            "Create a New Partition Table on %s");
    static final TextKey KPM_TABLE_WARNING = TextKey.of("jsc.disk_tool.kpm.table_warning",
            "WARNING: This will destroy all data on the device!");
    static final TextKey KPM_TABLE_CREATE = TextKey.of("jsc.disk_tool.kpm.table_create",
            "Create New Partition Table");
    static final TextKey KPM_APPLY_TITLE = TextKey.of("jsc.disk_tool.kpm.apply_title", "Apply Pending Operations");
    static final TextKey KPM_APPLY_ASK = TextKey.of("jsc.disk_tool.kpm.apply_ask",
            "Do you really want to apply the pending operations? This will permanently modify your disks.");

    // GNOME Disks.
    static final TextKey GD_DISK = TextKey.of("jsc.disk_tool.gd.disk", "%s Disk");
    static final TextKey GD_HARD_DISK = TextKey.of("jsc.disk_tool.gd.hard_disk", "%s Hard Disk");
    static final TextKey GD_MODEL = TextKey.of("jsc.disk_tool.gd.model", "Model");
    static final TextKey GD_SERIAL = TextKey.of("jsc.disk_tool.gd.serial", "Serial Number");
    static final TextKey GD_SIZE = TextKey.of("jsc.disk_tool.gd.size", "Size");
    static final TextKey GD_PARTITIONING = TextKey.of("jsc.disk_tool.gd.partitioning", "Partitioning");
    static final TextKey GD_MBR_REACH = TextKey.of("jsc.disk_tool.gd.mbr_reach",
            "Master Boot Record reaches only the first %s");
    static final TextKey GD_VOLUMES = TextKey.of("jsc.disk_tool.gd.volumes", "Volumes");
    static final TextKey GD_FREE_SPACE = TextKey.of("jsc.disk_tool.gd.free_space", "Free Space");
    static final TextKey GD_BEYOND = TextKey.of("jsc.disk_tool.gd.beyond", "Beyond the MBR");
    static final TextKey GD_PARTITION = TextKey.of("jsc.disk_tool.gd.partition", "Partition %s");
    static final TextKey GD_CONTENTS = TextKey.of("jsc.disk_tool.gd.contents", "Contents");
    static final TextKey GD_DEVICE = TextKey.of("jsc.disk_tool.gd.device", "Device");
    static final TextKey GD_MOUNTED = TextKey.of("jsc.disk_tool.gd.mounted", "%s, mounted");
    static final TextKey GD_NOT_MOUNTED = TextKey.of("jsc.disk_tool.gd.not_mounted", "%s, not mounted");
    static final TextKey GD_UNKNOWN = TextKey.of("jsc.disk_tool.gd.unknown", "Unknown");
    static final TextKey GD_CREATE = TextKey.of("jsc.disk_tool.gd.create", "Create Partition");
    static final TextKey GD_DELETE = TextKey.of("jsc.disk_tool.gd.delete", "Delete Partition");
    static final TextKey GD_FORMAT_PARTITION = TextKey.of("jsc.disk_tool.gd.format_partition", "Format Partition");
    static final TextKey GD_RESIZE = TextKey.of("jsc.disk_tool.gd.resize", "Resize");
    static final TextKey GD_FORMAT_DISK = TextKey.of("jsc.disk_tool.gd.format_disk", "Format Disk");
    static final TextKey GD_ERASE = TextKey.of("jsc.disk_tool.gd.erase", "Erase");
    static final TextKey GD_QUICK = TextKey.of("jsc.disk_tool.gd.quick", "Don't overwrite existing data (Quick)");
    static final TextKey GD_GPT = TextKey.of("jsc.disk_tool.gd.gpt",
            "Compatible with modern systems and hard disks > 2TB (GPT)");
    static final TextKey GD_MBR = TextKey.of("jsc.disk_tool.gd.mbr", "Compatible with all systems and devices (MBR)");
    static final TextKey GD_NONE = TextKey.of("jsc.disk_tool.gd.none", "No partitioning (Empty)");
    static final TextKey GD_ERASES = TextKey.of("jsc.disk_tool.gd.erases", "It erases everything on it.");
    static final TextKey GD_FORMAT_BUTTON = TextKey.of("jsc.disk_tool.gd.format_button", "Format...");
    static final TextKey GD_VOLUME_NAME = TextKey.of("jsc.disk_tool.gd.volume_name", "Volume Name");
    static final TextKey GD_TYPE = TextKey.of("jsc.disk_tool.gd.type", "Type");
    static final TextKey GD_PARTITION_SIZE = TextKey.of("jsc.disk_tool.gd.partition_size", "Partition Size (MB)");
    static final TextKey GD_CURRENT_SIZE = TextKey.of("jsc.disk_tool.gd.current_size", "Current Size");
    static final TextKey GD_MINIMAL_SIZE = TextKey.of("jsc.disk_tool.gd.minimal_size", "Minimal Size");
    static final TextKey GD_MAXIMAL_SIZE = TextKey.of("jsc.disk_tool.gd.maximal_size", "Maximal Size");
    static final TextKey GD_NEW_SIZE = TextKey.of("jsc.disk_tool.gd.new_size", "Size (MB)");
    static final TextKey GD_DELETE_ASK = TextKey.of("jsc.disk_tool.gd.delete_ask",
            "Are you sure you want to delete the partition? All data on the volume will be lost.");

    private DiskToolTexts() {
    }
}
