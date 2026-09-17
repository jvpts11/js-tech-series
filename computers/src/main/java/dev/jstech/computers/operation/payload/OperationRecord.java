/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.operation.OperationTypeId;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.operation.OperationFailure;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.util.Utf8Text;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * One entry in a network's Operations log, with provenance: what was moved, how much was requested vs actually
 * moved, the final status, and the per-source moves (from which Server, how much, to where) so the terminal can
 * show exactly where the data came from and went.
 *
 * <p>{@code id} is the Operation's identity while it is in flight, so a view can address it (change its
 * priority, cancel it); an instant Operation that was never queued carries {@link #NO_ID}. {@code priority}
 * is the level it was scheduled at. {@code waitedTicks} counts the ticks it sat queued or waiting before it
 * could run and {@code ranTicks} the ticks it was actually running: together they are how long it took.
 * {@code cause} is why it failed, where it did, so the row says more than the word {@code failed}.
 */
public record OperationRecord(UUID id, byte type, StorageKey key, long requested, long moved, byte status,
                              OperationPriority priority, List<MoveRow> moves, List<SubRow> subs,
                              long waitedTicks, long ranTicks, OperationFailure cause) {

    /*
     * The lists and the reason are copied on the way in, because a record that hands out the very list it was
     * built from is only as immutable as whoever built it. These are read by the client while the server goes
     * on working on what it passed.
     */
    public OperationRecord {
        moves = List.copyOf(moves);
        subs = List.copyOf(subs);
        cause = cause == null ? OperationFailure.NONE : cause;
    }

    /** The id of a record that never had a live Operation behind it (an instant maintenance record). */
    public static final UUID NO_ID = new UUID(0L, 0L);

    /*
     * The numbers the kinds themselves carry, written out here because a switch needs a label the compiler
     * can read. They used to be a numbering of their own, with nothing tying them to the ids the Operations
     * are registered under or to the words a terminal shows; a test fails if one of these and the kind it
     * names stop agreeing.
     */
    public static final byte TYPE_SELECT = 1;
    public static final byte TYPE_INSERT = 2;
    public static final byte TYPE_DELETE = 3;
    public static final byte TYPE_MOVE = 4;
    public static final byte TYPE_CRAFT = 5;
    public static final byte TYPE_ANALYZE = 6;
    public static final byte TYPE_REINDEX = 7;
    public static final byte TYPE_VACUUM = 8;
    public static final byte TYPE_DROP = 9;

    /*
     * The same eight states a state carries a number for, written here as the numbers themselves because a
     * switch needs a label it can read at compile time. They used to be a second numbering in an order of
     * their own, with nothing to stop the two drifting; they are the state's own numbers now, and a test
     * fails if one of these and the state it names ever stop agreeing.
     */
    public static final byte STATUS_PENDING = 1;
    public static final byte STATUS_PROCESSING = 2;
    public static final byte STATUS_WAITING = 3;
    public static final byte STATUS_COMPLETED = 4;
    public static final byte STATUS_PARTIAL = 5;
    public static final byte STATUS_FAILED = 6;
    public static final byte STATUS_RESOURCE_LOCKED = 7;
    public static final byte STATUS_DISCARDED = 8;

    public static final int MAX_MOVES = 32;
    public static final int MAX_SUBS = 32;

    /**
     * The verb a record's type reads by, as the prompt, the terminal and a program's queries all show it, or
     * {@code "OP"} for a type this version does not know.
     */
    public static String typeName(final byte type) {
        return OperationTypeId.verbOf(type);
    }

    /**
     * The word a record's status reads by, as the prompt, the terminal and a program's queries all show it, or
     * {@code "?"} for a status this version does not know.
     */
    public static String statusName(final byte status) {
        return switch (status) {
            case STATUS_COMPLETED -> "done";
            case STATUS_PARTIAL -> "partial";
            case STATUS_FAILED -> "failed";
            case STATUS_PROCESSING -> "running";
            case STATUS_WAITING -> "waiting";
            case STATUS_RESOURCE_LOCKED -> "locked";
            case STATUS_PENDING -> "pending";
            case STATUS_DISCARDED -> "discarded";
            default -> "?";
        };
    }

    /** An instant Operation's record: no live identity, the default priority, no SubOperation rows, no time. */
    public OperationRecord(final byte type, final StorageKey key, final long requested, final long moved,
                           final byte status, final List<MoveRow> moves) {
        this(NO_ID, type, key, requested, moved, status, OperationPriority.DEFAULT, moves, List.of(), 0, 0,
                OperationFailure.NONE);
    }

    /** A live Operation's record before the scheduler stamps its timing. */
    public OperationRecord(final UUID id, final byte type, final StorageKey key, final long requested,
                           final long moved, final byte status, final OperationPriority priority,
                           final List<MoveRow> moves, final List<SubRow> subs) {
        this(id, type, key, requested, moved, status, priority, moves, subs, 0, 0, OperationFailure.NONE);
    }

    public OperationRecord withStatus(final byte newStatus) {
        return new OperationRecord(id, type, key, requested, moved, newStatus, priority, moves, subs,
                waitedTicks, ranTicks, cause);
    }

    public OperationRecord withPriority(final OperationPriority newPriority) {
        return new OperationRecord(id, type, key, requested, moved, status, newPriority, moves, subs,
                waitedTicks, ranTicks, cause);
    }

    public OperationRecord withTiming(final long waited, final long ran) {
        return new OperationRecord(id, type, key, requested, moved, status, priority, moves, subs, waited, ran,
                cause);
    }

    /** The same record with the reason it failed, which only a failed one has any use for. */
    public OperationRecord withCause(final OperationFailure newCause) {
        return new OperationRecord(id, type, key, requested, moved, status, priority, moves, subs,
                waitedTicks, ranTicks, newCause);
    }

    /**
     * Why it failed, as a line of text, or nothing where the Operation did not fail or did not say.
     *
     * <p>The line is assembled here rather than by whoever draws it, so the terminal, the prompt and a window all
     * read the same words.
     */
    public Optional<Component> failureText() {
        if (!cause.isPresent()) {
            return Optional.empty();
        }
        final Object[] arguments = cause.arguments().toArray();
        return Optional.of(Component.translatable(cause.key(), arguments));
    }

    /** Whether the Operation delivered everything it was asked for. */
    public boolean completed() {
        return status == STATUS_COMPLETED;
    }

    /** Whether a live Operation stands behind this record (it can be addressed by {@link #id()}). */
    public boolean hasId() {
        return !NO_ID.equals(id);
    }

    public ItemStack icon() {
        return key.stack(1);
    }

    public Component name() {
        return key.displayName();
    }

    public boolean isFluid() {
        return key.isFluid();
    }

    /**
     * One provenance row.
     */
    public record MoveRow(String from, long qty, String to) {

        public static final StreamCodec<RegistryFriendlyByteBuf, MoveRow> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, MoveRow::from,
                        ByteBufCodecs.VAR_LONG, MoveRow::qty,
                        ByteBufCodecs.STRING_UTF8, MoveRow::to,
                        MoveRow::new);
    }

    /**
     * One live SubOperation row: a server's share of the Operation and how far along it is.
     */
    public record SubRow(String server, long planned, long moved, byte state) {

        public static final byte SUB_PENDING = 0;
        public static final byte SUB_READING = 1;
        public static final byte SUB_STREAMING = 2;
        public static final byte SUB_COMPLETED = 3;

        public static final StreamCodec<RegistryFriendlyByteBuf, SubRow> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, SubRow::server,
                        ByteBufCodecs.VAR_LONG, SubRow::planned,
                        ByteBufCodecs.VAR_LONG, SubRow::moved,
                        ByteBufCodecs.BYTE, SubRow::state,
                        SubRow::new);
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, List<MoveRow>> MOVES_CODEC =
            MoveRow.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_MOVES));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<SubRow>> SUBS_CODEC =
            SubRow.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SUBS));

    /*
     * Written by hand rather than through stringUtf8, whose limit is counted in characters while what it
     * enforces is bytes: an accent in a machine's name would make the encode throw and the whole packet would
     * be lost with it. The reason is already cut to fit where it is built, and this cuts again in case it was
     * built somewhere that forgot to.
     */
    private static final StreamCodec<RegistryFriendlyByteBuf, OperationFailure> CAUSE_CODEC =
            StreamCodec.of(
                    (buf, cause) -> {
                        buf.writeUtf(Utf8Text.clamp(cause.key(), OperationFailure.MAX_KEY_BYTES));
                        final List<String> arguments = cause.arguments();
                        final int count = Math.min(arguments.size(), OperationFailure.MAX_ARGUMENTS);
                        buf.writeByte(count);
                        for (int i = 0; i < count; i++) {
                            buf.writeUtf(Utf8Text.clamp(arguments.get(i), OperationFailure.MAX_ARGUMENT_BYTES));
                        }
                    },
                    buf -> {
                        final String key = buf.readUtf(OperationFailure.MAX_KEY_BYTES);
                        final int count = Math.min(buf.readByte(), OperationFailure.MAX_ARGUMENTS);
                        final List<String> arguments = new ArrayList<>(Math.max(0, count));
                        for (int i = 0; i < count; i++) {
                            arguments.add(buf.readUtf(OperationFailure.MAX_ARGUMENT_BYTES));
                        }
                        return new OperationFailure(key, arguments);
                    });

    // Built by hand because the record has more components than StreamCodec.composite carries.
    public static final StreamCodec<RegistryFriendlyByteBuf, OperationRecord> STREAM_CODEC =
            StreamCodec.of(
                    (buf, rec) -> {
                        UUIDUtil.STREAM_CODEC.encode(buf, rec.id());
                        buf.writeByte(rec.type());
                        StorageKey.STREAM_CODEC.encode(buf, rec.key());
                        buf.writeVarLong(rec.requested());
                        buf.writeVarLong(rec.moved());
                        buf.writeByte(rec.status());
                        buf.writeByte(rec.priority().id());
                        MOVES_CODEC.encode(buf, rec.moves());
                        SUBS_CODEC.encode(buf, rec.subs());
                        buf.writeVarLong(rec.waitedTicks());
                        buf.writeVarLong(rec.ranTicks());
                        CAUSE_CODEC.encode(buf, rec.cause());
                    },
                    buf -> new OperationRecord(
                            UUIDUtil.STREAM_CODEC.decode(buf),
                            buf.readByte(),
                            StorageKey.STREAM_CODEC.decode(buf),
                            buf.readVarLong(),
                            buf.readVarLong(),
                            buf.readByte(),
                            OperationPriority.byId(buf.readByte()),
                            MOVES_CODEC.decode(buf),
                            SUBS_CODEC.decode(buf),
                            buf.readVarLong(),
                            buf.readVarLong(),
                            CAUSE_CODEC.decode(buf)));

    public CompoundTag toNbt(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        if (hasId()) {
            tag.putUUID("id", id);
        }
        tag.putByte("type", type);
        StorageKey.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), key)
                .result().ifPresent(encoded -> tag.put("icon", encoded));
        tag.putLong("requested", requested);
        tag.putLong("moved", moved);
        tag.putByte("status", status);
        tag.putByte("priority", (byte) priority.id());
        tag.putLong("waited", waitedTicks);
        tag.putLong("ran", ranTicks);
        final ListTag moveList = new ListTag();
        for (final MoveRow row : moves) {
            final CompoundTag m = new CompoundTag();
            m.putString("from", row.from());
            m.putLong("qty", row.qty());
            m.putString("to", row.to());
            moveList.add(m);
        }
        tag.put("moves", moveList);
        final ListTag subList = new ListTag();
        for (final SubRow row : subs) {
            final CompoundTag s = new CompoundTag();
            s.putString("server", row.server());
            s.putLong("planned", row.planned());
            s.putLong("moved", row.moved());
            s.putByte("state", row.state());
            subList.add(s);
        }
        tag.put("subs", subList);
        // Only where there is one: the overwhelming majority of records did not fail and would carry empties.
        if (cause.isPresent()) {
            final CompoundTag why = new CompoundTag();
            why.putString("key", cause.key());
            final ListTag argumentList = new ListTag();
            for (final String argument : cause.arguments()) {
                argumentList.add(StringTag.valueOf(argument));
            }
            why.put("args", argumentList);
            tag.put("cause", why);
        }
        return tag;
    }

    public static OperationRecord fromNbt(final CompoundTag tag, final HolderLookup.Provider registries) {
        final StorageKey key = StorageKey.CODEC
                .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("icon"))
                .result().orElseGet(() -> StorageKey.of(Items.BARRIER));
        final List<MoveRow> moves = new ArrayList<>();
        final ListTag moveList = tag.getList("moves", Tag.TAG_COMPOUND);
        for (int i = 0; i < moveList.size(); i++) {
            final CompoundTag m = moveList.getCompound(i);
            moves.add(new MoveRow(m.getString("from"), m.getLong("qty"), m.getString("to")));
        }
        final List<SubRow> subs = new ArrayList<>();
        final ListTag subList = tag.getList("subs", Tag.TAG_COMPOUND);
        for (int i = 0; i < subList.size(); i++) {
            final CompoundTag s = subList.getCompound(i);
            subs.add(new SubRow(s.getString("server"), s.getLong("planned"), s.getLong("moved"), s.getByte("state")));
        }
        final UUID id = tag.hasUUID("id") ? tag.getUUID("id") : NO_ID;
        final OperationPriority priority = tag.contains("priority")
                ? OperationPriority.byId(tag.getByte("priority")) : OperationPriority.DEFAULT;
        OperationFailure cause = OperationFailure.NONE;
        if (tag.contains("cause", Tag.TAG_COMPOUND)) {
            final CompoundTag why = tag.getCompound("cause");
            final ListTag argumentList = why.getList("args", Tag.TAG_STRING);
            final List<String> arguments = new ArrayList<>(argumentList.size());
            for (int i = 0; i < argumentList.size(); i++) {
                arguments.add(argumentList.getString(i));
            }
            cause = new OperationFailure(why.getString("key"), arguments);
        }
        return new OperationRecord(id, tag.getByte("type"), key, tag.getLong("requested"),
                tag.getLong("moved"), tag.getByte("status"), priority, List.copyOf(moves), List.copyOf(subs),
                tag.getLong("waited"), tag.getLong("ran"), cause);
    }
}
