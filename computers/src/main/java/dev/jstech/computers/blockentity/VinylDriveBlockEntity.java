/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import com.mojang.serialization.Codec;
import dev.jstech.computers.MediaDevices;
import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.audio.MusicImports;
import dev.jstech.computers.audio.VinylDiscs;
import dev.jstech.computers.audio.VinylRecordings;
import dev.jstech.computers.block.VinylDriveBlock;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaReceipt;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.jstechseries.peripheral.IPeripheralEndpoint;
import dev.jstech.core.jstechseries.peripheral.PeripheralCableType;
import dev.jstech.core.jstechseries.peripheral.PeripheralLink;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.Loaded;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A Vinyl RW Drive (computers/docs/design/peripherals.md): a record cutter on the peripheral cable, driven by
 * Soundfoundry on the computer at its other end. With a disc on its platter it plays the song aloud in the room while
 * Soundfoundry records it, in the song's own time, into a file in the system's Music/Vinyl folder; with a blank vinyl
 * it cuts a recording made from a disc onto it, again in the song's time, and the cut is that disc itself. The sound a
 * recording keeps is the one the recording player's game holds for the disc, brought over while the song plays.
 */
public class VinylDriveBlockEntity extends SyncedBlockEntity implements IPeripheralEndpoint {

    private final FieldItemHandler platter = fields().items("Platter", 1).save().toClient().dropsWhenBroken()
            .slotLimit(1).accepts((index, stack) -> VinylDiscs.playable(stack));
    private final ValueField<Work> work = fields().value("Work", Work.CODEC, Work.NONE).save().toClient();
    /** The world's time the work started at, and how long it lasts, in ticks: the song's length. */
    private final ValueField<Long> started = fields().value("Started", Codec.LONG, 0L).save().toClient();
    private final IntField length = fields().integer("Length", 0).save().toClient();
    /** The song being recorded or cut, by its id. */
    private final ValueField<String> song = fields().value("Song", Codec.STRING, "").save().toClient();
    private final ValueField<String> lastSaved = fields().value("LastSaved", Codec.STRING, "").save().toClient();
    private final ValueField<Problem> problem = fields().value("Problem", Problem.CODEC, Problem.NONE).toClient();
    private final PeripheralLink link = new PeripheralLink(fields(), PeripheralCableType.COMPUTING,
            PeripheralLinks.COMPUTING);
    /* The sound brought for the recording under way, kept until the song ends; not saved: a world stopped mid-song
     * loses the recording, as a cut tape would. */
    @Nullable
    private Brought brought;

    /** The folder under the system's music folder that recordings from a disc go into. */
    public static final String FOLDER = "Vinyl";
    private static final int TICKS_A_DAY = 24_000;

    public VinylDriveBlockEntity(final BlockPos pos, final BlockState state) {
        super(MediaDevices.VINYL_DRIVE_BE.get(), pos, state);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final VinylDriveBlockEntity drive) {
        if (level instanceof ServerLevel server) {
            drive.link.tick(server, pos);
            drive.tickWork(server);
        }
    }

    /** The drive's era, which its port and its look follow. */
    public Era era() {
        return getBlockState().getBlock() instanceof VinylDriveBlock block ? block.era() : HardwareEra.STANDARD;
    }

    public ItemStackHandler platter() {
        return platter;
    }

    /** What lies on the platter. */
    public ItemStack onPlatter() {
        return platter.getStackInSlot(0);
    }

    public Work work() {
        return work.get();
    }

    /** The world's time the work started at. */
    public long started() {
        return started.get();
    }

    /** How long the work lasts, in ticks. */
    public int length() {
        return length.get();
    }

    /** The id of the song being recorded or cut, or empty. */
    public String song() {
        return song.get();
    }

    /** The file the last recording was kept in, or empty. */
    public String lastSaved() {
        return lastSaved.get();
    }

    public Problem problem() {
        return problem.get();
    }

