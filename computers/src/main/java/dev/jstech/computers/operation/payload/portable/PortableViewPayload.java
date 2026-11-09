/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.portable.AlertKind;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.PortableAlert;
import dev.jstech.computers.portable.PortableState;
import dev.jstech.computers.storage.StorageKey;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what a portable its player carries keeps on the server, for its system to show: the apps on it,
 * its alerts, the newest first, whether its network's Mirror is serving, which its store needs, and what its Alerts
 * app shows: the kinds of alert it takes and the items it watches.
 *
 * @param device    the device's id
 * @param installed the apps on it
 * @param alerts    its alerts, the newest first
 * @param mirror    whether its network's Mirror is serving it now
 * @param alertsOn  the kinds of alert it takes
 * @param watched   the items it watches, with the level of each
 */
public record PortableViewPayload(UUID device, Set<MobileApp> installed, List<PortableAlert> alerts, boolean mirror,
                                  Set<AlertKind> alertsOn, List<Watch> watched) implements CustomPacketPayload {

    /* The longest an app's or a kind's name runs on the wire. */
    private static final int MOST_NAME = 32;

    public static final CustomPacketPayload.Type<PortableViewPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "portable_view"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PortableViewPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, p.device());
                buf.writeVarInt(p.installed().size());
                for (final MobileApp app : p.installed()) {
                    buf.writeUtf(app.serializedName(), MOST_NAME);
                }
                final List<PortableAlert> sent = p.alerts().subList(0,
                        Math.min(PortableState.MOST_ALERTS, p.alerts().size()));
                buf.writeVarInt(sent.size());
                for (final PortableAlert alert : sent) {
                    PortableAlert.STREAM_CODEC.encode(buf, alert);
                }
                buf.writeBoolean(p.mirror());
                buf.writeVarInt(p.alertsOn().size());
                for (final AlertKind kind : p.alertsOn()) {
                    buf.writeUtf(kind.serializedName(), MOST_NAME);
                }
                final List<Watch> watches = p.watched().subList(0,
                        Math.min(PortableState.MOST_WATCHED, p.watched().size()));
                buf.writeVarInt(watches.size());
                for (final Watch watch : watches) {
                    StorageKey.STREAM_CODEC.encode(buf, watch.key());
                    buf.writeVarLong(watch.level());
                }
            },
            buf -> {
                final UUID device = UUIDUtil.STREAM_CODEC.decode(buf);
                final int apps = Math.min(MobileApp.values().length, Math.max(0, buf.readVarInt()));
                final Set<MobileApp> installed = EnumSet.noneOf(MobileApp.class);
                for (int i = 0; i < apps; i++) {
                    final MobileApp app = MobileApp.byName(buf.readUtf(MOST_NAME));
                    if (app != null) {
                        installed.add(app);
                    }
                }
                final int count = Math.min(PortableState.MOST_ALERTS, Math.max(0, buf.readVarInt()));
                final List<PortableAlert> alerts = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    alerts.add(PortableAlert.STREAM_CODEC.decode(buf));
                }
                final boolean mirror = buf.readBoolean();
                final int kinds = Math.min(AlertKind.values().length, Math.max(0, buf.readVarInt()));
                final Set<AlertKind> on = EnumSet.noneOf(AlertKind.class);
                for (int i = 0; i < kinds; i++) {
                    on.add(AlertKind.byName(buf.readUtf(MOST_NAME)));
                }
                final int watches = Math.min(PortableState.MOST_WATCHED, Math.max(0, buf.readVarInt()));
                final List<Watch> watched = new ArrayList<>(watches);
                for (int i = 0; i < watches; i++) {
                    watched.add(new Watch(StorageKey.STREAM_CODEC.decode(buf), buf.readVarLong()));
                }
                return new PortableViewPayload(device, installed, alerts, mirror, on, watched);
            });

    public PortableViewPayload {
        installed = installed.isEmpty() ? EnumSet.noneOf(MobileApp.class) : EnumSet.copyOf(installed);
        alerts = List.copyOf(alerts);
        alertsOn = alertsOn.isEmpty() ? EnumSet.noneOf(AlertKind.class) : EnumSet.copyOf(alertsOn);
        watched = List.copyOf(watched);
    }

    @Override
    public CustomPacketPayload.Type<PortableViewPayload> type() {
        return TYPE;
    }

    /**
     * An item a device watches.
     *
     * @param key   the item
     * @param level the count below which the device is told
     */
    public record Watch(StorageKey key, long level) {
    }
}
