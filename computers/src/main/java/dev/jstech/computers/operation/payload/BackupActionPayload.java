/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.backup.BackupJob;
import dev.jstech.computers.backup.TapeContents;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: everything a Backup Console asks of the network's Backup Service. One payload for all of it, as the
 * Messenger's is: what differs between looking, running a job, saving one, restoring a backup and handling a tape is a
 * word and the few values it takes, and the service answers each with the console's whole state.
 *
 * @param hostPos    the computer the console runs on
 * @param monitorPos the monitor it shows on
 * @param action     what is asked, one of the constants
 * @param job        the job looked at, run, saved or deleted
 * @param was        the name a saved job had before, empty for a new one
 * @param parts      what a saved job keeps, or what a restore puts back, a bit each
 * @param target     where a saved job writes, or the library whose tapes are looked at or handled
 * @param when       when a saved job runs
 * @param backup     the backup looked at or restored, the nil id for none
 * @param images     the computers whose system images a restore puts back
 * @param tape       the tape handled, by its slot from 0
 * @param text       the label written on a tape
 */
public record BackupActionPayload(BlockPos hostPos, BlockPos monitorPos, int action, String job, String was,
                                  int parts, BackupJob.Target target, BackupJob.Schedule when, UUID backup,
                                  List<UUID> images, int tape, String text) implements CustomPacketPayload {

    /** Ask for the state, the library shown and the backup chosen named. */
    public static final int LOOK = 0;
    /** Run the job named now. */
    public static final int RUN = 1;
    /** Save the job named, made or changed. */
    public static final int SAVE = 2;
    /** Delete the job named. */
    public static final int DELETE = 3;
    /** Restore the backup named. */
    public static final int RESTORE = 4;
    /** Write a label on a tape. */
    public static final int LABEL = 5;
    /** Erase a tape. */
    public static final int ERASE = 6;
    /** Take a tape out of its library. */
    public static final int EJECT = 7;
    /** The nil id, for no backup. */
    public static final UUID NONE = new UUID(0L, 0L);
    /** The most system images one restore names. */
    public static final int MOST_IMAGES = 32;

    public static final CustomPacketPayload.Type<BackupActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "backup_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackupActionPayload> STREAM_CODEC = StreamCodec.of(
            BackupActionPayload::write, BackupActionPayload::read);

    public BackupActionPayload {
        images = List.copyOf(images.size() > MOST_IMAGES ? images.subList(0, MOST_IMAGES) : images);
    }

    /** A request with nothing but an action and the job, library and backup it is about. */
    public static BackupActionPayload of(final BlockPos hostPos, final BlockPos monitorPos, final int action,
                                         final String job, final BackupJob.Target target, final UUID backup) {
        return new BackupActionPayload(hostPos, monitorPos, action, job, "", 0, target,
                BackupJob.Schedule.BY_HAND, backup, List.of(), -1, "");
    }

    @Override
    public CustomPacketPayload.Type<BackupActionPayload> type() {
        return TYPE;
    }

    private static void write(final RegistryFriendlyByteBuf buf, final BackupActionPayload payload) {
        BlockPos.STREAM_CODEC.encode(buf, payload.hostPos);
        BlockPos.STREAM_CODEC.encode(buf, payload.monitorPos);
        buf.writeVarInt(payload.action);
        buf.writeUtf(payload.job, BackupJob.MOST_NAME);
        buf.writeUtf(payload.was, BackupJob.MOST_NAME);
        buf.writeVarInt(payload.parts);
        BackupJob.Target.STREAM_CODEC.encode(buf, payload.target);
        BackupJob.Schedule.STREAM_CODEC.encode(buf, payload.when);
        UUIDUtil.STREAM_CODEC.encode(buf, payload.backup);
        UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_IMAGES)).encode(buf, payload.images);
        buf.writeVarInt(payload.tape);
        buf.writeUtf(payload.text, TapeContents.MOST_LABEL);
    }

    private static BackupActionPayload read(final RegistryFriendlyByteBuf buf) {
        return new BackupActionPayload(BlockPos.STREAM_CODEC.decode(buf), BlockPos.STREAM_CODEC.decode(buf),
                buf.readVarInt(), buf.readUtf(BackupJob.MOST_NAME), buf.readUtf(BackupJob.MOST_NAME), buf.readVarInt(),
                BackupJob.Target.STREAM_CODEC.decode(buf), BackupJob.Schedule.STREAM_CODEC.decode(buf),
                UUIDUtil.STREAM_CODEC.decode(buf),
                UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_IMAGES)).decode(buf),
                buf.readVarInt(), buf.readUtf(TapeContents.MOST_LABEL));
    }
}
