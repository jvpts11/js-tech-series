/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A part a vehicle's place takes, as a data pack or a mod declares it: its kind, the name players read (a translation
 * key), the item drawn for it, the item that mends it, its mass, and what it does, said by the one spec of its kind. A
 * part is an item while it is out of a vehicle, and keeps how worn it is.
 *
 * <pre>{@code
 * {"kind": "engine", "name": "item.mymod.diesel_engine", "mass": 180, "repair": "minecraft:iron_ingot",
 *  "engine": {"power": 90000, "idle_rpm": 800, "max_rpm": 4500, "efficiency": 0.3, "fuels": ["mymod:diesel"]}}
 * }</pre>
 *
 * @param kind      the kind of place it goes in
 * @param name      the translation key of its name
 * @param icon      the item drawn for it as an item, or none for the plain part
 * @param repair    the item that mends it, used with a wrench, or none when it is only ever replaced
 * @param mass      its mass, in kilograms
 * @param engine    what an engine does
 * @param wheel     what a wheel does
 * @param tank      what a fuel tank holds
 * @param battery   what a battery holds
 * @param gearbox   a gearbox's gears
 * @param light     what a light lights
 * @param horn      what a horn sounds
 * @param rotor     what a rotor lifts
 * @param propeller what a propeller pushes
 */
