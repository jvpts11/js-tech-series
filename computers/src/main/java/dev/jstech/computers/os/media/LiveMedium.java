/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.computers.program.install.LiveInstallState;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Whether a machine can still see the medium it was started from.
 *
 * <p>A machine running from a live medium is running out of that medium, so taking it out ends the session:
 * that is how a real one behaves and it is what makes the drive worth anything. What matters is the
 * difference between the medium being gone and the machine being unable to tell, and that difference is the
 * whole reason this is a question with three answers instead of two.
 *
 * <p>A drive in a chunk that is not loaded yet answers nothing. A world reloading brings the computer back
 * before the drive cabled to it, and for those few ticks a two-answer check reads the drive as empty. Acting
 * on that reading throws away an installation somebody may have spent an hour on, and throws it away
 * silently, which is worse: the terminal stays on the screen and simply stops answering. So a machine that
 * cannot tell is told to wait and ask again.
 */
public final class LiveMedium {

    /** What a machine can say about the medium it was started from. */
    public enum Answer {

        /** A drive it reaches holds that medium. */
        PRESENT,

        /** Every drive it reaches was looked at, and none of them holds it. */
        GONE,

        /** At least one drive could not be looked at, so nothing can be concluded yet. */
        UNKNOWN
    }

    private LiveMedium() {
    }

    /**
     * Whether any drive the machine reaches still holds the live medium of that distribution.
     *
     * @param level     the world the machine is in, or null before it has one
     * @param endpoints the drives the machine is cabled to, as packed positions
     * @param distro    the distribution whose medium the machine was started from
     */
    public static Answer holding(final Level level, final List<Long> endpoints,
                                 final LiveInstallState.Distro distro) {
        if (level == null) {
            return Answer.UNKNOWN;
        }
        boolean certain = true;
        for (final long endpoint : endpoints) {
            final BlockPos pos = BlockPos.of(endpoint);
            if (!level.isLoaded(pos)) {
                /* Cabled to something nobody can see right now, which is not the same as cabled to nothing. */
                certain = false;
                continue;
            }
            if (level.getBlockEntity(pos) instanceof MediaReaderBlockEntity reader && holds(reader, distro)) {
                return Answer.PRESENT;
            }
        }
        return certain ? Answer.GONE : Answer.UNKNOWN;
    }

    /** The distribution a live medium is of, or nothing when it is not a live medium at all. */
    public static LiveInstallState.Distro distroOf(final ResourceLocation os) {
        if (os == null) {
            return null;
        }
        for (final LiveInstallState.Distro distro : LiveInstallState.Distro.values()) {
            if (mediumName(distro).equals(os.getPath())) {
                return distro;
            }
        }
        return null;
    }

    /** Whether that drive holds the live medium of that distribution. */
    private static boolean holds(final MediaReaderBlockEntity reader, final LiveInstallState.Distro distro) {
        return reader.insertedKind() == MediaKind.OS_INSTALL
                && reader.insertedPayload() != null
                && mediumName(distro).equals(reader.insertedPayload().getPath());
    }

    /** What that distribution's medium is called, which is the distribution's own name. */
    private static String mediumName(final LiveInstallState.Distro distro) {
        return distro == LiveInstallState.Distro.ARCH ? "arch" : "gentoo";
    }
}
