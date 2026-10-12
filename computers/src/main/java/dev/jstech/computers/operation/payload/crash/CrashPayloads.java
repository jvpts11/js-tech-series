/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crash;

import dev.jstech.computers.block.ICrashScreenOpener;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.os.IOsHost;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The payloads of a system that crashed: its page on the glass, a key on its console, and the window that did it. */
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
    }

    private static void handleKey(final CrashKeyPayload payload, final ServerPlayer player, final ServerLevel level) {
        if (level.getBlockEntity(payload.host()) instanceof IOsHost machine) {
            machine.crashKey();
        }
    }

    private static void handleMemoryShort(final MemoryShortPayload payload, final ServerPlayer player,
                                          final ServerLevel level) {
        if (level.getBlockEntity(payload.host()) instanceof AbstractComputerBlockEntity machine) {
            machine.memoryShort(payload.program());
        }
    }
}
