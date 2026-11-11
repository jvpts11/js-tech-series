/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.audio.VinylDiscs;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.blockentity.VinylDriveBlockEntity;
import dev.jstech.computers.operation.payload.vinyl.VinylPayloads;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.IMediaDrive;
import dev.jstech.computers.os.media.MachineMedia;
import dev.jstech.computers.os.media.MediaDriveType;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Easy Disc Creator's copies (computers/docs/design/programs.md): a medium copied onto a blank one of the same format
 * or a larger one, at the media's own speeds; read first, written, then checked. With one drive, the source is read
 * first and the blank asked for. Install discs copy like any other; a medium that holds the network's storage never
 * does, and neither do the {@code .dat} files, which only show what a disk keeps. A music disc copies through the
 * Vinyl RW Drive: recorded in the song's own time, then cut onto a blank vinyl. One copy a computer at a time.
 */
@TextHolder
public final class DiscCopies {

    private static final Map<Key, Job> JOBS = new HashMap<>();

    /* The letters the program names drives by: the floppy drives from A, the rest from D, a vinyl drive from V. */
    private static final char FLOPPY_FIRST = 'A';
    private static final char OTHER_FIRST = 'D';
    private static final char VINYL_FIRST = 'V';
    /** How long checking a copy takes, in ticks. */
    private static final int CHECK_TICKS = 40;
    private static final long BYTES_A_MEGABYTE = 1_048_576L;
    public static final int MOST_COPIES = 9;
    private static final TextKey NOT_A_MEDIUM = TextKey.of("jsc.disc_copier.not_a_medium",
            "%s: holds nothing to copy.");
    private static final TextKey STORAGE = TextKey.of("jsc.disc_copier.storage",
            "%s: holds the network's storage. Easy Disc Creator copies media, never the items a storage disk keeps.");
    private static final TextKey TOO_SMALL = TextKey.of("jsc.disc_copier.too_small",
            "%s: is smaller than the source. A copy goes onto the same format or a larger one.");
    private static final TextKey NOT_BLANK = TextKey.of("jsc.disc_copier.not_blank",
            "%s: holds no blank to write on. Put in a blank %s.");
    private static final TextKey NO_BLANK_VINYL = TextKey.of("jsc.disc_copier.no_blank_vinyl",
            "%s: holds no blank vinyl to cut the copy onto.");
    private static final TextKey VINYL_ONLY = TextKey.of("jsc.disc_copier.vinyl_only",
            "A music disc copies only through its own Vinyl RW Drive, recorded and cut there.");
    private static final TextKey BUSY = TextKey.of("jsc.disc_copier.busy", "A copy is already under way.");
    private static final TextKey PUT_BLANK = TextKey.of("jsc.disc_copier.put_blank",
            "The source is read. Take it out of %s: and put in a blank %s.");
    private static final TextKey NEXT_BLANK = TextKey.of("jsc.disc_copier.next_blank",
            "Copy %s is done. Put the next blank %s in %s:.");
    private static final TextKey VINYL_RECORDED = TextKey.of("jsc.disc_copier.vinyl_recorded",
            "%s is recorded. Take the music disc out of %s: and put in a blank vinyl to cut the copy.");
    private static final TextKey DONE = TextKey.of("jsc.disc_copier.done", "The copy is done.");
    private static final TextKey LOST = TextKey.of("jsc.disc_copier.lost",
            "The copy stopped: a drive is gone or its medium was taken out.");

    private DiscCopies() {
    }

    /** Hands the game's bus what moves the copies on, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> tick(event.getServer().getAllLevels()));
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, event -> JOBS.clear());
    }

    /** The drives of {@code host} the program copies with, each lettered as the program names it. */
    public static List<Place> places(final ServerLevel level, final IOsHost host) {
        final List<Place> out = new ArrayList<>();
        char floppy = FLOPPY_FIRST;
        char other = OTHER_FIRST;
        for (final IMediaDrive drive : MachineMedia.of(host, level).drives()) {
            if (drive.type() == MediaDriveType.FLOPPY_DRIVE) {
                out.add(new Place(drive.pos().asLong(), floppy++, drive.type(), drive.medium().copy(), false));
            } else {
                out.add(new Place(drive.pos().asLong(), other++, drive.type(), drive.medium().copy(), false));
            }
        }
        char vinyl = VINYL_FIRST;
        final List<Long> endpoints = new ArrayList<>(host.enabledEndpoints());
        Collections.sort(endpoints);
        for (final long at : endpoints) {
            if (Loaded.blockEntity(level, BlockPos.of(at)) instanceof VinylDriveBlockEntity drive) {
                out.add(new Place(at, vinyl++, null, drive.onPlatter().copy(), true));
            }
        }
        return out;
    }

