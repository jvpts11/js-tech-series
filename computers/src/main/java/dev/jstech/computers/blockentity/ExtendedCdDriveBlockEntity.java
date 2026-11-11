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
import dev.jstech.computers.block.ExtendedCdDriveBlock;
import dev.jstech.computers.machine.DiscCopies;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * An Extended CD Drive at work (computers/docs/design/peripherals.md): it reads the disc in its reading tray once, then
 * writes it onto every blank in its seven writing trays at once, each at its own speed, copying what Easy Disc Creator
 * copies: never the network's storage. The Legacy's reads and writes CDs, the Transition's DVDs and CDs, the Advanced's
 * Blu-ray discs, DVDs and CDs.
 */
public class ExtendedCdDriveBlockEntity extends SyncedBlockEntity {

    private final FieldItemHandler read = fields().items("Read", 1).save().toClient().dropsWhenBroken()
            .slotLimit(1).accepts((index, stack) -> readable(stack));
    private final FieldItemHandler write = fields().items("Write", TRAYS).save().toClient().dropsWhenBroken()
            .slotLimit(1).accepts((index, stack) -> writable(stack));
    private final ValueField<Work> work = fields().value("Work", Work.CODEC, Work.IDLE).save().toClient();
    private final IntField left = fields().integer("Left", 0).save().toClient();
    private final IntField total = fields().integer("Total", 0).save().toClient();
    /** The trays this copy writes to, and those it has written, a bit each. */
    private final IntField writing = fields().integer("Writing", 0).save().toClient();
    private final IntField written = fields().integer("Written", 0).save().toClient();
    private final IntField burned = fields().integer("Burned", 0).save().toClient();
    private final ValueField<Problem> problem = fields().value("Problem", Problem.CODEC, Problem.NONE).toClient();
    /** The disc read, kept until every copy of it is written. */
    private final ValueField<ItemStack> image = fields().value("Image", ItemStack.OPTIONAL_CODEC, ItemStack.EMPTY)
            .save();
    private final ValueField<Long> bytes = fields().value("Bytes", Codec.LONG, 0L).save().toClient();

    /** How many writing trays a tower has. */
    public static final int TRAYS = 7;

    public ExtendedCdDriveBlockEntity(final BlockPos pos, final BlockState state) {
        super(MediaDevices.EXTENDED_CD_DRIVE_BE.get(), pos, state);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final ExtendedCdDriveBlockEntity tower) {
        if (level instanceof ServerLevel) {
            tower.tickWork();
        }
    }

    /** The tower's era. */
    public Era era() {
        return getBlockState().getBlock() instanceof ExtendedCdDriveBlock block ? block.era() : HardwareEra.LEGACY;
    }

    /** The formats the tower of {@code era} reads and writes: CDs, with DVDs from the Transition, Blu-ray after. */
    public static Set<MediaFormat> formats(final Era era) {
        final Era parts = HardwareEra.withParts(era);
        if (parts == HardwareEra.ADVANCED) {
            return EnumSet.of(MediaFormat.CD, MediaFormat.DVD, MediaFormat.BLU_RAY);
        }
        return parts == HardwareEra.TRANSITION ? EnumSet.of(MediaFormat.CD, MediaFormat.DVD)
                : EnumSet.of(MediaFormat.CD);
    }

    /** Starts copying the disc in the reading tray onto every blank that takes it; nothing when it cannot. */
    public void copy() {
        if (work.get() != Work.IDLE) {
            return;
        }
        final ItemStack source = read.getStackInSlot(0);
        final MediaFormat format = formatOf(source);
        if (source.isEmpty() || format == null || DiscCopies.blank(source)) {
            problem.set(Problem.NOTHING_TO_READ);
            return;
        }
        if (MediaItem.data(source).total() > 0) {
            problem.set(Problem.STORAGE);
            return;
        }
        int trays = 0;
        for (int tray = 0; tray < TRAYS; tray++) {
            final ItemStack blank = write.getStackInSlot(tray);
            if (formatOf(blank) != null && DiscCopies.blank(blank)
                    && DiscCopies.capacityOf(blank) >= DiscCopies.capacityOf(source)) {
                trays |= 1 << tray;
            }
        }
        if (trays == 0) {
            problem.set(Problem.NO_BLANKS);
            return;
        }
        problem.set(Problem.NONE);
        writing.set(trays);
        written.set(0);
        bytes.set(DiscCopies.bytesOf(source));
        work.set(Work.READING);
        total.set(DiscCopies.ticksFor(format, bytes.get()));
        left.set(total.get());
    }

