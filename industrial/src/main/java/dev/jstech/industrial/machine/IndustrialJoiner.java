/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.machine;

import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.energy.EnergyFaults;
import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.EnergyTooltips;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.energy.WrongClass;
import dev.jstech.core.jstechseries.energy.GridEquipment;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.look.IDescribed;
import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Objects;
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
 * A piece of J's Industrial's grid equipment, a transformer or an HVDC converter station, as its {@link JoinerRating}
 * says (industrial/docs/design/energy.md): a port of its higher class on the face it faces and one of its lower class
 * on its back, each holding two ticks of its ceiling. Each tick it evens what the two hold, moving at most its ceiling,
 * so energy crosses from the side it comes from to the side drawing it, either way, as a transformer steps the voltage
 * up or down and a station rectifies or inverts by the side the energy comes from. It makes and loses nothing; a port
 * a line too high burnt stops it.
 */
public abstract class IndustrialJoiner extends SyncedBlockEntity implements IDescribed {

    private final JoinerRating rating;
    private final EnergyStore low;
    private final EnergyStore high;
    private long moved;

    private static final int TICKS_HELD = 2;

    /** Equipment of {@code rating}. */
    protected IndustrialJoiner(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                               final JoinerRating rating) {
        super(type, pos, state);
        this.rating = Objects.requireNonNull(rating, "rating");
        final long perTick = rating.joulesPerTick();
        final PartField[] fields = new PartField[2];
        this.low = new EnergyStore("Low", JsEnergy.ENERGY, rating.lowerClass(), perTick * TICKS_HELD, perTick,
                perTick, () -> fields[0].changed());
        this.high = new EnergyStore("High", JsEnergy.ENERGY, rating.higherClass(), perTick * TICKS_HELD, perTick,
                perTick, () -> fields[1].changed());
        fields[0] = fields().part("Low", this.low).save();
        fields[1] = fields().part("High", this.high).save();
        this.low.onFault(this::fault);
        this.high.onFault(this::fault);
    }

    /** Gives the equipment of {@code type} its two ports, each on its face. */
    public static <T extends IndustrialJoiner> void register(final RegisterCapabilitiesEvent event,
                                                             final BlockEntityType<T> type) {
        event.registerBlockEntity(EnergyPorts.PORT, type, IndustrialJoiner::port);
    }

    /** One server tick of {@code joiner}, the ticker its block is ticked by. */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final IndustrialJoiner joiner) {
        if (level instanceof ServerLevel) {
            joiner.tick();
        }
    }

    /** Evens what its two ports hold, up to its ceiling, unless a port of it is burnt. */
    public void tick() {
        if (this.low.damaged() || this.high.damaged()) {
            this.moved = 0L;
            return;
        }
        final long even = (this.low.stored() + this.high.stored()) / 2;
        final long upward = Math.clamp(this.low.stored() - even, -this.rating.joulesPerTick(),
                this.rating.joulesPerTick());
        if (upward != 0) {
            this.low.setStored(this.low.stored() - upward);
            this.high.setStored(this.high.stored() + upward);
        }
        this.moved = upward;
    }

    /** What it says of itself. */
    public JoinerRating rating() {
        return this.rating;
    }

    /** The port of its lower class, on its back. */
    public EnergyStore lowPort() {
        return this.low;
    }

    /** The port of its higher class, on the face it faces. */
    public EnergyStore highPort() {
        return this.high;
    }

    /** What crossed it last tick, in joules: from its lower side to its higher above nothing, the other way below. */
    public long moved() {
        return this.moved;
    }

    /** Its port on {@code side}: the higher on the face it faces, the lower on its back, none elsewhere. */
    public @Nullable IEnergyPort port(final @Nullable Direction side) {
        final Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        if (side == facing) {
            return this.high;
        }
        return side == facing.getOpposite() ? this.low : null;
    }

    @Override
    public void describe(final List<Text> lines, final boolean details) {
        lines.add(IndustrialEnergyTexts.JOINS.with(this.rating.lowerClass().name().text(),
                this.rating.higherClass().name().text()));
        lines.add(IndustrialEnergyTexts.CEILING.with(JsEnergy.ENERGY.rate(this.rating.joulesPerTick())));
        final boolean station = this.rating.equipment() == GridEquipment.HVDC_STATION;
        if (this.moved > 0) {
            lines.add((station ? IndustrialEnergyTexts.RECTIFYING : IndustrialEnergyTexts.STEPPING_UP)
                    .with(JsEnergy.ENERGY.rate(this.moved)));
        } else if (this.moved < 0) {
            lines.add((station ? IndustrialEnergyTexts.INVERTING : IndustrialEnergyTexts.STEPPING_DOWN)
                    .with(JsEnergy.ENERGY.rate(-this.moved)));
        } else {
            lines.add(IndustrialEnergyTexts.IDLE.text());
        }
        if (details) {
            final long now = this.level == null ? 0L : this.level.getGameTime();
            lines.addAll(EnergyTooltips.store(this.low, now));
            lines.addAll(EnergyTooltips.store(this.high, now));
        }
    }

    /* A line of the wrong class met a port of it: what befalls the place, as with any block of energy. */
    private void fault(final WrongClass what) {
        if (this.level instanceof ServerLevel server) {
            EnergyFaults.strike(server, this.worldPosition, what);
        }
    }
}
