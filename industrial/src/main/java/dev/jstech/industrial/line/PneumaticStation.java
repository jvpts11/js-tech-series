/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.line;

import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.transit.Capsule;
import dev.jstech.core.transit.CapsuleStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * A station of J's Industrial's pneumatic post ({@link PneumaticPost}): J's Core's capsule station on the mod's tubes,
 * with an air port the Compressed Air Line fills; it shoots a capsule only with a capsule's air in it, and spends that
 * air, so without air the station stops.
 */
public class PneumaticStation extends CapsuleStationBlockEntity {

    private final AirPort air;

    public PneumaticStation(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state, PneumaticPost.TUBES, PneumaticPost.RULES);
        this.air = new AirPort(fields(), PneumaticPost.AIR_A_SEND,
                PneumaticPost.AIR_A_SEND * PneumaticPost.HOLDS_SENDS, this::setChanged);
    }

    /** Gives stations of {@code type} their faces: a station's, and the air port's on every face. */
    public static <T extends PneumaticStation> void registerWithAir(final RegisterCapabilitiesEvent event,
                                                                    final BlockEntityType<T> type) {
        register(event, type);
        MatterCapabilities.block(MatterState.GAS).ifPresent(capability -> event.registerBlockEntity(capability, type,
                (station, side) -> station.airPort().with(null)));
    }

    /** Its air port. */
    public AirPort airPort() {
        return this.air;
    }

    /** A capsule leaves only with its air in the port. */
    @Override
    protected boolean readyToSend(final ServerLevel level, final int stacks) {
        return this.air.ready();
    }

    /** It spends the capsule's air. */
    @Override
    protected void sent(final ServerLevel level, final Capsule capsule) {
        this.air.spend();
    }
}
