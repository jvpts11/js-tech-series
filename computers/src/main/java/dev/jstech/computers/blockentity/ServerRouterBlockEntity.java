/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import com.mojang.serialization.Codec;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.block.DataWires;
import dev.jstech.computers.block.ServerRouterBlock;
import dev.jstech.computers.datacenter.DatacenterSection;
import dev.jstech.computers.datacenter.LoadBalanceMode;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.network.ConnectivityIndex;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.network.ServerRouterElement;
import dev.jstech.core.tier.IndustrialTier;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

/**
 * BlockEntity backing the Server Router, the first network topology element.
 *
 * <p>Its screen reads a summary of the sections it found, declared as menu fields and exposed to the menu through
 * named accessors: the uplink face, the rack count and budget, whether it is over budget, and four values for each
 * of up to {@link #MAX_SECTIONS} sections.
 */
public class ServerRouterBlockEntity extends SyncedBlockEntity {

    private final ValueField<String> customName = fields().value("CustomName", Codec.STRING, "").save();
    private final Map<Direction, LoadBalanceMode> loadBalanceModes = new EnumMap<>(Direction.class);
    // Player-given section names by face, shown by the Cluster Manager instead of "Router · EAST".
    private final Map<Direction, String> sectionNames = new EnumMap<>(Direction.class);
    // Where the next spreading write on each face starts, so round-robin actually takes turns.
    private final Map<Direction, Integer> balanceCursors = new EnumMap<>(Direction.class);
    private final PartField faces = fields().part("Faces", new FacesPart()).save();
    @Nullable
    private NetworkUuid registeredNetwork;
    @Nullable
    private Direction inputFace;
    private List<DatacenterSection> sections = List.of();
    private Set<Long> unmanagedRacks = Set.of();
    private boolean overCapacity;
    private int recomputeCooldown;
    private int warnCooldown;

    // Values the config GUI shows, worked out on the server and mirrored to the client while the menu is open.
    private final DerivedInt inputFaceShown =
            fields().derived("InputFace", () -> inputFace == null ? -1 : inputFace.get3DDataValue()).toMenu();
    private final DerivedInt managedRacksShown = fields().derived("ManagedRacks", this::managedRackCount).toMenu();
    private final DerivedInt maxRacksShown = fields().derived("MaxRacks", this::maxRacks).toMenu();
    private final DerivedInt overCapacityShown = fields().derived("OverCapacity", () -> overCapacity).toMenu();
    private final DerivedInt sectionCountShown =
            fields().derived("SectionCount", () -> Math.min(sections.size(), MAX_SECTIONS)).toMenu();
    /** One row per section: face 3D value, racks, servers, mode id, filled in the constructor. */
    private final DerivedInt[][] sectionShown = new DerivedInt[MAX_SECTIONS][4];

    public static final IndustrialTier TIER = IndustrialTier.T3;

    /** A block has six faces; one is the auto-detected input, leaving at most five output sections. */
    public static final int MAX_SECTIONS = 5;

    private static final int RECOMPUTE_INTERVAL = 20;
    private static final int WARN_INTERVAL = 200;

