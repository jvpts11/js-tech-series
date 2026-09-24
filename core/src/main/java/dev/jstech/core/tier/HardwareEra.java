/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tier;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.Sizes;
import org.jetbrains.annotations.Nullable;

/**
 * Hardware Era: the progression axis for computational hardware (motherboards, CPUs, RAM, storage).
 *
 * <p>This is pure domain logic with no Minecraft dependency, so it stays unit-testable. Each era declares its
 * {@link #level()}, its place in the progression counted from 0 without gaps, and that level is also its stable id:
 * saves, packets and menu data carry it. When an era has to back a block-state property, the binding layer stores
 * the level as an {@code IntegerProperty} and turns the stored value back into an era with {@link #fromLevel(int)};
 * that keeps the Minecraft-aware property type out of this enum. Data files name an era by its
 * {@link #serializedName()}.
 */
@TextHolder
public enum HardwareEra implements IStableId, IStableName {
    VINTAGE(0, "vintage", TextKey.of("jscore.era.vintage", "Vintage")),
    LEGACY(1, "legacy", TextKey.of("jscore.era.legacy", "Legacy")),
    STANDARD(2, "standard", TextKey.of("jscore.era.standard", "Standard")),
    ADVANCED(3, "advanced", TextKey.of("jscore.era.advanced", "Advanced")),
    EXA(4, "exa", TextKey.of("jscore.era.exa", "Exa")),
    SINGULARITY(5, "singularity", TextKey.of("jscore.era.singularity", "Singularity"));

    private final int level;
    private final String serializedName;
    /** The era's name as a player reads it. */
    private final TextKey name;

    private static final StableIds<HardwareEra> IDS = StableIds.of(HardwareEra.class);

    /** An era named as one: "Legacy era", with the name put where each language puts it. */
    private static final TextKey NAMED = TextKey.of("jscore.era.named", "%s era");

    HardwareEra(final int level, final String serializedName, final TextKey name) {
        this.level = level;
        this.serializedName = serializedName;
        this.name = name;
    }

    /** The era's name: "Legacy". */
    public Text text() {
        return this.name.text();
    }

    /** The era as an era: "Legacy era". */
    public Text named() {
        return NAMED.with(this.name);
    }

    /**
     * The colour an era's screens are remembered by, as an RGB int for a tooltip: the green phosphor of a
     * CRT terminal for Vintage, the blue of the Legacy desktop's chrome, the accent blue of the Standard
     * desktop, and a colder cast for each generation past that. A part's era reads at a glance, before
     * the word does, which is what a player sorting a chest of boards and chips needs.
     */
    public int screenColor() {
        return switch (this) {
            case VINTAGE -> 0x33FF33;
            case LEGACY -> 0x245EDC;
            case STANDARD -> 0x0078D4;
            case ADVANCED -> 0x9B59FF;
            case EXA -> 0x00E5FF;
            case SINGULARITY -> 0xF2F2F2;
        };
    }

    /** The word size of the era's processors; what an item costs on the era's disks follows from it. */
    public int bits() {
        return switch (this) {
            case VINTAGE -> 16;
            case LEGACY -> 32;
            default -> 64;
        };
    }

    /**
     * The megabytes one item (or a bucket of fluid, which weighs the same) takes on a disk of this era: a
     * wider word makes a bigger record. Older hardware therefore packs more into the same megabytes, which
     * is how a 20 MB vintage drive holds anything at all, and a 500 GB standard one holds 2 000 items.
     */
    public long mbPerItem() {
        return switch (this) {
            case VINTAGE -> 1L;
            case LEGACY -> 16L;
            default -> 256L;
        };
    }

    /**
     * How many items a size in megabytes costs on this era's disks, rounded up: a system image, say.
     *
     * <p>Rounded up without the addition the obvious form uses, because that addition is itself an amount
     * that can run past the end: a size near the largest number there is would round up to a negative one,
     * and something enormous would then cost nothing to store.
     */
    public long itemsFor(final long mb) {
        return Sizes.ceilDiv(mb, mbPerItem());
    }

    /**
     * The bytes one thousandth of an item weighs on this era's disks, where an item weighs 1 000 of those
     * units, the same unit a millibucket of fluid weighs one of. A file of N bytes costs
     * {@code ceil(N / bytesPerMbEq())} of them.
     */
    public long bytesPerMbEq() {
        return mbPerItem() * 1024L * 1024L / 1000L;
    }

    public HardwareEra next() {
        return this == SINGULARITY ? SINGULARITY : fromLevel(level + 1);
    }

    public HardwareEra prev() {
        return this == VINTAGE ? VINTAGE : fromLevel(level - 1);
    }

    public int level() {
        return level;
    }

    @Override
    public int id() {
        return level;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    /**
     * The era whose {@link #level()} equals {@code level}. This is the inverse of {@link #level()}, used
     * by the binding layer to turn a block-state {@code IntegerProperty} value back into an era.
     *
     * @throws IllegalArgumentException if no era has that level
     */
    public static HardwareEra fromLevel(final int level) {
        final HardwareEra era = IDS.find(level);
        if (era == null) {
            throw new IllegalArgumentException("no hardware era at level " + level);
        }
        return era;
    }

    /** The era a save or a packet names by {@code id}, or null for an id no era has (the -1 of "no era"). */
    @Nullable
    public static HardwareEra find(final int id) {
        return IDS.find(id);
    }

    public boolean isAtLeast(HardwareEra other) {
        return this.level() >= other.level();
    }

    public boolean isAtMost(HardwareEra other) {
        return this.level() <= other.level();
    }
}
