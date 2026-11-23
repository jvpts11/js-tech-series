/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.lines;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.hazard.GasClouds;
import dev.jstech.core.heat.HeatMath;
import dev.jstech.core.heat.Thermal;
import dev.jstech.core.jstechseries.SharedCatalogue;
import dev.jstech.core.jstechseries.hazard.CarbonDioxide;
import dev.jstech.core.look.IDescribed;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A Gas Vent: the end of a gas line that lets what reaches it out into the air, so a machine whose by-product nobody
 * wants never stops full. It takes any gas through the inlet on its front, all the line brings, and once a second lets
 * what came out above it as a cloud of it, which does to whoever stands in it what the gas does ({@link GasClouds}), or
 * into the sealed room it stands in; carbon dioxide goes into the world's air as well ({@link CarbonDioxide}). The gas
 * puffs out from under its cap in its colour.
 */
@TextHolder
public final class GasVentBlockEntity extends SyncedBlockEntity implements IDescribed {

    private final Map<Material, Long> coming = new LinkedHashMap<>();
    private final IMatterHandler inlet = new Inlet();
    private List<MatterStack> lastSecond = List.of();
    private int ticks;

    public static final TextKey LETTING_OUT = TextKey.of("jscore.gas_vent.letting_out",
            "Letting out %s mB of %s a second");
    public static final TextKey IDLE = TextKey.of("jscore.gas_vent.idle", "Letting nothing out");
    /* How often it lets out what came, in ticks; the millibuckets a puff under its cap stands for, and the most. */
    private static final int SECOND = 20;
    private static final long PER_PUFF = 50L;
    private static final int MOST_PUFFS = 8;
    /* What its inlet says it can hold: more than any line brings, since what comes goes out. */
    private static final long ROOM = 1_000_000_000L;
    private static final double CAP_HEIGHT = 15.0 / 16.0;
    private static final double PUFF_SPREAD = 0.25;
    private static final double PUFF_RISE = 0.05;
    private static final double PUFF_SPEED = 0.01;

    public GasVentBlockEntity(final BlockPos pos, final BlockState state) {
        super(SharedPipes.GAS_VENT_BE.get(), pos, state);
    }

    /** What a gas line sees of it on {@code side}: its inlet on its front, and nothing on the others. */
    public @Nullable IMatterHandler inlet(final @Nullable Direction side) {
        return side == null || side == getBlockState().getValue(HorizontalDirectionalBlock.FACING) ? this.inlet : null;
    }

    /** One server tick: once a second, what came is let out. */
    public void tick() {
        if (++this.ticks >= SECOND && this.level instanceof ServerLevel server) {
            this.ticks = 0;
            letOut(server);
        }
    }

    @Override
    public void describe(final List<Text> lines, final boolean details) {
        if (this.lastSecond.isEmpty()) {
            lines.add(IDLE.text());
        }
        for (final MatterStack gas : this.lastSecond) {
            lines.add(LETTING_OUT.with(gas.amount(), gas.material().name()));
        }
    }

    /* Lets out what came in the second gone: a cloud of each gas above it, the world's carbon dioxide, its puffs. */
    private void letOut(final ServerLevel level) {
        final List<MatterStack> out = new ArrayList<>(this.coming.size());
        this.coming.forEach((gas, amount) -> out.add(new MatterStack(gas, amount)));
        this.coming.clear();
        this.lastSecond = List.copyOf(out);
        final BlockPos above = this.worldPosition.above();
        for (final MatterStack gas : out) {
            final double moles = gas.amount() / HeatMath.LITRES_PER_MOLE;
            GasClouds.release(level, above, gas.material(), moles);
            if (gas.material() == SharedCatalogue.CARBON_DIOXIDE) {
                CarbonDioxide.emit(moles * Thermal.of(gas.material(), false).molarMass());
            }
            final int puffs = (int) Math.min(MOST_PUFFS, Math.max(1L, gas.amount() / PER_PUFF));
            level.sendParticles(GasClouds.puff(gas.material()), this.worldPosition.getX() + 0.5,
                    this.worldPosition.getY() + CAP_HEIGHT, this.worldPosition.getZ() + 0.5, puffs, PUFF_SPREAD,
                    PUFF_RISE, PUFF_SPREAD, PUFF_SPEED);
        }
    }

    /* The inlet: takes all of any gas offered, holds none of it and gives nothing back. */
    private final class Inlet implements IMatterHandler {

        @Override
        public MatterState state() {
            return MatterState.GAS;
        }

        @Override
        public int tanks() {
            return 1;
        }

        @Override
        public Optional<MatterStack> stack(final int tank) {
            return Optional.empty();
        }

        @Override
        public long capacity(final int tank) {
            return ROOM;
        }

        @Override
        public long insert(final MatterStack stack, final boolean simulate) {
            if (stack.amount() <= 0 || !stack.material().takes(MatterState.GAS)) {
                return 0L;
            }
            if (!simulate) {
                coming.merge(stack.material(), stack.amount(), Long::sum);
            }
            return stack.amount();
        }

        @Override
        public long extract(final Material material, final long amount, final boolean simulate) {
            return 0L;
        }

        @Override
        public Optional<MatterStack> extractAny(final long amount, final boolean simulate) {
            return Optional.empty();
        }

        @Override
        public boolean givesOut(final int tank) {
            return false;
        }
    }
}
