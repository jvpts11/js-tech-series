/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.blockentity.IFieldPart;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The parts of a block space, kept by its block entity as one of its fields: the devices mounted on its faces (a bus
 * on a cable, a sensor), one a face, and the microblocks standing in any of its places. A part fits only where it
 * shares no room with another, as {@link SpaceFit} says. They are saved whole; the players who see the space are sent
 * which kind of device sits on which face, as the registry numbers it, and every microblock, which is what they draw.
 *
 * <pre>{@code
 * private final SpaceParts parts = new SpaceParts(this);
 * private final PartField mounted = fields().part(SpaceParts.KEY, parts).save().toClient();
 * }</pre>
 */
public final class SpaceParts implements IFieldPart {

    private final IPartHost host;
    private final IFacePart[] devices = new IFacePart[FACES];
    /** Each face's kind of device, as the registry numbers it; on a player's game the only thing known of them. */
    private final int[] kinds = new int[FACES];
    private final Microblock[] micro = new Microblock[SpaceSlots.COUNT];
    /* On a player's game, which faces' devices are at work, a bit a face; the server asks the devices themselves. */
    private int busy;
    private @Nullable VoxelShape shape;

    /** Where the parts are saved. */
    public static final String KEY = "Parts";
    private static final int FACES = 6;
    private static final int NONE = -1;
    private static final String FACE = "Face";
    private static final String TYPE = "Type";
    private static final String DATA = "Data";
    private static final String MICRO = "Micro";
    private static final String CLIENT_KINDS = "PartKinds";
    private static final String CLIENT_BUSY = "PartBusy";
    private static final double PIXEL = 1.0 / PartBox.SIDE;
    private static final Codec<List<Microblock>> MICROBLOCKS = Microblock.CODEC.listOf();

    public SpaceParts(final IPartHost host) {
        this.host = Objects.requireNonNull(host, "host");
        Arrays.fill(this.kinds, NONE);
    }

    // What stands where

    /** Where the space is. */
    public BlockPos hostPos() {
        return this.host.partPos();
    }

    /** Whether anything stands on {@code face}: a device or a microblock. */
    public boolean has(final Direction face) {
        final int index = face.get3DDataValue();
        return this.kinds[index] != NONE || this.micro[SpaceSlots.face(index)] != null;
    }

    /** Whether any part stands in the space. */
    public boolean any() {
        return count() > 0;
    }

    /** How many parts stand in the space: its devices and its microblocks. */
    public int count() {
        int count = 0;
        for (final int kind : this.kinds) {
            if (kind != NONE) {
                count++;
            }
        }
        for (final Microblock block : this.micro) {
            if (block != null) {
                count++;
            }
        }
        return count;
    }

    /** The device on {@code face}, on the server; null when there is none, and always on a player's game. */
    public @Nullable IFacePart get(final Direction face) {
        return this.devices[face.get3DDataValue()];
    }

    /** The kind of device on {@code face}, on either side; null when there is none. */
    public @Nullable PartType<?> type(final Direction face) {
        final int kind = this.kinds[face.get3DDataValue()];
        return kind == NONE ? null : CoreParts.REGISTRY.byId(kind);
    }

    /** The microblock standing in {@code slot}, on either side; null when there is none. */
    public @Nullable Microblock microblock(final int slot) {
        return SpaceSlots.valid(slot) ? this.micro[slot] : null;
    }

    /** Every microblock standing in the space. */
    public List<Microblock> microblocks() {
        final List<Microblock> out = new ArrayList<>();
        for (final Microblock block : this.micro) {
            if (block != null) {
                out.add(block);
            }
        }
        return out;
    }

    /**
     * Whether the face's link is cut: a device on it, or a solid microblock, closes it to a cable's wires; a hollow
     * cover and a facade leave it open.
     */
    public boolean cutsLink(final Direction face) {
        final int index = face.get3DDataValue();
        final Microblock block = this.micro[SpaceSlots.face(index)];
        return this.kinds[index] != NONE || block != null && block.piece().shape().cutsLink();
    }

    /** Whether {@code piece} fits beside the parts standing, and what runs through the middle. */
    public boolean fits(final SpaceFit.Piece piece) {
        if (!SpaceSlots.valid(piece.slot())) {
            return false;
        }
        return SpaceFit.fits(piece, pieces(), this.host.middle());
    }