    /**
     * Starts copying what lies in the drive at {@code source} onto the blank in the drive at {@code target}; why not,
     * or nothing when it started.
     */
    public static Text start(final ServerLevel level, final IOsHost host, final BlockPos hostPos,
                             final ServerPlayer player, final long source, final long target, final int copies) {
        final Key key = new Key(level.dimension(), hostPos);
        final Job running = JOBS.get(key);
        if (running != null && running.phase != Phase.DONE) {
            return BUSY.text();
        }
        JOBS.remove(key);
        final List<Place> places = places(level, host);
        final Place from = find(places, source);
        final Place to = find(places, target);
        if (from == null || to == null) {
            return LOST.text();
        }
        if (from.vinyl() || to.vinyl()) {
            return startVinyl(level, host, hostPos, player, key, from, to);
        }
        final Text why = refusal(from, to, from.pos() == to.pos());
        if (why != null) {
            return why;
        }
        final MediaFormat format = format(from.medium());
        final long bytes = bytesOf(from.medium());
        JOBS.put(key, new Job(from.pos(), to.pos(), from.letter(), to.letter(), format, bytes,
                Math.max(1, Math.min(MOST_COPIES, copies)), Phase.READING, ticksFor(format, bytes), from.medium()));
        return Text.EMPTY;
    }

    /** The player said OK to what the program asked: the blank is in, the next step goes on. */
    public static Text proceed(final ServerLevel level, final IOsHost host, final BlockPos hostPos) {
        final Key key = new Key(level.dimension(), hostPos);
        final Job job = JOBS.get(key);
        if (job == null || job.phase != Phase.ASKING) {
            return Text.EMPTY;
        }
        if (job.vinyl) {
            if (Loaded.blockEntity(level, BlockPos.of(job.target)) instanceof VinylDriveBlockEntity drive
                    && drive.cut(level, ResourceLocation.tryParse(job.song))) {
                job.phase = Phase.CUTTING;
                return Text.EMPTY;
            }
            return VinylDiscs.blank(drivePlatter(level, job.target)) ? LOST.text()
                    : NO_BLANK_VINYL.with(Character.toString(job.targetLetter));
        }
        final Place to = find(places(level, host), job.target);
        if (to == null || !blank(to.medium()) || format(to.medium()) == null
                || capacityOf(to.medium()) < capacityOf(job.image)) {
            return NOT_BLANK.with(Character.toString(job.targetLetter), job.format.text());
        }
        job.phase = Phase.WRITING;
        job.left = ticksFor(format(to.medium()), job.bytes);
        job.total = job.left;
        return Text.EMPTY;
    }

    /** Stops the copy on {@code hostPos}: what was written stays, and a drive at work stops. */
    public static void cancel(final ServerLevel level, final BlockPos hostPos) {
        final Job job = JOBS.remove(new Key(level.dimension(), hostPos));
        if (job != null && job.vinyl && Loaded.blockEntity(level, BlockPos.of(job.target))
                instanceof VinylDriveBlockEntity drive) {
            drive.stop(level);
        }
    }

    /** The copy on {@code hostPos}, as it stands, or empty. */
    public static Optional<Job> of(final ServerLevel level, final BlockPos hostPos) {
        return Optional.ofNullable(JOBS.get(new Key(level.dimension(), hostPos)));
    }

    /** Why {@code from} does not copy onto {@code to}, or null when it does; one drive asks for the blank later. */
    @Nullable
    public static Text refusal(final Place from, final Place to, final boolean oneDrive) {
        final String letter = Character.toString(from.letter());
        if (MediaItem.data(from.medium()).total() > 0) {
            return STORAGE.with(letter);
        }
        final MediaFormat format = format(from.medium());
        if (format == null || blank(from.medium())) {
            return NOT_A_MEDIUM.with(letter);
        }
        if (oneDrive) {
            return null;
        }
        final MediaFormat onto = format(to.medium());
        if (onto == null || !blank(to.medium())) {
            return NOT_BLANK.with(Character.toString(to.letter()), format.text());
        }
        return capacityOf(to.medium()) < capacityOf(from.medium()) ? TOO_SMALL.with(Character.toString(to.letter()))
                : null;
    }

