/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.computers.os.IOsHost;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Every drive a machine reads removable media in, in the one order the machine letters and lists them: the readers
 * linked to it by cable in the order of their positions, each Dock Station's docked disks right after its own port,
 * top tray first. A reader is a drive whether or not anything is in it; a dock's tray is one only while it holds a
 * disk.
 *
 * <p>This is the one place that looks for a machine's media. The drive letters, This PC, the explorers, the firmware,
 * the installers, the Crafting Manager, the Pattern Studio and the live media all ask it, so they agree on what a
 * machine has and in what order. Like the drives themselves, the answer is good for one operation: a disc can be
 * swapped or a cable cut between any two.
 */
public final class MachineMedia {

    private final List<IMediaDrive> drives;
    private final boolean complete;

    private MachineMedia(final List<IMediaDrive> drives, final boolean complete) {
        this.drives = drives;
        this.complete = complete;
    }

    /** The drives that machine has as they are now. */
    public static MachineMedia of(final IOsHost machine, final Level level) {
        final List<Long> endpoints = new ArrayList<>(machine.enabledEndpoints());
        Collections.sort(endpoints);
        final List<IMediaDrive> drives = new ArrayList<>();
        boolean complete = true;
        for (final long endpoint : endpoints) {
            final BlockPos pos = BlockPos.of(endpoint);
            if (!level.isLoaded(pos)) {
                complete = false;
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof MediaReaderBlockEntity reader)) {
                continue;
            }
            drives.add(new IMediaDrive.Reader(reader));
            if (reader instanceof DockStationBlockEntity dock) {
                for (int bay = 0; bay < DockStationBlockEntity.BAYS; bay++) {
                    if (!dock.disk(bay).isEmpty()) {
                        drives.add(new IMediaDrive.Docked(dock, bay));
                    }
                }
            }
        }
        return new MachineMedia(List.copyOf(drives), complete);
    }

    /** Every drive, in the machine's order. */
    public List<IMediaDrive> drives() {
        return drives;
    }

    /**
     * Whether every device linked to the machine could be looked at. One in a part of the world that is not loaded is
     * not seen at all, so a medium missing from these drives may still be in it.
     */
    public boolean complete() {
        return complete;
    }

    /** The drive at that volume, or null when the machine has none there. */
    @Nullable
    public IMediaDrive find(final MediaVolume volume) {
        for (final IMediaDrive drive : drives) {
            if (drive.volume().equals(volume)) {
                return drive;
            }
        }
        return null;
    }

    /** The first drive holding a medium of that kind that names what it installs, or null when none does. */
    @Nullable
    public IMediaDrive installing(final MediaKind kind) {
        for (final IMediaDrive drive : drives) {
            if (drive.installs(kind)) {
                return drive;
            }
        }
        return null;
    }
}
