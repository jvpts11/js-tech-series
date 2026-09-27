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
import dev.jstech.computers.block.DataCableBlock;
import dev.jstech.computers.block.ServerRouterBlock;
import dev.jstech.computers.datacenter.DatacenterSection;
import dev.jstech.computers.datacenter.LoadBalanceMode;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.network.ConnectivityIndex;
import dev.jstech.core.network.INetworkBridge;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.network.ServerRouterElement;
import dev.jstech.core.tier.IndustrialTier;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerData;
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
import java.util.Set;

/**
 * BlockEntity backing the Server Router, the first network topology element.
 *
 * <p>Its screen reads a summary of the sections it found, declared as menu fields in the order of the {@code DATA_}
 * indices: the uplink face, the rack count and budget, whether it is over budget, and four values for each of up to
 * {@link #MAX_SECTIONS} sections.
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

    public static final IndustrialTier TIER = IndustrialTier.T3;

    // The summary the config GUI reads, in menu order; section rows are flattened after the five totals.
    public static final int DATA_INPUT_FACE = 0;      // input face 3D value, or -1 if none
    public static final int DATA_MANAGED_RACKS = 1;
    public static final int DATA_MAX_RACKS = 2;
    public static final int DATA_OVER_CAPACITY = 3;   // 0 or 1
    public static final int DATA_SECTION_COUNT = 4;
    public static final int DATA_SECTION_BASE = 5;
    public static final int DATA_PER_SECTION = 4;     // face 3D value, racks, servers, mode id
    public static final int MAX_SECTIONS = 5;         // the 6 faces minus the one input face
    public static final int DATA_COUNT = DATA_SECTION_BASE + MAX_SECTIONS * DATA_PER_SECTION;

    private static final int RECOMPUTE_INTERVAL = 20;
    private static final int WARN_INTERVAL = 200;

    public ServerRouterBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.SERVER_ROUTER_BE.get(), pos, state);
        fields().derived("InputFace", () -> inputFace == null ? -1 : inputFace.get3DDataValue()).toMenu();
        fields().derived("ManagedRacks", () -> managedRackCount()).toMenu();
        fields().derived("MaxRacks", () -> maxRacks()).toMenu();
        fields().derived("OverCapacity", () -> overCapacity).toMenu();
        fields().derived("SectionCount", () -> Math.min(sections.size(), MAX_SECTIONS)).toMenu();
        for (int i = 0; i < MAX_SECTIONS; i++) {
            final int section = i;
            fields().derived("Section" + i + "Face", () -> sectionFace(section)).toMenu();
            fields().derived("Section" + i + "Racks",
                    () -> sectionRow(section, DatacenterSection::rackCount)).toMenu();
            fields().derived("Section" + i + "Servers",
                    () -> sectionRow(section, DatacenterSection::serverCount)).toMenu();
            fields().derived("Section" + i + "Mode",
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
            final ConnectivityIndex index = NetworkSystem.get(serverLevel).connectivity();
            final long encodedPos = worldPosition.asLong();
            if (!index.contains(encodedPos)) {
                index.onCablePlaced(encodedPos, bridgeNeighbors(serverLevel));
            }
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

    public ContainerData getDataAccess() {
        return fields().menuData();
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

    private Set<Long> bridgeNeighbors(final ServerLevel serverLevel) {
        final Set<Long> neighbors = new HashSet<>();
        for (final Direction direction : Direction.values()) {
            final BlockPos neighborPos = worldPosition.relative(direction);
            final var block = serverLevel.getBlockState(neighborPos).getBlock();
            if (block instanceof DataCableBlock || block instanceof INetworkBridge) {
                neighbors.add(neighborPos.asLong());
            }
        }
        return neighbors;
    }

    private void tick(final ServerLevel level) {
        final NetworkSystem system = NetworkSystem.get(level);
        final ConnectivityIndex index = system.connectivity();
        final long encodedPos = worldPosition.asLong();
        // Re-register after a chunk reload, mirroring the cable's self-healing index discipline.
        if (!index.contains(encodedPos)) {
            index.onCablePlaced(encodedPos, bridgeNeighbors(level));
        }

        final NetworkUuid network = index.networkOf(encodedPos).orElse(null);
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
        final Set<Long> blocked = Set.of(worldPosition.asLong());
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
            final long neighbor = worldPosition.relative(face).asLong();
            if (!index.contains(neighbor)) {
                continue; // no cable on this face
            }
            final Set<Long> branchCables = index.reachableFrom(neighbor, blocked);
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
        for (final long cablePos : branchCables) {
            final BlockPos cable = BlockPos.of(cablePos);
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
                    if (level.getBlockEntity(part.controllerPos()) instanceof ServerRackBlockEntity controller
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