    /** Whether {@code medium} is a writable one with nothing on it: no files, no installer, no stored items. */
    public static boolean blank(final ItemStack medium) {
        return medium.getItem() instanceof FormattedMediaItem item && item.writable()
                && !medium.has(ComputingComponents.MEDIA_PAYLOAD.get())
                && medium.getOrDefault(ComputingComponents.FILESYSTEM.get(), FilesystemContents.EMPTY).files()
                .isEmpty() && MediaItem.data(medium).total() == 0;
    }

    /** How many bytes copying {@code medium} moves: its files, and the system or program an install disc carries. */
    public static long bytesOf(final ItemStack medium) {
        long bytes = 0L;
        for (final StoredFile file : medium.getOrDefault(ComputingComponents.FILESYSTEM.get(),
                FilesystemContents.EMPTY).files().values()) {
            bytes += file.byteSize();
        }
        final ResourceLocation payload = medium.getItem() instanceof MediaItem ? MediaItem.payload(medium) : null;
        if (payload != null && MediaItem.kind(medium) == MediaKind.OS_INSTALL) {
            final OsDef os = OsRegistry.getOs(payload);
            bytes += os == null ? 0L : os.footprintMb() * BYTES_A_MEGABYTE;
        } else if (payload != null && MediaItem.kind(medium) == MediaKind.PROGRAM_INSTALL) {
            final ProgramSpec program = Programs.get(payload);
            bytes += program == null ? 0L : program.minDiskMb() * BYTES_A_MEGABYTE;
        }
        return Math.max(1L, bytes);
    }

    /** What a copy carries onto {@code onto}: the files, what an install disc installs and which disc, its label. */
    public static void copyOnto(final ItemStack image, final ItemStack onto) {
        onto.set(ComputingComponents.FILESYSTEM.get(), image.getOrDefault(ComputingComponents.FILESYSTEM.get(),
                FilesystemContents.EMPTY));
        if (image.has(ComputingComponents.MEDIA_KIND.get())) {
            onto.set(ComputingComponents.MEDIA_KIND.get(), image.get(ComputingComponents.MEDIA_KIND.get()));
        }
        if (image.has(ComputingComponents.MEDIA_PAYLOAD.get())) {
            onto.set(ComputingComponents.MEDIA_PAYLOAD.get(), image.get(ComputingComponents.MEDIA_PAYLOAD.get()));
        }
        if (image.has(ComputingComponents.MEDIA_PART.get())) {
            onto.set(ComputingComponents.MEDIA_PART.get(), image.get(ComputingComponents.MEDIA_PART.get()));
        }
        if (image.has(DataComponents.CUSTOM_NAME)) {
            onto.set(DataComponents.CUSTOM_NAME, image.get(DataComponents.CUSTOM_NAME));
        }
    }

    /** How many ticks reading or writing {@code bytes} on {@code format} takes: its seek, then its bytes. */
    public static int ticksFor(@Nullable final MediaFormat format, final long bytes) {
        if (format == null) {
            return 1;
        }
        final int speed = PatternEncoderBlockEntity.bytesPerTick(format);
        return (int) Math.min(Integer.MAX_VALUE, PatternEncoderBlockEntity.seekTicks(format)
                + (speed == Integer.MAX_VALUE ? 0L : bytes / speed) + 1L);
    }

    /* A vinyl copy: the disc on a Vinyl RW Drive is recorded, then cut onto a blank vinyl there. */
    private static Text startVinyl(final ServerLevel level, final IOsHost host, final BlockPos hostPos,
                                   final ServerPlayer player, final Key key, final Place from, final Place to) {
        if (!from.vinyl() || from.pos() != to.pos()
                || !(Loaded.blockEntity(level, BlockPos.of(from.pos())) instanceof VinylDriveBlockEntity drive)) {
            return VINYL_ONLY.text();
        }
        if (drive.record(level).isEmpty()) {
            return drive.problem().words();
        }
        final Job job = new Job(from.pos(), to.pos(), from.letter(), to.letter(), MediaFormat.CD, 1L, 1,
                Phase.RECORDING, drive.length(), from.medium());
        job.vinyl = true;
        job.song = drive.song();
        job.kept = drive.lastSaved();
        JOBS.put(key, job);
        // The player's game brings the disc's sound, as it does for Soundfoundry.
        PacketDistributor.sendToPlayer(player, VinylPayloads.state(level, host, hostPos, drive, true));
        return Text.EMPTY;
    }

