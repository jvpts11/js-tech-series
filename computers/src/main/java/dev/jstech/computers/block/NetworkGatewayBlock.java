/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.menu.NetworkGatewayMenu;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.peripheral.IPeripheralConnectable;
import dev.jstech.core.peripheral.PeripheralCableType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The Network Gateway block: placed facing the player, so its front (the ComputerCraft modem socket and
 * the status plate) looks at whoever placed it and its back (the peripheral cable socket) points at the
 * computer. A right-click opens its buffer; everything else about it is set on the host computer.
 */
public class NetworkGatewayBlock extends DeviceBlock implements IPeripheralConnectable {

    public static final MapCodec<NetworkGatewayBlock> CODEC = simpleCodec(NetworkGatewayBlock::new);

    /** True while the status lights are on: linked to a host, or blinking for Identify. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** The Gateway's block entity, ticking its link and its watches, with its buffer as its menu. */
    private static final Device<NetworkGatewayBlockEntity> DEVICE =
            Device.of(() -> ComputingModule.NETWORK_GATEWAY_BE.get())
                    .ticks(NetworkGatewayBlockEntity::serverTick)
                    .opensMenu(NetworkGatewayMenu::new);

    public NetworkGatewayBlock(final Properties properties) {
        super(properties, DEVICE);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public PeripheralCableType peripheralType() {
        return PeripheralCableType.COMPUTING;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }
}
