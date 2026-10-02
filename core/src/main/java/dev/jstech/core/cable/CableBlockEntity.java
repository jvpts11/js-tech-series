/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.connect.Neighbours;
import dev.jstech.core.energy.CoreEnergy;
import dev.jstech.core.fluid.FluidGrids;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridPlace;
import dev.jstech.core.multipart.FaceParts;
import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.multipart.IPartHost;
import dev.jstech.core.multipart.ModelLayout;
import dev.jstech.core.multipart.PartBoxes;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.multipart.PlacedModel;
import dev.jstech.core.network.ConnectivityIndex;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * The block entity of the Core's cable block: the wires running through it, each in its lane, the parts on its faces,
 * and which faces each wire crosses. A wire crosses a face where the block beyond holds a wire it joins, or is a device
 * that takes its line on that face; a wire of energy also plugs into any block that offers the game's energy there,
 * and a pipe into any block that holds fluids there. A part on a face closes it to wires.
 *
 * <p>Each wire is a place of the grid of its kind: it puts itself in as the block loads or as it is laid, joined to the
 * wires and devices it crosses to, and takes itself out as it is taken or the block goes. A wire of data keeps the
 * identity of its network in the save, so a run cut away from every network keeps it across a reload.
 *
 * <p>Which faces each wire crosses is worked out on the server when the block or a neighbour changes, kept in the save
 * and sent to the players who see the block, who draw and shape the wires from it.
 */
public final class CableBlockEntity extends SyncedBlockEntity implements IPartHost {

    private final Bundle bundle = new Bundle(this::bundleChanged);
    private final FaceParts parts = new FaceParts(this);
    /* Each lane's faces: the low six bits those its wire crosses, the six from bit 8 those it meets a device on. */
    private final int[] links = new int[Lane.COUNT];
    private final Map<Lane, NetworkUuid> savedNetworks = new EnumMap<>(Lane.class);
    private final PartField wiresField = fields().part(Bundle.KEY, this.bundle).save().toClient();
    private final PartField partsField = fields().part(FaceParts.KEY, this.parts).save().toClient();
    private final PartField linksField = fields().part(LINKS, new LinksPart()).save().toClient();
    private boolean inGrids;
    private @Nullable BundleShape shape;
    private @Nullable VoxelShape voxels;

    private static final String LINKS = "Links";
    private static final String NETWORKS = "Networks";
    private static final String LANE = "Lane";
    private static final String NETWORK = "Network";
    private static final int FACE_BITS = 0x3F;
    private static final int PLUG_SHIFT = 8;
    private static final double PIXEL = 1.0 / 16.0;

    public CableBlockEntity(final BlockPos pos, final BlockState state) {
        super(CoreCables.BLOCK_ENTITY.get(), pos, state);
        fields().whenNeighbourChanges((level, face) -> refreshLinks(false));
    }

