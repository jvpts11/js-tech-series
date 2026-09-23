/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.hardware.DiskSpec;
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
public record WorkstationFacts(String userName, String hostName, String network, String system,
                               String architecture, String windowSystem, String processor, int processorMhz,
                               long memoryMb, long memoryUsedMb, long videoMb, long diskMb, long diskUsedMb) {

    /** A group of facts under its heading, which the window draws in a well of its own. */
    public record Group(String title, List<Row> rows) {
    }

    /**
     * One fact: what it is, what it reads, and whether a meter beside the value shows how much of a whole it is.
     */
    public record Row(String label, String value, boolean meter) {

        /** A fact read as it is, with no meter. */
        public Row(final String label, final String value) {
            this(label, value, false);
        }
    }

    /** What the window says of a machine that is on no data network. */
    public static final String NO_NETWORK = "Not on a network";

    public WorkstationFacts {
        userName = orNothing(userName);
        hostName = orNothing(hostName);
        network = orNothing(network);
        system = orNothing(system);
        architecture = orNothing(architecture);
        windowSystem = orNothing(windowSystem);
        processor = orNothing(processor);
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
                new Group("Workstation", List.of(
                        new Row("User Name", this.userName),
                        new Row("Host Name", this.hostName),
                        new Row("Network", this.network.isEmpty() ? NO_NETWORK : this.network))),
                new Group("System", List.of(
                        new Row("Operating System", this.system),
                        new Row("Architecture", this.architecture),
                        new Row("Window System", this.windowSystem))),
                new Group("Hardware", List.of(
                        new Row("Processor", processorText()),
                        new Row("Physical Memory", this.memoryMb + " MB"),
                        new Row("Memory in Use", this.memoryUsedMb + " MB", true),
                        new Row("Video Memory", this.videoMb + " MB"),
                        new Row("System Disk", DiskSpec.sizeLabel(this.diskMb) + ", "
                                + DiskSpec.sizeLabel(this.diskUsedMb) + " used"))));
    }

    /** How much of the memory is held, from nought to one, which the meter beside it shows. */
    public double memoryShare() {
        return this.memoryMb == 0L ? 0.0 : (double) this.memoryUsedMb / this.memoryMb;
    }

    private String processorText() {
        if (this.processor.isEmpty()) {
            return "None";
        }
        return this.processorMhz == 0 ? this.processor : this.processor + ", " + this.processorMhz + " MHz";
    }

    private static String orNothing(final String text) {
        return text == null ? "" : text;
    }
}
