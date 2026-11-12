/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.backup.BackupJob;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what a Backup Console shows, all four tabs of it at once, so switching tabs never waits on the
 * network. The service's jobs; what a new job would keep and where it could write; the backups the network holds; the
 * backup chosen in Restore, its system images and what the book of losses lost since it; the tapes of the library
 * shown; and a line saying how the last thing asked went.
 *
 * @param service whether a Backup Service runs, on which server, and how the last thing asked went
 * @param jobs    its jobs
 * @param form    what the Back up tab offers
 * @param backups the backups the network holds, the newest first
 * @param detail  the backup chosen in Restore
 * @param tapes   the library shown in Tapes
 */
public record BackupStatePayload(Service service, List<Job> jobs, Form form, List<Held> backups, Detail detail,
                                 Tapes tapes) implements CustomPacketPayload {

    /** The most jobs, backups and loss lines that travel. */
    public static final int MOST_ROWS = 64;
    /** The most computers, places and lines of files that travel. */
    public static final int MOST_PLACES = 32;
    /** The longest name of a computer or a library that travels. */
    public static final int MOST_NAME = 48;
    /** The longest line of a computer's files that travels. */
    public static final int MOST_LINE = 96;

    public static final CustomPacketPayload.Type<BackupStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "backup_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BackupStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    Service.STREAM_CODEC, BackupStatePayload::service,
                    Job.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_ROWS)), BackupStatePayload::jobs,
                    Form.STREAM_CODEC, BackupStatePayload::form,
                    Held.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_ROWS)), BackupStatePayload::backups,
                    Detail.STREAM_CODEC, BackupStatePayload::detail,
                    Tapes.STREAM_CODEC, BackupStatePayload::tapes,
                    BackupStatePayload::new);

    public BackupStatePayload {
        jobs = List.copyOf(jobs);
        backups = List.copyOf(backups);
    }

    @Override
    public CustomPacketPayload.Type<BackupStatePayload> type() {
        return TYPE;
    }

    /**
     * The service.
     *
     * @param present whether one runs on the network
     * @param host    the server it runs on
     * @param engine  whether an Automation Engine runs to fire the timed jobs
     * @param note    how the last thing asked went
     */
    public record Service(boolean present, String host, boolean engine, Text note) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Service> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Service::present,
                ByteBufCodecs.stringUtf8(MOST_NAME), Service::host,
                ByteBufCodecs.BOOL, Service::engine,
                TextCodecs.STREAM_CODEC, Service::note,
                Service::new);
    }

    /**
     * One job.
     *
     * @param name    its name
     * @param parts   what it keeps, a bit each
     * @param target  where it writes
     * @param where   the library or the computer it writes to, as the network names it
     * @param when    when it runs
     * @param lastRun the world's clock when it last ran, or -1
     * @param result  how its last run went
     */
    public record Job(String name, int parts, BackupJob.Target target, String where, BackupJob.Schedule when,
                      long lastRun, BackupJob.RunState result) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Job> STREAM_CODEC = StreamCodec.of(
                (buf, job) -> {
                    buf.writeUtf(job.name, BackupJob.MOST_NAME);
                    buf.writeVarInt(job.parts);
                    BackupJob.Target.STREAM_CODEC.encode(buf, job.target);
                    buf.writeUtf(job.where, MOST_NAME);
                    BackupJob.Schedule.STREAM_CODEC.encode(buf, job.when);
                    buf.writeVarLong(job.lastRun);
                    BackupJob.RunState.STREAM_CODEC.encode(buf, job.result);
                },
                buf -> new Job(buf.readUtf(BackupJob.MOST_NAME), buf.readVarInt(),
                        BackupJob.Target.STREAM_CODEC.decode(buf), buf.readUtf(MOST_NAME),
                        BackupJob.Schedule.STREAM_CODEC.decode(buf), buf.readVarLong(),
                        BackupJob.RunState.STREAM_CODEC.decode(buf)));
    }

    /**
     * Where a job can write: a Tape Library or a drive of another server.
     *
     * @param target    the place
     * @param name      the library as the network names it, or the computer whose drive it is
     * @param freeMb    the room left there, in megabytes
     * @param mbPerItem what an item weighs there, in megabytes
     */
    public record Place(BackupJob.Target target, String name, long freeMb, long mbPerItem) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Place> STREAM_CODEC = StreamCodec.composite(
                BackupJob.Target.STREAM_CODEC, Place::target,
                ByteBufCodecs.stringUtf8(MOST_NAME), Place::name,
                ByteBufCodecs.VAR_LONG, Place::freeMb,
                ByteBufCodecs.VAR_LONG, Place::mbPerItem,
                Place::new);
    }

    /**
     * What the Back up tab offers.
     *
     * @param items     how many items the network holds
     * @param weight    what they weigh, in thousandths of an item
     * @param files     each computer's home folder, a line each
     * @param machines  the network's computers, by name
     * @param wallets   the network's computers that keep a Teracoin wallet, by name
     * @param libraries the Tape Libraries a job can write to
     * @param drives    the other servers' drives a job can write to
     */
    public record Form(long items, long weight, List<String> files, List<String> machines, List<String> wallets,
                       List<Place> libraries, List<Place> drives) {

        public static final Form EMPTY = new Form(0L, 0L, List.of(), List.of(), List.of(), List.of(), List.of());

        private static final StreamCodec<ByteBuf, List<String>> LINES =
                ByteBufCodecs.stringUtf8(MOST_LINE).apply(ByteBufCodecs.list(MOST_PLACES));
        private static final StreamCodec<ByteBuf, List<String>> NAMES =
                ByteBufCodecs.stringUtf8(MOST_NAME).apply(ByteBufCodecs.list(MOST_PLACES));
        private static final StreamCodec<RegistryFriendlyByteBuf, List<Place>> PLACES =
                Place.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_PLACES));

        /* Seven parts, one more than a composite codec takes, written out in their order. */
        public static final StreamCodec<RegistryFriendlyByteBuf, Form> STREAM_CODEC = StreamCodec.of(
                (buf, form) -> {
                    ByteBufCodecs.VAR_LONG.encode(buf, form.items());
                    ByteBufCodecs.VAR_LONG.encode(buf, form.weight());
                    LINES.encode(buf, form.files());
                    NAMES.encode(buf, form.machines());
                    NAMES.encode(buf, form.wallets());
                    PLACES.encode(buf, form.libraries());
                    PLACES.encode(buf, form.drives());
                },
                buf -> new Form(ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf),
                        LINES.decode(buf), NAMES.decode(buf), NAMES.decode(buf), PLACES.decode(buf),
                        PLACES.decode(buf)));

        public Form {
            files = List.copyOf(files);
            machines = List.copyOf(machines);
            wallets = List.copyOf(wallets);
            libraries = List.copyOf(libraries);
            drives = List.copyOf(drives);
        }
    }

    /**
     * One backup the network holds.
     *
     * @param id      the backup
     * @param job     the job that made it
     * @param clock   the world's clock when it was made
     * @param parts   what it keeps, a bit each
     * @param items   how many items it keeps
     * @param sizeMb  what it takes where it is held, in megabytes
     * @param onTape  whether a tape holds it
     * @param tape    the tape's slot, from 0
     * @param library the library, as the network names it
     * @param machine the computer whose drive holds it
     * @param drive   that drive, from 0
     */
    public record Held(UUID id, String job, long clock, int parts, long items, long sizeMb, boolean onTape, int tape,
                       String library, String machine, int drive) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Held> STREAM_CODEC = StreamCodec.of(
                (buf, held) -> {
                    UUIDUtil.STREAM_CODEC.encode(buf, held.id);
                    buf.writeUtf(held.job, BackupJob.MOST_NAME);
                    buf.writeVarLong(held.clock);
                    buf.writeVarInt(held.parts);
                    buf.writeVarLong(held.items);
                    buf.writeVarLong(held.sizeMb);
                    buf.writeBoolean(held.onTape);
                    buf.writeVarInt(held.tape);
                    buf.writeUtf(held.library, MOST_NAME);
                    buf.writeUtf(held.machine, MOST_NAME);
                    buf.writeVarInt(held.drive);
                },
                buf -> new Held(UUIDUtil.STREAM_CODEC.decode(buf), buf.readUtf(BackupJob.MOST_NAME),
                        buf.readVarLong(), buf.readVarInt(), buf.readVarLong(), buf.readVarLong(), buf.readBoolean(),
                        buf.readVarInt(), buf.readUtf(MOST_NAME), buf.readUtf(MOST_NAME), buf.readVarInt()));
    }

    /**
     * One computer's system image in the backup chosen.
     *
     * @param node    the computer
     * @param machine what it was called
     * @param system  the system it ran
     * @param sizeMb  what the image takes, in megabytes
     */
    public record Image(UUID node, String machine, String system, long sizeMb) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Image> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Image::node,
                ByteBufCodecs.stringUtf8(MOST_NAME), Image::machine,
                ByteBufCodecs.stringUtf8(MOST_NAME), Image::system,
                ByteBufCodecs.VAR_LONG, Image::sizeMb,
                Image::new);
    }

    /**
     * One line of the book of losses since the backup chosen.
     *
     * @param key      the item lost
     * @param lost     how many were lost and are not back yet
     * @param clock    the world's clock when they were lost
     * @param cause    how, by the ordinal of its cause
     * @param machine  the computer it happened on, or that ran the DROP
     * @param disk     the disk it happened on, from 1, or -1
     * @param inBackup how many the backup holds
     * @param back     how many restoring it gives back for this line
     */
    public record Loss(StorageKey key, long lost, long clock, int cause, String machine, int disk, long inBackup,
                       long back) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Loss> STREAM_CODEC = StreamCodec.of(
                (buf, loss) -> {
                    StorageKey.STREAM_CODEC.encode(buf, loss.key);
                    buf.writeVarLong(loss.lost);
                    buf.writeVarLong(loss.clock);
                    buf.writeVarInt(loss.cause);
                    buf.writeUtf(loss.machine, MOST_NAME);
                    buf.writeVarInt(loss.disk);
                    buf.writeVarLong(loss.inBackup);
                    buf.writeVarLong(loss.back);
                },
                buf -> new Loss(StorageKey.STREAM_CODEC.decode(buf), buf.readVarLong(), buf.readVarLong(),
                        buf.readVarInt(), buf.readUtf(MOST_NAME), buf.readVarInt(), buf.readVarLong(),
                        buf.readVarLong()));
    }

    /**
     * The backup chosen in Restore.
     *
     * @param id        the backup, the nil id for none
     * @param images    its system images
     * @param losses    the losses since it the book has not given back
     * @param windTape  the tape a drive is winding to it for a restore, from 0, or -1
     * @param permille  how far that wind has gone, in thousandths
     * @param ticksLeft the ticks left of it
     */
    public record Detail(UUID id, List<Image> images, List<Loss> losses, int windTape, int permille,
                         long ticksLeft) {

        public static final Detail NONE = new Detail(new UUID(0L, 0L), List.of(), List.of(), -1, 0, 0L);

        public static final StreamCodec<RegistryFriendlyByteBuf, Detail> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Detail::id,
                Image.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_PLACES)), Detail::images,
                Loss.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_ROWS)), Detail::losses,
                ByteBufCodecs.VAR_INT, Detail::windTape,
                ByteBufCodecs.VAR_INT, Detail::permille,
                ByteBufCodecs.VAR_LONG, Detail::ticksLeft,
                Detail::new);

        public Detail {
            images = List.copyOf(images);
            losses = List.copyOf(losses);
        }
    }

    /**
     * One slot of the library shown.
     *
     * @param slot      its slot, from 0
     * @param present   whether a tape is in it
     * @param label     the label on the tape
     * @param usedMb    how much of the tape is written, in megabytes
     * @param lastClock the world's clock its last backup was made at, or -1
     */
    public record Tape(int slot, boolean present, String label, long usedMb, long lastClock) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Tape> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Tape::slot,
                ByteBufCodecs.BOOL, Tape::present,
                ByteBufCodecs.stringUtf8(MOST_NAME), Tape::label,
                ByteBufCodecs.VAR_LONG, Tape::usedMb,
                ByteBufCodecs.VAR_LONG, Tape::lastClock,
                Tape::new);
    }

    /**
     * The Tape Library shown in Tapes, and its drive.
     *
     * @param library    the library, or nowhere when the network has none
     * @param name       the library as the network names it
     * @param count      how many libraries the network has
     * @param format     the tapes it takes, their name and size
     * @param capacityMb what each of its tapes holds, in megabytes
     * @param slots      its slots
     * @param drive      what its drive is doing: the tape in it, from 0, or -1; its state; how far it wound, in
     *                   thousandths; the ticks left
     */
    public record Tapes(BackupJob.Target library, String name, int count, String format, long capacityMb,
                        List<Tape> slots, Drive drive) {

        public static final Tapes NONE = new Tapes(BackupJob.Target.NOWHERE, "", 0, "", 0L, List.of(), Drive.IDLE);

        public static final StreamCodec<RegistryFriendlyByteBuf, Tapes> STREAM_CODEC = StreamCodec.of(
                (buf, tapes) -> {
                    BackupJob.Target.STREAM_CODEC.encode(buf, tapes.library);
                    buf.writeUtf(tapes.name, MOST_NAME);
                    buf.writeVarInt(tapes.count);
                    buf.writeUtf(tapes.format, MOST_NAME);
                    buf.writeVarLong(tapes.capacityMb);
                    Tape.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_PLACES)).encode(buf, tapes.slots);
                    Drive.STREAM_CODEC.encode(buf, tapes.drive);
                },
                buf -> new Tapes(BackupJob.Target.STREAM_CODEC.decode(buf), buf.readUtf(MOST_NAME), buf.readVarInt(),
                        buf.readUtf(MOST_NAME), buf.readVarLong(),
                        Tape.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_PLACES)).decode(buf),
                        Drive.STREAM_CODEC.decode(buf)));

        public Tapes {
            slots = List.copyOf(slots);
        }
    }

    /**
     * What a library's drive is doing.
     *
     * @param tape      the tape in it, from 0, or -1
     * @param state     its state, by the ordinal of the drive's states
     * @param permille  how far its wind has gone, in thousandths
     * @param ticksLeft the ticks left of the wind
     * @param job       the job its work is for, or empty
     * @param restoring whether that work is a restore rather than a backup
     */
    public record Drive(int tape, int state, int permille, long ticksLeft, String job, boolean restoring) {

        public static final Drive IDLE = new Drive(-1, 0, 0, 0L, "", false);

        public static final StreamCodec<RegistryFriendlyByteBuf, Drive> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Drive::tape,
                ByteBufCodecs.VAR_INT, Drive::state,
                ByteBufCodecs.VAR_INT, Drive::permille,
                ByteBufCodecs.VAR_LONG, Drive::ticksLeft,
                ByteBufCodecs.stringUtf8(BackupJob.MOST_NAME), Drive::job,
                ByteBufCodecs.BOOL, Drive::restoring,
                Drive::new);
    }
}
