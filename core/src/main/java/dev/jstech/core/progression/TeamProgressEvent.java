/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/**
 * A team came further, posted on the game's event bus: a step of an axis further than any it had, a milestone, or
 * more of a piece of knowledge. Each is posted once for each move forward, never when something already had is had
 * again.
 */
public abstract sealed class TeamProgressEvent extends Event
        permits TeamProgressEvent.StepReached, TeamProgressEvent.MilestoneReached, TeamProgressEvent.KnowledgeGained {

    private final MinecraftServer server;
    private final String team;

    protected TeamProgressEvent(final MinecraftServer server, final String team) {
        this.server = server;
        this.team = team;
    }

    public MinecraftServer server() {
        return this.server;
    }

    /** The team, by the id its source gives it. */
    public String team() {
        return this.team;
    }

    /** The team reached a step of an axis further than any it had. */
    public static final class StepReached extends TeamProgressEvent {

        private final ProgressionAxis<?> axis;
        private final IAxisStep from;
        private final IAxisStep to;

        public StepReached(final MinecraftServer server, final String team, final ProgressionAxis<?> axis,
                           final IAxisStep from, final IAxisStep to) {
            super(server, team);
            this.axis = axis;
            this.from = from;
            this.to = to;
        }

        public ProgressionAxis<?> axis() {
            return this.axis;
        }

        /** The furthest step the team had reached before. */
        public IAxisStep from() {
            return this.from;
        }

        /** The step reached now. */
        public IAxisStep to() {
            return this.to;
        }
    }

    /** The team reached a milestone, for the first time. */
    public static final class MilestoneReached extends TeamProgressEvent {

        private final Milestone milestone;

        public MilestoneReached(final MinecraftServer server, final String team, final Milestone milestone) {
            super(server, team);
            this.milestone = milestone;
        }

        public Milestone milestone() {
            return this.milestone;
        }
    }

    /** The team came to know more of a piece of knowledge: some of it, or all. */
    public static final class KnowledgeGained extends TeamProgressEvent {

        private final Knowledge knowledge;
        private final double before;
        private final double now;

        public KnowledgeGained(final MinecraftServer server, final String team, final Knowledge knowledge,
                               final double before, final double now) {
            super(server, team);
            this.knowledge = knowledge;
            this.before = before;
            this.now = now;
        }

        public Knowledge knowledge() {
            return this.knowledge;
        }

        /** How much of it the team knew before, from 0 to 1. */
        public double before() {
            return this.before;
        }

        /** How much of it the team knows now, from 0 to 1. */
        public double now() {
            return this.now;
        }

        /** Whether the team knows it whole now, having not before. */
        public boolean learnt() {
            return this.now >= 1.0 && this.before < 1.0;
        }
    }
}
