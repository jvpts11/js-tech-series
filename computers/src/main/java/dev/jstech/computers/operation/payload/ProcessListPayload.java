/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server to client when the terminal's Processes tab opens: the processes running on THIS computer (the
 * host the terminal is attached to), for the task manager. On a Mainframe that is the IQL Engine service and
 * each of its jobs; a computer with no service sends an empty list. Each line carries its kind (so the tab
 * picks the icon and the right actions), its name, its state and a short detail (uptime, the job's trigger,
 * or its condition).
 */
public record ProcessListPayload(List<ProcessLine> processes) implements CustomPacketPayload {

    public static final int MAX = 64;

    /** A service process (a background daemon, e.g. the IQL Engine): actions are Start/Stop/Restart. */
    public static final int KIND_SERVICE = 0;
    /** A job process (a scheduled IQL job): actions are End (pause) / Restart (re-arm). */
    public static final int KIND_JOB = 1;

    public static final CustomPacketPayload.Type<ProcessListPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "process_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessListPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ProcessLine.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), ProcessListPayload::processes,
                    ProcessListPayload::new);

    /* Copied on the way in, so what the client is handed cannot change under it after it arrives. */
    public ProcessListPayload {
        processes = List.copyOf(processes);
    }

    @Override
    public CustomPacketPayload.Type<ProcessListPayload> type() {
        return TYPE;
    }

    /** How a process stands, and the word the tab shows for it. */
    public enum ProcessState implements IStableId {
        /** A service that is serving. */
        RUNNING(0, "running"),
        /** A service that was stopped. */
        STOPPED(1, "stopped"),
        /** A job whose service is running, so it fires when its moment comes. */
        ACTIVE(2, "active"),
        /** A job the player paused. */
        PAUSED(3, "paused"),
        /** A job whose service is stopped, so nothing fires it. */
        IDLE(4, "idle");

        private final int id;
        private final String word;

        ProcessState(final int id, final String word) {
            this.id = id;
            this.word = word;
        }

        @Override
        public int id() {
            return this.id;
        }

        /** What the tab calls it. */
        public String word() {
            return this.word;
        }

        /** Whether the process is doing its work right now, which is what Stop, rather than Start, acts on. */
        public boolean running() {
            return this == RUNNING || this == ACTIVE;
        }
    }

    /** One process: its kind, display name, current state, and a short detail line. */
    public record ProcessLine(int kind, String name, ProcessState state, String detail) {

        public static final StreamCodec<RegistryFriendlyByteBuf, ProcessLine> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, ProcessLine::kind,
                        ByteBufCodecs.stringUtf8(48), ProcessLine::name,
                        StableCodecs.byId(ProcessState.class, ProcessState.STOPPED), ProcessLine::state,
                        ByteBufCodecs.stringUtf8(64), ProcessLine::detail,
                        ProcessLine::new);
    }
}
