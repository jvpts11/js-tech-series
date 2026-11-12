/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.jstechseries.operation.OperationStatus;
import dev.jstech.core.notification.NotificationKind;
import dev.jstech.core.notification.NotificationLevel;
import dev.jstech.core.notification.Notifications;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The network's alerts to its players' portables (computers/docs/design/wireless.md): an item below the level a
 * device watches it at, an Operation that failed, a degraded power supply, a UPS on its battery, a network conflict.
 * Each reaches the devices on that network now, those carried where one of its access points reaches, that take its
 * kind; the item levels are looked at on a timer, once per network for every device on it. And the lost devices are
 * let go here: those out of every inventory and every dock for longer than the server allows.
 */
@TextHolder
public final class PortableAlerts {

    static final TextKey ITEM_LOW = TextKey.of("jsc.portable.alert.item_low", "%s is low");
    /* How many are left, the network, and the level it went below. */
    static final TextKey ITEM_LOW_DETAIL = TextKey.of("jsc.portable.alert.item_low_detail",
            "%s left in %s, below %s");
    static final TextKey OPERATION_FAILED = TextKey.of("jsc.portable.alert.operation_failed",
            "An Operation failed");
    /* What it was about, and the network. */
    static final TextKey OPERATION_DETAIL = TextKey.of("jsc.portable.alert.operation_detail", "%s, on %s");
    static final TextKey POWER = TextKey.of("jsc.portable.alert.power", "Degraded supply");
    static final TextKey UPS = TextKey.of("jsc.portable.alert.ups", "UPS on battery");
    static final TextKey CONFLICT = TextKey.of("jsc.portable.alert.conflict", "Network conflict");
    /* The machine, then what the network's log says of it. */
    static final TextKey FROM_MACHINE = TextKey.of("jsc.portable.alert.from_machine", "%s: %s");
    static final TextKey NOTICE_NAME = TextKey.of("jsc.notification.kind.portable_alert", "Portable device alerts");

    /** The kind the network's alerts take on the HUD, at the attention level, for a player who wants them there. */
    public static final NotificationKind NOTICE = Notifications.kind(ResourceLocation.fromNamespaceAndPath(
            JsComputers.MODID, "portable_alert"), NotificationLevel.WARNING, NOTICE_NAME).declare();

    /** The item levels are looked at this often, in ticks. */
    public static final int WATCH_EVERY = 100;
    /** The lost devices are looked for this often, in ticks. */
    public static final int SWEEP_EVERY = 1_200;
    private static final long TICKS_AN_HOUR = 72_000L;

    private PortableAlerts() {
    }

    /** Declares the kind of notification the alerts take, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares it.
    }

    /**
     * What the network's log wrote that its players' portables are told of too: the alert of kind {@code kind} for
     * the machine {@code machineName}, empty for the whole network, saying {@code message}.
     */
    public static void fromLog(final ServerLevel level, @Nullable final NetworkUuid network, final AlertKind kind,
                               final String machineName, final Text message) {
        final TextKey title = switch (kind) {
            case POWER -> POWER;
            case UPS -> UPS;
            case CONFLICT -> CONFLICT;
            case ITEMS -> ITEM_LOW;
            case OPERATIONS -> OPERATION_FAILED;
        };
        raise(level, network, kind, title.text(),
                machineName.isEmpty() ? message : FROM_MACHINE.with(machineName, message));
    }

    /** An Operation of the network {@code network} settled as {@code record} says: told when it failed. */
    public static void settled(final ServerLevel level, @Nullable final NetworkUuid network,
                               final OperationRecord record) {
        if (network != null && record.status() == OperationStatus.FAILED.id()) {
            raise(level, network, AlertKind.OPERATIONS, OPERATION_FAILED.text(),
                    OPERATION_DETAIL.with(GameText.of(record.key().displayName()), ShortId.of(network.asString())));
        }
    }

    /**
     * Tells every device on {@code network} that takes {@code kind}, in every level, an alert saying {@code title}
     * over {@code detail}.
     */
    public static void raise(final ServerLevel level, @Nullable final NetworkUuid network, final AlertKind kind,
                             final Text title, final Text detail) {
        if (network == null) {
            return;
        }
        final PortableDevices devices = PortableDevices.get(level.getServer());
        for (final ServerLevel any : level.getServer().getAllLevels()) {
            for (final PortableLinks.Joined joined : PortableLinks.on(any, network)) {
                final PortableState state = devices.find(joined.device());
                if (state != null && state.alertsOn().contains(kind)) {
                    devices.deliver(joined.device(), new PortableAlert(kind, title, detail, any.getGameTime(), false));
                }
            }
        }
    }

    /** Looks at the watched items and for lost devices, each as often as it is due. */
    public static void tick(final MinecraftServer server) {
        final long now = server.overworld().getGameTime();
        if (now % WATCH_EVERY == 0) {
            watch(server);
        }
        if (now % SWEEP_EVERY == 0) {
            PortableDevices.get(server).eraseUnseenSince(now
                    - ComputersServerConfig.lostDeviceHours() * TICKS_AN_HOUR,
                    player -> server.getPlayerList().getPlayer(player) != null);
        }
    }

    /*
     * Each network's stock is read once for every device on it that watches something; a device is told of each
     * item that went below its level since it last looked.
     */
    private static void watch(final MinecraftServer server) {
        final PortableDevices devices = PortableDevices.get(server);
        for (final ServerLevel level : server.getAllLevels()) {
            final Map<NetworkUuid, List<UUID>> byNetwork = new HashMap<>();
            for (final PortableLinks.Joined joined : PortableLinks.all(level)) {
                final PortableState state = devices.find(joined.device());
                if (state != null && !state.watched().isEmpty() && state.alertsOn().contains(AlertKind.ITEMS)) {
                    byNetwork.computeIfAbsent(joined.network(), any -> new ArrayList<>()).add(joined.device());
                }
            }
            for (final Map.Entry<NetworkUuid, List<UUID>> entry : byNetwork.entrySet()) {
                final Map<StorageKey, Long> stock = NetworkStorage.of(level, entry.getKey()).query();
                final String network = ShortId.of(entry.getKey().asString());
                for (final UUID device : entry.getValue()) {
                    watchOne(level, devices, device, stock, network);
                }
            }
        }
    }

    private static void watchOne(final ServerLevel level, final PortableDevices devices, final UUID device,
                                 final Map<StorageKey, Long> stock, final String network) {
        final PortableState state = devices.find(device);
        if (state == null) {
            return;
        }
        for (final Map.Entry<StorageKey, Long> watch : Map.copyOf(state.watched()).entrySet()) {
            final long count = stock.getOrDefault(watch.getKey(), 0L);
            if (devices.wentBelow(device, watch.getKey(), count)) {
                final Text name = GameText.of(watch.getKey().displayName());
                devices.deliver(device, new PortableAlert(AlertKind.ITEMS, ITEM_LOW.with(name),
                        ITEM_LOW_DETAIL.with(count, network, watch.getValue()), level.getGameTime(), false));
            }
        }
    }
}
