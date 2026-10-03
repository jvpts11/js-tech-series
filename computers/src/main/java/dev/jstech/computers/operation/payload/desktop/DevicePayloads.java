/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.desktop;

import dev.jstech.computers.client.os.ClientDeviceMaps;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.DeviceMapPayload;
import dev.jstech.computers.operation.payload.RequestDeviceMapPayload;
import dev.jstech.computers.operation.payload.SetDeviceDisabledPayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.devices.DeviceMaps;
import dev.jstech.core.util.Loaded;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The payloads of every system's Device Manager: a window asks for the machine's hardware and ports, and disables
 * or enables a device on one of them; the machine answers each with its map as it then stands.
 */
public final class DevicePayloads {

    private DevicePayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestDeviceMapPayload.TYPE, RequestDeviceMapPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestDeviceMapPayload::hostPos), DevicePayloads::handleRequest);
        ComputerAccess.accept(registrar, SetDeviceDisabledPayload.TYPE, SetDeviceDisabledPayload.STREAM_CODEC,
                ComputerAccess.machine(SetDeviceDisabledPayload::hostPos), DevicePayloads::handleSetDisabled);
        registrar.playToClient(DeviceMapPayload.TYPE, DeviceMapPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> ClientDeviceMaps.accept(payload)));
    }

    private static void handleRequest(final RequestDeviceMapPayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        answer(player, level, payload.hostPos());
    }

    /* Only a device on one of the machine's own ports can be disabled from its Device Manager. */
    private static void handleSetDisabled(final SetDeviceDisabledPayload payload, final ServerPlayer player,
                                          final ServerLevel level) {
        if (Loaded.blockEntity(level, payload.hostPos()) instanceof IOsHost computer
                && computer.linkedEndpoints().contains(payload.device())) {
            computer.setDisabled(payload.device(), payload.disabled());
        }
        answer(player, level, payload.hostPos());
    }

    private static void answer(final ServerPlayer player, final ServerLevel level, final BlockPos host) {
        if (Loaded.blockEntity(level, host) instanceof IOsHost computer) {
            PacketDistributor.sendToPlayer(player, new DeviceMapPayload(host, DeviceMaps.of(level, computer)));
        }
    }
}
