/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.fire;

import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.SoundContext;
import dev.jstech.core.blockentity.FieldFluidTank;
import dev.jstech.core.blockentity.IFaceGate;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.effect.Effects;
import dev.jstech.core.fire.ExtinguishingAgent;
import dev.jstech.core.fire.Extinguishing;
import dev.jstech.core.fire.FireSenses;
import dev.jstech.core.inventory.FaceFluidHandler;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.inventory.ItemFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A sprinkler head: it holds a little water from its pipe, and every half second feels the air. Past 68 °C, the heat
 * that bursts a common red bulb, it opens and showers a cone below it, putting out the fires there as water does and
 * the flames on whatever burns in it, while it has water. It shuts again after half a minute with no heat.
 */
public final class SprinklerBlockEntity extends SyncedBlockEntity implements IFaceGate {

    private final FieldFluidTank water = fields().fluid("water", HOLDS).save().exposed();
    private int coolFor;

    /** How much water it holds from its pipe, in millibuckets. */
    public static final int HOLDS = 1_000;
    /** The temperature that bursts its bulb, in kelvin: 68 °C. */
    public static final double BURSTS_AT = 341.15;
    /** How much water it showers each time it works, in millibuckets: a hundred a second. */
    public static final int SHOWERS = 50;
    /** Each how many ticks it works. */
    public static final int WORKS_EVERY = 10;
    /* How many times it works with no heat before it shuts: half a minute. */
    private static final int SHUTS_AFTER = 60;
    /* How far below it showers, and how far out at the bottom. */
    private static final int SHOWERS_DOWN = 5;
    private static final double SPREADS = 0.6;

    public SprinklerBlockEntity(final BlockPos pos, final BlockState state) {
        super(FireSafety.SPRINKLER_ENTITY.get(), pos, state);
        this.water.setValidator(fluid -> fluid.is(FluidTags.WATER));
    }

    /** The water it holds. */
    public FieldFluidTank water() {
        return this.water;
    }

    /** Whether its bulb has burst and it showers. */
    public boolean open() {
        return getBlockState().hasProperty(SprinklerBlock.OPEN) && getBlockState().getValue(SprinklerBlock.OPEN);
    }

    /* It takes water in on every side and gives none out: the pipe's line feeds it like any input. */
    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T throughFace(final BlockCapability<T, ?> capability, @Nullable final Direction side,
                                       @Nullable final T whole) {
        if (capability != Capabilities.FluidHandler.BLOCK || whole == null) {
            return whole;
        }
        return (T) new FaceFluidHandler((IFluidHandler) whole, FaceMode.IN, ItemFilter.EVERYTHING);
    }

    /** Feels the air, opens or shuts, and showers while it is open and has water. */
    public void work() {
        if (!(this.level instanceof ServerLevel server)) {
            return;
        }
        final boolean hot = FireSenses.heatAt(server, this.worldPosition) >= BURSTS_AT;
        this.coolFor = hot ? 0 : this.coolFor + 1;
        if (hot && !open()) {
            server.setBlockAndUpdate(this.worldPosition, getBlockState().setValue(SprinklerBlock.OPEN, true));
        } else if (open() && this.coolFor >= SHUTS_AFTER) {
            server.setBlockAndUpdate(this.worldPosition, getBlockState().setValue(SprinklerBlock.OPEN, false));
        }
        if (open() && this.water.getFluidAmount() >= SHOWERS) {
            this.water.drain(SHOWERS, IFluidHandler.FluidAction.EXECUTE);
            shower(server);
        }
    }

    /* Showers the cone below: each fire in it as water puts it out, each body burning in it stops burning. */
    private void shower(final ServerLevel server) {
        for (int down = 1; down <= SHOWERS_DOWN; down++) {
            final int out = (int) Math.round(down * SPREADS);
            for (int x = -out; x <= out; x++) {
                for (int z = -out; z <= out; z++) {
                    final BlockPos at = this.worldPosition.offset(x, -down, z);
                    if (server.isLoaded(at) && server.getBlockState(at).getBlock() instanceof BaseFireBlock) {
                        Extinguishing.extinguish(server, at, ExtinguishingAgent.WATER, null);
                    }
                }
            }
        }
        final AABB cone = new AABB(this.worldPosition).inflate(SHOWERS_DOWN * SPREADS, 0, SHOWERS_DOWN * SPREADS)
                .expandTowards(0, -SHOWERS_DOWN, 0);
        for (final Entity body : server.getEntities((Entity) null, cone, Entity::isOnFire)) {
            body.clearFire();
        }
        Effects.at(server, FireSafety.SHOWER, Vec3.atCenterOf(this.worldPosition).add(0, -0.4, 0))
                .facing(new Vec3(0, -1, 0)).send();
        Audio.cue(server, this.worldPosition, FireSafety.SHOWERING, SoundContext.EMPTY);
    }
}
