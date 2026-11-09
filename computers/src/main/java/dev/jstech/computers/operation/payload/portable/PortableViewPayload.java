/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.PortableAlert;
import dev.jstech.computers.portable.PortableState;
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
 * its alerts, the newest first, and whether its network's Mirror is serving, which its store needs.
 *
 * @param device    the device's id
 * @param installed the apps on it
 * @param alerts    its alerts, the newest first
 * @param mirror    whether its network's Mirror is serving it now
 */
public record PortableViewPayload(UUID device, Set<MobileApp> installed, List<PortableAlert> alerts, boolean mirror)
        implements CustomPacketPayload {

    /* The longest an app's name runs on the wire. */
    private static final int MOST_APP_NAME = 32;

    public static final CustomPacketPayload.Type<PortableViewPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "portable_view"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PortableViewPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, p.device());
                buf.writeVarInt(p.installed().size());
                for (final MobileApp app : p.installed()) {
                    buf.writeUtf(app.serializedName(), MOST_APP_NAME);
                }
                final List<PortableAlert> sent = p.alerts().subList(0,
                        Math.min(PortableState.MOST_ALERTS, p.alerts().size()));
                buf.writeVarInt(sent.size());
                for (final PortableAlert alert : sent) {
                    PortableAlert.STREAM_CODEC.encode(buf, alert);
                }
                buf.writeBoolean(p.mirror());
            },
            buf -> {
                final UUID device = UUIDUtil.STREAM_CODEC.decode(buf);
                final int apps = Math.min(MobileApp.values().length, Math.max(0, buf.readVarInt()));
                final Set<MobileApp> installed = EnumSet.noneOf(MobileApp.class);
                for (int i = 0; i < apps; i++) {
                    final MobileApp app = MobileApp.byName(buf.readUtf(MOST_APP_NAME));
                    if (app != null) {
                        installed.add(app);
                    }
                }
                final int count = Math.min(PortableState.MOST_ALERTS, Math.max(0, buf.readVarInt()));
                final List<PortableAlert> alerts = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    alerts.add(PortableAlert.STREAM_CODEC.decode(buf));
                }
                return new PortableViewPayload(device, installed, alerts, buf.readBoolean());
            });

    public PortableViewPayload {
        installed = installed.isEmpty() ? EnumSet.noneOf(MobileApp.class) : EnumSet.copyOf(installed);
        alerts = List.copyOf(alerts);
    }

    @Override
    public CustomPacketPayload.Type<PortableViewPayload> type() {
        return TYPE;
    }
}
