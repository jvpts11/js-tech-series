/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.codec.StreamCodec;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/**
 * What a rocket's cabin shows of its flight, as its live panel reads it: the mission's clock, the propellant the
 * burning stage has left, how fast it climbs, the throttle, its attitude, the cabin's air, the propellant left in the
 * whole rocket, and the panel's lamps lit. Made by whatever flies the rocket; one standing still on the ground shows
 * {@link #AT_REST}.
 *
 * @param clock      the mission's clock, in seconds: below nought, counting down to lift-off
 * @param fuel       the share of its fuel the burning stage has left, 0 to 1
 * @param oxidiser   the share of its oxidiser the burning stage has left, 0 to 1
 * @param climb      how fast it climbs, in blocks a second; below nought, falling
 * @param throttle   the throttle's setting, 0 to 1
 * @param pitch      how far it is turned about its pitch axis from how it stood on the pad, in degrees
 * @param roll       how far it is turned about its roll axis from how it stood on the pad, in degrees
 * @param heading    how far it is turned about the world's up from how it stood on the pad, in degrees
 * @param air        the cabin's air, as a share of the Overworld's at sea level, 0 to 2
 * @param propellant the share of all its propellant the rocket has left, 0 to 1
 * @param lit        the panel's lamps lit
 */
public record CabinReadings(int clock, float fuel, float oxidiser, float climb, float throttle, float pitch,
                            float roll, float heading, float air, float propellant, Set<CabinLamp> lit) {

    /** A rocket standing still on the ground, its tanks full, before its count has started. */
    public static final CabinReadings AT_REST = new CabinReadings(0, 1, 1, 0, 0, 0, 0, 0, 1, 1, Set.of());
    /** How fast it climbs or falls at the ends of the climb's strip, in blocks a second. */
    public static final float CLIMB_SCALE = 50;
    private static final float MOST_AIR = 2;
    private static final int MINUTE = 60;
    private static final int HOUR = 3600;
    /* The clock shows minutes and seconds to this many minutes, then hours and minutes to this many hours. */
    private static final int MOST_MINUTES = 99;
    private static final int MOST_HOURS = 99;
    private static final int TOP_MINUTE = 59;
    public static final Codec<CabinReadings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("clock").forGetter(CabinReadings::clock),
            Codec.FLOAT.fieldOf("fuel").forGetter(CabinReadings::fuel),
            Codec.FLOAT.fieldOf("oxidiser").forGetter(CabinReadings::oxidiser),
            Codec.FLOAT.fieldOf("climb").forGetter(CabinReadings::climb),
            Codec.FLOAT.fieldOf("throttle").forGetter(CabinReadings::throttle),
            Codec.FLOAT.fieldOf("pitch").forGetter(CabinReadings::pitch),
            Codec.FLOAT.fieldOf("roll").forGetter(CabinReadings::roll),
            Codec.FLOAT.fieldOf("heading").forGetter(CabinReadings::heading),
            Codec.FLOAT.fieldOf("air").forGetter(CabinReadings::air),
            Codec.FLOAT.fieldOf("propellant").forGetter(CabinReadings::propellant),
            StableCodecs.byName(CabinLamp.class).listOf().<Set<CabinLamp>>xmap(CabinReadings::lamps, List::copyOf)
                    .optionalFieldOf("lit", Set.of()).forGetter(CabinReadings::lit)
    ).apply(instance, CabinReadings::new));
    /** How it is sent to a player, its lamps as one bit each, by each lamp's number. */
    public static final StreamCodec<ByteBuf, CabinReadings> STREAM_CODEC = StreamCodec.of(CabinReadings::write,
            CabinReadings::read);

    public CabinReadings {
        fuel = share(fuel);
        oxidiser = share(oxidiser);
        throttle = share(throttle);
        propellant = share(propellant);
        air = Math.max(0, Math.min(MOST_AIR, air));
        lit = lamps(lit);
    }

    /**
     * The turn of {@code turn}, how a rocket stands now from how it stood on the pad, as the cabin reads it: about the
     * world's up first, its heading; then about its pitch axis; then about its roll axis.
     */
    public static Attitude attitude(final Quaternionfc turn) {
        final Vector3f angles = turn.getEulerAnglesYXZ(new Vector3f());
        return new Attitude((float) Math.toDegrees(angles.x), (float) Math.toDegrees(angles.z),
                (float) Math.toDegrees(angles.y));
    }

    /** The same readings, its attitude {@code turned}. */
    public CabinReadings turned(final Attitude turned) {
        return new CabinReadings(this.clock, this.fuel, this.oxidiser, this.climb, this.throttle, turned.pitch(),
                turned.roll(), turned.heading(), this.air, this.propellant, this.lit);
    }

    /** The same readings, the mission's clock at {@code seconds}: below nought, counting down to lift-off. */
    public CabinReadings clocked(final int seconds) {
        return new CabinReadings(seconds, this.fuel, this.oxidiser, this.climb, this.throttle, this.pitch,
                this.roll, this.heading, this.air, this.propellant, this.lit);
    }

    /** Whether the lamp {@code lamp} is lit. */
    public boolean lit(final CabinLamp lamp) {
        return this.lit.contains(lamp);
    }

    /**
     * The clock as its four digits show it, its sign first: minutes and seconds to 99 minutes, hours and minutes after
     * that, to 99 hours.
     */
    public String timer() {
        final int whole = Math.abs(this.clock);
        final char sign = this.clock < 0 ? '-' : '+';
        final int high;
        final int low;
        if (whole < (MOST_MINUTES + 1) * MINUTE) {
            high = whole / MINUTE;
            low = whole % MINUTE;
        } else if (whole < (MOST_HOURS + 1) * HOUR) {
            high = whole / HOUR;
            low = whole % HOUR / MINUTE;
        } else {
            high = MOST_HOURS;
            low = TOP_MINUTE;
        }
        return sign + two(high) + ":" + two(low);
    }

    /** Where the climb's strip points, from 0 falling fastest to 1 climbing fastest, still at the middle. */
    public float climbShare() {
        return share(0.5F + this.climb / (2 * CLIMB_SCALE));
    }

    private static String two(final int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }

    private static float share(final float value) {
        return Float.isNaN(value) ? 0 : Math.max(0, Math.min(1, value));
    }

    private static Set<CabinLamp> lamps(final Collection<CabinLamp> lamps) {
        final EnumSet<CabinLamp> out = EnumSet.noneOf(CabinLamp.class);
        out.addAll(lamps);
        return Collections.unmodifiableSet(out);
    }

    private static void write(final ByteBuf buf, final CabinReadings readings) {
        buf.writeInt(readings.clock);
        buf.writeFloat(readings.fuel);
        buf.writeFloat(readings.oxidiser);
        buf.writeFloat(readings.climb);
        buf.writeFloat(readings.throttle);
        buf.writeFloat(readings.pitch);
        buf.writeFloat(readings.roll);
        buf.writeFloat(readings.heading);
        buf.writeFloat(readings.air);
        buf.writeFloat(readings.propellant);
        int bits = 0;
        for (final CabinLamp lamp : readings.lit) {
            bits |= 1 << lamp.id();
        }
        buf.writeShort(bits);
    }

    private static CabinReadings read(final ByteBuf buf) {
        final int clock = buf.readInt();
        final float fuel = buf.readFloat();
        final float oxidiser = buf.readFloat();
        final float climb = buf.readFloat();
        final float throttle = buf.readFloat();
        final float pitch = buf.readFloat();
        final float roll = buf.readFloat();
        final float heading = buf.readFloat();
        final float air = buf.readFloat();
        final float propellant = buf.readFloat();
        final int bits = buf.readShort();
        final EnumSet<CabinLamp> lit = EnumSet.noneOf(CabinLamp.class);
        for (final CabinLamp lamp : CabinLamp.BANK) {
            if ((bits & 1 << lamp.id()) != 0) {
                lit.add(lamp);
            }
        }
        return new CabinReadings(clock, fuel, oxidiser, climb, throttle, pitch, roll, heading, air, propellant, lit);
    }

    /**
     * How a rocket is turned from how it stood on the pad, as its cabin reads it.
     *
     * @param pitch   about its pitch axis, in degrees
     * @param roll    about its roll axis, in degrees
     * @param heading about the world's up, in degrees
     */
    public record Attitude(float pitch, float roll, float heading) {
    }
}
