/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import java.util.HashMap;
import java.util.Map;

/**
 * The computers listening to a Soundfoundry Server, and what they cost it in memory: the server holds a stream open
 * for each of them, so a busy server really runs out of room, and a computer that asks to listen then is turned away.
 *
 * <p>A computer playing from the server says so again every little while; one that has not for {@link #FORGET_AFTER}
 * ticks is let go of, whether it stopped, went off or left the network without saying.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class SoundfoundryListeners {

    /** When each listening computer last said it was listening, by where it stands. */
    private final Map<Long, Long> heard = new HashMap<>();

    /** What each computer listening costs the server in memory, past the service's own floor. */
    public static final int RAM_PER_LISTENER_MB = 4;
    /** How long a computer that has said nothing is still counted, in ticks. */
    public static final long FORGET_AFTER = 40L;

    /**
     * The computer at {@code computer} is listening at {@code now}.
     *
     * @return whether it was not listening before
     */
    public boolean listen(final long computer, final long now) {
        forget(now);
        return heard.put(computer, now) == null;
    }

    /** Whether that computer is listening. */
    public boolean listening(final long computer, final long now) {
        forget(now);
        return heard.containsKey(computer);
    }

    /** The computer stopped listening. */
    public void leave(final long computer) {
        heard.remove(computer);
    }

    /** How many computers are listening. */
    public int count(final long now) {
        forget(now);
        return heard.size();
    }

    /** What the listeners cost the server in memory now, past the service's floor. */
    public int ramMb(final long now) {
        return count(now) * RAM_PER_LISTENER_MB;
    }

    /** Lets every listener go, the server having stopped. */
    public void clear() {
        heard.clear();
    }

    private void forget(final long now) {
        heard.values().removeIf(at -> now - at > FORGET_AFTER);
    }
}