    /** Gives the disc in the reading tray to {@code player}; nothing while it copies. */
    public void eject(final Player player) {
        if (work.get() == Work.IDLE && !read.getStackInSlot(0).isEmpty()) {
            player.getInventory().placeItemBackInInventory(read.extractItem(0, 1, false));
        }
    }

    public ItemStackHandler readTray() {
        return read;
    }

    public ItemStackHandler writeTrays() {
        return write;
    }

    public Work work() {
        return work.get();
    }

    /** How far through its step it is, from 0 to 1. */
    public float done() {
        return total.get() <= 0 ? 0F : 1F - left.get() / (float) total.get();
    }

    /** Whether tray {@code tray} is written to by the copy under way, and whether it is done. */
    public boolean writing(final int tray) {
        return (writing.get() & 1 << tray) != 0;
    }

    public boolean written(final int tray) {
        return (written.get() & 1 << tray) != 0;
    }

    /** How many copies the copy under way makes. */
    public int copies() {
        return Integer.bitCount(writing.get());
    }

    /** How many discs it has burned, all told. */
    public int burned() {
        return burned.get();
    }

    /** How many bytes the disc read holds. */
    public long bytes() {
        return bytes.get();
    }

    public Problem problem() {
        return problem.get();
    }

    /* A step at a time: the disc read, then every copy written at the slowest blank's speed, then done. */
    private void tickWork() {
        if (work.get() == Work.IDLE) {
            return;
        }
        left.set(left.get() - 1);
        if (left.get() > 0) {
            return;
        }
        if (work.get() == Work.READING) {
            image.set(read.getStackInSlot(0).copy());
            int slowest = 1;
            for (int tray = 0; tray < TRAYS; tray++) {
                if (writing(tray)) {
                    slowest = Math.max(slowest, DiscCopies.ticksFor(formatOf(write.getStackInSlot(tray)),
                            bytes.get()));
                }
            }
            work.set(Work.WRITING);
            total.set(slowest);
            left.set(slowest);
            return;
        }
        int done = 0;
        for (int tray = 0; tray < TRAYS; tray++) {
            final ItemStack blank = write.getStackInSlot(tray);
            if (writing(tray) && DiscCopies.blank(blank)) {
                final ItemStack copy = blank.copy();
                DiscCopies.copyOnto(image.get(), copy);
                write.setStackInSlot(tray, copy);
                done |= 1 << tray;
            }
        }
        written.set(done);
        burned.set(burned.get() + Integer.bitCount(done));
        image.set(ItemStack.EMPTY);
        work.set(Work.IDLE);
    }

    /* Whether the tower reads {@code stack}: a disc of a format it takes. */
    private boolean readable(final ItemStack stack) {
        final MediaFormat format = formatOf(stack);
        return format != null && formats(era()).contains(format);
    }

    /* Whether the tower writes on {@code stack}: a writable disc of a format it takes. */
    private boolean writable(final ItemStack stack) {
        return readable(stack) && stack.getItem() instanceof FormattedMediaItem item && item.writable();
    }

    @Nullable
    private static MediaFormat formatOf(final ItemStack stack) {
        return stack.getItem() instanceof FormattedMediaItem item ? item.format() : null;
    }

    /** What the tower is doing. */
    public enum Work implements IStableId {
        IDLE(0),
        READING(1),
        WRITING(2);

        private final int id;

        private static final StableIds<Work> IDS = StableIds.of(Work.class);
        private static final Codec<Work> CODEC = Codec.BYTE.xmap(b -> IDS.byId(b, IDLE), w -> (byte) w.id());

        Work(final int id) {
            this.id = id;
        }

        @Override
        public int id() {
            return id;
        }
    }

    /** Why the last copy did not start. */
    public enum Problem implements IStableId {
        NONE(0),
        NOTHING_TO_READ(1),
        STORAGE(2),
        NO_BLANKS(3);

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
    }
}