    // Putting in and taking out

    /**
     * Mounts {@code device} on {@code face}, when it fits there.
     *
     * @return whether it was mounted
     * @throws IllegalArgumentException when the device's type is not registered
     */
    public boolean add(final Direction face, final IFacePart device) {
        // An unregistered type has no number, and the registry answers -1, which is also the mark of an empty face.
        final int kind = CoreParts.REGISTRY.getId(device.type());
        if (kind < 0) {
            throw new IllegalArgumentException("the part type " + device.type() + " is not registered");
        }
        final int index = face.get3DDataValue();
        if (!fits(new SpaceFit.Piece(SpaceSlots.face(index), List.of(SpaceFit.deviceBox(index)), true))) {
            return false;
        }
        device.attach(this.host, face);
        this.devices[index] = device;
        this.kinds[index] = kind;
        changed();
        return true;
    }

    /** Takes the device off {@code face} and gives it back; null when there was none. */
    public @Nullable IFacePart remove(final Direction face) {
        final int index = face.get3DDataValue();
        final IFacePart removed = this.devices[index];
        final boolean had = this.kinds[index] != NONE;
        this.devices[index] = null;
        this.kinds[index] = NONE;
        if (had) {
            changed();
        }
        return removed;
    }

    /**
     * Stands {@code block} in its place, when it fits there; a facade only in a space that takes them.
     *
     * @return whether it stands there now
     */
    public boolean add(final Microblock block) {
        if (block.piece().shape() == MicroShape.FACADE && !this.host.takesFacades() || !fits(block.fitPiece())) {
            return false;
        }
        this.micro[block.slot()] = block;
        changed();
        return true;
    }

    /** Takes the microblock out of {@code slot} and gives it back; null when there was none. */
    public @Nullable Microblock removeMicroblock(final int slot) {
        final Microblock removed = microblock(slot);
        if (removed != null) {
            this.micro[slot] = null;
            changed();
        }
        return removed;
    }

    // Working

    /** Runs every device's tick, on the server. */
    public void tick() {
        for (final IFacePart device : this.devices) {
            if (device != null) {
                device.serverTick();
            }
        }
    }

    /** Drops what every device holds, and leaves the parts on, as the space goes in a way that keeps them. */
    public void dropContents(final ServerLevel level) {
        for (final IFacePart device : this.devices) {
            if (device != null) {
                device.dropContents(level);
            }
        }
    }

    /** What taking the whole space gives back: every device's item and every microblock's. */
    public List<ItemStack> drops() {
        final List<ItemStack> out = new ArrayList<>();
        for (final IFacePart device : this.devices) {
            if (device != null) {
                out.add(device.partItem());
            }
        }
        for (final Microblock block : this.micro) {
            if (block != null) {
                out.add(CoreSpaces.stack(block.piece()));
            }
        }
        return out;
    }

    // Shape, aim and looks

    /** What the devices draw: each kind's model, its busy one for a device at work, turned to its face. */
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

    /** Whether the device on {@code face} is at work, on either side; false when there is none. */
    public boolean busy(final Direction face) {
        return (busyFaces() & 1 << face.get3DDataValue()) != 0;
    }

    /** The shape of every part standing, within the block. */
    public VoxelShape shape() {
        if (this.shape == null) {
            VoxelShape made = Shapes.empty();
            for (final SpaceFit.Piece piece : pieces()) {
                made = Shapes.or(made, voxels(piece.boxes()));
            }
            this.shape = made.optimize();
        }
        return this.shape;
    }

    /** The shape of the part in {@code slot}, within the block; empty for none. */
    public VoxelShape shape(final int slot) {
        for (final SpaceFit.Piece piece : pieces()) {
            if (piece.slot() == slot) {
                return voxels(piece.boxes());
            }
        }
        return Shapes.empty();
    }

    /** The part a ray from {@code start} to {@code end} meets first in the space at {@code pos}, or none. */
    public @Nullable Hit aim(final BlockPos pos, final Vec3 start, final Vec3 end) {
        Hit best = null;
        for (final SpaceFit.Piece piece : pieces()) {
            final VoxelShape outline = voxels(piece.boxes());
            final BlockHitResult hit = outline.clip(start, end, pos);
            if (hit != null) {
                final double distance = start.distanceToSqr(hit.getLocation());
                if (best == null || distance < best.distance()) {
                    best = new Hit(piece.slot(), outline, distance, hit);
                }
            }
        }
        return best;
    }

