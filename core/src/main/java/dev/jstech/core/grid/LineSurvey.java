/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import dev.jstech.core.id.IStableName;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * What an instrument reads of one connected part of a line in the last tick, as a network probe shows it: each block
 * the line plugs into, with what it gave and took; what the part moved, what was asked of it and what it lost on the
 * way; how many cables it has; and its narrowest cable, the most the part carries. Amounts are in the line's own unit.
 * Positions are packed into one number each, as the game packs a block's.
 *
 * @param members   each block the part plugs into
 * @param moved     what it moved
 * @param wanted    what was asked of it; -1 when its line cannot tell
 * @param lost      what it lost on the way
 * @param cables    how many cables it has
 * @param narrowest its narrowest cable; none for a part with no limit
 */
public record LineSurvey(List<Member> members, long moved, long wanted, long lost, int cables,
                         Optional<Narrowest> narrowest) {

    /** What a line that cannot tell what was asked of it says it was asked. */
    public static final long UNKNOWN = -1L;

    public LineSurvey {
        members = List.copyOf(members);
        Objects.requireNonNull(narrowest, "narrowest");
    }

    /**
     * One block a part plugs into.
     *
     * @param pos  where it is
     * @param role what it is to the line
     * @param gave what it gave the line
     * @param took what it took from the line
     */
    public record Member(long pos, Role role, long gave, long took) {

        public Member {
            Objects.requireNonNull(role, "role");
        }
    }

    /**
     * The narrowest cable of a part: the one that carries least, which the whole part moves no more than.
     *
     * @param pos     where its block is
     * @param cable   its cable's id
     * @param ceiling the most it carries in a tick
     */
    public record Narrowest(long pos, String cable, long ceiling) {

        public Narrowest {
            Objects.requireNonNull(cable, "cable");
        }
    }

    /** What a block is to a line. */
    public enum Role implements IStableName {
        /** It gives what the line carries: a generator, a machine's output. */
        SOURCE("source"),
        /** It takes what the line carries: a machine, an input. */
        CONSUMER("consumer"),
        /** It takes and gives it: a battery, a tank, a chest. */
        STORE("store"),
        /** Nothing went in or out of it. */
        IDLE("idle");

        private final String serializedName;

        Role(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        /** What a block that gave and took as said is to a line. */
        public static Role of(final boolean gave, final boolean took) {
            if (gave && took) {
                return STORE;
            }
            return gave ? SOURCE : took ? CONSUMER : IDLE;
        }
    }
}
