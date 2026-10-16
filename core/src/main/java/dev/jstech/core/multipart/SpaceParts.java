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
import dev.jstech.core.look.LookTexts;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The parts of a block space, kept by its block entity as one of its fields: the devices mounted on its faces (a bus
 * on a cable, a sensor), one a face, the microblocks standing in any of its places, and the game's own small blocks
 * hung on its faces ({@link SmallBlocks}). A part fits only where it shares no room with another, as {@link SpaceFit}
 * says. They are saved whole; the players who see the space are sent which kind of device sits on which face, as the
 * registry numbers it, every microblock and every small block, which is what they draw.
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
    /* Each face's device's quarter turns about its face, as a wrench turned it; on either side. */
    private final int[] turns = new int[FACES];
    private final Microblock[] micro = new Microblock[SpaceSlots.COUNT];
    /* The game's own small block hung on each face, a torch or a lever ({@link SmallBlocks}), on either side. */
    private final BlockState[] small = new BlockState[FACES];
    /* When each pressed button or plate is looked at again, in game ticks; none for 0. On the server. */
    private final long[] smallDue = new long[FACES];
    /* On a player's game, which faces' devices are at work, a bit a face; the server asks the devices themselves. */
    private int busy;
    private @Nullable VoxelShape shape;
    /* The shape without what has no collision, the small blocks, worked out with the shape. */
    private @Nullable VoxelShape solid;

    /** Where the parts are saved. */
    public static final String KEY = "Parts";
    private static final int FACES = 6;
    private static final int NONE = -1;
    private static final String FACE = "Face";
    private static final String TYPE = "Type";
    private static final String DATA = "Data";
    private static final String MICRO = "Micro";
    private static final String SMALL = "Small";
    private static final String STATE = "State";
    private static final String DUE = "Due";
    private static final String TURNS = "Turns";
    private static final String CLIENT_TURNS = "PartTurns";
    private static final int QUARTERS = 4;
    private static final int AXES = 3;
    private static final String CLIENT_KINDS = "PartKinds";
    private static final String CLIENT_BUSY = "PartBusy";
    private static final double PIXEL = 1.0 / PartBox.SIDE;
    private static final int MAX_STRENGTH = 15;
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

    /** Whether anything stands on {@code face}: a device, a microblock or a small block. */
    public boolean has(final Direction face) {
        final int index = face.get3DDataValue();
        return this.kinds[index] != NONE || this.micro[SpaceSlots.face(index)] != null || this.small[index] != null;
    }

    /** Whether any part stands in the space. */
    public boolean any() {
        return count() > 0;
    }

    /** How many parts stand in the space: its devices, its microblocks and its small blocks. */
    public int count() {
        int count = smallBlocks().size();
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

    /** The small block hung on {@code face}, on either side; null when there is none. */
    public @Nullable BlockState small(final Direction face) {
        return this.small[face.get3DDataValue()];
    }

    /** Every small block hung in the space. */
    public List<BlockState> smallBlocks() {
        final List<BlockState> out = new ArrayList<>();
        for (final BlockState state : this.small) {
            if (state != null) {
                out.add(state);
            }
        }
        return out;
    }

    /** When the small block on {@code face} is looked at again, in game ticks; 0 for never. On the server. */
    public long smallDue(final Direction face) {
        return this.smallDue[face.get3DDataValue()];
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

    /**
     * The faces a piece of an opaque block covers whole, a bit for each in the order of the game's directions: a
     * cover, a panel or a slab, or a facade, but never a hollow piece.
     */
    public int coveredFaces() {
        int covered = 0;
        for (int face = 0; face < FACES; face++) {
            final Microblock block = this.micro[SpaceSlots.face(face)];
            if (block != null && block.piece().shape() != MicroShape.HOLLOW
                    && MicroblockMaterials.opaque(block.piece().material())) {
                covered |= 1 << face;
            }
        }
        return covered;
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
        if (!fits(SpaceFit.Piece.device(SpaceSlots.face(index), device.type().boxesOn(index)))) {
            return false;
        }
        device.attach(this.host, face);
        this.devices[index] = device;
        this.kinds[index] = kind;
        this.turns[index] = 0;
        changed();
        return true;
    }

    /** The quarter turns the device on {@code face} was turned about its face, 0 to 3; on either side. */
    public int turns(final Direction face) {
        return this.turns[face.get3DDataValue()];
    }

    /**
     * Whether a wrench turns the part in {@code slot}: a device of a kind that turns, a lever or a button on a floor
     * or a ceiling, a post through the middle; when it still fits turned. On either side.
     */
    public boolean canTurn(final int slot) {
        return turnedFits(slot, false);
    }

    /**
     * Turns the part in {@code slot} as a wrench does: a device a quarter turn about its face, a lever or a button on
     * a floor or a ceiling a quarter turn, a post through the middle to the next axis.
     *
     * @return whether it turned
     */
    public boolean turn(final int slot) {
        return turnedFits(slot, true);
    }

    /** Takes the device off {@code face} and gives it back; null when there was none. */
    public @Nullable IFacePart remove(final Direction face) {
        final int index = face.get3DDataValue();
        final IFacePart removed = this.devices[index];
        final boolean had = this.kinds[index] != NONE;
        this.devices[index] = null;
        this.kinds[index] = NONE;
        this.turns[index] = 0;
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

    /** Whether the small block in {@code state} fits on its face, beside the parts standing. */
    public boolean fitsSmall(final BlockState state) {
        return SmallBlocks.takes(state.getBlock()) && fits(SmallBlocks.pieceOf(state));
    }

    /**
     * Hangs the small block in {@code state} on its face, when it fits there.
     *
     * @return whether it hangs there now
     */
    public boolean addSmall(final BlockState state) {
        if (!fitsSmall(state)) {
            return false;
        }
        final int index = SmallBlocks.faceOf(state).get3DDataValue();
        this.small[index] = state;
        this.smallDue[index] = 0L;
        changed();
        return true;
    }

    /**
     * Sets the small block on {@code face} to {@code state}, a lever pulled or a plate pressed, and when it is looked
     * at again; its block's neighbours hear of a change in what it gives.
     */
    public void setSmall(final Direction face, final BlockState state, final long due) {
        final int index = face.get3DDataValue();
        final BlockState was = this.small[index];
        final boolean wasDue = this.smallDue[index] != 0L;
        this.small[index] = state;
        this.smallDue[index] = due;
        if (state != was || wasDue != (due != 0L)) {
            changed();
        }
        if (was == null || SmallBlocks.signalChanged(was, state)) {
            this.host.partSignalChanged(face);
        }
    }

    /** Takes the small block off {@code face} and gives it back; null when there was none. */
    public @Nullable BlockState removeSmall(final Direction face) {
        final int index = face.get3DDataValue();
        final BlockState removed = this.small[index];
        if (removed != null) {
            this.small[index] = null;
            this.smallDue[index] = 0L;
            changed();
            this.host.partSignalChanged(face);
        }
        return removed;
    }

    // Working

    /** Runs every device's tick, on the server. */
    public void tick() {
        for (final IFacePart device : this.devices) {
            if (device != null && device.ticks()) {
                device.serverTick();
            }
        }
        final ServerLevel level = this.host.partServerLevel();
        if (level != null && anyDue()) {
            SmallBlocks.tick(level, this);
        }
    }

    /**
     * Whether any part needs the space's tick, on the server: a device that ticks, or a pressed button or plate. A
     * space whose parts need none is never ticked.
     */
    public boolean ticks() {
        for (final IFacePart device : this.devices) {
            if (device != null && device.ticks()) {
                return true;
            }
        }
        return anyDue();
    }

    /** The light the parts give, the brightest of them, 0 to 15: a torch's, a device's; on the server. */
    public int light() {
        int light = 0;
        for (final IFacePart device : this.devices) {
            if (device != null) {
                light = Math.max(light, strength(device.light()));
            }
        }
        for (final BlockState state : this.small) {
            if (state != null) {
                light = Math.max(light, state.getLightEmission());
            }
        }
        return light;
    }

    // Redstone

    /** The redstone strength the device on {@code face} gives out of it, 0 to 15; on the server. */
    public int signal(final Direction face) {
        final IFacePart device = get(face);
        return device == null ? 0 : strength(device.signal());
    }

    /** The strength the device on {@code face} gives into the block there, 0 to 15; on the server. */
    public int directSignal(final Direction face) {
        final IFacePart device = get(face);
        return device == null ? 0 : strength(device.directSignal());
    }

    /** What a comparator reading {@code face} gives, 0 to 15; on the server. */
    public int comparator(final Direction face) {
        final IFacePart device = get(face);
        return device == null ? 0 : strength(device.comparator());
    }

    /**
     * The strength the small blocks give the block that asks from {@code direction}, the way the game names it: a
     * lever, button or plate gives every side, and its own block more, as the game's own does.
     */
    public int smallSignal(final BlockGetter level, final BlockPos pos, final Direction direction,
                           final boolean direct) {
        int most = 0;
        for (final BlockState state : this.small) {
            if (state != null) {
                most = Math.max(most, direct ? state.getDirectSignal(level, pos, direction)
                        : state.getSignal(level, pos, direction));
            }
        }
        return most;
    }

    /**
     * Whether redstone wire beside {@code face} joins the space: its device's kind deals in redstone, or a lever, a
     * button or a plate hangs in the space, which wire joins from every side as it does the game's own; either side.
     */
    public boolean joinsRedstone(final Direction face) {
        final PartType<?> type = type(face);
        if (type != null && type.joinsRedstone()) {
            return true;
        }
        for (final BlockState state : this.small) {
            if (state != null && state.isSignalSource()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the lines a player looking at the part in {@code slot} reads: what its device says of itself, or the
     * redstone a lever, a button or a plate gives. Asked on the server.
     */
    public void describe(final int slot, final List<Text> lines, final boolean details) {
        if (!SpaceSlots.isFace(slot)) {
            return;
        }
        final int index = SpaceSlots.faceOf(slot);
        final BlockState state = this.small[index];
        if (state != null) {
            if (state.isSignalSource()) {
                lines.add(LookTexts.REDSTONE.with(SmallBlocks.strengthOf(state)));
            }
            return;
        }
        final IFacePart device = this.devices[index];
        if (device != null) {
            device.describe(lines, details);
        }
    }

    /** Tells the device on {@code face} that the block there, or the redstone reaching it, changed. */
    public void neighbourChanged(final Direction face) {
        final IFacePart device = get(face);
        if (device != null) {
            device.neighbourChanged();
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
        for (final BlockState state : this.small) {
            if (state != null) {
                out.add(SmallBlocks.itemOf(state));
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
                models.add(PlacedModel.facing(type.model((working & 1 << face.get3DDataValue()) != 0), face,
                        this.turns[face.get3DDataValue()]));
            }
        }
        return models.isEmpty() ? ModelLayout.EMPTY : new ModelLayout(models);
    }

    /** Whether a device of a kind that moves stands on any face, drawn each frame on the players' games. */
    public boolean anyMoving() {
        for (final Direction face : Direction.values()) {
            final PartType<?> type = type(face);
            if (type != null && type.moves()) {
                return true;
            }
        }
        return false;
    }

    /** Whether the device on {@code face} is at work, on either side; false when there is none. */
    public boolean busy(final Direction face) {
        return (busyFaces() & 1 << face.get3DDataValue()) != 0;
    }

    /** The shape of every part standing, within the block. */
    public VoxelShape shape() {
        if (this.shape == null) {
            VoxelShape made = Shapes.empty();
            VoxelShape held = Shapes.empty();
            for (final SpaceFit.Piece piece : pieces()) {
                final VoxelShape outline = outlineOf(piece);
                made = Shapes.or(made, outline);
                if (!isSmall(piece)) {
                    held = Shapes.or(held, outline);
                }
            }
            this.shape = made.optimize();
            this.solid = held.optimize();
        }
        return this.shape;
    }

    /** The shape of what in the space has collision: every part but the small blocks, which have none. */
    public VoxelShape solidShape() {
        shape();
        return this.solid == null ? Shapes.empty() : this.solid;
    }

    /** The shape of the part in {@code slot}, within the block; empty for none. */
    public VoxelShape shape(final int slot) {
        for (final SpaceFit.Piece piece : pieces()) {
            if (piece.slot() == slot) {
                return outlineOf(piece);
            }
        }
        return Shapes.empty();
    }

    /** The part a ray from {@code start} to {@code end} meets first in the space at {@code pos}, or none. */
    public @Nullable Hit aim(final BlockPos pos, final Vec3 start, final Vec3 end) {
        Hit best = null;
        for (final SpaceFit.Piece piece : pieces()) {
            final VoxelShape outline = outlineOf(piece);
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
            final PartType<?> type = type(Direction.from3DDataValue(face));
            if (type != null) {
                out.add(SpaceFit.Piece.device(SpaceSlots.face(face), type.boxesOn(face, this.turns[face])));
            }
        }
        for (final Microblock block : this.micro) {
            if (block != null) {
                out.add(block.fitPiece());
            }
        }
        for (final BlockState state : this.small) {
            if (state != null) {
                out.add(SmallBlocks.pieceOf(state));
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
            if (this.turns[i] != 0) {
                entry.putByte(TURNS, (byte) this.turns[i]);
            }
            final CompoundTag data = new CompoundTag();
            device.save(data, registries);
            entry.put(DATA, data);
            list.add(entry);
        }
        if (!list.isEmpty()) {
            tag.put(KEY, list);
        }
        writeMicro(tag);
        writeSmall(tag, true);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        Arrays.fill(this.devices, null);
        Arrays.fill(this.kinds, NONE);
        Arrays.fill(this.turns, 0);
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
            this.turns[index] = Math.floorMod(entry.getByte(TURNS), QUARTERS);
        }
        readMicro(tag);
        readSmall(tag, registries);
        this.shape = null;
    }

    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putIntArray(CLIENT_KINDS, this.kinds.clone());
        tag.putIntArray(CLIENT_TURNS, this.turns.clone());
        tag.putByte(CLIENT_BUSY, (byte) busyFaces());
        writeMicro(tag);
        writeSmall(tag, false);
    }

    @Override
    public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        if (!tag.contains(CLIENT_KINDS)) {
            return;
        }
        final int[] incoming = tag.getIntArray(CLIENT_KINDS);
        final int[] turned = tag.getIntArray(CLIENT_TURNS);
        for (int i = 0; i < FACES; i++) {
            this.kinds[i] = i < incoming.length ? incoming[i] : NONE;
            this.turns[i] = i < turned.length ? Math.floorMod(turned[i], QUARTERS) : 0;
        }
        this.busy = tag.getByte(CLIENT_BUSY) & 0xFF;
        readMicro(tag);
        readSmall(tag, registries);
        this.shape = null;
        MovingParts.note(this.host.partPos(), anyMoving());
    }

    /* A strength of redstone or light, kept within the game's range whatever a part says. */
    private static int strength(final int said) {
        return Math.clamp(said, 0, MAX_STRENGTH);
    }

    /* Whether the part in {@code slot} has a turned form that fits beside the rest; with {@code apply}, takes it. */
    private boolean turnedFits(final int slot, final boolean apply) {
        if (slot == SpaceSlots.CENTRE) {
            final Microblock post = this.micro[slot];
            if (post == null || post.piece().shape() != MicroShape.POST) {
                return false;
            }
            final Microblock turned = new Microblock(post.piece(), slot, (post.axis() + 1) % AXES);
            if (!fitsInstead(slot, turned.fitPiece())) {
                return false;
            }
            if (apply) {
                this.micro[slot] = turned;
                changed();
            }
            return true;
        }
        if (!SpaceSlots.isFace(slot)) {
            return false;
        }
        final int index = SpaceSlots.faceOf(slot);
        final BlockState small = this.small[index];
        if (small != null) {
            final BlockState turned = SmallBlocks.turned(small);
            if (turned == null || !fitsInstead(slot, SmallBlocks.pieceOf(turned))) {
                return false;
            }
            if (apply) {
                this.small[index] = turned;
                changed();
            }
            return true;
        }
        final PartType<?> type = type(Direction.from3DDataValue(index));
        final int next = (this.turns[index] + 1) % QUARTERS;
        if (type == null || !type.turnable()
                || !fitsInstead(slot, SpaceFit.Piece.device(slot, type.boxesOn(index, next)))) {
            return false;
        }
        if (apply) {
            this.turns[index] = next;
            final IFacePart device = this.devices[index];
            if (device != null) {
                device.turned(next);
            }
            changed();
        }
        return true;
    }

    /* Whether {@code piece} fits beside every part but the one in its own place, which it takes the place of. */
    private boolean fitsInstead(final int slot, final SpaceFit.Piece piece) {
        final List<SpaceFit.Piece> others = new ArrayList<>(pieces());
        others.removeIf(other -> other.slot() == slot);
        return SpaceFit.fits(piece, others, this.host.middle());
    }

    /* Whether a pressed button or plate waits to be looked at again. */
    private boolean anyDue() {
        for (final long due : this.smallDue) {
            if (due != 0L) {
                return true;
            }
        }
        return false;
    }

    /* Whether {@code piece} is a small block's, in its face's place. */
    private boolean isSmall(final SpaceFit.Piece piece) {
        return SpaceSlots.isFace(piece.slot()) && this.small[SpaceSlots.faceOf(piece.slot())] != null;
    }

    /* What a piece is outlined, hit and shaped by: a small block's own shape, else its boxes. */
    private VoxelShape outlineOf(final SpaceFit.Piece piece) {
        return isSmall(piece) ? SmallBlocks.shapeOf(this.small[SpaceSlots.faceOf(piece.slot())])
                : voxels(piece.boxes());
    }

    /* Each small block by its face and state; in a save, also when it is looked at again. */
    private void writeSmall(final CompoundTag tag, final boolean saved) {
        final ListTag list = new ListTag();
        for (int i = 0; i < FACES; i++) {
            if (this.small[i] == null) {
                continue;
            }
            final CompoundTag entry = new CompoundTag();
            entry.putByte(FACE, (byte) i);
            entry.put(STATE, NbtUtils.writeBlockState(this.small[i]));
            if (saved && this.smallDue[i] != 0L) {
                entry.putLong(DUE, this.smallDue[i]);
            }
            list.add(entry);
        }
        if (!list.isEmpty()) {
            tag.put(SMALL, list);
        }
    }

    /* A small block of a block that is gone, or no longer one a space takes, is not kept. */
    private void readSmall(final CompoundTag tag, final HolderLookup.Provider registries) {
        Arrays.fill(this.small, null);
        Arrays.fill(this.smallDue, 0L);
        for (final Tag element : tag.getList(SMALL, Tag.TAG_COMPOUND)) {
            final CompoundTag entry = (CompoundTag) element;
            final int index = entry.getByte(FACE) & 0xFF;
            final BlockState state = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK),
                    entry.getCompound(STATE));
            if (index < FACES && SmallBlocks.takes(state.getBlock())
                    && SmallBlocks.faceOf(state).get3DDataValue() == index) {
                this.small[index] = state;
                this.smallDue[index] = entry.getLong(DUE);
            }
        }
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
