/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import dev.jstech.core.util.BlockDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * The fields of one {@link SyncedBlockEntity}: declared once, each with where it goes, and from then on saved,
 * loaded, sent to the players who see the block and shown to its menu by this class, never by hand.
 *
 * <p>Fields are declared as the block entity is built, in its field initialisers. The first save, load, update or
 * menu closes the declarations, since a menu's data is laid out from them and must never shift under it.
 */
public final class BlockEntityFields {

    private final SyncedBlockEntity owner;
    private final List<IField> fields = new ArrayList<>();
    private final Set<String> keys = new HashSet<>();
    private final List<IField> polled = new ArrayList<>();
    private boolean closed;
    private @Nullable MenuData menuData;

    BlockEntityFields(final SyncedBlockEntity owner) {
        this.owner = owner;
    }

    /** Declares an int, starting at {@code initial}. */
    public IntField integer(final String key, final int initial) {
        return add(new IntField(flags(key), initial));
    }

    /** Declares a true-or-false, starting at {@code initial}. */
    public BoolField flag(final String key, final boolean initial) {
        return add(new BoolField(flags(key), initial));
    }

    /** Declares a long, starting at {@code initial}. */
    public LongField longInteger(final String key, final long initial) {
        return add(new LongField(flags(key), initial));
    }

    /** Declares a value the server works out from the block entity's state rather than holds. */
    public DerivedInt derived(final String key, final IntSupplier value) {
        return add(new DerivedInt(flags(key), this, value));
    }

    /** Declares a yes-or-no the server works out, carried as 1 or 0. */
    public DerivedInt derived(final String key, final BooleanSupplier value) {
        return derived(key, () -> value.getAsBoolean() ? 1 : 0);
    }

    /** Declares an inventory of {@code slots} slots. */
    public FieldItemHandler items(final String key, final int slots) {
        return add(new FieldItemHandler(flags(key), slots));
    }

    /** Declares an FE store taking at most {@code maxReceive} and giving at most {@code maxExtract} FE a transfer. */
    public FieldEnergyStorage energy(final String key, final int capacity, final int maxReceive,
                                     final int maxExtract) {
        return add(new FieldEnergyStorage(flags(key), capacity, maxReceive, maxExtract));
    }

    /**
     * The data a menu open on the block carries: every field declared {@code toMenu()}, in the order declared, an
     * int each (a long takes two). On the server it reads the fields; on the client the values the menu receives
     * are written into the fields themselves, so the screen reads the block entity's own accessors on both sides.
     */
    public ContainerData menuData() {
        if (menuData == null) {
            close();
            menuData = new MenuData(fields);
        }
        return menuData;
    }

    /**
     * What the block offers for {@code capability} on any side: the first inventory or energy store declared
     * {@code exposed()}, or null when it offers none.
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T capability(final BlockCapability<T, ?> capability) {
        for (final IField field : fields) {
            if (capability == Capabilities.ItemHandler.BLOCK
                    && field instanceof FieldItemHandler items && items.isExposed()) {
                return (T) items;
            }
            if (capability == Capabilities.EnergyStorage.BLOCK
                    && field instanceof FieldEnergyStorage energy && energy.isExposed()) {
                return (T) energy;
            }
        }
        return null;
    }

    /** Spills every inventory declared {@code dropsWhenBroken()} on the ground at {@code pos}. */
    public void spill(final Level level, final BlockPos pos) {
        for (final IField field : fields) {
            if (field instanceof FieldItemHandler items && items.dropsOnBreak()) {
                BlockDrops.spill(level, pos, items);
            }
        }
    }

    /** Sends the fields declared {@code toClient()} to the players who see the block, at the end of this tick. */
    public void syncToClients() {
        if (owner.getLevel() instanceof ServerLevel level) {
            ClientUpdates.dirty(level, owner);
        }
    }

    void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        close();
        for (final IField field : fields) {
            if (field.flags().saved()) {
                field.write(tag, registries);
            }
        }
    }

    void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        close();
        for (final IField field : fields) {
            if (field.flags().saved() && tag.contains(field.flags().key())) {
                field.read(tag, registries);
            }
        }
    }

    CompoundTag writeClient(final HolderLookup.Provider registries) {
        close();
        final CompoundTag tag = new CompoundTag();
        for (final IField field : fields) {
            if (field.flags().client()) {
                field.write(tag, registries);
            }
        }
        return tag;
    }

    void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        close();
        for (final IField field : fields) {
            if (field.flags().client() && tag.contains(field.flags().key())) {
                field.read(tag, registries);
            }
        }
    }

    /** Whether the server has to check this block entity each tick for worked-out values that changed. */
    boolean polls() {
        close();
        return !polled.isEmpty();
    }

    /** Schedules the players' update when a worked-out value they see has changed. */
    void poll() {
        for (final IField field : polled) {
            if (field.pollChanged()) {
                syncToClients();
                return;
            }
        }
    }

    @Nullable Level level() {
        return owner.getLevel();
    }

    void changed(final FieldFlags flags) {
        if (flags.saved()) {
            owner.setChanged();
        }
        if (flags.client()) {
            syncToClients();
        }
    }

    void checkOpen(final String key) {
        if (closed) {
            throw new IllegalStateException("field '" + key + "' is declared after the fields were first used");
        }
    }

    private FieldFlags flags(final String key) {
        checkOpen(key);
        if (!keys.add(key)) {
            throw new IllegalStateException("field '" + key + "' is declared twice");
        }
        return new FieldFlags(this, key);
    }

    private <F extends IField> F add(final F field) {
        fields.add(field);
        return field;
    }

    private void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (final IField field : fields) {
            if (field instanceof DerivedInt && field.flags().client()) {
                polled.add(field);
            }
        }
    }

    /** A menu's view of the fields declared {@code toMenu()}, laid out once in declaration order. */
    private final class MenuData implements ContainerData {

        private final IField[] owners;
        private final int[] parts;

        MenuData(final List<IField> declared) {
            final List<IField> slotOwners = new ArrayList<>();
            final List<Integer> slotParts = new ArrayList<>();
            for (final IField field : declared) {
                if (!field.flags().menu()) {
                    continue;
                }
                for (int part = 0; part < field.menuSlots(); part++) {
                    slotOwners.add(field);
                    slotParts.add(part);
                }
            }
            owners = slotOwners.toArray(IField[]::new);
            parts = slotParts.stream().mapToInt(Integer::intValue).toArray();
        }

        @Override
        public int get(final int index) {
            return index >= 0 && index < owners.length ? owners[index].menuGet(parts[index]) : 0;
        }

        @Override
        public void set(final int index, final int value) {
            if (index >= 0 && index < owners.length) {
                owners[index].menuSet(parts[index], value);
            }
        }

        @Override
        public int getCount() {
            return owners.length;
        }
    }
}
