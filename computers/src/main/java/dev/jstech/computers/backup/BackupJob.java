/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One of a Backup Service's jobs: what it keeps, where it writes it and when it runs, and how its last run went
 * (computers/docs/design/programs.md). A timed job is an Automation Engine job besides, which runs it by its name.
 *
 * @param name    what the player calls it, by which it is run
 * @param parts   what it keeps
 * @param target  where it writes its backups
 * @param when    when it runs
 * @param lastRun the world's clock when it last ran, or -1 when it never did
 * @param result  how its last run went
 */
public record BackupJob(String name, Set<BackupPart> parts, Target target, Schedule when, long lastRun,
                        RunState result) {

    /** The longest name a job takes. */
    public static final int MOST_NAME = 24;

    public static final Codec<BackupJob> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(BackupJob::name),
            StableCodecs.byName(BackupPart.class).listOf().xmap(BackupJob::partsOf, List::copyOf).fieldOf("parts")
                    .forGetter(BackupJob::parts),
            Target.CODEC.fieldOf("target").forGetter(BackupJob::target),
            Schedule.CODEC.fieldOf("when").forGetter(BackupJob::when),
            Codec.LONG.optionalFieldOf("last_run", -1L).forGetter(BackupJob::lastRun),
            StableCodecs.byName(RunState.class).optionalFieldOf("result", RunState.NEVER)
                    .forGetter(BackupJob::result)
    ).apply(instance, BackupJob::new));

    public BackupJob {
        name = name.length() > MOST_NAME ? name.substring(0, MOST_NAME) : name;
        parts = parts.isEmpty() ? EnumSet.noneOf(BackupPart.class) : EnumSet.copyOf(parts);
    }

    /** The job with its last run written down: at the world's clock {@code clock}, ending as {@code how}. */
    public BackupJob ran(final long clock, final RunState how) {
        return new BackupJob(name, parts, target, when, clock, how);
    }

    /** The job with its last run ending as {@code how}, the clock it started at kept. */
    public BackupJob ended(final RunState how) {
        return new BackupJob(name, parts, target, when, lastRun, how);
    }

    /**
     * Whether {@code name} may name a job: one word of letters, digits and underscores, so the Automation Engine job
     * that runs it on time names it in the network's language as it is.
     */
    public static boolean validName(final String name) {
        if (name.isEmpty() || name.length() > MOST_NAME) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (!(c == '_' || c < 128 && Character.isLetterOrDigit(c))) {
                return false;
            }
        }
        return true;
    }

    private static Set<BackupPart> partsOf(final List<BackupPart> parts) {
        return parts.isEmpty() ? EnumSet.noneOf(BackupPart.class) : EnumSet.copyOf(parts);
    }

    /**
     * Where a job writes its backups: a Tape Library, by its rack and the row it is topped at, or a drive of another
     * server, by the server's node and the drive among its own counted from 0.
     *
     * @param library whether it is a Tape Library rather than a server's drive
     * @param rack    the library's rack, as a packed position
     * @param row     the row the library is topped at
     * @param server  the server whose drive it is
     * @param drive   the drive of that server, from 0
     */
    public record Target(boolean library, long rack, int row, UUID server, int drive) {

        /** Nowhere: a job whose target went is refused until it is given another. */
        public static final Target NOWHERE = new Target(false, 0L, -1, new UUID(0L, 0L), -1);

        public static final Codec<Target> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("library").forGetter(Target::library),
                Codec.LONG.fieldOf("rack").forGetter(Target::rack),
                Codec.INT.fieldOf("row").forGetter(Target::row),
                UUIDUtil.CODEC.fieldOf("server").forGetter(Target::server),
                Codec.INT.fieldOf("drive").forGetter(Target::drive)
        ).apply(instance, Target::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Target> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Target::library,
                ByteBufCodecs.VAR_LONG, Target::rack,
                ByteBufCodecs.VAR_INT, Target::row,
                UUIDUtil.STREAM_CODEC, Target::server,
                ByteBufCodecs.VAR_INT, Target::drive,
                Target::new);

        /** The Tape Library topped at {@code row} of the rack at {@code rack}. */
        public static Target library(final long rack, final int row) {
            return new Target(true, rack, row, NOWHERE.server(), -1);
        }

        /** Drive {@code drive} of the server {@code server}. */
        public static Target disk(final UUID server, final int drive) {
            return new Target(false, 0L, -1, server, drive);
        }
    }

    /**
     * When a job runs: by hand only, every day at an hour, or one day of the week at an hour.
     *
     * @param kind    which of the three
     * @param hour    the hour of the world's clock it runs at, 0 to 23
     * @param weekday the day of the week a weekly job runs on, 0 for Monday to 6 for Sunday
     */
    public record Schedule(Kind kind, int hour, int weekday) {

        /** By hand only. */
        public static final Schedule BY_HAND = new Schedule(Kind.BY_HAND, 0, 0);

        public static final Codec<Schedule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StableCodecs.byName(Kind.class).fieldOf("kind").forGetter(Schedule::kind),
                Codec.INT.optionalFieldOf("hour", 0).forGetter(Schedule::hour),
                Codec.INT.optionalFieldOf("weekday", 0).forGetter(Schedule::weekday)
        ).apply(instance, Schedule::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Schedule> STREAM_CODEC = StreamCodec.composite(
                StableCodecs.byId(Kind.class, Kind.BY_HAND).cast(), Schedule::kind,
                ByteBufCodecs.VAR_INT, Schedule::hour,
                ByteBufCodecs.VAR_INT, Schedule::weekday,
                Schedule::new);

        public Schedule {
            hour = Math.floorMod(hour, 24);
            weekday = Math.floorMod(weekday, 7);
        }

        /** Whether an Automation Engine job runs it, rather than only a hand. */
        public boolean timed() {
            return kind != Kind.BY_HAND;
        }

        /** The three kinds of schedule, each with the id and the name it travels and is saved by. */
        public enum Kind implements IStableId, IStableName {
            BY_HAND(0, "by_hand"),
            DAILY(1, "daily"),
            WEEKLY(2, "weekly");

            private final int id;
            private final String serializedName;

            Kind(final int id, final String serializedName) {
                this.id = id;
                this.serializedName = serializedName;
            }

            @Override
            public int id() {
                return id;
            }

            @Override
            public String serializedName() {
                return serializedName;
            }
        }
    }

    /** How a job's last run stands, with the id and the name it travels and is saved by. */
    public enum RunState implements IStableId, IStableName {
        /** It never ran. */
        NEVER(0, "never"),
        /** Its backup was written. */
        DONE(1, "done"),
        /** Its target had no room for the backup. */
        NO_ROOM(2, "no_room"),
        /** Its target was not there: no library, no tape of its era, or no such server or drive. */
        NO_TARGET(3, "no_target"),
        /** It was under way when the drive or the service went away. */
        INTERRUPTED(4, "interrupted"),
        /** It is under way: its backup waits at a Tape Library's drive. */
        RUNNING(5, "running");

        public static final StreamCodec<ByteBuf, RunState> STREAM_CODEC = StableCodecs.byId(RunState.class, NEVER);

        private final int id;
        private final String serializedName;

        RunState(final int id, final String serializedName) {
            this.id = id;
            this.serializedName = serializedName;
        }

        @Override
        public int id() {
            return id;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }
}
