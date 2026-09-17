/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.CopyFilePayload;
import dev.jstech.computers.operation.payload.MoveFilePayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * What the explorer has taken, and whether taking it was a move.
 *
 * <p>Cut and copy differ in one thing only: whether the files leave where they were. Holding that
 * difference as a flag beside the paths, rather than as two ways of doing the same thing, is what makes
 * pasting one operation and what makes a cut clear itself afterwards while a copy does not.
 *
 * <p>It holds paths rather than files. A file the player cut may be gone, renamed or replaced by the time
 * they paste, and the machine is the one that knows: sending it a path it cannot find is an answer, while
 * holding a copy of a file that no longer exists would be a lie the explorer told itself.
 *
 * <p>A read-only entry is never taken. Those are projections of what the computer holds rather than files,
 * so there is nothing to move, and leaving them out here is what stops a paste from half working.
 */
final class FileClipboard {

    /** The paths waiting to be pasted. */
    private final List<String> paths = new ArrayList<>();

    /** Whether pasting them moves them rather than copying them. */
    private boolean cut;

    /** Whether anything is waiting, which is what greys out Paste. */
    boolean isEmpty() {
        return paths.isEmpty();
    }

    /** Takes these paths to be moved on the next paste. */
    void cut(final List<String> taken) {
        paths.clear();
        paths.addAll(taken);
        cut = true;
    }

    /** Takes these paths to be copied on the next paste. */
    void copy(final List<String> taken) {
        paths.clear();
        paths.addAll(taken);
        cut = false;
    }

    /**
     * Puts what was taken into {@code dir} on the machine at {@code host}, as a move or a copy. A cut
     * empties afterwards, because what was cut is now somewhere else; a copy stays, so it can be put in
     * several places.
     */
    void pasteInto(final BlockPos host, final String dir) {
        if (paths.isEmpty()) {
            return;
        }
        for (final String src : paths) {
            if (cut) {
                PacketDistributor.sendToServer(new MoveFilePayload(host, src, dir));
            } else {
                PacketDistributor.sendToServer(new CopyFilePayload(host, src, dir));
            }
        }
        if (cut) {
            paths.clear();
        }
        FilesApps.diskChanged();
    }
}
