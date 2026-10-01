/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import com.mojang.serialization.Codec;
import dev.jstech.core.persistence.SaveLayout;
import dev.jstech.core.util.BlockDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * The fields of one {@link SyncedBlockEntity}: declared once, each with where it goes, and from then on saved,
 * loaded, sent to the players who see the block and shown to its menu by this class, never by hand.
 *
 * <p>Fields are declared as the block entity is built, in its field initialisers. The first save, load, update or
 * menu closes the declarations, since a menu's data is laid out from them and must never shift under it.
 *
 * <p>Every save carries the version of the block entity's layout, under {@value SaveLayout#VERSION_KEY}: the first,
 * unless the block entity declares a {@link #layout} of its own with steps from older ones. A save from before block
 * entities carried a version reads as version 0.
 */
public final class BlockEntityFields {

    private final SyncedBlockEntity owner;
    private final List<IField> fields = new ArrayList<>();
    private final Set<String> keys = new HashSet<>();
    private final List<IField> polled = new ArrayList<>();
    private final List<StateMirror<?>> mirrors = new ArrayList<>();
    private final List<BiConsumer<ServerLevel, BlockPos>> brokenListeners = new ArrayList<>();
    private boolean closed;
    private @Nullable SaveLayout layout;
    private @Nullable MenuData menuData;
    /* What the block offers to pipes and cables, found once when the declarations close: asked on every lookup. */
    private @Nullable FieldItemHandler exposedItems;
    private @Nullable FieldEnergyStorage exposedEnergy;
    private @Nullable FieldFluidTank exposedFluid;

    /** The first layout of each kind of block entity that declares none, made once a kind, named by its id. */
    private static final Map<BlockEntityType<?>, SaveLayout> FIRST_LAYOUTS = new ConcurrentHashMap<>();

    BlockEntityFields(final SyncedBlockEntity owner) {
        this.owner = owner;
    }

    /**
     * Declares the layout the block entity is saved in, when it is past its first: its version, and the steps that
     * bring a save of an older version up to it. Declared once, in a field initialiser, from a constant shared by
     * every block entity of the kind.
     */
    public void layout(final SaveLayout declared) {
        checkOpen(SaveLayout.VERSION_KEY);
        if (this.layout != null) {
            throw new IllegalStateException("the layout is declared twice");
        }
        this.layout = Objects.requireNonNull(declared, "declared");
    }

    /** The layout the block entity is saved in. */
    public SaveLayout layout() {
        final SaveLayout declared = this.layout;
        return declared != null ? declared : FIRST_LAYOUTS.computeIfAbsent(owner.getType(),
                type -> SaveLayout.of(String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type))));
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

    /** Declares a fluid tank of {@code capacity} millibuckets. */
    public FieldFluidTank fluid(final String key, final int capacity) {
        return add(new FieldFluidTank(flags(key), capacity));
    }

    /** Declares a value {@code codec} writes, which always holds one, starting at {@code initial}. */
    public <T> ValueField<T> value(final String key, final Codec<T> codec, final T initial) {
        return add(new ValueField<>(flags(key), codec, initial, false));
    }

    /** Declares a value {@code codec} writes, which may hold nothing, and starts with nothing. */
    public <T> ValueField<T> nullable(final String key, final Codec<T> codec) {
        return add(new ValueField<>(flags(key), codec, null, true));
    }

    /** Declares a part that writes itself under keys of its own; {@code name} only tells it from the others. */
    public PartField part(final String name, final IFieldPart part) {
        return add(new PartField(flags(name), part));
    }

    /**
     * Keeps a property of the block's state at what {@code value} says: whether a drive holds a medium, whether a
     * screen is lit. It is checked each tick and set, for the players to see, when it differs.
     */
    public <T extends Comparable<T>> void mirror(final Property<T> property, final Supplier<T> value) {
        checkOpen(property.getName());
        mirrors.add(new StateMirror<>(property, value));
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
        close();
        if (capability == Capabilities.ItemHandler.BLOCK) {
            return (T) exposedItems;
        }
        if (capability == Capabilities.EnergyStorage.BLOCK) {
            return (T) exposedEnergy;
        }
        return capability == Capabilities.FluidHandler.BLOCK ? (T) exposedFluid : null;
    }

    /**
     * Runs {@code listener} on the server when the block is broken or replaced, before its inventories spill: a
     * peripheral frees its place on its owner, a drive keeps the sound of a medium coming out for a real eject.
     */
    public void whenBroken(final BiConsumer<ServerLevel, BlockPos> listener) {
        brokenListeners.add(listener);
    }

    /**
     * The block at {@code pos} was broken or replaced: what was declared to happen then happens, and every inventory
     * declared {@code dropsWhenBroken()} spills on the ground.
     */
    public void broken(final Level level, final BlockPos pos) {
        if (level instanceof ServerLevel server) {
            for (final BiConsumer<ServerLevel, BlockPos> listener : brokenListeners) {
                listener.accept(server, pos);
            }
        }
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
        layout().stamp(tag);
    }

    void load(final CompoundTag saved, final HolderLookup.Provider registries) {
        close();
        final CompoundTag tag = layout().read(saved);
        for (final IField field : fields) {
            if (!field.flags().saved()) {
                continue;
            }
            if (!field.keyed() || tag.contains(field.flags().key())) {
                field.read(tag, registries);
            } else {
                field.absent();
            }
        }
    }

    CompoundTag writeClient(final HolderLookup.Provider registries) {
        close();
        final CompoundTag tag = new CompoundTag();
        for (final IField field : fields) {
            if (field.flags().client()) {
                field.writeClient(tag, registries);
            }
        }
        return tag;
    }

    void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        close();
        for (final IField field : fields) {
            if (!field.flags().client()) {
                continue;
            }
            if (!field.keyed() || tag.contains(field.flags().key())) {
                field.readClient(tag, registries);
            } else {
                field.absent();
            }
        }
    }

    /** Whether the server has to check this block entity each tick for what the players see changing. */
    boolean polls() {
        close();
        return !polled.isEmpty() || !mirrors.isEmpty();
    }

    /**
     * Schedules the players' update when a worked-out value they see, or a mirrored property, has changed. Every
     * worked-out value is looked at, so each one that moved is counted as sent with the one update.
     */
    void poll() {
        boolean changed = false;
        for (final IField field : polled) {
            changed |= field.pollChanged();
        }
        if (changed || (!mirrors.isEmpty() && mirrored(owner.getBlockState()) != owner.getBlockState())) {
            syncToClients();
        }
    }

    /** {@code state} with every mirrored property at what its value says. */
    BlockState mirrored(final BlockState state) {
        BlockState wanted = state;
        for (final StateMirror<?> mirror : mirrors) {
            wanted = mirror.apply(wanted);
        }
        return wanted;
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
        if (SaveLayout.VERSION_KEY.equals(key)) {
            throw new IllegalStateException("'" + key + "' is where the save keeps its version, not a field");
        }
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
            if (exposedItems == null && field instanceof FieldItemHandler items && items.isExposed()) {
                exposedItems = items;
            }
            if (exposedEnergy == null && field instanceof FieldEnergyStorage energy && energy.isExposed()) {
                exposedEnergy = energy;
            }
            if (exposedFluid == null && field instanceof FieldFluidTank tank && tank.isExposed()) {
                exposedFluid = tank;
            }
        }
    }

    /**
     * A property of the block's state kept at what a value says.
     *
     * @param <T> the property's values
     */
    private record StateMirror<T extends Comparable<T>>(Property<T> property, Supplier<T> value) {

        BlockState apply(final BlockState state) {
            if (!state.hasProperty(property)) {
                return state;
            }
            final T wanted = value.get();
            return state.getValue(property).equals(wanted) ? state : state.setValue(property, wanted);
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
