/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: a trash window is putting things back, destroying them for good, or emptying the whole trash of
 * the computer at {@code hostPos}. The things are named by what they are kept under in the trash; emptying names none.
 */
public record TrashActionPayload(BlockPos hostPos, Action action, List<String> stored) implements CustomPacketPayload {

    /** The most things one action names, which is as many as a trash window lists. */
    public static final int MAX_STORED = TrashListingPayload.MAX_ENTRIES;

    public static final CustomPacketPayload.Type<TrashActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "trash_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrashActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, TrashActionPayload::hostPos,
                    ByteBufCodecs.stringUtf8(Action.MOST_LETTERS).map(Action::byId, Action::id),
                    TrashActionPayload::action,
                    ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(MAX_STORED)), TrashActionPayload::stored,
                    TrashActionPayload::new);

    /** What is done to the things named, by an id of its own on the wire rather than by its place in the list. */
    public enum Action {
        /** Each goes back where it came from. */
        RESTORE("restore"),
        /** Each is deleted for good. */
        SHRED("shred"),
        /** Everything in the trash is deleted for good. */
        EMPTY("empty");

        /** The longest id an action has. */
        static final int MOST_LETTERS = 16;

        private final String id;

        Action(final String id) {
            this.id = id;
        }

        public String id() {
            return this.id;
        }

        /** The action with that id; one no action has means the harmless one, putting back. */
        static Action byId(final String id) {
            for (final Action action : Action.values()) {
                if (action.id.equals(id)) {
                    return action;
                }
            }
            return RESTORE;
        }
    }

    public TrashActionPayload {
        stored = List.copyOf(stored);
    }

    @Override
    public CustomPacketPayload.Type<TrashActionPayload> type() {
        return TYPE;
    }
}
