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
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

/**
 * A vehicle as a data pack or a mod declares it, so a pack adds a car, a boat or a drone with no code: the name players
 * read, its model, its body, its seats and the roles they hold, the places its parts go and the parts it is made with,
 * its boot and cargo tanks, its paints and variants, how it goes, where it hitches and tows, and the key it is driven
 * with. Places are in metres from the middle of its underside, x to its left, y up, z ahead.
 *
 * @param name     the translation key of its name
 * @param model    the model it is drawn with, or none to draw nothing
 * @param body     its size, mass and drag
 * @param movement how it goes: on its wheels, in the air or afloat
 * @param seats    its seats, each with its role
 * @param places   the places its parts go
 * @param boot     how many slots its boot has
 * @param cargo    its cargo tanks
 * @param paints   the paints it can wear, the first the one it is made in
 * @param variants the shapes it comes in, the first the plain one
 * @param hitching where it hitches to a vehicle ahead and tows one behind
 * @param key      the item cut as its key, or none to be driven by whom its owner lets
 * @param icon     the item drawn for it while it is an item, or none to draw its model
 * @param doors    its doors, by the names of the model's bones that swing open, up to sixteen
 */
public record VehicleDef(String name, Optional<ResourceLocation> model, Body body, Movement movement,
                         List<Seat> seats, List<Place> places, int boot, List<CargoTank> cargo, List<Paint> paints,
                         List<Variant> variants, Hitching hitching, Optional<ResourceLocation> key,
                         Optional<ResourceLocation> icon, List<String> doors) {

    public static final Codec<VehicleDef> CODEC = RecordCodecBuilder.<VehicleDef>create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(VehicleDef::name),
            ResourceLocation.CODEC.optionalFieldOf("model").forGetter(VehicleDef::model),
            Body.CODEC.fieldOf("body").forGetter(VehicleDef::body),
            StableCodecs.byName(Movement.class).optionalFieldOf("movement", Movement.ROAD)
                    .forGetter(VehicleDef::movement),
            Seat.CODEC.listOf().fieldOf("seats").forGetter(VehicleDef::seats),
            Place.CODEC.listOf().optionalFieldOf("places", List.of()).forGetter(VehicleDef::places),
            Codec.intRange(0, 54).optionalFieldOf("boot", 0).forGetter(VehicleDef::boot),
            CargoTank.CODEC.listOf().optionalFieldOf("cargo", List.of()).forGetter(VehicleDef::cargo),
            Paint.CODEC.listOf().optionalFieldOf("paints", List.of()).forGetter(VehicleDef::paints),
            Variant.CODEC.listOf().optionalFieldOf("variants", List.of()).forGetter(VehicleDef::variants),
            Hitching.CODEC.optionalFieldOf("hitching", Hitching.NONE).forGetter(VehicleDef::hitching),
            ResourceLocation.CODEC.optionalFieldOf("key").forGetter(VehicleDef::key),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(VehicleDef::icon),
            Codec.STRING.listOf(0, VehicleSwitches.MOST_DOORS).optionalFieldOf("doors", List.of())
                    .forGetter(VehicleDef::doors))
            .apply(instance, VehicleDef::new)).validate(VehicleDef::check);

    public VehicleDef {
        Objects.requireNonNull(name, "name");
        seats = List.copyOf(seats);
        places = List.copyOf(places);
        cargo = List.copyOf(cargo);
        paints = List.copyOf(paints);
        variants = List.copyOf(variants);
        doors = List.copyOf(doors);
    }

    /** Starts declaring a vehicle whose name players read as the translation key {@code name}. */
    public static Builder builder(final String name) {
        return new Builder(name);
    }

    /** Its place called {@code name}, or none. */
    public Optional<Place> place(final String placeName) {
        for (final Place place : places) {
            if (place.name().equals(placeName)) {
                return Optional.of(place);
            }
        }
        return Optional.empty();
    }

    /** Its paint called {@code id}, or its first, or none when it has no paints. */
    public Optional<Paint> paint(final String id) {
        for (final Paint paint : paints) {
            if (paint.id().equals(id)) {
                return Optional.of(paint);
            }
        }
        return paints.isEmpty() ? Optional.empty() : Optional.of(paints.getFirst());
    }

    /** Its variant called {@code id}, or its first, or none when it has no variants. */
    public Optional<Variant> variant(final String id) {
        for (final Variant variant : variants) {
            if (variant.id().equals(id)) {
                return Optional.of(variant);
            }
        }
        return variants.isEmpty() ? Optional.empty() : Optional.of(variants.getFirst());
    }

    /* A vehicle has a seat, and its places names of their own. */
    private static DataResult<VehicleDef> check(final VehicleDef def) {
        final Set<String> names = new HashSet<>();
        for (final Place place : def.places) {
            if (!names.add(place.name())) {
                return DataResult.error(() -> "two places of a vehicle are called " + place.name());
            }
        }
        return DataResult.success(def);
    }

    /** How a vehicle goes. */
    public enum Movement implements IStableName {
        /** On its wheels, along the ground. */
        ROAD("road"),
        /** In the air, held up by its rotors. */
        AIR("air"),
        /** Afloat, pushed by its propellers. */
        WATER("water");

        private final String serializedName;

        Movement(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }

    /**
     * Its body.
     *
     * @param width    how wide its box is, in blocks
     * @param height   how tall its box is, in blocks
     * @param mass     its mass without its parts, in kilograms
     * @param dragArea how much air it pushes aside, its drag coefficient times its front's area, in square metres
     */
    public record Body(float width, float height, double mass, double dragArea) {

        public static final Codec<Body> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.floatRange(0.1F, 16).fieldOf("width").forGetter(Body::width),
                Codec.floatRange(0.1F, 16).fieldOf("height").forGetter(Body::height),
                Codec.doubleRange(1, Double.MAX_VALUE).fieldOf("mass").forGetter(Body::mass),
                Codec.doubleRange(0, 1000).optionalFieldOf("drag_area", 0.8).forGetter(Body::dragArea))
                .apply(instance, Body::new));
    }

    /**
     * A seat.
     *
     * @param at   where whoever sits in it sits
     * @param role what they do from it, by the role's id
     */
    public record Seat(Vec3 at, ResourceLocation role) {

        public static final Codec<Seat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Vec3.CODEC.fieldOf("at").forGetter(Seat::at),
                ResourceLocation.CODEC.optionalFieldOf("role", VehicleRoles.PASSENGER).forGetter(Seat::role))
                .apply(instance, Seat::new));
    }

    /**
     * A place a part goes.
     *
     * @param name   what it is called, unique in its vehicle; a model's bone of the same name is the part there
     * @param kind   the kind of part it takes
     * @param at     where it is
     * @param part   the part it is made with, by the part's id, or none to be made empty
     * @param steers whether a wheel there steers
     * @param driven whether a wheel there is driven by the engine
     */
    public record Place(String name, VehiclePartKind kind, Vec3 at, Optional<ResourceLocation> part, boolean steers,
                        boolean driven) {

        public static final Codec<Place> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Place::name),
                StableCodecs.byName(VehiclePartKind.class).fieldOf("kind").forGetter(Place::kind),
                Vec3.CODEC.optionalFieldOf("at", Vec3.ZERO).forGetter(Place::at),
                ResourceLocation.CODEC.optionalFieldOf("part").forGetter(Place::part),
                Codec.BOOL.optionalFieldOf("steers", false).forGetter(Place::steers),
                Codec.BOOL.optionalFieldOf("driven", false).forGetter(Place::driven))
                .apply(instance, Place::new));
    }

    /**
     * A cargo tank.
     *
     * @param capacity what it holds, in millibuckets
     * @param holds    the fluids it holds, by tag, or none for any
     */
    public record CargoTank(int capacity, Optional<TagKey<Fluid>> holds) {

        public static final Codec<CargoTank> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("capacity").forGetter(CargoTank::capacity),
                TagKey.codec(Registries.FLUID).optionalFieldOf("holds").forGetter(CargoTank::holds))
                .apply(instance, CargoTank::new));
    }

    /**
     * A paint.
     *
     * @param id      what it is known by
     * @param name    the translation key of its name
     * @param texture the texture it is drawn with, or none for its model's own
     * @param dye     the dye that paints it, or none when no dye does
     */
    public record Paint(String id, String name, Optional<ResourceLocation> texture, Optional<DyeColor> dye) {

        public static final Codec<Paint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Paint::id),
                Codec.STRING.fieldOf("name").forGetter(Paint::name),
                ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(Paint::texture),
                DyeColor.CODEC.optionalFieldOf("dye").forGetter(Paint::dye))
                .apply(instance, Paint::new));
    }

    /**
     * A shape it comes in.
     *
     * @param id    what it is known by
     * @param name  the translation key of its name
     * @param model the model it is drawn with, or none for the vehicle's own
     */
    public record Variant(String id, String name, Optional<ResourceLocation> model) {

        public static final Codec<Variant> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Variant::id),
                Codec.STRING.fieldOf("name").forGetter(Variant::name),
                ResourceLocation.CODEC.optionalFieldOf("model").forGetter(Variant::model))
                .apply(instance, Variant::new));
    }

    /**
     * Where it hitches to a vehicle ahead and where it tows one behind.
     *
     * @param hitch its coupling, at its front, or none when nothing tows it
     * @param tow   its tow point, at its back, or none when it tows nothing
     */
    public record Hitching(Optional<Vec3> hitch, Optional<Vec3> tow) {

        /** Neither. */
        public static final Hitching NONE = new Hitching(Optional.empty(), Optional.empty());

        public static final Codec<Hitching> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Vec3.CODEC.optionalFieldOf("hitch").forGetter(Hitching::hitch),
                Vec3.CODEC.optionalFieldOf("tow").forGetter(Hitching::tow))
                .apply(instance, Hitching::new));
    }

    /** Declares a vehicle one property at a time, in code. */
    public static final class Builder {

        private final String name;
        private final List<Seat> seats = new ArrayList<>();
        private final List<Place> places = new ArrayList<>();
        private final List<CargoTank> cargo = new ArrayList<>();
        private final List<Paint> paints = new ArrayList<>();
        private final List<Variant> variants = new ArrayList<>();
        private final List<String> doors = new ArrayList<>();
        private Optional<ResourceLocation> model = Optional.empty();
        private Body body = new Body(1, 1, 1000, 0.8);
        private Movement movement = Movement.ROAD;
        private int boot;
        private Optional<Vec3> hitch = Optional.empty();
        private Optional<Vec3> tow = Optional.empty();
        private Optional<ResourceLocation> key = Optional.empty();
        private Optional<ResourceLocation> icon = Optional.empty();

        private Builder(final String name) {
            this.name = Objects.requireNonNull(name, "name");
        }

        public Builder model(final ResourceLocation drawnWith) {
            this.model = Optional.of(drawnWith);
            return this;
        }

        public Builder body(final float width, final float height, final double mass, final double dragArea) {
            this.body = new Body(width, height, mass, dragArea);
            return this;
        }

        public Builder movement(final Movement goes) {
            this.movement = Objects.requireNonNull(goes, "goes");
            return this;
        }

        public Builder seat(final Vec3 at, final ResourceLocation role) {
            this.seats.add(new Seat(at, role));
            return this;
        }

        /** A place for a part of {@code kind}, made with {@code part}, or empty when it is null. */
        public Builder place(final String placeName, final VehiclePartKind kind, final Vec3 at,
                             final ResourceLocation part) {
            this.places.add(new Place(placeName, kind, at, Optional.ofNullable(part), false, false));
            return this;
        }

        /** A wheel's place, made with {@code part}, steering and driven as said. */
        public Builder wheel(final String placeName, final Vec3 at, final ResourceLocation part, final boolean steers,
                             final boolean driven) {
            this.places.add(new Place(placeName, VehiclePartKind.WHEEL, at, Optional.ofNullable(part), steers,
                    driven));
            return this;
        }

        public Builder boot(final int slots) {
            this.boot = slots;
            return this;
        }

        public Builder cargo(final int capacity, final TagKey<Fluid> holds) {
            this.cargo.add(new CargoTank(capacity, Optional.ofNullable(holds)));
            return this;
        }

        public Builder paint(final String id, final String paintName, final ResourceLocation texture,
                             final DyeColor dye) {
            this.paints.add(new Paint(id, paintName, Optional.ofNullable(texture), Optional.ofNullable(dye)));
            return this;
        }

        public Builder variant(final String id, final String variantName, final ResourceLocation variantModel) {
            this.variants.add(new Variant(id, variantName, Optional.ofNullable(variantModel)));
            return this;
        }

        public Builder hitch(final Vec3 at) {
            this.hitch = Optional.of(at);
            return this;
        }

        public Builder tow(final Vec3 at) {
            this.tow = Optional.of(at);
            return this;
        }

        public Builder key(final ResourceLocation item) {
            this.key = Optional.of(item);
            return this;
        }

        public Builder icon(final ResourceLocation item) {
            this.icon = Optional.of(item);
            return this;
        }

        /** A door, by the name of its model's bone that swings open. */
        public Builder door(final String bone) {
            this.doors.add(bone);
            return this;
        }

        public VehicleDef build() {
            final VehicleDef def = new VehicleDef(name, model, body, movement, seats, places, boot, cargo, paints,
                    variants, new Hitching(hitch, tow), key, icon, doors);
            return check(def).getOrThrow(IllegalArgumentException::new);
        }
    }
}
