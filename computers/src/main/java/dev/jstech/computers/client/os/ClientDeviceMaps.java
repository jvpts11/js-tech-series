/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.DeviceMapPayload;
import dev.jstech.computers.operation.payload.RequestDeviceMapPayload;
import dev.jstech.computers.operation.payload.SetDeviceDisabledPayload;
import dev.jstech.computers.os.devices.DeviceMap;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Where the windows that show a machine's hardware and ports hear from it: each window that is up listens for its
 * machine's map, asks for it, and asks for a device to be disabled or enabled; the machine's answer reaches every
 * window showing that machine.
 */
public final class ClientDeviceMaps {

    /** The windows listening, each for one machine. */
    private static final List<Listener> LISTENING = new ArrayList<>();

    private ClientDeviceMaps() {
    }

    /** Starts {@code onMap} hearing every map {@code host} answers with, and asks for one now. */
    public static void listen(final BlockPos host, final Consumer<DeviceMap> onMap) {
        LISTENING.add(new Listener(host, onMap));
        ask(host);
    }

    /** Stops {@code onMap} hearing anything more. */
    public static void forget(final Consumer<DeviceMap> onMap) {
        LISTENING.removeIf(listener -> listener.onMap() == onMap);
    }

    /** Forgets every listener, for a world being left: a window that never closed cannot hear the next one. */
    public static void forgetAll() {
        LISTENING.clear();
    }

    /** Asks {@code host} for its map as it stands. */
    public static void ask(final BlockPos host) {
        PacketDistributor.sendToServer(new RequestDeviceMapPayload(host));
    }

    /** Asks {@code host} to disable the device at {@code device}, or to enable it again. */
    public static void setDisabled(final BlockPos host, final long device, final boolean disabled) {
        PacketDistributor.sendToServer(new SetDeviceDisabledPayload(host, device, disabled));
    }

    /** Hands a machine's answer to every window showing that machine. */
    public static void accept(final DeviceMapPayload payload) {
        for (final Listener listener : List.copyOf(LISTENING)) {
            if (listener.host().equals(payload.hostPos())) {
                listener.onMap().accept(payload.map());
            }
        }
    }

    /** The device at {@code pos} in {@code map}, or null; a shorthand for the windows. */
    @Nullable
    public static DeviceMap.Device device(@Nullable final DeviceMap map, final long pos) {
        return map == null ? null : map.device(pos);
    }

    private record Listener(BlockPos host, Consumer<DeviceMap> onMap) {
    }
}