    private static void tick(final Iterable<ServerLevel> levels) {
        if (JOBS.isEmpty()) {
            return;
        }
        for (final ServerLevel level : levels) {
            JOBS.entrySet().removeIf(entry -> entry.getKey().dimension().equals(level.dimension())
                    && step(level, entry.getKey().host(), entry.getValue()));
        }
    }

    /* One tick of a copy made at the computer at {@code host}; whether it is over and goes. */
    private static boolean step(final ServerLevel level, final BlockPos host, final Job job) {
        if (job.vinyl) {
            return stepVinyl(level, job);
        }
        if (job.phase == Phase.ASKING || job.phase == Phase.DONE) {
            return false;
        }
        job.left--;
        if (job.left > 0) {
            return false;
        }
        switch (job.phase) {
            case READING -> {
                job.phase = job.source == job.target ? Phase.ASKING : Phase.WRITING;
                job.ask = job.phase == Phase.ASKING ? PUT_BLANK.with(Character.toString(job.sourceLetter),
                        job.format.text()) : Text.EMPTY;
                job.left = ticksFor(job.format, job.bytes);
                job.total = job.left;
            }
            case WRITING -> {
                if (!write(level, job)) {
                    job.note = LOST.text();
                    job.phase = Phase.DONE;
                    return false;
                }
                job.phase = Phase.CHECKING;
                job.left = CHECK_TICKS;
                job.total = CHECK_TICKS;
            }
            case CHECKING -> {
                job.made++;
                // A medium with no kind reads as a system's installer, so only one carrying a system counts.
                if (MediaItem.kind(job.image) == MediaKind.OS_INSTALL
                        && job.image.has(ComputingComponents.MEDIA_PAYLOAD.get())) {
                    JscEvents.awardOperatorAt(level, host, JscEvents.INSTALLER_COPIED);
                }
                if (job.made >= job.copies) {
                    job.phase = Phase.DONE;
                    job.note = DONE.text();
                } else {
                    job.phase = Phase.ASKING;
                    job.ask = NEXT_BLANK.with(job.made, job.format.text(), Character.toString(job.targetLetter));
                }
            }
            default -> {
            }
        }
        return false;
    }

    /* A vinyl copy waits on its drive: the recording kept, then the cut made. */
    private static boolean stepVinyl(final ServerLevel level, final Job job) {
        if (!(Loaded.blockEntity(level, BlockPos.of(job.target)) instanceof VinylDriveBlockEntity drive)) {
            job.phase = Phase.DONE;
            job.note = LOST.text();
            return false;
        }
        job.left = Math.max(0, drive.length() - (int) (level.getGameTime() - drive.started()));
        job.total = drive.length();
        if (job.phase == Phase.RECORDING && drive.work() == VinylDriveBlockEntity.Work.NONE) {
            job.phase = drive.lastSaved().equals(job.kept) ? Phase.DONE : Phase.ASKING;
            job.note = job.phase == Phase.DONE ? drive.problem().words() : Text.EMPTY;
            job.ask = VINYL_RECORDED.with(drive.lastSaved().substring(drive.lastSaved().lastIndexOf('/') + 1),
                    Character.toString(job.targetLetter));
        } else if (job.phase == Phase.CUTTING && drive.work() == VinylDriveBlockEntity.Work.NONE) {
            job.phase = Phase.DONE;
            job.note = DONE.text();
        }
        return false;
    }

    /* Puts the image read from the source on the blank in the target drive, as the source carried it. */
    private static boolean write(final ServerLevel level, final Job job) {
        for (final IMediaDrive drive : drivesAt(level, job)) {
            if (drive.pos().asLong() == job.target && blank(drive.medium())) {
                copyOnto(job.image, drive.medium());
                drive.commit();
                return true;
            }
        }
        return false;
    }

