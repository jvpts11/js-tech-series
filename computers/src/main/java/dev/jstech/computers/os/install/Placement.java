/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

/**
 * Where on its disk the player told the installer to put the system (computers/docs/design/storage-devices.md): on
 * which partition or in which free space, with which file system, beside which system and how much of its partition
 * that one keeps, and, for ZFS, the second disk of a mirror. What is left at its default the installer decides itself.
 *
 * @param startMb    where the chosen partition or free space starts, or {@link #ANYWHERE}
 * @param fileSystem the file system to format with by its serialized name, {@link #KEEP} to leave the partition's as it
 *                   is, or empty for the installer's own
 * @param shrinkToMb how big the partition of the system beside it is left, or 0 when nothing is shrunk
 * @param mirrorSlot the slot of the second disk of a ZFS mirror, or {@link InstallerFlow#NO_DISK}
 */
public record Placement(long startMb, String fileSystem, long shrinkToMb, int mirrorSlot) {

    /** The installer decides where. */
    public static final long ANYWHERE = -1L;

    /** The partition's file system left as it is, with what it holds. */
    public static final String KEEP = "keep";

    /** Nothing chosen: the installer decides it all. */
    public static final Placement NONE = new Placement(ANYWHERE, "", 0L, InstallerFlow.NO_DISK);

    public Placement {
        fileSystem = fileSystem == null ? "" : fileSystem;
        shrinkToMb = Math.max(0L, shrinkToMb);
    }

    /** On the partition or in the free space starting at {@code mb}, nothing shrunk. */
    public Placement at(final long mb) {
        return new Placement(mb, this.fileSystem, 0L, this.mirrorSlot);
    }

    /** Formatted with that file system, or {@link #KEEP}, or empty for the installer's own. */
    public Placement formattedAs(final String name) {
        return new Placement(this.startMb, name, this.shrinkToMb, this.mirrorSlot);
    }

    /** Beside the system on its disk, whose partition is left at {@code mb}. */
    public Placement shrinking(final long mb) {
        return new Placement(ANYWHERE, this.fileSystem, mb, this.mirrorSlot);
    }

    /** With the disk in {@code slot} as the second of a ZFS mirror, or none. */
    public Placement mirroredOn(final int slot) {
        return new Placement(this.startMb, this.fileSystem, this.shrinkToMb, slot);
    }

    /** Where it goes and what is shrunk forgotten, as choosing another disk forgets them; the file system stays. */
    public Placement onAnotherDisk() {
        return new Placement(ANYWHERE, this.fileSystem, 0L, this.mirrorSlot);
    }
}