    public ServerRouterBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.SERVER_ROUTER_BE.get(), pos, state);
        for (int i = 0; i < MAX_SECTIONS; i++) {
            final int section = i;
            sectionShown[i][0] = fields().derived("Section" + i + "Face", () -> sectionFace(section)).toMenu();
            sectionShown[i][1] = fields().derived("Section" + i + "Racks",
                    () -> sectionRow(section, DatacenterSection::rackCount)).toMenu();
            sectionShown[i][2] = fields().derived("Section" + i + "Servers",
                    () -> sectionRow(section, DatacenterSection::serverCount)).toMenu();
            sectionShown[i][3] = fields().derived("Section" + i + "Mode",
                    () -> sectionRow(section, row -> loadBalanceMode(row.face()).id())).toMenu();
        }
        // A router broken or replaced leaves its network's topology at once, not when its chunk unloads.
        fields().whenBroken((level, at) -> onBroken(level));
    }

    public static void serverTick(final Level level, final BlockPos pos,
                                  final BlockState state, final ServerRouterBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.tick(serverLevel);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            DataWires.placeRouter(serverLevel, worldPosition);
        }
    }

    public void recomputeNow() {
        if (level instanceof ServerLevel serverLevel && registeredNetwork != null) {
            recomputeSections(serverLevel, NetworkSystem.get(serverLevel), registeredNetwork);
            recomputeCooldown = RECOMPUTE_INTERVAL;
        }
    }

    public void onBroken(final ServerLevel level) {
        if (registeredNetwork != null) {
            NetworkSystem.get(level).unregisterRouter(registeredNetwork, worldPosition.asLong());
            registeredNetwork = null;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        /*
         * Also unregister on chunk unload, not just on destruction, so the router never lingers in the
         * still-loaded per-level network. onBroken is idempotent.
         */
        if (level instanceof ServerLevel serverLevel) {
            onBroken(serverLevel);
        }
    }

    // Accessors for the config GUI and the Datacenter Station

    /** The uplink face the config GUI shows, as last shown to the open menu. */
    @Nullable
    public Direction inputFaceShown() {
        final int v = inputFaceShown.getAsInt();
        return v < 0 ? null : Direction.from3DDataValue(v);
    }

    /** The managed-rack count the config GUI shows, as last shown to the open menu. */
    public int managedRacksShown() {
        return managedRacksShown.getAsInt();
    }

    /** The rack budget the config GUI shows, as last shown to the open menu. */
    public int maxRacksShown() {
        return maxRacksShown.getAsInt();
    }

    /** Whether the config GUI shows the router over its rack budget, as last shown to the open menu. */
    public boolean overCapacityShown() {
        return overCapacityShown.isSet();
    }

    /** How many section rows the config GUI shows, as last shown to the open menu. */
    public int sectionCountShown() {
        return sectionCountShown.getAsInt();
    }

    /** The face of section row {@code i} the config GUI shows, or null past the last section. */
    @Nullable
    public Direction sectionFaceShown(final int i) {
        if (i < 0 || i >= MAX_SECTIONS) {
            return null;
        }
        final int v = sectionValueShown(i, 0);
        return v < 0 ? null : Direction.from3DDataValue(v);
    }

    /** The rack count of section row {@code i} the config GUI shows, or 0 past the last section. */
    public int sectionRacksShown(final int i) {
        return sectionValueShown(i, 1);
    }

    /** The server count of section row {@code i} the config GUI shows, or 0 past the last section. */
    public int sectionServersShown(final int i) {
        return sectionValueShown(i, 2);
    }

    /** The load-balance mode of section row {@code i} the config GUI shows, or {@code ROUND_ROBIN} past it. */
    public LoadBalanceMode sectionModeShown(final int i) {
        return LoadBalanceMode.byId(sectionValueShown(i, 3));
    }

    /* The value at {@code field} of section row {@code section}'s menu tuple, or 0 for a row out of range. */
    private int sectionValueShown(final int section, final int field) {
        return section >= 0 && section < MAX_SECTIONS ? sectionShown[section][field].getAsInt() : 0;
    }

    public String customName() {
        return customName.get();
    }

    public void setCustomName(@Nullable final String name) {
        customName.set(name == null ? "" : name);
    }

    /**
     * Which server a spreading write on {@code face} should start from, advancing the rotation by one.
     * Kept per face and written with the router, so successive writes really do take turns.
     */
    public int nextBalanceStart(final Direction face) {
        final int start = balanceCursors.getOrDefault(face, 0);
        balanceCursors.put(face, start == Integer.MAX_VALUE ? 0 : start + 1);
        faces.changed();
        return start;
    }

    /** The player's name for the section on {@code face}, or empty when it goes by the router and face. */
    public String sectionName(final Direction face) {
        return sectionNames.getOrDefault(face, "");
    }

    public void setSectionName(final Direction face, @Nullable final String name) {
        if (name == null || name.isEmpty()) {
            sectionNames.remove(face);
        } else {
            sectionNames.put(face, name);
        }
        faces.changed();
    }

    public List<DatacenterSection> sections() {
        return sections;
    }

    @Nullable
    public Direction inputFace() {
        return inputFace;
    }

    public Set<Long> unmanagedRacks() {
        return unmanagedRacks;
    }

    public boolean isOverCapacity() {
        return overCapacity;
    }

    public int maxRacks() {
        return ServerRouterElement.maxRacksFor(TIER);
    }

    public int managedRackCount() {
        int count = 0;
        for (final DatacenterSection section : sections) {
            count += section.rackCount();
        }
        return count;
    }

    public LoadBalanceMode loadBalanceMode(final Direction face) {
        return loadBalanceModes.getOrDefault(face, LoadBalanceMode.ROUND_ROBIN);
    }

    public void cycleLoadBalanceMode(final Direction face) {
        loadBalanceModes.put(face, loadBalanceMode(face).next());
        faces.changed();
    }

    private void tick(final ServerLevel level) {
        final NetworkSystem system = NetworkSystem.get(level);
        final ConnectivityIndex index = system.connectivity();
        final long encodedPos = worldPosition.asLong();
        // Re-register after a chunk reload, mirroring the cable's self-healing index discipline.
        DataWires.placeRouter(level, worldPosition);

        final OptionalLong here = DataWires.routerNumber(level, worldPosition);
        final NetworkUuid network = here.isEmpty() ? null : index.networkOf(here.getAsLong()).orElse(null);
        if (network == null) {
            // Off-network (no UUID yet, or the network collapsed): drop registration and any topology.
            if (registeredNetwork != null) {
                system.unregisterRouter(registeredNetwork, encodedPos);
                registeredNetwork = null;
            }
            sections = List.of();
            unmanagedRacks = Set.of();
            inputFace = null;
            overCapacity = false;
            return;
        }
        if (registeredNetwork != null && !registeredNetwork.equals(network)) {
            system.unregisterRouter(registeredNetwork, encodedPos);
        }
        system.registerRouter(new ServerRouterElement(network, encodedPos, TIER));
        registeredNetwork = network;

        if (--recomputeCooldown <= 0) {
            recomputeCooldown = RECOMPUTE_INTERVAL;
            recomputeSections(level, system, network);
        }
        if (overCapacity && --warnCooldown <= 0) {
            warnCooldown = WARN_INTERVAL;
            JsComputers.LOGGER.warn(
                    "Server Router at {} manages {} racks but its tier budget is {}; {} rack(s) left unmanaged.",
                    worldPosition, managedRackCount(), maxRacks(), unmanagedRacks.size());
        }
    }

    private void recomputeSections(final ServerLevel level, final NetworkSystem system, final NetworkUuid network) {
        final ConnectivityIndex index = system.connectivity();
        final OptionalLong here = DataWires.routerNumber(level, worldPosition);
        final Set<Long> blocked = here.isEmpty() ? Set.of() : Set.of(here.getAsLong());
        final List<DatacenterSection> found = new ArrayList<>();
        final Set<Set<Long>> seenRackSets = new HashSet<>();
        /*
         * The back face is the dedicated uplink to the Mainframe; every other face is a
         * potential datacenter section.
         */
        final Direction uplink = getBlockState().getBlock()
                instanceof ServerRouterBlock
                ? getBlockState().getValue(
                        HorizontalDirectionalBlock.FACING).getOpposite()
                : null;

        for (final Direction face : Direction.values()) {
            if (face == uplink) {
                continue; // the uplink side is never a datacenter section
            }
            final List<Long> wires = DataWires.numbersReaching(level, worldPosition, face,
                    wire -> DataWires.linkOf(wire) != null);
            if (wires.isEmpty()) {
                continue; // no cable on this face
            }
            final Set<Long> branchCables = new LinkedHashSet<>();
            for (final long wire : wires) {
                branchCables.addAll(index.reachableFrom(wire, blocked));
            }
            if (branchCables.isEmpty()) {
                continue;
            }
            final BranchScan scan = scanBranch(level, branchCables);
            if (scan.hasMainframe) {
                continue; // a section branch must not loop back to the Mainframe (miswired), ignore
            }
            if (scan.rackControllers.isEmpty()) {
                continue; // an empty branch carries no datacenter
            }
            if (!seenRackSets.add(scan.rackControllers)) {
                continue; // a looped topology surfaced the same racks on two faces, count once
            }

            final List<NodeUuid> servers = new ArrayList<>();
            long storage = 0L;
            for (final ServerNode server : system.serversOf(network)) {
                final var location = system.locationOf(server.nodeUuid());
                if (location.isPresent() && scan.rackControllers.contains(location.get().rackPos())) {
                    servers.add(server.nodeUuid());
                    storage += server.storageItems();
                }
            }
            found.add(new DatacenterSection(face, new LinkedHashSet<>(scan.rackControllers), servers, storage));
        }

        // Capacity is a single budget across every section: walk the racks in section order and mark
        final int budget = maxRacks();
        final Set<Long> unmanaged = new LinkedHashSet<>();
        int rackCount = 0;
        for (final DatacenterSection section : found) {
            for (final long rack : section.rackPositions()) {
                rackCount++;
                if (rackCount > budget) {
                    unmanaged.add(rack);
                }
            }
        }

        if (this.sections.isEmpty() && !found.isEmpty()) {
            JscEvents.awardOperator(this, JscEvents.DATACENTER_FORMED);
        }
        this.sections = List.copyOf(found);
        this.unmanagedRacks = Set.copyOf(unmanaged);
        this.inputFace = uplink;
        this.overCapacity = !unmanaged.isEmpty();
        for (final DatacenterSection section : found) {
            loadBalanceModes.putIfAbsent(section.face(), LoadBalanceMode.ROUND_ROBIN);
        }
    }

    private BranchScan scanBranch(final ServerLevel level, final Set<Long> branchCables) {
        final Set<Long> racks = new LinkedHashSet<>();
        boolean hasMainframe = false;
        for (final BlockPos cable : Cables.blocksOf(level, branchCables)) {
            for (final Direction direction : Direction.values()) {
                final BlockEntity neighbor = level.getBlockEntity(cable.relative(direction));
                /*
                 * A datacenter is made of Server Racks. A Supercomputer Rack lives on the compute fabric
                 * behind its HBW Interface and is never a section member, even if the branch walk
                 * happens to reach it through that fabric.
                 */
                if (neighbor instanceof ServerRackBlockEntity rack) {
                    if (rack.rackType() != RackChassis.RackType.SUPERCOMPUTER) {
                        racks.add(rack.getBlockPos().asLong());
                    }
                } else if (neighbor instanceof ServerRackPartBlockEntity part && part.controllerPos() != null) {
                    if (Loaded.blockEntity(level, part.controllerPos()) instanceof ServerRackBlockEntity controller
                            && controller.rackType()
                                    != RackChassis.RackType.SUPERCOMPUTER) {
                        racks.add(part.controllerPos().asLong());
                    }
                } else if (neighbor instanceof MainframeBlockEntity || neighbor instanceof MainframePartBlockEntity) {
                    hasMainframe = true;
                }
            }
        }
        return new BranchScan(racks, hasMainframe);
    }

    /* The face of the section in row {@code index} of the summary, or -1 past the last section. */
    private int sectionFace(final int index) {
        return index < Math.min(sections.size(), MAX_SECTIONS) ? sections.get(index).face().get3DDataValue() : -1;
    }

    /* A value of the section in row {@code index} of the summary, or 0 past the last section. */
    private int sectionRow(final int index, final ISectionValue value) {
        return index < Math.min(sections.size(), MAX_SECTIONS) ? value.of(sections.get(index)) : 0;
    }

    /**
     * Result of walking one router branch: the racks it contains and whether it reaches the Mainframe.
     */
    private record BranchScan(Set<Long> rackControllers, boolean hasMainframe) {
    }

    /** One of the values a summary row shows of its section. */
    @FunctionalInterface
    private interface ISectionValue {
        int of(DatacenterSection section);
    }

    /** What the router keeps for each face: the section's name, its balancing mode and its turn. */
    private final class FacesPart implements IFieldPart {

        @Override
        public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
            final CompoundTag names = new CompoundTag();
            sectionNames.forEach((face, name) -> names.putString(face.getName(), name));
            if (!names.isEmpty()) {
                tag.put("SectionNames", names);
            }
            final CompoundTag cursors = new CompoundTag();
            balanceCursors.forEach((face, cursor) -> cursors.putInt(face.getName(), cursor));
            if (!cursors.isEmpty()) {
                tag.put("BalanceCursors", cursors);
            }
            final CompoundTag modes = new CompoundTag();
            for (final Map.Entry<Direction, LoadBalanceMode> entry : loadBalanceModes.entrySet()) {
                modes.putByte(entry.getKey().getName(), (byte) entry.getValue().id());
            }
            if (!modes.isEmpty()) {
                tag.put("LoadBalance", modes);
            }
        }

        @Override
        public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
            sectionNames.clear();
            final CompoundTag names = tag.getCompound("SectionNames");
            balanceCursors.clear();
            final CompoundTag cursors = tag.getCompound("BalanceCursors");
            loadBalanceModes.clear();
            final CompoundTag modes = tag.getCompound("LoadBalance");
            for (final Direction direction : Direction.values()) {
                final String key = direction.getName();
                if (names.contains(key)) {
                    sectionNames.put(direction, names.getString(key));
                }
                if (cursors.contains(key)) {
                    balanceCursors.put(direction, cursors.getInt(key));
                }
                if (modes.contains(key)) {
                    loadBalanceModes.put(direction, LoadBalanceMode.byId(modes.getByte(key)));
                }
            }
        }
    }
}
