/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.rocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.client.model.kit.KitModels;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * The live cabins of the rocket parts' models, each read from the file beside its model,
 * {@code jsmodels/rocket/<part>.cabin.json}: the faces of its panel the flight is shown on, each with the block it
 * stands in, the regions of each face where something live is drawn, and the attitude ball and the throttle, each a
 * bone of the model. Places are in the model's own space, in blocks; a region's box is in fractions across its face,
 * from its left, and down it, from its top. Read the first time a model is drawn, and again after the models reload.
 */
public final class RocketCabins {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<ResourceLocation, Optional<Cabin>> READ = new HashMap<>();
    private static long readAt = -1;

    private RocketCabins() {
    }

    /** The live cabin of the model {@code model}, if it has one. */
    public static Optional<Cabin> of(final ResourceLocation model) {
        if (readAt != KitModels.reloadedAt()) {
            READ.clear();
            readAt = KitModels.reloadedAt();
        }
        return READ.computeIfAbsent(model, RocketCabins::read);
    }

    private static Optional<Cabin> read(final ResourceLocation model) {
        final ResourceLocation file = ResourceLocation.fromNamespaceAndPath(model.getNamespace(),
                "jsmodels/" + model.getPath() + ".cabin.json");
        final Optional<Resource> found = Minecraft.getInstance().getResourceManager().getResource(file);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        try (Reader reader = found.get().openAsReader()) {
            final JsonElement json = JsonParser.parseReader(reader);
            return Cabin.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(problem -> LOG.warn(
                    "The live cabin {} cannot be read: {}", file, problem));
        } catch (final IOException | RuntimeException wrong) {
            LOG.warn("The live cabin {} cannot be read", file, wrong);
            return Optional.empty();
        }
    }

    /**
     * A model's live cabin.
     *
     * @param faces    the faces the flight is shown on
     * @param regions  where on its faces each live thing is drawn
     * @param ball     the attitude ball, or a starship's navigation globe, if it has one
     * @param throttle the throttle, if it has one
     * @param stick    a starship's side stick, if it has one
     */
    public record Cabin(List<Face> faces, List<Region> regions, Optional<Ball> ball, Optional<Throttle> throttle,
                        Optional<Stick> stick) {

        public static final Codec<Cabin> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Face.CODEC.listOf().fieldOf("faces").forGetter(Cabin::faces),
                Region.CODEC.listOf().fieldOf("regions").forGetter(Cabin::regions),
                Ball.CODEC.optionalFieldOf("ball").forGetter(Cabin::ball),
                Throttle.CODEC.optionalFieldOf("throttle").forGetter(Cabin::throttle),
                Stick.CODEC.optionalFieldOf("stick").forGetter(Cabin::stick)
        ).apply(instance, Cabin::new));

        public Cabin {
            faces = List.copyOf(faces);
            regions = List.copyOf(regions);
        }

        /** The face named {@code name}, if it has one. */
        public Optional<Face> face(final String name) {
            return this.faces.stream().filter(face -> face.name().equals(name)).findFirst();
        }
    }

    /**
     * A flat face of a cabin's panel.
     *
     * @param name       its name, as its regions name it
     * @param cell       the bone of the block it stands in: a block missing takes the face with it
     * @param centre     its middle
     * @param right      the way across it, to its right as the crew see it
     * @param up         the way up it, as the crew see it
     * @param out        the way it faces, towards the crew
     * @param halfWidth  half its width
     * @param halfHeight half its height
     */
    public record Face(String name, String cell, Vector3f centre, Vector3f right, Vector3f up, Vector3f out,
                       float halfWidth, float halfHeight) {

        public static final Codec<Face> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Face::name),
                Codec.STRING.fieldOf("cell").forGetter(Face::cell),
                ExtraCodecs.VECTOR3F.fieldOf("centre").forGetter(Face::centre),
                ExtraCodecs.VECTOR3F.fieldOf("right").forGetter(Face::right),
                ExtraCodecs.VECTOR3F.fieldOf("up").forGetter(Face::up),
                ExtraCodecs.VECTOR3F.fieldOf("out").forGetter(Face::out),
                Codec.FLOAT.fieldOf("half_width").forGetter(Face::halfWidth),
                Codec.FLOAT.fieldOf("half_height").forGetter(Face::halfHeight)
        ).apply(instance, Face::new));
    }

    /**
     * Where on a face a live thing is drawn.
     *
     * @param face  the face's name
     * @param kind  what is drawn there
     * @param index which of its kind it is, as the lamps and the strips are counted
     * @param box   its left, top, right and bottom, in fractions of its face
     */
    public record Region(String face, String kind, int index, List<Float> box) {

        public static final Codec<Region> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("face").forGetter(Region::face),
                Codec.STRING.fieldOf("kind").forGetter(Region::kind),
                Codec.INT.optionalFieldOf("index", 0).forGetter(Region::index),
                Codec.FLOAT.listOf(4, 4).fieldOf("box").forGetter(Region::box)
        ).apply(instance, Region::new));

        public Region {
            box = List.copyOf(box);
        }

        /** Its left, in fractions of its face. */
        public float left() {
            return this.box.get(0);
        }

        /** Its top, in fractions of its face. */
        public float top() {
            return this.box.get(1);
        }

        /** Its right, in fractions of its face. */
        public float right() {
            return this.box.get(2);
        }

        /** Its bottom, in fractions of its face. */
        public float bottom() {
            return this.box.get(3);
        }
    }

    /**
     * The attitude ball: a bone turned about its middle, its pole along {@code up} at rest.
     *
     * @param bone   the bone
     * @param centre its middle, the bone's pivot
     * @param up     its pole at rest, up the panel
     * @param right  the way across the panel
     * @param out    the way the panel faces, towards the crew
     */
    public record Ball(String bone, Vector3f centre, Vector3f up, Vector3f right, Vector3f out) {

        public static final Codec<Ball> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("bone").forGetter(Ball::bone),
                ExtraCodecs.VECTOR3F.fieldOf("centre").forGetter(Ball::centre),
                ExtraCodecs.VECTOR3F.fieldOf("up").forGetter(Ball::up),
                ExtraCodecs.VECTOR3F.fieldOf("right").forGetter(Ball::right),
                ExtraCodecs.VECTOR3F.fieldOf("out").forGetter(Ball::out)
        ).apply(instance, Ball::new));
    }

    /**
     * The throttle: a bone slid along its slot.
     *
     * @param bone   the bone
     * @param travel how far it slides from the slot's foot to its head
     * @param rest   where along the slot the model has it, 0 to 1
     */
    public record Throttle(String bone, Vector3f travel, float rest) {

        public static final Codec<Throttle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("bone").forGetter(Throttle::bone),
                ExtraCodecs.VECTOR3F.fieldOf("travel").forGetter(Throttle::travel),
                Codec.FLOAT.fieldOf("rest").forGetter(Throttle::rest)
        ).apply(instance, Throttle::new));
    }

    /**
     * A side stick: a bone leaning about its foot, forward by the pitch asked and aside by the roll.
     *
     * @param bone  the bone, its pivot the stick's foot
     * @param right the way across the cabin, which it leans forward about
     * @param out   the way back along the cabin, which it leans aside about
     */
    public record Stick(String bone, Vector3f right, Vector3f out) {

        public static final Codec<Stick> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("bone").forGetter(Stick::bone),
                ExtraCodecs.VECTOR3F.fieldOf("right").forGetter(Stick::right),
                ExtraCodecs.VECTOR3F.fieldOf("out").forGetter(Stick::out)
        ).apply(instance, Stick::new));
    }
}
