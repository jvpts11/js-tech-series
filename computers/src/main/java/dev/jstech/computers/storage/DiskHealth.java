/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What corruption did to a disk, carried on the item beside its usage summary so the hand, This PC and the client
 * read it without the volume store (computers/docs/design/storage.md): how much it holds is corrupted and waiting for
 * the Defrag, how much of it is lost to bad sectors, and whether it died. Absent on a disk nothing happened to.
 *
 * @param corruptedWeight what the corrupted items weigh, which they still take of the disk
 * @param corruptedItems  how many corrupted things wait for the Defrag: items, and system files
 * @param damage          how much of what the disk stores is corrupted, in permille
 * @param badSectors      how much of its capacity is lost to bad sectors, in permille
 * @param dead            whether it died of its corruption
 */
public record DiskHealth(long corruptedWeight, long corruptedItems, int damage, int badSectors, boolean dead) {

    public static final DiskHealth SOUND = new DiskHealth(0L, 0L, 0, 0, false);

    public static final Codec<DiskHealth> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.LONG.optionalFieldOf("corrupted_weight", 0L).forGetter(DiskHealth::corruptedWeight),
            Codec.LONG.optionalFieldOf("corrupted_items", 0L).forGetter(DiskHealth::corruptedItems),
            Codec.INT.optionalFieldOf("damage", 0).forGetter(DiskHealth::damage),
            Codec.INT.optionalFieldOf("bad_sectors", 0).forGetter(DiskHealth::badSectors),
            Codec.BOOL.optionalFieldOf("dead", false).forGetter(DiskHealth::dead)
    ).apply(builder, DiskHealth::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiskHealth> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, DiskHealth::corruptedWeight,
            ByteBufCodecs.VAR_LONG, DiskHealth::corruptedItems,
            ByteBufCodecs.VAR_INT, DiskHealth::damage,
            ByteBufCodecs.VAR_INT, DiskHealth::badSectors,
            ByteBufCodecs.BOOL, DiskHealth::dead,
            DiskHealth::new);

    public DiskHealth {
        corruptedWeight = Math.max(0L, corruptedWeight);
        corruptedItems = Math.max(0L, corruptedItems);
        damage = Math.max(0, Math.min(Corruption.WHOLE, damage));
        badSectors = Math.max(0, Math.min(Corruption.WHOLE, badSectors));
    }

    /** Read off the volume: what is corrupted, the damage, the bad sectors and a death. */
    public static DiskHealth of(final StorageVolume volume) {
        final long items = (volume.corruptedWeight() + StorageKey.MB_EQ_PER_ITEM - 1) / StorageKey.MB_EQ_PER_ITEM;
        return new DiskHealth(volume.corruptedWeight(), items + volume.corruptedFiles().files().size(),
                volume.damage(), volume.badSectors(), volume.dead());
    }

    /** Whether nothing happened to the disk. */
    public boolean sound() {
        return corruptedItems == 0L && badSectors == 0 && !dead;
    }

    /** What a disk of {@code capacity} still holds: nothing once dead, less what bad sectors took otherwise. */
    public long usable(final long capacity) {
        return dead ? 0L : Corruption.usable(capacity, badSectors);
    }
}
