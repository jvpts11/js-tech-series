/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import dev.jstech.core.JsCore;
import dev.jstech.core.blockentity.IFieldPart;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The parts mounted on a block's six faces, one a face, kept by the block's entity as one of its fields. They are
 * saved whole, each with its kind's id and what it keeps; the players who see the block are sent only which kind
 * sits on which face, as the registry numbers it, which is enough to draw and shape the block.
 *
 * <pre>{@code
 * private final FaceParts parts = new FaceParts(this);
 * private final PartField mounted = fields().part("Parts", parts).save().toClient();
 * }</pre>
 */
public final class FaceParts implements IFieldPart {

    private final IPartHost host;
    private final IFacePart[] parts = new IFacePart[FACES];
    /** Each face's kind, as the registry numbers it; on a player's game the only thing known of the parts. */
    private final int[] kinds = new int[FACES];
    /* On a player's game, which faces' parts are at work, a bit a face; the server asks the parts themselves. */
    private int busy;

    /** Where the parts are saved, a compound each. */
    public static final String KEY = "Parts";
    private static final int FACES = 6;
    private static final int NONE = -1;
    private static final String FACE = "Face";
    private static final String TYPE = "Type";
    private static final String DATA = "Data";
    private static final String CLIENT_KINDS = "PartKinds";
    private static final String CLIENT_BUSY = "PartBusy";

    public FaceParts(final IPartHost host) {
        this.host = Objects.requireNonNull(host, "host");
        Arrays.fill(this.kinds, NONE);
    }

    /** Whether a part sits on {@code face}. */
    public boolean has(final Direction face) {
        return this.kinds[face.get3DDataValue()] != NONE;
    }

    /** Whether any face holds a part. */
    public boolean any() {
        for (final int kind : this.kinds) {
            if (kind != NONE) {
                return true;
            }
        }
        return false;
    }

    /** The part on {@code face}, on the server; null when there is none, and always on a player's game. */
    public @Nullable IFacePart get(final Direction face) {
        return this.parts[face.get3DDataValue()];
    }

    /** The kind of part on {@code face}, on either side; null when there is none. */
    public @Nullable PartType<?> type(final Direction face) {
        final int kind = this.kinds[face.get3DDataValue()];
        return kind == NONE ? null : CoreParts.REGISTRY.byId(kind);
    }

    /**
     * Mounts {@code part} on {@code face}, which has to be free.
     *
     * @throws IllegalArgumentException when the part's type is not registered
     */
    public void add(final Direction face, final IFacePart part) {
        if (has(face)) {
            throw new IllegalStateException("the " + face + " face already holds a part");
        }
        // An unregistered type has no number, and the registry answers -1, which is also the mark of an empty face.
        final int kind = CoreParts.REGISTRY.getId(part.type());
        if (kind < 0) {
            throw new IllegalArgumentException("the part type " + part.type() + " is not registered");
        }
        part.attach(this.host, face);
        this.parts[face.get3DDataValue()] = part;
        this.kinds[face.get3DDataValue()] = kind;
        this.host.partChanged();
    }

    /** Takes the part off {@code face} and gives it back; null when there was none. */
    public @Nullable IFacePart remove(final Direction face) {
        final int index = face.get3DDataValue();
        final IFacePart removed = this.parts[index];
        final boolean had = this.kinds[index] != NONE;
        this.parts[index] = null;
        this.kinds[index] = NONE;
        if (had) {
            this.host.partChanged();
        }
        return removed;
    }

    /** Runs every part's tick, on the server. */
    public void tick() {
        for (final IFacePart part : this.parts) {
            if (part != null) {
                part.serverTick();
            }
        }
    }

    /** Drops what every part holds, and leaves the parts on, as the block goes in a way that keeps them. */
    public void dropContents(final ServerLevel level) {
        for (final IFacePart part : this.parts) {
            if (part != null) {
                part.dropContents(level);
            }
        }
    }

    /** What the parts draw: each kind's model, its busy one for a part at work, turned to its face, on either side. */
    public ModelLayout layout() {
        final int working = busyFaces();
        final List<PlacedModel> models = new ArrayList<>();
        for (final Direction face : Direction.values()) {
            final PartType<?> type = type(face);
            if (type != null) {
                models.add(PlacedModel.facing(type.model((working & 1 << face.get3DDataValue()) != 0), face));
            }
        }
        return models.isEmpty() ? ModelLayout.EMPTY : new ModelLayout(models);
    }

    /** Whether the part on {@code face} is at work, on either side; false when there is none. */
    public boolean busy(final Direction face) {
        return (busyFaces() & 1 << face.get3DDataValue()) != 0;
    }

    /** The shape of every part mounted, within the block. */
    public VoxelShape shape() {
        VoxelShape shape = Shapes.empty();
        for (final Direction face : Direction.values()) {
            if (has(face)) {
                shape = Shapes.or(shape, PartBoxes.shape(face));
            }
        }
        return shape;
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        for (int i = 0; i < FACES; i++) {
            final IFacePart part = this.parts[i];
            final ResourceLocation id = part == null ? null : part.type().id();
            if (id == null) {
                continue;
            }
            final CompoundTag entry = new CompoundTag();
            entry.putByte(FACE, (byte) i);
            entry.putString(TYPE, id.toString());
            final CompoundTag data = new CompoundTag();
            part.save(data, registries);
            entry.put(DATA, data);
            list.add(entry);
        }
        if (!list.isEmpty()) {
            tag.put(KEY, list);
        }
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        Arrays.fill(this.parts, null);
        Arrays.fill(this.kinds, NONE);
        for (final Tag element : tag.getList(KEY, Tag.TAG_COMPOUND)) {
            final CompoundTag entry = (CompoundTag) element;
            final int index = entry.getByte(FACE) & 0xFF;
            final ResourceLocation id = ResourceLocation.tryParse(entry.getString(TYPE));
            final PartType<?> type = id == null ? null : CoreParts.REGISTRY.get(id);
            if (index >= FACES || type == null) {
                // A part of a mod that is gone is not kept: the next save writes the block without it.
                JsCore.LOGGER.warn("Dropping the part of type {} on face {}: no such part type is registered",
                        entry.getString(TYPE), index);
                continue;
            }
            final IFacePart part = type.create();
            part.attach(this.host, Direction.from3DDataValue(index));
            part.load(entry.getCompound(DATA), registries);
            this.parts[index] = part;
            this.kinds[index] = CoreParts.REGISTRY.getId(type);
        }
    }

    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putIntArray(CLIENT_KINDS, this.kinds.clone());
        tag.putByte(CLIENT_BUSY, (byte) busyFaces());
    }

    @Override
    public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        if (!tag.contains(CLIENT_KINDS)) {
            return;
        }
        final int[] incoming = tag.getIntArray(CLIENT_KINDS);
        for (int i = 0; i < FACES; i++) {
            this.kinds[i] = i < incoming.length ? incoming[i] : NONE;
        }
        this.busy = tag.getByte(CLIENT_BUSY) & 0xFF;
    }

    /* The faces whose parts are at work, a bit a face: asked of the parts on the server, as last sent on a game. */
    private int busyFaces() {
        int faces = this.busy;
        for (int i = 0; i < FACES; i++) {
            final IFacePart part = this.parts[i];
            if (part != null && part.busy()) {
                faces |= 1 << i;
            }
        }
        return faces;
    }
}
