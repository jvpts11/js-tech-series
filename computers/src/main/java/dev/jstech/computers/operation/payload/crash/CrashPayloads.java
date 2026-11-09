/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crash;

import dev.jstech.computers.block.ICrashScreenOpener;
import dev.jstech.computers.client.os.ActiveDesktop;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.MachineMemory;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.QuincePartitions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The payloads of a system short of memory or crashed: its page on the glass, a key on its console, the window that
 * did it, and the windows its kernel ended to make room.
 */
public final class CrashPayloads {

    private CrashPayloads() {
    }

    public static void register(final PayloadRegistrar registrar) {
        // The crashed system's page: the monitor joins it where the machine has got to.
        registrar.playToClient(OpenCrashPayload.TYPE, OpenCrashPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> ICrashScreenOpener.Holder.open(payload)));
        // Only from the crash page of that machine, by a player in front of it.
        ComputerAccess.accept(registrar, CrashKeyPayload.TYPE, CrashKeyPayload.STREAM_CODEC,
                ComputerAccess.screen(CrashKeyPayload::host, MonitorSessionMenu.Phase.SYSTEM_CRASH),
                CrashPayloads::handleKey);
        // Only from that machine's own desktop: nobody brings a machine down from anywhere but its keyboard.
        ComputerAccess.accept(registrar, MemoryShortPayload.TYPE, MemoryShortPayload.STREAM_CODEC,
                ComputerAccess.machine(MemoryShortPayload::host), CrashPayloads::handleMemoryShort);
        // Quince's question of what to force quit, and the player's answer from that machine's desktop.
        registrar.playToClient(MemoryAskPayload.TYPE, MemoryAskPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) ->
                        ActiveDesktop.memoryAsked(payload.host(), payload.programs())));
        ComputerAccess.accept(registrar, ForceQuitPayload.TYPE, ForceQuitPayload.STREAM_CODEC,
                ComputerAccess.machine(ForceQuitPayload::host), CrashPayloads::handleForceQuit);
        // A classic Quince program's Get Info, from that machine's own desktop.
        ComputerAccess.accept(registrar, PartitionPayload.TYPE, PartitionPayload.STREAM_CODEC,
                ComputerAccess.machine(PartitionPayload::host), CrashPayloads::handlePartition);
        // The windows a kernel ended for their memory: the desktop showing that machine closes them.
        registrar.playToClient(MemoryKilledPayload.TYPE, MemoryKilledPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) ->
                        ActiveDesktop.memoryKilled(payload.host(), payload.windows())));
    }

    private static void handleKey(final CrashKeyPayload payload, final ServerPlayer player, final ServerLevel level) {
        if (level.getBlockEntity(payload.host()) instanceof IOsHost machine) {
            machine.crashKey();
        }
    }

    private static void handleMemoryShort(final MemoryShortPayload payload, final ServerPlayer player,
                                          final ServerLevel level) {
        if (level.getBlockEntity(payload.host()) instanceof IOsHost machine) {
            machine.memoryShort(payload.program(), 0);
        }
    }

    private static void handleForceQuit(final ForceQuitPayload payload, final ServerPlayer player,
                                        final ServerLevel level) {
        if (level.getBlockEntity(payload.host()) instanceof IOsHost machine) {
            machine.forceQuit(payload.tokens());
        }
    }

    /** Remembers the block on the machine; a program that is not a classic Quince one's is left alone. */
    private static void handlePartition(final PartitionPayload payload, final ServerPlayer player,
                                        final ServerLevel level) {
        if (level.getBlockEntity(payload.host()) instanceof IOsHost machine && machine.console() != null
                && MachineMemory.classic(machine.installedOs()) && OsRegistry.getProgram(
                        ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, payload.program())) != null) {
            machine.console().settings().setPartition(payload.program(),
                    new QuincePartitions.Partition(payload.minimumMb(), payload.preferredMb()));
            machine.setChanged();
        }
    }
}
