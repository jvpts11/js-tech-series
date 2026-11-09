/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: what a player did on the system of a portable held in their hand: installed an app from its
 * store, looked at its alerts, or let them all go.
 *
 * @param device the device's id
 * @param action what was done
 * @param app    the app installed, by its serialized name; empty for the other actions
 */
public record PortableActionPayload(UUID device, Action action, String app) implements CustomPacketPayload {

    /* The longest an app's name runs on the wire. */
    private static final int MOST_APP_NAME = 32;
    private static final int MOST_ACTION_NAME = 16;

    public static final CustomPacketPayload.Type<PortableActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "portable_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PortableActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, PortableActionPayload::device,
                    ByteBufCodecs.stringUtf8(MOST_ACTION_NAME).map(Action::byName, Action::serializedName),
                    PortableActionPayload::action,
                    ByteBufCodecs.stringUtf8(MOST_APP_NAME), PortableActionPayload::app,
                    PortableActionPayload::new);

    @Override
    public CustomPacketPayload.Type<PortableActionPayload> type() {
        return TYPE;
    }

    /** What a player can do on a portable's system that the server keeps. */
    public enum Action implements IStableName {
        /** Installs the app named from the store, served by the network's Mirror. */
        INSTALL("install"),
        /** Marks every alert seen, as opening where they are listed does. */
        SEE_ALERTS("see_alerts"),
        /** Lets every alert go. */
        CLEAR_ALERTS("clear_alerts");

        private final String serializedName;

        private static final StableNames<Action> NAMES = StableNames.of(Action.class);

        Action(final String serializedName) {
            this.serializedName = serializedName;
        }

        /** The action of that name; an unknown one only marks the alerts seen, which changes nothing harmful. */
        public static Action byName(final String name) {
            return NAMES.byName(name, SEE_ALERTS);
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }
}
