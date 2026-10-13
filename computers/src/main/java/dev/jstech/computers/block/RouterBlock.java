/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.blockentity.RouterBlockEntity;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.network.INetworkBridge;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A router of the data network, a plain block with six equal faces, each a place for a cable.
 *
 * <p>Each era has its router, joining its access line to its backbone: the cables of its era and of every earlier one
 * on any face, all of them one network. A router extends and joins the network and never splits it. Two eras of a line
 * meet here, at the router of the newer one.
 *
 * <p>The optical router is the router of the fibre: it takes only the backbone's fibre, from the Standard's to its own
 * era's, and it is what turns and branches it, since fibre runs only straight.
 */
public class RouterBlock extends Block implements EntityBlock, IFaceConnector, INetworkBridge {

    private final HardwareEra era;
    private final boolean optical;
    private final FacePorts ports;

    public static final MapCodec<RouterBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            StableCodecs.byName(HardwareEra.class).fieldOf("era").forGetter(RouterBlock::era),
            Codec.BOOL.optionalFieldOf("optical", false).forGetter(RouterBlock::optical)
    ).apply(i, RouterBlock::new));

    public RouterBlock(final Properties properties, final HardwareEra era, final boolean optical) {
        super(properties);
        this.era = era;
        this.optical = optical;
        this.ports = optical ? FacePorts.everyFace(DataLines.upTo(era, DataLine.BACKBONE))
                : FacePorts.everyFace(DataLines.upTo(era, DataLine.ACCESS, DataLine.BACKBONE));
    }

    /** The era the router belongs to: the newest cables it takes. */
    public HardwareEra era() {
        return era;
    }

    /** Whether it is the router of the fibre. */
    public boolean optical() {
        return optical;
    }

    @Override
    protected MapCodec<? extends RouterBlock> codec() {
        return CODEC;
    }

    @Override
    public FacePorts ports() {
        return ports;
    }

    /* The optical router takes the backbone's fibre and nothing older: the copper backbone has routers of its own. */
    @Override
    public boolean accepts(final BlockState state, final Direction face, final Connection offered) {
        if (!IFaceConnector.super.accepts(state, face, offered)) {
            return false;
        }
        if (!optical) {
            return true;
        }
        final DataLink link = DataLines.linkOf(offered);
        return link != null && link.straight();
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            DataWires.removeRouter(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new RouterBlockEntity(pos, state);
    }
}