    /**
     * Starts recording the disc on the platter: the song plays aloud in the room and the recording lasts as long as
     * the song; what Soundfoundry asked it for. Empty when there is no disc, or the drive is already at work.
     */
    public Optional<Holder<JukeboxSong>> record(final ServerLevel server) {
        final Optional<Holder<JukeboxSong>> disc = VinylDiscs.songOf(onPlatter(), server.registryAccess());
        if (work.get() != Work.NONE || disc.isEmpty() || disc.get().unwrapKey().isEmpty()) {
            problem.set(disc.isEmpty() ? Problem.NO_DISC : Problem.BUSY);
            return Optional.empty();
        }
        brought = null;
        begin(server, Work.RECORDING, disc.get());
        server.levelEvent(LevelEvent.SOUND_PLAY_JUKEBOX_SONG, worldPosition,
                server.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG).getId(disc.get().value()));
        return disc;
    }

    /**
     * Starts cutting the recording from the disc of {@code from} onto the blank vinyl on the platter, in the song's own
     * time; whether it started. A recording that came from no disc, {@code from} null, is refused.
     */
    public boolean cut(final ServerLevel server, @Nullable final ResourceLocation from) {
        if (work.get() != Work.NONE || !VinylDiscs.blank(onPlatter())) {
            problem.set(work.get() != Work.NONE ? Problem.BUSY : Problem.NO_BLANK);
            return false;
        }
        if (from == null) {
            problem.set(Problem.NOT_FROM_A_DISC);
            return false;
        }
        final Optional<Holder.Reference<JukeboxSong>> found = server.registryAccess()
                .registryOrThrow(Registries.JUKEBOX_SONG).getHolder(ResourceKey.create(Registries.JUKEBOX_SONG, from));
        if (found.isEmpty() || VinylDiscs.discOf(from).isEmpty()) {
            problem.set(Problem.NOT_FROM_A_DISC);
            return false;
        }
        begin(server, Work.CUTTING, found.get());
        return true;
    }

    /** Stops the recording or the cut: a recording is let go, and a cut vinyl stays blank. */
    public void stop(final ServerLevel server) {
        if (work.get() == Work.RECORDING) {
            server.levelEvent(LevelEvent.SOUND_STOP_JUKEBOX_SONG, worldPosition, 0);
        }
        brought = null;
        work.set(Work.NONE);
        song.set("");
    }

    /** Pops the record off the platter, out in front of the drive; nothing while it records or cuts. */
    public void eject(final ServerLevel server) {
        if (work.get() != Work.NONE || onPlatter().isEmpty()) {
            return;
        }
        final BlockPos front = worldPosition.relative(getBlockState().getValue(HorizontalDirectionalBlock.FACING));
        Containers.dropItemStack(server, front.getX() + 0.5, front.getY() + 0.2, front.getZ() + 0.5,
                platter.extractItem(0, 1, false));
    }

    /**
     * The sound of the disc being recorded has come from the recording player's game. It is kept once the song ends;
     * one that comes for another song, or when nothing records, is refused.
     */
    public MediaReceipt brought(final ServerLevel server, final String forSong, final String name,
                                final MediaId media, final MediaInfo info) {
        if (work.get() != Work.RECORDING || !song.get().equals(forSong)) {
            return MediaReceipt.refused(Problem.NOT_RECORDING.words());
        }
        brought = new Brought(name, media, info);
        if (server.getGameTime() >= started.get() + length.get()) {
            finishRecording(server);
        }
        return MediaReceipt.accepted(Problem.KEEPING.words());
    }

    @Override
    public PeripheralCableType cableType() {
        return link.cableType();
    }

    @Override
    public Optional<Long> linkedOwner() {
        return link.linkedOwner();
    }

    @Override
    public void onOwnerLinked(final long ownerPos) {
        link.linked(ownerPos);
    }

    @Override
    public void onOwnerUnlinked() {
        link.unlinked();
    }

    /** The linked computer's position, or null while unlinked. */
    @Nullable
    public BlockPos ownerPos() {
        return link.ownerPos();
    }

    private void begin(final ServerLevel server, final Work what, final Holder<JukeboxSong> of) {
        problem.set(Problem.NONE);
        work.set(what);
        started.set(server.getGameTime());
        length.set(of.value().lengthInTicks());
        song.set(of.unwrapKey().map(key -> key.location().toString()).orElse(""));
    }

    /* The song's time is up: a recording is kept once its sound is here, a cut becomes the disc itself. */
    private void tickWork(final ServerLevel server) {
        if (work.get() == Work.NONE || server.getGameTime() < started.get() + length.get()) {
            return;
        }
        if (work.get() == Work.CUTTING) {
            final ResourceLocation id = ResourceLocation.tryParse(song.get());
            final ItemStack disc = id == null ? ItemStack.EMPTY : VinylDiscs.discOf(id);
            if (!disc.isEmpty() && VinylDiscs.blank(onPlatter())) {
                platter.setStackInSlot(0, disc);
            }
            work.set(Work.NONE);
            song.set("");
        } else if (brought != null) {
            finishRecording(server);
        }
        // A recording whose sound has not come yet waits for it; Stop lets it go.
    }

    /* Keeps the brought sound in the linked computer's Music/Vinyl folder, noted as recorded from the disc. */
    private void finishRecording(final ServerLevel server) {
        final Brought kept = brought;
        final ResourceLocation id = ResourceLocation.tryParse(song.get());
        brought = null;
        work.set(Work.NONE);
        song.set("");
        final BlockPos owner = ownerPos();
        if (kept == null || id == null || owner == null || !Loaded.at(server, owner)
                || !(server.getBlockEntity(owner) instanceof IOsHost computer)) {
            problem.set(Problem.NOT_SAVED);
            return;
        }
        final String folder = MusicImports.musicFolderOf(computer) + "/" + FOLDER;
        final MediaReceipt receipt = MusicImports.keep(server, computer, folder, false, kept.name(), kept.media(),
                kept.info());
        if (!receipt.accepted()) {
            problem.set(Problem.NOT_SAVED);
            return;
        }
        VinylRecordings.of(server).recorded(kept.media(), id, server.getDayTime() / TICKS_A_DAY + 1);
        lastSaved.set(folder + "/" + kept.name());
    }

    /** What the drive is doing. */
    public enum Work implements IStableId {
        NONE(0),
        RECORDING(1),
        CUTTING(2);

        private final int id;

        private static final StableIds<Work> IDS = StableIds.of(Work.class);
        private static final Codec<Work> CODEC = Codec.BYTE.xmap(b -> IDS.byId(b, NONE), w -> (byte) w.id());

        Work(final int id) {
            this.id = id;
        }

        @Override
        public int id() {
            return id;
        }
    }

    /** What stopped the drive last, as Soundfoundry says it. */
    public enum Problem implements IStableId {
        NONE(0),
        NO_DISC(1),
        NO_BLANK(2),
        NOT_FROM_A_DISC(3),
        BUSY(4),
        NOT_SAVED(5),
        NOT_RECORDING(6),
        KEEPING(7);

        private final int id;

        private static final StableIds<Problem> IDS = StableIds.of(Problem.class);
        private static final Codec<Problem> CODEC = Codec.BYTE.xmap(b -> IDS.byId(b, NONE), p -> (byte) p.id());

        Problem(final int id) {
            this.id = id;
        }

        @Override
        public int id() {
            return id;
        }

        /** The words for it; none for nothing wrong. */
        public Text words() {
            return switch (this) {
                case NONE -> Text.EMPTY;
                case NO_DISC -> VinylTexts.NO_DISC.text();
                case NO_BLANK -> VinylTexts.NO_BLANK.text();
                case NOT_FROM_A_DISC -> VinylTexts.NOT_FROM_A_DISC.text();
                case BUSY -> VinylTexts.BUSY.text();
                case NOT_SAVED -> VinylTexts.NOT_SAVED.text();
                case NOT_RECORDING -> VinylTexts.NOT_RECORDING.text();
                case KEEPING -> VinylTexts.KEEPING.text();
            };
        }
    }

    /* The sound a recording player's game brought for the song being recorded. */
    private record Brought(String name, MediaId media, MediaInfo info) {
    }
}
