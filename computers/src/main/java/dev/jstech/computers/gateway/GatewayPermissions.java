/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import dev.jstech.core.operation.OperationPriority;

/**
 * What a Gateway lets the ComputerCraft side do. Three switches (read the network, run operations, reach
 * the shared folders) and two knobs that keep a CC program from crowding the host: the highest priority
 * a request from CC may carry, and how many calls a tick the Gateway answers.
 *
 * @param read       whether CC may read the network (types, totals, servers, watches)
 * @param operations whether CC may pull, push, craft, cancel and start programs
 * @param files      how far CC may reach into the shared folders
 * @param ceiling    the priority a CC request can never outrank
 * @param callCap    calls answered per tick, one of {@link #CAPS}
 */
public record GatewayPermissions(boolean read, boolean operations, FileAccess files, OperationPriority ceiling,
                                 int callCap) {

    /** How far the ComputerCraft side may reach into the folders our computers share. */
    public enum FileAccess {
        OFF("off"),
        READ("read"),
        READ_WRITE("read & write");

        private final String label;

        FileAccess(final String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static FileAccess at(final int index) {
            final FileAccess[] all = values();
            return all[Math.max(0, Math.min(all.length - 1, index))];
        }
    }

    /** The priorities a ceiling may sit at, in order. */
    public static final OperationPriority[] CEILINGS =
            {OperationPriority.LOW, OperationPriority.MEDIUM, OperationPriority.HIGH};
    /** The call caps a Gateway offers, in order. */
    public static final int[] CAPS = {4, 8, 16};

    /** A fresh Gateway: reads and operations allowed, files readable, medium ceiling, eight calls a tick. */
    public static final GatewayPermissions DEFAULT =
            new GatewayPermissions(true, true, FileAccess.READ, OperationPriority.MEDIUM, 8);

    public GatewayPermissions {
        if (files == null) {
            files = FileAccess.READ;
        }
        ceiling = CEILINGS[ceilingIndexOf(ceiling)];
        callCap = CAPS[capIndexOf(callCap)];
    }

    /** Builds permissions from the plain numbers the wire and the save carry. */
    public static GatewayPermissions of(final boolean read, final boolean operations, final int files,
                                        final int ceiling, final int cap) {
        return new GatewayPermissions(read, operations, FileAccess.at(files), ceilingAt(ceiling), capAt(cap));
    }

    public static OperationPriority ceilingAt(final int index) {
        return CEILINGS[Math.max(0, Math.min(CEILINGS.length - 1, index))];
    }

    public static int capAt(final int index) {
        return CAPS[Math.max(0, Math.min(CAPS.length - 1, index))];
    }

    public GatewayPermissions withRead(final boolean value) {
        return new GatewayPermissions(value, operations, files, ceiling, callCap);
    }

    public GatewayPermissions withOperations(final boolean value) {
        return new GatewayPermissions(read, value, files, ceiling, callCap);
    }

    public GatewayPermissions withFiles(final FileAccess value) {
        return new GatewayPermissions(read, operations, value, ceiling, callCap);
    }

    public GatewayPermissions withCeiling(final OperationPriority value) {
        return new GatewayPermissions(read, operations, files, value, callCap);
    }

    public GatewayPermissions withCallCap(final int value) {
        return new GatewayPermissions(read, operations, files, ceiling, value);
    }

    public int filesIndex() {
        return files.ordinal();
    }

    public int ceilingIndex() {
        return ceilingIndexOf(ceiling);
    }

    public int capIndex() {
        return capIndexOf(callCap);
    }

    public boolean allowsWrite() {
        return files == FileAccess.READ_WRITE;
    }

    /** The priority a CC request actually runs at: what it asked for, no higher than the ceiling. */
    public OperationPriority cap(final OperationPriority asked) {
        return asked == null || asked.ordinal() > ceiling.ordinal() ? ceiling : asked;
    }

    /** The index of the ceiling that is at least {@code priority}, so any priority maps onto a knob position. */
    private static int ceilingIndexOf(final OperationPriority priority) {
        if (priority == null) {
            return 1;
        }
        for (int i = 0; i < CEILINGS.length; i++) {
            if (priority.ordinal() <= CEILINGS[i].ordinal()) {
                return i;
            }
        }
        return CEILINGS.length - 1;
    }

    /** The index of the cap that is at least {@code cap}, so any number maps onto a knob position. */
    private static int capIndexOf(final int cap) {
        for (int i = 0; i < CAPS.length; i++) {
            if (cap <= CAPS[i]) {
                return i;
            }
        }
        return CAPS.length - 1;
    }
}
