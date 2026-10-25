/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.os.media.MediaDriveType;
import dev.jstech.core.jstechseries.tier.Era;
import org.jetbrains.annotations.Nullable;

/**
 * A part that goes in a case's 5.25-inch bay: an internal optical drive, which reads its discs as the external drive
 * of its kind does, or the hot-swap bay, a tray a disk slides into from the front. Any of them goes in any bay.
 *
 * @param kind what it is
 * @param era  the era it came out in, which is where it is found
 */
public record BayDriveSpec(Kind kind, Era era) {

    /** The parts a bay takes. */
    public enum Kind {

        /** A CD drive. */
        CD(MediaDriveType.CD_DRIVE),

        /** A DVD drive, which reads CDs too. */
        DVD(MediaDriveType.DVD_DRIVE),

        /** A Blu-ray drive, which reads DVDs and CDs too. */
        BLU_RAY(MediaDriveType.BLU_RAY_DRIVE),

        /** The hot-swap bay: a disk slides into it from the front. */
        HOT_SWAP(null);

        @Nullable
        private final MediaDriveType reads;

        Kind(@Nullable final MediaDriveType reads) {
            this.reads = reads;
        }

        /** The drive it reads its discs as, or null for the hot-swap bay, which holds a disk rather than a disc. */
        @Nullable
        public MediaDriveType reads() {
            return reads;
        }
    }

    /** Whether it is an optical drive, with a tray for a disc. */
    public boolean optical() {
        return kind.reads() != null;
    }
}
