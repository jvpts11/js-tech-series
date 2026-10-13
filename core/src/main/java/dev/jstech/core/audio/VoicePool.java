/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * The voices of one sound device, and which sounds hold them: a device plays so many sounds at once and no more. A
 * sound that wants voices when none are free takes them from the sounds that started first, which stop, the way the
 * sound chips of the time gave a new note the channel of the oldest; a PC speaker, with one voice, cuts each beep off
 * with the next.
 *
 * <p>A sound holds its voices until it ends, or until it is let go of or has them taken. This class is pure and
 * carries no Minecraft dependency.
 */
public final class VoicePool {

    private final List<Claim> claims = new ArrayList<>();

    /**
     * Voices held by one sound.
     *
     * @param id      what the sound is known by
     * @param voices  how many voices it holds
     * @param endsAt  when it ends by itself, in ticks, or {@link Long#MAX_VALUE} when it goes on until let go of
     */
    public record Claim(String id, int voices, long endsAt) {
    }

    /**
     * Gives a sound {@code voices} of the device's {@code capacity}, taking them from the sounds that started first
     * when too few are free. A sound that asks for more voices than the device has gets them all.
     *
     * @param now    the tick it starts at, which ends the sounds that have run out by then
     * @param endsAt when it ends by itself, or {@link Long#MAX_VALUE}
     * @return the ids of the sounds whose voices it took, which the caller stops; a sound that asked again under an id
     *         it already held is not among them
     */
    public synchronized List<String> claim(final int capacity, final String id, final int voices, final long now,
                                           final long endsAt) {
        expire(now);
        claims.removeIf(claim -> claim.id().equals(id));
        final int wanted = Math.min(Math.max(1, voices), Math.max(0, capacity));
        final List<String> stolen = new ArrayList<>();
        if (wanted == 0) {
            return stolen;
        }
        while (held() + wanted > capacity && !claims.isEmpty()) {
            stolen.add(claims.removeFirst().id());
        }
        claims.add(new Claim(id, wanted, endsAt));
        return stolen;
    }

    /** The sound let go of its voices, having stopped. */
    public synchronized void release(final String id) {
        claims.removeIf(claim -> claim.id().equals(id));
    }

    /** How many voices are held at {@code now}. */
    public synchronized int held(final long now) {
        expire(now);
        return held();
    }

    /** Whether that sound still holds its voices at {@code now}. */
    public synchronized boolean holds(final String id, final long now) {
        expire(now);
        return claims.stream().anyMatch(claim -> claim.id().equals(id));
    }

    /** Whether no sound holds any voice, however long ago it ran out. */
    public synchronized boolean empty() {
        return claims.isEmpty();
    }

    /** Every sound holding voices, the first to start first. */
    public synchronized List<Claim> claims() {
        return List.copyOf(claims);
    }

    /** Every voice is let go of, the device having gone. */
    public synchronized void clear() {
        claims.clear();
    }

    private void expire(final long now) {
        claims.removeIf(claim -> claim.endsAt() <= now);
    }

    private int held() {
        int held = 0;
        for (final Claim claim : claims) {
            held += claim.voices();
        }
        return held;
    }
}
