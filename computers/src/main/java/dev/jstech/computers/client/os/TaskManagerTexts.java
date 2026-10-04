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
 * What the Task Manager says, in each of the shapes it takes on the desktops: its pages, menus, columns, buttons,
 * meters and status bar. Program names, counts, sizes and clocks are data. Kept apart from the window so the
 * language generator can read it on a server too, where windows do not exist.
 */
@TextHolder
final class TaskManagerTexts {

    // The window, by the shape it takes.
    static final TextKey TITLE = TextKey.of("jsc.task_manager.title", "Task Manager");
    static final TextKey TITLE_CLOSE_PROGRAM = TextKey.of("jsc.task_manager.title_close_program", "Close Program");
    static final TextKey TITLE_SYSTEM_MONITOR = TextKey.of("jsc.task_manager.title_system_monitor", "System Monitor");

    // The pages.
    static final TextKey APPLICATIONS = TextKey.of("jsc.task_manager.applications", "Applications");
    static final TextKey PROCESSES = TextKey.of("jsc.task_manager.processes", "Processes");
    static final TextKey PERFORMANCE = TextKey.of("jsc.task_manager.performance", "Performance");
    static final TextKey NETWORKING = TextKey.of("jsc.task_manager.networking", "Networking");
    static final TextKey SERVICES = TextKey.of("jsc.task_manager.services", "Services");
    static final TextKey STORAGE = TextKey.of("jsc.task_manager.storage", "Storage");
    static final TextKey NETWORK = TextKey.of("jsc.task_manager.network", "Network");
    static final TextKey OVERVIEW = TextKey.of("jsc.task_manager.overview", "Overview");
    static final TextKey HISTORY = TextKey.of("jsc.task_manager.history", "History");
    static final TextKey RESOURCES = TextKey.of("jsc.task_manager.resources", "Resources");
    static final TextKey FILE_SYSTEMS = TextKey.of("jsc.task_manager.file_systems", "File Systems");
    static final TextKey READING = TextKey.of("jsc.task_manager.reading", "Reading machine...");

    // Frames 95's Close Program box.
    static final TextKey MAY_LOSE_WORK =
            TextKey.of("jsc.task_manager.may_lose_work", "Ending a program may lose unsaved work.");
    static final TextKey RESTARTS =
            TextKey.of("jsc.task_manager.restarts", "Ending the system restarts the machine.");
    static final TextKey END_TASK = TextKey.of("jsc.task_manager.end_task", "End Task");
    static final TextKey SHUT_DOWN = TextKey.of("jsc.task_manager.shut_down", "Shut Down");
    static final TextKey CANCEL = TextKey.of("jsc.task_manager.cancel", "Cancel");

    // Frames XP's menus, lists and status bar.
    static final TextKey FILE = TextKey.of("jsc.task_manager.file", "File");
    static final TextKey OPTIONS = TextKey.of("jsc.task_manager.options", "Options");
    static final TextKey VIEW = TextKey.of("jsc.task_manager.view", "View");
    static final TextKey WINDOWS = TextKey.of("jsc.task_manager.windows", "Windows");
    static final TextKey HELP = TextKey.of("jsc.task_manager.help", "Help");
    static final TextKey TASK = TextKey.of("jsc.task_manager.task", "Task");
    static final TextKey STATUS = TextKey.of("jsc.task_manager.status", "Status");
    static final TextKey RUNNING = TextKey.of("jsc.task_manager.running", "Running");
    static final TextKey SWITCH_TO = TextKey.of("jsc.task_manager.switch_to", "Switch To");
    static final TextKey NEW_TASK = TextKey.of("jsc.task_manager.new_task", "New Task");
    static final TextKey IMAGE_NAME = TextKey.of("jsc.task_manager.image_name", "Image Name");
    static final TextKey MEM_USAGE = TextKey.of("jsc.task_manager.mem_usage", "Mem Usage");
    static final TextKey END_PROCESS = TextKey.of("jsc.task_manager.end_process", "End Process");
    static final TextKey PROCESS_COUNT = TextKey.of("jsc.task_manager.process_count", "Processes: %s");
    static final TextKey CPU_USAGE_IS = TextKey.of("jsc.task_manager.cpu_usage_is", "CPU Usage: %s%%");
    static final TextKey COMMIT = TextKey.of("jsc.task_manager.commit", "Commit: %sM / %sM");

    // The other desktops' lists.
    static final TextKey NAME = TextKey.of("jsc.task_manager.name", "Name");
    static final TextKey PROCESS_NAME = TextKey.of("jsc.task_manager.process_name", "Process Name");
    static final TextKey KIND = TextKey.of("jsc.task_manager.kind", "Kind");
    static final TextKey CPU = TextKey.of("jsc.task_manager.cpu", "CPU");
    static final TextKey MEMORY = TextKey.of("jsc.task_manager.memory", "Memory");
    static final TextKey END_TASK_LOWER = TextKey.of("jsc.task_manager.end_task_lower", "End task");
    static final TextKey END = TextKey.of("jsc.task_manager.end", "End");

