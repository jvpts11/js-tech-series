/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * The variants of one machine, by era or by tier, declared once as one family, lowest first: one block each, all
 * making the same block entity, which reads from its block which variant it is. A machine of the family grows into the
 * next in its place, its contents and settings kept ({@link MachineGrowth}), as an installer item does it.
 *
 * <pre>{@code
 * public static final MachineFamily CRUSHERS = MachineFamily.of(id("crusher"),
 *         List.of(BASIC_CRUSHER, ADVANCED_CRUSHER, ELITE_CRUSHER));
 * public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER =
 *         CONTENT.blockEntity("crusher", CrusherBlockEntity::new, BASIC_CRUSHER, ADVANCED_CRUSHER, ELITE_CRUSHER);
 * }</pre>
 */
public final class MachineFamily {

    private final ResourceLocation id;
    private final List<Supplier<? extends Block>> tiers;

    /* Declared while the mods' classes load, read once the game runs: a list safe to read while it was written. */
    private static final List<MachineFamily> FAMILIES = new CopyOnWriteArrayList<>();

    private MachineFamily(final ResourceLocation id, final List<Supplier<? extends Block>> tiers) {
        this.id = id;
        this.tiers = List.copyOf(tiers);
    }

    /** Declares the family named {@code id} of {@code tiers}, the lowest first; at least two. */
    public static MachineFamily of(final ResourceLocation id, final List<? extends Supplier<? extends Block>> tiers) {
        Objects.requireNonNull(id, "id");
        if (tiers.size() < 2) {
            throw new IllegalArgumentException("the family " + id + " needs two tiers at least to grow from one to "
                    + "the next");
        }
        final MachineFamily family = new MachineFamily(id, List.copyOf(tiers));
        FAMILIES.add(family);
        return family;
    }

    /** The family {@code block} is a tier of, if any. */
    public static Optional<MachineFamily> of(final Block block) {
        for (final MachineFamily family : FAMILIES) {
            if (family.tierOf(block) >= 0) {
                return Optional.of(family);
            }
        }
        return Optional.empty();
    }

    public ResourceLocation id() {
        return this.id;
    }

    /** How many tiers it has. */
    public int size() {
        return this.tiers.size();
    }

    /** Its tier {@code index}, the lowest 0. */
    public Block tier(final int index) {
        return this.tiers.get(index).get();
    }

    /** Which of its tiers {@code block} is; -1 for none. */
    public int tierOf(final Block block) {
        for (int i = 0; i < this.tiers.size(); i++) {
            if (this.tiers.get(i).get() == block) {
                return i;
            }
        }
        return -1;
    }

    /** The tier after {@code block}'s; none for its highest, or for a block not of the family. */
    public Optional<Block> next(final Block block) {
        final int at = tierOf(block);
        return at < 0 || at + 1 >= this.tiers.size() ? Optional.empty() : Optional.of(tier(at + 1));
    }
}
