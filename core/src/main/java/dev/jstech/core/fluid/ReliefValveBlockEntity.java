/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fluid;

import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.hazard.GasClouds;
import dev.jstech.core.machine.IMachineParameters;
import dev.jstech.core.machine.MachineParameter;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.Loaded;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A pressure relief valve, standing against a vessel of gas with its back: above the pressure it is set to, it lets
 * the excess out of its front, into the store of gas there (a discharge tank), or into the air as a cloud
 * ({@link GasClouds}), as much as its rate a tick, so the vessel never bursts; at or below it, it stays shut. A boiler
 * or any heated vessel that has one does not explode.
 */
@TextHolder
public class ReliefValveBlockEntity extends SyncedBlockEntity implements IMachineParameters {

    private final long rate;
    private final LongField setTo;
    private final LongField vented;
    private @Nullable List<MachineParameter> parameters;

    /** The millibuckets of a gas at the open air's pressure a mole is, as it goes out into the air. */
    public static final double MILLIBUCKETS_A_MOLE = 22.4;
    /** The highest pressure a controller may set it to, in atmospheres. */
    public static final long MOST_PRESSURE = 1_000L;
    public static final TextKey OPENS_ABOVE = TextKey.of("jscore.relief_valve.opens_above", "Opens above");
    public static final TextKey ATMOSPHERES = TextKey.of("jscore.relief_valve.atmospheres", "atm");

    /** A relief valve letting out at most {@code rate} millibuckets a tick, set to {@code pressure} atmospheres. */
    public ReliefValveBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                  final long rate, final long pressure) {
        super(type, pos, state);
        if (rate <= 0 || pressure < 1) {
            throw new IllegalArgumentException("a relief valve lets something out above some pressure, not " + rate
                    + " above " + pressure);
        }
        this.rate = rate;
        this.setTo = fields().longInteger("SetTo", pressure).save().toClient();
        this.vented = fields().longInteger("Vented", 0L).save();
    }

    /** Gives relief valves of {@code type} their values, which a controller reads and sets. */
    public static <T extends ReliefValveBlockEntity> void register(final RegisterCapabilitiesEvent event,
                                                                   final BlockEntityType<T> type) {
        event.registerBlockEntity(IMachineParameters.CAPABILITY, type, (valve, side) -> valve);
    }

    /** One server tick of {@code valve}: above its pressure, the vessel's excess let out. */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final ReliefValveBlockEntity valve) {
        if (level instanceof ServerLevel server) {
            valve.relieve(server);
        }
    }

    /** The pressure it opens above, in atmospheres. */
    public long setTo() {
        return this.setTo.get();
    }

    /** Sets the pressure it opens above, in atmospheres, at least the open air's. */
    public void setTo(final long atmospheres) {
        this.setTo.set(Math.max(1L, atmospheres));
    }

    /** What it has let out in all, in millibuckets. */
    public long vented() {
        return this.vented.get();
    }

    /** What a controller reads and sets of it: the pressure it opens above. */
    @Override
    public List<MachineParameter> parameters() {
        if (this.parameters == null) {
            this.parameters = List.of(MachineParameter.set("set_pressure", OPENS_ABOVE, ATMOSPHERES, 1,
                    MOST_PRESSURE, this.setTo::get, value -> setTo(Math.round(value))));
        }
        return this.parameters;
    }

    /* Lets out of the vessel behind it what its pressure is above the set one, up to its rate. */
    private void relieve(final ServerLevel level) {
        final Direction front = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        final IMatterHandler vessel = gasOf(level, this.worldPosition.relative(front.getOpposite()), front);
        if (vessel == null || vessel.tanks() <= 0 || vessel.pressureNow(0) <= this.setTo.get()) {
            return;
        }
        final Optional<MatterStack> held = vessel.stack(0);
        if (held.isEmpty()) {
            return;
        }
        // What would bring it back to the set pressure, its share of what it holds, up to the valve's rate.
        final double over = 1 - this.setTo.get() / vessel.pressureNow(0);
        final long excess = Math.min(this.rate, Math.max(1L, (long) Math.ceil(held.get().amount() * over)));
        final IMatterHandler ahead = gasOf(level, this.worldPosition.relative(front), front.getOpposite());
        final long fits = ahead == null ? excess : ahead.insert(held.get().withAmount(excess), true);
        final long out = vessel.extract(held.get().material(), fits > 0 ? fits : excess, false);
        if (out <= 0) {
            return;
        }
        final long into = ahead == null ? 0L : ahead.insert(held.get().withAmount(out), false);
        if (out > into) {
            GasClouds.release(level, this.worldPosition, held.get().material(), (out - into) / MILLIBUCKETS_A_MOLE);
        }
        this.vented.set(this.vented.get() + out);
    }

    private static @Nullable IMatterHandler gasOf(final ServerLevel level, final BlockPos at, final Direction face) {
        if (!Loaded.at(level, at)) {
            return null;
        }
        return MatterCapabilities.block(MatterState.GAS).map(capability -> level.getCapability(capability, at, face))
                .orElse(null);
    }
}