    /** Every part standing, as the fit sees it. */
    public List<SpaceFit.Piece> pieces() {
        final List<SpaceFit.Piece> out = new ArrayList<>();
        for (int face = 0; face < FACES; face++) {
            if (this.kinds[face] != NONE) {
                out.add(new SpaceFit.Piece(SpaceSlots.face(face), List.of(SpaceFit.deviceBox(face)), true));
            }
        }
        for (final Microblock block : this.micro) {
            if (block != null) {
                out.add(block.fitPiece());
            }
        }
        return out;
    }

    // Saving and sending

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        for (int i = 0; i < FACES; i++) {
            final IFacePart device = this.devices[i];
            final ResourceLocation id = device == null ? null : device.type().id();
            if (id == null) {
                continue;
            }
            final CompoundTag entry = new CompoundTag();
            entry.putByte(FACE, (byte) i);
            entry.putString(TYPE, id.toString());
            final CompoundTag data = new CompoundTag();
            device.save(data, registries);
            entry.put(DATA, data);
            list.add(entry);
        }
        if (!list.isEmpty()) {
            tag.put(KEY, list);
        }
        writeMicro(tag);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        Arrays.fill(this.devices, null);
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
            final IFacePart device = type.create();
            device.attach(this.host, Direction.from3DDataValue(index));
            device.load(entry.getCompound(DATA), registries);
            this.devices[index] = device;
            this.kinds[index] = CoreParts.REGISTRY.getId(type);
        }
        readMicro(tag);
        this.shape = null;
    }

    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putIntArray(CLIENT_KINDS, this.kinds.clone());
        tag.putByte(CLIENT_BUSY, (byte) busyFaces());
        writeMicro(tag);
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
        readMicro(tag);
        this.shape = null;
    }

    /* The parts changed: the shape follows them, the block is saved and sent, and what it offers is asked again. */
    private void changed() {
        this.shape = null;
        this.host.partChanged();
        final ServerLevel level = this.host.partServerLevel();
        if (level != null) {
            level.invalidateCapabilities(this.host.partPos());
        }
    }

    private void writeMicro(final CompoundTag tag) {
        final List<Microblock> standing = microblocks();
        if (!standing.isEmpty()) {
            MICROBLOCKS.encodeStart(NbtOps.INSTANCE, standing).ifSuccess(written -> tag.put(MICRO, written));
        }
    }

    /* A microblock of a block that is gone, or one that no longer fits, is not kept. */
    private void readMicro(final CompoundTag tag) {
        Arrays.fill(this.micro, null);
        if (!tag.contains(MICRO)) {
            return;
        }
        MICROBLOCKS.parse(NbtOps.INSTANCE, tag.get(MICRO)).resultOrPartial(problem ->
                JsCore.LOGGER.warn("Dropping microblocks that cannot be read: {}", problem)).ifPresent(read -> {
                    for (final Microblock block : read) {
                        this.micro[block.slot()] = block;
                    }
                });
    }

    /* The faces whose devices are at work, a bit a face: asked of the devices on the server, as last sent on a game. */
    private int busyFaces() {
        int faces = this.busy;
        for (int i = 0; i < FACES; i++) {
            final IFacePart device = this.devices[i];
            if (device != null && device.busy()) {
                faces |= 1 << i;
            }
        }
        return faces;
    }

    private static VoxelShape voxels(final List<PartBox> boxes) {
        VoxelShape made = Shapes.empty();
        for (final PartBox box : boxes) {
            made = Shapes.or(made, Shapes.box(box.x0() * PIXEL, box.y0() * PIXEL, box.z0() * PIXEL,
                    box.x1() * PIXEL, box.y1() * PIXEL, box.z1() * PIXEL));
        }
        return made;
    }

    /**
     * The part a ray met first.
     *
     * @param slot     the place it stands in
     * @param outline  its shape, to draw the highlight round
     * @param distance how far along the ray it was met, squared
     * @param hit      where and on which side it was met
     */
    public record Hit(int slot, VoxelShape outline, double distance, BlockHitResult hit) {
    }
}
