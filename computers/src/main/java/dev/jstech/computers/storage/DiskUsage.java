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
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A drive's usage summary, carried on the item so the hand, the creative tab and the client can read it
 * without the volume store: the stored weight, and what that weight is made of (items by the piece, fluids
 * and chemicals by the millibucket) so a drive full of water never reads as "22 944 items".
 */
@TextHolder
public record DiskUsage(long usedWeight, long items, long fluidMb, long chemicalMb, int types) {

    public static final DiskUsage EMPTY = new DiskUsage(0L, 0L, 0L, 0L, 0);

    private static final TextKey HOLDS = TextKey.of("jsc.storage.disk_usage.holds", "Holds %s, or %s mB of fluid");
    private static final TextKey HOLDS_OR_CHEMICAL = TextKey.of("jsc.storage.disk_usage.holds_or_chemical",
            "Holds %s, or %s mB of fluid or chemical");
    private static final TextKey ITEMS_ONE = TextKey.of("jsc.storage.disk_usage.items_one", "%s item");
    private static final TextKey ITEMS_MANY = TextKey.of("jsc.storage.disk_usage.items_many", "%s items");
    private static final TextKey FLUID = TextKey.of("jsc.storage.disk_usage.fluid", "%s mB of fluid");
    private static final TextKey CHEMICAL = TextKey.of("jsc.storage.disk_usage.chemical", "%s mB of chemical");
    private static final TextKey LIST = TextKey.of("jsc.storage.disk_usage.list", "%s, %s");
    private static final TextKey TYPES_ONE = TextKey.of("jsc.storage.disk_usage.types_one", "%s type");
    private static final TextKey TYPES_MANY = TextKey.of("jsc.storage.disk_usage.types_many", "%s types");
    private static final TextKey USED_SHARE =
            TextKey.of("jsc.storage.disk_usage.used_share", "Used %s%%: %s across %s");
    private static final TextKey USED = TextKey.of("jsc.storage.disk_usage.used", "Used: %s across %s");

    public static final Codec<DiskUsage> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.LONG.fieldOf("used_weight").forGetter(DiskUsage::usedWeight),
            Codec.LONG.fieldOf("items").forGetter(DiskUsage::items),
            Codec.LONG.fieldOf("fluid_mb").forGetter(DiskUsage::fluidMb),
            Codec.LONG.fieldOf("chemical_mb").forGetter(DiskUsage::chemicalMb),
            Codec.INT.fieldOf("types").forGetter(DiskUsage::types)
    ).apply(builder, DiskUsage::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiskUsage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, DiskUsage::usedWeight,
            ByteBufCodecs.VAR_LONG, DiskUsage::items,
            ByteBufCodecs.VAR_LONG, DiskUsage::fluidMb,
            ByteBufCodecs.VAR_LONG, DiskUsage::chemicalMb,
            ByteBufCodecs.VAR_INT, DiskUsage::types,
            DiskUsage::new);

    /** Read off the volume's running totals: a summary is rewritten on every write and must stay O(1). */
    public static DiskUsage of(final StorageVolume volume) {
        return new DiskUsage(volume.usedWeight(), volume.itemUnits(), volume.fluidUnits(), volume.chemicalUnits(),
                volume.types());
    }

    public boolean isEmpty() {
        return usedWeight <= 0L && types == 0;
    }

    /**
     * What a drive of {@code capacityItems} can hold, in both units its data comes in. Chemicals are named
     * only while a chemical mod is present: without one there is no such data to hold.
     */
    public static Text capacityLine(final long capacityItems) {
        return (ChemicalBridges.anyRegistered() ? HOLDS_OR_CHEMICAL : HOLDS).with(itemCount(capacityItems),
                group(capacityItems * StorageKey.MB_EQ_PER_ITEM));
    }

    /**
     * What is on the drive and how much of a drive of {@code capacityItems} that takes, e.g.
     * {@code "Used 28%: 12 items, 22,944 mB of fluid across 2 types"}.
     */
    public Text summary(final long capacityItems) {
        final List<Text> parts = new ArrayList<>(3);
        if (items > 0L) {
            parts.add(itemCount(items));
        }
        if (fluidMb > 0L) {
            parts.add(FLUID.with(group(fluidMb)));
        }
        if (chemicalMb > 0L) {
            parts.add(CHEMICAL.with(group(chemicalMb)));
        }
        Text held = parts.isEmpty() ? Text.EMPTY : parts.getFirst();
        for (int i = 1; i < parts.size(); i++) {
            held = LIST.with(held, parts.get(i));
        }
        final Text kinds = (types == 1 ? TYPES_ONE : TYPES_MANY).with(types);
        final long capacityWeight = capacityItems * StorageKey.MB_EQ_PER_ITEM;
        return capacityWeight > 0L
                ? USED_SHARE.with(Math.min(100L, usedWeight * 100L / capacityWeight), held, kinds)
                : USED.with(held, kinds);
    }

    private static Text itemCount(final long count) {
        return (count == 1L ? ITEMS_ONE : ITEMS_MANY).with(group(count));
    }

    private static String group(final long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }
}