    /** Runs the parts' ticks, on the server. */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final CableBlockEntity cable) {
        cable.parts.tick();
    }

    // The wires

    /** The wires, lane by lane. */
    public List<Wire> wires() {
        return this.bundle.wires();
    }

    /** The wire of {@code type}, or null when the block holds none. */
    public @Nullable Wire wire(final CableType type) {
        return this.bundle.of(type);
    }

    /** Whether the block holds a wire of {@code type}. */
    public boolean holds(final CableType type) {
        return this.bundle.has(type);
    }

    /** Whether {@code wire} could be laid here. */
    public boolean takes(final Wire wire) {
        return this.bundle.takes(wire);
    }

    /** Whether the block holds nothing: no wire and no part. */
    public boolean isEmpty() {
        return pieces() == 0;
    }

    /** How many pieces the block holds: its wires and its parts. */
    public int pieces() {
        int count = this.bundle.size();
        for (final Direction face : Direction.values()) {
            if (this.parts.has(face)) {
                count++;
            }
        }
        return count;
    }

    /** Lays {@code wire}, on the server; false when the block does not take it. */
    public boolean lay(final Wire wire) {
        CoreCables.checkLanes();
        if (!this.bundle.add(wire)) {
            return false;
        }
        refreshLinks(false);
        if (this.inGrids && this.level instanceof ServerLevel server) {
            enter(server, wire);
        }
        afterWiresChanged();
        return true;
    }

    /** Takes out the wire of {@code type}, on the server; false when there was none. */
    public boolean take(final CableType type) {
        final Wire wire = this.bundle.of(type);
        if (wire == null) {
            return false;
        }
        if (this.level instanceof ServerLevel server) {
            leave(server, wire);
        }
        this.bundle.remove(type);
        this.links[wire.slot().id()] = 0;
        this.savedNetworks.remove(wire.slot());
        if (isEmpty() && this.level != null && !this.level.isClientSide()) {
            // A cable block never stands empty: with its last piece taken out, it goes.
            this.level.removeBlock(this.worldPosition, false);
            return true;
        }
        refreshLinks(false);
        afterWiresChanged();
        return true;
    }

    /** Dyes the wire of {@code type}, on the server; false when there is none or it is that colour already. */
    public boolean dye(final CableType type, final DyeColor dye) {
        final Wire wire = this.bundle.of(type);
        if (wire == null || !this.bundle.dye(wire.dyed(dye))) {
            return false;
        }
        refreshLinks(false);
        if (this.inGrids && this.level instanceof ServerLevel server) {
            // Its colour is what it joins by: it goes back in its grid joined to what it joins now.
            leave(server, wire);
            enter(server, wire.dyed(dye));
        }
        afterWiresChanged();
        return true;
    }

    /** The wire in {@code lane}, or null when the lane is empty. */
    public @Nullable Wire wireIn(final Lane lane) {
        return this.bundle.at(lane);
    }

    /** The faces the wire in {@code lane} crosses, a bit for each in the order of the game's directions. */
    public int links(final Lane lane) {
        return this.links[lane.id()] & FACE_BITS;
    }

    /** The faces the wire in {@code lane} plugs into a device on, a bit for each in the game's order of directions. */
    public int plugs(final Lane lane) {
        return this.links[lane.id()] >> PLUG_SHIFT & FACE_BITS;
    }

    /** Whether the wire of {@code type} crosses {@code face}. */
    public boolean crosses(final CableType type, final Direction face) {
        final Wire wire = this.bundle.of(type);
        return wire != null && (links(wire.slot()) & 1 << face.get3DDataValue()) != 0;
    }

    /** The wires that cross {@code face}. */
    public List<Wire> wiresThrough(final Direction face) {
        final List<Wire> through = new ArrayList<>();
        for (final Wire wire : this.bundle.wires()) {
            if ((links(wire.slot()) & 1 << face.get3DDataValue()) != 0) {
                through.add(wire);
            }
        }
        return through;
    }

    /** Where the wire of {@code type} stands in its grid. */
    public GridPlace place(final CableType type) {
        return GridPlace.wire(this.worldPosition.asLong(), Wire.of(type).slot().id());
    }

    /**
     * The network the wire of {@code type} is on, on the server: the one it reaches the owner of without crossing a run
     * longer than its cable reaches. Empty for none, or a wire not of data.
     */
    public Optional<NetworkUuid> network(final CableType type) {
        final OptionalLong number = dataNumber(type);
        return number.isEmpty() ? Optional.empty()
                : NetworkSystem.get((ServerLevel) this.level).connectivity().networkOf(number.getAsLong());
    }

    // The parts

    /** The parts on the block's faces. */
    public FaceParts parts() {
        return this.parts;
    }

    public boolean hasPart(final Direction face) {
        return this.parts.has(face);
    }

    public @Nullable PartType<?> partType(final Direction face) {
        return this.parts.type(face);
    }

    public @Nullable IFacePart getPart(final Direction face) {
        return this.parts.get(face);
    }

    public void addPart(final Direction face, final IFacePart part) {
        this.parts.add(face, part);
    }

    public @Nullable IFacePart removePart(final Direction face) {
        return this.parts.remove(face);
    }

    public boolean hasAnyPart() {
        return this.parts.any();
    }

    /** Drops what every part holds, and leaves the parts on, as the block goes in a way that keeps them. */
    public void dropAllBuffers(final ServerLevel level) {
        this.parts.dropContents(level);
    }

    @Override
    public @Nullable Level partLevel() {
        return this.level;
    }

    @Override
    public BlockPos partPos() {
        return this.worldPosition;
    }

    @Override
    public void partChanged() {
        this.partsField.changed();
        this.voxels = null;
        // A part closes its face to wires, and taking it off opens it again; the neighbours hear of it.
        if (refreshLinks(true) && this.level != null) {
            Neighbours.portsChanged(this.level, this.worldPosition);
        }
    }

    // Shape, aim and drops

    /** How the wires lie, worked out from what the block holds. */
    public BundleShape shape() {
        if (this.shape == null) {
            this.shape = drawing().shape();
        }
        return this.shape;
    }

    /** The wires as the block's model draws them, each with its faces. */
    public CableDrawing drawing() {
        final List<Wire> wires = this.bundle.wires();
        final List<Integer> linked = new ArrayList<>(wires.size());
        for (final Wire wire : wires) {
            linked.add(this.links[wire.slot().id()]);
        }
        return new CableDrawing(wires, linked);
    }

    /** The block's shape: its wires, its junction box and its parts. */
    public VoxelShape voxelShape() {
        if (this.voxels == null) {
            VoxelShape made = Shapes.empty();
            for (final BundleShape.Piece piece : shape().pieces()) {
                if (piece.kind() == BundleShape.Kind.JACKET || piece.kind() == BundleShape.Kind.HOUSING) {
                    made = Shapes.or(made, voxel(piece.box()));
                }
            }
            made = Shapes.or(made, this.parts.shape());
            this.voxels = made.isEmpty() ? Shapes.block() : made.optimize();
        }
        return this.voxels;
    }

    /**
     * What a ray from {@code start} to {@code end} picks in the block: the part or the wire it meets first. In a
     * junction box, a ray that meets the housing between the wires picks the wire nearest where it met it.
     */
    public Aim aim(final Vec3 start, final Vec3 end) {
        final BlockPos pos = this.worldPosition;
        double best = Double.MAX_VALUE;
        Aim picked = Aim.NOTHING;
        for (final Direction face : Direction.values()) {
            if (!this.parts.has(face)) {
                continue;
            }
            final BlockHitResult hit = PartBoxes.shape(face).clip(start, end, pos);
            if (hit != null && start.distanceToSqr(hit.getLocation()) < best) {
                best = start.distanceToSqr(hit.getLocation());
                picked = new Aim(face, null, PartBoxes.shape(face));
            }
        }
        final BundleShape bundleShape = shape();
        final List<Wire> wires = this.bundle.wires();
        for (int i = 0; i < wires.size(); i++) {
            final VoxelShape reach = reachShape(bundleShape, i);
            final BlockHitResult hit = reach.clip(start, end, pos);
            if (hit != null && start.distanceToSqr(hit.getLocation()) < best) {
                best = start.distanceToSqr(hit.getLocation());
                picked = new Aim(null, wires.get(i), reach);
            }
        }
        if (bundleShape.junction()) {
            final Aim housing = aimAtHousing(bundleShape, wires, start, end, best);
            if (housing != null) {
                picked = housing;
            }
        }
        return picked;
    }

    /** What breaking the whole block gives back: every wire's cable and every part. */
    public List<ItemStack> drops() {
        final List<ItemStack> out = new ArrayList<>();
        for (final Wire wire : this.bundle.wires()) {
            out.add(wire.type().stack());
        }
        for (final Direction face : Direction.values()) {
            final IFacePart part = this.parts.get(face);
            if (part != null) {
                out.add(part.partItem());
            }
        }
        return out;
    }

    // The grids

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level instanceof ServerLevel server) {
            CoreCables.checkLanes();
            this.inGrids = true;
            for (final Wire wire : this.bundle.wires()) {
                enter(server, wire);
            }
        }
    }

    /** Takes every wire out of its grid, as the block goes. */
    public void leaveGrids(final ServerLevel server) {
        for (final Wire wire : this.bundle.wires()) {
            leave(server, wire);
        }
        this.inGrids = false;
    }

    /* The parts changed on the client: the render mesh and the collision and selection shape follow them. */
    @Override
    protected void afterClientUpdate() {
        this.shape = null;
        this.voxels = null;
        requestModelDataUpdate();
        if (this.level != null) {
            final BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /* The cable's model draws its wires, their plugs and its parts from this, into the world's mesh. */
    @Override
    public ModelData getModelData() {
        final List<PlacedModel> placed = new ArrayList<>(this.parts.layout().models());
        final List<Wire> wires = this.bundle.wires();
        for (final BundleShape.Plug plug : shape().plugs()) {
            placed.add(plugModel(wires.get(plug.strand()).type(), plug));
        }
        return ModelData.builder()
                .with(ModelLayout.PROPERTY, placed.isEmpty() ? ModelLayout.EMPTY : new ModelLayout(placed))
                .with(CableDrawing.PROPERTY, drawing())
                .build();
    }

    /* Puts {@code wire} in its grid, joined to what it crosses to, and gives it back its saved network. */
    private void enter(final ServerLevel server, final Wire wire) {
        final GridPlace place = place(wire.type());
        final List<GridPlace> neighbours = new ArrayList<>();
        final int crossed = links(wire.slot());
        for (final Direction face : Direction.values()) {
            if ((crossed & 1 << face.get3DDataValue()) != 0) {
                final long next = this.worldPosition.relative(face).asLong();
                neighbours.add(GridPlace.wire(next, wire.slot().id()));
                neighbours.add(GridPlace.whole(next));
            }
        }
        CoreGrids.place(server, wire.type().grid(), place, wire.member(), neighbours);
        final NetworkUuid saved = this.savedNetworks.get(wire.slot());
        if (saved != null && wire.type().grid().carriesNetwork()) {
            final long number = CoreGrids.places(server).number(place);
            final ConnectivityIndex index = NetworkSystem.get(server).connectivity();
            if (index.joinedNetwork(number).isEmpty()) {
                index.assignUuid(number, saved);
            }
        }
    }

    private void leave(final ServerLevel server, final Wire wire) {
        CoreGrids.remove(server, wire.type().grid(), place(wire.type()));
    }

    /* The number the wire of {@code type} is known by in the data grid, on the server, for a data wire held here. */
    private OptionalLong dataNumber(final CableType type) {
        if (!(this.level instanceof ServerLevel server) || !type.grid().carriesNetwork() || !holds(type)) {
            return OptionalLong.empty();
        }
        return CoreGrids.places(server).find(place(type));
    }

    /*
     * Works out which faces each wire crosses, on the server. A neighbour that is not loaded keeps the face as it was.
     * When the block's own parts changed, a wire whose faces changed goes back in its grid joined to what it crosses
     * to now; a neighbour that changed puts itself in or takes itself out of the grids on its own.
     */
    private boolean refreshLinks(final boolean ownParts) {
        if (!(this.level instanceof ServerLevel server)) {
            return false;
        }
        boolean changed = false;
        boolean energyPlugs = false;
        boolean fluidPlugs = false;
        for (final Wire wire : this.bundle.wires()) {
            final int lane = wire.slot().id();
            final int before = this.links[lane];
            final int now = linksOf(server, wire, before);
            if (now != before) {
                this.links[lane] = now;
                changed = true;
                energyPlugs |= wire.type().grid() == GridKind.POWER;
                fluidPlugs |= wire.type().grid() == GridKind.FLUID;
                if (ownParts && this.inGrids && (now & FACE_BITS) != (before & FACE_BITS)) {
                    leave(server, wire);
                    enter(server, wire);
                }
            }
        }
        // The blocks an energy wire or a pipe plugs into are not in its grid: the grid hears of them here.
        if (energyPlugs) {
            CoreEnergy.of(server).tapsChanged();
        }
        if (fluidPlugs) {
            FluidGrids.of(server).tapsChanged();
        }
        if (changed) {
            this.shape = null;
            this.voxels = null;
            this.linksField.changed();
        }
        return changed;
    }

    private int linksOf(final ServerLevel server, final Wire wire, final int before) {
        int linked = 0;
        for (final Direction face : Direction.values()) {
            final int bit = 1 << face.get3DDataValue();
            final BlockPos next = this.worldPosition.relative(face);
            if (this.parts.has(face)) {
                continue;
            }
            if (!server.isLoaded(next)) {
                linked |= before & (bit | bit << PLUG_SHIFT);
                continue;
            }
            // The block is read first: asking a place that holds no cable for its block entity costs a lookup for none.
            final BlockState state = server.getBlockState(next);
            if (state.getBlock() instanceof CableBlock) {
                if (server.getBlockEntity(next) instanceof CableBlockEntity other) {
                    final Wire there = other.bundle.of(wire.type());
                    if (there != null && wire.joins(there) && !other.parts.has(face.getOpposite())
                            && other.roomFor(wire.type(), face.getOpposite())) {
                        linked |= bit;
                    }
                }
                continue;
            }
            if (state.getBlock() instanceof IFaceConnector device
                    && device.accepts(state, face.getOpposite(), wire.type().line())) {
                linked |= bit | bit << PLUG_SHIFT;
            } else if (wire.type().grid() == GridKind.POWER
                    && server.getCapability(Capabilities.EnergyStorage.BLOCK, next, face.getOpposite()) != null) {
                // An energy wire plugs into any block that offers the game's energy on that face.
                linked |= bit | bit << PLUG_SHIFT;
            } else if (wire.type().grid() == GridKind.FLUID
                    && server.getCapability(Capabilities.FluidHandler.BLOCK, next, face.getOpposite()) != null) {
                // And a pipe into any block that holds fluids on that face.
                linked |= bit | bit << PLUG_SHIFT;
            }
        }
        return shaped(wire.type(), linked, before);
    }

    /* Whether the wire of {@code type} here could join one more cable on {@code face} and keep its shape. */
    private boolean roomFor(final CableType type, final Direction face) {
        final Wire mine = this.bundle.of(type);
        if (mine == null) {
            return false;
        }
        final int joined = this.links[mine.slot().id()] & FACE_BITS;
        final int bit = 1 << face.get3DDataValue();
        return (joined & bit) != 0 || WireShape.allows(type.runsStraight(), type.mostJoins(), joined | bit);
    }

    /* The faces a wire that keeps a shape joins, out of those it could; its plugs follow the faces it keeps. */
    private static int shaped(final CableType type, final int linked, final int before) {
        if (!type.runsStraight() && type.mostJoins() <= 0) {
            return linked;
        }
        final int faces = WireShape.select(type.runsStraight(), type.mostJoins(), linked & FACE_BITS,
                before & FACE_BITS);
        return faces | (linked & (faces << PLUG_SHIFT));
    }

    private void bundleChanged() {
        this.wiresField.changed();
        this.shape = null;
        this.voxels = null;
    }

    /* The wires changed: the neighbours see what this block now takes on its faces. */
    private void afterWiresChanged() {
        this.linksField.changed();
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            Neighbours.portsChanged(this.level, this.worldPosition);
        }
    }

    /* The housing the ray meets before anything else picks the wire nearest the point it meets it at. */
    private @Nullable Aim aimAtHousing(final BundleShape bundleShape, final List<Wire> wires, final Vec3 start,
                                       final Vec3 end, final double best) {
        for (final BundleShape.Piece piece : bundleShape.pieces()) {
            if (piece.kind() != BundleShape.Kind.HOUSING) {
                continue;
            }
            final BlockHitResult hit = voxel(piece.box()).clip(start, end, this.worldPosition);
            if (hit == null || start.distanceToSqr(hit.getLocation()) >= best) {
                return null;
            }
            final Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(this.worldPosition)).scale(16.0);
            int nearest = -1;
            double nearestDistance = Double.MAX_VALUE;
            for (int i = 0; i < wires.size(); i++) {
                for (final BundleShape.Box box : bundleShape.reach(i)) {
                    final double distance = box.distanceTo(local.x, local.y, local.z);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = i;
                    }
                }
            }
            return nearest < 0 ? null : new Aim(null, wires.get(nearest), reachShape(bundleShape, nearest));
        }
        return null;
    }

    private static VoxelShape reachShape(final BundleShape bundleShape, final int strand) {
        VoxelShape made = Shapes.empty();
        for (final BundleShape.Box box : bundleShape.reach(strand)) {
            made = Shapes.or(made, voxel(box));
        }
        return made;
    }

    private static VoxelShape voxel(final BundleShape.Box box) {
        return Shapes.box(box.minX() * PIXEL, box.minY() * PIXEL, box.minZ() * PIXEL,
                box.maxX() * PIXEL, box.maxY() * PIXEL, box.maxZ() * PIXEL);
    }

    /* A wire's plug, turned to its face and moved to where the wire crosses it. */
    private static PlacedModel plugModel(final CableType type, final BundleShape.Plug plug) {
        final int axis = BundleShape.axis(plug.face());
        final float[] offset = new float[3];
        final int across = axis == 0 ? 2 : 0;
        final int up = axis == 1 ? 2 : 1;
        offset[across] = (float) plug.across();
        offset[up] = (float) plug.up();
        return new PlacedModel(type.plug(), Direction.from3DDataValue(plug.face()), offset[0], offset[1],
                offset[2]);
    }

    /**
     * What a player aims at in a cable block: a part, a wire, or nothing in it; and the outline that shows it.
     *
     * @param part    the face whose part is aimed at, or null
     * @param wire    the wire aimed at, or null
     * @param outline the outline to draw, within the block
     */
    public record Aim(@Nullable Direction part, @Nullable Wire wire, VoxelShape outline) {

        /** Nothing in the block. */
        public static final Aim NOTHING = new Aim(null, null, Shapes.empty());

        /** Whether it picks nothing. */
        public boolean isNothing() {
            return this.part == null && this.wire == null;
        }
    }

    /** Each lane's faces, kept in the save and sent to the players who see the block. */
    private final class LinksPart implements IFieldPart {

        @Override
        public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
            tag.putIntArray(LINKS, CableBlockEntity.this.links.clone());
            final ListTag networks = new ListTag();
            if (CableBlockEntity.this.level instanceof ServerLevel server) {
                for (final Wire wire : CableBlockEntity.this.bundle.wires()) {
                    // What the wire is joined into is kept, reached or not: reaching is worked out again on load.
                    final OptionalLong number = dataNumber(wire.type());
                    final Optional<NetworkUuid> joined = number.isEmpty() ? Optional.empty()
                            : NetworkSystem.get(server).connectivity().joinedNetwork(number.getAsLong());
                    joined.ifPresent(network -> {
                        final CompoundTag entry = new CompoundTag();
                        entry.putString(LANE, wire.slot().serializedName());
                        entry.putString(NETWORK, network.asString());
                        networks.add(entry);
                    });
                }
            }
            if (!networks.isEmpty()) {
                tag.put(NETWORKS, networks);
            }
        }

        @Override
        public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
            readLinks(tag);
            CableBlockEntity.this.savedNetworks.clear();
            for (final Tag element : tag.getList(NETWORKS, Tag.TAG_COMPOUND)) {
                final CompoundTag entry = (CompoundTag) element;
                final Lane lane = Lane.byName(entry.getString(LANE));
                if (lane != null) {
                    CableBlockEntity.this.savedNetworks.put(lane, NetworkUuid.fromString(entry.getString(NETWORK)));
                }
            }
        }

        @Override
        public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
            tag.putIntArray(LINKS, CableBlockEntity.this.links.clone());
        }

        @Override
        public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
            readLinks(tag);
        }

        private void readLinks(final CompoundTag tag) {
            final int[] read = tag.getIntArray(LINKS);
            for (int i = 0; i < CableBlockEntity.this.links.length; i++) {
                CableBlockEntity.this.links[i] = i < read.length ? read[i] : 0;
            }
            CableBlockEntity.this.shape = null;
            CableBlockEntity.this.voxels = null;
        }
    }
}