public record VehiclePartDef(VehiclePartKind kind, String name, Optional<ResourceLocation> icon,
                             Optional<ResourceLocation> repair, double mass, Optional<Engine> engine,
                             Optional<Wheel> wheel, Optional<Tank> tank, Optional<Battery> battery,
                             Optional<Gearbox> gearbox, Optional<Light> light, Optional<Horn> horn,
                             Optional<Thrust> rotor, Optional<Thrust> propeller) {

    /** A colour as {@code #RRGGBB}. */
    public static final Codec<Integer> COLOUR = Codec.STRING.comapFlatMap(VehiclePartDef::readColour,
            colour -> String.format(Locale.ROOT, "#%06X", colour & 0xFFFFFF));

    public static final Codec<VehiclePartDef> CODEC = RecordCodecBuilder.<VehiclePartDef>create(instance ->
            instance.group(
                    StableCodecs.byName(VehiclePartKind.class).fieldOf("kind").forGetter(VehiclePartDef::kind),
                    Codec.STRING.fieldOf("name").forGetter(VehiclePartDef::name),
                    ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(VehiclePartDef::icon),
                    ResourceLocation.CODEC.optionalFieldOf("repair").forGetter(VehiclePartDef::repair),
                    Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("mass", 0.0)
                            .forGetter(VehiclePartDef::mass),
                    Engine.CODEC.optionalFieldOf("engine").forGetter(VehiclePartDef::engine),
                    Wheel.CODEC.optionalFieldOf("wheel").forGetter(VehiclePartDef::wheel),
                    Tank.CODEC.optionalFieldOf("tank").forGetter(VehiclePartDef::tank),
                    Battery.CODEC.optionalFieldOf("battery").forGetter(VehiclePartDef::battery),
                    Gearbox.CODEC.optionalFieldOf("gearbox").forGetter(VehiclePartDef::gearbox),
                    Light.CODEC.optionalFieldOf("light").forGetter(VehiclePartDef::light),
                    Horn.CODEC.optionalFieldOf("horn").forGetter(VehiclePartDef::horn),
                    Thrust.CODEC.optionalFieldOf("rotor").forGetter(VehiclePartDef::rotor),
                    Thrust.CODEC.optionalFieldOf("propeller").forGetter(VehiclePartDef::propeller))
            .apply(instance, VehiclePartDef::new)).validate(VehiclePartDef::check);

    public VehiclePartDef {
        if (name.isBlank()) {
            throw new IllegalArgumentException("a vehicle part has a name");
        }
    }

    /** An engine. */
    public static VehiclePartDef engine(final String name, final double mass, final Engine engine) {
        return of(VehiclePartKind.ENGINE, name, mass).with(engine);
    }

    /** A wheel. */
    public static VehiclePartDef wheel(final String name, final double mass, final Wheel wheel) {
        return new VehiclePartDef(VehiclePartKind.WHEEL, name, Optional.empty(), Optional.empty(), mass,
                Optional.empty(), Optional.of(wheel), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** A fuel tank holding {@code millibuckets}. */
    public static VehiclePartDef tank(final String name, final double mass, final int millibuckets) {
        return new VehiclePartDef(VehiclePartKind.TANK, name, Optional.empty(), Optional.empty(), mass,
                Optional.empty(), Optional.empty(), Optional.of(new Tank(millibuckets)), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** A battery holding {@code joules}. */
    public static VehiclePartDef battery(final String name, final double mass, final int joules) {
        return new VehiclePartDef(VehiclePartKind.BATTERY, name, Optional.empty(), Optional.empty(), mass,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(new Battery(joules)),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** A gearbox. */
    public static VehiclePartDef gearbox(final String name, final double mass, final Gearbox gearbox) {
        return new VehiclePartDef(VehiclePartKind.GEARBOX, name, Optional.empty(), Optional.empty(), mass,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(gearbox),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** A light. */
    public static VehiclePartDef light(final String name, final double mass, final Light light) {
        return new VehiclePartDef(VehiclePartKind.LIGHT, name, Optional.empty(), Optional.empty(), mass,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(light), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** A horn. */
    public static VehiclePartDef horn(final String name, final double mass, final Horn horn) {
        return new VehiclePartDef(VehiclePartKind.HORN, name, Optional.empty(), Optional.empty(), mass,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of(horn), Optional.empty(), Optional.empty());
    }

    /** A rotor that lifts. */
    public static VehiclePartDef rotor(final String name, final double mass, final Thrust rotor) {
        return new VehiclePartDef(VehiclePartKind.ROTOR, name, Optional.empty(), Optional.empty(), mass,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of(rotor), Optional.empty());
    }

    /** A propeller that pushes afloat. */
    public static VehiclePartDef propeller(final String name, final double mass, final Thrust propeller) {
        return new VehiclePartDef(VehiclePartKind.PROPELLER, name, Optional.empty(), Optional.empty(), mass,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(propeller));
    }

    /** The same part, drawn as {@code item} when it is out of a vehicle. */
    public VehiclePartDef drawnAs(final ResourceLocation item) {
        return new VehiclePartDef(kind, name, Optional.of(item), repair, mass, engine, wheel, tank, battery, gearbox,
                light, horn, rotor, propeller);
    }

    /** The same part, mended with a wrench and {@code item}. */
    public VehiclePartDef mendedWith(final ResourceLocation item) {
        return new VehiclePartDef(kind, name, icon, Optional.of(item), mass, engine, wheel, tank, battery, gearbox,
                light, horn, rotor, propeller);
    }

    /* A part of {@code kind} and no spec yet. */
    private static VehiclePartDef of(final VehiclePartKind kind, final String name, final double mass) {
        return new VehiclePartDef(kind, name, Optional.empty(), Optional.empty(), mass, Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    /* The same part, an engine doing as {@code spec} says. */
    private VehiclePartDef with(final Engine spec) {
        return new VehiclePartDef(kind, name, icon, repair, mass, Optional.of(spec), wheel, tank, battery, gearbox,
                light, horn, rotor, propeller);
    }

    /* A part says what it does as its kind does. */
    private static DataResult<VehiclePartDef> check(final VehiclePartDef part) {
        final boolean has = switch (part.kind) {
            case ENGINE -> part.engine.isPresent();
            case WHEEL -> part.wheel.isPresent();
            case TANK -> part.tank.isPresent();
            case BATTERY -> part.battery.isPresent();
            case GEARBOX -> part.gearbox.isPresent();
            case LIGHT -> part.light.isPresent();
            case HORN -> part.horn.isPresent();
            case ROTOR -> part.rotor.isPresent();
            case PROPELLER -> part.propeller.isPresent();
        };
        return has ? DataResult.success(part)
                : DataResult.error(() -> "a part of the kind " + part.kind.serializedName()
                        + " says what it does in a \"" + part.kind.serializedName() + "\" field");
    }

    private static DataResult<Integer> readColour(final String written) {
        if (written.length() != 7 || written.charAt(0) != '#') {
            return DataResult.error(() -> "a colour is written #RRGGBB: " + written);
        }
        try {
            return DataResult.success(HexFormat.fromHexDigits(written.substring(1)));
        } catch (final IllegalArgumentException wrong) {
            return DataResult.error(() -> "a colour is written #RRGGBB: " + written);
        }
    }

    /**
     * What an engine does.
     *
     * @param power      its top power, in watts
     * @param idleRpm    how fast it idles, in turns a minute; nought for an electric one
     * @param maxRpm     how fast it turns at most
     * @param efficiency the share of what it burns it turns into work
     * @param electric   whether it draws on the battery instead of burning fuel
     * @param fuels      the fuels it burns, by id
     * @param sound      the running sound it makes, by the sound's id, or none to run silent
     */
    public record Engine(double power, double idleRpm, double maxRpm, double efficiency, boolean electric,
                         List<ResourceLocation> fuels, Optional<ResourceLocation> sound) {

        public static final Codec<Engine> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(1, Double.MAX_VALUE).fieldOf("power").forGetter(Engine::power),
                Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("idle_rpm", 800.0)
                        .forGetter(Engine::idleRpm),
                Codec.doubleRange(1, Double.MAX_VALUE).optionalFieldOf("max_rpm", 6000.0)
                        .forGetter(Engine::maxRpm),
                Codec.doubleRange(0.01, 1).optionalFieldOf("efficiency", 0.3).forGetter(Engine::efficiency),
                Codec.BOOL.optionalFieldOf("electric", false).forGetter(Engine::electric),
                ResourceLocation.CODEC.listOf().optionalFieldOf("fuels", List.of()).forGetter(Engine::fuels),
                ResourceLocation.CODEC.optionalFieldOf("sound").forGetter(Engine::sound))
                .apply(instance, Engine::new));

        public Engine {
            fuels = List.copyOf(fuels);
        }

        /** The engine as the drivetrain needs it, worn to {@code health}. */
        public VehicleDrive.Engine drive(final double health) {
            return new VehicleDrive.Engine(power, electric ? 0 : Math.min(idleRpm, maxRpm - 1), maxRpm, efficiency,
                    electric, health);
        }
    }

    /**
     * What a wheel does.
     *
     * @param radius    its radius, in metres
     * @param grip      how well its tyre holds on a dry road, 1 a road tyre's
     * @param stiffness how stiff its spring is, in newtons a metre
     * @param damping   how hard its damper holds back the spring, in newton seconds a metre
     * @param travel    how far its spring gives, in metres
     */
    public record Wheel(double radius, double grip, double stiffness, double damping, double travel) {

        public static final Codec<Wheel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0.05, 4).fieldOf("radius").forGetter(Wheel::radius),
                Codec.doubleRange(0, 4).optionalFieldOf("grip", 1.0).forGetter(Wheel::grip),
                Codec.doubleRange(1, Double.MAX_VALUE).optionalFieldOf("stiffness", 30_000.0)
                        .forGetter(Wheel::stiffness),
                Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("damping", 3000.0).forGetter(Wheel::damping),
                Codec.doubleRange(0, 2).optionalFieldOf("travel", 0.2).forGetter(Wheel::travel))
                .apply(instance, Wheel::new));
    }

    /** What a fuel tank holds, in millibuckets. */
    public record Tank(int capacity) {

        public static final Codec<Tank> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("capacity").forGetter(Tank::capacity))
                .apply(instance, Tank::new));
    }

    /** What a battery holds, in joules. */
    public record Battery(int capacity) {

        public static final Codec<Battery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("capacity").forGetter(Battery::capacity))
                .apply(instance, Battery::new));
    }

    /**
     * A gearbox's gears.
     *
     * @param ratios     the forward gears, the first the lowest
     * @param reverse    the reverse gear
     * @param finalDrive the axle's ratio after the gearbox
     */
    public record Gearbox(List<Double> ratios, double reverse, double finalDrive) {

        public static final Codec<Gearbox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0.01, 100).listOf(1, 16).fieldOf("ratios").forGetter(Gearbox::ratios),
                Codec.doubleRange(0.01, 100).optionalFieldOf("reverse", 3.5).forGetter(Gearbox::reverse),
                Codec.doubleRange(0.01, 100).optionalFieldOf("final_drive", 4.0).forGetter(Gearbox::finalDrive))
                .apply(instance, Gearbox::new));

        public Gearbox {
            ratios = List.copyOf(ratios);
        }

        /** The gearbox as the drivetrain needs it. */
        public VehicleDrive.Gearbox drive() {
            return new VehicleDrive.Gearbox(ratios, reverse, finalDrive);
        }
    }

    /**
     * What a light lights.
     *
     * @param role   what it is for, which says when it shines
     * @param level  how bright, from 1 to 15
     * @param colour its colour
     * @param reach  how far ahead a headlight lights the way, in blocks
     */
    public record Light(VehicleLightRole role, int level, int colour, double reach) {

        public static final Codec<Light> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StableCodecs.byName(VehicleLightRole.class).fieldOf("role").forGetter(Light::role),
                Codec.intRange(1, 15).optionalFieldOf("level", 14).forGetter(Light::level),
                COLOUR.optionalFieldOf("colour", 0xFFF4E0).forGetter(Light::colour),
                Codec.doubleRange(0, 64).optionalFieldOf("reach", 12.0).forGetter(Light::reach))
                .apply(instance, Light::new));
    }

    /** What a horn sounds, by the sound's id, or none to sound nothing. */
    public record Horn(Optional<ResourceLocation> sound) {

        public static final Codec<Horn> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("sound").forGetter(Horn::sound))
                .apply(instance, Horn::new));
    }

    /** What a rotor lifts or a propeller pushes, in newtons at full power. */
    public record Thrust(double thrust) {

        public static final Codec<Thrust> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0, Double.MAX_VALUE).fieldOf("thrust").forGetter(Thrust::thrust))
                .apply(instance, Thrust::new));
    }
}