    // The performance and network pages.
    static final TextKey PERCENT = TextKey.of("jsc.task_manager.percent", "%s %%");
    static final TextKey MEGABYTES = TextKey.of("jsc.task_manager.megabytes", "%s MB");
    static final TextKey CPU_USAGE = TextKey.of("jsc.task_manager.cpu_usage", "CPU Usage");
    static final TextKey MEMORY_USAGE = TextKey.of("jsc.task_manager.memory_usage", "Memory Usage");
    static final TextKey CPU_HISTORY = TextKey.of("jsc.task_manager.cpu_history", "CPU Usage History");
    static final TextKey MEMORY_HISTORY = TextKey.of("jsc.task_manager.memory_history", "Memory Usage History");
    static final TextKey PROGRAMS = TextKey.of("jsc.task_manager.programs", "Programs");
    static final TextKey IN_USE = TextKey.of("jsc.task_manager.in_use", "In use");
    static final TextKey FREE_MB = TextKey.of("jsc.task_manager.free_mb", "Free MB");
    static final TextKey PROCESSOR = TextKey.of("jsc.task_manager.processor", "Processor");
    static final TextKey CONNECTED = TextKey.of("jsc.task_manager.connected", "Connected");
    static final TextKey NOT_CONNECTED = TextKey.of("jsc.task_manager.not_connected", "Not connected");
    static final TextKey LINK_ACTIVITY =
            TextKey.of("jsc.task_manager.link_activity", "Link activity over the last minute");
    static final TextKey MEMORY_OF = TextKey.of("jsc.task_manager.memory_of", "%s / %s MB");
    static final TextKey LOAD_AND_CLOCK = TextKey.of("jsc.task_manager.load_and_clock", "%s%%  %s");
    static final TextKey SYSTEM_DISK = TextKey.of("jsc.task_manager.system_disk", "%s (system)");

    // What a process is.
    static final TextKey KIND_SYSTEM = TextKey.of("jsc.task_manager.kind_system", "system");
    static final TextKey KIND_DESKTOP = TextKey.of("jsc.task_manager.kind_desktop", "desktop");
    static final TextKey KIND_SERVICE = TextKey.of("jsc.task_manager.kind_service", "service");
    static final TextKey KIND_PROGRAM = TextKey.of("jsc.task_manager.kind_program", "program");
    static final TextKey KIND_SCRIPT = TextKey.of("jsc.task_manager.kind_script", "script");
    static final TextKey KIND_GRAPHICS = TextKey.of("jsc.task_manager.kind_graphics", "graphics");
    static final TextKey SHARED_GRAPHICS = TextKey.of("jsc.task_manager.shared_graphics", "Graphics (shared memory)");
    // The graphics card: its page and its part of the performance page.
    static final TextKey GPU = TextKey.of("jsc.task_manager.gpu", "GPU");
    static final TextKey NO_GPU = TextKey.of("jsc.task_manager.no_gpu", "This computer has no graphics.");
    static final TextKey GPU_3D = TextKey.of("jsc.task_manager.gpu_3d", "3D");
    static final TextKey DEDICATED_MEMORY_USAGE = TextKey.of("jsc.task_manager.dedicated_memory_usage",
            "Dedicated GPU memory usage");
    static final TextKey SHARED_MEMORY_USAGE = TextKey.of("jsc.task_manager.shared_memory_usage",
            "Shared GPU memory usage");
    static final TextKey UTILIZATION = TextKey.of("jsc.task_manager.utilization", "Utilization");
    static final TextKey DEDICATED_MEMORY = TextKey.of("jsc.task_manager.dedicated_memory", "Dedicated memory");
    static final TextKey SHARED_MEMORY = TextKey.of("jsc.task_manager.shared_memory", "Shared memory");
    static final TextKey CORES = TextKey.of("jsc.task_manager.cores", "Cores");
    static final TextKey CORES_AT = TextKey.of("jsc.task_manager.cores_at", "%s at %s");
    static final TextKey SLOT = TextKey.of("jsc.task_manager.slot", "Slot");
    static final TextKey ON_THE_PROCESSOR = TextKey.of("jsc.task_manager.on_the_processor", "On the processor");
    static final TextKey USED_BY = TextKey.of("jsc.task_manager.used_by", "Used by");
    static final TextKey VIDEO_MEMORY = TextKey.of("jsc.task_manager.video_memory", "Video Memory");
    static final TextKey VIDEO_MEMORY_HISTORY = TextKey.of("jsc.task_manager.video_memory_history",
            "Video Memory History");
    static final TextKey VIDEO_TOTAL = TextKey.of("jsc.task_manager.video_total", "Video total");
    static final TextKey VIDEO_MONITORS = TextKey.of("jsc.task_manager.video_monitors", "Monitors");
    static final TextKey VIDEO_FREE = TextKey.of("jsc.task_manager.video_free", "Video free");
    static final TextKey VIDEO_OF = TextKey.of("jsc.task_manager.video_of", "%s of %s");

    private TaskManagerTexts() {
    }
}