    /* The drives the copy's host has, found again: the job keeps their places, not the drives themselves. */
    private static List<IMediaDrive> drivesAt(final ServerLevel level, final Job job) {
        for (final Map.Entry<Key, Job> entry : JOBS.entrySet()) {
            if (entry.getValue() == job && Loaded.blockEntity(level, entry.getKey().host()) instanceof IOsHost host) {
                return MachineMedia.of(host, level).drives();
            }
        }
        return List.of();
    }

    private static ItemStack drivePlatter(final ServerLevel level, final long at) {
        return Loaded.blockEntity(level, BlockPos.of(at)) instanceof VinylDriveBlockEntity drive ? drive.onPlatter()
                : ItemStack.EMPTY;
    }

    @Nullable
    private static MediaFormat format(final ItemStack medium) {
        return medium.getItem() instanceof FormattedMediaItem item ? item.format() : null;
    }

    /** The bytes {@code medium} holds, 0 for no medium: a copy needs a blank at least as big as the disc it copies. */
    public static long capacityOf(final ItemStack medium) {
        return medium.getItem() instanceof FormattedMediaItem item ? item.capacityBytes() : 0L;
    }

    @Nullable
    private static Place find(final List<Place> places, final long pos) {
        for (final Place place : places) {
            if (place.pos() == pos) {
                return place;
            }
        }
        return null;
    }

    /**
     * A drive the program copies with.
     *
     * @param pos    where it is
     * @param letter the letter the program names it by
     * @param type   the kind of drive, or null for a Vinyl RW Drive
     * @param medium what it holds, a copy
     * @param vinyl  whether it is a Vinyl RW Drive
     */
    public record Place(long pos, char letter, @Nullable MediaDriveType type, ItemStack medium, boolean vinyl) {
    }

    /** Where a copy stands; its id is what the program is told. */
    public enum Phase implements IStableId {
        READING(0),
        ASKING(1),
        WRITING(2),
        CHECKING(3),
        RECORDING(4),
        CUTTING(5),
        DONE(6);

        private final int id;

        private static final StableIds<Phase> IDS = StableIds.of(Phase.class);

        Phase(final int id) {
            this.id = id;
        }

        @Override
        public int id() {
            return id;
        }

        /** The phase of an id, or null for one no phase declares. */
        @Nullable
        public static Phase byId(final int id) {
            return IDS.byId(id, null);
        }
    }

    /** A copy under way, kept while the server runs. */
    public static final class Job {

        private final long source;
        private final long target;
        private final char sourceLetter;
        private final char targetLetter;
        private final MediaFormat format;
        private final long bytes;
        private final int copies;
        private final ItemStack image;
        private Phase phase;
        private int left;
        private int total;
        private int made;
        private boolean vinyl;
        private String song = "";
        private String kept = "";
        private Text ask = Text.EMPTY;
        private Text note = Text.EMPTY;

        Job(final long source, final long target, final char sourceLetter, final char targetLetter,
            final MediaFormat format, final long bytes, final int copies, final Phase phase, final int ticks,
            final ItemStack image) {
            this.source = source;
            this.target = target;
            this.sourceLetter = sourceLetter;
            this.targetLetter = targetLetter;
            this.format = format;
            this.bytes = bytes;
            this.copies = copies;
            this.phase = phase;
            this.left = ticks;
            this.total = ticks;
            this.image = image.copy();
        }

        public Phase phase() {
            return phase;
        }

        public char sourceLetter() {
            return sourceLetter;
        }

        public char targetLetter() {
            return targetLetter;
        }

        /** How far through the step it is on, from 0 to 1. */
        public double done() {
            return total <= 0 ? 1.0 : 1.0 - Math.max(0, left) / (double) total;
        }

        /** The ticks the step it is on still takes. */
        public int left() {
            return Math.max(0, left);
        }

        /** How many bytes the copy moves. */
        public long bytes() {
            return bytes;
        }

        public boolean vinyl() {
            return vinyl;
        }

        /** What the program asks of the player now, or nothing. */
        public Text ask() {
            return phase == Phase.ASKING ? ask : Text.EMPTY;
        }

        /** What the copy came to, once it is over. */
        public Text note() {
            return note;
        }

        /** The medium copied, as it was read. */
        public ItemStack image() {
            return image.copy();
        }
    }

    /* A computer, by its world and place. */
    private record Key(ResourceKey<Level> dimension, BlockPos host) {
    }
}
