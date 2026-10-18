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
import dev.jstech.core.fire.FireSenses;
import dev.jstech.core.heat.HeatReadings;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.signal.SensorBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A fire detector reading what it senses each half second: its alarm first, which a redstone line wired to it gives
 * at once, and then the smoke as a share of the light it takes away, or the air's temperature. A smoke detector calls
 * at a tenth of the light lost, a heat detector at 57 °C, as the common ones do.
 */
public final class FireDetectorBlockEntity extends SensorBlockEntity {

    private double sensed = Double.NaN;

    /** The smoke at which a smoke detector calls, as a share of the light it takes away. */
    public static final double SMOKE_CALLS = 0.1;
    /** The temperature at which a heat detector calls, in kelvin: 57 °C. */
    public static final double HEAT_CALLS = 330.15;

    public FireDetectorBlockEntity(final BlockPos pos, final BlockState state) {
        super(FireSafety.DETECTOR.get(), pos, state);
    }

    @Override
    public List<Reading> readings() {
        final boolean calling = getBlockState().hasProperty(FireDetectorBlock.ALARM)
                && getBlockState().getValue(FireDetectorBlock.ALARM);
        final Reading alarm = new Reading(FireSafetyTexts.ALARM, calling ? 1 : 0, 0, 1, null);
        if (Double.isNaN(this.sensed)) {
            return List.of(alarm);
        }
        return sense() == FireDetectorBlock.Sense.SMOKE
                ? List.of(alarm, new Reading(FireSafetyTexts.SMOKE, this.sensed * 100, 0, 100, FireSafetyTexts.PERCENT))
                : List.of(alarm, HeatReadings.thermometer(this.sensed));
    }

    @Override
    public void read() {
        if (this.level instanceof ServerLevel server) {
            final boolean smoke = sense() == FireDetectorBlock.Sense.SMOKE;
            this.sensed = smoke ? FireSenses.smokeAt(server, this.worldPosition)
                    : FireSenses.heatAt(server, this.worldPosition);
            final boolean calls = smoke ? this.sensed >= SMOKE_CALLS : this.sensed >= HEAT_CALLS;
            final BlockState state = getBlockState();
            if (state.hasProperty(FireDetectorBlock.ALARM) && state.getValue(FireDetectorBlock.ALARM) != calls) {
                server.setBlockAndUpdate(this.worldPosition, state.setValue(FireDetectorBlock.ALARM, calls));
                if (calls) {
                    Audio.cue(server, this.worldPosition, FireSafety.ALARM_CALLS, SoundContext.EMPTY);
                }
            }
        }
        super.read();
    }

    /** What it senses, by its block. */
    public FireDetectorBlock.Sense sense() {
        return getBlockState().getBlock() instanceof FireDetectorBlock detector ? detector.sense()
                : FireDetectorBlock.Sense.SMOKE;
    }
}
