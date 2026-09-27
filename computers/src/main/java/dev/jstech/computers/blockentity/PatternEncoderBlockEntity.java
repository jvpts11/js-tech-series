/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import com.mojang.serialization.Codec;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.advancement.Acting;
import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.advancement.MachineOperators;
import dev.jstech.computers.audio.MediaBaySounds;
import dev.jstech.computers.block.PatternEncoderBlock;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaBay;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.peripheral.IPeripheralEndpoint;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.peripheral.PeripheralLink;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextTags;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * The Pattern Encoder: the burner that puts recipe files onto removable media. It is a peripheral of a computer,
 * linked over the peripheral cable like a drive, and it authors nothing itself; the computer's Pattern Studio does
 * the authoring and hands finished files over one at a time. Each file is a job: the head seeks, the bytes go down at
 * the medium's rate, the medium is read back and compared, and the bay stays locked until the job is over. Jobs queue
 * up, so a session's worth of recipes can be sent at once.
 *
 * <p>The encoder comes in three eras, and each writes the media of its day: a Vintage encoder writes floppy disks, a
 * Legacy one writes CDs, a Standard one writes DVDs, CDs and USB sticks (and no floppies).
 *
 * <p>The players who see it are sent the medium, the job's phase and progress, the file, the message, the count
 * written, the link and how many jobs wait; the waiting jobs themselves stay on the server.
 */
