/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.program.MessengerLog;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: what a player did on a portable held in their hand, on its system or in one of its apps: installed
 * an app, looked at or let go its alerts, chose which alerts it takes and which items it watches, asked the network
 * for items into it, took them out or sent them back, deposited, crafted, kept and sent its pictures, talked on the
 * Messenger, or asked what its maps and its prospecting show.
 *
 * @param device the device's id
 * @param action what was done
 * @param named  what it was done to, by name: an app, a kind of alert, a room of the Messenger, a computer; empty for
 *               none
 * @param text   what was said on the Messenger; empty otherwise
 * @param key    the item it was about, for the actions on items
 * @param amount how many, the level of a watch, the inventory's slot of a deposit, or the number of a picture
 */
public record PortableActionPayload(UUID device, Action action, String named, String text, Optional<StorageKey> key,
                                    long amount) implements CustomPacketPayload {

    /** The longest a name runs on the wire. */
    public static final int MOST_NAMED = 64;
    private static final int MOST_ACTION_NAME = 16;

    public static final CustomPacketPayload.Type<PortableActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "portable_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PortableActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, PortableActionPayload::device,
                    ByteBufCodecs.stringUtf8(MOST_ACTION_NAME).map(Action::byName, Action::serializedName),
                    PortableActionPayload::action,
                    ByteBufCodecs.stringUtf8(MOST_NAMED), PortableActionPayload::named,
                    ByteBufCodecs.stringUtf8(MessengerLog.MAX_TEXT), PortableActionPayload::text,
                    ByteBufCodecs.optional(StorageKey.STREAM_CODEC), PortableActionPayload::key,
                    ByteBufCodecs.VAR_LONG, PortableActionPayload::amount,
                    PortableActionPayload::new);

    /** An action about nothing in particular. */
    public static PortableActionPayload of(final UUID device, final Action action) {
        return new PortableActionPayload(device, action, "", "", Optional.empty(), 0L);
    }

    /** An action about something named. */
    public static PortableActionPayload named(final UUID device, final Action action, final String named) {
        return new PortableActionPayload(device, action, named, "", Optional.empty(), 0L);
    }

    /** An action on {@code amount} of an item. */
    public static PortableActionPayload item(final UUID device, final Action action, final StorageKey key,
                                             final long amount) {
        return new PortableActionPayload(device, action, "", "", Optional.of(key), amount);
    }

    /** An action on a number: an inventory's slot, a picture. */
    public static PortableActionPayload numbered(final UUID device, final Action action, final String named,
                                                 final long amount) {
        return new PortableActionPayload(device, action, named, "", Optional.empty(), amount);
    }

    @Override
    public CustomPacketPayload.Type<PortableActionPayload> type() {
        return TYPE;
    }

    /** What a player can do on a portable that the server keeps or answers. */
    public enum Action implements IStableName {
        /** Installs the app named from the store, served by the network's Mirror. */
        INSTALL("install"),
        /** Marks every alert seen, as opening where they are listed does. */
        SEE_ALERTS("see_alerts"),
        /** Says the player unlocked the device: slid, swiped or tapped its lock open. */
        UNLOCK("unlock"),
        /** Lets every alert go. */
        CLEAR_ALERTS("clear_alerts"),
        /** Takes the kind of alert named, or stops taking it. */
        TOGGLE_ALERT("toggle_alert"),
        /** Watches the item at the level in {@code amount}, or moves its level. */
        WATCH("watch"),
        /** Stops watching the item. */
        UNWATCH("unwatch"),
        /** Asks for the network app's view: the network's stock, the device's storage, the crafts and the jobs. */
        NETWORK("network"),
        /** Requests {@code amount} of the item from the network into the device's own storage. */
        REQUEST("request"),
        /** Takes {@code amount} of the item from the device's storage into the inventory. */
        TAKE("take"),
        /** Returns {@code amount} of the item from the device's storage to the network. */
        RETURN("return"),
        /** Deposits the inventory's slot {@code amount} into the network. */
        DEPOSIT("deposit"),
        /** Deposits the whole of the inventory, but for what is held, into the network. */
        DEPOSIT_ALL("deposit_all"),
        /** Crafts {@code amount} of the item on the network. */
        CRAFT("craft"),
        /** Asks how the network would craft {@code amount} of the item. */
        PLAN("plan"),
        /** Asks for the gallery: its pictures and the computers they can be sent to. */
        GALLERY("gallery"),
        /** Sends the picture numbered {@code amount} to the computer named, by its node. */
        SEND_PICTURE("send_picture"),
        /** Deletes the picture numbered {@code amount}. */
        DELETE_PICTURE("delete_picture"),
        /** Looks at the Messenger's room named, or the lobby. */
        MESSENGER("messenger"),
        /** Says {@code text} in the Messenger's room named. */
        SAY("say"),
        /** Leaves the Messenger. */
        LEAVE("leave"),
        /** Asks what the maps show round the device. */
        MAP("map"),
        /** Asks what the prospecting finds round the device. */
        PROSPECT("prospect"),
        /** Asks for the device's wallet: its balance and its history. */
        WALLET("wallet"),
        /** Makes the device's wallet, when it has none. */
        WALLET_CREATE("wallet_create"),
        /** Sends {@code amount} from the device's wallet to the address named. */
        WALLET_SEND("wallet_send");

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
