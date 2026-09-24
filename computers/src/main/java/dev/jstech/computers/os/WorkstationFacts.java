/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * What a workstation is, as CDE's Workstation Info shows it: who is at it and where it stands, the system under
 * the desktop, and the hardware the system runs on. Every fact is one the machine really holds; the ones a real
 * workstation also reported and this one has nothing behind (a host id, an address, when it last started) are not
 * here at all.
 *
 * <p>Pure, with no Minecraft types, so how each fact reads is unit-tested.
 *
 * @param userName     the account the system's prompt is for
 * @param hostName     the name the machine answers to at a prompt
 * @param network      the id of the data network the machine is on, or empty when it is on none
 * @param system       the system by name with its release, as {@code uname} would put them together
 * @param architecture the system's word for the processor's architecture
 * @param windowSystem the desktop by name with its version
 * @param processor    the processor by its own model name, or empty when it has none
 * @param processorMhz the clock the processor runs at
 * @param memoryMb     the memory the machine has
 * @param memoryUsedMb the memory what runs on it holds right now
 * @param videoMb      the memory of its graphics
 * @param diskMb       the size of the disk the system is on
 * @param diskUsedMb   how much of that disk is taken
 */
@TextHolder
public record WorkstationFacts(String userName, String hostName, String network, String system,
                               String architecture, String windowSystem, Text processor, int processorMhz,
                               long memoryMb, long memoryUsedMb, long videoMb, long diskMb, long diskUsedMb) {

    /** A group of facts under its heading, which the window draws in a well of its own. */
    public record Group(Text title, List<Row> rows) {
    }

    /**
     * One fact: what it is, what it reads, and whether a meter beside the value shows how much of a whole it is.
     */
    public record Row(Text label, Text value, boolean meter) {

        /** A fact read as it is, with no meter. */
        public Row(final Text label, final Text value) {
            this(label, value, false);
        }
    }

    /** What the window says of a machine that is on no data network. */
    public static final TextKey NO_NETWORK = TextKey.of("jsc.workstation.no_network", "Not on a network");

    private static final TextKey WORKSTATION = TextKey.of("jsc.workstation.group.workstation", "Workstation");
    private static final TextKey SYSTEM = TextKey.of("jsc.workstation.group.system", "System");
    private static final TextKey HARDWARE = TextKey.of("jsc.workstation.group.hardware", "Hardware");
    private static final TextKey USER_NAME = TextKey.of("jsc.workstation.user_name", "User Name");
    private static final TextKey HOST_NAME = TextKey.of("jsc.workstation.host_name", "Host Name");
    private static final TextKey NETWORK = TextKey.of("jsc.workstation.network", "Network");
    private static final TextKey OPERATING_SYSTEM = TextKey.of("jsc.workstation.operating_system", "Operating System");
    private static final TextKey ARCHITECTURE = TextKey.of("jsc.workstation.architecture", "Architecture");
    private static final TextKey WINDOW_SYSTEM = TextKey.of("jsc.workstation.window_system", "Window System");
    private static final TextKey PROCESSOR = TextKey.of("jsc.workstation.processor", "Processor");
    private static final TextKey PHYSICAL_MEMORY = TextKey.of("jsc.workstation.physical_memory", "Physical Memory");
    private static final TextKey MEMORY_IN_USE = TextKey.of("jsc.workstation.memory_in_use", "Memory in Use");
    private static final TextKey VIDEO_MEMORY = TextKey.of("jsc.workstation.video_memory", "Video Memory");
    private static final TextKey SYSTEM_DISK = TextKey.of("jsc.workstation.system_disk", "System Disk");
    private static final TextKey MEGABYTES = TextKey.of("jsc.workstation.megabytes", "%s MB");
    private static final TextKey DISK_USE = TextKey.of("jsc.workstation.disk_use", "%s, %s used");
    private static final TextKey NO_PROCESSOR = TextKey.of("jsc.workstation.no_processor", "None");
    private static final TextKey PROCESSOR_CLOCK = TextKey.of("jsc.workstation.processor_clock", "%s, %s MHz");

    public WorkstationFacts {
        userName = orNothing(userName);
        hostName = orNothing(hostName);
        network = orNothing(network);
        system = orNothing(system);
        architecture = orNothing(architecture);
        windowSystem = orNothing(windowSystem);
        processor = processor == null ? Text.EMPTY : processor;
        processorMhz = Math.max(0, processorMhz);
        memoryMb = Math.max(0L, memoryMb);
        memoryUsedMb = Math.max(0L, Math.min(memoryMb, memoryUsedMb));
        videoMb = Math.max(0L, videoMb);
        diskMb = Math.max(0L, diskMb);
        diskUsedMb = Math.max(0L, Math.min(diskMb, diskUsedMb));
    }

    /** The three groups the window shows, in its order. */
    public List<Group> groups() {
        return List.of(
                new Group(WORKSTATION.text(), List.of(
                        new Row(USER_NAME.text(), Text.literal(this.userName)),
                        new Row(HOST_NAME.text(), Text.literal(this.hostName)),
                        new Row(NETWORK.text(),
                                this.network.isEmpty() ? NO_NETWORK.text() : Text.literal(this.network)))),
                new Group(SYSTEM.text(), List.of(
                        new Row(OPERATING_SYSTEM.text(), Text.literal(this.system)),
                        new Row(ARCHITECTURE.text(), Text.literal(this.architecture)),
                        new Row(WINDOW_SYSTEM.text(), Text.literal(this.windowSystem)))),
                new Group(HARDWARE.text(), List.of(
                        new Row(PROCESSOR.text(), processorText()),
                        new Row(PHYSICAL_MEMORY.text(), MEGABYTES.with(this.memoryMb)),
                        new Row(MEMORY_IN_USE.text(), MEGABYTES.with(this.memoryUsedMb), true),
                        new Row(VIDEO_MEMORY.text(), MEGABYTES.with(this.videoMb)),
                        new Row(SYSTEM_DISK.text(), DISK_USE.with(DiskSpec.sizeLabel(this.diskMb),
                                DiskSpec.sizeLabel(this.diskUsedMb))))));
    }

    /** How much of the memory is held, from nought to one, which the meter beside it shows. */
    public double memoryShare() {
        return this.memoryMb == 0L ? 0.0 : (double) this.memoryUsedMb / this.memoryMb;
    }

    private Text processorText() {
        if (this.processor.isEmpty()) {
            return NO_PROCESSOR.text();
        }
        return this.processorMhz == 0 ? this.processor : PROCESSOR_CLOCK.with(this.processor, this.processorMhz);
    }

    private static String orNothing(final String text) {
        return text == null ? "" : text;
    }
}
