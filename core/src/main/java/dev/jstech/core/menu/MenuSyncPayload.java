/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.menu;

import dev.jstech.core.JsCore;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The values of an open menu that changed since the last tick, each written by its own codec: the one payload every
 * {@link SyncedValue} travels in, so a mod writes none.
 *
 * @param containerId the open menu they belong to
 * @param entries     each value that changed: its place among the menu's values, and what its codec wrote
 */
record MenuSyncPayload(int containerId, List<Entry> entries) implements CustomPacketPayload {

    /** The most values one payload carries, more than any menu declares. */
    static final int MOST_VALUES = 256;
    /** The most one value may write, so a value gone wrong cannot fill the connection. */
    static final int MOST_BYTES = 1 << 18;

    static final Type<MenuSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "menu_sync"));

    static final StreamCodec<RegistryFriendlyByteBuf, MenuSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MenuSyncPayload::containerId,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_VALUES)), MenuSyncPayload::entries,
            MenuSyncPayload::new);

    MenuSyncPayload {
        entries = List.copyOf(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * One value.
     *
     * @param index its place among the menu's synced values, in the order they were declared
     * @param data  what its codec wrote
     */
    record Entry(int index, byte[] data) {

        static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::index, ByteBufCodecs.byteArray(MOST_BYTES), Entry::data, Entry::new);

        Entry {
            data = data.clone();
        }

        /** What the codec wrote, a copy each time, so nobody changes what was received. */
        @Override
        public byte[] data() {
            return data.clone();
        }
    }
}
