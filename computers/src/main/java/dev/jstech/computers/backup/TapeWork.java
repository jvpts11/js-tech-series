/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import dev.jstech.core.uuid.NetworkUuid;
import java.util.Set;
import java.util.UUID;

/**
 * Something the Backup Service gave a Tape Library to do, waiting for its drive: a backup to write, or one to read back
 * and restore. Which tape and where on it is worked out when the drive takes it, from the tapes as they are then, so a
 * write queued behind another lands after it. Kept only while the world runs: a job under way when the world closes is
 * written down as interrupted.
 */
public sealed interface TapeWork permits TapeWork.Write, TapeWork.Read {

    /** The job it is for, as the console names it. */
    String job();

    /**
     * A backup to write onto the first tape with room for it.
     *
     * @param backup      the backup, kept by the world once it is written
     * @param size        how much of a tape it takes, in items of the library's era
     * @param serviceRack the rack of the server running the service whose job it is, as a packed position
     * @param serviceRow  that server's row
     */
    record Write(BackupArchive.Backup backup, long size, long serviceRack, int serviceRow) implements TapeWork {

        @Override
        public String job() {
            return backup.job();
        }
    }

    /**
     * A backup to read back and restore.
     *
     * @param backup  the backup
     * @param job     the job that made it
     * @param network the network it is restored onto
     * @param parts   what of it is restored
     * @param images  the computers whose system images are restored, by their nodes
     */
    record Read(UUID backup, String job, NetworkUuid network, Set<BackupPart> parts, Set<UUID> images)
            implements TapeWork {

        public Read {
            parts = Set.copyOf(parts);
            images = Set.copyOf(images);
        }
    }
}
