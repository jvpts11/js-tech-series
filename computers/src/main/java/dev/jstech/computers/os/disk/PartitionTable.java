/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.disk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.os.FileSystemType;
import dev.jstech.core.id.StableCodecs;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

/**
 * The partition table written on a disk (computers/docs/design/storage-devices.md), as the disk item carries it: the
 * table itself ({@link DiskLayout}), what each partition holds, and which partition is open. The open partition is the
 * one the disk's machine started from, and what it holds is kept where a disk with no table keeps it, in the disk's own
 * components, so everything that reads the disk a system runs from reads it as before; what every other partition
 * holds waits here, under its number, until it is opened. {@link DiskPartitions} is how it is read and changed.
 *
 * @param layout the table
 * @param held   what each partition but the open one holds, by number
 * @param open   the number of the open partition, or 0 when none is
 */
public record PartitionTable(DiskLayout layout, List<Held> held, int open) {

    /* The most partitions a table keeps, the GUID Partition Table's, which bounds what a disk may carry. */
    private static final int MOST = PartitionScheme.GPT.mostPartitions();

    private static final Codec<PartitionEntry> ENTRY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("number").forGetter(PartitionEntry::number),
            Codec.LONG.fieldOf("start").forGetter(PartitionEntry::startMb),
            Codec.LONG.fieldOf("size").forGetter(PartitionEntry::sizeMb),
            StableCodecs.byName(FileSystemType.class).optionalFieldOf("fs")
                    .forGetter(entry -> Optional.ofNullable(entry.fs())),
            StableCodecs.byName(PartitionRole.class).optionalFieldOf("role", PartitionRole.DATA)
                    .forGetter(PartitionEntry::role),
            Codec.BOOL.optionalFieldOf("active", false).forGetter(PartitionEntry::active)
    ).apply(inst, (number, start, size, fs, role, active) ->
            new PartitionEntry(number, start, size, fs.orElse(null), role, active)));

    private static final StreamCodec<RegistryFriendlyByteBuf, PartitionEntry> ENTRY_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PartitionEntry::number,
                    ByteBufCodecs.VAR_LONG, PartitionEntry::startMb,
                    ByteBufCodecs.VAR_LONG, PartitionEntry::sizeMb,
                    ByteBufCodecs.STRING_UTF8, entry -> entry.fs() == null ? "" : entry.fs().serializedName(),
                    ByteBufCodecs.STRING_UTF8, entry -> entry.role().serializedName(),
                    ByteBufCodecs.BOOL, PartitionEntry::active,
                    (number, start, size, fs, role, active) -> new PartitionEntry(number, start, size,
                            fs.isEmpty() ? null : FileSystemType.byName(fs), PartitionRole.byName(role), active));

    private static final Codec<DiskLayout> LAYOUT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            StableCodecs.byName(PartitionScheme.class).fieldOf("scheme").forGetter(DiskLayout::scheme),
            Codec.LONG.fieldOf("disk").forGetter(DiskLayout::diskMb),
            ENTRY_CODEC.listOf().optionalFieldOf("partitions", List.of()).forGetter(DiskLayout::partitions)
    ).apply(inst, DiskLayout::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, DiskLayout> LAYOUT_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, layout -> layout.scheme().serializedName(),
                    ByteBufCodecs.VAR_LONG, DiskLayout::diskMb,
                    ENTRY_STREAM_CODEC.apply(ByteBufCodecs.list(MOST)), DiskLayout::partitions,
                    (scheme, disk, partitions) -> new DiskLayout(PartitionScheme.byName(scheme), disk, partitions));

    public static final Codec<PartitionTable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            LAYOUT_CODEC.fieldOf("layout").forGetter(PartitionTable::layout),
            Held.CODEC.listOf().optionalFieldOf("held", List.of()).forGetter(PartitionTable::held),
            Codec.INT.optionalFieldOf("open", 0).forGetter(PartitionTable::open)
    ).apply(inst, PartitionTable::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PartitionTable> STREAM_CODEC = StreamCodec.composite(
            LAYOUT_STREAM_CODEC, PartitionTable::layout,
            Held.STREAM_CODEC.apply(ByteBufCodecs.list(MOST)), PartitionTable::held,
            ByteBufCodecs.VAR_INT, PartitionTable::open,
            PartitionTable::new);

    public PartitionTable {
        held = List.copyOf(held);
    }

    /** What partition {@code number} holds while it is not open; nothing for one that holds nothing yet. */
    public DataComponentPatch heldBy(final int number) {
        for (final Held entry : held) {
            if (entry.number() == number) {
                return entry.data();
            }
        }
        return DataComponentPatch.EMPTY;
    }

    /** The same table with {@code layout}, what every partition gone from it held let go of. */
    public PartitionTable withLayout(final DiskLayout layout) {
        final List<Held> kept = new ArrayList<>();
        for (final Held entry : held) {
            if (layout.find(entry.number()) != null) {
                kept.add(entry);
            }
        }
        return new PartitionTable(layout, kept, layout.find(open) != null ? open : 0);
    }

    /** The same table with partition {@code number} holding {@code data} (nothing for an empty patch). */
    public PartitionTable holding(final int number, final DataComponentPatch data) {
        final List<Held> next = new ArrayList<>();
        for (final Held entry : held) {
            if (entry.number() != number) {
                next.add(entry);
            }
        }
        if (!data.isEmpty()) {
            next.add(new Held(number, data));
        }
        return new PartitionTable(layout, next, open);
    }

    /** The same table with partition {@code number} open, or none for 0. */
    public PartitionTable opened(final int number) {
        return new PartitionTable(layout, held, number);
    }

    /** The open partition, or null when none is. */
    @Nullable
    public PartitionEntry openPartition() {
        return open == 0 ? null : layout.find(open);
    }

    /**
     * What one partition that is not open holds: its own components, as a disk with no table carries them.
     *
     * @param number the partition
     * @param data   what it holds
     */
    public record Held(int number, DataComponentPatch data) {

        static final Codec<Held> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("number").forGetter(Held::number),
                DataComponentPatch.CODEC.fieldOf("data").forGetter(Held::data)
        ).apply(inst, Held::new));

        static final StreamCodec<RegistryFriendlyByteBuf, Held> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Held::number,
                DataComponentPatch.STREAM_CODEC, Held::data,
                Held::new);
    }
}
