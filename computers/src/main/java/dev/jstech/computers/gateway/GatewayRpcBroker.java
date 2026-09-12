/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * The questions this Gateway has put to the computers on the other side, and who is waiting for each.
 *
 * <p>A question is remembered by the name it was sent under, which says everything needed to deliver its
 * answer: which computer was asked, and which program here is waiting. So an answer is delivered by
 * looking one name up, never by walking every program on the machine, and two things that would
 * otherwise go wrong cannot: an answer cannot reach a program that asked a different Gateway, and a
 * computer over there cannot answer a question that was put to another computer.
 *
 * <p>What is remembered is small and short-lived. Nothing here is saved with the world: a question whose
 * answer never came is not a question any more once the world has been read back, and the program that
 * asked is told as much rather than asking again, so nothing with a consequence happens twice.
 */
public final class GatewayRpcBroker {

    /**
     * Who is waiting for an answer.
     *
     * <p>Two kinds, and no third: a program on the host machine, or a player looking at a folder of that
     * computer in the file explorer. Both wait the same way and both are told the same way; what differs
     * is only where the answer goes when it lands.
     */
    public sealed interface Waiting {

        /** A program of ours, parked on the call it made. */
        record ByProgram(int id) implements Waiting {
        }

        /** A player with the explorer open, who will be sent what comes back. */
        record ByPlayer(java.util.UUID id, String path) implements Waiting {
        }
    }

    /** One question in flight: who was asked, who is waiting, and when the waiting stops mattering. */
    public record Pending(GatewayRequestId id, int computer, Waiting who, String verb, long expiresAt) {
    }

    /** What became of an answer that arrived. */
    public enum Landing {
        /** It reached the program that was waiting for it. */
        DELIVERED,
        /** Nobody is waiting for it any more: the program gave up, or was stopped. */
        UNKNOWN,
        /** It came from a computer that was not the one asked. */
        WRONG_COMPUTER,
        /** It said more than an answer may say. */
        TOO_MUCH
    }

    /** The most questions that may be in flight at once, so a program cannot fill the machine with them. */
    private static final int IN_FLIGHT = 64;

    private final Map<GatewayRequestId, Pending> pending = new LinkedHashMap<>();

    /** Remembers a question about to be sent; null when too many are already out. */
    @Nullable
    public Pending submit(final int computer, final Waiting who, final String verb, final long expiresAt) {
        if (this.pending.size() >= IN_FLIGHT) {
            return null;
        }
        final Pending made = new Pending(GatewayRequestId.made(), computer, who, verb, expiresAt);
        this.pending.put(made.id(), made);
        return made;
    }

    /**
     * Takes an answer off the list and says what became of it.
     *
     * <p>The answer is measured before anything else, because refusing it is the point of measuring:
     * what is too big never reaches the program and the question is closed all the same.
     */
    public Landing landing(@Nullable final GatewayRequestId id, final int from, @Nullable final Object answer) {
        final Pending waiting = id == null ? null : this.pending.get(id);
        if (waiting == null) {
            return Landing.UNKNOWN;
        }
        if (waiting.computer() != from) {
            return Landing.WRONG_COMPUTER;
        }
        this.pending.remove(id);
        return GatewayLimits.refuse(answer) == null ? Landing.DELIVERED : Landing.TOO_MUCH;
    }

    /** The question of that name, or null when there is none waiting. */
    @Nullable
    public Pending waiting(@Nullable final GatewayRequestId id) {
        return id == null ? null : this.pending.get(id);
    }

    /** Forgets every question put to that computer, which is what it going away means; the ones forgotten. */
    public List<Pending> forget(final int computer) {
        if (this.pending.isEmpty()) {
            return List.of();
        }
        final List<Pending> gone = new java.util.ArrayList<>();
        for (final Pending one : List.copyOf(this.pending.values())) {
            if (one.computer() == computer) {
                this.pending.remove(one.id());
                gone.add(one);
            }
        }
        return gone;
    }

    /**
     * Forgets the questions whose waiting is over; the ones forgotten, for the log.
     *
     * <p>Costs nothing while nothing is in flight, which is almost always.
     */
    public List<Pending> expired(final long now) {
        if (this.pending.isEmpty()) {
            return List.of();
        }
        final List<Pending> over = new java.util.ArrayList<>();
        for (final Pending one : List.copyOf(this.pending.values())) {
            if (now >= one.expiresAt()) {
                this.pending.remove(one.id());
                over.add(one);
            }
        }
        return over;
    }

    /** How many questions are in flight, for the Gateway's own reckoning. */
    public int inFlight() {
        return this.pending.size();
    }
}