@TextHolder
public class PatternEncoderBlockEntity extends SyncedBlockEntity implements IPeripheralEndpoint, GeoBlockEntity {

    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);
    /** What the bay sounds like taking a disc or a USB drive in and giving it back. */
    private final MediaBaySounds baySounds = new MediaBaySounds();
    /** On the client, the medium drawn in the bay, kept a moment after it is taken so its way out is seen. */
    private final MediaBay bay = new MediaBay();
    /*
     * The bay is locked while the head is on the medium: pulling the disc mid-write is how a real burner ruins one,
     * so the encoder simply refuses.
     */
    private final FieldItemHandler media = fields().items("Media", 1).save().toClient().dropsWhenBroken()
            .slotLimit(1).accepts((index, stack) -> acceptsMedia(stack)).lockedWhile(this::locked)
            .onChange(index -> mediaMoved()).onLoad(() -> baySounds.settle(mediaStack()));
    private final Deque<BurnRequest> queue = new ArrayDeque<>();
    private final PartField queuePart = fields().part("Queue", new QueuePart()).save();
    private final DerivedInt queueSize = fields().derived("QueueSize", () -> queue.size()).toClient();
    private final ValueField<Phase> phase = fields().value("Phase", PHASE_CODEC, Phase.IDLE).save().toClient();
    private final IntField phaseTicks = fields().integer("PhaseTicks", 0).save();
    /*
     * The players are sent when the phase began rather than how far it has gone, so a bar moving every tick costs one
     * update a phase and not one a tick; the client works the rest out from the world's clock.
     */
    private final LongField phaseStartedAt = fields().longInteger("PhaseStartedAt", 0L).toClient();
    private final IntField phaseTotal = fields().integer("PhaseTotal", 0).save().toClient();
    private final IntField writeTicks = fields().integer("WriteTicks", 0).save().toClient();
    private final ValueField<String> currentFile = fields().value("CurrentFile", Codec.STRING, "").save().toClient();
    private final ValueField<Text> message = fields().value("Message", TEXT_CODEC, Text.EMPTY).save().toClient();
    private final IntField completed = fields().integer("Completed", 0).save().toClient();
    private final PeripheralLink link = new PeripheralLink(fields(), PeripheralCableType.COMPUTING,
            PeripheralLinks.COMPUTING);
    /** The medium held just before an update from the server was read, on the client. */
    private ItemStack beforeUpdate = ItemStack.EMPTY;
    /** Whether the block is being broken, when the medium leaves without the sound of an eject. */
    private boolean breaking;

    /** The most jobs waiting behind the one being written. */
    public static final int QUEUE_MAX = 8;
    /** How long a finished or failed job stays on the display before the next one starts. */
    public static final int HOLD_TICKS = 30;

    /** A phase saved as the byte of its id, as the encoder always saved it. */
    private static final Codec<Phase> PHASE_CODEC = Codec.BYTE.xmap(Phase::byId, phase -> (byte) phase.id());
    /** A message saved as a translatable text's tag. */
    private static final Codec<Text> TEXT_CODEC = CompoundTag.CODEC.xmap(TextTags::read, TextTags::write);

    private static final TextKey READY = TextKey.of("jsc.pattern_encoder.ready", "Ready");
    private static final TextKey STARTING = TextKey.of("jsc.pattern_encoder.starting", "Starting...");
    private static final TextKey INSERT_MEDIA = TextKey.of("jsc.pattern_encoder.insert_media", "Insert media");
    private static final TextKey SEEKING = TextKey.of("jsc.pattern_encoder.seeking", "Seeking");
    private static final TextKey WRITING = TextKey.of("jsc.pattern_encoder.writing", "Writing %s.craft");
    private static final TextKey VERIFYING = TextKey.of("jsc.pattern_encoder.verifying", "Verifying %s.craft");
    private static final TextKey DONE_WRITING = TextKey.of("jsc.pattern_encoder.done_writing", "Done: %s.craft");
    private static final TextKey FAILED = TextKey.of("jsc.pattern_encoder.error", "Error");
    private static final TextKey CANCELLED = TextKey.of("jsc.pattern_encoder.cancelled", "Cancelled");
    private static final TextKey WRITE_FAILED = TextKey.of("jsc.pattern_encoder.write_failed", "Write failed: %s");
    private static final TextKey MEDIUM_FULL = TextKey.of("jsc.pattern_encoder.medium_full", "medium full");
    private static final TextKey NO_MEDIUM = TextKey.of("jsc.pattern_encoder.no_medium", "no medium");
    private static final TextKey VERIFY_FAILED = TextKey.of("jsc.pattern_encoder.verify_failed", "Verify failed: %s");

    public PatternEncoderBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.PATTERN_ENCODER_BE.get(), pos, state);
        fields().whenBroken((level, at) -> breaking = true);
    }

    /** Whether an encoder of {@code era} writes media of {@code format}. */
    public static boolean eraAccepts(final HardwareEra era, final MediaFormat format) {
        return switch (era) {
            case VINTAGE -> format == MediaFormat.FLOPPY;
            case LEGACY -> format == MediaFormat.CD;
            default -> format == MediaFormat.DVD || format == MediaFormat.CD || format == MediaFormat.USB;
        };
    }

    /**
     * Bytes the head lays down per tick on {@code format}. The fixed part of a burn is the drive, not the file: a
     * floppy seeks before it writes and a CD spins up, which is why an old drive feels slow even for a tiny file. A
     * USB stick has no head to move and takes the file at once.
     */
    public static int bytesPerTick(final MediaFormat format) {
        return switch (format) {
            case FLOPPY -> 250;
            case CD -> 1024;
            case DVD -> 4096;
            case USB -> Integer.MAX_VALUE;
        };
    }

    /** Ticks {@code format} spends finding its place before the first byte: a seek, a spin-up, a handshake. */
    public static int seekTicks(final MediaFormat format) {
        return switch (format) {
            case FLOPPY -> 20;
            case CD -> 30;
            case DVD -> 15;
            case USB -> 4;
        };
    }

    /** Ticks the read-back after the last byte takes on {@code format}. */
    public static int verifyTicks(final MediaFormat format) {
        return switch (format) {
            case FLOPPY, CD -> 10;
            case DVD -> 5;
            case USB -> 2;
        };
    }

    /** The whole burn of {@code bytes} on {@code format}: seek, write and verify. */
    public static int burnTicks(final MediaFormat format, final int bytes) {
        return seekTicks(format) + writeTicks(format, bytes) + verifyTicks(format);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final PatternEncoderBlockEntity encoder) {
        if (level instanceof ServerLevel server) {
            encoder.link.tick(server, pos);
            encoder.tickJob(server);
            /*
             * Unchanged while a phase runs, since its count and the clock move together; it moves when a phase begins,
             * or when the chunk was away and the clock went on without it. An idle encoder counts nothing.
             */
            if (encoder.phase.get() != Phase.IDLE) {
                encoder.phaseStartedAt.set(server.getGameTime() - encoder.phaseTicks.get());
            }
        }
    }

    /** The era of this encoder's chassis, which decides the media it writes. */
    public HardwareEra era() {
        return getBlockState().getBlock() instanceof PatternEncoderBlock block ? block.era() : HardwareEra.STANDARD;
    }

    /** Whether the bay takes {@code stack}: writable media of a format this era's encoder writes. */
    public boolean acceptsMedia(final ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof FormattedMediaItem item
                && item.writable()
                && eraAccepts(era(), item.format());
    }

    public ItemStackHandler media() {
        return media;
    }

    /**
     * The medium the client draws in the bay: the one in it, or, for a moment after one was taken out, that one,
     * so its way out is seen; the eject clip hides it at the moment it is taken.
     */
    public ItemStack drawnMedium() {
        return bay.drawn(mediaStack(), level);
    }

    public ItemStack mediaStack() {
        return media.getStackInSlot(0);
    }

    public boolean hasMedia() {
        return !mediaStack().isEmpty();
    }

    /** Puts {@code stack} in the bay if it is empty and the medium is accepted; returns what was not taken. */
    public ItemStack insertMedia(final ItemStack stack) {
        return media.insertItem(0, stack, false);
    }

    /** Takes the medium out, unless a job holds it; empty when nothing came out. */
    public ItemStack ejectMedia() {
        return media.extractItem(0, 1, false);
    }

    /**
     * Queues a file for burning. Refused when the queue is full or the name is not one the filesystem can hold; the
     * medium is checked when the job starts, so a job can be queued before the disc goes in.
     */
    public boolean queueBurn(final String fileName, final String content) {
        if (queue.size() >= QUEUE_MAX || fileName == null || fileName.isBlank() || content == null
                || fileName.length() + ".craft".length() > FsPaths.MAX_NAME_LENGTH) {
            return false;
        }
        queue.addLast(new BurnRequest(fileName, content));
        // The burn finishes on its own later; whoever asked for it is the one it is credited to.
        Acting.current().ifPresent(player -> MachineOperators.note(this, player));
        queuePart.changed();
        return true;
    }

    public int queued() {
        return queue.size();
    }

    public Phase phase() {
        return phase.get();
    }

    /** Whether the head is on the medium (seeking, writing or verifying). */
    public boolean busy() {
        final Phase now = phase.get();
        return now == Phase.SEEK || now == Phase.WRITE || now == Phase.VERIFY;
    }

    /** Whether the bay refuses to give the medium up. */
    public boolean locked() {
        return busy();
    }

    /** The file being burned (or just burned), without its extension. */
    public String currentFile() {
        return currentFile.get();
    }

    /** Files burned since the block was placed. */
    public int completed() {
        return completed.get();
    }

    /** The format of the medium in the bay, or null with the bay empty; what the body draws. */
    @Nullable
    public MediaFormat mediaFormat() {
        return bayFormat();
    }

    /** Progress of the current job across seek, write and verify, 0..100. */
    public int progressPercent() {
        final Phase now = phase.get();
        if (now == Phase.DONE) {
            return 100;
        }
        if (!busy()) {
            return 0;
        }
        final MediaFormat format = bayFormat();
        final int seek = format == null ? 0 : seekTicks(format);
        final int verify = format == null ? 0 : verifyTicks(format);
        final int total = seek + writeTicks.get() + verify;
        final int inPhase = phaseElapsed();
        final int elapsed = switch (now) {
            case SEEK -> inPhase;
            case WRITE -> seek + inPhase;
            case VERIFY -> seek + writeTicks.get() + inPhase;
            default -> 0;
        };
        return total <= 0 ? 0 : Math.max(0, Math.min(100, elapsed * 100 / total));
    }

    /** One line for a display: what the encoder is doing, or why it stopped. */
    public Text statusLine() {
        return switch (phase.get()) {
            case IDLE -> hasMedia() ? (queue.isEmpty() ? READY.text() : STARTING.text()) : INSERT_MEDIA.text();
            case SEEK -> SEEKING.text();
            case WRITE -> WRITING.with(currentFile.get());
            case VERIFY -> VERIFYING.with(currentFile.get());
            case DONE -> DONE_WRITING.with(currentFile.get());
            case ERROR -> message.get().isEmpty() ? FAILED.text() : message.get();
        };
    }

    /** The reason the last job failed, or nothing. */
    public Text lastError() {
        return phase.get() == Phase.ERROR ? message.get() : Text.EMPTY;
    }

    /** Drops every waiting job and stops the current one; nothing half-written is left on the medium. */
    public void cancelAll() {
        queue.clear();
        queuePart.changed();
        if (busy()) {
            enter(Phase.ERROR, HOLD_TICKS);
            message.set(CANCELLED.text());
        }
    }

    /** The waiting jobs' file names, for a display. */
    public List<String> queuedNames() {
        final List<String> names = new ArrayList<>();
        for (final BurnRequest job : queue) {
            names.add(job.fileName());
        }
        return names;
    }

    /** The queue length a display shows: the count the server sent, on the client; the real one on the server. */
    public int displayQueued() {
        return queueSize.getAsInt();
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

    /*
     * The body's only motion is the medium going in and coming out: the tray riding out and back, a floppy sliding
     * through its slot, a stick going into its port. Each plays once, triggered by the server when the bay's slot
     * fills or empties; the lamps are bone visibility set by the renderer, which blinks them.
     */
    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(MediaBay.controller(this, "pattern_encoder"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }

    @Override
    protected void beforeClientUpdate() {
        beforeUpdate = mediaStack().copy();
    }

    @Override
    protected void afterClientUpdate() {
        bay.seen(beforeUpdate, mediaStack(), level);
    }

    /* How far the current phase has gone: counted on the server, worked out from the world's clock on the client. */
    private int phaseElapsed() {
        if (level == null || !level.isClientSide()) {
            return phaseTicks.get();
        }
        return (int) Math.max(0L, Math.min(phaseTotal.get(), level.getGameTime() - phaseStartedAt.get()));
    }

    private static int writeTicks(final MediaFormat format, final int bytes) {
        return Math.max(1, (int) Math.ceil(bytes / (double) bytesPerTick(format)));
    }

    /** What the medium has left for files, in the filesystem's weight units. */
    private static long mediaFreeWeight(final ItemStack mediaStack) {
        if (!(mediaStack.getItem() instanceof FormattedMediaItem item)) {
            return 0L;
        }
        final long capacity = (long) item.format().capacityItems() * StorageKey.MB_EQ_PER_ITEM;
        final long used = DiskFilesystem.filesWeight(mediaStack);
        return Math.max(0L, capacity - used);
    }

    /** The format in the bay, or null with the bay empty. */
    @Nullable
    private MediaFormat bayFormat() {
        return mediaStack().getItem() instanceof FormattedMediaItem item ? item.format() : null;
    }

    /* A medium went in or came out: the bay sounds it and plays its clip; a medium spilled as it breaks is silent. */
    private void mediaMoved() {
        if (breaking) {
            baySounds.settle(mediaStack());
            return;
        }
        final MediaBaySounds.Move move = baySounds.changed(level, worldPosition, mediaStack());
        if (move != null && move.format() != null) {
            triggerAnim(MediaBay.CONTROLLER, MediaBay.clip(move.format(), move.in()));
        }
    }

    private void tickJob(final ServerLevel level) {
        switch (phase.get()) {
            case IDLE -> {
                if (queue.isEmpty()) {
                    return;
                }
                if (!hasMedia()) {
                    /*
                     * The job waits for a disc rather than failing: the player queued it on purpose and the display
                     * says what is missing.
                     */
                    return;
                }
                final BurnRequest next = queue.peekFirst();
                currentFile.set(next.fileName());
                final MediaFormat format = ((FormattedMediaItem) mediaStack().getItem()).format();
                writeTicks.set(writeTicks(format, next.content().getBytes(StandardCharsets.UTF_8).length));
                enter(Phase.SEEK, seekTicks(format));
            }
            case SEEK -> {
                phaseTicks.add(1);
                if (phaseTicks.get() >= phaseTotal.get()) {
                    enter(Phase.WRITE, writeTicks.get());
                }
            }
            case WRITE -> {
                phaseTicks.add(1);
                if (phaseTicks.get() >= phaseTotal.get()) {
                    finishWriting(level);
                }
            }
            case VERIFY -> {
                phaseTicks.add(1);
                if (phaseTicks.get() >= phaseTotal.get()) {
                    finishVerifying();
                }
            }
            case DONE, ERROR -> {
                phaseTicks.add(1);
                if (phaseTicks.get() >= phaseTotal.get()) {
                    enter(Phase.IDLE, 0);
                }
            }
        }
    }

    private void finishWriting(final ServerLevel level) {
        final BurnRequest job = queue.pollFirst();
        queuePart.changed();
        if (job == null) {
            enter(Phase.IDLE, 0);
            return;
        }
        final String written = write(level, job);
        if (written == null) {
            enter(Phase.ERROR, HOLD_TICKS);
            message.set(WRITE_FAILED.with(hasMedia() ? MEDIUM_FULL : NO_MEDIUM));
        } else {
            currentFile.set(written);
            final MediaFormat format = bayFormat();
            enter(Phase.VERIFY, format == null ? 1 : verifyTicks(format));
        }
    }

    private void finishVerifying() {
        final String path = currentFile.get() + ".craft";
        final Optional<String> back = hasMedia() ? DiskFilesystem.read(mediaStack(), path) : Optional.empty();
        if (back.isPresent()) {
            completed.add(1);
            JscEvents.awardOperator(this, JscEvents.PATTERN_ENCODED);
            enter(Phase.DONE, HOLD_TICKS);
        } else {
            enter(Phase.ERROR, HOLD_TICKS);
            message.set(VERIFY_FAILED.with(path));
        }
    }

    private void enter(final Phase next, final int total) {
        phase.set(next);
        phaseTicks.set(0);
        phaseTotal.set(total);
        if (next != Phase.ERROR) {
            message.set(Text.EMPTY);
        }
    }

    /**
     * Puts the job's file on the medium under a free name and returns the base name it got, or null when the medium
     * refused it. A second file with the same name never overwrites the first; it gets a suffix.
     */
    @Nullable
    private String write(final ServerLevel level, final BurnRequest job) {
        final ItemStack mediaStack = mediaStack();
        if (!(mediaStack.getItem() instanceof FormattedMediaItem)) {
            return null;
        }
        final String path = DiskFilesystem.uniquePath(mediaStack, job.fileName(), ".craft", job.content());
        final long freeWeight = mediaFreeWeight(mediaStack);
        final DiskFilesystem.WriteResult result = DiskFilesystem.write(
                mediaStack, path, FileType.CRAFT, job.content(), freeWeight, FilesystemKind.HIERARCHICAL,
                level.getGameTime());
        if (result != DiskFilesystem.WriteResult.OK) {
            return null;
        }
        media.setStackInSlot(0, mediaStack);
        return path.endsWith(".craft") ? path.substring(0, path.length() - ".craft".length()) : path;
    }

    /** Where a job is. */
    public enum Phase implements IStableId {
        IDLE(0),
        SEEK(1),
        WRITE(2),
        VERIFY(3),
        DONE(4),
        ERROR(5);

        private final int id;

        private static final StableIds<Phase> IDS = StableIds.of(Phase.class);

        Phase(final int id) {
            this.id = id;
        }

        /** The phase that declares {@code id}; an id no phase declares reads as {@link #IDLE}. */
        public static Phase byId(final int id) {
            return IDS.byId(id, IDLE);
        }

        @Override
        public int id() {
            return id;
        }
    }

    /** One file waiting to be burned: its base name (no extension) and its content. */
    public record BurnRequest(String fileName, String content) {
    }

    /** The waiting jobs, saved in order under {@code Queue}; the server keeps them to itself. */
    private final class QueuePart implements IFieldPart {

        @Override
        public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
            final ListTag jobs = new ListTag();
            for (final BurnRequest job : queue) {
                final CompoundTag saved = new CompoundTag();
                saved.putString("Name", job.fileName());
                saved.putString("Content", job.content());
                jobs.add(saved);
            }
            tag.put("Queue", jobs);
        }

        @Override
        public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
            queue.clear();
            final ListTag jobs = tag.getList("Queue", Tag.TAG_COMPOUND);
            for (int i = 0; i < jobs.size() && queue.size() < QUEUE_MAX; i++) {
                final CompoundTag job = jobs.getCompound(i);
                queue.addLast(new BurnRequest(job.getString("Name"), job.getString("Content")));
            }
        }
    }
}
