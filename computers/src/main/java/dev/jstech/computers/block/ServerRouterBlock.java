/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.ServerRouterBlockEntity;
import dev.jstech.computers.menu.ServerRouterMenu;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLines;
import dev.jstech.core.jstechseries.network.INetworkBridge;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Server Router: a network topology element that switches the network and groups Server Racks into datacenter
 * sections, one per output face. Its facing is purely cosmetic (the port banks); sections still bind per face
 * regardless.
 *
 * <p>Each era from the Legacy on has its own, like the ordinary routers: it takes the data cables of its era and of
 * every earlier one on any face, and manages more racks the newer it is (the Vintage has no datacenter sections).
 */
public class ServerRouterBlock extends DeviceBlock implements IFaceConnector, INetworkBridge {

    private final Era era;
    private final FacePorts ports;

    public static final MapCodec<ServerRouterBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            EraCodecs.CODEC.fieldOf("era").forGetter(ServerRouterBlock::era)
    ).apply(i, ServerRouterBlock::new));

    /** The router's block entity, ticking to keep its place in the network and its sections. */
    private static final Device<ServerRouterBlockEntity> DEVICE =
            Device.of(() -> ComputingModule.SERVER_ROUTER_BE.get()).ticks(ServerRouterBlockEntity::serverTick);

    public ServerRouterBlock(final Properties properties, final Era era) {
        super(properties, DEVICE);
        this.era = era;
        this.ports = FacePorts.everyFace(DataLines.upTo(era, DataLine.values()));
    }

    /** The era the router was built in: the newest cables it takes, and how many racks it manages. */
    public Era era() {
        return era;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public FacePorts ports() {
        return ports;
    }

    /* Its screen opens with a fresh topology summary and with its name, which the plain device menu does not carry. */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ServerRouterBlockEntity router)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            router.recomputeNow();
            serverPlayer.openMenu(new SimpleMenuProvider(
                            (id, inv, p) -> new ServerRouterMenu(id, inv, router, router.customName()),
                            getName()),
                    buf -> {
                        buf.writeBlockPos(pos);
                        buf.writeUtf(router.customName());
                    });
        }
        return InteractionResult.CONSUME;
    }

    /* The router is a node of the connectivity index as well, which it leaves with the block. */
    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            DataWires.removeRouter(serverLevel, pos);
        }
    }
}
