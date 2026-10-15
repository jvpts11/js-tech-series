/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.inventory.ItemFilters;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What each face of a machine does with each of its resources, as its Sides tab sets it: takes in, gives out, both or
 * nothing, the face named from the machine's own point of view so turning it keeps what each face does. And, for each
 * resource, whether the machine pushes what it makes into the block beside its giving faces and pulls what it works
 * from the block beside its taking faces, by itself, at what rate and through what filter.
 */
public final class MachineSides implements IFieldPart {

    private final Supplier<Set<MachineResource>> declared;
    private @Nullable Set<MachineResource> resources;
    private final Map<MachineResource, Map<RelativeFace, FaceMode>> modes = new EnumMap<>(MachineResource.class);
    private final Map<MachineResource, Automation> automation = new EnumMap<>(MachineResource.class);
    private final Runnable changed;

    private static final String KEY = "Sides";
    private static final String FACES = "faces";
    private static final String AUTOMATIC = "automatic";
    private static final Codec<Automation> AUTOMATION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("push", false).forGetter(Automation::push),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("push_rate", Automation.RATE)
                    .forGetter(Automation::pushRate),
            Codec.BOOL.optionalFieldOf("pull", false).forGetter(Automation::pull),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("pull_rate", Automation.RATE)
                    .forGetter(Automation::pullRate),
            ItemFilters.CODEC.optionalFieldOf("filter", ItemFilter.EVERYTHING).forGetter(Automation::filter)
    ).apply(instance, Automation::new));
    private static final Codec<Map<RelativeFace, FaceMode>> FACES_CODEC = Codec.unboundedMap(
            StableCodecs.byName(RelativeFace.class), StableCodecs.byName(FaceMode.class));

    /**
     * The sides of a machine with the resources {@code declared} gives, asked once its machine is built, every face
     * taking in and giving out each of them until it is set, which tells {@code changed} when anything is set.
     */
    public MachineSides(final Supplier<Set<MachineResource>> declared, final Runnable changed) {
        this.declared = Objects.requireNonNull(declared, "declared");
        this.changed = Objects.requireNonNull(changed, "changed");
    }

    /** The resources the machine has, which its Sides tab shows. */
    public List<MachineResource> resources() {
        return MachineResource.round().stream().filter(this::has).toList();
    }

    /** Whether the machine has {@code resource} at all. */
    public boolean has(final MachineResource resource) {
        if (this.resources == null) {
            this.resources = Set.copyOf(this.declared.get());
        }
        return this.resources.contains(resource);
    }

    /** What {@code face} does with {@code resource}; nothing for a resource the machine does not have. */
    public FaceMode mode(final MachineResource resource, final RelativeFace face) {
        if (!has(resource)) {
            return FaceMode.NONE;
        }
        final Map<RelativeFace, FaceMode> set = this.modes.get(resource);
        return set == null ? FaceMode.BOTH : set.getOrDefault(face, FaceMode.BOTH);
    }

    /** What the world's {@code side} of a machine in {@code state} does with {@code resource}. */
    public FaceMode mode(final MachineResource resource, final BlockState state, final Direction side) {
        return mode(resource, RelativeFace.of(RelativeFace.facingOf(state).orElse(Direction.NORTH), side));
    }

    /** Sets what {@code face} does with {@code resource}. */
    public void set(final MachineResource resource, final RelativeFace face, final FaceMode mode) {
        if (has(resource) && mode(resource, face) != Objects.requireNonNull(mode, "mode")) {
            this.modes.computeIfAbsent(resource, key -> new EnumMap<>(RelativeFace.class)).put(face, mode);
            this.changed.run();
        }
    }

    /** What the machine does by itself with {@code resource}. */
    public Automation automation(final MachineResource resource) {
        return this.automation.getOrDefault(resource, Automation.NONE);
    }

    /** Sets what the machine does by itself with {@code resource}. */
    public void setAutomation(final MachineResource resource, final Automation to) {
        if (has(resource) && !automation(resource).equals(Objects.requireNonNull(to, "to"))) {
            this.automation.put(resource, to);
            this.changed.run();
        }
    }

    /** Whether the machine pushes or pulls anything by itself. */
    public boolean automatic() {
        for (final Automation each : this.automation.values()) {
            if (each.push() || each.pull()) {
                return true;
            }
        }
        return false;
    }

    /** Copies every face and every automation of {@code from} that this machine's resources take. */
    public void copy(final MachineSides from) {
        for (final MachineResource resource : resources()) {
            for (final RelativeFace face : RelativeFace.values()) {
                if (from.has(resource)) {
                    set(resource, face, from.mode(resource, face));
                }
            }
            if (from.has(resource)) {
                setAutomation(resource, from.automation(resource));
            }
        }
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        write(tag, registries, false);
    }

    /**
     * Writes every resource it has, set or left as it was, for another machine to {@link #paste}: a copy says of each
     * resource how it stands, so pasting it leaves nothing of the other machine's own.
     */
    public void copyOut(final CompoundTag tag, final HolderLookup.Provider registries) {
        write(tag, registries, true);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.modes.clear();
        this.automation.clear();
        final CompoundTag own = tag.getCompound(KEY);
        for (final MachineResource resource : resources()) {
            if (!own.contains(resource.serializedName())) {
                continue;
            }
            final CompoundTag one = own.getCompound(resource.serializedName());
            final Tag faces = one.get(FACES);
            if (faces != null) {
                FACES_CODEC.parse(NbtOps.INSTANCE, faces).result().ifPresent(read -> {
                    final Map<RelativeFace, FaceMode> copy = new EnumMap<>(RelativeFace.class);
                    copy.putAll(read);
                    this.modes.put(resource, copy);
                });
            }
            final Tag auto = one.get(AUTOMATIC);
            if (auto != null) {
                AUTOMATION_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), auto).result()
                        .ifPresent(read -> this.automation.put(resource, read));
            }
        }
    }

    /**
     * Takes what {@code tag}, written by {@link #save} on another machine, says of each resource this machine has as
     * well: what each face does with it and what the machine does with it by itself, filters and all. A resource the
     * other machine did not have is left as it was.
     */
    public void paste(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CompoundTag own = tag.getCompound(KEY);
        boolean pasted = false;
        for (final MachineResource resource : resources()) {
            if (!own.contains(resource.serializedName())) {
                continue;
            }
            final CompoundTag one = own.getCompound(resource.serializedName());
            final Map<RelativeFace, FaceMode> faces = new EnumMap<>(RelativeFace.class);
            final Tag written = one.get(FACES);
            if (written != null) {
                FACES_CODEC.parse(NbtOps.INSTANCE, written).result().ifPresent(faces::putAll);
            }
            this.modes.put(resource, faces);
            final Tag auto = one.get(AUTOMATIC);
            this.automation.remove(resource);
            if (auto != null) {
                AUTOMATION_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), auto).result()
                        .ifPresent(read -> this.automation.put(resource, read));
            }
            pasted = true;
        }
        if (pasted) {
            this.changed.run();
        }
    }

    /** Whether {@code tag}, written by {@link #save}, says anything of a resource this machine has. */
    public boolean speaksOf(final CompoundTag tag) {
        final CompoundTag own = tag.getCompound(KEY);
        for (final MachineResource resource : resources()) {
            if (own.contains(resource.serializedName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        save(tag, registries);
    }

    /* Writes each resource's faces and automation; every resource for {@code all}, only those set otherwise. */
    private void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean all) {
        final CompoundTag own = new CompoundTag();
        for (final MachineResource resource : resources()) {
            final CompoundTag one = new CompoundTag();
            final Map<RelativeFace, FaceMode> set = this.modes.get(resource);
            if (set != null && !set.isEmpty()) {
                FACES_CODEC.encodeStart(NbtOps.INSTANCE, set).result().ifPresent(faces -> one.put(FACES, faces));
            }
            final Automation auto = this.automation.get(resource);
            if (auto != null && !auto.equals(Automation.NONE)) {
                AUTOMATION_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), auto).result()
                        .ifPresent(written -> one.put(AUTOMATIC, written));
            }
            if (all || !one.isEmpty()) {
                own.put(resource.serializedName(), one);
            }
        }
        tag.put(KEY, own);
    }

    @Override
    public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        load(tag, registries);
    }

    /**
     * What a machine does by itself with one resource: pushing what it makes into the block beside each face that
     * gives out, and pulling what it works from the block beside each face that takes in, each at most its rate a
     * second, only what the filter lets pass.
     *
     * @param push     whether it pushes out
     * @param pushRate the most it pushes a second: items, millibuckets or energy
     * @param pull     whether it pulls in
     * @param pullRate the most it pulls a second
     * @param filter   what may pass
     */
    public record Automation(boolean push, int pushRate, boolean pull, int pullRate, ItemFilter filter) {

        /** What a machine pushes and pulls a second when nothing says: a stack of items. */
        public static final int RATE = 64;
        /** Nothing done by itself. */
        public static final Automation NONE = new Automation(false, RATE, false, RATE, ItemFilter.EVERYTHING);

        public Automation {
            Objects.requireNonNull(filter, "filter");
            pushRate = Math.max(1, pushRate);
            pullRate = Math.max(1, pullRate);
        }

        /** The same, pushing at {@code rate} a second, or not pushing for {@code on} false. */
        public Automation pushing(final boolean on, final int rate) {
            return new Automation(on, rate, this.pull, this.pullRate, this.filter);
        }

        /** The same, pulling at {@code rate} a second, or not pulling for {@code on} false. */
        public Automation pulling(final boolean on, final int rate) {
            return new Automation(this.push, this.pushRate, on, rate, this.filter);
        }

        /** The same, through {@code to}. */
        public Automation filtered(final ItemFilter to) {
            return new Automation(this.push, this.pushRate, this.pull, this.pullRate, to);
        }
    }
}
