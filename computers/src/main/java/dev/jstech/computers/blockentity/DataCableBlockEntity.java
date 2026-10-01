/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.DataCableBlock;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.multipart.FaceParts;
import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.multipart.IPartHost;
import dev.jstech.core.multipart.ModelLayout;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.network.ConnectivityIndex;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.network.INetworkBridge;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.persistence.ISaveUpgrade;
import dev.jstech.core.persistence.SaveLayout;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * BlockEntity backing a {@link DataCableBlock}: the parts mounted on its faces, saved whole, of which the players who
 * see it are sent only which kind sits on which face, enough to draw and shape the cable.
 *
 * <p>Its layout is at version 2: version 1 saved each part's kind as a number of the cable's own, version 2 by the
 * id the kind is registered under with the Core's parts.
 */
public class DataCableBlockEntity extends SyncedBlockEntity implements IPartHost {

    private final FaceParts parts = new FaceParts(this);
    private final PartField mounted = fields().part(FaceParts.KEY, parts).save().toClient();
    @Nullable
    private NetworkUuid loadedNetwork;

    /** The cable's layout: the parts' kinds by their ids since version 2. */
    public static final SaveLayout LAYOUT = SaveLayout.builder(JsComputers.MODID + ":data_cable")
            .version(2)
            .upgrade(1, ISaveUpgrade.compound(DataCableBlockEntity::kindsByTheirIds))
            .build();

    public DataCableBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.DATA_CABLE_BE.get(), pos, state);
        fields().layout(LAYOUT);
        fields().part("Network", new NetworkPart()).save();
    }

    public static void serverTick(final Level level, final BlockPos pos,
                                  final BlockState state, final DataCableBlockEntity cable) {
        if (level instanceof ServerLevel) {
            cable.parts.tick();
        }
    }

    public DataTier tier() {
        return getBlockState().getBlock() instanceof DataCableBlock cable
                ? cable.tier()
                : DataTier.T1_ETHERNET;
    }

    // What the parts see of the cable

    @Override
    public @Nullable Level partLevel() {
        return level;
    }

    @Override
    public BlockPos partPos() {
        return worldPosition;
    }

    @Override
    public void partChanged() {
        mounted.changed();
    }

    // Helpers the parts use to reach the world, the network and neighbors

    @Nullable
    public ServerLevel serverLevel() {
        return level instanceof ServerLevel serverLevel ? serverLevel : null;
    }

    @Nullable
    public NetworkUuid network() {
        final ServerLevel serverLevel = serverLevel();
        if (serverLevel == null) {
            return null;
        }
        return NetworkSystem.get(serverLevel).connectivity().networkOf(worldPosition.asLong()).orElse(null);
    }

    @Nullable
    public MainframeBlockEntity mainframe() {
        final ServerLevel serverLevel = serverLevel();
        final NetworkUuid network = network();
        if (serverLevel == null || network == null) {
            return null;
        }
        return NetworkSystem.get(serverLevel).mainframePositionOf(network)
                .map(pos -> serverLevel.getBlockEntity(BlockPos.of(pos)))
                .filter(MainframeBlockEntity.class::isInstance)
                .map(MainframeBlockEntity.class::cast)
                .orElse(null);
    }

    public ExternalDataPort neighborPort(final Direction face) {
        final ServerLevel serverLevel = serverLevel();
        if (serverLevel == null) {
            return new ExternalDataPort(null, null);
        }
        // Every kind of data the faced block holds: a bus moves whatever is there.
        return ExternalDataPort.at(
                serverLevel, worldPosition.relative(face), face.getOpposite());
    }

    // Part hosting

    /** The parts on the cable's faces. */
    public FaceParts parts() {
        return parts;
    }

    public boolean hasPart(final Direction face) {
        return parts.has(face);
    }

    @Nullable
    public PartType<?> partType(final Direction face) {
        return parts.type(face);
    }

    @Nullable
    public IFacePart getPart(final Direction face) {
        return parts.get(face);
    }

    public void addPart(final Direction face, final IFacePart part) {
        parts.add(face, part);
    }

    @Nullable
    public IFacePart removePart(final Direction face) {
        return parts.remove(face);
    }

    public boolean hasAnyPart() {
        return parts.any();
    }

    public void dropAllParts(final ServerLevel serverLevel) {
        parts.dropAll(serverLevel, worldPosition);
    }

    public void dropAllBuffers(final ServerLevel serverLevel) {
        parts.dropContents(serverLevel);
    }

    // Connectivity (transient runtime index)

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            final ConnectivityIndex index = NetworkSystem.get(serverLevel).connectivity();
            final long encodedPos = worldPosition.asLong();
            if (!index.contains(encodedPos)) {
                index.onCablePlaced(encodedPos, networkNeighbors(serverLevel), tier());
            }
            // Restore this cable's persisted network identity when its segment has none yet.
            if (loadedNetwork != null && index.networkOf(encodedPos).isEmpty()) {
                index.assignUuid(encodedPos, loadedNetwork);
            }
        }
    }

    /* The parts changed on the client: the render mesh and the collision and selection shape follow them. */
    @Override
    protected void afterClientUpdate() {
        requestModelDataUpdate();
        if (level != null) {
            final BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /* The cable's model draws the parts on its faces from this, into the world's mesh. */
    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(ModelLayout.PROPERTY, parts.layout()).build();
    }

    private Set<Long> networkNeighbors(final ServerLevel serverLevel) {
        final Set<Long> neighbors = new HashSet<>();
        final DataTier myTier = tier();
        for (final Direction direction : Direction.values()) {
            final BlockPos neighborPos = worldPosition.relative(direction);
            final var block = serverLevel.getBlockState(neighborPos).getBlock();
            if (block instanceof DataCableBlock other && other.tier() == myTier) {
                neighbors.add(neighborPos.asLong());
            } else if (block instanceof INetworkBridge) {
                neighbors.add(neighborPos.asLong());
            }
        }
        return neighbors;
    }

    /* Version 1 to 2: each part's kind, saved as a number of the cable's own, becomes the id it is registered by. */
    private static CompoundTag kindsByTheirIds(final CompoundTag cable) {
        for (final Tag element : cable.getList(FaceParts.KEY, Tag.TAG_COMPOUND)) {
            final CompoundTag entry = (CompoundTag) element;
            if (entry.get("Type") instanceof NumericTag number) {
                final String id = ComputingParts.FORMER_NUMBERS.get(number.getAsInt());
                if (id != null) {
                    entry.putString("Type", id);
                }
            }
        }
        return cable;
    }

    /**
     * The network identity of the cable's segment, saved so an orphaned segment keeps it across a reload; the live
     * index is what holds it while the world runs.
     */
    private final class NetworkPart implements IFieldPart {

        @Override
        public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
            final NetworkUuid network = network();
            if (network != null) {
                tag.putString("Network", network.asString());
            }
        }

        @Override
        public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
            loadedNetwork = tag.contains("Network") ? NetworkUuid.fromString(tag.getString("Network")) : null;
        }
    }
}
